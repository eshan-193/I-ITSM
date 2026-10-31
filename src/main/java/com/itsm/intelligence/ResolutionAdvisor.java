package com.itsm.intelligence;

import com.itsm.domain.incident.Incident;
import com.itsm.intelligence.ai.LlmClient;
import com.itsm.repository.IncidentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.*;

/**
 * Knowledge-base search: suggests fixes that worked for similar past incidents, and can
 * optionally ask the AI to turn them into concrete troubleshooting steps.
 */
@Component
public class ResolutionAdvisor {

    private static final Logger log = LoggerFactory.getLogger(ResolutionAdvisor.class);
    private static final double MIN_SIMILARITY = 0.12;

    private final IncidentRepository incidents;
    private final LlmClient llm;
    private final JsonMapper json = JsonMapper.builder().build();

    public ResolutionAdvisor(IncidentRepository incidents, LlmClient llm) {
        this.incidents = incidents;
        this.llm = llm;
    }

    @Transactional(readOnly = true)
    public List<SimilarIncident> similarResolved(String title, String description, Long excludeId) {
        List<Incident> knowledgeBase = incidents.findKnowledgeBase().stream()
                .filter(i -> !Objects.equals(i.getId(), excludeId))
                .toList();
        if (knowledgeBase.isEmpty()) {
            return List.of();
        }
        Map<Incident, String> docs = new LinkedHashMap<>();
        knowledgeBase.forEach(i -> docs.put(i, i.getTitle() + " " + i.getTitle() + " " + i.getDescription()
                + " " + Objects.requireNonNullElse(i.getRootCause(), "")));
        TfIdfIndex<Incident> index = new TfIdfIndex<>(docs);
        return index.mostSimilar(title + " " + title + " " + description, 3).stream()
                .filter(e -> e.getValue() >= MIN_SIMILARITY)
                .map(e -> new SimilarIncident(e.getKey(), e.getValue()))
                .toList();
    }

    public boolean aiAvailable() {
        return llm.isAvailable();
    }

    /** AI-generated troubleshooting steps grounded in similar past fixes. Empty if AI is unavailable. */
    @Transactional(readOnly = true)
    public Optional<AiAdvice> aiAdvice(Incident incident) {
        if (!llm.isAvailable()) {
            return Optional.empty();
        }
        StringBuilder history = new StringBuilder();
        for (SimilarIncident s : similarResolved(incident.getTitle(), incident.getDescription(), incident.getId())) {
            history.append("- ").append(s.incident().getTitle()).append(" → fix: ").append(s.incident().getResolution()).append('\n');
        }
        String prompt = """
                You are a senior IT support engineer. Suggest how to resolve this incident.
                Service: %s
                Category: %s
                Title: %s
                Description: %s

                Fixes that worked for similar past incidents:
                %s
                Reply with JSON only: {"likelyCause": "<one sentence>", "steps": ["<step>", "..."]} with at most 5 short steps.
                """.formatted(incident.getService().getName(),
                incident.getCategory() == null ? "Unknown" : incident.getCategory().getName(),
                incident.getTitle(), incident.getDescription(),
                history.isEmpty() ? "(none recorded)" : history);
        try {
            JsonNode node = json.readTree(llm.generateJson(prompt));
            List<String> steps = new ArrayList<>();
            node.path("steps").forEach(s -> steps.add(s.asString()));
            return Optional.of(new AiAdvice(node.path("likelyCause").asString(""), steps, llm.providerName()));
        } catch (RuntimeException e) { // provider failure (ExternalServiceException) or malformed JSON
            log.warn("AI resolution advice failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public record AiAdvice(String likelyCause, List<String> steps, String provider) {
    }
}
