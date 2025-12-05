package padroes;

import java.util.Arrays;

/**
 * Implementação do algoritmo Boyer-Moore (heurística Bad Character).
 */
public class BoyerMoore {

    private static final int ALPHABET_SIZE = 256; // Suporte a ASCII estendido

    /**
     * Procura a primeira ocorrência do padrão no texto.
     * @param texto O texto onde pesquisar.
     * @param padrao O termo a ser pesquisado.
     * @return true se encontrar.
     */
    public static boolean search(String texto, String padrao) {
        if (padrao == null || padrao.length() == 0) return false;
        if (texto == null || padrao.length() > texto.length()) return false;

        // Normaliza para busca case-insensitive
        String T = texto.toLowerCase();
        String P = padrao.toLowerCase();

        int m = P.length();
        int n = T.length();

        int[] badChar = new int[ALPHABET_SIZE];
        badCharHeuristic(P, m, badChar);

        int s = 0; // s é o deslocamento do padrão em relação ao texto
        while (s <= (n - m)) {
            int j = m - 1;

            // Move j da direita para a esquerda enquanto os caracteres coincidem
            while (j >= 0 && P.charAt(j) == T.charAt(s + j)) {
                j--;
            }

            if (j < 0) {
                // Padrão encontrado na posição s
                return true;

                // Para continuar a busca:
                // s += (s + m < n) ? m - badChar[T.charAt(s + m)] : 1;
            } else {
                // Desloca o padrão de acordo com a regra do Bad Character
                // Math.max garante que o deslocamento seja positivo
                s += Math.max(1, j - badChar[T.charAt(s + j)]);
            }
        }
        return false;
    }

    /**
     * Preenche a tabela de Bad Character.
     * Guarda a última posição de cada caractere no padrão.
     */
    private static void badCharHeuristic(String str, int size, int[] badChar) {
        Arrays.fill(badChar, -1); // Inicializa tudo com -1
        for (int i = 0; i < size; i++) {
            // Usa o código ASCII do caractere como índice
            // (int) garante compatibilidade, mas cuidado com caracteres > 255 (Unicode)
            // Para um trabalho académico, assumimos ASCII/ISO-8859-1
            int index = str.charAt(i);
            if (index < ALPHABET_SIZE) {
                badChar[index] = i;
            }
        }
    }
}