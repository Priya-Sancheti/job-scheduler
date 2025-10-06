package com.scheduler.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record JobSpec(
    @NotBlank(message = "Schedule cannot be blank")
    @Pattern(regexp = "^\\s*\\S+\\s+\\S+\\s+\\S+\\s+\\S+\\s+\\S+\\s+\\S+\\s*$", 
             message = "Schedule must be a valid 6-part CRON expression")
    String schedule,
    
    @NotBlank(message = "API URL cannot be blank")
    String apiUrl,
    
    @NotBlank(message = "Type cannot be blank")
    String type
) {}
