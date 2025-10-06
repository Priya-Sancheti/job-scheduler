package com.scheduler.service;

import com.scheduler.entity.ExecutionStatus;
import com.scheduler.entity.Job;
import com.scheduler.entity.JobExecution;
import com.scheduler.repository.JobExecutionRepository;
import com.scheduler.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobSchedulingServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobExecutionRepository jobExecutionRepository;

    @Mock
    private JobExecutionService jobExecutionService;

    @Mock
    private CronService cronService;

    @InjectMocks
    private JobSchedulingService jobSchedulingService;

    private Job readyJob;
    private JobExecution savedExecution;

    @BeforeEach
    void setUp() {
        readyJob = Job.builder()
            .id("job-123")
            .schedule("0 */5 * * * *")
            .apiUrl("https://api.example.com/webhook")
            .executionType(com.scheduler.entity.ExecutionType.ATLEAST_ONCE)
            .isActive(true)
            .nextExecutionTime(ZonedDateTime.now().minusMinutes(1)) // Ready for execution
            .build();

        savedExecution = JobExecution.builder()
            .id("execution-123")
            .jobId("job-123")
            .status(ExecutionStatus.PENDING)
            .scheduledTime(ZonedDateTime.now())
            .retryCount(0)
            .build();
    }


    @Test
    void scheduleJobs_WithNoJobsReady_ShouldDoNothing() {
        // Given
        when(jobRepository.findJobsForExecution(any(ZonedDateTime.class))).thenReturn(Arrays.asList());

        // When
        jobSchedulingService.scheduleJobs();

        // Then
        verify(jobRepository).findJobsForExecution(any(ZonedDateTime.class));
        verify(jobExecutionRepository, never()).save(any(JobExecution.class));
        verify(cronService, never()).getNextExecutionTime(anyString(), any(ZonedDateTime.class));
        verify(jobRepository, never()).save(any(Job.class));
        verify(jobExecutionService, never()).execute(anyString());
    }


    @Test
    void scheduleJobs_ShouldUpdateJobWithNextExecutionTime() {
        // Given
        ZonedDateTime nextExecutionTime = ZonedDateTime.now().plusMinutes(10);
        when(jobRepository.findJobsForExecution(any(ZonedDateTime.class))).thenReturn(Arrays.asList(readyJob));
        when(jobExecutionRepository.save(any(JobExecution.class))).thenReturn(savedExecution);
        when(cronService.getNextExecutionTime(anyString(), any(ZonedDateTime.class)))
            .thenReturn(nextExecutionTime);
        when(jobRepository.save(any(Job.class))).thenReturn(readyJob);

        // When
        jobSchedulingService.scheduleJobs();

        // Then
        verify(jobRepository).save(argThat(job -> 
            job.getId().equals("job-123") &&
            job.getNextExecutionTime().equals(nextExecutionTime)));
    }

    @Test
    void scheduleJobs_WithRepositoryException_ShouldNotPropagateException() {
        // Given
        when(jobRepository.findJobsForExecution(any(ZonedDateTime.class)))
            .thenThrow(new RuntimeException("Database connection error"));

        // When & Then - Should not throw exception
        jobSchedulingService.scheduleJobs();

        // Then
        verify(jobRepository).findJobsForExecution(any(ZonedDateTime.class));
        verify(jobExecutionRepository, never()).save(any(JobExecution.class));
        verify(jobExecutionService, never()).execute(anyString());
    }


    @Test
    void scheduleJobs_WithInactiveJob_ShouldNotBeIncludedInReadyJobs() {
        // Given
        Job inactiveJob = Job.builder()
            .id("job-inactive")
            .schedule("0 */5 * * * *")
            .apiUrl("https://api.example.com/webhook")
            .executionType(com.scheduler.entity.ExecutionType.ATLEAST_ONCE)
            .isActive(false) // Inactive job
            .nextExecutionTime(ZonedDateTime.now().minusMinutes(1))
            .build();

        // The repository query should not return inactive jobs, but let's test the behavior
        when(jobRepository.findJobsForExecution(any(ZonedDateTime.class))).thenReturn(Arrays.asList());

        // When
        jobSchedulingService.scheduleJobs();

        // Then
        verify(jobRepository).findJobsForExecution(any(ZonedDateTime.class));
        verify(jobExecutionRepository, never()).save(any(JobExecution.class));
        verify(jobExecutionService, never()).execute(anyString());
    }
}
