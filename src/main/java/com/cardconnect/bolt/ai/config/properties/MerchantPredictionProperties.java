package com.cardconnect.bolt.ai.config.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for merchant-specific predictions
 * Externalized via application.properties with environment variable support
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "ai.merchant")
public class MerchantPredictionProperties {

    /**
     * Enable or disable merchant-specific predictions
     */
    private Boolean predictionsEnabled = true;

    /**
     * Default threshold configuration for merchants without custom thresholds
     */
    private DefaultThresholds defaultThresholds = new DefaultThresholds();

    /**
     * Risk score calculation weights
     */
    private RiskWeights riskWeights = new RiskWeights();

    /**
     * Cache configuration
     */
    private CacheConfig cache = new CacheConfig();

    @Data
    public static class DefaultThresholds {
        /**
         * Default error rate warning threshold (0.02 = 2%)
         */
        @Min(0) @Max(1)
        private Double errorRateWarning = 0.02;

        /**
         * Default error rate critical threshold (0.05 = 5%)
         */
        @Min(0) @Max(1)
        private Double errorRateCritical = 0.05;

        /**
         * Default processing time warning threshold in milliseconds
         */
        @Min(0)
        private Long processingTimeWarningMs = 5000L;

        /**
         * Default processing time critical threshold in milliseconds
         */
        @Min(0)
        private Long processingTimeCriticalMs = 10000L;

        /**
         * Default risk score warning threshold
         */
        @Min(0) @Max(1)
        private Double riskScoreWarning = 0.4;

        /**
         * Default risk score critical threshold
         */
        @Min(0) @Max(1)
        private Double riskScoreCritical = 0.7;
    }

    @Data
    public static class RiskWeights {
        /**
         * Weight for error rate in risk calculation
         */
        @Min(0) @Max(1)
        private Double errorRateWeight = 0.5;

        /**
         * Weight for processing time in risk calculation
         */
        @Min(0) @Max(1)
        private Double processingTimeWeight = 0.3;

        /**
         * Weight for trend analysis in risk calculation
         */
        @Min(0) @Max(1)
        private Double trendWeight = 0.1;

        /**
         * Weight for volume changes in risk calculation
         */
        @Min(0) @Max(1)
        private Double volumeWeight = 0.1;

        /**
         * Validate that weights sum to 1.0
         */
        public boolean isValid() {
            double sum = errorRateWeight + processingTimeWeight + trendWeight + volumeWeight;
            return Math.abs(sum - 1.0) < 0.001;
        }
    }

    @Data
    public static class CacheConfig {
        /**
         * Cache TTL in minutes for merchant predictions
         */
        @Min(1)
        private Integer ttlMinutes = 5;
    }
}

