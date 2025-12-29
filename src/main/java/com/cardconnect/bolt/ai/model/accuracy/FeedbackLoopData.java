package com.cardconnect.bolt.ai.model.accuracy;

import com.cardconnect.bolt.ai.model.HealthStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Feedback loop data for model improvement
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackLoopData {

    private LocalDateTime generatedAt;
    private String analysisWindow;

    // Error patterns - predictions that were wrong
    private List<ErrorPattern> errorPatterns;

    // High-confidence errors (most concerning)
    private List<HighConfidenceError> highConfidenceErrors;

    // Common misclassifications
    private List<MisclassificationPattern> misclassificationPatterns;

    // Feature importance insights
    private List<FeatureInsight> featureInsights;

    // Recommendations for improvement
    private List<ImprovementRecommendation> improvementRecommendations;

    // Model drift detection
    private DriftAnalysis driftAnalysis;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorPattern {
        private HealthStatus predicted;
        private HealthStatus actual;
        private Long occurrenceCount;
        private Double averageConfidence;
        private List<String> commonMetricPatterns;
        private String likelyCause;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HighConfidenceError {
        private Long predictionId;
        private LocalDateTime predictionTime;
        private HealthStatus predicted;
        private HealthStatus actual;
        private Double confidence;
        private String reasoning;
        private List<String> possibleCauses;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MisclassificationPattern {
        private HealthStatus fromStatus; // What was predicted
        private HealthStatus toStatus;   // What it actually was
        private Long count;
        private Double percentageOfErrors;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeatureInsight {
        private String featureName;
        private Double correlationWithError;
        private String insight;
        private String recommendation;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImprovementRecommendation {
        private String area; // "PROMPT", "THRESHOLDS", "FEATURES", "MODEL"
        private String priority; // "HIGH", "MEDIUM", "LOW"
        private String recommendation;
        private Double expectedImprovementPercent;
        private String rationale;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DriftAnalysis {
        private Boolean driftDetected;
        private Double driftScore; // 0 = no drift, 1 = severe drift
        private String driftType; // "CONCEPT_DRIFT", "DATA_DRIFT", "NONE"
        private LocalDateTime driftStartDate;
        private String description;
        private List<String> recommendations;
    }
}

