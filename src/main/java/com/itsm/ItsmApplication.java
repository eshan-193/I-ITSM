package com.itsm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Intelligent IT Service Management &amp; Incident Resolution System (I-ITSM).
 * <p>
 * Scheduling powers the SLA monitor and auto-escalation; async powers background
 * email delivery.
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
@ConfigurationPropertiesScan
public class ItsmApplication {

    public static void main(String[] args) {
        SpringApplication.run(ItsmApplication.class, args);
    }
}
