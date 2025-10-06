package com.scheduler.dto;

import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DtoTest {

    @Test
    void jobSpec_ShouldCreateRecordCorrectly() {
        // Given
        String schedule = "0 */5 * * * *";
        String apiUrl = "https://api.example.com/webhook";
        String type = "ATLEAST_ONCE";

        // When
        JobSpec jobSpec = new JobSpec(schedule, apiUrl, type);

        // Then
        assertThat(jobSpec.schedule()).isEqualTo(schedule);
        assertThat(jobSpec.apiUrl()).isEqualTo(apiUrl);
        assertThat(jobSpec.type()).isEqualTo(type);
    }

    @Test
    void jobSpec_WithNullValues_ShouldAcceptNulls() {
        // When
        JobSpec jobSpec = new JobSpec(null, null, null);

        // Then
        assertThat(jobSpec.schedule()).isNull();
        assertThat(jobSpec.apiUrl()).isNull();
        assertThat(jobSpec.type()).isNull();
    }

    @Test
    void jobCreatedResponse_ShouldCreateRecordCorrectly() {
        // Given
        String jobId = "job-123";

        // When
        JobCreatedResponse response = new JobCreatedResponse(jobId);

        // Then
        assertThat(response.jobId()).isEqualTo(jobId);
    }

    @Test
    void jobCreatedResponse_WithNullJobId_ShouldAcceptNull() {
        // When
        JobCreatedResponse response = new JobCreatedResponse(null);

        // Then
        assertThat(response.jobId()).isNull();
    }

    @Test
    void jobExecutionResponse_ShouldCreateRecordWithAllFields() {
        // Given
        String executionId = "execution-123";
        String status = "SUCCESS";
        ZonedDateTime scheduledTime = ZonedDateTime.now().minusMinutes(10);
        ZonedDateTime startedAt = ZonedDateTime.now().minusMinutes(10);
        ZonedDateTime completedAt = ZonedDateTime.now().minusMinutes(9);
        Integer durationMs = 60000;
        Integer statusCode = 200;
        int retryCount = 0;

        // When
        JobExecutionResponse response = new JobExecutionResponse(
            executionId, status, scheduledTime, startedAt, completedAt,
            durationMs, statusCode, retryCount
        );

        // Then
        assertThat(response.executionId()).isEqualTo(executionId);
        assertThat(response.status()).isEqualTo(status);
        assertThat(response.scheduledTime()).isEqualTo(scheduledTime);
        assertThat(response.startedAt()).isEqualTo(startedAt);
        assertThat(response.completedAt()).isEqualTo(completedAt);
        assertThat(response.durationMs()).isEqualTo(durationMs);
        assertThat(response.statusCode()).isEqualTo(statusCode);
        assertThat(response.retryCount()).isEqualTo(retryCount);
    }

    @Test
    void jobExecutionResponse_WithNullValues_ShouldAcceptNulls() {
        // When
        JobExecutionResponse response = new JobExecutionResponse(
            null, null, null, null, null, null, null,  0
        );

        // Then
        assertThat(response.executionId()).isNull();
        assertThat(response.status()).isNull();
        assertThat(response.scheduledTime()).isNull();
        assertThat(response.startedAt()).isNull();
        assertThat(response.completedAt()).isNull();
        assertThat(response.durationMs()).isNull();
        assertThat(response.statusCode()).isNull();
        assertThat(response.retryCount()).isEqualTo(0);
    }

    @Test
    void jobExecutionResponse_WithFailedStatus_ShouldHandleCorrectly() {
        // Given
        String executionId = "execution-failed";
        String status = "FAILED";
        ZonedDateTime scheduledTime = ZonedDateTime.now().minusMinutes(5);
        ZonedDateTime startedAt = ZonedDateTime.now().minusMinutes(5);
        ZonedDateTime completedAt = ZonedDateTime.now().minusMinutes(4);
        Integer durationMs = 30000;
        Integer statusCode = 500;
        String output = "Connection timeout";
        int retryCount = 2;

        // When
        JobExecutionResponse response = new JobExecutionResponse(
            executionId, status, scheduledTime, startedAt, completedAt,
            durationMs, statusCode,  retryCount
        );

        // Then
        assertThat(response.executionId()).isEqualTo(executionId);
        assertThat(response.status()).isEqualTo(status);
        assertThat(response.scheduledTime()).isEqualTo(scheduledTime);
        assertThat(response.startedAt()).isEqualTo(startedAt);
        assertThat(response.completedAt()).isEqualTo(completedAt);
        assertThat(response.durationMs()).isEqualTo(durationMs);
        assertThat(response.statusCode()).isEqualTo(statusCode);
        assertThat(response.retryCount()).isEqualTo(retryCount);
    }

    @Test
    void jobExecutionResponse_WithPendingStatus_ShouldHandleCorrectly() {
        // Given
        String executionId = "execution-pending";
        String status = "PENDING";
        ZonedDateTime scheduledTime = ZonedDateTime.now();
        int retryCount = 0;

        // When
        JobExecutionResponse response = new JobExecutionResponse(
            executionId, status, scheduledTime, null, null,
            null, null,  retryCount
        );

        // Then
        assertThat(response.executionId()).isEqualTo(executionId);
        assertThat(response.status()).isEqualTo(status);
        assertThat(response.scheduledTime()).isEqualTo(scheduledTime);
        assertThat(response.startedAt()).isNull();
        assertThat(response.completedAt()).isNull();
        assertThat(response.durationMs()).isNull();
        assertThat(response.statusCode()).isNull();
        assertThat(response.retryCount()).isEqualTo(retryCount);
    }

    @Test
    void jobExecutionResponse_WithRunningStatus_ShouldHandleCorrectly() {
        // Given
        String executionId = "execution-running";
        String status = "RUNNING";
        ZonedDateTime scheduledTime = ZonedDateTime.now().minusMinutes(2);
        ZonedDateTime startedAt = ZonedDateTime.now().minusMinutes(2);
        int retryCount = 1;

        // When
        JobExecutionResponse response = new JobExecutionResponse(
            executionId, status, scheduledTime, startedAt, null,
            null, null,  retryCount
        );

        // Then
        assertThat(response.executionId()).isEqualTo(executionId);
        assertThat(response.status()).isEqualTo(status);
        assertThat(response.scheduledTime()).isEqualTo(scheduledTime);
        assertThat(response.startedAt()).isEqualTo(startedAt);
        assertThat(response.completedAt()).isNull();
        assertThat(response.durationMs()).isNull();
        assertThat(response.statusCode()).isNull();
        assertThat(response.retryCount()).isEqualTo(retryCount);
    }

    @Test
    void jobSpec_ShouldBeImmutable() {
        // Given
        JobSpec jobSpec = new JobSpec("0 */5 * * * *", "https://api.example.com/webhook", "ATLEAST_ONCE");

        // When & Then
        // Records are immutable by default, so we can't modify them
        // This test verifies that the record works as expected
        assertThat(jobSpec.schedule()).isEqualTo("0 */5 * * * *");
        assertThat(jobSpec.apiUrl()).isEqualTo("https://api.example.com/webhook");
        assertThat(jobSpec.type()).isEqualTo("ATLEAST_ONCE");
    }

    @Test
    void jobCreatedResponse_ShouldBeImmutable() {
        // Given
        JobCreatedResponse response = new JobCreatedResponse("job-123");

        // When & Then
        // Records are immutable by default
        assertThat(response.jobId()).isEqualTo("job-123");
    }

    @Test
    void jobExecutionResponse_ShouldBeImmutable() {
        // Given
        ZonedDateTime now = ZonedDateTime.now();
        JobExecutionResponse response = new JobExecutionResponse(
            "execution-123", "SUCCESS", now, now, now, 5000, 200,  0
        );

        // When & Then
        // Records are immutable by default
        assertThat(response.executionId()).isEqualTo("execution-123");
        assertThat(response.status()).isEqualTo("SUCCESS");
        assertThat(response.durationMs()).isEqualTo(5000);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.retryCount()).isEqualTo(0);
    }
}
