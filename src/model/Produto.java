package model;

import java.io.*;

public class Produto implements Register {

    private int id;
    private String nome;
    private String descricao;
    private float preco;
    private int idCategoria;

    public Produto() {
        this.id = -1;
        this.nome = "";
        this.descricao = "";
        this.preco = 0.0f;
        this.idCategoria = -1;
    }

    public Produto(String nome, String descricao, float preco, int idCategoria) {
        this.id = -1;
        this.nome = nome;
        this.descricao = descricao;
        this.preco = preco;
        this.idCategoria = idCategoria;
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

    // ... outros getters e setters ...

    @Override
    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(this.id);
        dos.writeUTF(this.nome);
        dos.writeUTF(this.descricao);
        dos.writeFloat(this.preco);
        dos.writeInt(this.idCategoria);
        return baos.toByteArray();
    }

    @Override
    public void fromByteArray(byte[] byteArray) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(byteArray);
        DataInputStream dis = new DataInputStream(bais);
        this.id = dis.readInt();
        this.nome = dis.readUTF();
        this.descricao = dis.readUTF();
        this.preco = dis.readFloat();
        this.idCategoria = dis.readInt();
    }

    @Override
    public String toString() {
        return "Produto{" +
                "id=" + id +
                ", nome='" + nome + '\'' +
                ", descricao='" + descricao + '\'' +
                ", preco=" + preco +
                ", idCategoria=" + idCategoria +
                '}';
    }

    @Override
    public String getSecondaryKey() {
        // A chave secundária para Produto será o nome
        return this.nome;
    }
}

