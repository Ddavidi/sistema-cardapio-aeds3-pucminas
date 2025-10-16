package dao;

import indices.BPlusTree;
import indices.ExtensibleHash;
import model.Register;
import java.io.*;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe de Acesso a Dados (DAO) genérica para manipular entidades 'Register'.
 * Controla a persistência em ficheiro binário e a indexação.
 */
public class DAO<T extends Register> {

    private final RandomAccessFile dbFile;
    private final Constructor<T> constructor;
    private final ExtensibleHash hash;
    private final BPlusTree bPlusTree;

    public DAO(String dbFilePath, Class<T> clazz, boolean useBPlusTree) throws IOException, NoSuchMethodException {
        this.dbFile = new RandomAccessFile(dbFilePath, "rw");
        this.constructor = clazz.getConstructor();

        // Inicializa o Hash Extensível para a chave primária (ID)
        String baseName = dbFilePath.replace(".db", "");
        this.hash = new ExtensibleHash(baseName + ".hash.dir", baseName + ".hash.bkt");

        // Inicializa a Árvore B+ para a chave secundária (se aplicável)
        if(useBPlusTree) {
            this.bPlusTree = new BPlusTree(baseName + ".bptree.idx");
        } else {
            this.bPlusTree = null;
        }

        if (dbFile.length() == 0) {
            // Cabeçalho: Escreve o último ID como 0 se o ficheiro for novo
            dbFile.writeInt(0);
        }
    }

    /**
     * Fecha todos os ficheiros abertos por este DAO.
     */
    public void close() throws IOException {
        dbFile.close();
        hash.close();
        if (bPlusTree != null) {
            bPlusTree.close();
        }
    }

    public int create(T obj) throws IOException {
        dbFile.seek(0);
        int ultimoID = dbFile.readInt();
        int novoID = ultimoID + 1;
        dbFile.seek(0);
        dbFile.writeInt(novoID);

        obj.setID(novoID);
        byte[] byteArray = obj.toByteArray();

        dbFile.seek(dbFile.length());
        long posicao = dbFile.getFilePointer();

        // Escreve o registo no ficheiro de dados
        dbFile.writeByte(0); // Lápide (0 = ativo)
        dbFile.writeInt(byteArray.length);
        dbFile.write(byteArray);

        // Atualiza os índices
        hash.insert(novoID, posicao);
        if (bPlusTree != null) {
            bPlusTree.insert(obj.getSecondaryKey(), novoID);
        }

        return novoID;
    }

    public T read(int id) throws Exception {
        long posicao = hash.search(id);
        if (posicao == -1) return null;

        dbFile.seek(posicao);
        byte lapide = dbFile.readByte();
        if (lapide == 1) return null; // Registo excluído

        int tamanho = dbFile.readInt();
        byte[] byteArray = new byte[tamanho];
        dbFile.read(byteArray);

        T obj = constructor.newInstance();
        obj.fromByteArray(byteArray);
        return obj;
    }

    public boolean update(T obj) throws Exception {
        long posicao = hash.search(obj.getID());
        if (posicao == -1) return false;

        dbFile.seek(posicao);
        byte lapide = dbFile.readByte();
        if (lapide == 1) return false;

        int tamanhoAntigo = dbFile.readInt();
        byte[] novoByteArray = obj.toByteArray();

        if (novoByteArray.length <= tamanhoAntigo) {
            dbFile.seek(posicao + 5); // Pula lápide e tamanho
            dbFile.write(novoByteArray);
        } else {
            dbFile.seek(posicao);
            dbFile.writeByte(1); // Marca o antigo como excluído

            dbFile.seek(dbFile.length());
            long novaPosicao = dbFile.getFilePointer();
            dbFile.writeByte(0);
            dbFile.writeInt(novoByteArray.length);
            dbFile.write(novoByteArray);
            hash.insert(obj.getID(), novaPosicao); // Atualiza o ponteiro no hash
        }

        // A atualização da Árvore B+ em caso de mudança da chave secundária não está implementada.
        // Requereria apagar o nó antigo e inserir o novo.
        return true;
    }

    public boolean delete(int id) throws IOException {
        long posicao = hash.search(id);
        if (posicao == -1) return false;

        dbFile.seek(posicao);
        dbFile.writeByte(1); // Marca a lápide como excluído

        // A remoção dos índices não está implementada nesta versão
        return true;
    }

    public List<T> listAll() throws Exception {
        List<T> lista = new ArrayList<>();
        dbFile.seek(4); // Pula o cabeçalho
        while (dbFile.getFilePointer() < dbFile.length()) {
            long pos = dbFile.getFilePointer();
            byte lapide = dbFile.readByte();
            int tamanho = dbFile.readInt();
            byte[] byteArray = new byte[tamanho];
            dbFile.read(byteArray);
            if (lapide == 0) {
                T obj = constructor.newInstance();
                obj.fromByteArray(byteArray);
                lista.add(obj);
            }
        }
        return lista;
    }

    public List<T> listAllSortedBySecondaryKey() throws Exception {
        if (bPlusTree == null) {
            throw new UnsupportedOperationException("A Árvore B+ não está habilitada para esta entidade.");
        }
        List<T> listaOrdenada = new ArrayList<>();
        List<Integer> idsOrdenados = bPlusTree.listAll();

        for (int id : idsOrdenados) {
            T obj = read(id);
            if(obj != null) {
                listaOrdenada.add(obj);
            }
        }
        return listaOrdenada;
    }
}

