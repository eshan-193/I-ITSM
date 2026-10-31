package com.itsm.config;

import com.itsm.domain.assignment.*;
import com.itsm.domain.priority.MatrixPriorityCalculator;
import com.itsm.domain.priority.PriorityCalculator;
import com.itsm.domain.priority.WeightedPriorityCalculator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Registers the plain-Java domain strategies as Spring beans.
 * <p>
 * The domain classes themselves have no Spring annotations, which keeps them
 * framework-independent and trivially unit-testable. Adding a strategy = one class
 * plus one {@code @Bean} line here; services discover all beans of the interface type.
 */
@Configuration
public class DomainConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public PriorityCalculator weightedPriorityCalculator() {
        return new WeightedPriorityCalculator();
    }

    @Bean
    public PriorityCalculator matrixPriorityCalculator() {
        return new MatrixPriorityCalculator();
    }

    @Bean
    public AssignmentStrategy smartAssignmentStrategy() {
        return new SmartAssignmentStrategy();
    }

    @Bean
    public AssignmentStrategy skillBasedAssignmentStrategy() {
        return new SkillBasedAssignmentStrategy();
    }

    @Bean
    public AssignmentStrategy loadBasedAssignmentStrategy() {
        return new LoadBasedAssignmentStrategy();
    }

    @Bean
    public AssignmentStrategy roundRobinAssignmentStrategy() {
        return new RoundRobinAssignmentStrategy();
    }
}
