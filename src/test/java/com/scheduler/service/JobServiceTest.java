package com.scheduler.service;

import com.scheduler.dto.JobCreatedResponse;
import com.scheduler.dto.JobExecutionResponse;
import com.scheduler.dto.JobSpec;
import com.scheduler.entity.ExecutionType;
import com.scheduler.entity.Job;
import com.scheduler.entity.JobExecution;
import com.scheduler.entity.ExecutionStatus;
import com.scheduler.exception.JobNotFoundException;
import com.scheduler.repository.JobExecutionRepository;
import com.scheduler.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobExecutionRepository jobExecutionRepository;

    @Mock
    private CronService cronService;

    @InjectMocks
    private JobService jobService;

    private JobSpec validJobSpec;
    private Job savedJob;
    private JobExecution jobExecution;

    @BeforeEach
    void setUp() {
        validJobSpec = new JobSpec(
            "0 */5 * * * *",
            "https://api.example.com/webhook",
            "ATLEAST_ONCE"
        );

        savedJob = Job.builder()
            .id("job-123")
            .schedule("0 */5 * * * *")
            .apiUrl("https://api.example.com/webhook")
            .executionType(ExecutionType.ATLEAST_ONCE)
            .isActive(true)
            .nextExecutionTime(ZonedDateTime.now().plusMinutes(5))
            .build();

        jobExecution = JobExecution.builder()
            .id("execution-123")
            .jobId("job-123")
            .status(ExecutionStatus.SUCCESS)
            .scheduledTime(ZonedDateTime.now().minusMinutes(10))
            .startedAt(ZonedDateTime.now().minusMinutes(10))
            .completedAt(ZonedDateTime.now().minusMinutes(9))
            .durationMs(50000L)
            .statusCode(200)
            .retryCount(0)
            .build();
    }

    @Test
    void createJob_WithValidJobSpec_ShouldReturnJobCreatedResponse() {
        // Given
        when(cronService.getNextExecutionTime(anyString(), any(ZonedDateTime.class)))
            .thenReturn(ZonedDateTime.now().plusMinutes(5));
        when(jobRepository.save(any(Job.class))).thenReturn(savedJob);

        // When
        JobCreatedResponse response = jobService.createJob(validJobSpec);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.jobId()).isEqualTo("job-123");
        verify(jobRepository).save(any(Job.class));
    }

    @Test
    void createJob_WithAtMostOnceType_ShouldCreateJobSuccessfully() {
        // Given
        JobSpec atMostOnceSpec = new JobSpec(
            "0 */5 * * * *",
            "https://api.example.com/webhook",
            "ATMOST_ONCE"
        );
        when(cronService.getNextExecutionTime(anyString(), any(ZonedDateTime.class)))
            .thenReturn(ZonedDateTime.now().plusMinutes(5));
        when(jobRepository.save(any(Job.class))).thenReturn(savedJob);

        // When
        JobCreatedResponse response = jobService.createJob(atMostOnceSpec);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.jobId()).isEqualTo("job-123");
        verify(jobRepository).save(any(Job.class));
    }

    @Test
    void createJob_WithInvalidExecutionType_ShouldThrowException() {
        // Given
        JobSpec invalidSpec = new JobSpec(
            "0 */5 * * * *",
            "https://api.example.com/webhook",
            "INVALID_TYPE"
        );

        // When & Then
        assertThatThrownBy(() -> jobService.createJob(invalidSpec))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid execution type: INVALID_TYPE. Must be either ATLEAST_ONCE or ATMOST_ONCE");
    }

    @Test
    void createJob_WithInvalidCronExpression_ShouldThrowException() {
        // Given
        when(cronService.getNextExecutionTime(anyString(), any(ZonedDateTime.class)))
            .thenThrow(new IllegalArgumentException("Invalid CRON expression"));

        // When & Then
        assertThatThrownBy(() -> jobService.createJob(validJobSpec))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid CRON expression: Invalid CRON expression");
    }

    @Test
    void getJobExecutions_WithValidJobId_ShouldReturnExecutionList() {
        // Given
        String jobId = "job-123";
        List<JobExecution> executions = Arrays.asList(jobExecution);
        int currentPage =  0;
        int pageSize =  10;

        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(currentPage, pageSize, sort);
        Page<JobExecution> mockPage = new PageImpl<>(executions, pageable, 1);
        when(jobRepository.existsById(jobId)).thenReturn(true);
        when(jobExecutionRepository.findByJobId(jobId,pageable)).thenReturn(mockPage);

        // When
        List<JobExecutionResponse> response = jobService.getPaginatedExecutions(jobId,0,10);

        // Then
        assertThat(response).hasSize(1);
        JobExecutionResponse executionResponse = response.get(0);
        assertThat(executionResponse.executionId()).isEqualTo("execution-123");
        assertThat(executionResponse.status()).isEqualTo("SUCCESS");
        assertThat(executionResponse.statusCode()).isEqualTo(200);
        assertThat(executionResponse.retryCount()).isEqualTo(0);

        verify(jobExecutionRepository).findByJobId(jobId,pageable);
    }

    @Test
    void getJobExecutions_WithNonExistentJobId_ShouldThrowException() {
        // Given
        String nonExistentJobId = "non-existent";
        when(jobRepository.existsById(nonExistentJobId)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> jobService.getPaginatedExecutions(nonExistentJobId,0,10))
            .isInstanceOf(JobNotFoundException.class)
            .hasMessage("Job not found with ID: non-existent");

        verify(jobRepository).existsById(nonExistentJobId);
        verify(jobExecutionRepository, never()).findByJobId(anyString(),any());
    }

    @Test
    void getJobExecutions_WithMultipleExecutions_ShouldReturnAllExecutions() {
        // Given
        String jobId = "job-123";
        JobExecution execution1 = JobExecution.builder()
            .id("execution-1")
            .jobId(jobId)
            .status(ExecutionStatus.SUCCESS)
            .scheduledTime(ZonedDateTime.now().minusMinutes(20))
            .startedAt(ZonedDateTime.now().minusMinutes(20))
            .completedAt(ZonedDateTime.now().minusMinutes(19))
            .durationMs(60000L)
            .statusCode(200)
            .retryCount(0)
            .build();

        JobExecution execution2 = JobExecution.builder()
            .id("execution-2")
            .jobId(jobId)
            .status(ExecutionStatus.FAILED)
            .scheduledTime(ZonedDateTime.now().minusMinutes(10))
            .startedAt(ZonedDateTime.now().minusMinutes(10))
            .completedAt(ZonedDateTime.now().minusMinutes(9))
            .durationMs(30000L)
            .statusCode(500)
            .retryCount(1)
            .build();

        List<JobExecution> executions = Arrays.asList(execution1, execution2);
        int currentPage =  0;
        int pageSize =  10;

        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(currentPage, pageSize, sort);

        Page<JobExecution> mockPage = new PageImpl<>(executions, pageable, 2);

        when(jobRepository.existsById(jobId)).thenReturn(true);
        when(jobExecutionRepository.findByJobId(jobId,pageable)).thenReturn(mockPage);


        // When
        List<JobExecutionResponse> response = jobService.getPaginatedExecutions(jobId,0,10);

        // Then
        assertThat(response).hasSize(2);
        assertThat(response.get(0).executionId()).isEqualTo("execution-1");
        assertThat(response.get(0).status()).isEqualTo("SUCCESS");
        assertThat(response.get(1).executionId()).isEqualTo("execution-2");
        assertThat(response.get(1).status()).isEqualTo("FAILED");
        assertThat(response.get(1).retryCount()).isEqualTo(1);
    }

}
