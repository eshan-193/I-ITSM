package com.itsm.intelligence;

import com.itsm.domain.catalog.Category;
import com.itsm.domain.catalog.ItService;
import com.itsm.domain.incident.Impact;
import com.itsm.domain.incident.Urgency;
import com.itsm.intelligence.ai.LlmClient;
import com.itsm.repository.ItServiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Turns "the payment site has been down for 20 mins, nobody in finance can bill customers"
 * into a structured incident form. Uses the AI when available, otherwise heuristics.
 */
@Component
public class IncidentDraftExtractor {

    private static final Logger log = LoggerFactory.getLogger(IncidentDraftExtractor.class);
    private static final Pattern NUMBER_OF_USERS = Pattern.compile("(\\d{1,5})\\s*(users|people|employees|staff|customers|colleagues)");

    private final LlmClient llm;
    private final ItServiceRepository services;
    private final CategorySuggestionService categories;
    private final JsonMapper json = JsonMapper.builder().build();

    public IncidentDraftExtractor(LlmClient llm, ItServiceRepository services, CategorySuggestionService categories) {
        this.llm = llm;
        this.services = services;
        this.categories = categories;
    }

    @Transactional(readOnly = true)
    public IncidentDraft extract(String text) {
        List<ItService> active = services.findByActiveTrueOrderByName();
        if (llm.isAvailable()) {
            try {
                return fromAi(text, active);
            } catch (RuntimeException e) { // provider failure (ExternalServiceException) or malformed JSON
                log.warn("AI extraction failed, using heuristics: {}", e.getMessage());
            }
        }
        return heuristic(text, active);
    }

    private IncidentDraft fromAi(String text, List<ItService> active) {
        String serviceList = active.stream().map(ItService::getName).collect(Collectors.joining(", "));
        String prompt = """
                Extract a structured IT incident from this employee message.
                Known services: %s
                Message: \"\"\"%s\"\"\"
                Reply with JSON only:
                {"title": "<max 80 chars>", "description": "<clear restatement with any details>",
                 "service": "<exact name from the known services or empty>",
                 "impact": "LOW|MEDIUM|HIGH", "urgency": "LOW|MEDIUM|HIGH", "affectedUsers": <integer>}
                impact: LOW=one person, MEDIUM=a team, HIGH=organisation-wide/customers.
                urgency: LOW=workaround exists, MEDIUM=soon, HIGH=work blocked now.
                """.formatted(serviceList, text.replace("\"\"\"", "'"));
        JsonNode node = json.readTree(llm.generateJson(prompt));
        String title = node.path("title").asString(firstSentence(text));
        String description = node.path("description").asString(text);
        ItService service = matchService(node.path("service").asString(""), active).orElseGet(() -> guessService(text, active));
        Impact impact = parseEnum(Impact.class, node.path("impact").asString(""), Impact.MEDIUM);
        Urgency urgency = parseEnum(Urgency.class, node.path("urgency").asString(""), Urgency.MEDIUM);
        int users = Math.max(1, node.path("affectedUsers").asInt(1));
        Category category = categories.suggestOffline(title + " " + description).map(CategorySuggestion::category).orElse(null);
        return new IncidentDraft(title, description, service, category, impact, urgency, users, llm.providerName());
    }

    IncidentDraft heuristic(String text, List<ItService> active) {
        String lower = text.toLowerCase(Locale.ROOT);
        Impact impact = containsAny(lower, "everyone", "all users", "whole company", "entire", "customers", "organisation", "organization", "nobody")
                ? Impact.HIGH
                : containsAny(lower, "team", "department", "several", "many", "colleagues", "we ") ? Impact.MEDIUM : Impact.LOW;
        Urgency urgency = containsAny(lower, "urgent", "asap", "immediately", "blocked", "cannot work", "can't work", "down", "outage", "critical")
                ? Urgency.HIGH
                : containsAny(lower, "soon", "slow", "intermittent", "sometimes") ? Urgency.MEDIUM : Urgency.LOW;
        int users = 1;
        Matcher m = NUMBER_OF_USERS.matcher(lower);
        if (m.find()) {
            users = Integer.parseInt(m.group(1));
        } else if (impact == Impact.HIGH) {
            users = 100;
        } else if (impact == Impact.MEDIUM) {
            users = 10;
        }
        Category category = categories.suggestOffline(text).map(CategorySuggestion::category).orElse(null);
        return new IncidentDraft(firstSentence(text), text.trim(), guessService(text, active), category,
                impact, urgency, users, "Offline text analysis");
    }

    private static ItService guessService(String text, List<ItService> active) {
        Set<String> terms = TextAnalyzer.termSet(text);
        ItService best = null;
        int bestHits = 0;
        for (ItService s : active) {
            Set<String> nameTerms = TextAnalyzer.termSet(s.getName() + " " + Objects.requireNonNullElse(s.getDescription(), ""));
            nameTerms.retainAll(terms);
            if (nameTerms.size() > bestHits) {
                bestHits = nameTerms.size();
                best = s;
            }
        }
        return best;
    }

    private static Optional<ItService> matchService(String name, List<ItService> active) {
        return active.stream().filter(s -> s.getName().equalsIgnoreCase(name.trim())).findFirst();
    }

    private static String firstSentence(String text) {
        String t = text.trim().split("[.!?\\n]")[0].trim();
        if (t.length() > 80) {
            t = t.substring(0, 77) + "...";
        }
        return t.isEmpty() ? "Incident report" : Character.toUpperCase(t.charAt(0)) + t.substring(1);
    }

    private static boolean containsAny(String text, String... words) {
        for (String w : words) {
            if (text.contains(w)) return true;
        }
        return false;
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String value, E fallback) {
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
