package com.cardconnect.bolt.ai.service;

import com.cardconnect.bolt.ai.model.AIPredictionResult;
import com.cardconnect.bolt.ai.model.BatchMetrics;
import com.cardconnect.bolt.ai.model.HealthStatus;
import com.cardconnect.bolt.ai.model.TrendAnalysis;
import com.cardconnect.bolt.ai.repository.OfflineBatchMetricsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;

/**
 * Rule-based trend analysis service (fallback when AI is unavailable)
 */
@Service
@Transactional(readOnly = true)
@Slf4j
public class OfflineBatchTrendAnalysisService {

    private final OfflineBatchMetricsRepository metricsRepository;

    private static final double ERROR_RATE_WARNING_THRESHOLD = 0.05; // 5%
    private static final double ERROR_RATE_CRITICAL_THRESHOLD = 0.10; // 10%
    private static final double PROCESSING_TIME_INCREASE_THRESHOLD = 1.5; // 50% increase

    public OfflineBatchTrendAnalysisService(OfflineBatchMetricsRepository metricsRepository) {
        this.metricsRepository = metricsRepository;
    }

    /**
     * Analyze trends using rule-based logic
     */
    public AIPredictionResult analyzeTrends() {
        try {
            LocalDateTime now = LocalDateTime.now();

            // Get metrics for last 24 hours
            List<BatchMetrics> last24Hours = metricsRepository.findByTimestampAfter(now.minusHours(24));

            if (last24Hours.isEmpty()) {
                return AIPredictionResult.insufficientData();
            }

            // Get metrics for previous 24 hours (for comparison)
            List<BatchMetrics> previous24Hours = metricsRepository.findByTimestampBetween(
                now.minusHours(48), now.minusHours(24));

            // Perform rule-based analysis
            double currentErrorRate = calculateErrorRate(last24Hours);
            double previousErrorRate = previous24Hours.isEmpty() ? 0 : calculateErrorRate(previous24Hours);

            double currentAvgTime = calculateAverageProcessingTime(last24Hours);
            double previousAvgTime = previous24Hours.isEmpty() ? currentAvgTime : calculateAverageProcessingTime(previous24Hours);

            // Determine health status
            HealthStatus status = determineHealthStatus(currentErrorRate, currentAvgTime, previousAvgTime);

            // Build result
            AIPredictionResult result = AIPredictionResult.builder()
                .predictedStatus(status)
                .confidence(0.7) // Lower confidence for rule-based
                .timeHorizon(6)
                .keyFindings(generateFindings(currentErrorRate, currentAvgTime, last24Hours))
                .trendAnalysis(buildTrendAnalysis(currentErrorRate, previousErrorRate, currentAvgTime, previousAvgTime))
                .riskFactors(identifyRiskFactors(currentErrorRate, currentAvgTime, previousAvgTime))
                .recommendations(generateRecommendations(status, currentErrorRate, currentAvgTime))
                .reasoning("Analysis performed using rule-based system")
                .build();

            log.info("Rule-based analysis completed: {} (error rate: {}%)",
                status, String.format("%.2f", currentErrorRate * 100));

            return result;

        } catch (Exception e) {
            log.error("Rule-based analysis failed", e);
            return AIPredictionResult.error("Rule-based analysis failed: " + e.getMessage());
        }
    }

    private HealthStatus determineHealthStatus(double errorRate, double currentAvgTime, double previousAvgTime) {
        if (errorRate > ERROR_RATE_CRITICAL_THRESHOLD) {
            return HealthStatus.CRITICAL;
        }

        if (errorRate > ERROR_RATE_WARNING_THRESHOLD) {
            return HealthStatus.WARNING;
        }

        if (currentAvgTime > previousAvgTime * PROCESSING_TIME_INCREASE_THRESHOLD) {
            return HealthStatus.WARNING;
        }

        return HealthStatus.HEALTHY;
    }

    private List<String> generateFindings(double errorRate, double avgTime, List<BatchMetrics> metrics) {
        List<String> findings = new ArrayList<>();

        if (errorRate < 0.01) {
            findings.add("System operating normally with minimal errors");
        } else if (errorRate < ERROR_RATE_WARNING_THRESHOLD) {
            findings.add(String.format("Error rate at %.2f%% - within acceptable range", errorRate * 100));
        } else {
            findings.add(String.format("Elevated error rate detected: %.2f%%", errorRate * 100));
        }

        findings.add(String.format("Average processing time: %.0f ms", avgTime));
        findings.add(String.format("Total batches analyzed: %d", metrics.size()));

        return findings;
    }

    private TrendAnalysis buildTrendAnalysis(double currentError, double previousError,
                                             double currentTime, double previousTime) {
        TrendAnalysis analysis = new TrendAnalysis();

        // Error rate trend
        if (currentError > previousError * 1.2) {
            analysis.setErrorRateTrend("INCREASING");
        } else if (currentError < previousError * 0.8) {
            analysis.setErrorRateTrend("DECREASING");
        } else {
            analysis.setErrorRateTrend("STABLE");
        }

        // Processing time trend
        if (currentTime > previousTime * 1.2) {
            analysis.setProcessingTimeTrend("INCREASING");
        } else if (currentTime < previousTime * 0.8) {
            analysis.setProcessingTimeTrend("DECREASING");
        } else {
            analysis.setProcessingTimeTrend("STABLE");
        }

        // Simple anomaly detection
        analysis.setAnomalyDetected(currentError > ERROR_RATE_WARNING_THRESHOLD ||
                                    currentTime > previousTime * PROCESSING_TIME_INCREASE_THRESHOLD);

        return analysis;
    }

    private List<String> identifyRiskFactors(double errorRate, double currentAvgTime, double previousAvgTime) {
        List<String> risks = new ArrayList<>();

        if (errorRate > ERROR_RATE_WARNING_THRESHOLD) {
            risks.add("Error rate above threshold - potential system degradation");
        }

        if (currentAvgTime > previousAvgTime * PROCESSING_TIME_INCREASE_THRESHOLD) {
            risks.add("Processing time increased significantly - performance degradation");
        }

        if (risks.isEmpty()) {
            risks.add("No significant risk factors detected");
        }

        return risks;
    }

    private List<String> generateRecommendations(HealthStatus status, double errorRate, double avgTime) {
        List<String> recommendations = new ArrayList<>();

        switch (status) {
            case CRITICAL:
                recommendations.add("IMMEDIATE: Investigate error sources and address critical issues");
                recommendations.add("Review system logs for error patterns");
                recommendations.add("Consider scaling resources or implementing circuit breakers");
                break;

            case WARNING:
                recommendations.add("Monitor error trends closely");
                recommendations.add("Review recent changes to system or configuration");
                recommendations.add("Check database connection pool and system resources");
                break;

            case HEALTHY:
                recommendations.add("Continue monitoring current metrics");
                recommendations.add("System operating normally");
                break;

            default:
                recommendations.add("Insufficient data for recommendations");
        }

        return recommendations;
    }

    private double calculateErrorRate(List<BatchMetrics> metrics) {
        long totalProcessed = metrics.stream().mapToLong(BatchMetrics::getProcessedCount).sum();
        long totalErrors = metrics.stream().mapToLong(BatchMetrics::getErrorCount).sum();
        return totalProcessed > 0 ? (double) totalErrors / totalProcessed : 0;
    }

    private double calculateAverageProcessingTime(List<BatchMetrics> metrics) {
        return metrics.stream()
            .mapToLong(BatchMetrics::getProcessingTimeMs)
            .average()
            .orElse(0.0);
    }
}

