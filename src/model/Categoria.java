package model;

import java.io.*;

public class Categoria implements Register {

    private int id;
    private String nome;

    public Categoria() {
        this.id = -1;
        this.nome = "";
    }

    public Categoria(String nome) {
        this.id = -1;
        this.nome = nome;
    }

    @Override
    public int getID() {
        return this.id;
    }

    @Override
    public void setID(int id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    @Override
    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(this.id);
        dos.writeUTF(this.nome);
        return baos.toByteArray();
    }

    @Override
    public void fromByteArray(byte[] byteArray) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(byteArray);
        DataInputStream dis = new DataInputStream(bais);
        this.id = dis.readInt();
        this.nome = dis.readUTF();
    }

    @Override
    public String toString() {
        return "Categoria{" +
                "id=" + id +
                ", nome='" + nome + '\'' +
                '}';
    }

    @Override
    public String getSecondaryKey() {
        // A chave secundária para Categoria também será o nome
        return this.nome;
    }
}

