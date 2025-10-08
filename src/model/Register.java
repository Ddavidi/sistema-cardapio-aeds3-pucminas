package model;

import java.io.IOException;

/**
 * Interface que define os métodos essenciais para qualquer entidade
 * que será persistida em arquivo.
 */
public interface Register {

    int getID();
    void setID(int id);

    /**
     * Serializa o objeto para um array de bytes.
     * @return O array de bytes representando o objeto.
     * @throws IOException
     */
    byte[] toByteArray() throws IOException;

    /**
     * Deserializa um array de bytes para preencher os atributos do objeto.
     * @param byteArray O array de bytes a ser lido.
     * @throws IOException
     */
    void fromByteArray(byte[] byteArray) throws IOException;

    /**
     * Retorna a chave secundária do objeto (ex: nome, email),
     * usada para indexação na Árvore B+.
     * @return A String que representa a chave secundária.
     */
    String getSecondaryKey();
}

