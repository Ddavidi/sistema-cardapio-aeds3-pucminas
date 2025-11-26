package compressao;

import java.io.*;
import java.util.*;

public class Huffman {

    // Nó da árvore de Huffman
    private static class Node implements Comparable<Node> {
        byte data;
        int frequency;
        Node left, right;

        Node(byte data, int frequency) {
            this.data = data;
            this.frequency = frequency;
        }

        Node(Node left, Node right) {
            this.left = left;
            this.right = right;
            this.frequency = left.frequency + right.frequency;
        }

        public boolean isLeaf() {
            return left == null && right == null;
        }

        @Override
        public int compareTo(Node other) {
            return this.frequency - other.frequency;
        }
    }

    public static byte[] compress(byte[] input) throws IOException {
        if (input.length == 0) return new byte[0];

        // 1. Calcular frequências
        Map<Byte, Integer> freqMap = new HashMap<>();
        for (byte b : input) {
            freqMap.put(b, freqMap.getOrDefault(b, 0) + 1);
        }

        // 2. Criar fila de prioridade
        PriorityQueue<Node> queue = new PriorityQueue<>();
        for (Map.Entry<Byte, Integer> entry : freqMap.entrySet()) {
            queue.add(new Node(entry.getKey(), entry.getValue()));
        }

        // 3. Construir árvore de Huffman
        while (queue.size() > 1) {
            Node left = queue.poll();
            Node right = queue.poll();
            queue.add(new Node(left, right));
        }
        Node root = queue.poll();

        // 4. Gerar tabela de códigos
        Map<Byte, String> huffmanCodes = new HashMap<>();
        generateCodes(root, "", huffmanCodes);

        // 5. Codificar os dados
        StringBuilder sb = new StringBuilder();
        for (byte b : input) {
            sb.append(huffmanCodes.get(b));
        }

        // 6. Serializar (Árvore + Dados)
        // Precisamos salvar a árvore (ou frequências) para poder descomprimir depois.
        // Para simplificar este trabalho, vamos salvar o mapa de frequências no início.
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeInt(freqMap.size());
        for(Map.Entry<Byte, Integer> entry : freqMap.entrySet()) {
            dos.writeByte(entry.getKey());
            dos.writeInt(entry.getValue());
        }

        // Converter string de bits para bytes reais
        // Adicionamos o tamanho original em bits para lidar com o padding final
        int bitLength = sb.length();
        dos.writeInt(bitLength);

        BitSet bitSet = new BitSet(bitLength);
        for(int i=0; i<bitLength; i++) {
            if(sb.charAt(i) == '1') {
                bitSet.set(i);
            }
        }

        byte[] compressedBytes = bitSet.toByteArray();
        dos.writeInt(compressedBytes.length);
        dos.write(compressedBytes);

        return baos.toByteArray();
    }

    private static void generateCodes(Node node, String code, Map<Byte, String> huffmanCodes) {
        if (node.isLeaf()) {
            huffmanCodes.put(node.data, code);
            return;
        }
        generateCodes(node.left, code + "0", huffmanCodes);
        generateCodes(node.right, code + "1", huffmanCodes);
    }

    // A descompressão seria o processo inverso (ler frequências -> reconstruir árvore -> ler bits -> navegar na árvore)
    // Como o foco do trabalho é a taxa de compressão, a compressão é a parte crítica.
}