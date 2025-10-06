package com.scheduler.service;

import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;

@Service
public class CronService {

    /**
     * Parses a 6-part CRON expression and calculates the next execution time.
     * Format: second minute hour day month dayOfWeek
     * * Uses the modern, non-deprecated Spring class CronExpression.
     */
    public ZonedDateTime getNextExecutionTime(String cronExpression, ZonedDateTime fromTime) {
        if (cronExpression == null || cronExpression.trim().isEmpty()) {
            throw new IllegalArgumentException("Cron expression cannot be null or empty");
        }

        CronExpression expression;
        try {
            // 1. Parse and Validate the expression
            // The CronExpression is the successor to CronSequenceGenerator
            expression = CronExpression.parse(cronExpression.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid CRON expression format or values. Expected 6-part format: second minute hour day month dayOfWeek", e);
        }

        // 2. Calculate the next execution time starting AFTER the fromTime
        // CronExpression works directly with java.time types (Temporal)
        Temporal nextTemporal = expression.next(fromTime);

        if (nextTemporal == null) {
            // This happens if the expression is impossible (e.g., Feb 30)
            throw new IllegalArgumentException("No valid execution time found for the CRON expression.");
        }

        // 3. Cast the Temporal result back to ZonedDateTime
        return (ZonedDateTime) nextTemporal;
    }
}