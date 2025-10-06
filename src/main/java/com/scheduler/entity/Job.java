package com.scheduler.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@Entity
@Table(name = "jobs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Job {
    
    @Id
    private String id;
    
    @Column(nullable = false)
    private String schedule;
    
    @Column(nullable = false)
    private String apiUrl;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionType executionType;
    
    @Column(nullable = false)
    private Boolean isActive;
    
    @Column(name = "next_execution_time")
    private ZonedDateTime nextExecutionTime;
    
    @Column(name = "created_at")
    private ZonedDateTime createdAt;
    
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
        updatedAt = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
    }
}
