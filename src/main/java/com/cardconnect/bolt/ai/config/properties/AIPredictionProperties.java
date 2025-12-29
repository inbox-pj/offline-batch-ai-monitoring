package com.cardconnect.bolt.ai.config.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for AI prediction settings
 * All values are externalized - NO hardcoded defaults
 * Configure via application.properties with environment variable support
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "ai.prediction")
public class AIPredictionProperties {

    /**
     * Enable or disable AI predictions
     * Configure via: AI_PREDICTION_ENABLED
     */
    @NotNull(message = "AI prediction enabled flag is required. Set via ai.prediction.enabled or AI_PREDICTION_ENABLED")
    private Boolean enabled;

    /**
     * Analysis window in hours (how far back to look)
     * Configure via: AI_ANALYSIS_WINDOW_HOURS
     */
    @NotNull(message = "Analysis window hours is required")
    @Min(1)
    @Max(168)
    private Integer analysisWindowHours;

    /**
     * Forecast horizon in hours (how far ahead to predict)
     * Configure via: AI_FORECAST_HORIZON_HOURS
     */
    @NotNull(message = "Forecast horizon hours is required")
    @Min(1)
    @Max(48)
    private Integer forecastHorizonHours;

    /**
     * Confidence threshold for predictions (0.0 to 1.0)
     * Configure via: AI_CONFIDENCE_THRESHOLD
     */
    @NotNull(message = "Confidence threshold is required")
    @Min(0)
    @Max(1)
    private Double confidenceThreshold;

    /**
     * Maximum daily requests to AI service
     * Configure via: AI_MAX_DAILY_REQUESTS
     */
    @NotNull(message = "Max daily requests is required")
    @Min(1)
    private Integer maxDailyRequests;

    /**
     * Maximum daily cost in cents
     * Configure via: AI_MAX_DAILY_COST_CENTS
     */
    @NotNull(message = "Max daily cost cents is required")
    @Min(0)
    private Integer maxDailyCostCents;

    /**
     * Cache configuration
     */
    @NotNull(message = "Cache configuration is required")
    private CacheConfig cache;

    /**
     * Fallback configuration
     */
    @NotNull(message = "Fallback configuration is required")
    private FallbackConfig fallback;

    @Data
    public static class CacheConfig {
        @NotNull(message = "Cache enabled flag is required")
        private Boolean enabled;

        @NotNull(message = "Cache TTL minutes is required")
        @Min(1)
        @Max(60)
        private Integer ttlMinutes;
    }

    @Data
    public static class FallbackConfig {
        @NotNull(message = "Fallback enabled flag is required")
        private Boolean enabled;

        @NotNull(message = "Rule based confidence is required")
        @Min(0)
        @Max(1)
        private Double ruleBasedConfidence;
    }
}

