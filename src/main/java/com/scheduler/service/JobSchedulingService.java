package com.scheduler.service;

import com.scheduler.entity.Job;
import com.scheduler.entity.JobExecution;
import com.scheduler.entity.ExecutionStatus;
import com.scheduler.repository.JobRepository;
import com.scheduler.repository.JobExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobSchedulingService {
    
    private final JobRepository jobRepository;
    private final JobExecutionRepository jobExecutionRepository;
    private final JobExecutionService jobExecutionService;
    private final CronService cronService;

    
    /**
     * Scheduled method that runs every second to check for jobs that need to be executed
     * Uses distributed locking with SKIP LOCKED to ensure only one instance processes each job
     */
    @Scheduled(fixedRate = 1000)
    @Transactional
    public void scheduleJobs() {
        try {
            ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
            
            // This query uses pessimistic locking with SKIP LOCKED
            // Only one instance will be able to lock and process each job
            List<Job> jobsToExecute = jobRepository.findJobsForExecution(now);
            
            if (jobsToExecute.isEmpty()) {
                return;
            }
            
            log.debug("Found {} jobs ready for execution", jobsToExecute.size());
            
            for (Job job : jobsToExecute) {
                try {
                    String executionId = processJob(job);
                    // Use TransactionSynchronization to ensure 'executeAsync' is called ONLY after commit.
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            // Execute the job asynchronously
                            jobExecutionService.execute(executionId);
                        }
                    });
                    log.info("Job {} scheduled for execution with execution ID: {}", job.getId(), executionId);

                } catch (Exception e) {
                    log.error("Error processing job {}: {}", job.getId(), e.getMessage(), e);
                }
            }
            
        } catch (Exception e) {
            log.error("Error in job scheduling: {}", e.getMessage(), e);
        }
    }

    private String processJob(Job job) {
        log.debug("Processing job: {}", job.getId());
        
        // Create a new job execution record
        JobExecution execution = JobExecution.builder()
            .id(UUID.randomUUID().toString())
            .jobId(job.getId())
            .status(ExecutionStatus.PENDING)
            .scheduledTime(ZonedDateTime.now())
            .build();
        
        jobExecutionRepository.save(execution);
        
        // Calculate next execution time and update job
        try {
            ZonedDateTime nextExecutionTime = cronService.getNextExecutionTime(
                job.getSchedule(), 
                ZonedDateTime.now()
            );
            job.setNextExecutionTime(nextExecutionTime);
            jobRepository.save(job);
            
            log.debug("Updated next execution time for job {}: {}", job.getId(), nextExecutionTime);
        } catch (Exception e) {
            log.error("Error calculating next execution time for job {}: {}", job.getId(), e.getMessage());
            // Disable the job if cron expression is invalid
            job.setIsActive(false);
            jobRepository.save(job);
        }
        return execution.getId();
    }
}
