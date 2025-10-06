package com.scheduler.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app")
public class ApplicationProperties {
    
    private Executor executor = new Executor();
    private Job job = new Job();
    
    @Data
    public static class Executor {
        private int corePoolSize = 5;
        private int maxPoolSize = 200;
        private int queueCapacity = 1000;
    }
    
    @Data
    public static class Job {
        private HttpClient httpClient = new HttpClient();
        private Recovery recovery = new Recovery();
        private Retry retry = new Retry();
        
        @Data
        public static class HttpClient {
            private int timeoutSeconds = 95;
        }
        
        @Data
        public static class Recovery {
            private int staleTimeoutSeconds = 100;
        }
        
        @Data
        public static class Retry {
            private int maxAttempts = 5;
            private long initialDelayMs = 1000;
            private double multiplier = 2.0;
        }
    }
}
