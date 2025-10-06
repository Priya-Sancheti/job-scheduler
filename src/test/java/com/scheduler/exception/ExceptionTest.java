package com.scheduler.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionTest {

    @Test
    void jobNotFoundException_WithMessage_ShouldCreateExceptionWithMessage() {
        // Given
        String message = "Job not found with ID: job-123";

        // When
        JobNotFoundException exception = new JobNotFoundException(message);

        // Then
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void jobNotFoundException_WithMessageAndCause_ShouldCreateExceptionWithBoth() {
        // Given
        String message = "Job not found with ID: job-123";
        RuntimeException cause = new RuntimeException("Database connection failed");

        // When
        JobNotFoundException exception = new JobNotFoundException(message, cause);

        // Then
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
        assertThat(exception.getCause().getMessage()).isEqualTo("Database connection failed");
    }

    @Test
    void jobNotFoundException_WithNullMessage_ShouldHandleNullMessage() {
        // When
        JobNotFoundException exception = new JobNotFoundException(null);

        // Then
        assertThat(exception.getMessage()).isNull();
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void jobNotFoundException_WithEmptyMessage_ShouldHandleEmptyMessage() {
        // When
        JobNotFoundException exception = new JobNotFoundException("");

        // Then
        assertThat(exception.getMessage()).isEqualTo("");
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void jobNotFoundException_WithNullCause_ShouldHandleNullCause() {
        // Given
        String message = "Job not found";

        // When
        JobNotFoundException exception = new JobNotFoundException(message, null);

        // Then
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void jobNotFoundException_ShouldInheritFromRuntimeException() {
        // Given
        String message = "Test exception";

        // When
        JobNotFoundException exception = new JobNotFoundException(message);

        // Then
        assertThat(exception).isInstanceOf(RuntimeException.class);
        assertThat(exception).isInstanceOf(JobNotFoundException.class);
    }

    @Test
    void jobNotFoundException_WithNestedCause_ShouldPreserveCauseChain() {
        // Given
        String message = "Job not found";
        RuntimeException nestedCause = new RuntimeException("Nested exception");
        RuntimeException cause = new RuntimeException("Outer exception", nestedCause);

        // When
        JobNotFoundException exception = new JobNotFoundException(message, cause);

        // Then
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
        assertThat(exception.getCause().getCause()).isEqualTo(nestedCause);
        assertThat(exception.getCause().getCause().getMessage()).isEqualTo("Nested exception");
    }


    @Test
    void jobNotFoundException_WithSpecialCharacters_ShouldHandleSpecialCharacters() {
        // Given
        String messageWithSpecialChars = "Job not found with ID: job-123@#$%^&*()_+-=[]{}|;':\",./<>?";

        // When
        JobNotFoundException exception = new JobNotFoundException(messageWithSpecialChars);

        // Then
        assertThat(exception.getMessage()).isEqualTo(messageWithSpecialChars);
    }

    @Test
    void jobNotFoundException_WithUnicodeCharacters_ShouldHandleUnicode() {
        // Given
        String messageWithUnicode = "Job not found with ID: job-123-测试-🚀";

        // When
        JobNotFoundException exception = new JobNotFoundException(messageWithUnicode);

        // Then
        assertThat(exception.getMessage()).isEqualTo(messageWithUnicode);
    }
}
