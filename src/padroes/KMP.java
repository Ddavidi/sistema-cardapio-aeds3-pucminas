package padroes;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementação do algoritmo Knuth-Morris-Pratt (KMP) para casamento de padrões.
 */
public class KMP {

    /**
     * Procura todas as ocorrências do padrão no texto.
     * @param texto O texto onde pesquisar (ex: nome do produto).
     * @param padrao O termo a ser pesquisado.
     * @return true se encontrar pelo menos uma ocorrência.
     */
    public static boolean search(String texto, String padrao) {
        if (padrao == null || padrao.length() == 0) return false;
        if (texto == null || padrao.length() > texto.length()) return false;

        // Normaliza para minúsculas para busca case-insensitive (opcional, mas recomendável)
        String T = texto.toLowerCase();
        String P = padrao.toLowerCase();

        int n = T.length();
        int m = P.length();

        int[] lps = computeLPSArray(P);

        int i = 0; // índice para T
        int j = 0; // índice para P

        while (i < n) {
            if (P.charAt(j) == T.charAt(i)) {
                j++;
                i++;
            }
            if (j == m) {
                return true; // Encontrou o padrão
                // Se quiséssemos todas as ocorrências: j = lps[j - 1];
            } else if (i < n && P.charAt(j) != T.charAt(i)) {
                if (j != 0) {
                    j = lps[j - 1];
                } else {
                    i = i + 1;
                }
            }
        }
        return false;
    }

    /**
     * Calcula o array LPS (Longest Prefix Suffix).
     * LPS[i] guarda o comprimento do maior prefixo próprio de P[0..i]
     * que é também sufixo de P[0..i].
     */
    private static int[] computeLPSArray(String pat) {
        int m = pat.length();
        int[] lps = new int[m];
        int len = 0; // comprimento do prefixo anterior mais longo
        int i = 1;
        lps[0] = 0; // lps[0] é sempre 0

        while (i < m) {
            if (pat.charAt(i) == pat.charAt(len)) {
                len++;
                lps[i] = len;
                i++;
            } else {
                if (len != 0) {
                    len = lps[len - 1];
                } else {
                    lps[i] = len;
                    i++;
                }
            }
        }
        return lps;
    }
}