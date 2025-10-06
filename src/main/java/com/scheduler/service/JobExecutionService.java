package com.scheduler.service;

import com.scheduler.entity.Job;
import com.scheduler.entity.JobExecution;
import com.scheduler.entity.ExecutionStatus;
import com.scheduler.repository.JobRepository;
import com.scheduler.repository.JobExecutionRepository;
import com.scheduler.config.ApplicationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.ZonedDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobExecutionService {
    
    private final JobExecutionRepository jobExecutionRepository;
    private final JobRepository jobRepository;
    private final ApiClientService apiClientService;
    
    @Async("jobExecutor")
    @Transactional
    public void execute(String jobExecutionId) {
        JobExecution execution = jobExecutionRepository.findById(jobExecutionId)
            .orElseThrow(() -> new RuntimeException("Job execution not found: " + jobExecutionId));
        
        Job job = jobRepository.findById(execution.getJobId())
            .orElseThrow(() -> new RuntimeException("Job not found: " + execution.getJobId()));
        
        log.info("Starting execution of job {} with execution ID: {}", job.getId(), execution.getId());
        
        ZonedDateTime startTime = ZonedDateTime.now();
        execution.setStatus(ExecutionStatus.RUNNING);
        execution.setStartedAt(startTime);
        jobExecutionRepository.save(execution);
        
        try {
            // Perform HTTP to the job's API URL
            int responseStatusCode = apiClientService.executeApiCall(job.getApiUrl(), HttpMethod.GET ,execution.getId(),null);
            
            ZonedDateTime endTime = ZonedDateTime.now();
            long duration = java.time.Duration.between(startTime, endTime).toMillis();

            if(responseStatusCode == 200) {
                execution.setStatus(ExecutionStatus.SUCCESS);
            } else {
                execution.setStatus(ExecutionStatus.FAILED);
            }
            execution.setCompletedAt(endTime);
            execution.setDurationMs(duration);
            execution.setStatusCode(responseStatusCode);
            
            jobExecutionRepository.save(execution);
            
            log.info("Successfully completed execution of job {} with execution ID: {} in {} ms", 
                job.getId(), execution.getId(), duration);
                
        } catch (Exception e) {
            ZonedDateTime endTime = ZonedDateTime.now();
            long duration = java.time.Duration.between(startTime, endTime).toMillis();
            
            execution.setStatus(ExecutionStatus.FAILED);
            execution.setCompletedAt(endTime);
            execution.setDurationMs(duration);
            execution.setStatusCode(500);
            
            jobExecutionRepository.save(execution);
            
            log.error("Failed to execute job {} with execution ID: {} after {} ms. Error: {}", 
                job.getId(), execution.getId(), duration, e.getMessage(), e);
        }
    }
}
