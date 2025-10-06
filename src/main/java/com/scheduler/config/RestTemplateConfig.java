package com.scheduler.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {
    @Bean
    public RestTemplate restTemplate() {
        // Configure timeouts and other settings here for a robust client
        return new RestTemplateBuilder()
                .setConnectTimeout(Duration.ofSeconds(5)) // 5 seconds to establish connection
                .setReadTimeout(Duration.ofSeconds(90))  // 90 seconds to wait for data
                .build();
    }
}
