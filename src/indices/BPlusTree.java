package indices;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementação de uma Árvore B+ para indexação de chaves secundárias.
 * Mapeia uma chave (String) para um valor (int - o ID do registo).
 */
public class BPlusTree {

    private final RandomAccessFile file;
    private final int ORDER = 5; // Ordem da árvore (número máximo de filhos)
    private final int MAX_KEYS = ORDER - 1; // 4
    private final int MIN_KEYS = (ORDER - 1) / 2; // 2
    private final int KEY_SIZE = 30; // Tamanho fixo para a chave (String)
    private long rootAddress;

    // Classe interna para representar um Nó da árvore
    private class Node {
        long address;
        boolean isLeaf;
        int keyCount;
        String[] keys = new String[MAX_KEYS];
        int[] values = new int[MAX_KEYS]; // Para folhas: IDs dos dados
        long[] children = new long[ORDER]; // Para nós internos: ponteiros para outros nós

        Node(long addr) {
            this.address = addr;
        }

        // Lê um nó do ficheiro a partir do seu endereço
        void readFromFile() throws IOException {
            file.seek(address);
            isLeaf = file.readByte() == 1;
            keyCount = file.readInt();
            for (int i = 0; i < MAX_KEYS; i++) {
                byte[] keyBytes = new byte[KEY_SIZE];
                file.read(keyBytes);
                keys[i] = new String(keyBytes, "UTF-8").trim();
                values[i] = file.readInt();
            }
            for (int i = 0; i < ORDER; i++) {
                children[i] = file.readLong();
            }
        }

        // Escreve as informações do nó de volta para o ficheiro no seu endereço
        void writeToFile() throws IOException {
            file.seek(address);
            file.writeByte(isLeaf ? 1 : 0);
            file.writeInt(keyCount);
            for (int i = 0; i < MAX_KEYS; i++) {
                byte[] keyBytes = new byte[KEY_SIZE];
                if (keys[i] != null) {
                    byte[] strBytes = keys[i].getBytes("UTF-8");
                    System.arraycopy(strBytes, 0, keyBytes, 0, Math.min(strBytes.length, KEY_SIZE));
                }
                file.write(keyBytes);
                file.writeInt(values[i]);
            }
            for (int i = 0; i < ORDER; i++) {
                file.writeLong(children[i]);
            }
        }
    }

    public BPlusTree(String filePath) throws IOException {
        this.file = new RandomAccessFile(filePath, "rw");
        if (file.length() == 0) {
            // Ficheiro vazio: cria o cabeçalho e o nó raiz
            this.rootAddress = 8; // O primeiro nó começa após o cabeçalho de 8 bytes
            Node root = new Node(rootAddress);
            root.isLeaf = true;
            root.keyCount = 0;
            root.writeToFile();
            file.seek(0);
            file.writeLong(this.rootAddress); // Escreve o endereço do nó raiz no cabeçalho
        } else {
            // Ficheiro existe: lê o endereço do nó raiz do cabeçalho
            file.seek(0);
            this.rootAddress = file.readLong();
        }
    }

    public void close() throws IOException {
        file.close();
    }

    // Método público para inserir uma chave e valor
    public void insert(String key, int value) throws IOException {
        Node root = new Node(rootAddress);
        root.readFromFile();

        // Se a raiz está cheia, a árvore cresce em altura
        if (root.keyCount == MAX_KEYS) {
            Node newRoot = new Node(file.length()); // Novo nó será a nova raiz
            newRoot.isLeaf = false;
            newRoot.children[0] = root.address;

            splitChild(newRoot, 0, root); // Divide a raiz antiga

            newRoot.writeToFile();
            this.rootAddress = newRoot.address; // Atualiza o endereço da raiz
            file.seek(0);
            file.writeLong(this.rootAddress); // Atualiza o cabeçalho
            insertNonFull(newRoot, key, value);
        } else {
            insertNonFull(root, key, value);
        }
    }

    // Método público para buscar por uma chave
    public List<Integer> search(String key) throws IOException {
        List<Integer> results = new ArrayList<>();
        searchRecursive(rootAddress, key, results);
        return results;
    }

