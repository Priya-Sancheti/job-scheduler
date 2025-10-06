package com.scheduler.service;

import com.scheduler.dto.JobCreatedResponse;
import com.scheduler.dto.JobExecutionResponse;
import com.scheduler.dto.JobSpec;
import com.scheduler.entity.ExecutionStatus;
import com.scheduler.entity.ExecutionType;
import com.scheduler.entity.Job;
import com.scheduler.entity.JobExecution;
import com.scheduler.exception.JobNotFoundException;
import com.scheduler.repository.JobRepository;
import com.scheduler.repository.JobExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobService {

    private final JobRepository jobRepository;
    private final JobExecutionRepository jobExecutionRepository;
    private final CronService cronService;

    @Transactional
    public JobCreatedResponse createJob(JobSpec jobSpec) {
        log.info("Creating new job with schedule: {}, apiUrl: {}, type: {}",
                jobSpec.schedule(), jobSpec.apiUrl(), jobSpec.type());

        // Validate execution type
        ExecutionType executionType;
        try {
            executionType = ExecutionType.valueOf(jobSpec.type().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid execution type: " + jobSpec.type() +
                    ". Must be either ATLEAST_ONCE or ATMOST_ONCE");
        }

        // Validate and calculate next execution time
        ZonedDateTime nextExecutionTime;
        try {
            nextExecutionTime = cronService.getNextExecutionTime(jobSpec.schedule(), ZonedDateTime.now(ZoneId.of("Asia/Kolkata")));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid CRON expression: " + e.getMessage(), e);
        }

        // Create job
        Job job = Job.builder()
                .id(UUID.randomUUID().toString())
                .schedule(jobSpec.schedule())
                .apiUrl(jobSpec.apiUrl())
                .executionType(executionType)
                .isActive(true)
                .nextExecutionTime(nextExecutionTime)
                .build();

        job = jobRepository.save(job);

        log.info("Successfully created job with ID: {} and next execution at: {}",
                job.getId(), job.getNextExecutionTime());

        return new JobCreatedResponse(job.getId());
    }

    @Transactional(readOnly = true)
    public List<JobExecutionResponse> getPaginatedExecutions(
            String jobId,
            Integer page,
            Integer size) {

        int currentPage = (page != null && page >= 0) ? page : 0;
        int pageSize = (size != null && size > 0) ? size : 10; // Default pageSize is 10


        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(currentPage, pageSize, sort);

        boolean jobExists = jobRepository.existsById(jobId);

        if (jobExists == false) {
            throw new JobNotFoundException("Job not found with ID: non-existent");
        }

        // --- 3. Execute the Paginated Query ---
        Page<JobExecution> executionPage = jobExecutionRepository.findByJobId(jobId, pageable);

        // --- 4. Map the Page content to the Response DTO List ---
        return executionPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private JobExecutionResponse mapToResponse(JobExecution execution) {
        return new JobExecutionResponse(
                execution.getId(),
                execution.getStatus().name(),
                execution.getScheduledTime(),
                execution.getStartedAt(),
                execution.getCompletedAt(),
                execution.getDurationMs() != null ? execution.getDurationMs().intValue() : null,
                execution.getStatusCode(),
                execution.getRetryCount()
        );
    }

    @Transactional(readOnly = true)
    public List<JobExecutionResponse> getByStatus(ExecutionStatus status) {
        // --- 3. Execute the Paginated Query ---
        List<JobExecution> jobExecutionList = jobExecutionRepository.findByStatus(status);

        // --- 4. Map the Page content to the Response DTO List ---
        return jobExecutionList.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

}
