package com.cardconnect.bolt.ai.model.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Prediction Accuracy Dashboard showing AI prediction performance metrics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredictionAccuracyReport {

    private LocalDate periodStart;
    private LocalDate periodEnd;
    private LocalDateTime generatedAt;

    // Overall Accuracy Metrics
    private int totalPredictions;
    private int evaluatedPredictions;
    private int correctPredictions;
    private int incorrectPredictions;
    private int pendingEvaluation;

    // Accuracy Rates
    private Double overallAccuracy;
    private Double precision;
    private Double recall;
    private Double f1Score;

    // Confidence Analysis
    private double averageConfidence;
    private double averageConfidenceCorrect;
    private double averageConfidenceIncorrect;
    private ConfidenceCalibration confidenceCalibration;

    // Accuracy by Status
    private Map<String, StatusAccuracy> accuracyByPredictedStatus;

    // Daily Accuracy Trend
    private List<DailyAccuracy> dailyAccuracyTrend;

    // Confusion Matrix
    private ConfusionMatrix confusionMatrix;

    // Model Performance Insights
    private List<String> insights;
    private List<String> recommendations;

    // Comparison: AI vs Rule-Based
    private ModelComparison modelComparison;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusAccuracy {
        private String status;
        private int totalPredictions;
        private int correctPredictions;
        private double accuracy;
        private double averageConfidence;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyAccuracy {
        private LocalDate date;
        private int totalPredictions;
        private int correctPredictions;
        private Double accuracy;
        private double averageConfidence;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfusionMatrix {
        // Predicted vs Actual counts
        private int trueHealthy;    // Predicted Healthy, Actual Healthy
        private int falseWarning;   // Predicted Warning, Actual Healthy
        private int falseCritical;  // Predicted Critical, Actual Healthy

        private int falseHealthy;   // Predicted Healthy, Actual Warning/Critical
        private int trueWarning;    // Predicted Warning, Actual Warning
        private int missedCritical; // Predicted Warning, Actual Critical

        private int missedWarning;  // Predicted Healthy, Actual Warning
        private int trueCritical;   // Predicted Critical, Actual Critical
        private int falseAlarm;     // Predicted Critical, Actual Healthy/Warning
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfidenceCalibration {
        // How well confidence scores correlate with accuracy
        private double calibrationScore; // 0-1, higher is better
        private List<ConfidenceBucket> buckets;
        private String calibrationAssessment;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfidenceBucket {
        private String confidenceRange; // e.g., "70-80%"
        private int predictionsCount;
        private double actualAccuracy;
        private double expectedAccuracy;
        private double calibrationError;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelComparison {
        private double aiAccuracy;
        private double ruleBasedAccuracy;
        private double aiAverageConfidence;
        private int aiPredictionCount;
        private int ruleBasedPredictionCount;
        private String winnerModel;
        private String comparisonSummary;
    }
}

