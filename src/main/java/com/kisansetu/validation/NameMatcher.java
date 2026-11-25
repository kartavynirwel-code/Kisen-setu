package com.kisansetu.validation;

import java.util.Arrays;
import java.util.Locale;

/** Fuzzy comparison of person names: case, punctuation, spacing and word order are ignored. */
public final class NameMatcher {

    private NameMatcher() {
    }

    /** Similarity in [0, 1]; 1 means identical after normalization. */
    public static double similarity(String a, String b) {
        return jaroWinkler(canonical(a), canonical(b));
    }

    public static boolean matches(String a, String b, double threshold) {
        return similarity(a, b) >= threshold;
    }

    /** Upper-case, drop punctuation, sort the words so "Patel Ramesh" equals "Ramesh Patel". */
    static String canonical(String name) {
        String cleaned = name.toUpperCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N} ]", " ").trim();
        if (cleaned.isEmpty()) {
            return "";
        }
        String[] words = cleaned.split("\\s+");
        Arrays.sort(words);
        return String.join(" ", words);
    }

    static double jaroWinkler(String s1, String s2) {
        double jaro = jaro(s1, s2);
        int prefix = 0;
        int max = Math.min(4, Math.min(s1.length(), s2.length()));
        while (prefix < max && s1.charAt(prefix) == s2.charAt(prefix)) {
            prefix++;
        }
        return jaro + prefix * 0.1 * (1 - jaro);
    }

    static double jaro(String s1, String s2) {
        if (s1.equals(s2)) {
            return 1.0;
        }
        int len1 = s1.length();
        int len2 = s2.length();
        if (len1 == 0 || len2 == 0) {
            return 0.0;
        }
        int window = Math.max(0, Math.max(len1, len2) / 2 - 1);
        boolean[] matched1 = new boolean[len1];
        boolean[] matched2 = new boolean[len2];
        int matches = 0;
        for (int i = 0; i < len1; i++) {
            int from = Math.max(0, i - window);
            int to = Math.min(len2, i + window + 1);
            for (int j = from; j < to; j++) {
                if (!matched2[j] && s1.charAt(i) == s2.charAt(j)) {
                    matched1[i] = true;
                    matched2[j] = true;
                    matches++;
                    break;
                }
            }
        }
        if (matches == 0) {
            return 0.0;
        }
        int outOfOrder = 0;
        int k = 0;
        for (int i = 0; i < len1; i++) {
            if (!matched1[i]) {
                continue;
            }
            while (!matched2[k]) {
                k++;
            }
            if (s1.charAt(i) != s2.charAt(k)) {
                outOfOrder++;
            }
            k++;
        }
        double m = matches;
        double transpositions = outOfOrder / 2.0;
        return (m / len1 + m / len2 + (m - transpositions) / m) / 3.0;
    }
}