    // Método para listar todos os valores em ordem
    public List<Integer> listAll() throws IOException {
        List<Integer> allValues = new ArrayList<>();
        Node node = new Node(rootAddress);
        node.readFromFile();

        // Encontra a primeira folha (a mais à esquerda)
        while (!node.isLeaf) {
            node = new Node(node.children[0]);
            node.readFromFile();
        }

        // Percorre todas as folhas sequencialmente usando o ponteiro de encadeamento
        while (true) {
            for (int i = 0; i < node.keyCount; i++) {
                allValues.add(node.values[i]);
            }
            long nextNodeAddress = node.children[ORDER - 1];
            if (nextNodeAddress == 0) break; // Chegou ao fim da lista de folhas
            node = new Node(nextNodeAddress);
            node.readFromFile();
        }
        return allValues;
    }

    // Busca recursiva
    private void searchRecursive(long nodeAddress, String key, List<Integer> results) throws IOException {
        Node node = new Node(nodeAddress);
        node.readFromFile();
        int i = 0;
        while (i < node.keyCount && key.compareTo(node.keys[i]) > 0) {
            i++;
        }
        if (node.isLeaf) {
            if (i < node.keyCount && key.equals(node.keys[i])) {
                results.add(node.values[i]);
            }
        } else {
            searchRecursive(node.children[i], key, results);
        }
    }

    // Insere numa folha que não está cheia
    private void insertNonFull(Node node, String key, int value) throws IOException {
        if (node.isLeaf) {
            // Se for folha, insere a chave na posição correta
            int i = node.keyCount - 1;
            while (i >= 0 && key.compareTo(node.keys[i]) < 0) {
                node.keys[i + 1] = node.keys[i];
                node.values[i + 1] = node.values[i];
                i--;
            }
            node.keys[i + 1] = key;
            node.values[i + 1] = value;
            node.keyCount++;
            node.writeToFile();
        } else {
            // Se for nó interno, encontra o filho correto para descer
            int i = node.keyCount - 1;
            while (i >= 0 && key.compareTo(node.keys[i]) < 0) {
                i--;
            }
            i++;
            Node child = new Node(node.children[i]);
            child.readFromFile();

            // Se o filho estiver cheio, divide-o antes de descer
            if (child.keyCount == MAX_KEYS) {
                splitChild(node, i, child);
                if (key.compareTo(node.keys[i]) > 0) {
                    i++;
                }
            }
            Node childToInsert = new Node(node.children[i]);
            childToInsert.readFromFile();
            insertNonFull(childToInsert, key, value);
        }
    }

    // Divide um nó filho 'child' que está cheio
    private void splitChild(Node parent, int childIndex, Node child) throws IOException {
        Node newChild = new Node(file.length());
        newChild.isLeaf = child.isLeaf;

        // A chave do meio do nó 'child' sobe para o 'parent'
        String medianKey = child.keys[MIN_KEYS];
        int medianValue = child.values[MIN_KEYS];

        newChild.keyCount = MAX_KEYS - MIN_KEYS - 1;
        for (int j = 0; j < newChild.keyCount; j++) {
            newChild.keys[j] = child.keys[j + MIN_KEYS + 1];
            newChild.values[j] = child.values[j + MIN_KEYS + 1];
        }

        if (!child.isLeaf) {
            for (int j = 0; j < newChild.keyCount + 1; j++) {
                newChild.children[j] = child.children[j + MIN_KEYS + 1];
            }
        } else {
            newChild.children[ORDER - 1] = child.children[ORDER - 1];
            child.children[ORDER - 1] = newChild.address;
        }

        child.keyCount = MIN_KEYS;

        for (int j = parent.keyCount; j >= childIndex + 1; j--) {
            parent.children[j + 1] = parent.children[j];
        }
        parent.children[childIndex + 1] = newChild.address;

        for (int j = parent.keyCount - 1; j >= childIndex; j--) {
            parent.keys[j + 1] = parent.keys[j];
            parent.values[j+1] = parent.values[j];
        }
        parent.keys[childIndex] = medianKey;
        parent.values[childIndex] = medianValue;
        parent.keyCount++;

        parent.writeToFile();
        child.writeToFile();
        newChild.writeToFile();
    }
}

