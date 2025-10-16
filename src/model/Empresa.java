package model;

import java.io.*;
import java.util.Arrays;
import java.util.Date;

/**
 * Classe que representa a entidade Empresa.
 * Contém um campo multivalorado (telefones) e um campo de data.
 */
public class Empresa implements Register {

    private int idEmpresa;
    private String nome;
    private String cnpj;
    private Date dataCadastro;
    private String[] telefones; // CAMPO MULTIVALORADO

    public Empresa() {
        this.idEmpresa = -1;
        this.nome = "";
        this.cnpj = "";
        this.dataCadastro = new Date(); // Data atual por padrão
        this.telefones = new String[0];
    }

    public Empresa(String nome, String cnpj, String[] telefones) {
        this.idEmpresa = -1;
        this.nome = nome;
        this.cnpj = cnpj;
        this.dataCadastro = new Date(); // Data da criação do objeto
        this.telefones = telefones;
    }

    // --- MÉTODOS DA INTERFACE REGISTER ---

    @Override
    public int getID() { return this.idEmpresa; }

    @Override
    public void setID(int id) { this.idEmpresa = id; }

    @Override
    public String getSecondaryKey() { return this.nome; }

    @Override
    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeInt(this.idEmpresa);
        dos.writeUTF(this.nome);
        dos.writeUTF(this.cnpj);
        dos.writeLong(this.dataCadastro.getTime()); // Salva a data como um long

        // Converte o array de telefones numa única String separada por ";"
        String telefonesStr = String.join(";", this.telefones);
        dos.writeUTF(telefonesStr);

        return baos.toByteArray();
    }

    @Override
    public void fromByteArray(byte[] byteArray) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(byteArray);
        DataInputStream dis = new DataInputStream(bais);

        this.idEmpresa = dis.readInt();
        this.nome = dis.readUTF();
        this.cnpj = dis.readUTF();
        this.dataCadastro = new Date(dis.readLong()); // Lê o long e converte para data

        // Lê a string única e a converte de volta para um array
        String telefonesStr = dis.readUTF();
        if (telefonesStr.isEmpty()) {
            this.telefones = new String[0];
        } else {
            this.telefones = telefonesStr.split(";");
        }
    }

    // --- GETTERS E SETTERS ---

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCnpj() { return cnpj; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj; }
    public Date getDataCadastro() { return dataCadastro; }
    public String[] getTelefones() { return telefones; }
    public void setTelefones(String[] telefones) { this.telefones = telefones; }

    @Override
    public String toString() {
        return "Empresa [ID=" + idEmpresa + ", Nome='" + nome + "', CNPJ='" + cnpj +
                "', Data de Cadastro=" + dataCadastro + ", Telefones=" + Arrays.toString(telefones) + "]";
    }
}

