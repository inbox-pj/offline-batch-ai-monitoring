package com.cardconnect.bolt.ai.model.accuracy;

import com.cardconnect.bolt.ai.model.HealthStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Comprehensive accuracy metrics including precision, recall, F1 score
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccuracyMetrics {

    private LocalDateTime calculatedAt;
    private String analysisWindow;

    // Overall metrics
    private Long totalPredictions;
    private Long evaluatedPredictions;
    private Long correctPredictions;
    private Double overallAccuracy;
    private Double averageConfidence;

    // Classification metrics by status
    private Map<HealthStatus, ClassificationMetrics> metricsByStatus;

    // Weighted averages
    private Double weightedPrecision;
    private Double weightedRecall;
    private Double weightedF1Score;

    // Macro averages (unweighted)
    private Double macroPrecision;
    private Double macroRecall;
    private Double macroF1Score;

    // Confusion matrix
    private ConfusionMatrix confusionMatrix;

    // Trends
    private Double accuracyTrend;  // Positive = improving
    private Double confidenceCalibration; // How well confidence correlates with accuracy

    // Insights
    private java.util.List<String> insights;
    private java.util.List<String> recommendations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassificationMetrics {
        private HealthStatus status;
        private Long truePositives;
        private Long falsePositives;
        private Long falseNegatives;
        private Long trueNegatives;
        private Long support; // Total actual instances of this class

        private Double precision;
        private Double recall;
        private Double f1Score;
        private Double specificity;
        private Double accuracy;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfusionMatrix {
        // Rows = Predicted, Columns = Actual
        // [HEALTHY, WARNING, CRITICAL, UNKNOWN]
        private int[][] matrix;
        private java.util.List<HealthStatus> labels;

        public int get(HealthStatus predicted, HealthStatus actual) {
            int row = labels.indexOf(predicted);
            int col = labels.indexOf(actual);
            if (row >= 0 && col >= 0) {
                return matrix[row][col];
            }
            return 0;
        }
    }
}

