package indices;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementação de um Hash Extensível para indexação de chaves primárias.
 * Mapeia um ID (int) para a sua posição no ficheiro de dados (long).
 */
public class ExtensibleHash {
    private final RandomAccessFile directoryFile;
    private final RandomAccessFile bucketsFile;
    private int globalDepth;
    private final int BUCKET_SIZE = 4;

    private class Bucket {
        long address;
        int localDepth;
        int count;
        int[] keys = new int[BUCKET_SIZE];
        long[] values = new long[BUCKET_SIZE];

        Bucket(long addr, int depth) {
            this.address = addr;
            this.localDepth = depth;
            this.count = 0;
        }

        void readFromFile() throws IOException {
            bucketsFile.seek(address);
            localDepth = bucketsFile.readInt();
            count = bucketsFile.readInt();
            for (int i = 0; i < BUCKET_SIZE; i++) {
                keys[i] = bucketsFile.readInt();
                values[i] = bucketsFile.readLong();
            }
        }

        void writeToFile() throws IOException {
            bucketsFile.seek(address);
            bucketsFile.writeInt(localDepth);
            bucketsFile.writeInt(count);
            for (int i = 0; i < BUCKET_SIZE; i++) {
                bucketsFile.writeInt(keys[i]);
                bucketsFile.writeLong(values[i]);
            }
        }
    }

    public ExtensibleHash(String dirPath, String buckPath) throws IOException {
        this.directoryFile = new RandomAccessFile(dirPath, "rw");
        this.bucketsFile = new RandomAccessFile(buckPath, "rw");

        if (directoryFile.length() == 0) {
            globalDepth = 1;
            directoryFile.writeInt(globalDepth);

            Bucket b1 = new Bucket(0, 1);
            b1.writeToFile();
            Bucket b2 = new Bucket(bucketsFile.length(), 1);
            b2.writeToFile();

            directoryFile.writeLong(b1.address);
            directoryFile.writeLong(b2.address);
        } else {
            directoryFile.seek(0);
            globalDepth = directoryFile.readInt();
        }
    }

    public void close() throws IOException {
        directoryFile.close();
        bucketsFile.close();
    }

    // --- MÉTODOS PÚBLICOS ---

    public void insert(int key, long value) throws IOException {
        Bucket b = findBucket(key);

        // Verifica se a chave já existe para evitar duplicados
        for(int i=0; i < b.count; i++) {
            if(b.keys[i] == key) {
                // Idealmente, lançaria uma exceção de chave duplicada
                return;
            }
        }

        if (b.count < BUCKET_SIZE) {
            b.keys[b.count] = key;
            b.values[b.count] = value;
            b.count++;
            b.writeToFile();
        } else {
            splitBucket(b);
            insert(key, value); // Tenta a inserção novamente após a divisão
        }
    }

    /**
     * NOVO MÉTODO: Atualiza o ponteiro de uma chave existente.
     * @param key A chave (ID) a ser atualizada.
     * @param value O novo ponteiro (posição no ficheiro de dados).
     * @return true se a chave foi encontrada e atualizada, false caso contrário.
     */
    public boolean update(int key, long value) throws IOException {
        Bucket b = findBucket(key);
        for (int i = 0; i < b.count; i++) {
            if (b.keys[i] == key) {
                b.values[i] = value;
                b.writeToFile();
                return true;
            }
        }
        return false;
    }

    public boolean delete(int key) throws IOException {
        Bucket b = findBucket(key);
        for (int i = 0; i < b.count; i++) {
            if (b.keys[i] == key) {
                b.keys[i] = b.keys[b.count - 1];
                b.values[i] = b.values[b.count - 1];
                b.count--;
                b.writeToFile();
                return true;
            }
        }
        return false;
    }

    public long search(int key) throws IOException {
        Bucket b = findBucket(key);
        for (int i = 0; i < b.count; i++) {
            if (b.keys[i] == key) {
                return b.values[i];
            }
        }
        return -1;
    }

    // --- MÉTODOS AUXILIARES ---

    private Bucket findBucket(int key) throws IOException {
        int hash = key & ((1 << globalDepth) - 1);
        directoryFile.seek(4 + (long) hash * 8);
        long bucketAddress = directoryFile.readLong();

        Bucket b = new Bucket(bucketAddress, 0);
        b.readFromFile();
        return b;
    }

    private void splitBucket(Bucket b) throws IOException {
        if (b.localDepth == globalDepth) {
            doubleDirectory();
        }

        b.localDepth++;
        Bucket newB = new Bucket(bucketsFile.length(), b.localDepth);

        List<Integer> tempKeys = new ArrayList<>();
        List<Long> tempValues = new ArrayList<>();
        for(int i=0; i<b.count; i++) {
            tempKeys.add(b.keys[i]);
            tempValues.add(b.values[i]);
        }

        b.count = 0;
        newB.count = 0;

        for(int i=0; i<tempKeys.size(); i++){
            redistributeEntry(b, newB, tempKeys.get(i), tempValues.get(i));
        }

        b.writeToFile();
        newB.writeToFile();

        updateDirectoryAfterSplit(b, newB);
    }

    private void doubleDirectory() throws IOException {
        long oldDirSize = 1L << globalDepth;
        long[] oldDirPointers = new long[(int)oldDirSize];
        directoryFile.seek(4);
        for(int i=0; i<oldDirSize; i++) {
            oldDirPointers[i] = directoryFile.readLong();
        }

        globalDepth++;
        directoryFile.seek(0);
        directoryFile.writeInt(globalDepth);

        for(int i=0; i < oldDirPointers.length * 2; i++) {
            directoryFile.writeLong(oldDirPointers[i/2]);
        }
    }

    private void redistributeEntry(Bucket b1, Bucket b2, int key, long value) {
        int hash = key & ((1 << b1.localDepth) - 1);
        int originalBucketHash = getBucketHash(b1);

        if (hash == originalBucketHash) {
            b1.keys[b1.count] = key;
            b1.values[b1.count] = value;
            b1.count++;
        } else {
            b2.keys[b2.count] = key;
            b2.values[b2.count] = value;
            b2.count++;
        }
    }

    private void updateDirectoryAfterSplit(Bucket b1, Bucket b2) throws IOException {
        int hash1 = getBucketHash(b1);
        int hash2 = getBucketHash(b2);

        long dirSize = 1L << globalDepth;
        for(int i=0; i < dirSize; i++){
            int currentHash = i & ((1 << b1.localDepth) - 1);
            if (currentHash == hash1) {
                directoryFile.seek(4 + (long) i * 8);
                directoryFile.writeLong(b1.address);
            } else if(currentHash == hash2) {
                directoryFile.seek(4 + (long) i * 8);
                directoryFile.writeLong(b2.address);
            }
        }
    }

    private int getBucketHash(Bucket b) {
        // A forma como as chaves são distribuídas depende da sua profundidade local.
        // O primeiro ponteiro no diretório que aponta para este balde pode ser usado para derivar o seu "hash"
        for (int i=0; i < (1 << globalDepth); i++) {
            try {
                directoryFile.seek(4 + (long)i * 8);
                if (directoryFile.readLong() == b.address) {
                    return i & ((1 << b.localDepth) - 1);
                }
            } catch(IOException e) { /* Ignora */ }
        }
        return -1; // Não deveria acontecer
    }
}

