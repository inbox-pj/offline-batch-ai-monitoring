package com.cardconnect.bolt.ai.model.report;

import com.cardconnect.bolt.ai.model.HealthStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Weekly Trend Report with week-over-week comparison
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyTrendReport {

    private LocalDate weekStartDate;
    private LocalDate weekEndDate;
    private LocalDateTime generatedAt;

    // Weekly Totals
    private long totalBatches;
    private long totalTransactionsProcessed;
    private long totalErrors;
    private double averageErrorRate;
    private double averageProcessingTimeMs;

    // Daily Breakdown
    private List<DailyMetricsSummary> dailyMetrics;

    // Trend Analysis
    private TrendDirection errorRateTrend;
    private TrendDirection processingTimeTrend;
    private TrendDirection volumeTrend;

    // Week-over-Week Comparison
    private WeekComparison weekOverWeekComparison;

    // Top Performers and Issues
    private List<MerchantPerformance> topPerformingMerchants;
    private List<MerchantPerformance> worstPerformingMerchants;

    // Prediction Accuracy for the Week
    private int totalPredictions;
    private int correctPredictions;
    private Double weeklyPredictionAccuracy;

    // Alerts Summary
    private int criticalAlertsCount;
    private int warningAlertsCount;
    private List<String> significantEvents;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyMetricsSummary {
        private LocalDate date;
        private long batchCount;
        private long transactionCount;
        private long errorCount;
        private double errorRate;
        private double avgProcessingTimeMs;
        private HealthStatus dominantHealthStatus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeekComparison {
        private double errorRateChange;
        private double processingTimeChange;
        private double volumeChange;
        private double batchCountChange;
        private String summary;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MerchantPerformance {
        private String merchId;
        private String hsn;
        private long totalTransactions;
        private long errorCount;
        private double errorRate;
        private double avgProcessingTimeMs;
        private double performanceScore;
    }

    public enum TrendDirection {
        IMPROVING,
        STABLE,
        DEGRADING,
        UNKNOWN
    }
}

