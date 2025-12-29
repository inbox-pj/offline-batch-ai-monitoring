package com.cardconnect.bolt.ai.model.accuracy;

import com.cardconnect.bolt.ai.model.HealthStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * A/B testing results comparing AI vs rule-based predictions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ABTestResult {

    private LocalDateTime testPeriodStart;
    private LocalDateTime testPeriodEnd;
    private String analysisWindow;

    // AI model performance
    private ModelPerformance aiModelPerformance;

    // Rule-based model performance
    private ModelPerformance ruleBasedPerformance;

    // Statistical comparison
    private Double accuracyDifference;  // AI - RuleBased (positive = AI better)
    private Double f1ScoreDifference;
    private Double confidenceDifference;

    // Statistical significance
    private Double pValue;
    private Boolean isStatisticallySignificant;
    private Double effectSize; // Cohen's d

    // Winner determination
    private String winner; // "AI", "RULE_BASED", or "TIE"
    private Double winMargin;

    // Recommendations
    private List<String> insights;
    private List<String> recommendations;

    // Breakdown by prediction type
    private Map<HealthStatus, StatusComparison> comparisonByStatus;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelPerformance {
        private String modelType; // "AI" or "RULE_BASED"
        private Long totalPredictions;
        private Long evaluatedPredictions;
        private Long correctPredictions;
        private Double accuracy;
        private Double precision;
        private Double recall;
        private Double f1Score;
        private Double averageConfidence;
        private Double averageResponseTimeMs;
        private Long errorCount; // Model failures

        // Per-status accuracy
        private Map<HealthStatus, Double> accuracyByStatus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusComparison {
        private HealthStatus status;
        private Double aiAccuracy;
        private Double ruleBasedAccuracy;
        private Double aiF1Score;
        private Double ruleBasedF1Score;
        private Long aiCount;
        private Long ruleBasedCount;
        private String betterModel;
    }
}

