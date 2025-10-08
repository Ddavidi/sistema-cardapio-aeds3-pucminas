package indices;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementação de um Hash Extensível para indexação de dados.
 * Mapeia uma chave (int) para um valor (long).
 *
 * Estrutura dos Arquivos:
 * - Diretório (hash.dir):
 * - 4 bytes: Profundidade Global (pG)
 * - N * 8 bytes: Ponteiros para os baldes (long)
 *
 * - Baldes (hash.bkt):
 * - N blocos, onde cada bloco (balde) contém:
 * - 4 bytes: Profundidade Local (pL)
 * - 4 bytes: Quantidade de chaves no balde
 * - M * 12 bytes: Pares de (chave, valor) -> (int, long)
 */
public class ExtensibleHash {

    private final String dirPath;
    private final String bucketPath;
    private RandomAccessFile dirFile;
    private RandomAccessFile bucketFile;
    private int globalDepth;
    private final int BUCKET_SIZE = 4; // Quantidade de pares (chave, valor) por balde

    // Classe interna para representar um Balde (Bucket)
    private class Bucket {
        int localDepth;
        int count;
        int[] keys;
        long[] values;
        long filePointer; // Posição do balde no arquivo

        Bucket(long pointer) {
            this.localDepth = globalDepth;
            this.count = 0;
            this.keys = new int[BUCKET_SIZE];
            this.values = new long[BUCKET_SIZE];
            this.filePointer = pointer;
        }

        // Lê um balde do arquivo
        void readFromFile() throws IOException {
            bucketFile.seek(filePointer);
            localDepth = bucketFile.readInt();
            count = bucketFile.readInt();
            for (int i = 0; i < BUCKET_SIZE; i++) {
                keys[i] = bucketFile.readInt();
                values[i] = bucketFile.readLong();
            }
        }

        // Escreve o balde de volta no arquivo
        void writeToFile() throws IOException {
            bucketFile.seek(filePointer);
            bucketFile.writeInt(localDepth);
            bucketFile.writeInt(count);
            for (int i = 0; i < BUCKET_SIZE; i++) {
                bucketFile.writeInt(keys[i]);
                bucketFile.writeLong(values[i]);
            }
        }

        boolean isFull() {
            return count == BUCKET_SIZE;
        }

        // Insere um par chave-valor no balde
        void insert(int key, long value) {
            if (!isFull()) {
                keys[count] = key;
                values[count] = value;
                count++;
            }
        }

        // Remove uma chave do balde
        boolean delete(int key) {
            for (int i = 0; i < count; i++) {
                if (keys[i] == key) {
                    // Move o último elemento para a posição do removido
                    keys[i] = keys[count - 1];
                    values[i] = values[count - 1];
                    count--;
                    return true;
                }
            }
            return false;
        }
    }

    public ExtensibleHash(String dirPath, String bucketPath) throws IOException {
        this.dirPath = dirPath;
        this.bucketPath = bucketPath;
        this.dirFile = new RandomAccessFile(this.dirPath, "rw");
        this.bucketFile = new RandomAccessFile(this.bucketPath, "rw");

        if (dirFile.length() == 0) {
            // Inicializa o diretório e o primeiro balde
            this.globalDepth = 1;
            dirFile.writeInt(this.globalDepth);

            long bucketPointer = bucketFile.length();
            Bucket b = new Bucket(bucketPointer);
            b.localDepth = 1;
            b.writeToFile();

            dirFile.writeLong(bucketPointer);
            dirFile.writeLong(bucketPointer); // Ambos os ponteiros iniciais apontam para o mesmo balde
        } else {
            // Carrega a profundidade global
            dirFile.seek(0);
            this.globalDepth = dirFile.readInt();
        }
    }

    private int hash(int key) {
        return key % (1 << globalDepth); // Retorna os pG bits menos significativos
    }

    // Busca o valor associado a uma chave
    public long search(int key) throws IOException {
        int hashValue = hash(key);
        dirFile.seek(4 + (long) hashValue * 8);
        long bucketPointer = dirFile.readLong();

        Bucket b = new Bucket(bucketPointer);
        b.readFromFile();

        for (int i = 0; i < b.count; i++) {
            if (b.keys[i] == key) {
                return b.values[i];
            }
        }
        return -1; // Não encontrado
    }

    // Insere um novo par (chave, valor)
    public void insert(int key, long value) throws IOException {
        int hashValue = hash(key);
        dirFile.seek(4 + (long) hashValue * 8);
        long bucketPointer = dirFile.readLong();

        Bucket b = new Bucket(bucketPointer);
        b.readFromFile();

        if (!b.isFull()) {
            b.insert(key, value);
            b.writeToFile();
        } else {
            // Balde cheio, precisa dividir
            if (b.localDepth == globalDepth) {
                // Duplica o diretório
                duplicateDirectory();
            }

            // Cria um novo balde
            long newBucketPointer = bucketFile.length();
            Bucket newBucket = new Bucket(newBucketPointer);
            newBucket.localDepth = b.localDepth + 1;

            // Redistribui as chaves
            List<Integer> tempKeys = new ArrayList<>();
            List<Long> tempValues = new ArrayList<>();
            for(int i=0; i<b.count; i++){
                tempKeys.add(b.keys[i]);
                tempValues.add(b.values[i]);
            }
            tempKeys.add(key);
            tempValues.add(value);

            b.count = 0;
            b.localDepth++;

            for(int i=0; i<tempKeys.size(); i++){
                int currentKey = tempKeys.get(i);
                long currentValue = tempValues.get(i);
                int newHash = currentKey % (1 << globalDepth);

                if((newHash & (1 << (b.localDepth - 1))) == 0){
                    b.insert(currentKey, currentValue);
                } else {
                    newBucket.insert(currentKey, currentValue);
                }
            }

            b.writeToFile();
            newBucket.writeToFile();

            // Reorganiza os ponteiros do diretório
            int mask = (1 << b.localDepth) - 1;
            int prefix = hashValue & mask;

            for(int i=0; i < (1 << globalDepth); i++){
                if((i & mask) == prefix){
                    if ((i & (1 << (b.localDepth - 1))) != 0) {
                        dirFile.seek(4 + (long)i * 8);
                        dirFile.writeLong(newBucketPointer);
                    }
                }
            }
        }
    }

    // Deleta um par (chave, valor)
    public boolean delete(int key) throws IOException {
        long bucketPointer = searchBucketPointer(key);
        if(bucketPointer == -1) return false;

        Bucket b = new Bucket(bucketPointer);
        b.readFromFile();

        if (b.delete(key)) {
            b.writeToFile();
            // Lógica de merge de baldes poderia ser implementada aqui
            return true;
        }
        return false;
    }

    private void duplicateDirectory() throws IOException {
        dirFile.seek(4);
        int oldSize = 1 << globalDepth;
        long[] oldPointers = new long[oldSize];
        for(int i=0; i<oldSize; i++){
            oldPointers[i] = dirFile.readLong();
        }

        globalDepth++;
        dirFile.seek(0);
        dirFile.writeInt(globalDepth);

        for(int i=0; i<oldSize * 2; i++){
            dirFile.writeLong(oldPointers[i % oldSize]);
        }
    }

    private long searchBucketPointer(int key) throws IOException{
        int hashValue = hash(key);
        dirFile.seek(4 + (long) hashValue * 8);
        return dirFile.readLong();
    }
}
