package com.scheduler.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@Entity
@Table(name = "job_executions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobExecution {
    
    @Id
    private String id;
    
    @Column(name = "job_id", nullable = false)
    private String jobId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionStatus status;
    
    @Column(name = "scheduled_time", nullable = false)
    private ZonedDateTime scheduledTime;
    
    @Column(name = "started_at")
    private ZonedDateTime startedAt;
    
    @Column(name = "completed_at")
    private ZonedDateTime completedAt;
    
    @Column(name = "duration_ms")
    private Long durationMs;
    
    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "retry_count")
    private Integer retryCount;
    
    @Column(name = "created_at")
    private ZonedDateTime createdAt;
    
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
        updatedAt = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
        if (retryCount == null) {
            retryCount = 0;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
    }
}
