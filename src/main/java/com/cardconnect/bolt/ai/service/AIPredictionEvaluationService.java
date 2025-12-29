package com.cardconnect.bolt.ai.service;

import com.cardconnect.bolt.ai.model.AIPredictionAudit;
import com.cardconnect.bolt.ai.model.HealthStatus;
import com.cardconnect.bolt.ai.repository.AIPredictionAuditRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Evaluates AI prediction accuracy over time
 */
@Service
@Transactional(readOnly = true)
@Slf4j
public class AIPredictionEvaluationService {

    private final AIPredictionAuditRepository auditRepository;
    private final MeterRegistry meterRegistry;

    public AIPredictionEvaluationService(AIPredictionAuditRepository auditRepository,
                                          MeterRegistry meterRegistry) {
        this.auditRepository = auditRepository;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Evaluate model accuracy daily
     */
    @Scheduled(cron = "0 0 9 * * *") // Daily at 9 AM
    public void evaluateModelAccuracy() {
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);

        List<AIPredictionAudit> predictions = auditRepository
            .findByPredictionTimeAfterAndActualOutcomeNotNull(startDate);

        if (predictions.isEmpty()) {
            log.info("No predictions with outcomes found for evaluation");
            return;
        }

        // Calculate accuracy metrics
        long totalPredictions = predictions.size();
        long correctPredictions = predictions.stream()
            .filter(AIPredictionAudit::getIsCorrect)
            .count();

        double accuracy = (double) correctPredictions / totalPredictions;

        // Calculate by status
        Map<HealthStatus, Double> accuracyByStatus = calculateAccuracyByStatus(predictions);

        // Calculate precision and recall
        Map<HealthStatus, Double> precision = calculatePrecision(predictions);
        Map<HealthStatus, Double> recall = calculateRecall(predictions);

        // Record metrics
        meterRegistry.gauge("ai.prediction.accuracy.overall", accuracy);

        accuracyByStatus.forEach((status, acc) ->
            meterRegistry.gauge("ai.prediction.accuracy.by.status",
                Tags.of("status", status.name()), acc));

        // Log results
        log.info("╔══════════════════════════════════════════════════╗");
        log.info("║  AI Model Evaluation (Last 7 Days)              ║");
        log.info("╠══════════════════════════════════════════════════╣");
        log.info("  Total Predictions: {}", totalPredictions);
        log.info("  Correct: {} ({:.1f}%)", correctPredictions, accuracy * 100);
        log.info("  Accuracy by Status: {}", accuracyByStatus);
        log.info("  Precision: {}", precision);
        log.info("  Recall: {}", recall);
        log.info("╚══════════════════════════════════════════════════╝");

        // Alert if accuracy drops below threshold
        if (accuracy < 0.7) {
            log.error("🚨 AI model accuracy below threshold: {:.1f}% - Retraining recommended",
                accuracy * 100);
        }
    }

    private Map<HealthStatus, Double> calculateAccuracyByStatus(List<AIPredictionAudit> predictions) {
        return Arrays.stream(HealthStatus.values())
            .collect(Collectors.toMap(
                status -> status,
                status -> {
                    List<AIPredictionAudit> statusPredictions = predictions.stream()
                        .filter(p -> p.getPredictedStatus() == status)
                        .collect(Collectors.toList());

                    if (statusPredictions.isEmpty()) return 0.0;

                    long correct = statusPredictions.stream()
                        .filter(AIPredictionAudit::getIsCorrect)
                        .count();

                    return (double) correct / statusPredictions.size();
                }
            ));
    }

    private Map<HealthStatus, Double> calculatePrecision(List<AIPredictionAudit> predictions) {
        return Arrays.stream(HealthStatus.values())
            .collect(Collectors.toMap(
                status -> status,
                status -> {
                    List<AIPredictionAudit> predictedAsStatus = predictions.stream()
                        .filter(p -> p.getPredictedStatus() == status)
                        .collect(Collectors.toList());

                    if (predictedAsStatus.isEmpty()) return 0.0;

                    long truePositives = predictedAsStatus.stream()
                        .filter(p -> p.getActualOutcome() == status)
                        .count();

                    return (double) truePositives / predictedAsStatus.size();
                }
            ));
    }

    private Map<HealthStatus, Double> calculateRecall(List<AIPredictionAudit> predictions) {
        return Arrays.stream(HealthStatus.values())
            .collect(Collectors.toMap(
                status -> status,
                status -> {
                    List<AIPredictionAudit> actualStatus = predictions.stream()
                        .filter(p -> p.getActualOutcome() == status)
                        .collect(Collectors.toList());

                    if (actualStatus.isEmpty()) return 0.0;

                    long truePositives = actualStatus.stream()
                        .filter(p -> p.getPredictedStatus() == status)
                        .count();

                    return (double) truePositives / actualStatus.size();
                }
            ));
    }
}

