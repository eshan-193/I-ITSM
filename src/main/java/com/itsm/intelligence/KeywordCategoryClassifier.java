package com.itsm.intelligence;

import com.itsm.domain.catalog.Category;
import com.itsm.domain.incident.ClassificationSource;
import com.itsm.domain.incident.Incident;
import com.itsm.repository.IncidentRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Offline classifier that combines two signals:
 * <ol>
 *   <li><b>Keywords</b> configured per category by the administrator ("vpn", "wifi" → NETWORK);</li>
 *   <li><b>History</b>: TF-IDF similarity to past incidents of each category, so the
 *       classifier gets better as more incidents are recorded.</li>
 * </ol>
 * {@code score = keyword hits + 3 × (average similarity of the 3 closest past incidents)}.
 */
@Component
public class KeywordCategoryClassifier implements CategoryClassifier {

    private static final int HISTORY_NEIGHBOURS = 3;

    private final IncidentRepository incidents;

    public KeywordCategoryClassifier(IncidentRepository incidents) {
        this.incidents = incidents;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CategorySuggestion> suggest(String text, List<Category> categories) {
        if (text == null || text.isBlank() || categories.isEmpty()) {
            return Optional.empty();
        }
        Map<Category, Double> history = historyScores(text, categories);
        return classify(text, categories, history);
    }

    /** Pure scoring step, separated so it can be unit-tested without a database. */
    static Optional<CategorySuggestion> classify(String text, List<Category> categories, Map<Category, Double> history) {
        String lower = " " + text.toLowerCase(Locale.ROOT) + " ";
        Set<String> terms = TextAnalyzer.termSet(text);

        Category best = null;
        double bestScore = 0;
        double total = 0;
        List<String> bestHits = List.of();
        for (Category category : categories) {
            List<String> hits = new ArrayList<>();
            for (String keyword : category.keywordList()) {
                boolean matched = keyword.contains(" ")
                        ? lower.contains(keyword)
                        : terms.contains(TextAnalyzer.stem(keyword));
                if (matched) {
                    hits.add(keyword);
                }
            }
            double score = hits.size() + 3 * history.getOrDefault(category, 0.0);
            total += score;
            if (score > bestScore) {
                bestScore = score;
                best = category;
                bestHits = hits;
            }
        }
        if (best == null || bestScore < 0.5) {
            return Optional.empty();
        }
        double share = bestScore / total;
        double confidence = Math.min(0.95, 0.35 + 0.4 * share + 0.05 * Math.min(bestHits.size(), 4));
        String explanation = bestHits.isEmpty()
                ? "Similar to past " + best.getName() + " incidents"
                : "Matched keywords: " + String.join(", ", bestHits.subList(0, Math.min(5, bestHits.size())));
        return Optional.of(new CategorySuggestion(best, confidence, ClassificationSource.KEYWORD, explanation));
    }

    private Map<Category, Double> historyScores(String text, List<Category> categories) {
        List<Incident> past = incidents.findClassified();
        if (past.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> docs = new LinkedHashMap<>();
        Map<Long, Category> categoryOf = new HashMap<>();
        for (Incident i : past) {
            docs.put(i.getId(), i.getTitle() + " " + i.getTitle() + " " + i.getDescription());
            categoryOf.put(i.getId(), i.getCategory());
        }
        TfIdfIndex<Long> index = new TfIdfIndex<>(docs);
        Map<Category, List<Double>> sims = new HashMap<>();
        for (Map.Entry<Long, Double> e : index.mostSimilar(text, 50)) {
            sims.computeIfAbsent(categoryOf.get(e.getKey()), k -> new ArrayList<>()).add(e.getValue());
        }
        Map<Category, Double> scores = new HashMap<>();
        for (Category c : categories) {
            List<Double> list = sims.getOrDefault(c, List.of());
            double avg = list.stream().limit(HISTORY_NEIGHBOURS).mapToDouble(Double::doubleValue).sum() / HISTORY_NEIGHBOURS;
            scores.put(c, avg);
        }
        return scores;
    }
}
