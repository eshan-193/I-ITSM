package com.itsm.intelligence;

import com.itsm.domain.catalog.Category;
import com.itsm.repository.CategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Tries the AI classifier first and falls back to the offline classifier, so the
 * feature keeps working with no API key, no network, or a provider outage.
 */
@Service
public class CategorySuggestionService {

    private static final Logger log = LoggerFactory.getLogger(CategorySuggestionService.class);

    private final AiCategoryClassifier ai;
    private final KeywordCategoryClassifier offline;
    private final CategoryRepository categories;

    public CategorySuggestionService(AiCategoryClassifier ai, KeywordCategoryClassifier offline, CategoryRepository categories) {
        this.ai = ai;
        this.offline = offline;
        this.categories = categories;
    }

    public Optional<CategorySuggestion> suggest(String text) {
        List<Category> active = categories.findByActiveTrueOrderByName();
        try {
            Optional<CategorySuggestion> fromAi = ai.suggest(text, active);
            if (fromAi.isPresent()) {
                return fromAi;
            }
        } catch (RuntimeException e) { // provider failure (ExternalServiceException) or malformed JSON
            log.warn("AI categorisation failed, using offline classifier: {}", e.getMessage());
        }
        return offline.suggest(text, active);
    }

    /** Offline classifier only: instant, used where a slow network call would hurt (live typing). */
    public Optional<CategorySuggestion> suggestOffline(String text) {
        return offline.suggest(text, categories.findByActiveTrueOrderByName());
    }
}
