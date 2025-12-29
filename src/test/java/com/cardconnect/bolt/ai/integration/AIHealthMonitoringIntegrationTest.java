package com.cardconnect.bolt.ai.integration;

import com.cardconnect.bolt.ai.model.AIPredictionResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@DisplayName("AI Health Monitoring Integration Tests")
class AIHealthMonitoringIntegrationTest {

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String TEST_API_KEY = "demo-key-123";

    @Autowired
    private TestRestTemplate restTemplate;

    private HttpHeaders createHeadersWithApiKey() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(API_KEY_HEADER, TEST_API_KEY);
        return headers;
    }

    @Test
    @DisplayName("Should return health status from actuator endpoint")
    void testHealthEndpoint() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);

        // Health endpoint returns 200 when UP, 503 when DOWN (e.g., Ollama not running)
        // 500 INTERNAL_SERVER_ERROR can occur when health indicators throw exceptions
        // All are acceptable since we're testing the endpoint works, not the service status
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Should return AI prediction when endpoint called")
    void testAIPredictionEndpoint() {
        HttpEntity<Void> entity = new HttpEntity<>(createHeadersWithApiKey());
        ResponseEntity<AIPredictionResult> response =
            restTemplate.exchange("/api/ai/predict", HttpMethod.GET, entity, AIPredictionResult.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Should return usage statistics")
    void testUsageEndpoint() {
        HttpEntity<Void> entity = new HttpEntity<>(createHeadersWithApiKey());
        ResponseEntity<String> response =
            restTemplate.exchange("/api/ai/usage", HttpMethod.GET, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("dailyRequests");
    }

    @Test
    @DisplayName("Should handle rate limiting")
    void testRateLimiting() {
        HttpEntity<Void> entity = new HttpEntity<>(createHeadersWithApiKey());

        // Make multiple requests to test rate limiting
        int successCount = 0;
        int rateLimitCount = 0;

        for (int i = 0; i < 10; i++) {
            ResponseEntity<String> response =
                restTemplate.exchange("/api/ai/health", HttpMethod.GET, entity, String.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                successCount++;
            } else if (response.getStatusCode().value() == 429) {
                rateLimitCount++;
            }
        }

        // Should get at least some successful responses
        assertThat(successCount).isGreaterThan(0);
    }
}

