package com.cardconnect.bolt.ai.model.report;

import com.cardconnect.bolt.ai.model.HealthStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Daily Summary Report containing key metrics for a specific day
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailySummaryReport {

    private LocalDate reportDate;
    private LocalDateTime generatedAt;

    // Overall Statistics
    private long totalBatches;
    private long totalTransactionsProcessed;
    private long totalErrors;
    private double errorRate;
    private double averageProcessingTimeMs;
    private double maxProcessingTimeMs;
    private double minProcessingTimeMs;

    // Health Status Summary
    private HealthStatus overallHealthStatus;
    private int healthyPeriods;
    private int warningPeriods;
    private int criticalPeriods;

    // Prediction Statistics
    private int totalPredictions;
    private double averagePredictionConfidence;
    private int correctPredictions;
    private int incorrectPredictions;
    private Double predictionAccuracyRate;

    // Top Issues
    private List<MerchantIssue> topMerchantsByErrors;
    private List<String> criticalAlerts;
    private List<String> warnings;

    // Time-based Analysis
    private Map<Integer, Long> errorsByHour;
    private Map<Integer, Double> avgProcessingTimeByHour;

    // Comparison with Previous Day
    private Double errorRateChange;
    private Double processingTimeChange;
    private Double batchCountChange;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MerchantIssue {
        private String merchId;
        private String hsn;
        private long errorCount;
        private double errorRate;
        private long totalTransactions;
    }
}

