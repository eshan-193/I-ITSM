package com.itsm.domain.priority;

/**
 * Strategy pattern: an interchangeable priority algorithm.
 * The administrator chooses the active implementation in Settings.
 */
public interface PriorityCalculator {

    /** Stable identifier stored in settings, e.g. "WEIGHTED". */
    String key();

    String displayName();

    String description();

    PriorityResult calculate(PriorityInput input);
}
