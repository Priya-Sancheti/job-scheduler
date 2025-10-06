package com.scheduler.service;

import com.scheduler.config.ApplicationProperties;
import com.scheduler.entity.JobExecution;
import com.scheduler.entity.ExecutionStatus;
import com.scheduler.repository.JobExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FailureRecoveryService {
    
    private final JobExecutionRepository jobExecutionRepository;
    private final JobExecutionService jobExecutionService;
    private final ApplicationProperties properties;
    
    /**
     * Scheduled method to detect and mark stale RUNNING jobs as FAILED
     * Runs every 30 seconds
     */
    @Scheduled(fixedRate = 30000)
    @Transactional
    public void detectStaleExecutions() {
        try {
            ZonedDateTime staleThreshold = ZonedDateTime.now().minusSeconds(properties.getJob().getRecovery().getStaleTimeoutSeconds());
            
            List<JobExecution> staleExecutions = jobExecutionRepository
                .findByStatusAndScheduledTimeBefore(ExecutionStatus.RUNNING, staleThreshold);
            
            if (!staleExecutions.isEmpty()) {
                log.warn("Found {} stale executions, marking as FAILED", staleExecutions.size());
                
                for (JobExecution execution : staleExecutions) {
                    execution.setStatus(ExecutionStatus.FAILED);
                    execution.setCompletedAt(ZonedDateTime.now());
                    jobExecutionRepository.save(execution);
                    
                    log.warn("Marked stale execution {} as FAILED", execution.getId());
                }
            }
            
        } catch (Exception e) {
            log.error("Error detecting stale executions: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Scheduled method to retry FAILED jobs with exponential backoff
     * Runs every 60 seconds
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void retryFailedExecutions() {
        try {
            List<JobExecution> failedExecutions = jobExecutionRepository
                .findByStatusAndRetryCountLessThanOrderByCreatedAtAsc(ExecutionStatus.FAILED, properties.getJob().getRetry().getMaxAttempts());
            
            if (!failedExecutions.isEmpty()) {
                log.info("Found {} failed executions eligible for retry", failedExecutions.size());
                
                for (JobExecution failedExecution : failedExecutions) {
                    if (shouldRetry(failedExecution)) {
                        createRetryExecution(failedExecution);
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("Error retrying failed executions: {}", e.getMessage(), e);
        }
    }
    
    boolean shouldRetry(JobExecution failedExecution) {
        ZonedDateTime now = ZonedDateTime.now();
        long delayMs = calculateRetryDelay(failedExecution.getRetryCount());
        ZonedDateTime retryTime = failedExecution.getCompletedAt().plusNanos(delayMs * 1_000_000);
        
        return now.isAfter(retryTime);
    }
    
    long calculateRetryDelay(int retryCount) {
        return (long) (properties.getJob().getRetry().getInitialDelayMs() * Math.pow(properties.getJob().getRetry().getMultiplier(), retryCount));
    }
    
    private void createRetryExecution(JobExecution failedExecution) {
        JobExecution retryExecution = JobExecution.builder()
            .id(UUID.randomUUID().toString())
            .jobId(failedExecution.getJobId())
            .status(ExecutionStatus.PENDING)
            .scheduledTime(ZonedDateTime.now())
            .retryCount(failedExecution.getRetryCount() + 1)
            .build();
        
        jobExecutionRepository.save(retryExecution);
        
        // Execute the retry asynchronously
        jobExecutionService.execute(retryExecution.getId());
        
        log.info("Created retry execution {} for job {} (attempt {})", 
            retryExecution.getId(), failedExecution.getJobId(), retryExecution.getRetryCount());
    }
}
