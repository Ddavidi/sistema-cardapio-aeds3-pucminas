package compressao;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class BackupManager {

    // Lista de todos os arquivos que compõem a base de dados
    private static final String[] FILES_TO_BACKUP = {
            "empresas.db", "empresas.hash.dir", "empresas.hash.bkt", "empresas.bptree.idx",
            "cardapios.db", "cardapios.hash.dir", "cardapios.hash.bkt", "cardapios.bptree.idx",
            "produtos.db", "produtos.hash.dir", "produtos.hash.bkt", "produtos.bptree.idx",
            "categorias.db", "categorias.hash.dir", "categorias.hash.bkt",
            "produtocardapio.db", "produtocardapio.hash.dir", "produtocardapio.hash.bkt", "produtocardapio.bptree.idx",
            "public.key", "private.key" // Incluir chaves RSA é importante para backup completo
    };

    public static void createBackup(int version) {
        try {
            System.out.println("\n--- INICIANDO PROCESSO DE BACKUP ---");

            // 1. Juntar todos os arquivos num único array de bytes (formato de arquivamento simples)
            ByteArrayOutputStream archiveStream = new ByteArrayOutputStream();
            DataOutputStream archiveDos = new DataOutputStream(archiveStream);

            long totalOriginalSize = 0;

            for (String fileName : FILES_TO_BACKUP) {
                File f = new File(fileName);
                if (f.exists()) {
                    byte[] fileContent = Files.readAllBytes(f.toPath());
                    // Escreve: [Nome do Arquivo] [Tamanho] [Conteúdo]
                    archiveDos.writeUTF(fileName);
                    archiveDos.writeInt(fileContent.length);
                    archiveDos.write(fileContent);

                    totalOriginalSize += fileContent.length;
                    System.out.println("Arquivado: " + fileName + " (" + fileContent.length + " bytes)");
                }
            }

            byte[] originalData = archiveStream.toByteArray();
            System.out.println("\nTamanho total original: " + totalOriginalSize + " bytes");

            // 2. Comprimir usando LZW
            long startTime = System.currentTimeMillis();
            byte[] lzwCompressed = LZW.compress(originalData);
            long lzwTime = System.currentTimeMillis() - startTime;
            saveFile("backup_v" + version + ".lzw", lzwCompressed);

            printStats("LZW", totalOriginalSize, lzwCompressed.length, lzwTime);

            // 3. Comprimir usando Huffman
            startTime = System.currentTimeMillis();
            byte[] huffmanCompressed = Huffman.compress(originalData);
            long huffmanTime = System.currentTimeMillis() - startTime;
            saveFile("backup_v" + version + ".huff", huffmanCompressed);

            printStats("Huffman", totalOriginalSize, huffmanCompressed.length, huffmanTime);

        } catch (Exception e) {
            System.out.println("Erro ao criar backup: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void saveFile(String fileName, byte[] data) throws IOException {
        FileOutputStream fos = new FileOutputStream(fileName);
        fos.write(data);
        fos.close();
    }

    private static void printStats(String algorithm, long original, long compressed, long time) {
        float ratio = 100.0f - ((float) compressed / original * 100.0f);
        System.out.println("\n--- Resultados " + algorithm + " ---");
        System.out.println("Tamanho Original: " + original + " bytes");
        System.out.println("Tamanho Comprimido: " + compressed + " bytes");
        System.out.printf("Taxa de Compressão: %.2f%%\n", ratio);
        System.out.println("Tempo de Execução: " + time + "ms");
        System.out.println("Arquivo gerado com sucesso.");
    }
}