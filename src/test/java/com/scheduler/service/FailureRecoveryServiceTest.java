package com.scheduler.service;

import com.scheduler.config.ApplicationProperties;
import com.scheduler.entity.ExecutionStatus;
import com.scheduler.entity.JobExecution;
import com.scheduler.repository.JobExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FailureRecoveryServiceTest {

    @Mock
    private JobExecutionRepository jobExecutionRepository;

    @Mock
    private JobExecutionService jobExecutionService;

    @Mock
    private ApplicationProperties properties;

    @InjectMocks
    private FailureRecoveryService failureRecoveryService;

    private ApplicationProperties.Job.Recovery recovery;
    private ApplicationProperties.Job.Retry retry;
    private ApplicationProperties.Job jobProps;

    @BeforeEach
    void setUp() {
        // Set up recovery properties
        recovery = new ApplicationProperties.Job.Recovery();
        recovery.setStaleTimeoutSeconds(100);

        // Set up retry properties
        retry = new ApplicationProperties.Job.Retry();
        retry.setMaxAttempts(5);
        retry.setInitialDelayMs(1000);
        retry.setMultiplier(2.0);

        // Set up job properties
        jobProps = new ApplicationProperties.Job();
        jobProps.setRecovery(recovery);
        jobProps.setRetry(retry);

        when(properties.getJob()).thenReturn(jobProps);
    }




    @Test
    void retryFailedExecutions_WithEligibleFailedExecutions_ShouldCreateRetryExecutions() {
        // Given
        ZonedDateTime completedTime = ZonedDateTime.now().minusSeconds(5); // Completed 5 seconds ago
        JobExecution failedExecution = JobExecution.builder()
            .id("execution-1")
            .jobId("job-1")
            .status(ExecutionStatus.FAILED)
            .completedAt(completedTime)
            .retryCount(1)
            .build();

        List<JobExecution> failedExecutions = Arrays.asList(failedExecution);
        when(jobExecutionRepository.findByStatusAndRetryCountLessThanOrderByCreatedAtAsc(
            ExecutionStatus.FAILED, 5))
            .thenReturn(failedExecutions);

        // When
        failureRecoveryService.retryFailedExecutions();

        // Then
        verify(jobExecutionRepository).save(argThat(execution -> 
            execution.getJobId().equals("job-1") &&
            execution.getStatus() == ExecutionStatus.PENDING &&
            execution.getRetryCount() == 2)); // Incremented retry count

        verify(jobExecutionService).execute(anyString());
    }

    @Test
    void retryFailedExecutions_WithMaxRetryAttemptsReached_ShouldNotRetry() {
        // Given
        JobExecution maxRetryExecution = JobExecution.builder()
            .id("execution-1")
            .jobId("job-1")
            .status(ExecutionStatus.FAILED)
            .completedAt(ZonedDateTime.now().minusSeconds(5))
            .retryCount(5) // Max retries reached
            .build();

        List<JobExecution> failedExecutions = Arrays.asList(maxRetryExecution);
        when(jobExecutionRepository.findByStatusAndRetryCountLessThanOrderByCreatedAtAsc(
            ExecutionStatus.FAILED, 5))
            .thenReturn(failedExecutions);

        // When
        failureRecoveryService.retryFailedExecutions();

        // Then
        verify(jobExecutionRepository, never()).save(any(JobExecution.class));
        verify(jobExecutionService, never()).execute(anyString());
    }


    @Test
    void retryFailedExecutions_WithExponentialBackoff_ShouldRespectBackoffDelay() {
        // Given
        // Set completed time to allow for retry (more than 2000ms ago for retry count 1)
        ZonedDateTime completedTime = ZonedDateTime.now().minusSeconds(3);
        JobExecution failedExecution = JobExecution.builder()
            .id("execution-1")
            .jobId("job-1")
            .status(ExecutionStatus.FAILED)
            .completedAt(completedTime)
            .retryCount(1) // Second retry attempt
            .build();

        List<JobExecution> failedExecutions = Arrays.asList(failedExecution);
        when(jobExecutionRepository.findByStatusAndRetryCountLessThanOrderByCreatedAtAsc(
            ExecutionStatus.FAILED, 5))
            .thenReturn(failedExecutions);

        // When
        failureRecoveryService.retryFailedExecutions();

        // Then
        verify(jobExecutionRepository).save(argThat(execution -> 
            execution.getJobId().equals("job-1") &&
            execution.getStatus() == ExecutionStatus.PENDING &&
            execution.getRetryCount() == 2)); // Incremented retry count

        verify(jobExecutionService).execute(anyString());
    }

    @Test
    void retryFailedExecutions_WithMultipleEligibleFailures_ShouldRetryAll() {
        // Given
        ZonedDateTime completedTime = ZonedDateTime.now().minusSeconds(5);
        JobExecution failedExecution1 = JobExecution.builder()
            .id("execution-1")
            .jobId("job-1")
            .status(ExecutionStatus.FAILED)
            .completedAt(completedTime)
            .retryCount(0)
            .build();

        JobExecution failedExecution2 = JobExecution.builder()
            .id("execution-2")
            .jobId("job-2")
            .status(ExecutionStatus.FAILED)
            .completedAt(completedTime)
            .retryCount(2)
            .build();

        List<JobExecution> failedExecutions = Arrays.asList(failedExecution1, failedExecution2);
        when(jobExecutionRepository.findByStatusAndRetryCountLessThanOrderByCreatedAtAsc(
            ExecutionStatus.FAILED, 5))
            .thenReturn(failedExecutions);

        // When
        failureRecoveryService.retryFailedExecutions();

        // Then
        verify(jobExecutionRepository, times(2)).save(any(JobExecution.class));
        verify(jobExecutionService, times(2)).execute(anyString());
    }

    @Test
    void calculateRetryDelay_ShouldUseExponentialBackoff() {
        // Given
        FailureRecoveryService service = new FailureRecoveryService(
            jobExecutionRepository, jobExecutionService, properties);

        // When & Then
        // First retry: 1000ms * 2^0 = 1000ms
        long delay0 = service.calculateRetryDelay(0);
        assertThat(delay0).isEqualTo(1000);

        // Second retry: 1000ms * 2^1 = 2000ms
        long delay1 = service.calculateRetryDelay(1);
        assertThat(delay1).isEqualTo(2000);

        // Third retry: 1000ms * 2^2 = 4000ms
        long delay2 = service.calculateRetryDelay(2);
        assertThat(delay2).isEqualTo(4000);

        // Fourth retry: 1000ms * 2^3 = 8000ms
        long delay3 = service.calculateRetryDelay(3);
        assertThat(delay3).isEqualTo(8000);
    }

    @Test
    void shouldRetry_WithSufficientDelay_ShouldReturnTrue() {
        // Given
        JobExecution failedExecution = JobExecution.builder()
            .id("execution-1")
            .jobId("job-1")
            .status(ExecutionStatus.FAILED)
            .completedAt(ZonedDateTime.now().minusSeconds(5)) // 5 seconds ago
            .retryCount(0)
            .build();

        FailureRecoveryService service = new FailureRecoveryService(
            jobExecutionRepository, jobExecutionService, properties);

        // When
        boolean shouldRetry = service.shouldRetry(failedExecution);

        // Then
        assertThat(shouldRetry).isTrue();
    }


    @Test
    void retryFailedExecutions_WithException_ShouldNotPropagateException() {
        // Given
        when(jobExecutionRepository.findByStatusAndRetryCountLessThanOrderByCreatedAtAsc(
            ExecutionStatus.FAILED, 5))
            .thenThrow(new RuntimeException("Database error"));

        // When & Then - Should not throw exception
        failureRecoveryService.retryFailedExecutions();

        // Then
        verify(jobExecutionRepository, never()).save(any(JobExecution.class));
        verify(jobExecutionService, never()).execute(anyString());
    }
}
