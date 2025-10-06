package com.scheduler.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class CronServiceTest {

    private CronService cronService;

    @BeforeEach
    void setUp() {
        cronService = new CronService();
    }

    @Test
    void getNextExecutionTime_WithValidCronExpression_ShouldReturnNextExecutionTime() {
        // Given
        String cronExpression = "0 0 * * * *"; // Every hour at minute 0
        ZonedDateTime fromTime = ZonedDateTime.parse("2024-01-01T10:30:00Z");

        // When
        ZonedDateTime nextExecution = cronService.getNextExecutionTime(cronExpression, fromTime);

        // Then
        assertThat(nextExecution).isNotNull();
        assertThat(nextExecution).isAfter(fromTime);
        assertThat(nextExecution.getMinute()).isEqualTo(0);
        assertThat(nextExecution.getSecond()).isEqualTo(0);
    }

    @Test
    void getNextExecutionTime_WithEveryMinuteExpression_ShouldReturnNextMinute() {
        // Given
        String cronExpression = "0 * * * * *"; // Every minute at second 0
        ZonedDateTime fromTime = ZonedDateTime.parse("2024-01-01T10:30:45Z");

        // When
        ZonedDateTime nextExecution = cronService.getNextExecutionTime(cronExpression, fromTime);

        // Then
        assertThat(nextExecution).isNotNull();
        assertThat(nextExecution.getMinute()).isEqualTo(31); // Next minute
        assertThat(nextExecution.getSecond()).isEqualTo(0);
        assertThat(nextExecution.getHour()).isEqualTo(10);
    }

    @Test
    void getNextExecutionTime_WithEvery5Minutes_ShouldReturnCorrectNextTime() {
        // Given
        String cronExpression = "0 */5 * * * *"; // Every 5 minutes
        ZonedDateTime fromTime = ZonedDateTime.parse("2024-01-01T10:32:00Z");

        // When
        ZonedDateTime nextExecution = cronService.getNextExecutionTime(cronExpression, fromTime);

        // Then
        assertThat(nextExecution).isNotNull();
        assertThat(nextExecution.getMinute()).isEqualTo(35); // Next 5-minute mark
        assertThat(nextExecution.getSecond()).isEqualTo(0);
    }

    @Test
    void getNextExecutionTime_WithSpecificTime_ShouldReturnExactTime() {
        // Given
        String cronExpression = "0 30 14 * * *"; // 2:30 PM daily
        ZonedDateTime fromTime = ZonedDateTime.parse("2024-01-01T10:00:00Z");

        // When
        ZonedDateTime nextExecution = cronService.getNextExecutionTime(cronExpression, fromTime);

        // Then
        assertThat(nextExecution).isNotNull();
        assertThat(nextExecution.getHour()).isEqualTo(14);
        assertThat(nextExecution.getMinute()).isEqualTo(30);
        assertThat(nextExecution.getSecond()).isEqualTo(0);
    }

    @Test
    void getNextExecutionTime_WithWeekdayExpression_ShouldReturnCorrectDay() {
        // Given
        String cronExpression = "0 0 9 * * 1-5"; // 9 AM on weekdays (Monday-Friday)
        ZonedDateTime fromTime = ZonedDateTime.parse("2024-01-01T10:00:00Z"); // Monday

        // When
        ZonedDateTime nextExecution = cronService.getNextExecutionTime(cronExpression, fromTime);

        // Then
        assertThat(nextExecution).isNotNull();
        assertThat(nextExecution.getHour()).isEqualTo(9);
        assertThat(nextExecution.getMinute()).isEqualTo(0);
        assertThat(nextExecution.getSecond()).isEqualTo(0);
        // Should be on a weekday (1-5)
        assertThat(nextExecution.getDayOfWeek().getValue()).isBetween(1, 5);
    }

    @Test
    void getNextExecutionTime_WithRangeExpression_ShouldReturnWithinRange() {
        // Given
        String cronExpression = "0 0 9-17 * * *"; // Every hour from 9 AM to 5 PM
        ZonedDateTime fromTime = ZonedDateTime.parse("2024-01-01T10:30:00Z");

        // When
        ZonedDateTime nextExecution = cronService.getNextExecutionTime(cronExpression, fromTime);

        // Then
        assertThat(nextExecution).isNotNull();
        assertThat(nextExecution.getHour()).isEqualTo(11); // Next hour in range
        assertThat(nextExecution.getMinute()).isEqualTo(0);
        assertThat(nextExecution.getSecond()).isEqualTo(0);
    }

    @Test
    void getNextExecutionTime_WithStepExpression_ShouldReturnCorrectStep() {
        // Given
        String cronExpression = "0 0 */2 * * *"; // Every 2 hours
        ZonedDateTime fromTime = ZonedDateTime.parse("2024-01-01T11:30:00Z");

        // When
        ZonedDateTime nextExecution = cronService.getNextExecutionTime(cronExpression, fromTime);

        // Then
        assertThat(nextExecution).isNotNull();
        assertThat(nextExecution.getHour()).isEqualTo(12); // Next 2-hour mark
        assertThat(nextExecution.getMinute()).isEqualTo(0);
        assertThat(nextExecution.getSecond()).isEqualTo(0);
    }

    @Test
    void getNextExecutionTime_WithNullExpression_ShouldThrowException() {
        // Given
        ZonedDateTime fromTime = ZonedDateTime.now();

        // When & Then
        assertThatThrownBy(() -> cronService.getNextExecutionTime(null, fromTime))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Cron expression cannot be null or empty");
    }

    @Test
    void getNextExecutionTime_WithEmptyExpression_ShouldThrowException() {
        // Given
        ZonedDateTime fromTime = ZonedDateTime.now();

        // When & Then
        assertThatThrownBy(() -> cronService.getNextExecutionTime("", fromTime))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Cron expression cannot be null or empty");
    }


    @Test
    void getNextExecutionTime_WithComplexExpression_ShouldHandleCorrectly() {
        // Given
        String cronExpression = "0 15,45 9-17 * * 1-5"; // At 15 and 45 minutes past the hour from 9 AM to 5 PM on weekdays
        ZonedDateTime fromTime = ZonedDateTime.parse("2024-01-01T10:30:00Z"); // Monday

        // When
        ZonedDateTime nextExecution = cronService.getNextExecutionTime(cronExpression, fromTime);

        // Then
        assertThat(nextExecution).isNotNull();
        assertThat(nextExecution.getHour()).isEqualTo(10);
        assertThat(nextExecution.getMinute()).isEqualTo(45); // Next 15,45 minute mark
        assertThat(nextExecution.getSecond()).isEqualTo(0);
    }
}
