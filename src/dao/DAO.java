package dao;

import indices.BPlusTree;
import indices.ExtensibleHash;
import model.Register;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe genérica de Acesso a Dados (DAO) para manipular entidades que implementam a interface Register.
 * Esta classe é responsável por todas as operações de CRUD (Create, Read, Update, Delete)
 * diretamente num ficheiro binário, além de gerir os ficheiros de índice associados (Hash Extensível e Árvore B+).
 * @param <T> O tipo da entidade (ex: Produto, Categoria) que este DAO irá gerir.
 */
public class DAO<T extends Register> {

    // Caminho para o ficheiro de dados principal (ex: "produtos.db")
    private final String filePath;
    // Objeto para manipulação direta do ficheiro binário
    private RandomAccessFile file;
    // Construtor da classe T, usado para criar novas instâncias de forma genérica
    private final Constructor<T> constructor;
    // Índice de Hash Extensível para busca rápida por ID (chave primária)
    private ExtensibleHash hash;
    // Índice de Árvore B+ para busca e listagem ordenada por uma chave secundária (ex: nome)
    private BPlusTree bPlusTree;

    /**
     * Construtor da classe DAO.
     * @param filePath O caminho para o ficheiro de dados .db.
     * @param cls A classe da entidade que será gerida (ex: Produto.class).
     * @param enableBPlusTree Um booleano que indica se o índice de Árvore B+ deve ser ativado para esta entidade.
     * @throws IOException Se ocorrer um erro ao abrir ou criar os ficheiros.
     */
    public DAO(String filePath, Class<T> cls, boolean enableBPlusTree) throws IOException {
        this.filePath = filePath;
        this.file = new RandomAccessFile(this.filePath, "rw");

        try {
            // Guarda o construtor padrão da classe para poder criar novas instâncias depois
            this.constructor = cls.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("A classe " + cls.getName() + " não possui um construtor padrão.");
        }

        // Gera os nomes dos ficheiros de índice a partir do nome do ficheiro de dados
        String baseName = filePath.replace(".db", "");
        this.hash = new ExtensibleHash(baseName + ".hash.dir", baseName + ".hash.bkt");

        // Inicializa a Árvore B+ apenas se for solicitado
        if (enableBPlusTree) {
            this.bPlusTree = new BPlusTree(baseName + ".bptree.idx");
        }

        // Se o ficheiro de dados estiver vazio, escreve o cabeçalho inicial (último ID = 0)
        if (file.length() == 0) {
            file.writeInt(0);
        }
    }

    /**
     * Cria um novo registo no ficheiro de dados e nos índices.
     * @param obj O objeto a ser inserido.
     * @return O ID atribuído ao novo objeto.
     * @throws IOException Se ocorrer um erro de escrita.
     */
    public int create(T obj) throws IOException {
        // 1. Atualiza o ID do objeto
        file.seek(0);
        int ultimoID = file.readInt();
        int novoID = ultimoID + 1;
        obj.setID(novoID);

        // 2. Posiciona o ponteiro no final do ficheiro para escrever o novo registo
        long posicaoRegistro = file.length();
        file.seek(posicaoRegistro);

        // 3. Serializa o objeto e escreve no formato: [lápide (1 byte)] [tamanho (4 bytes)] [dados]
        byte[] data = obj.toByteArray();
        file.writeByte(0); // Lápide (0 = ativo)
        file.writeInt(data.length);
        file.write(data);

        // 4. Atualiza o cabeçalho do ficheiro com o novo último ID
        file.seek(0);
        file.writeInt(novoID);

        // 5. Insere a referência no índice de Hash: (ID -> posição no ficheiro)
        hash.insert(novoID, posicaoRegistro);

        // 6. Se a Árvore B+ estiver ativa, insere a referência: (Chave Secundária -> ID)
        if (bPlusTree != null) {
            bPlusTree.insert(obj.getSecondaryKey(), novoID);
        }

        return novoID;
    }

    /**
     * Lista todos os registos ativos, ordenados pela chave secundária, utilizando a Árvore B+.
     * @return Uma lista de objetos ordenada.
     * @throws IOException Se ocorrer um erro de leitura.
     */
    public List<T> listAllSortedBySecondaryKey() throws IOException {
        if (bPlusTree == null) {
            throw new IllegalStateException("A Árvore B+ não está habilitada para este DAO.");
        }

        // 1. Obtém a lista de todos os IDs, já ordenados, da Árvore B+
        List<Integer> sortedIds = bPlusTree.listAll();
        List<T> sortedList = new ArrayList<>();

        // 2. Para cada ID, busca o objeto correspondente usando o método read (que é rápido por usar o hash)
        for (int id : sortedIds) {
            T obj = read(id);
            // Verifica se o objeto não foi excluído
            if (obj != null) {
                sortedList.add(obj);
            }
        }
        return sortedList;
    }

