package com.scheduler.service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Service
public class ApiClientService {

    private final RestTemplate restTemplate;


    public ApiClientService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Executes an HTTP request to an external API with an idempotency key.
     * * @param apiUrl The full URL of the API endpoint.
     * @param method The HTTP method (GET, POST, PUT, DELETE, etc.).
     * @param executionId The unique key for the X-Idempotency-Key header.
     * @param requestBody The object to be sent as the request body (can be null for GET/DELETE).
     * @return The response body as a String.
     */
    public int executeApiCall(
            String apiUrl,
            HttpMethod method,
            String executionId,
            Object requestBody) {

        try {
            // --- 1. Set Headers ---
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Idempotency-Key", executionId);
            if (method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH) {
                headers.setContentType(MediaType.APPLICATION_JSON);
            }

            HttpEntity<?> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response =
                    restTemplate.exchange(apiUrl, method, entity, String.class);

            return response.getStatusCode().value();

        } catch (HttpStatusCodeException e) {
            return e.getStatusCode().value();
        } catch (Exception e) {
                throw new RuntimeException("API call failed due to connection/network error: " + e.getMessage(), e);
        }
    }
}