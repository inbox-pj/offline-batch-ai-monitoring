package com.cardconnect.bolt.ai.service;

import com.cardconnect.bolt.ai.model.AIPredictionAudit;
import com.cardconnect.bolt.ai.model.BatchMetrics;
import com.cardconnect.bolt.ai.model.HealthStatus;
import com.cardconnect.bolt.ai.model.report.*;
import com.cardconnect.bolt.ai.model.report.WeeklyTrendReport.TrendDirection;
import com.cardconnect.bolt.ai.repository.AIPredictionAuditRepository;
import com.cardconnect.bolt.ai.repository.OfflineBatchMetricsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for generating analytics and reports
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsReportingService {

    private final OfflineBatchMetricsRepository metricsRepository;
    private final AIPredictionAuditRepository predictionAuditRepository;

    private static final double ERROR_RATE_WARNING_THRESHOLD = 0.05;
    private static final double ERROR_RATE_CRITICAL_THRESHOLD = 0.10;

    // ========================================================================
    // DAILY SUMMARY REPORT
    // ========================================================================

    /**
     * Generate daily summary report for a specific date
     */
    public DailySummaryReport generateDailySummary(LocalDate date) {
        log.info("Generating daily summary report for: {}", date);

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        List<BatchMetrics> dayMetrics = metricsRepository.findByTimestampBetween(startOfDay, endOfDay);
        List<AIPredictionAudit> dayPredictions = predictionAuditRepository.findByPredictionTimeAfter(startOfDay)
                .stream()
                .filter(p -> p.getPredictionTime().isBefore(endOfDay))
                .toList();

        // Get previous day for comparison
        LocalDateTime prevDayStart = date.minusDays(1).atStartOfDay();
        LocalDateTime prevDayEnd = date.minusDays(1).atTime(LocalTime.MAX);
        List<BatchMetrics> prevDayMetrics = metricsRepository.findByTimestampBetween(prevDayStart, prevDayEnd);

        return buildDailySummary(date, dayMetrics, dayPredictions, prevDayMetrics);
    }

    private DailySummaryReport buildDailySummary(LocalDate date, List<BatchMetrics> metrics,
                                                  List<AIPredictionAudit> predictions,
                                                  List<BatchMetrics> prevDayMetrics) {
        if (metrics.isEmpty()) {
            return DailySummaryReport.builder()
                    .reportDate(date)
                    .generatedAt(LocalDateTime.now())
                    .totalBatches(0)
                    .overallHealthStatus(HealthStatus.UNKNOWN)
                    .build();
        }

        // Calculate basic stats
        long totalBatches = metrics.size();
        long totalProcessed = metrics.stream().mapToLong(BatchMetrics::getProcessedCount).sum();
        long totalErrors = metrics.stream().mapToLong(BatchMetrics::getErrorCount).sum();
        double errorRate = totalProcessed > 0 ? (double) totalErrors / totalProcessed : 0;

        DoubleSummaryStatistics processingStats = metrics.stream()
                .mapToDouble(BatchMetrics::getProcessingTimeMs)
                .summaryStatistics();

        // Determine health status
        HealthStatus overallHealth = determineHealthStatus(errorRate);

        // Calculate prediction accuracy
        long correctPredictions = predictions.stream().filter(p -> Boolean.TRUE.equals(p.getIsCorrect())).count();
        long evaluatedPredictions = predictions.stream().filter(p -> p.getIsCorrect() != null).count();
        Double predictionAccuracy = evaluatedPredictions > 0
                ? (double) correctPredictions / evaluatedPredictions * 100 : null;

        // Top merchants by errors
        List<DailySummaryReport.MerchantIssue> topMerchantsByErrors = calculateTopMerchantsByErrors(metrics);

        // Errors by hour
        Map<Integer, Long> errorsByHour = metrics.stream()
                .filter(m -> m.getErrorCount() > 0)
                .collect(Collectors.groupingBy(
                        m -> m.getTimestamp().getHour(),
                        Collectors.summingLong(BatchMetrics::getErrorCount)
                ));

        // Calculate changes from previous day
        Double errorRateChange = null;
        Double processingTimeChange = null;
        Double batchCountChange = null;
        if (!prevDayMetrics.isEmpty()) {
            long prevTotalProcessed = prevDayMetrics.stream().mapToLong(BatchMetrics::getProcessedCount).sum();
            long prevTotalErrors = prevDayMetrics.stream().mapToLong(BatchMetrics::getErrorCount).sum();
            double prevErrorRate = prevTotalProcessed > 0 ? (double) prevTotalErrors / prevTotalProcessed : 0;
            double prevAvgProcessingTime = prevDayMetrics.stream()
                    .mapToDouble(BatchMetrics::getProcessingTimeMs).average().orElse(0);

            errorRateChange = prevErrorRate > 0 ? ((errorRate - prevErrorRate) / prevErrorRate) * 100 : null;
            processingTimeChange = prevAvgProcessingTime > 0
                    ? ((processingStats.getAverage() - prevAvgProcessingTime) / prevAvgProcessingTime) * 100 : null;
            batchCountChange = !prevDayMetrics.isEmpty()
                    ? ((double)(totalBatches - prevDayMetrics.size()) / prevDayMetrics.size()) * 100 : null;
        }

        // Generate alerts
        List<String> criticalAlerts = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        if (errorRate > ERROR_RATE_CRITICAL_THRESHOLD) {
            criticalAlerts.add(String.format("Critical error rate: %.2f%% (threshold: %.0f%%)",
                    errorRate * 100, ERROR_RATE_CRITICAL_THRESHOLD * 100));
        } else if (errorRate > ERROR_RATE_WARNING_THRESHOLD) {
            warnings.add(String.format("Elevated error rate: %.2f%% (threshold: %.0f%%)",
                    errorRate * 100, ERROR_RATE_WARNING_THRESHOLD * 100));
        }

        return DailySummaryReport.builder()
                .reportDate(date)
                .generatedAt(LocalDateTime.now())
                .totalBatches(totalBatches)
                .totalTransactionsProcessed(totalProcessed)
                .totalErrors(totalErrors)
                .errorRate(errorRate)
                .averageProcessingTimeMs(processingStats.getAverage())
                .maxProcessingTimeMs(processingStats.getMax())
                .minProcessingTimeMs(processingStats.getMin())
                .overallHealthStatus(overallHealth)
                .totalPredictions(predictions.size())
                .averagePredictionConfidence(predictions.stream()
                        .mapToDouble(AIPredictionAudit::getConfidence).average().orElse(0))
                .correctPredictions((int) correctPredictions)
                .incorrectPredictions((int) (evaluatedPredictions - correctPredictions))
                .predictionAccuracyRate(predictionAccuracy)
                .topMerchantsByErrors(topMerchantsByErrors)
                .criticalAlerts(criticalAlerts)
                .warnings(warnings)
                .errorsByHour(errorsByHour)
                .errorRateChange(errorRateChange)
                .processingTimeChange(processingTimeChange)
                .batchCountChange(batchCountChange)
                .build();
    }

    // ========================================================================
    // WEEKLY TREND REPORT
    // ========================================================================

    /**
     * Generate weekly trend report starting from a specific date
     */
    public WeeklyTrendReport generateWeeklyTrend(LocalDate weekStartDate) {
        log.info("Generating weekly trend report for week starting: {}", weekStartDate);

        LocalDate weekEndDate = weekStartDate.plusDays(6);
        LocalDateTime startDateTime = weekStartDate.atStartOfDay();
        LocalDateTime endDateTime = weekEndDate.atTime(LocalTime.MAX);

        List<BatchMetrics> weekMetrics = metricsRepository.findByTimestampBetween(startDateTime, endDateTime);
        List<AIPredictionAudit> weekPredictions = predictionAuditRepository.findByPredictionTimeAfter(startDateTime)
                .stream()
                .filter(p -> p.getPredictionTime().isBefore(endDateTime))
                .toList();

        // Previous week for comparison
        LocalDateTime prevWeekStart = weekStartDate.minusWeeks(1).atStartOfDay();
        LocalDateTime prevWeekEnd = weekStartDate.minusDays(1).atTime(LocalTime.MAX);
        List<BatchMetrics> prevWeekMetrics = metricsRepository.findByTimestampBetween(prevWeekStart, prevWeekEnd);

        return buildWeeklyTrend(weekStartDate, weekEndDate, weekMetrics, weekPredictions, prevWeekMetrics);
    }

    private WeeklyTrendReport buildWeeklyTrend(LocalDate weekStart, LocalDate weekEnd,
                                                List<BatchMetrics> metrics,
                                                List<AIPredictionAudit> predictions,
                                                List<BatchMetrics> prevWeekMetrics) {
        // Calculate weekly totals
        long totalBatches = metrics.size();
        long totalProcessed = metrics.stream().mapToLong(BatchMetrics::getProcessedCount).sum();
        long totalErrors = metrics.stream().mapToLong(BatchMetrics::getErrorCount).sum();
        double avgErrorRate = totalProcessed > 0 ? (double) totalErrors / totalProcessed : 0;
        double avgProcessingTime = metrics.stream().mapToDouble(BatchMetrics::getProcessingTimeMs).average().orElse(0);

        // Daily breakdown
        List<WeeklyTrendReport.DailyMetricsSummary> dailyMetrics = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate day = weekStart.plusDays(i);
            LocalDateTime dayStart = day.atStartOfDay();
            LocalDateTime dayEnd = day.atTime(LocalTime.MAX);

            List<BatchMetrics> dayData = metrics.stream()
                    .filter(m -> !m.getTimestamp().isBefore(dayStart) && !m.getTimestamp().isAfter(dayEnd))
                    .toList();

            if (!dayData.isEmpty()) {
                long dayProcessed = dayData.stream().mapToLong(BatchMetrics::getProcessedCount).sum();
                long dayErrors = dayData.stream().mapToLong(BatchMetrics::getErrorCount).sum();
                double dayErrorRate = dayProcessed > 0 ? (double) dayErrors / dayProcessed : 0;

                dailyMetrics.add(WeeklyTrendReport.DailyMetricsSummary.builder()
                        .date(day)
                        .batchCount(dayData.size())
                        .transactionCount(dayProcessed)
                        .errorCount(dayErrors)
                        .errorRate(dayErrorRate)
                        .avgProcessingTimeMs(dayData.stream().mapToDouble(BatchMetrics::getProcessingTimeMs).average().orElse(0))
                        .dominantHealthStatus(determineHealthStatus(dayErrorRate))
                        .build());
            }
        }

        // Calculate trends
        TrendDirection errorTrend = calculateTrend(dailyMetrics.stream()
                .map(WeeklyTrendReport.DailyMetricsSummary::getErrorRate).toList());
        TrendDirection processingTimeTrend = calculateTrend(dailyMetrics.stream()
                .map(WeeklyTrendReport.DailyMetricsSummary::getAvgProcessingTimeMs).toList());
        TrendDirection volumeTrend = calculateTrend(dailyMetrics.stream()
                .map(d -> (double) d.getTransactionCount()).toList());

        // Week-over-week comparison
        WeeklyTrendReport.WeekComparison weekComparison = calculateWeekComparison(metrics, prevWeekMetrics);

        // Merchant performance
        List<WeeklyTrendReport.MerchantPerformance> topPerformers = calculateMerchantPerformance(metrics, true);
        List<WeeklyTrendReport.MerchantPerformance> worstPerformers = calculateMerchantPerformance(metrics, false);

        // Prediction accuracy
        long correctPredictions = predictions.stream().filter(p -> Boolean.TRUE.equals(p.getIsCorrect())).count();
        long evaluatedPredictions = predictions.stream().filter(p -> p.getIsCorrect() != null).count();
        Double weeklyAccuracy = evaluatedPredictions > 0
                ? (double) correctPredictions / evaluatedPredictions * 100 : null;

        return WeeklyTrendReport.builder()
                .weekStartDate(weekStart)
                .weekEndDate(weekEnd)
                .generatedAt(LocalDateTime.now())
                .totalBatches(totalBatches)
                .totalTransactionsProcessed(totalProcessed)
                .totalErrors(totalErrors)
                .averageErrorRate(avgErrorRate)
                .averageProcessingTimeMs(avgProcessingTime)
                .dailyMetrics(dailyMetrics)
                .errorRateTrend(errorTrend)
                .processingTimeTrend(processingTimeTrend)
                .volumeTrend(volumeTrend)
                .weekOverWeekComparison(weekComparison)
                .topPerformingMerchants(topPerformers)
                .worstPerformingMerchants(worstPerformers)
                .totalPredictions(predictions.size())
                .correctPredictions((int) correctPredictions)
                .weeklyPredictionAccuracy(weeklyAccuracy)
                .build();
    }

    // ========================================================================
    // MERCHANT SCORECARD
    // ========================================================================

    /**
     * Generate merchant scorecard for a specific merchant
     */
    public MerchantScorecard generateMerchantScorecard(String merchId, String hsn, int days) {
        log.info("Generating merchant scorecard for: {}:{} over {} days", merchId, hsn, days);

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<BatchMetrics> merchantMetrics = metricsRepository.findByMerchIdAndHsn(merchId, hsn).stream()
                .filter(m -> !m.getTimestamp().isBefore(startDateTime) && !m.getTimestamp().isAfter(endDateTime))
                .toList();

        // Get all metrics for peer comparison
        List<BatchMetrics> allMetrics = metricsRepository.findByTimestampBetween(startDateTime, endDateTime);

        return buildMerchantScorecard(merchId, hsn, startDate, endDate, merchantMetrics, allMetrics);
    }

    private MerchantScorecard buildMerchantScorecard(String merchId, String hsn,
                                                      LocalDate startDate, LocalDate endDate,
                                                      List<BatchMetrics> merchantMetrics,
                                                      List<BatchMetrics> allMetrics) {
        if (merchantMetrics.isEmpty()) {
            return MerchantScorecard.builder()
                    .merchId(merchId)
                    .hsn(hsn)
                    .periodStart(startDate)
                    .periodEnd(endDate)
                    .generatedAt(LocalDateTime.now())
                    .overallScore(0)
                    .healthStatus(HealthStatus.UNKNOWN)
                    .healthGrade("N/A")
                    .build();
        }

        // Calculate metrics
        long totalBatches = merchantMetrics.size();
        long totalProcessed = merchantMetrics.stream().mapToLong(BatchMetrics::getProcessedCount).sum();
        long totalErrors = merchantMetrics.stream().mapToLong(BatchMetrics::getErrorCount).sum();
        double errorRate = totalProcessed > 0 ? (double) totalErrors / totalProcessed : 0;
        double avgProcessingTime = merchantMetrics.stream().mapToDouble(BatchMetrics::getProcessingTimeMs).average().orElse(0);

        // Calculate score components
        MerchantScorecard.ScoreBreakdown scoreBreakdown = calculateScoreBreakdown(errorRate, avgProcessingTime, merchantMetrics);
        double overallScore = scoreBreakdown.getErrorRateScore() + scoreBreakdown.getProcessingTimeScore()
                + scoreBreakdown.getReliabilityScore() + scoreBreakdown.getVolumeScore();

        // Daily scores
        List<MerchantScorecard.DailyScore> dailyScores = calculateDailyScores(merchantMetrics, startDate, endDate);

        // Peer comparison
        MerchantScorecard.PeerComparison peerComparison = calculatePeerComparison(merchId, hsn, errorRate, allMetrics);

        // Risk assessment
        MerchantScorecard.RiskLevel riskLevel = assessRiskLevel(errorRate, overallScore);
        List<String> riskFactors = identifyRiskFactors(errorRate, avgProcessingTime, merchantMetrics);

        // Recommendations
        List<String> recommendations = generateRecommendations(errorRate, avgProcessingTime, riskLevel);

        return MerchantScorecard.builder()
                .merchId(merchId)
                .hsn(hsn)
                .periodStart(startDate)
                .periodEnd(endDate)
                .generatedAt(LocalDateTime.now())
                .overallScore(overallScore)
                .healthStatus(determineHealthStatus(errorRate))
                .healthGrade(calculateGrade(overallScore))
                .totalBatches(totalBatches)
                .totalTransactionsProcessed(totalProcessed)
                .totalErrors(totalErrors)
                .errorRate(errorRate)
                .averageProcessingTimeMs(avgProcessingTime)
                .scoreBreakdown(scoreBreakdown)
                .dailyScores(dailyScores)
                .overallTrend(calculateScoreTrend(dailyScores))
                .peerComparison(peerComparison)
                .riskLevel(riskLevel)
                .riskFactors(riskFactors)
                .recommendations(recommendations)
                .build();
    }

    /**
     * Get all merchant scorecards ranked by score
     */
    public List<MerchantScorecard> getAllMerchantScorecards(int days) {
        log.info("Generating scorecards for all merchants over {} days", days);

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<BatchMetrics> allMetrics = metricsRepository.findByTimestampBetween(startDateTime, endDateTime);

        // Group by merchant
        Map<String, List<BatchMetrics>> metricsByMerchant = allMetrics.stream()
                .collect(Collectors.groupingBy(m -> m.getMerchId() + ":" + m.getHsn()));

        List<MerchantScorecard> scorecards = new ArrayList<>();
        for (Map.Entry<String, List<BatchMetrics>> entry : metricsByMerchant.entrySet()) {
            String[] parts = entry.getKey().split(":");
            String merchId = parts[0];
            String hsn = parts.length > 1 ? parts[1] : "";

            MerchantScorecard scorecard = buildMerchantScorecard(merchId, hsn, startDate, endDate,
                    entry.getValue(), allMetrics);
            scorecards.add(scorecard);
        }

        // Sort by score descending
        scorecards.sort((a, b) -> Double.compare(b.getOverallScore(), a.getOverallScore()));

        return scorecards;
    }

    // ========================================================================
    // PREDICTION ACCURACY REPORT
    // ========================================================================

    /**
     * Generate prediction accuracy report for a date range
     */
    public PredictionAccuracyReport generatePredictionAccuracyReport(int days) {
        log.info("Generating prediction accuracy report for last {} days", days);

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);
        LocalDateTime startDateTime = startDate.atStartOfDay();

        List<AIPredictionAudit> predictions = predictionAuditRepository.findByPredictionTimeAfter(startDateTime);

        return buildPredictionAccuracyReport(startDate, endDate, predictions);
    }

    private PredictionAccuracyReport buildPredictionAccuracyReport(LocalDate startDate, LocalDate endDate,
                                                                    List<AIPredictionAudit> predictions) {
        int totalPredictions = predictions.size();
        List<AIPredictionAudit> evaluated = predictions.stream()
                .filter(p -> p.getIsCorrect() != null).toList();
        int evaluatedCount = evaluated.size();
        long correctCount = evaluated.stream().filter(p -> Boolean.TRUE.equals(p.getIsCorrect())).count();
        long incorrectCount = evaluatedCount - correctCount;
        int pendingCount = totalPredictions - evaluatedCount;

        Double overallAccuracy = evaluatedCount > 0 ? (double) correctCount / evaluatedCount * 100 : null;

        // Confidence analysis
        double avgConfidence = predictions.stream().mapToDouble(AIPredictionAudit::getConfidence).average().orElse(0);
        double avgConfidenceCorrect = evaluated.stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsCorrect()))
                .mapToDouble(AIPredictionAudit::getConfidence).average().orElse(0);
        double avgConfidenceIncorrect = evaluated.stream()
                .filter(p -> Boolean.FALSE.equals(p.getIsCorrect()))
                .mapToDouble(AIPredictionAudit::getConfidence).average().orElse(0);

        // Accuracy by status
        Map<String, PredictionAccuracyReport.StatusAccuracy> accuracyByStatus = new HashMap<>();
        for (HealthStatus status : HealthStatus.values()) {
            List<AIPredictionAudit> statusPredictions = evaluated.stream()
                    .filter(p -> p.getPredictedStatus() == status).toList();
            if (!statusPredictions.isEmpty()) {
                long correct = statusPredictions.stream().filter(p -> Boolean.TRUE.equals(p.getIsCorrect())).count();
                accuracyByStatus.put(status.name(), PredictionAccuracyReport.StatusAccuracy.builder()
                        .status(status.name())
                        .totalPredictions(statusPredictions.size())
                        .correctPredictions((int) correct)
                        .accuracy((double) correct / statusPredictions.size() * 100)
                        .averageConfidence(statusPredictions.stream()
                                .mapToDouble(AIPredictionAudit::getConfidence).average().orElse(0))
                        .build());
            }
        }

        // Daily accuracy trend
        List<PredictionAccuracyReport.DailyAccuracy> dailyAccuracy = calculateDailyAccuracyTrend(predictions, startDate, endDate);

        // Generate insights
        List<String> insights = generateAccuracyInsights(overallAccuracy, avgConfidence, avgConfidenceCorrect, avgConfidenceIncorrect);
        List<String> recommendations = generateAccuracyRecommendations(overallAccuracy, avgConfidence);

        return PredictionAccuracyReport.builder()
                .periodStart(startDate)
                .periodEnd(endDate)
                .generatedAt(LocalDateTime.now())
                .totalPredictions(totalPredictions)
                .evaluatedPredictions(evaluatedCount)
                .correctPredictions((int) correctCount)
                .incorrectPredictions((int) incorrectCount)
                .pendingEvaluation(pendingCount)
                .overallAccuracy(overallAccuracy)
                .averageConfidence(avgConfidence)
                .averageConfidenceCorrect(avgConfidenceCorrect)
                .averageConfidenceIncorrect(avgConfidenceIncorrect)
                .accuracyByPredictedStatus(accuracyByStatus)
                .dailyAccuracyTrend(dailyAccuracy)
                .insights(insights)
                .recommendations(recommendations)
                .build();
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    private HealthStatus determineHealthStatus(double errorRate) {
        if (errorRate > ERROR_RATE_CRITICAL_THRESHOLD) {
            return HealthStatus.CRITICAL;
        } else if (errorRate > ERROR_RATE_WARNING_THRESHOLD) {
            return HealthStatus.WARNING;
        }
        return HealthStatus.HEALTHY;
    }

    private List<DailySummaryReport.MerchantIssue> calculateTopMerchantsByErrors(List<BatchMetrics> metrics) {
        Map<String, List<BatchMetrics>> byMerchant = metrics.stream()
                .collect(Collectors.groupingBy(m -> m.getMerchId() + ":" + m.getHsn()));

        return byMerchant.entrySet().stream()
                .map(entry -> {
                    String[] parts = entry.getKey().split(":");
                    List<BatchMetrics> merchantMetrics = entry.getValue();
                    long errors = merchantMetrics.stream().mapToLong(BatchMetrics::getErrorCount).sum();
                    long processed = merchantMetrics.stream().mapToLong(BatchMetrics::getProcessedCount).sum();
                    return DailySummaryReport.MerchantIssue.builder()
                            .merchId(parts[0])
                            .hsn(parts.length > 1 ? parts[1] : "")
                            .errorCount(errors)
                            .errorRate(processed > 0 ? (double) errors / processed : 0)
                            .totalTransactions(processed)
                            .build();
                })
                .filter(m -> m.getErrorCount() > 0)
                .sorted((a, b) -> Long.compare(b.getErrorCount(), a.getErrorCount()))
                .limit(10)
                .toList();
    }

    private TrendDirection calculateTrend(List<Double> values) {
        if (values.size() < 2) {
            return TrendDirection.UNKNOWN;
        }

        // Simple linear regression slope
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        int n = values.size();
        for (int i = 0; i < n; i++) {
            sumX += i;
            sumY += values.get(i);
            sumXY += i * values.get(i);
            sumX2 += i * i;
        }
        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);

        if (slope < -0.01) {
            return TrendDirection.IMPROVING;
        } else if (slope > 0.01) {
            return TrendDirection.DEGRADING;
        }
        return TrendDirection.STABLE;
    }

    private WeeklyTrendReport.WeekComparison calculateWeekComparison(List<BatchMetrics> currentWeek,
                                                                      List<BatchMetrics> previousWeek) {
        if (previousWeek.isEmpty()) {
            return WeeklyTrendReport.WeekComparison.builder()
                    .summary("No previous week data available for comparison")
                    .build();
        }

        long currProcessed = currentWeek.stream().mapToLong(BatchMetrics::getProcessedCount).sum();
        long currErrors = currentWeek.stream().mapToLong(BatchMetrics::getErrorCount).sum();
        double currErrorRate = currProcessed > 0 ? (double) currErrors / currProcessed : 0;
        double currAvgTime = currentWeek.stream().mapToDouble(BatchMetrics::getProcessingTimeMs).average().orElse(0);

        long prevProcessed = previousWeek.stream().mapToLong(BatchMetrics::getProcessedCount).sum();
        long prevErrors = previousWeek.stream().mapToLong(BatchMetrics::getErrorCount).sum();
        double prevErrorRate = prevProcessed > 0 ? (double) prevErrors / prevProcessed : 0;
        double prevAvgTime = previousWeek.stream().mapToDouble(BatchMetrics::getProcessingTimeMs).average().orElse(0);

        double errorRateChange = prevErrorRate > 0 ? ((currErrorRate - prevErrorRate) / prevErrorRate) * 100 : 0;
        double processingTimeChange = prevAvgTime > 0 ? ((currAvgTime - prevAvgTime) / prevAvgTime) * 100 : 0;
        double volumeChange = prevProcessed > 0 ? ((double)(currProcessed - prevProcessed) / prevProcessed) * 100 : 0;
        double batchCountChange = !previousWeek.isEmpty()
                ? ((double)(currentWeek.size() - previousWeek.size()) / previousWeek.size()) * 100 : 0;

        String summary = String.format("Error rate %s by %.1f%%, processing time %s by %.1f%%, volume %s by %.1f%%",
                errorRateChange < 0 ? "improved" : "increased", Math.abs(errorRateChange),
                processingTimeChange < 0 ? "improved" : "increased", Math.abs(processingTimeChange),
                volumeChange > 0 ? "increased" : "decreased", Math.abs(volumeChange));

        return WeeklyTrendReport.WeekComparison.builder()
                .errorRateChange(errorRateChange)
                .processingTimeChange(processingTimeChange)
                .volumeChange(volumeChange)
                .batchCountChange(batchCountChange)
                .summary(summary)
                .build();
    }

    private List<WeeklyTrendReport.MerchantPerformance> calculateMerchantPerformance(List<BatchMetrics> metrics,
                                                                                      boolean topPerformers) {
        Map<String, List<BatchMetrics>> byMerchant = metrics.stream()
                .collect(Collectors.groupingBy(m -> m.getMerchId() + ":" + m.getHsn()));

        List<WeeklyTrendReport.MerchantPerformance> performances = byMerchant.entrySet().stream()
                .map(entry -> {
                    String[] parts = entry.getKey().split(":");
                    List<BatchMetrics> merchantMetrics = entry.getValue();
                    long processed = merchantMetrics.stream().mapToLong(BatchMetrics::getProcessedCount).sum();
                    long errors = merchantMetrics.stream().mapToLong(BatchMetrics::getErrorCount).sum();
                    double errorRate = processed > 0 ? (double) errors / processed : 0;
                    double avgTime = merchantMetrics.stream().mapToDouble(BatchMetrics::getProcessingTimeMs).average().orElse(0);

                    // Performance score: lower error rate and processing time = higher score
                    double performanceScore = 100 - (errorRate * 500) - (avgTime / 100);

                    return WeeklyTrendReport.MerchantPerformance.builder()
                            .merchId(parts[0])
                            .hsn(parts.length > 1 ? parts[1] : "")
                            .totalTransactions(processed)
                            .errorCount(errors)
                            .errorRate(errorRate)
                            .avgProcessingTimeMs(avgTime)
                            .performanceScore(Math.max(0, performanceScore))
                            .build();
                })
                .toList();

        if (topPerformers) {
            return performances.stream()
                    .sorted((a, b) -> Double.compare(b.getPerformanceScore(), a.getPerformanceScore()))
                    .limit(5)
                    .toList();
        } else {
            return performances.stream()
                    .sorted(Comparator.comparingDouble(WeeklyTrendReport.MerchantPerformance::getPerformanceScore))
                    .limit(5)
                    .toList();
        }
    }

    private MerchantScorecard.ScoreBreakdown calculateScoreBreakdown(double errorRate, double avgProcessingTime,
                                                                      List<BatchMetrics> metrics) {
        // Error rate score: 25 points max, loses points as error rate increases
        double errorRateScore = Math.max(0, 25 - (errorRate * 250));

        // Processing time score: 25 points max, based on average processing time
        double processingTimeScore = avgProcessingTime < 2000 ? 25 :
                avgProcessingTime < 5000 ? 20 :
                avgProcessingTime < 10000 ? 15 : 10;

        // Reliability score: based on consistency
        double stdDev = calculateStandardDeviation(metrics.stream()
                .mapToDouble(BatchMetrics::getProcessingTimeMs).toArray());
        double reliabilityScore = stdDev < 500 ? 25 : stdDev < 1000 ? 20 : stdDev < 2000 ? 15 : 10;

        // Volume score: based on total transactions
        long totalVolume = metrics.stream().mapToLong(BatchMetrics::getProcessedCount).sum();
        double volumeScore = totalVolume > 10000 ? 25 : totalVolume > 5000 ? 20 : totalVolume > 1000 ? 15 : 10;

        return MerchantScorecard.ScoreBreakdown.builder()
                .errorRateScore(errorRateScore)
                .processingTimeScore(processingTimeScore)
                .reliabilityScore(reliabilityScore)
                .volumeScore(volumeScore)
                .build();
    }

    private double calculateStandardDeviation(double[] values) {
        if (values.length == 0) return 0;
        double mean = Arrays.stream(values).average().orElse(0);
        double variance = Arrays.stream(values).map(v -> Math.pow(v - mean, 2)).average().orElse(0);
        return Math.sqrt(variance);
    }

    private List<MerchantScorecard.DailyScore> calculateDailyScores(List<BatchMetrics> metrics,
                                                                     LocalDate startDate, LocalDate endDate) {
        List<MerchantScorecard.DailyScore> dailyScores = new ArrayList<>();
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            LocalDate day = current;
            List<BatchMetrics> dayMetrics = metrics.stream()
                    .filter(m -> m.getTimestamp().toLocalDate().equals(day))
                    .toList();

            if (!dayMetrics.isEmpty()) {
                long processed = dayMetrics.stream().mapToLong(BatchMetrics::getProcessedCount).sum();
                long errors = dayMetrics.stream().mapToLong(BatchMetrics::getErrorCount).sum();
                double errorRate = processed > 0 ? (double) errors / processed : 0;
                double avgTime = dayMetrics.stream().mapToDouble(BatchMetrics::getProcessingTimeMs).average().orElse(0);

                // Simple daily score calculation
                double score = Math.max(0, 100 - (errorRate * 500) - (avgTime / 100));

                dailyScores.add(MerchantScorecard.DailyScore.builder()
                        .date(day)
                        .score(score)
                        .errorRate(errorRate)
                        .avgProcessingTimeMs(avgTime)
                        .batchCount(dayMetrics.size())
                        .build());
            }
            current = current.plusDays(1);
        }

        return dailyScores;
    }

    private MerchantScorecard.TrendDirection calculateScoreTrend(List<MerchantScorecard.DailyScore> dailyScores) {
        if (dailyScores.size() < 2) {
            return MerchantScorecard.TrendDirection.UNKNOWN;
        }

        List<Double> scores = dailyScores.stream().map(MerchantScorecard.DailyScore::getScore).toList();
        TrendDirection trend = calculateTrend(scores);

        return switch (trend) {
            case IMPROVING -> MerchantScorecard.TrendDirection.DEGRADING; // Inverted because higher score is better
            case DEGRADING -> MerchantScorecard.TrendDirection.IMPROVING;
            case STABLE -> MerchantScorecard.TrendDirection.STABLE;
            default -> MerchantScorecard.TrendDirection.UNKNOWN;
        };
    }

    private MerchantScorecard.PeerComparison calculatePeerComparison(String merchId, String hsn,
                                                                      double merchantErrorRate,
                                                                      List<BatchMetrics> allMetrics) {
        Map<String, Double> merchantErrorRates = allMetrics.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getMerchId() + ":" + m.getHsn(),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> {
                                    long processed = list.stream().mapToLong(BatchMetrics::getProcessedCount).sum();
                                    long errors = list.stream().mapToLong(BatchMetrics::getErrorCount).sum();
                                    return processed > 0 ? (double) errors / processed : 0.0;
                                }
                        )
                ));

        List<Double> allErrorRates = new ArrayList<>(merchantErrorRates.values());
        Collections.sort(allErrorRates);

        double avgPeerErrorRate = allErrorRates.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        int rank = (int) allErrorRates.stream().filter(r -> r < merchantErrorRate).count();
        double percentileRank = allErrorRates.isEmpty() ? 50 : (1 - (double) rank / allErrorRates.size()) * 100;

        String summary = merchantErrorRate < avgPeerErrorRate
                ? String.format("Performing better than average (%.1f%% vs %.1f%% avg error rate)",
                        merchantErrorRate * 100, avgPeerErrorRate * 100)
                : String.format("Performing below average (%.1f%% vs %.1f%% avg error rate)",
                        merchantErrorRate * 100, avgPeerErrorRate * 100);

        return MerchantScorecard.PeerComparison.builder()
                .percentileRank(percentileRank)
                .averagePeerScore(100 - avgPeerErrorRate * 500)
                .scoreDifferenceFromAverage((100 - merchantErrorRate * 500) - (100 - avgPeerErrorRate * 500))
                .totalMerchantsCompared(merchantErrorRates.size())
                .comparisonSummary(summary)
                .build();
    }

    private MerchantScorecard.RiskLevel assessRiskLevel(double errorRate, double score) {
        if (errorRate > ERROR_RATE_CRITICAL_THRESHOLD || score < 40) {
            return MerchantScorecard.RiskLevel.CRITICAL;
        } else if (errorRate > ERROR_RATE_WARNING_THRESHOLD || score < 60) {
            return MerchantScorecard.RiskLevel.HIGH;
        } else if (errorRate > 0.02 || score < 80) {
            return MerchantScorecard.RiskLevel.MEDIUM;
        }
        return MerchantScorecard.RiskLevel.LOW;
    }

    private List<String> identifyRiskFactors(double errorRate, double avgProcessingTime, List<BatchMetrics> metrics) {
        List<String> risks = new ArrayList<>();

        if (errorRate > ERROR_RATE_CRITICAL_THRESHOLD) {
            risks.add("Critical error rate exceeds " + (ERROR_RATE_CRITICAL_THRESHOLD * 100) + "%");
        } else if (errorRate > ERROR_RATE_WARNING_THRESHOLD) {
            risks.add("Error rate above warning threshold of " + (ERROR_RATE_WARNING_THRESHOLD * 100) + "%");
        }

        if (avgProcessingTime > 5000) {
            risks.add("High average processing time: " + String.format("%.0f", avgProcessingTime) + "ms");
        }

        // Check for increasing error trend
        List<Double> recentErrors = metrics.stream()
                .sorted(Comparator.comparing(BatchMetrics::getTimestamp))
                .map(m -> m.getProcessedCount() > 0 ? (double) m.getErrorCount() / m.getProcessedCount() : 0.0)
                .toList();
        if (calculateTrend(recentErrors) == TrendDirection.DEGRADING) {
            risks.add("Error rate showing increasing trend");
        }

        if (risks.isEmpty()) {
            risks.add("No significant risk factors identified");
        }

        return risks;
    }

    private List<String> generateRecommendations(double errorRate, double avgProcessingTime,
                                                  MerchantScorecard.RiskLevel riskLevel) {
        List<String> recommendations = new ArrayList<>();

        switch (riskLevel) {
            case CRITICAL:
                recommendations.add("URGENT: Investigate root cause of high error rate immediately");
                recommendations.add("Review recent configuration or system changes");
                recommendations.add("Consider temporarily reducing batch sizes");
                break;
            case HIGH:
                recommendations.add("Schedule investigation of error patterns");
                recommendations.add("Monitor closely for further degradation");
                break;
            case MEDIUM:
                recommendations.add("Review processing logs for optimization opportunities");
                if (avgProcessingTime > 3000) {
                    recommendations.add("Consider optimizing batch processing performance");
                }
                break;
            case LOW:
                recommendations.add("Continue current monitoring practices");
                recommendations.add("System performing within acceptable parameters");
                break;
        }

        return recommendations;
    }

    private String calculateGrade(double score) {
        if (score >= 90) return "A";
        if (score >= 80) return "B";
        if (score >= 70) return "C";
        if (score >= 60) return "D";
        return "F";
    }

    private List<PredictionAccuracyReport.DailyAccuracy> calculateDailyAccuracyTrend(
            List<AIPredictionAudit> predictions, LocalDate startDate, LocalDate endDate) {
        List<PredictionAccuracyReport.DailyAccuracy> dailyAccuracy = new ArrayList<>();
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            LocalDate day = current;
            List<AIPredictionAudit> dayPredictions = predictions.stream()
                    .filter(p -> p.getPredictionTime().toLocalDate().equals(day))
                    .toList();

            if (!dayPredictions.isEmpty()) {
                List<AIPredictionAudit> evaluated = dayPredictions.stream()
                        .filter(p -> p.getIsCorrect() != null).toList();
                long correct = evaluated.stream().filter(p -> Boolean.TRUE.equals(p.getIsCorrect())).count();

                dailyAccuracy.add(PredictionAccuracyReport.DailyAccuracy.builder()
                        .date(day)
                        .totalPredictions(dayPredictions.size())
                        .correctPredictions((int) correct)
                        .accuracy(evaluated.isEmpty() ? null : (double) correct / evaluated.size() * 100)
                        .averageConfidence(dayPredictions.stream()
                                .mapToDouble(AIPredictionAudit::getConfidence).average().orElse(0))
                        .build());
            }
            current = current.plusDays(1);
        }

        return dailyAccuracy;
    }

    private List<String> generateAccuracyInsights(Double overallAccuracy, double avgConfidence,
                                                   double avgConfidenceCorrect, double avgConfidenceIncorrect) {
        List<String> insights = new ArrayList<>();

        if (overallAccuracy != null) {
            if (overallAccuracy >= 80) {
                insights.add("Prediction accuracy is excellent at " + String.format("%.1f%%", overallAccuracy));
            } else if (overallAccuracy >= 60) {
                insights.add("Prediction accuracy is moderate at " + String.format("%.1f%%", overallAccuracy));
            } else {
                insights.add("Prediction accuracy needs improvement at " + String.format("%.1f%%", overallAccuracy));
            }
        }

        if (avgConfidenceCorrect > avgConfidenceIncorrect + 0.1) {
            insights.add("Confidence scores are well calibrated - higher confidence correlates with accuracy");
        } else if (avgConfidenceCorrect < avgConfidenceIncorrect) {
            insights.add("Confidence calibration issue detected - incorrect predictions have higher confidence");
        }

        return insights;
    }

    private List<String> generateAccuracyRecommendations(Double overallAccuracy, double avgConfidence) {
        List<String> recommendations = new ArrayList<>();

        if (overallAccuracy != null && overallAccuracy < 70) {
            recommendations.add("Consider adjusting prediction thresholds");
            recommendations.add("Review and update AI prompts for better accuracy");
        }

        if (avgConfidence < 0.6) {
            recommendations.add("Low confidence predictions may benefit from more historical data");
        }

        if (recommendations.isEmpty()) {
            recommendations.add("Continue monitoring prediction accuracy");
        }

        return recommendations;
    }
}

