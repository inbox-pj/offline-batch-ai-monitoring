package com.cardconnect.bolt.ai.config.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Configuration properties for API security
 * All values are externalized - NO hardcoded defaults
 * Configure via application.properties with environment variable support
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "api")
public class ApiSecurityProperties {

    /**
     * Security configuration
     */
    @NotNull(message = "Security configuration is required")
    private SecurityConfig security;

    /**
     * Rate limit configuration
     */
    @NotNull(message = "Rate limit configuration is required")
    private RateLimitConfig rateLimit;

    @Data
    public static class SecurityConfig {
        /**
         * Enable API key authentication
         * Configure via: API_SECURITY_ENABLED
         */
        @NotNull(message = "Security enabled flag is required")
        private Boolean enabled;

        /**
         * Valid API keys - MUST be externalized via environment variables
         * Configure via: API_KEYS (comma-separated)
         */
        @NotEmpty(message = "API keys are required. Set via api.security.api-keys or API_KEYS")
        private List<String> apiKeys;
    }

    @Data
    public static class RateLimitConfig {
        /**
         * Enable rate limiting
         * Configure via: API_RATE_LIMIT_ENABLED
         */
        @NotNull(message = "Rate limit enabled flag is required")
        private Boolean enabled;

        /**
         * Maximum requests per minute per client
         * Configure via: API_RATE_LIMIT_RPM
         */
        @NotNull(message = "Requests per minute is required")
        @Min(1)
        @Max(10000)
        private Integer requestsPerMinute;
    }
}

