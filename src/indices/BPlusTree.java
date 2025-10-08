package indices;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Implementação de uma Árvore B+ para indexação de chaves secundárias.
 * Mapeia uma chave (String) para um valor (int - o ID do registo).
 *
 * Estrutura do Ficheiro de Índice:
 * - 8 bytes: Endereço do nó raiz
 * - Nós:
 * - 1 byte: É folha? (1 para sim, 0 para não)
 * - 4 bytes: Número de chaves no nó
 * - N * (TAMANHO_CHAVE + 4): Chave, ID do Dado (para folhas)
 * - (N+1) * 8: Ponteiros para filhos (para nós internos)
 */
public class BPlusTree {

    private final RandomAccessFile file;
    private final int ORDER = 5; // Ordem da árvore (número máximo de filhos de um nó interno)
    private final int MAX_KEYS = ORDER - 1; // Máximo de chaves num nó
    private final int MIN_KEYS = (ORDER - 1) / 2; // Mínimo de chaves num nó
    private final int KEY_SIZE = 30; // Tamanho fixo em bytes para a chave (String)
    private long rootAddress;

    // Classe interna para representar um Nó da Árvore B+
    private class Node {
        long address;       // Endereço deste nó no ficheiro
        boolean isLeaf;     // Se o nó é uma folha
        int keyCount;       // Número de chaves atualmente no nó
        String[] keys = new String[MAX_KEYS];
        int[] values = new int[MAX_KEYS];   // Para folhas: IDs dos dados
        long[] children = new long[ORDER];    // Para nós internos: ponteiros para outros nós

        Node(long addr) {
            this.address = addr;
        }

        // Lê os dados de um nó a partir do ficheiro
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

        // Escreve os dados de um nó para o ficheiro
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
            // Se o ficheiro é novo, cria o nó raiz como uma folha.
            // O cabeçalho (ponteiro da raiz) ocupa 8 bytes. O primeiro nó começa no endereço 8.
            Node root = new Node(8); // CORREÇÃO: Endereço inicial alterado de 4 para 8
            root.isLeaf = true;
            root.keyCount = 0;
            root.writeToFile();
            this.rootAddress = root.address;
            file.seek(0);
            file.writeLong(this.rootAddress);
        } else {
            // Se o ficheiro já existe, apenas lê o endereço do nó raiz.
            file.seek(0);
            this.rootAddress = file.readLong();
        }
    }

    // Método público para inserir uma chave e um valor
    public void insert(String key, int value) throws IOException {
        Node root = new Node(rootAddress);
        root.readFromFile();
        if (root.keyCount == MAX_KEYS) {
            // Se a raiz está cheia, ela precisa de ser dividida.
            // A nova raiz será um nó interno.
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

    // Método público para listar todos os valores em ordem de chave
    public List<Integer> listAll() throws IOException {
        List<Integer> allValues = new ArrayList<>();
        Node node = new Node(rootAddress);
        node.readFromFile();

        // Encontra a primeira folha (a mais à esquerda)
        while (!node.isLeaf) {
            node = new Node(node.children[0]);
            node.readFromFile();
        }

        // Percorre todas as folhas sequencialmente usando o ponteiro para o irmão
        while (true) {
            for (int i = 0; i < node.keyCount; i++) {
                allValues.add(node.values[i]);
            }
            // O último ponteiro de uma folha aponta para a próxima folha
            long nextNodeAddress = node.children[ORDER - 1];
            if (nextNodeAddress == 0) break; // Não há mais folhas
            node = new Node(nextNodeAddress);
            node.readFromFile();
        }
        return allValues;
    }

    // Insere numa folha que não está cheia
    private void insertNonFull(Node node, String key, int value) throws IOException {
        if (node.isLeaf) {
            // Se for uma folha, encontra a posição e insere a chave/valor
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
            // Se for um nó interno, encontra o filho correto para descer na árvore
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
                // Decide para qual dos novos filhos descer
                if (key.compareTo(node.keys[i]) > 0) {
                    i++;
                }
            }
            Node childToInsert = new Node(node.children[i]);
            childToInsert.readFromFile();
            insertNonFull(childToInsert, key, value);
        }
    }

    // Divide um filho cheio de um pai
    private void splitChild(Node parent, int childIndex, Node child) throws IOException {
        Node newChild = new Node(file.length());
        newChild.isLeaf = child.isLeaf;

        // A chave do meio do filho cheio sobe para o pai
        String keyToMoveUp = child.keys[MIN_KEYS];

        // O novo filho (newChild) recebe a segunda metade das chaves do filho cheio
        newChild.keyCount = MIN_KEYS;
        for (int j = 0; j < MIN_KEYS; j++) {
            newChild.keys[j] = child.keys[j + MIN_KEYS + 1];
            newChild.values[j] = child.values[j + MIN_KEYS + 1];
        }

        if (!child.isLeaf) {
            for (int j = 0; j < MIN_KEYS + 1; j++) {
                newChild.children[j] = child.children[j + MIN_KEYS + 1];
            }
        } else {
            // Se for uma folha, encadeia as folhas para permitir a travessia sequencial
            newChild.children[ORDER-1] = child.children[ORDER-1];
            child.children[ORDER-1] = newChild.address;
        }

        // O filho original (child) agora tem apenas a primeira metade das chaves
        child.keyCount = MIN_KEYS;

        // Reorganiza os ponteiros dos filhos no pai
        for (int j = parent.keyCount; j >= childIndex + 1; j--) {
            parent.children[j + 1] = parent.children[j];
        }
        parent.children[childIndex + 1] = newChild.address;

        // Reorganiza as chaves no pai e insere a chave que subiu
        for (int j = parent.keyCount - 1; j >= childIndex; j--) {
            parent.keys[j + 1] = parent.keys[j];
        }
        parent.keys[childIndex] = keyToMoveUp;
        parent.keyCount++;

        // Escreve todas as alterações nos ficheiros
        parent.writeToFile();
        child.writeToFile();
        newChild.writeToFile();
    }
}

