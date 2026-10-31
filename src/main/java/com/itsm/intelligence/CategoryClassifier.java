package com.itsm.intelligence;

import com.itsm.domain.catalog.Category;

import java.util.List;
import java.util.Optional;

/**
 * Suggests an incident category from its text. Implementations are interchangeable:
 * a keyword/TF-IDF classifier that works offline, and an AI classifier backed by Gemini.
 */
public interface CategoryClassifier {

    Optional<CategorySuggestion> suggest(String text, List<Category> categories);
}
