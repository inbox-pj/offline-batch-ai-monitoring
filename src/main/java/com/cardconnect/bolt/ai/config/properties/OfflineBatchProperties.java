package com.cardconnect.bolt.ai.config.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for offline batch monitoring
 * All values are externalized - NO hardcoded defaults
 * Configure via application.properties with environment variable support
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "offline.batch")
public class OfflineBatchProperties {

    /**
     * Health check configuration
     */
    @NotNull(message = "Health configuration is required")
    private HealthConfig health;

    /**
     * Process configuration
     */
    @NotNull(message = "Process configuration is required")
    private ProcessConfig process;

    @Data
    public static class HealthConfig {
        /**
         * Enable health monitoring
         * Configure via: OFFLINE_BATCH_HEALTH_ENABLED
         */
        @NotNull(message = "Health enabled flag is required")
        private Boolean enabled;

        /**
         * Threshold in hours for considering batch as old
         * Configure via: OFFLINE_BATCH_HEALTH_OLD_THRESHOLD_HOURS
         */
        @NotNull(message = "Old batch threshold hours is required")
        @Min(1)
        @Max(72)
        private Integer oldBatchThresholdHours;

        /**
         * Processor health configuration
         */
        @NotNull(message = "Processor health config is required")
        private ProcessorHealthConfig processor;

        @Data
        public static class ProcessorHealthConfig {
            @NotNull(message = "Processor health enabled flag is required")
            private Boolean enabled;
        }
    }

    @Data
    public static class ProcessConfig {
        /**
         * Period in seconds between batch processing
         * Configure via: OFFLINE_BATCH_PROCESS_PERIOD_SECONDS
         */
        @NotNull(message = "Process period seconds is required")
        @Min(10)
        @Max(3600)
        private Integer periodSeconds;

        /**
         * Authentication configuration
         */
        @NotNull(message = "Auth configuration is required")
        private AuthConfig auth;

        /**
         * Batch size
         * Configure via: OFFLINE_BATCH_PROCESS_BATCH_SIZE
         */
        @NotNull(message = "Batch size is required")
        @Min(1)
        @Max(1000)
        private Integer batchSize;

        @Data
        public static class AuthConfig {
            @NotNull(message = "Auth timeout seconds is required")
            @Min(10)
            @Max(600)
            private Integer timeoutSeconds;

            @NotNull(message = "Auth throughput limit is required")
            @Min(1)
            @Max(100)
            private Integer throughputLimit;
        }
    }
}

