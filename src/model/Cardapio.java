package model;

import java.io.*;

/**
 * Classe que representa a entidade Cardapio.
 */
public class Cardapio implements Register {

    private int idCardapio;
    private String nome;
    private String descricao;
    private boolean ativo;

    public Cardapio() {
        this.idCardapio = -1;
        this.nome = "";
        this.descricao = "";
        this.ativo = false;
    }

    public Cardapio(String nome, String descricao, boolean ativo) {
        this.idCardapio = -1;
        this.nome = nome;
        this.descricao = descricao;
        this.ativo = ativo;
    }

    // --- MÉTODOS DA INTERFACE REGISTER ---

    @Override
    public int getID() { return this.idCardapio; }

    @Override
    public void setID(int id) { this.idCardapio = id; }

    @Override
    public String getSecondaryKey() { return this.nome; }

    @Override
    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(this.idCardapio);
        dos.writeUTF(this.nome);
        dos.writeUTF(this.descricao);
        dos.writeBoolean(this.ativo);
        return baos.toByteArray();
    }

    @Override
    public void fromByteArray(byte[] byteArray) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(byteArray);
        DataInputStream dis = new DataInputStream(bais);
        this.idCardapio = dis.readInt();
        this.nome = dis.readUTF();
        this.descricao = dis.readUTF();
        this.ativo = dis.readBoolean();
    }

    // --- GETTERS E SETTERS ---

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }

    @Override
    public String toString() {
        return "Cardapio [ID=" + idCardapio + ", Nome='" + nome + "', Descrição='" + descricao + "', Ativo=" + ativo + "]";
    }
}

