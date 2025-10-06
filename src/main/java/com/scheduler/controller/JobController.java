package com.scheduler.controller;

import com.scheduler.dto.JobCreatedResponse;
import com.scheduler.dto.JobExecutionResponse;
import com.scheduler.dto.JobSpec;
import com.scheduler.entity.ExecutionStatus;
import com.scheduler.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
@Slf4j
public class JobController {
    
    private final JobService jobService;
    
    /**
     * Creates a new scheduled job
     */
    @PostMapping
    public ResponseEntity<JobCreatedResponse> createJob(@Valid @RequestBody JobSpec jobSpec) {
        log.info("Received request to create job with schedule: {}", jobSpec.schedule());
        
        JobCreatedResponse response = jobService.createJob(jobSpec);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Retrieves all executions for a specific job
     */
    @GetMapping("/{jobId}/executions")
    public ResponseEntity<List<JobExecutionResponse>> getJobExecutions(@PathVariable String jobId) {
        log.debug("Received request to get executions for job: {}", jobId);
        
        List<JobExecutionResponse> executions = jobService.getPaginatedExecutions(jobId,0,10);
        
        return ResponseEntity.ok(executions);
    }

    @GetMapping("/status")
    public ResponseEntity<List<JobExecutionResponse>> getAllJob() {
        log.debug("Received request to get executions for job: {}");

        List<JobExecutionResponse> executions = jobService.getByStatus(ExecutionStatus.PENDING);

        return ResponseEntity.ok(executions);
    }
}
