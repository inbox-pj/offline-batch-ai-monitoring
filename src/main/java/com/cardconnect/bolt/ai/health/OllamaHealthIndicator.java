package com.cardconnect.bolt.ai.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;

/**
 * Health indicator for Ollama service connectivity
 */
@Component
@Slf4j
public class OllamaHealthIndicator implements HealthIndicator {

    private final HttpClient httpClient;
    private final String ollamaBaseUrl;

    public OllamaHealthIndicator(@Value("${spring.ai.ollama.base-url:http://localhost:11434}") String ollamaBaseUrl) {
        this.ollamaBaseUrl = ollamaBaseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public Health health() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ollamaBaseUrl + "/api/tags"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return Health.up()
                        .withDetail("url", ollamaBaseUrl)
                        .withDetail("status", "reachable")
                        .withDetail("response_code", response.statusCode())
                        .build();
            } else {
                return Health.down()
                        .withDetail("url", ollamaBaseUrl)
                        .withDetail("status", "unreachable")
                        .withDetail("response_code", response.statusCode())
                        .build();
            }
        } catch (Exception e) {
            log.warn("Ollama health check failed: {}", e.getMessage());
            return Health.down()
                    .withDetail("url", ollamaBaseUrl)
                    .withDetail("status", "unreachable")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}

