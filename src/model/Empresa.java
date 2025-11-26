package model;

import seguranca.RSA; // IMPORTANTE
import java.io.*;
import java.util.Arrays;
import java.util.Date;

public class Empresa implements Register {

    private int idEmpresa;
    private String nome;
    private String cnpj; // Será armazenado criptografado no ficheiro
    private Date dataCadastro;
    private String[] telefones;

    // Instância estática do RSA para não recarregar chaves a cada objeto
    private static RSA rsa = new RSA();

    public Empresa() {
        this.idEmpresa = -1;
        this.nome = "";
        this.cnpj = "";
        this.dataCadastro = new Date();
        this.telefones = new String[0];
    }

    public Empresa(String nome, String cnpj, String[] telefones) {
        this.idEmpresa = -1;
        this.nome = nome;
        this.cnpj = cnpj;
        this.dataCadastro = new Date();
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

        // --- CRIPTOGRAFIA RSA ---
        // Criptografa o CNPJ antes de escrever
        String cnpjCifrado = rsa.encrypt(this.cnpj);
        dos.writeUTF(cnpjCifrado);
        // ------------------------

        dos.writeLong(this.dataCadastro.getTime());

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

        // --- DESCRIPTOGRAFIA RSA ---
        // Lê o texto cifrado e descriptografa para a memória
        String cnpjCifrado = dis.readUTF();
        try {
            this.cnpj = rsa.decrypt(cnpjCifrado);
        } catch (Exception e) {
            this.cnpj = "ERRO_DECRIPTOGRAFIA";
        }
        // ---------------------------

        this.dataCadastro = new Date(dis.readLong());

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
                "', Data=" + dataCadastro + ", Tels=" + Arrays.toString(telefones) + "]";
    }
}