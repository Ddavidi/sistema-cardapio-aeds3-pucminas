package model;

import java.io.*;

public class Produto implements Register {

    private int idProduto;
    private String nome;
    private String descricao;
    private int idCategoria;
    private float preco; // Adicionado para compatibilidade

    public Produto() {
        this.idProduto = -1;
        this.nome = "";
        this.descricao = "";
        this.idCategoria = -1;
        this.preco = 0.0f;
    }

    public Produto(String nome, String descricao, int idCategoria, float preco) {
        this.idProduto = -1;
        this.nome = nome;
        this.descricao = descricao;
        this.idCategoria = idCategoria;
        this.preco = preco;
    }

    // Construtor sem preço (mantido para compatibilidade)
    public Produto(String nome, String descricao, int idCategoria) {
        this(nome, descricao, idCategoria, 0.0f);
    }

    @Override
    public int getID() { return this.idProduto; }

    @Override
    public void setID(int id) { this.idProduto = id; }

    @Override
    public String getSecondaryKey() { return this.nome; }

    @Override
    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(this.idProduto);
        dos.writeUTF(this.nome);
        dos.writeUTF(this.descricao);
        dos.writeInt(this.idCategoria);
        dos.writeFloat(this.preco); // Persiste o preço
        return baos.toByteArray();
    }

    @Override
    public void fromByteArray(byte[] byteArray) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(byteArray);
        DataInputStream dis = new DataInputStream(bais);
        this.idProduto = dis.readInt();
        this.nome = dis.readUTF();
        this.descricao = dis.readUTF();
        this.idCategoria = dis.readInt();
        try {
            this.preco = dis.readFloat(); // Tenta ler o preço
        } catch (EOFException e) {
            this.preco = 0.0f; // Se for registro antigo sem preço, assume 0
        }
    }

    // Getters e Setters
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public int getIdCategoria() { return idCategoria; }
    public void setIdCategoria(int idCategoria) { this.idCategoria = idCategoria; }
    public float getPreco() { return preco; }
    public void setPreco(float preco) { this.preco = preco; }

    @Override
    public String toString() {
        return "Produto [ID=" + idProduto + ", Nome='" + nome + "', Descrição='" + descricao + "', Preço=" + preco + ", ID Categoria=" + idCategoria + "]";
    }
}