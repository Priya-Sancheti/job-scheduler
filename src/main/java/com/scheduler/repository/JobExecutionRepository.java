package com.scheduler.repository;

import com.scheduler.entity.JobExecution;
import com.scheduler.entity.ExecutionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobExecutionRepository extends JpaRepository<JobExecution, String> {

    List<JobExecution> findByStatusAndScheduledTimeBefore(ExecutionStatus status, ZonedDateTime before);
    
    List<JobExecution> findByStatusAndRetryCountLessThanOrderByCreatedAtAsc(ExecutionStatus status, Integer maxRetryCount);

    @Query("SELECT je FROM JobExecution je WHERE je.jobId = :jobId AND je.status = 'FAILED' AND je.retryCount < :maxRetries ORDER BY je.createdAt ASC")
    List<JobExecution> findFailedExecutionsForRetry(@Param("jobId") String jobId, @Param("maxRetries") Integer maxRetries);

    Page<JobExecution> findByJobId(String jobId, Pageable pageable);

    List<JobExecution> findByStatus(ExecutionStatus status);
}
