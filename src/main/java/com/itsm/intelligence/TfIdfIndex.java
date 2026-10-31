package com.itsm.intelligence;

import java.util.*;

/**
 * A tiny TF-IDF vector space over a set of documents.
 * <p>
 * Words that appear in many documents ("payment" in a payments-heavy log) get a low
 * weight, rare and specific words ("timeout", "ssl") get a high weight, so cosine
 * similarity focuses on what actually distinguishes two incidents.
 *
 * @param <K> the document key type (e.g. incident id)
 */
public class TfIdfIndex<K> {

    private final Map<K, Map<String, Double>> vectors = new LinkedHashMap<>();
    private final Map<String, Integer> documentFrequency = new HashMap<>();
    private int documentCount;

    public TfIdfIndex(Map<K, String> documents) {
        Map<K, List<String>> tokenised = new LinkedHashMap<>();
        documents.forEach((key, text) -> {
            List<String> terms = TextAnalyzer.terms(text);
            tokenised.put(key, terms);
            new HashSet<>(terms).forEach(t -> documentFrequency.merge(t, 1, Integer::sum));
        });
        documentCount = documents.size();
        tokenised.forEach((key, terms) -> vectors.put(key, vectorise(terms)));
    }

    public Map<String, Double> vectorise(String text) {
        return vectorise(TextAnalyzer.terms(text));
    }

    private Map<String, Double> vectorise(List<String> terms) {
        Map<String, Integer> counts = new HashMap<>();
        terms.forEach(t -> counts.merge(t, 1, Integer::sum));
        Map<String, Double> vector = new HashMap<>();
        counts.forEach((term, count) -> {
            double tf = 1 + Math.log(count);
            double idf = Math.log((documentCount + 1.0) / (documentFrequency.getOrDefault(term, 0) + 1.0)) + 1;
            vector.put(term, tf * idf);
        });
        return vector;
    }

    public Map<K, Map<String, Double>> vectors() {
        return Collections.unmodifiableMap(vectors);
    }

    /** Similarity of {@code text} to every indexed document, best first. */
    public List<Map.Entry<K, Double>> mostSimilar(String text, int limit) {
        Map<String, Double> query = vectorise(text);
        return vectors.entrySet().stream()
                .map(e -> Map.entry(e.getKey(), cosine(query, e.getValue())))
                .filter(e -> e.getValue() > 0)
                .sorted(Map.Entry.<K, Double>comparingByValue().reversed())
                .limit(limit)
                .toList();
    }

    public static double cosine(Map<String, Double> a, Map<String, Double> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0;
        }
        double dot = 0;
        for (Map.Entry<String, Double> e : a.entrySet()) {
            Double other = b.get(e.getKey());
            if (other != null) {
                dot += e.getValue() * other;
            }
        }
        return dot == 0 ? 0 : dot / (norm(a) * norm(b));
    }

    private static double norm(Map<String, Double> v) {
        double sum = 0;
        for (double x : v.values()) {
            sum += x * x;
        }
        return Math.sqrt(sum);
    }
}
