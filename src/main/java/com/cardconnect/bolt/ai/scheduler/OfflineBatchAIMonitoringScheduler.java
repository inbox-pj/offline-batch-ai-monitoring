package com.cardconnect.bolt.ai.scheduler;

import com.cardconnect.bolt.ai.config.properties.AIPredictionProperties;
import com.cardconnect.bolt.ai.model.AIPredictionResult;
import com.cardconnect.bolt.ai.model.HealthStatus;
import com.cardconnect.bolt.ai.service.OfflineBatchHybridAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Scheduler for periodic AI monitoring of offline batch health
 * All scheduling parameters are configurable via properties
 */
@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ai.prediction.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class OfflineBatchAIMonitoringScheduler {

    private final OfflineBatchHybridAnalysisService hybridAnalysisService;
    private final AIPredictionProperties predictionProperties;

    /**
     * Perform AI analysis at configurable intervals
     * Default: every 5 minutes (configurable via ai.prediction.scheduler.interval-ms)
     */
    @Scheduled(fixedRateString = "${ai.prediction.scheduler.interval-ms:300000}")
    public void performScheduledAnalysis() {
        try {
            log.debug("Starting scheduled AI analysis");

            AIPredictionResult prediction = hybridAnalysisService.analyzeTrendsAndPredict();

            // Get confidence threshold from configuration
            double criticalThreshold = predictionProperties.getConfidenceThreshold();
            double warningThreshold = criticalThreshold - 0.10; // 10% lower for warnings

            // Log results based on severity
            if (prediction.getPredictedStatus() == HealthStatus.CRITICAL &&
                prediction.getConfidence() > criticalThreshold) {

                logCriticalPrediction(prediction);

            } else if (prediction.getPredictedStatus() == HealthStatus.WARNING &&
                       prediction.getConfidence() > warningThreshold) {

                logWarningPrediction(prediction);

            } else {
                log.info("✅ System healthy - AI prediction: {} (confidence: {}%)",
                    prediction.getPredictedStatus(),
                    String.format("%.1f", prediction.getConfidence() * 100));
            }

        } catch (Exception e) {
            log.error("Scheduled AI analysis failed", e);
        }
    }

    private void logCriticalPrediction(AIPredictionResult prediction) {
        log.error("╔═══════════════════════════════════════════════════════════╗");
        log.error("║  🚨 CRITICAL: AI predicts critical issues                 ║");
        log.error("╠═══════════════════════════════════════════════════════════╣");
        log.error("  Confidence: {}%", String.format("%.1f", prediction.getConfidence() * 100));
        log.error("  Time Horizon: {} hours", prediction.getTimeHorizon());

        if (prediction.getKeyFindings() != null) {
            log.error("  Key Findings:");
            prediction.getKeyFindings().forEach(finding ->
                log.error("    • {}", finding));
        }

        if (prediction.getRecommendations() != null) {
            log.error("  IMMEDIATE ACTIONS REQUIRED:");
            prediction.getRecommendations().forEach(rec ->
                log.error("    → {}", rec));
        }
        log.error("╚═══════════════════════════════════════════════════════════╝");
    }

    private void logWarningPrediction(AIPredictionResult prediction) {
        log.warn("⚠️  WARNING: Potential issues detected (confidence: {}%)",
            String.format("%.1f", prediction.getConfidence() * 100));

        if (prediction.getRecommendations() != null) {
            log.warn("  Recommendations:");
            prediction.getRecommendations().forEach(rec ->
                log.warn("    • {}", rec));
        }
    }

    /**
     * Generate weekly summary - configurable via cron expression
     * Default: every Monday at 8 AM
     */
    @Scheduled(cron = "${ai.prediction.scheduler.weekly-summary-cron:0 0 8 * * MON}")
    public void generateWeeklySummary() {
        log.info("╔═══════════════════════════════════════════════════════════╗");
        log.info("║  📊 Weekly AI Monitoring Summary                          ║");
        log.info("╠═══════════════════════════════════════════════════════════╣");
        log.info("  Week: {} to {}", LocalDate.now().minusDays(7), LocalDate.now());
        log.info("  Weekly summary generation - feature coming soon");
        log.info("╚═══════════════════════════════════════════════════════════╝");
    }
}

