package compressao;

import java.io.*;
import java.util.*;

public class LZW {

    /**
     * Comprime um array de bytes usando LZW.
     */
    public static byte[] compress(byte[] input) throws IOException {
        // Dicionário inicial com todos os bytes possíveis (0-255)
        Map<String, Integer> dictionary = new HashMap<>();
        for (int i = 0; i < 256; i++) {
            dictionary.put("" + (char) i, i);
        }

        String w = "";
        List<Integer> result = new ArrayList<>();
        int dictSize = 256;

        for (byte b : input) {
            // Converte byte para char (0-255) para usar como chave no mapa
            char c = (char) (b & 0xFF);
            String wc = w + c;

            if (dictionary.containsKey(wc)) {
                w = wc;
            } else {
                result.add(dictionary.get(w));
                // Adiciona nova sequência ao dicionário
                dictionary.put(wc, dictSize++);
                w = "" + c;
            }
        }

        if (!w.isEmpty()) {
            result.add(dictionary.get(w));
        }

        // Converte a lista de inteiros (códigos) para array de bytes
        // Usamos DataOutputStream para escrever os inteiros (4 bytes cada)
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        for (int code : result) {
            dos.writeInt(code);
        }

        return baos.toByteArray();
    }

    /**
     * Descomprime um array de bytes usando LZW.
     */
    public static byte[] decompress(byte[] input) throws IOException {
        // Reconstrói a lista de códigos a partir dos bytes
        List<Integer> codes = new ArrayList<>();
        ByteArrayInputStream bais = new ByteArrayInputStream(input);
        DataInputStream dis = new DataInputStream(bais);

        while (dis.available() > 0) {
            codes.add(dis.readInt());
        }

        // Dicionário inicial
        Map<Integer, String> dictionary = new HashMap<>();
        for (int i = 0; i < 256; i++) {
            dictionary.put(i, "" + (char) i);
        }

        String w = "" + (char) (int) codes.remove(0);
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        result.write(w.getBytes("ISO-8859-1")); // Garante que 1 char = 1 byte

        int dictSize = 256;

        for (int k : codes) {
            String entry;
            if (dictionary.containsKey(k)) {
                entry = dictionary.get(k);
            } else if (k == dictSize) {
                entry = w + w.charAt(0);
            } else {
                throw new IllegalArgumentException("Bad compressed k: " + k);
            }

            result.write(entry.getBytes("ISO-8859-1"));

            // Adiciona ao dicionário
            dictionary.put(dictSize++, w + entry.charAt(0));
            w = entry;
        }

        return result.toByteArray();
    }
}