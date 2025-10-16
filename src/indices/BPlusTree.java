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
    private final int ORDER = 5;
    private final int MAX_KEYS = ORDER - 1;
    private final int MIN_KEYS = (ORDER - 1) / 2;
    private final int KEY_SIZE = 30;
    private long rootAddress;

    private class Node {
        long address;
        boolean isLeaf;
        int keyCount;
        String[] keys = new String[MAX_KEYS];
        int[] values = new int[MAX_KEYS];
        long[] children = new long[ORDER];

        Node(long addr) { this.address = addr; }

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
            this.rootAddress = 8;
            Node root = new Node(rootAddress);
            root.isLeaf = true;
            root.keyCount = 0;
            root.writeToFile();
            file.seek(0);
            file.writeLong(this.rootAddress);
        } else {
            file.seek(0);
            this.rootAddress = file.readLong();
        }
    }

    public void close() throws IOException {
        file.close();
    }

    // --- MÉTODOS PÚBLICOS ---

    public void insert(String key, int value) throws IOException {
        // (Este método permanece igual)
        Node root = new Node(rootAddress);
        root.readFromFile();
        if (root.keyCount == MAX_KEYS) {
            Node newRoot = new Node(file.length());
            newRoot.isLeaf = false;
            newRoot.children[0] = root.address;
            splitChild(newRoot, 0, root);
            newRoot.writeToFile();
            this.rootAddress = newRoot.address;
            file.seek(0);
            file.writeLong(this.rootAddress);
            insertNonFull(newRoot, key, value);
        } else {
            insertNonFull(root, key, value);
        }
    }

    /**
     * @param key A chave (nome) a ser removida.
     * @return true se a chave foi encontrada e removida, false caso contrário.
     */
    public boolean delete(String key) throws IOException {
        return deleteRecursive(rootAddress, key);
    }

    private boolean deleteRecursive(long nodeAddress, String key) throws IOException {
        Node node = new Node(nodeAddress);
        node.readFromFile();

        int i = 0;
        while (i < node.keyCount && key.compareTo(node.keys[i]) > 0) {
            i++;
        }

        if (node.isLeaf) {
            if (i < node.keyCount && key.equals(node.keys[i])) {
                // Chave encontrada, vamos removê-la
                for (int j = i; j < node.keyCount - 1; j++) {
                    node.keys[j] = node.keys[j + 1];
                    node.values[j] = node.values[j + 1];
                }
                node.keyCount--;
                node.writeToFile();
                return true;
            }
            return false; // Chave não encontrada na folha
        } else {
            // Continua a busca no filho apropriado
            return deleteRecursive(node.children[i], key);
        }
    }

    public List<Integer> search(String key) throws IOException {
        // (Este método permanece igual)
        List<Integer> results = new ArrayList<>();
        searchRecursive(rootAddress, key, results);
        return results;
    }

    public List<Integer> listAll() throws IOException {
        // (Este método permanece igual)
        List<Integer> allValues = new ArrayList<>();
        Node node = new Node(rootAddress);
        node.readFromFile();
        while (!node.isLeaf) {
            node = new Node(node.children[0]);
            node.readFromFile();
        }
        while (true) {
            for (int i = 0; i < node.keyCount; i++) {
                allValues.add(node.values[i]);
            }
            long nextNodeAddress = node.children[ORDER - 1];
            if (nextNodeAddress == 0) break;
            node = new Node(nextNodeAddress);
            node.readFromFile();
        }
        return allValues;
    }

    // --- MÉTODOS AUXILIARES ---

    private void searchRecursive(long nodeAddress, String key, List<Integer> results) throws IOException {
        // (Este método permanece igual)
        Node node = new Node(nodeAddress);
        node.readFromFile();
        int i = 0;
        while (i < node.keyCount && key.compareTo(node.keys[i]) > 0) i++;
        if (node.isLeaf) {
            if (i < node.keyCount && key.equals(node.keys[i])) results.add(node.values[i]);
        } else {
            searchRecursive(node.children[i], key, results);
        }
    }

    private void insertNonFull(Node node, String key, int value) throws IOException {
        // (Este método permanece igual)
        if (node.isLeaf) {
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
            int i = node.keyCount - 1;
            while (i >= 0 && key.compareTo(node.keys[i]) < 0) i--;
            i++;
            Node child = new Node(node.children[i]);
            child.readFromFile();
            if (child.keyCount == MAX_KEYS) {
                splitChild(node, i, child);
                if (key.compareTo(node.keys[i]) > 0) i++;
            }
            Node childToInsert = new Node(node.children[i]);
            childToInsert.readFromFile();
            insertNonFull(childToInsert, key, value);
        }
    }

    private void splitChild(Node parent, int childIndex, Node child) throws IOException {
        // (Este método permanece igual)
        Node newChild = new Node(file.length());
        newChild.isLeaf = child.isLeaf;
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

