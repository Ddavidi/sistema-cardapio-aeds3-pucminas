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
    private final int BUCKET_SIZE = 4; // Quantos pares (chave, valor) cabem num balde

    // Classe interna para representar um Balde (Bucket)
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
            // Inicializa o diretório e o primeiro balde
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

    public void insert(int key, long value) throws IOException {
        int hash = key & ((1 << globalDepth) - 1);
        directoryFile.seek(4 + (long) hash * 8);
        long bucketAddress = directoryFile.readLong();

        Bucket b = new Bucket(bucketAddress, 0);
        b.readFromFile();

        if (b.count < BUCKET_SIZE) {
            b.keys[b.count] = key;
            b.values[b.count] = value;
            b.count++;
            b.writeToFile();
        } else {
            // Balde cheio, precisa de dividir
            if (b.localDepth == globalDepth) {
                // Duplica o diretório
                globalDepth++;
                directoryFile.seek(0);
                directoryFile.writeInt(globalDepth);
                long newDirSize = (long) (1 << globalDepth);
                long[] oldDir = new long[1 << (globalDepth - 1)];
                directoryFile.seek(4);
                for(int i=0; i < oldDir.length; i++) {
                    oldDir[i] = directoryFile.readLong();
                }
                directoryFile.seek(4);
                for(int i=0; i<newDirSize; i++) {
                    directoryFile.writeLong(oldDir[i/2]);
                }
            }

            // Divide o balde
            b.localDepth++;
            Bucket newB = new Bucket(bucketsFile.length(), b.localDepth);

            // Redistribui as chaves
            List<Integer> tempKeys = new ArrayList<>();
            List<Long> tempValues = new ArrayList<>();
            for(int i=0; i<b.count; i++) {
                tempKeys.add(b.keys[i]);
                tempValues.add(b.values[i]);
            }
            tempKeys.add(key);
            tempValues.add(value);

            b.count = 0;
            for(int i=0; i<tempKeys.size(); i++){
                int currentKey = tempKeys.get(i);
                long currentValue = tempValues.get(i);
                int newHash = currentKey & ((1 << b.localDepth) - 1);

                if(newHash == (key & ((1 << b.localDepth) - 1))){
                    b.keys[b.count] = currentKey;
                    b.values[b.count] = currentValue;
                    b.count++;
                } else {
                    newB.keys[newB.count] = currentKey;
                    newB.values[newB.count] = currentValue;
                    newB.count++;
                }
            }

            b.writeToFile();
            newB.writeToFile();

            // Atualiza os ponteiros no diretório
            for(int i=0; i < (1 << globalDepth); i++){
                if((i & ((1 << b.localDepth) - 1)) == (key & ((1 << b.localDepth) - 1))){
                    directoryFile.seek(4 + (long) i * 8);
                    directoryFile.writeLong(b.address);
                } else if ((i & ((1 << newB.localDepth) - 1)) == (key & ((1 << newB.localDepth) - 1))){
                    directoryFile.seek(4 + (long) i * 8);
                    directoryFile.writeLong(newB.address);
                }
            }
        }
    }

    public long search(int key) throws IOException {
        int hash = key & ((1 << globalDepth) - 1);
        directoryFile.seek(4 + (long) hash * 8);
        long bucketAddress = directoryFile.readLong();

        Bucket b = new Bucket(bucketAddress, 0);
        b.readFromFile();

        for (int i = 0; i < b.count; i++) {
            if (b.keys[i] == key) {
                return b.values[i];
            }
        }
        return -1; // Não encontrado
    }
}

