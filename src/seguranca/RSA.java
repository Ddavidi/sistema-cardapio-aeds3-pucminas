package seguranca;

import java.io.*;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Scanner;

/**
 * Implementação manual do algoritmo RSA para fins académicos.
 * Utiliza BigInteger para lidar com números grandes.
 */
public class RSA {

    private BigInteger n, d, e;
    private int bitlen = 1024; // Tamanho da chave em bits

    /**
     * Construtor: Tenta carregar chaves existentes. Se não existirem, gera novas.
     */
    public RSA() {
        File publicKeyFile = new File("public.key");
        File privateKeyFile = new File("private.key");

        if (publicKeyFile.exists() && privateKeyFile.exists()) {
            try {
                loadKeys();
            } catch (Exception ex) {
                System.out.println("Erro ao carregar chaves. Gerando novas...");
                generateKeys();
            }
        } else {
            generateKeys();
        }
    }

    /**
     * Gera as chaves pública e privada.
     * 1. Escolhe dois primos grandes p e q.
     * 2. Calcula n = p * q.
     * 3. Calcula phi = (p-1) * (q-1).
     * 4. Escolhe e tal que 1 < e < phi e mdc(e, phi) = 1.
     * 5. Calcula d tal que d * e = 1 mod phi.
     */
    public void generateKeys() {
        SecureRandom r = new SecureRandom();
        BigInteger p = new BigInteger(bitlen / 2, 100, r);
        BigInteger q = new BigInteger(bitlen / 2, 100, r);

        n = p.multiply(q);

        BigInteger phi = (p.subtract(BigInteger.ONE)).multiply(q.subtract(BigInteger.ONE));

        e = new BigInteger("65537"); // Valor comum para 'e' (primo de Fermat)
        while (phi.gcd(e).intValue() > 1) {
            e = e.add(new BigInteger("2"));
        }

        d = e.modInverse(phi);

        saveKeys();
    }

    /**
     * Criptografa uma mensagem (String) usando a chave pública.
     * M -> C = M^e mod n
     */
    public String encrypt(String message) {
        return (new BigInteger(message.getBytes())).modPow(e, n).toString();
    }

    /**
     * Descriptografa uma mensagem cifrada (String numérica) usando a chave privada.
     * C -> M = C^d mod n
     */
    public String decrypt(String message) {
        return new String((new BigInteger(message)).modPow(d, n).toByteArray());
    }

    // --- Persistência das Chaves ---

    private void saveKeys() {
        try {
            BufferedWriter pubWriter = new BufferedWriter(new FileWriter("public.key"));
            pubWriter.write(e.toString() + "\n");
            pubWriter.write(n.toString());
            pubWriter.close();

            BufferedWriter privWriter = new BufferedWriter(new FileWriter("private.key"));
            privWriter.write(d.toString() + "\n");
            privWriter.write(n.toString());
            privWriter.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void loadKeys() throws IOException {
        Scanner pubScanner = new Scanner(new File("public.key"));
        this.e = new BigInteger(pubScanner.next());
        this.n = new BigInteger(pubScanner.next());
        pubScanner.close();

        Scanner privScanner = new Scanner(new File("private.key"));
        this.d = new BigInteger(privScanner.next());
        this.n = new BigInteger(privScanner.next()); // O n deve ser o mesmo
        privScanner.close();
    }
}