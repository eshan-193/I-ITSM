package com.itsm.domain.sla;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

/**
 * Tracks one incident's SLA deadlines and breaches.
 * <p>
 * Embedded in {@code Incident} (composition): it is stored in the same row and has no
 * life of its own. Rules:
 * <ul>
 *   <li>the response clock stops when a technician acknowledges;</li>
 *   <li>the resolution clock stops when the incident is resolved;</li>
 *   <li>time spent waiting for the reporter (AWAITING_INFO) does not count, so the
 *       resolution deadline is pushed back by the paused duration.</li>
 * </ul>
 */
@Embeddable
public class SlaTracker {

    @Column(name = "sla_started_at")
    private LocalDateTime startedAt;

    @Column(name = "response_deadline")
    private LocalDateTime responseDeadline;

    @Column(name = "resolution_deadline")
    private LocalDateTime resolutionDeadline;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "sla_resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "response_breached")
    private boolean responseBreached;

    @Column(name = "resolution_breached")
    private boolean resolutionBreached;

    @Column(name = "sla_warning_sent")
    private boolean warningSent;

    @Column(name = "paused_since")
    private LocalDateTime pausedSince;

    @Column(name = "paused_minutes")
    private long pausedMinutes;

    public SlaTracker() {
        // for JPA and new incidents
    }

    /** Starts (or restarts, after a priority change) the clocks from the incident's creation time. */
    public void start(LocalDateTime createdAt, SlaPolicy policy) {
        this.startedAt = createdAt;
        this.responseDeadline = createdAt.plus(policy.responseTarget());
        this.resolutionDeadline = createdAt.plus(policy.resolutionTarget()).plusMinutes(pausedMinutes);
        this.warningSent = false;
    }

    public boolean isStarted() {
        return startedAt != null;
    }

    public void recordResponse(LocalDateTime now) {
        if (respondedAt != null) {
            return;
        }
        respondedAt = now;
        if (isStarted() && now.isAfter(responseDeadline)) {
            responseBreached = true;
        }
    }

    public void recordResolution(LocalDateTime now) {
        resolvedAt = now;
        if (isStarted() && now.isAfter(resolutionDeadline)) {
            resolutionBreached = true;
        }
    }

    /** The fix was rejected: the resolution clock runs again (deadline unchanged). */
    public void reopen() {
        resolvedAt = null;
    }

    public void pause(LocalDateTime now) {
        if (pausedSince == null) {
            pausedSince = now;
        }
    }

    public void resume(LocalDateTime now) {
        if (pausedSince == null) {
            return;
        }
        long minutes = Duration.between(pausedSince, now).toMinutes();
        pausedMinutes += minutes;
        if (resolutionDeadline != null) {
            resolutionDeadline = resolutionDeadline.plusMinutes(minutes);
        }
        pausedSince = null;
    }

    public boolean isPaused() {
        return pausedSince != null;
    }

    /**
     * Detects breaches that have newly happened by {@code now} and records them.
     * Called by the background SLA monitor.
     *
     * @return the breach types that were newly detected (empty if none)
     */
    public Set<SlaBreachType> detectNewBreaches(LocalDateTime now) {
        Set<SlaBreachType> newBreaches = EnumSet.noneOf(SlaBreachType.class);
        if (!isStarted()) {
            return newBreaches;
        }
        if (!responseBreached && respondedAt == null && now.isAfter(responseDeadline)) {
            responseBreached = true;
            newBreaches.add(SlaBreachType.RESPONSE);
        }
        if (!resolutionBreached && resolvedAt == null && !isPaused() && now.isAfter(resolutionDeadline)) {
            resolutionBreached = true;
            newBreaches.add(SlaBreachType.RESOLUTION);
        }
        return newBreaches;
    }

    /** Fraction (0..1+) of the resolution window already used, excluding paused time. */
    public double resolutionElapsedFraction(LocalDateTime now) {
        if (!isStarted()) {
            return 0;
        }
        LocalDateTime end = resolvedAt != null ? resolvedAt : (pausedSince != null ? pausedSince : now);
        long total = Duration.between(startedAt, resolutionDeadline).toSeconds();
        long used = Duration.between(startedAt, end).toSeconds() - pausedMinutes * 60;
        return total <= 0 ? 1 : Math.max(0, (double) used / total);
    }

    public boolean shouldWarn(LocalDateTime now, int warningPercent) {
        return isStarted() && !warningSent && resolvedAt == null && !resolutionBreached
                && resolutionElapsedFraction(now) * 100 >= warningPercent;
    }

    public void markWarningSent() {
        warningSent = true;
    }

    public SlaStatus status(LocalDateTime now, int warningPercent) {
        if (!isStarted()) {
            return SlaStatus.NOT_STARTED;
        }
        if (responseBreached || resolutionBreached) {
            return SlaStatus.BREACHED;
        }
        if (resolvedAt != null) {
            return SlaStatus.MET;
        }
        if (isPaused()) {
            return SlaStatus.PAUSED;
        }
        if (now.isAfter(resolutionDeadline) || (respondedAt == null && now.isAfter(responseDeadline))) {
            return SlaStatus.BREACHED;
        }
        return resolutionElapsedFraction(now) * 100 >= warningPercent ? SlaStatus.AT_RISK : SlaStatus.ON_TRACK;
    }

    /** Minutes left until the resolution deadline (negative when overdue). */
    public long minutesToResolutionDeadline(LocalDateTime now) {
        return isStarted() ? Duration.between(now, resolutionDeadline).toMinutes() : 0;
    }

    public boolean isBreached() {
        return responseBreached || resolutionBreached;
    }

    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getResponseDeadline() { return responseDeadline; }
    public LocalDateTime getResolutionDeadline() { return resolutionDeadline; }
    public LocalDateTime getRespondedAt() { return respondedAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public boolean isResponseBreached() { return responseBreached; }
    public boolean isResolutionBreached() { return resolutionBreached; }
    public boolean isWarningSent() { return warningSent; }
    public LocalDateTime getPausedSince() { return pausedSince; }
    public long getPausedMinutes() { return pausedMinutes; }
}
