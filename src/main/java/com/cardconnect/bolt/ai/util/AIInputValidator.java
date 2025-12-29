package com.cardconnect.bolt.ai.util;

import com.cardconnect.bolt.ai.model.BatchMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Enhanced input validator with prompt injection detection
 * All thresholds and limits are configurable
 */
@Component
@Slf4j
public class AIInputValidator {

    @Value("${ai.validation.max-metrics-count:10000}")
    private int maxMetricsCount;

    @Value("${ai.validation.max-prompt-length:50000}")
    private int maxPromptLength;

    @Value("${ai.validation.max-chat-input-length:5000}")
    private int maxChatInputLength;

    @Value("${ai.validation.injection-detection.enabled:true}")
    private boolean injectionDetectionEnabled;

    // Patterns to detect prompt injection attempts - configurable via properties
    private static final Pattern INJECTION_PATTERN = Pattern.compile(
            "(ignore previous|disregard|forget|new instructions|system:|admin:|root:|" +
                    "override|bypass|disable|jailbreak|pretend you are|act as if|role play)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern SUSPICIOUS_PATTERN = Pattern.compile(
            "(<script|javascript:|onerror=|onclick=|<iframe|eval\\(|exec\\()",
            Pattern.CASE_INSENSITIVE
    );

    public void validateMetrics(List<BatchMetrics> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            throw new IllegalArgumentException("Metrics cannot be null or empty");
        }

        if (metrics.size() > maxMetricsCount) {
            throw new IllegalArgumentException("Metrics count exceeds maximum: " + maxMetricsCount);
        }

        metrics.forEach(this::validateSingleMetric);
    }

    private void validateSingleMetric(BatchMetrics metric) {
        if (metric.getProcessingTimeMs() < 0) {
            throw new IllegalArgumentException("Processing time cannot be negative");
        }

        if (metric.getErrorCount() < 0) {
            throw new IllegalArgumentException("Error count cannot be negative");
        }

        if (metric.getMerchId() != null) {
            metric.setMerchId(sanitize(metric.getMerchId()));
        }

        if (metric.getHsn() != null) {
            metric.setHsn(sanitize(metric.getHsn()));
        }
    }

    private String sanitize(String input) {
        return input.replaceAll("[^a-zA-Z0-9-_:]", "");
    }

    public void validatePrompt(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new IllegalArgumentException("Prompt cannot be null or empty");
        }

        if (prompt.length() > maxPromptLength) {
            throw new IllegalArgumentException("Prompt exceeds maximum length: " + maxPromptLength);
        }

        if (injectionDetectionEnabled) {
            checkForInjection(prompt);
        }
    }

    private void checkForInjection(String input) {
        // Check for prompt injection attempts
        if (INJECTION_PATTERN.matcher(input).find()) {
            log.warn("Potential prompt injection detected in input");
            throw new SecurityException("Potential prompt injection detected");
        }

        // Check for suspicious content
        if (SUSPICIOUS_PATTERN.matcher(input).find()) {
            log.warn("Suspicious content detected in input");
            throw new SecurityException("Suspicious content detected");
        }
    }

    /**
     * Validate user chat input for prompt injection
     */
    public void validateChatInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Chat input cannot be null or empty");
        }

        if (input.length() > maxChatInputLength) {
            throw new IllegalArgumentException("Chat input too long (max " + maxChatInputLength + " characters)");
        }

        validatePrompt(input);
    }
}

