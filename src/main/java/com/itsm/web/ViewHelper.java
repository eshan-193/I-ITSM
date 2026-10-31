package com.itsm.web;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Formatting helpers for templates, used as {@code ${@fmt.ago(date)}}. */
@Component("fmt")
public class ViewHelper {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("dd MMM HH:mm");

    private final Clock clock;

    public ViewHelper(Clock clock) {
        this.clock = clock;
    }

    public String dateTime(LocalDateTime t) {
        return t == null ? "—" : DATE_TIME.format(t);
    }

    public String shortDate(LocalDateTime t) {
        return t == null ? "—" : SHORT.format(t);
    }

    /** "5 min ago", "3 h ago", "2 d ago". */
    public String ago(LocalDateTime t) {
        if (t == null) return "—";
        return humanize(Duration.between(t, LocalDateTime.now(clock))) + " ago";
    }

    /** "in 25 min" or "overdue 2 h". */
    public String until(LocalDateTime t) {
        if (t == null) return "—";
        Duration d = Duration.between(LocalDateTime.now(clock), t);
        return d.isNegative() ? "overdue " + humanize(d.negated()) : "in " + humanize(d);
    }

    public String minutes(double minutes) {
        if (minutes <= 0) return "—";
        return humanize(Duration.ofMinutes(Math.round(minutes)));
    }

    public String humanize(Duration d) {
        long m = d.toMinutes();
        if (m < 1) return "<1 min";
        if (m < 60) return m + " min";
        if (m < 60 * 24) return (m / 60) + " h " + (m % 60 == 0 ? "" : (m % 60) + " min");
        return (m / (60 * 24)) + " d " + ((m / 60) % 24) + " h";
    }

    public int percent(double fraction) {
        return (int) Math.max(0, Math.min(100, Math.round(fraction * 100)));
    }
}
