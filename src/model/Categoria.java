package model;

import java.io.*;

/**
 * Classe que representa a entidade Categoria.
 */
public class Categoria implements Register {

    private int idCategoria;
    private String nome;

    public Categoria() {
        this.idCategoria = -1;
        this.nome = "";
    }

    public Categoria(String nome) {
        this.idCategoria = -1;
        this.nome = nome;
    }

    // --- MÉTODOS DA INTERFACE REGISTER ---

    @Override
    public int getID() { return this.idCategoria; }

    @Override
    public void setID(int id) { this.idCategoria = id; }

    @Override
    public String getSecondaryKey() { return this.nome; }

    @Override
    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(this.idCategoria);
        dos.writeUTF(this.nome);
        return baos.toByteArray();
    }

    @Override
    public void fromByteArray(byte[] byteArray) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(byteArray);
        DataInputStream dis = new DataInputStream(bais);
        this.idCategoria = dis.readInt();
        this.nome = dis.readUTF();
    }

    // --- GETTERS E SETTERS ---

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    @Override
    public String toString() {
        return "Categoria [ID=" + idCategoria + ", Nome='" + nome + "']";
    }
}

