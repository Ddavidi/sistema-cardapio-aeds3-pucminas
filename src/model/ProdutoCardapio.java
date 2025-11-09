package model;

import java.io.*;

/**
 * Classe que representa a entidade associativa ProdutoCardapio.
 * Implementa o relacionamento N:N entre Produto e Cardapio.
 */
public class ProdutoCardapio implements Register {

    private int idProdutoCardapio;
    private int idProduto;
    private int idCardapio;
    private float preco;

    public ProdutoCardapio() {
        this.idProdutoCardapio = -1;
        this.idProduto = -1;
        this.idCardapio = -1;
        this.preco = 0.0f;
    }

    public ProdutoCardapio(int idProduto, int idCardapio, float preco) {
        this.idProdutoCardapio = -1;
        this.idProduto = idProduto;
        this.idCardapio = idCardapio;
        this.preco = preco;
    }

    // --- MÉTODOS DA INTERFACE REGISTER ---

    @Override
    public int getID() { return this.idProdutoCardapio; }

    @Override
    public void setID(int id) { this.idProdutoCardapio = id; }

    /**
     * Retorna uma chave secundária composta, formatada como "idCardapio-idProduto".
     * O padding (ex: %010d) garante que as chaves sejam ordenadas lexicograficamente
     * de forma correta (ex: "10-" vem depois de "9-").
     * Isto permite buscas por prefixo na Árvore B+ (ex: "todos os produtos do cardápio 10").
     * @return A chave secundária composta.
     */
    @Override
    public String getSecondaryKey() {
        return String.format("%010d-%010d", this.idCardapio, this.idProduto);
    }

    @Override
    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(this.idProdutoCardapio);
        dos.writeInt(this.idProduto);
        dos.writeInt(this.idCardapio);
        dos.writeFloat(this.preco);
        return baos.toByteArray();
    }

    @Override
    public void fromByteArray(byte[] byteArray) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(byteArray);
        DataInputStream dis = new DataInputStream(bais);
        this.idProdutoCardapio = dis.readInt();
        this.idProduto = dis.readInt();
        this.idCardapio = dis.readInt();
        this.preco = dis.readFloat();
    }

    // --- GETTERS E SETTERS ---

    public int getIdProduto() { return idProduto; }
    public void setIdProduto(int idProduto) { this.idProduto = idProduto; }
    public int getIdCardapio() { return idCardapio; }
    public void setIdCardapio(int idCardapio) { this.idCardapio = idCardapio; }
    public float getPreco() { return preco; }
    public void setPreco(float preco) { this.preco = preco; }

    @Override
    public String toString() {
        return "ProdutoCardapio [ID=" + idProdutoCardapio + ", ID Produto=" + idProduto +
                ", ID Cardapio=" + idCardapio + ", Preço=" + preco + "]";
    }
}