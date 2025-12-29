package com.cardconnect.bolt.ai.health;

import com.cardconnect.bolt.ai.model.AIPredictionResult;
import com.cardconnect.bolt.ai.model.HealthStatus;
import com.cardconnect.bolt.ai.service.AIResponseCacheService;
import com.cardconnect.bolt.ai.service.OfflineBatchHybridAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Health indicator that uses AI to predict offline batch health status
 */
@Component
@Slf4j
public class OfflineBatchAIHealthIndicator implements HealthIndicator {

    private final OfflineBatchHybridAnalysisService hybridAnalysisService;
    private final AIResponseCacheService cacheService;

    private AIPredictionResult cachedPrediction;
    private LocalDateTime lastPredictionTime;

    public OfflineBatchAIHealthIndicator(OfflineBatchHybridAnalysisService hybridAnalysisService,
                                          AIResponseCacheService cacheService) {
        this.hybridAnalysisService = hybridAnalysisService;
        this.cacheService = cacheService;
    }

    @Override
    public Health health() {
        try {
            // Get prediction (with caching)
            AIPredictionResult prediction = cacheService.getCachedOrAnalyze(
                () -> hybridAnalysisService.analyzeTrendsAndPredict()
            );

            cachedPrediction = prediction;
            lastPredictionTime = LocalDateTime.now();

            return buildHealthFromPrediction(prediction);

        } catch (Exception e) {
            log.error("AI health prediction failed", e);
            return Health.down()
                .withDetail("error", "AI prediction failed: " + e.getMessage())
                .withDetail("fallback", "Using traditional health checks")
                .build();
        }
    }

    private Health buildHealthFromPrediction(AIPredictionResult prediction) {
        Health.Builder builder;

        // Determine health status based on AI prediction
        if (prediction.getPredictedStatus() == HealthStatus.CRITICAL &&
            prediction.getConfidence() > 0.75) {
            builder = Health.down();
            logCriticalAlert(prediction);
        } else if (prediction.getPredictedStatus() == HealthStatus.WARNING &&
                   prediction.getConfidence() > 0.65) {
            builder = Health.status("WARNING");
            logWarningAlert(prediction);
        } else {
            builder = Health.up();
        }

        // Add comprehensive details
        builder.withDetail("aiAnalysis", Map.of(
            "predictedStatus", prediction.getPredictedStatus(),
            "confidence", String.format("%.1f%%", prediction.getConfidence() * 100),
            "timeHorizon", prediction.getTimeHorizon() + " hours",
            "lastAnalysis", lastPredictionTime != null ?
                lastPredictionTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "N/A"
        ));

        if (prediction.getKeyFindings() != null && !prediction.getKeyFindings().isEmpty()) {
            builder.withDetail("keyFindings", prediction.getKeyFindings());
        }

        if (prediction.getTrendAnalysis() != null) {
            builder.withDetail("trendAnalysis", Map.of(
                "errorRateTrend", prediction.getTrendAnalysis().getErrorRateTrend(),
                "processingTimeTrend", prediction.getTrendAnalysis().getProcessingTimeTrend(),
                "anomalyDetected", prediction.getTrendAnalysis().isAnomalyDetected()
            ));
        }

        if (prediction.getRiskFactors() != null && !prediction.getRiskFactors().isEmpty()) {
            builder.withDetail("riskFactors", prediction.getRiskFactors());
        }

        if (prediction.getRecommendations() != null && !prediction.getRecommendations().isEmpty()) {
            builder.withDetail("recommendations", prediction.getRecommendations());
        }

        if (prediction.getReasoning() != null) {
            builder.withDetail("reasoning", prediction.getReasoning());
        }

        return builder.build();
    }

    private void logCriticalAlert(AIPredictionResult prediction) {
        log.error("╔══════════════════════════════════════════════════════════════╗");
        log.error("║  🚨 AI CRITICAL PREDICTION                                   ║");
        log.error("╠══════════════════════════════════════════════════════════════╣");
        log.error("  Status: {} (Confidence: {:.1f}%)",
            prediction.getPredictedStatus(),
            prediction.getConfidence() * 100);
        log.error("  Time Horizon: {} hours", prediction.getTimeHorizon());

        if (prediction.getKeyFindings() != null) {
            log.error("  Key Findings:");
            prediction.getKeyFindings().forEach(finding ->
                log.error("    🔍 {}", finding));
        }

        if (prediction.getRiskFactors() != null) {
            log.error("  Risk Factors:");
            prediction.getRiskFactors().forEach(risk ->
                log.error("    ⚠️  {}", risk));
        }

        if (prediction.getRecommendations() != null) {
            log.error("  Recommended Actions:");
            prediction.getRecommendations().forEach(rec ->
                log.error("    💡 {}", rec));
        }

        log.error("  Reasoning: {}", prediction.getReasoning());
        log.error("╚══════════════════════════════════════════════════════════════╝");
    }

    private void logWarningAlert(AIPredictionResult prediction) {
        log.warn("⚠️  AI WARNING: Potential issues detected in next {} hours (confidence: {:.1f}%)",
            prediction.getTimeHorizon(),
            prediction.getConfidence() * 100);

        if (prediction.getRecommendations() != null) {
            prediction.getRecommendations().forEach(rec ->
                log.warn("  💡 RECOMMENDATION: {}", rec));
        }
    }
}

