package com.itsm.intelligence;

import com.itsm.domain.catalog.ItService;
import com.itsm.domain.incident.Incident;
import com.itsm.domain.incident.IncidentStatus;
import com.itsm.repository.IncidentRepository;
import com.itsm.service.SettingKey;
import com.itsm.service.SettingsService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Finds open incidents that probably describe the same underlying problem.
 * <p>
 * TF-IDF cosine similarity over title (counted twice) and description, plus a bonus
 * when both incidents affect the same service. Example: "Payment application
 * unavailable", "Payment transaction failing" and "Unable to make payment" all share
 * the rare-ish term "payment" and the same service, so they cluster together.
 */
@Component
public class DuplicateDetector {

    static final double SAME_SERVICE_BONUS = 0.15;
    private static final int LOOKBACK_DAYS = 14;

    private final IncidentRepository incidents;
    private final SettingsService settings;
    private final Clock clock;

    public DuplicateDetector(IncidentRepository incidents, SettingsService settings, Clock clock) {
        this.incidents = incidents;
        this.settings = settings;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<SimilarIncident> findPossibleDuplicates(String title, String description, ItService service, Long excludeId) {
        LocalDateTime since = LocalDateTime.now(clock).minusDays(LOOKBACK_DAYS);
        List<Incident> open = incidents.findByStatusInAndCreatedAtAfter(IncidentStatus.UNRESOLVED, since).stream()
                .filter(i -> !Objects.equals(i.getId(), excludeId))
                .toList();
        return rank(title, description, service, open, settings.getDouble(SettingKey.DUPLICATE_THRESHOLD));
    }

    /** Pure ranking step, unit-testable without a database. */
    static List<SimilarIncident> rank(String title, String description, ItService service,
                                      List<Incident> candidates, double threshold) {
        if (candidates.isEmpty()) {
            return List.of();
        }
        Map<Incident, String> docs = new LinkedHashMap<>();
        candidates.forEach(i -> docs.put(i, document(i.getTitle(), i.getDescription())));
        TfIdfIndex<Incident> index = new TfIdfIndex<>(docs);
        Map<String, Double> query = index.vectorise(document(title, description));

        List<SimilarIncident> result = new ArrayList<>();
        index.vectors().forEach((incident, vector) -> {
            double score = TfIdfIndex.cosine(query, vector);
            if (service != null && service.equals(incident.getService()) && score > 0) {
                score += SAME_SERVICE_BONUS;
            }
            score = Math.min(1.0, score);
            if (score >= threshold) {
                result.add(new SimilarIncident(incident, score));
            }
        });
        result.sort(Comparator.comparingDouble(SimilarIncident::similarity).reversed());
        return result.subList(0, Math.min(5, result.size()));
    }

    private static String document(String title, String description) {
        String t = title == null ? "" : title;
        return t + " " + t + " " + (description == null ? "" : description);
    }
}
