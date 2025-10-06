package com.scheduler.dto;

import java.time.ZonedDateTime;

public record JobExecutionResponse(
    String executionId,
    String status,
    ZonedDateTime scheduledTime,
    ZonedDateTime startedAt,
    ZonedDateTime completedAt,
    Integer durationMs,
    Integer statusCode,
    int retryCount
) {}