    /**
     * Lê um registo do ficheiro de dados com base no seu ID, utilizando o índice de Hash para acesso rápido.
     * @param id O ID do objeto a ser lido.
     * @return O objeto encontrado, ou null se não existir ou tiver sido excluído.
     * @throws IOException Se ocorrer um erro de leitura.
     */
    public T read(int id) throws IOException {
        // 1. Procura a posição do registo no índice de Hash
        long posicao = hash.search(id);
        if (posicao == -1) return null; // ID não encontrado no índice

        // 2. Vai até a posição no ficheiro de dados
        file.seek(posicao);

        // 3. Verifica a lápide
        byte lapide = file.readByte();
        if (lapide == 1) return null; // Registo foi excluído

        // 4. Lê o tamanho e os dados do registo
        int tamanho = file.readInt();
        byte[] data = new byte[tamanho];
        file.read(data);

        // 5. Desserializa os bytes para um objeto
        T obj = createInstance();
        obj.fromByteArray(data);

        // 6. Confirma se o ID do objeto lido é o mesmo que foi procurado (verificação de consistência)
        return (obj.getID() == id) ? obj : null;
    }

    /**
     * Atualiza um registo.
     * Estratégia simplificada: remove o registo antigo e cria um novo.
     * @param obj O objeto com os dados atualizados (deve ter o mesmo ID).
     * @return true se a atualização foi bem-sucedida, false caso contrário.
     * @throws IOException Se ocorrer um erro de I/O.
     */
    public boolean update(T obj) throws IOException {
        // Verifica se o objeto a ser atualizado realmente existe
        T oldObj = read(obj.getID());
        if (oldObj == null) return false;

        // Estratégia de apagar e criar de novo.
        // Simplificação: A remoção na Árvore B+ não foi implementada.
        // A chave antiga permanecerá no índice, mas será ignorada na listagem porque read(id) retornará null.
        delete(obj.getID());
        create(obj);
        return true;
    }

    /**
     * Exclui um registo logicamente (marcação com lápide) e remove-o dos índices.
     * @param id O ID do objeto a ser excluído.
     * @return true se a exclusão foi bem-sucedida, false caso contrário.
     * @throws IOException Se ocorrer um erro de I/O.
     */
    public boolean delete(int id) throws IOException {
        // 1. Procura a posição do registo no índice de Hash
        long posicao = hash.search(id);
        if (posicao == -1) return false; // ID não encontrado

        // 2. Vai até a posição e marca a lápide como '1' (excluído)
        file.seek(posicao);
        file.writeByte(1);

        // 3. Remove a entrada do índice de Hash para que não seja mais encontrada
        hash.delete(id);

        // NOTA: A remoção na Árvore B+ não foi implementada para simplificar o projeto.
        // A chave antiga (ex: nome do produto) continuará no índice,
        // mas o método `listAllSorted` irá ignorá-la porque `read(id)` retornará `null`.
        return true;
    }

    /**
     * Lista todos os registos ativos através de uma varredura sequencial do ficheiro.
     * Este método é mais lento e não deve ser usado para grandes volumes de dados.
     * @return Uma lista com todos os objetos ativos.
     * @throws IOException Se ocorrer um erro de leitura.
     */
    public List<T> listAll() throws IOException {
        List<T> list = new ArrayList<>();
        file.seek(4); // Pula o cabeçalho (último ID)

        while (file.getFilePointer() < file.length()) {
            byte lapide = file.readByte();
            int tamanho = file.readInt();

            if (lapide == 0) { // Se o registo estiver ativo
                byte[] data = new byte[tamanho];
                file.read(data);
                T obj = createInstance();
                obj.fromByteArray(data);
                list.add(obj);
            } else { // Se o registo estiver excluído, apenas pula os seus bytes
                file.skipBytes(tamanho);
            }
        }
        return list;
    }

    /**
     * Cria uma nova instância da classe genérica T usando reflexão.
     * @return Uma nova instância de T.
     */
    private T createInstance() {
        try {
            return constructor.newInstance();
        } catch (Exception e) {
            // Lança uma exceção de tempo de execução se não conseguir criar a instância
            throw new RuntimeException("Erro ao criar instância da classe " + constructor.getDeclaringClass().getName(), e);
        }
    }
}

