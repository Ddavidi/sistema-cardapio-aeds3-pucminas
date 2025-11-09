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

        String baseName = dbFilePath.replace(".db", "");
        this.hash = new ExtensibleHash(baseName + ".hash.dir", baseName + ".hash.bkt");

        if(useBPlusTree) {
            this.bPlusTree = new BPlusTree(baseName + ".bptree.idx");
        } else {
            this.bPlusTree = null;
        }

        if (dbFile.length() == 0) {
            dbFile.writeInt(0);
        }
    }

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

        dbFile.writeByte(0);
        dbFile.writeInt(byteArray.length);
        dbFile.write(byteArray);

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
        if (lapide == 1) return null;

        int tamanho = dbFile.readInt();
        byte[] byteArray = new byte[tamanho];
        dbFile.read(byteArray);

        T obj = constructor.newInstance();
        obj.fromByteArray(byteArray);
        return obj;
    }

    public boolean update(T obj) throws Exception {
        T oldObj = read(obj.getID());
        if (oldObj == null) return false;

        String oldSecondaryKey = (bPlusTree != null) ? oldObj.getSecondaryKey() : null;

        long posicao = hash.search(obj.getID());
        byte[] novoByteArray = obj.toByteArray();

        dbFile.seek(posicao);
        dbFile.readByte();
        int tamanhoAntigo = dbFile.readInt();

        if (novoByteArray.length <= tamanhoAntigo) {
            dbFile.seek(posicao + 5);
            dbFile.write(novoByteArray);
        } else {
            dbFile.seek(posicao);
            dbFile.writeByte(1);

            dbFile.seek(dbFile.length());
            long novaPosicao = dbFile.getFilePointer();
            dbFile.writeByte(0);
            dbFile.writeInt(novoByteArray.length);
            dbFile.write(novoByteArray);

            hash.update(obj.getID(), novaPosicao);
        }

        if (bPlusTree != null) {
            String newSecondaryKey = obj.getSecondaryKey();
            if (oldSecondaryKey != null && !oldSecondaryKey.equals(newSecondaryKey)) {
                bPlusTree.delete(oldSecondaryKey);
                bPlusTree.insert(newSecondaryKey, obj.getID());
            }
        }
        return true;
    }

    public boolean delete(int id) throws Exception {
        T obj = read(id);
        if (obj == null) return false;

        long posicao = hash.search(id);
        dbFile.seek(posicao);
        dbFile.writeByte(1);

        hash.delete(id);
        if (bPlusTree != null) {
            bPlusTree.delete(obj.getSecondaryKey());
        }

        return true;
    }

    public List<T> listAll() throws Exception {
        List<T> lista = new ArrayList<>();
        dbFile.seek(4);
        while (dbFile.getFilePointer() < dbFile.length()) {
            byte lapide = dbFile.readByte();
            int tamanho = dbFile.readInt();

            if (lapide == 0) {
                byte[] byteArray = new byte[tamanho];
                dbFile.read(byteArray);
                T obj = constructor.newInstance();
                obj.fromByteArray(byteArray);
                lista.add(obj);
            } else {
                dbFile.skipBytes(tamanho);
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

    public List<T> listAllBySecondaryKeyPrefix(String prefix) throws Exception {
        if (bPlusTree == null) {
            throw new UnsupportedOperationException("A Árvore B+ não está habilitada para esta entidade.");
        }
        List<T> listaOrdenada = new ArrayList<>();
        List<Integer> idsOrdenados = bPlusTree.searchByPrefix(prefix);

        for (int id : idsOrdenados) {
            T obj = read(id);
            if(obj != null) {
                listaOrdenada.add(obj);
            }
        }
        return listaOrdenada;
    }
}