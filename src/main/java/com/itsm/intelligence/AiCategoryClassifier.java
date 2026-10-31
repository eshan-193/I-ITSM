package com.itsm.intelligence;

import com.itsm.domain.catalog.Category;
import com.itsm.domain.incident.ClassificationSource;
import com.itsm.intelligence.ai.LlmClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** Asks the LLM to pick one of the configured categories and explain why. */
@Component
public class AiCategoryClassifier implements CategoryClassifier {

    private final LlmClient llm;
    private final JsonMapper json = JsonMapper.builder().build();

    public AiCategoryClassifier(LlmClient llm) {
        this.llm = llm;
    }

    @Override
    public Optional<CategorySuggestion> suggest(String text, List<Category> categories) {
        if (!llm.isAvailable() || text == null || text.isBlank()) {
            return Optional.empty();
        }
        String options = categories.stream()
                .map(c -> "- " + c.getCode() + ": " + c.getName() + (c.getDescription() == null ? "" : " (" + c.getDescription() + ")"))
                .collect(Collectors.joining("\n"));
        String prompt = """
                You are an IT service desk triage assistant. Classify the incident into exactly one category.
                Categories:
                %s

                Incident report:
                \"\"\"%s\"\"\"

                Reply with JSON only: {"category": "<CODE>", "confidence": <0.0-1.0>, "reason": "<one short sentence>"}
                """.formatted(options, text.replace("\"\"\"", "'"));
        JsonNode node = json.readTree(llm.generateJson(prompt));
        String code = node.path("category").asString("");
        double confidence = node.path("confidence").asDouble(0.7);
        String reason = node.path("reason").asString("Suggested by AI");
        return categories.stream()
                .filter(c -> c.getCode().equalsIgnoreCase(code))
                .findFirst()
                .map(c -> new CategorySuggestion(c, Math.max(0, Math.min(1, confidence)), ClassificationSource.AI, reason));
    }
}
