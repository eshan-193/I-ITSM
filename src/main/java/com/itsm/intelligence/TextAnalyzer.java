package com.itsm.intelligence;

import java.util.*;

/**
 * Turns free text into normalised terms: lower-case, strip punctuation, drop stop
 * words, and apply light suffix stemming ("failing", "failed", "fails" → "fail").
 * Shared by duplicate detection, categorisation and resolution suggestions.
 */
public final class TextAnalyzer {

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "the", "and", "or", "but", "if", "of", "to", "in", "on", "at", "for", "from", "by", "with",
            "is", "are", "was", "were", "be", "been", "being", "am", "it", "its", "this", "that", "these", "those",
            "i", "me", "my", "we", "our", "you", "your", "he", "she", "they", "them", "their", "us",
            "not", "no", "can", "cannot", "cant", "could", "would", "should", "will", "do", "does", "did", "doing",
            "have", "has", "had", "as", "so", "than", "too", "very", "just", "also", "please", "help", "hi", "hello",
            "there", "here", "when", "while", "since", "after", "before", "again", "still", "any", "some", "all",
            "get", "got", "getting", "unable", "able", "issue", "problem", "today", "now", "morning",
            "what", "which", "who", "how", "why", "where", "about", "into", "out", "up", "down", "dont", "doesnt");

    private TextAnalyzer() {
    }

    public static List<String> terms(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        List<String> terms = new ArrayList<>();
        for (String raw : text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
            if (raw.length() < 2 || STOP_WORDS.contains(raw)) {
                continue;
            }
            terms.add(stem(raw));
        }
        return terms;
    }

    public static Set<String> termSet(String text) {
        return new LinkedHashSet<>(terms(text));
    }

    /** Very small suffix stripper; good enough for short incident descriptions. */
    static String stem(String word) {
        if (word.length() <= 4 || word.chars().allMatch(Character::isDigit)) {
            return word;
        }
        for (String suffix : new String[]{"ations", "ation", "ings", "ing", "ies", "ied", "ed", "es", "ly", "s"}) {
            if (word.endsWith(suffix) && word.length() - suffix.length() >= 3) {
                String base = word.substring(0, word.length() - suffix.length());
                if (suffix.equals("ies") || suffix.equals("ied")) {
                    return base + "y";
                }
                return base;
            }
        }
        return word;
    }

    /** Jaccard similarity of two term sets (0–1). */
    public static double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0;
        }
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return (double) intersection.size() / union.size();
    }
}
