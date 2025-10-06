package com.scheduler.controller;

import com.scheduler.dto.JobCreatedResponse;
import com.scheduler.dto.JobExecutionResponse;
import com.scheduler.dto.JobSpec;
import com.scheduler.entity.JobExecution;
import com.scheduler.service.JobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobControllerTest {

    @Mock
    private JobService jobService;

    @InjectMocks
    private JobController jobController;

    private JobSpec validJobSpec;
    private JobCreatedResponse jobCreatedResponse;
    private List<JobExecutionResponse> jobExecutionResponses;

    @BeforeEach
    void setUp() {
        validJobSpec = new JobSpec(
            "0 */5 * * * *",
            "https://api.example.com/webhook",
            "AT_LEAST_ONCE"
        );

        jobCreatedResponse = new JobCreatedResponse("job-123");

        jobExecutionResponses = Arrays.asList(
            new JobExecutionResponse(
                "execution-1",
                "SUCCESS",
                ZonedDateTime.now().minusMinutes(10),
                ZonedDateTime.now().minusMinutes(10),
                ZonedDateTime.now().minusMinutes(9),
                60000,
                200,
                0
            ),
            new JobExecutionResponse(
                "execution-2",
                "FAILED",
                ZonedDateTime.now().minusMinutes(5),
                ZonedDateTime.now().minusMinutes(5),
                ZonedDateTime.now().minusMinutes(4),
                30000,
                500,
                1
            )
        );
    }

    @Test
    void createJob_WithValidJobSpec_ShouldReturnCreatedResponse() {
        // Given
        when(jobService.createJob(any(JobSpec.class))).thenReturn(jobCreatedResponse);

        // When
        ResponseEntity<JobCreatedResponse> response = jobController.createJob(validJobSpec);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().jobId()).isEqualTo("job-123");
    }

    @Test
    void createJob_WithDifferentJobSpec_ShouldCallServiceWithCorrectParameters() {
        // Given
        JobSpec customJobSpec = new JobSpec(
            "0 0 9 * * 1-5",
            "https://api.example.com/different-webhook",
            "AT_MOST_ONCE"
        );
        JobCreatedResponse customResponse = new JobCreatedResponse("job-456");
        when(jobService.createJob(customJobSpec)).thenReturn(customResponse);

        // When
        ResponseEntity<JobCreatedResponse> response = jobController.createJob(customJobSpec);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().jobId()).isEqualTo("job-456");
    }

    @Test
    void getJobExecutions_WithValidJobId_ShouldReturnExecutionList() {
        // Given
        String jobId = "job-123";
        when(jobService.getPaginatedExecutions(jobId,0,10)).thenReturn(jobExecutionResponses);

        // When
        ResponseEntity<List<JobExecutionResponse>> response = jobController.getJobExecutions(jobId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(2);
        
        JobExecutionResponse firstExecution = response.getBody().get(0);
        assertThat(firstExecution.executionId()).isEqualTo("execution-1");
        assertThat(firstExecution.status()).isEqualTo("SUCCESS");
        assertThat(firstExecution.statusCode()).isEqualTo(200);
        assertThat(firstExecution.retryCount()).isEqualTo(0);

        JobExecutionResponse secondExecution = response.getBody().get(1);
        assertThat(secondExecution.executionId()).isEqualTo("execution-2");
        assertThat(secondExecution.status()).isEqualTo("FAILED");
        assertThat(secondExecution.statusCode()).isEqualTo(500);
        assertThat(secondExecution.retryCount()).isEqualTo(1);
    }

    @Test
    void getJobExecutions_WithEmptyExecutionList_ShouldReturnEmptyList() {
        // Given
        String jobId = "job-empty";
        when(jobService.getPaginatedExecutions(jobId,0,10)).thenReturn(Arrays.asList());

        // When
        ResponseEntity<List<JobExecutionResponse>> response = jobController.getJobExecutions(jobId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void getJobExecutions_WithLongJobId_ShouldHandleCorrectly() {
        // Given
        String longJobId = "job-with-very-long-id-that-might-be-a-uuid-or-similar-identifier";
        when(jobService.getPaginatedExecutions(longJobId,0,10)).thenReturn(jobExecutionResponses);

        // When
        ResponseEntity<List<JobExecutionResponse>> response = jobController.getJobExecutions(longJobId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(2);
    }

    @Test
    void getJobExecutions_WithSpecialCharactersInJobId_ShouldHandleCorrectly() {
        // Given
        String specialJobId = "job-123-with-special-chars-@#$%";
        when(jobService.getPaginatedExecutions(specialJobId,0,10)).thenReturn(jobExecutionResponses);

        // When
        ResponseEntity<List<JobExecutionResponse>> response = jobController.getJobExecutions(specialJobId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(2);
    }

    @Test
    void getJobExecutions_WithSingleExecution_ShouldReturnSingleItemList() {
        // Given
        String jobId = "job-single";
        List<JobExecutionResponse> singleExecution = Arrays.asList(jobExecutionResponses.get(0));
        when(jobService.getPaginatedExecutions(jobId,0,10)).thenReturn(singleExecution);

        // When
        ResponseEntity<List<JobExecutionResponse>> response = jobController.getJobExecutions(jobId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).executionId()).isEqualTo("execution-1");
    }

    @Test
    void getJobExecutions_WithLargeExecutionList_ShouldReturnAllExecutions() {
        // Given
        String jobId = "job-large";
        List<JobExecutionResponse> largeExecutionList = Arrays.asList(
            jobExecutionResponses.get(0),
            jobExecutionResponses.get(1),
            jobExecutionResponses.get(0), // Duplicate for testing
            jobExecutionResponses.get(1)  // Duplicate for testing
        );
        when(jobService.getPaginatedExecutions(jobId,0,10)).thenReturn(largeExecutionList);

        // When
        ResponseEntity<List<JobExecutionResponse>> response = jobController.getJobExecutions(jobId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(4);
    }

    @Test
    void createJob_ShouldCallServiceWithExactJobSpec() {
        // Given
        JobSpec specificJobSpec = new JobSpec(
            "0 30 14 * * *",
            "https://specific-api.example.com/callback",
            "AT_LEAST_ONCE"
        );
        when(jobService.createJob(specificJobSpec)).thenReturn(jobCreatedResponse);

        // When
        jobController.createJob(specificJobSpec);

        // Then
        // Verify that the service was called with the exact JobSpec
        // This is implicit in the when().thenReturn() setup, but we can verify the response
        ResponseEntity<JobCreatedResponse> response = jobController.createJob(specificJobSpec);
        assertThat(response.getBody().jobId()).isEqualTo("job-123");
    }

    @Test
    void getJobExecutions_ShouldCallServiceWithExactJobId() {
        // Given
        String specificJobId = "specific-job-id-123";
        when(jobService.getPaginatedExecutions(specificJobId,0,10)).thenReturn(jobExecutionResponses);

        // When
        jobController.getJobExecutions(specificJobId);

        // Then
        // Verify that the service was called with the exact job ID
        // This is implicit in the when().thenReturn() setup, but we can verify the response
        ResponseEntity<List<JobExecutionResponse>> response = jobController.getJobExecutions(specificJobId);
        assertThat(response.getBody()).hasSize(2);
    }
}
