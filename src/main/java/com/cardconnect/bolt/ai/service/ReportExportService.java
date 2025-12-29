package com.cardconnect.bolt.ai.service;

import com.cardconnect.bolt.ai.model.report.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service for exporting reports to various formats (CSV, PDF, etc.)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ReportExportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ========================================================================
    // DAILY SUMMARY EXPORT
    // ========================================================================

    /**
     * Export daily summary report to CSV format
     */
    public byte[] exportDailySummaryToCsv(DailySummaryReport report) {
        log.info("Exporting daily summary report to CSV for date: {}", report.getReportDate());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(baos);

        // Header section
        writer.println("Daily Summary Report");
        writer.println("Report Date," + report.getReportDate().format(DATE_FORMATTER));
        writer.println("Generated At," + report.getGeneratedAt().format(DATETIME_FORMATTER));
        writer.println();

        // Overall Statistics
        writer.println("OVERALL STATISTICS");
        writer.println("Metric,Value");
        writer.println("Total Batches," + report.getTotalBatches());
        writer.println("Total Transactions Processed," + report.getTotalTransactionsProcessed());
        writer.println("Total Errors," + report.getTotalErrors());
        writer.println("Error Rate," + String.format("%.4f", report.getErrorRate()));
        writer.println("Error Rate %," + String.format("%.2f%%", report.getErrorRate() * 100));
        writer.println("Average Processing Time (ms)," + String.format("%.2f", report.getAverageProcessingTimeMs()));
        writer.println("Max Processing Time (ms)," + String.format("%.2f", report.getMaxProcessingTimeMs()));
        writer.println("Min Processing Time (ms)," + String.format("%.2f", report.getMinProcessingTimeMs()));
        writer.println("Overall Health Status," + report.getOverallHealthStatus());
        writer.println();

        // Prediction Statistics
        writer.println("PREDICTION STATISTICS");
        writer.println("Metric,Value");
        writer.println("Total Predictions," + report.getTotalPredictions());
        writer.println("Average Prediction Confidence," + String.format("%.2f", report.getAveragePredictionConfidence()));
        writer.println("Correct Predictions," + report.getCorrectPredictions());
        writer.println("Incorrect Predictions," + report.getIncorrectPredictions());
        if (report.getPredictionAccuracyRate() != null) {
            writer.println("Prediction Accuracy Rate," + String.format("%.2f%%", report.getPredictionAccuracyRate()));
        }
        writer.println();

        // Comparison with Previous Day
        writer.println("COMPARISON WITH PREVIOUS DAY");
        writer.println("Metric,Change %");
        if (report.getErrorRateChange() != null) {
            writer.println("Error Rate Change," + String.format("%.2f%%", report.getErrorRateChange()));
        }
        if (report.getProcessingTimeChange() != null) {
            writer.println("Processing Time Change," + String.format("%.2f%%", report.getProcessingTimeChange()));
        }
        if (report.getBatchCountChange() != null) {
            writer.println("Batch Count Change," + String.format("%.2f%%", report.getBatchCountChange()));
        }
        writer.println();

        // Top Merchants by Errors
        if (report.getTopMerchantsByErrors() != null && !report.getTopMerchantsByErrors().isEmpty()) {
            writer.println("TOP MERCHANTS BY ERRORS");
            writer.println("Merchant ID,HSN,Error Count,Error Rate,Total Transactions");
            for (DailySummaryReport.MerchantIssue merchant : report.getTopMerchantsByErrors()) {
                writer.println(String.format("%s,%s,%d,%.4f,%d",
                        merchant.getMerchId(),
                        merchant.getHsn(),
                        merchant.getErrorCount(),
                        merchant.getErrorRate(),
                        merchant.getTotalTransactions()));
            }
            writer.println();
        }

        // Errors by Hour
        if (report.getErrorsByHour() != null && !report.getErrorsByHour().isEmpty()) {
            writer.println("ERRORS BY HOUR");
            writer.println("Hour,Error Count");
            report.getErrorsByHour().entrySet().stream()
                    .sorted(java.util.Map.Entry.comparingByKey())
                    .forEach(entry -> writer.println(String.format("%02d:00,%d", entry.getKey(), entry.getValue())));
            writer.println();
        }

        // Alerts
        if (report.getCriticalAlerts() != null && !report.getCriticalAlerts().isEmpty()) {
            writer.println("CRITICAL ALERTS");
            report.getCriticalAlerts().forEach(alert -> writer.println("CRITICAL," + alert));
            writer.println();
        }
        if (report.getWarnings() != null && !report.getWarnings().isEmpty()) {
            writer.println("WARNINGS");
            report.getWarnings().forEach(warning -> writer.println("WARNING," + warning));
        }

        writer.flush();
        return baos.toByteArray();
    }

    // ========================================================================
    // WEEKLY TREND EXPORT
    // ========================================================================

    /**
     * Export weekly trend report to CSV format
     */
    public byte[] exportWeeklyTrendToCsv(WeeklyTrendReport report) {
        log.info("Exporting weekly trend report to CSV for week starting: {}", report.getWeekStartDate());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(baos);

        // Header section
        writer.println("Weekly Trend Report");
        writer.println("Week Start," + report.getWeekStartDate().format(DATE_FORMATTER));
        writer.println("Week End," + report.getWeekEndDate().format(DATE_FORMATTER));
        writer.println("Generated At," + report.getGeneratedAt().format(DATETIME_FORMATTER));
        writer.println();

        // Weekly Totals
        writer.println("WEEKLY TOTALS");
        writer.println("Metric,Value");
        writer.println("Total Batches," + report.getTotalBatches());
        writer.println("Total Transactions Processed," + report.getTotalTransactionsProcessed());
        writer.println("Total Errors," + report.getTotalErrors());
        writer.println("Average Error Rate," + String.format("%.4f", report.getAverageErrorRate()));
        writer.println("Average Processing Time (ms)," + String.format("%.2f", report.getAverageProcessingTimeMs()));
        writer.println();

        // Trend Analysis
        writer.println("TREND ANALYSIS");
        writer.println("Metric,Trend");
        writer.println("Error Rate Trend," + report.getErrorRateTrend());
        writer.println("Processing Time Trend," + report.getProcessingTimeTrend());
        writer.println("Volume Trend," + report.getVolumeTrend());
        writer.println();

        // Daily Breakdown
        if (report.getDailyMetrics() != null && !report.getDailyMetrics().isEmpty()) {
            writer.println("DAILY BREAKDOWN");
            writer.println("Date,Batch Count,Transaction Count,Error Count,Error Rate,Avg Processing Time (ms),Health Status");
            for (WeeklyTrendReport.DailyMetricsSummary daily : report.getDailyMetrics()) {
                writer.println(String.format("%s,%d,%d,%d,%.4f,%.2f,%s",
                        daily.getDate().format(DATE_FORMATTER),
                        daily.getBatchCount(),
                        daily.getTransactionCount(),
                        daily.getErrorCount(),
                        daily.getErrorRate(),
                        daily.getAvgProcessingTimeMs(),
                        daily.getDominantHealthStatus()));
            }
            writer.println();
        }

        // Week-over-Week Comparison
        if (report.getWeekOverWeekComparison() != null) {
            writer.println("WEEK-OVER-WEEK COMPARISON");
            writer.println("Metric,Change %");
            writer.println("Error Rate Change," + String.format("%.2f%%", report.getWeekOverWeekComparison().getErrorRateChange()));
            writer.println("Processing Time Change," + String.format("%.2f%%", report.getWeekOverWeekComparison().getProcessingTimeChange()));
            writer.println("Volume Change," + String.format("%.2f%%", report.getWeekOverWeekComparison().getVolumeChange()));
            writer.println("Batch Count Change," + String.format("%.2f%%", report.getWeekOverWeekComparison().getBatchCountChange()));
            writer.println("Summary," + report.getWeekOverWeekComparison().getSummary());
            writer.println();
        }

        // Top Performing Merchants
        if (report.getTopPerformingMerchants() != null && !report.getTopPerformingMerchants().isEmpty()) {
            writer.println("TOP PERFORMING MERCHANTS");
            writer.println("Merchant ID,HSN,Total Transactions,Error Count,Error Rate,Avg Processing Time (ms),Performance Score");
            for (WeeklyTrendReport.MerchantPerformance merchant : report.getTopPerformingMerchants()) {
                writer.println(String.format("%s,%s,%d,%d,%.4f,%.2f,%.2f",
                        merchant.getMerchId(),
                        merchant.getHsn(),
                        merchant.getTotalTransactions(),
                        merchant.getErrorCount(),
                        merchant.getErrorRate(),
                        merchant.getAvgProcessingTimeMs(),
                        merchant.getPerformanceScore()));
            }
            writer.println();
        }

        // Worst Performing Merchants
        if (report.getWorstPerformingMerchants() != null && !report.getWorstPerformingMerchants().isEmpty()) {
            writer.println("WORST PERFORMING MERCHANTS");
            writer.println("Merchant ID,HSN,Total Transactions,Error Count,Error Rate,Avg Processing Time (ms),Performance Score");
            for (WeeklyTrendReport.MerchantPerformance merchant : report.getWorstPerformingMerchants()) {
                writer.println(String.format("%s,%s,%d,%d,%.4f,%.2f,%.2f",
                        merchant.getMerchId(),
                        merchant.getHsn(),
                        merchant.getTotalTransactions(),
                        merchant.getErrorCount(),
                        merchant.getErrorRate(),
                        merchant.getAvgProcessingTimeMs(),
                        merchant.getPerformanceScore()));
            }
            writer.println();
        }

        // Prediction Accuracy
        writer.println("PREDICTION ACCURACY");
        writer.println("Metric,Value");
        writer.println("Total Predictions," + report.getTotalPredictions());
        writer.println("Correct Predictions," + report.getCorrectPredictions());
        if (report.getWeeklyPredictionAccuracy() != null) {
            writer.println("Weekly Prediction Accuracy," + String.format("%.2f%%", report.getWeeklyPredictionAccuracy()));
        }

        writer.flush();
        return baos.toByteArray();
    }

    // ========================================================================
    // MERCHANT SCORECARD EXPORT
    // ========================================================================

    /**
     * Export merchant scorecard to CSV format
     */
    public byte[] exportMerchantScorecardToCsv(MerchantScorecard scorecard) {
        log.info("Exporting merchant scorecard to CSV for: {}:{}", scorecard.getMerchId(), scorecard.getHsn());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(baos);

        // Header section
        writer.println("Merchant Scorecard");
        writer.println("Merchant ID," + scorecard.getMerchId());
        writer.println("HSN," + scorecard.getHsn());
        writer.println("Period Start," + scorecard.getPeriodStart().format(DATE_FORMATTER));
        writer.println("Period End," + scorecard.getPeriodEnd().format(DATE_FORMATTER));
        writer.println("Generated At," + scorecard.getGeneratedAt().format(DATETIME_FORMATTER));
        writer.println();

        // Overall Score
        writer.println("OVERALL SCORE");
        writer.println("Metric,Value");
        writer.println("Overall Score," + String.format("%.2f", scorecard.getOverallScore()));
        writer.println("Health Status," + scorecard.getHealthStatus());
        writer.println("Health Grade," + scorecard.getHealthGrade());
        writer.println("Risk Level," + scorecard.getRiskLevel());
        writer.println("Overall Trend," + scorecard.getOverallTrend());
        writer.println();

        // Key Metrics
        writer.println("KEY METRICS");
        writer.println("Metric,Value");
        writer.println("Total Batches," + scorecard.getTotalBatches());
        writer.println("Total Transactions Processed," + scorecard.getTotalTransactionsProcessed());
        writer.println("Total Errors," + scorecard.getTotalErrors());
        writer.println("Error Rate," + String.format("%.4f", scorecard.getErrorRate()));
        writer.println("Error Rate %," + String.format("%.2f%%", scorecard.getErrorRate() * 100));
        writer.println("Average Processing Time (ms)," + String.format("%.2f", scorecard.getAverageProcessingTimeMs()));
        writer.println();

        // Score Breakdown
        if (scorecard.getScoreBreakdown() != null) {
            writer.println("SCORE BREAKDOWN");
            writer.println("Component,Score,Max Score");
            writer.println("Error Rate Score," + String.format("%.2f", scorecard.getScoreBreakdown().getErrorRateScore()) + ",25");
            writer.println("Processing Time Score," + String.format("%.2f", scorecard.getScoreBreakdown().getProcessingTimeScore()) + ",25");
            writer.println("Reliability Score," + String.format("%.2f", scorecard.getScoreBreakdown().getReliabilityScore()) + ",25");
            writer.println("Volume Score," + String.format("%.2f", scorecard.getScoreBreakdown().getVolumeScore()) + ",25");
            writer.println();
        }

        // Peer Comparison
        if (scorecard.getPeerComparison() != null) {
            writer.println("PEER COMPARISON");
            writer.println("Metric,Value");
            writer.println("Percentile Rank," + String.format("%.2f", scorecard.getPeerComparison().getPercentileRank()));
            writer.println("Total Merchants Compared," + scorecard.getPeerComparison().getTotalMerchantsCompared());
            writer.println("Average Peer Score," + String.format("%.2f", scorecard.getPeerComparison().getAveragePeerScore()));
            writer.println("Score Difference from Average," + String.format("%.2f", scorecard.getPeerComparison().getScoreDifferenceFromAverage()));
            writer.println("Comparison Summary," + scorecard.getPeerComparison().getComparisonSummary());
            writer.println();
        }

        // Daily Scores
        if (scorecard.getDailyScores() != null && !scorecard.getDailyScores().isEmpty()) {
            writer.println("DAILY SCORES");
            writer.println("Date,Score,Error Rate,Avg Processing Time (ms),Batch Count");
            for (MerchantScorecard.DailyScore daily : scorecard.getDailyScores()) {
                writer.println(String.format("%s,%.2f,%.4f,%.2f,%d",
                        daily.getDate().format(DATE_FORMATTER),
                        daily.getScore(),
                        daily.getErrorRate(),
                        daily.getAvgProcessingTimeMs(),
                        daily.getBatchCount()));
            }
            writer.println();
        }

        // Risk Factors
        if (scorecard.getRiskFactors() != null && !scorecard.getRiskFactors().isEmpty()) {
            writer.println("RISK FACTORS");
            scorecard.getRiskFactors().forEach(writer::println);
            writer.println();
        }

        // Recommendations
        if (scorecard.getRecommendations() != null && !scorecard.getRecommendations().isEmpty()) {
            writer.println("RECOMMENDATIONS");
            scorecard.getRecommendations().forEach(writer::println);
        }

        writer.flush();
        return baos.toByteArray();
    }

    /**
     * Export all merchant scorecards to CSV format
     */
    public byte[] exportAllMerchantScorecardsToCsv(List<MerchantScorecard> scorecards) {
        log.info("Exporting {} merchant scorecards to CSV", scorecards.size());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(baos);

        writer.println("All Merchants Scorecard Summary");
        writer.println("Generated At," + java.time.LocalDateTime.now().format(DATETIME_FORMATTER));
        writer.println("Total Merchants," + scorecards.size());
        writer.println();

        writer.println("MERCHANT SCORECARDS");
        writer.println("Rank,Merchant ID,HSN,Overall Score,Health Grade,Health Status,Risk Level,Error Rate,Avg Processing Time (ms),Total Transactions,Total Errors");

        int rank = 1;
        for (MerchantScorecard scorecard : scorecards) {
            writer.println(String.format("%d,%s,%s,%.2f,%s,%s,%s,%.4f,%.2f,%d,%d",
                    rank++,
                    scorecard.getMerchId(),
                    scorecard.getHsn(),
                    scorecard.getOverallScore(),
                    scorecard.getHealthGrade(),
                    scorecard.getHealthStatus(),
                    scorecard.getRiskLevel(),
                    scorecard.getErrorRate(),
                    scorecard.getAverageProcessingTimeMs(),
                    scorecard.getTotalTransactionsProcessed(),
                    scorecard.getTotalErrors()));
        }

        writer.flush();
        return baos.toByteArray();
    }

    // ========================================================================
    // PREDICTION ACCURACY EXPORT
    // ========================================================================

    /**
     * Export prediction accuracy report to CSV format
     */
    public byte[] exportPredictionAccuracyToCsv(PredictionAccuracyReport report) {
        log.info("Exporting prediction accuracy report to CSV");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(baos);

        // Header section
        writer.println("Prediction Accuracy Report");
        writer.println("Period Start," + report.getPeriodStart().format(DATE_FORMATTER));
        writer.println("Period End," + report.getPeriodEnd().format(DATE_FORMATTER));
        writer.println("Generated At," + report.getGeneratedAt().format(DATETIME_FORMATTER));
        writer.println();

        // Overall Accuracy Metrics
        writer.println("OVERALL ACCURACY METRICS");
        writer.println("Metric,Value");
        writer.println("Total Predictions," + report.getTotalPredictions());
        writer.println("Evaluated Predictions," + report.getEvaluatedPredictions());
        writer.println("Correct Predictions," + report.getCorrectPredictions());
        writer.println("Incorrect Predictions," + report.getIncorrectPredictions());
        writer.println("Pending Evaluation," + report.getPendingEvaluation());
        if (report.getOverallAccuracy() != null) {
            writer.println("Overall Accuracy," + String.format("%.2f%%", report.getOverallAccuracy()));
        }
        writer.println();

        // Confidence Analysis
        writer.println("CONFIDENCE ANALYSIS");
        writer.println("Metric,Value");
        writer.println("Average Confidence," + String.format("%.4f", report.getAverageConfidence()));
        writer.println("Average Confidence (Correct)," + String.format("%.4f", report.getAverageConfidenceCorrect()));
        writer.println("Average Confidence (Incorrect)," + String.format("%.4f", report.getAverageConfidenceIncorrect()));
        writer.println();

        // Accuracy by Status
        if (report.getAccuracyByPredictedStatus() != null && !report.getAccuracyByPredictedStatus().isEmpty()) {
            writer.println("ACCURACY BY PREDICTED STATUS");
            writer.println("Status,Total Predictions,Correct Predictions,Accuracy,Average Confidence");
            for (PredictionAccuracyReport.StatusAccuracy status : report.getAccuracyByPredictedStatus().values()) {
                writer.println(String.format("%s,%d,%d,%.2f%%,%.4f",
                        status.getStatus(),
                        status.getTotalPredictions(),
                        status.getCorrectPredictions(),
                        status.getAccuracy(),
                        status.getAverageConfidence()));
            }
            writer.println();
        }

        // Daily Accuracy Trend
        if (report.getDailyAccuracyTrend() != null && !report.getDailyAccuracyTrend().isEmpty()) {
            writer.println("DAILY ACCURACY TREND");
            writer.println("Date,Total Predictions,Correct Predictions,Accuracy,Average Confidence");
            for (PredictionAccuracyReport.DailyAccuracy daily : report.getDailyAccuracyTrend()) {
                writer.println(String.format("%s,%d,%d,%s,%.4f",
                        daily.getDate().format(DATE_FORMATTER),
                        daily.getTotalPredictions(),
                        daily.getCorrectPredictions(),
                        daily.getAccuracy() != null ? String.format("%.2f%%", daily.getAccuracy()) : "N/A",
                        daily.getAverageConfidence()));
            }
            writer.println();
        }

        // Insights
        if (report.getInsights() != null && !report.getInsights().isEmpty()) {
            writer.println("INSIGHTS");
            report.getInsights().forEach(writer::println);
            writer.println();
        }

        // Recommendations
        if (report.getRecommendations() != null && !report.getRecommendations().isEmpty()) {
            writer.println("RECOMMENDATIONS");
            report.getRecommendations().forEach(writer::println);
        }

        writer.flush();
        return baos.toByteArray();
    }
}

