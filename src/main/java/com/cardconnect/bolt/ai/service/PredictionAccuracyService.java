package com.cardconnect.bolt.ai.service;

import com.cardconnect.bolt.ai.model.AIPredictionAudit;
import com.cardconnect.bolt.ai.model.BatchMetrics;
import com.cardconnect.bolt.ai.model.HealthStatus;
import com.cardconnect.bolt.ai.model.accuracy.ABTestResult;
import com.cardconnect.bolt.ai.model.accuracy.ABTestResult.ModelPerformance;
import com.cardconnect.bolt.ai.model.accuracy.ABTestResult.StatusComparison;
import com.cardconnect.bolt.ai.model.accuracy.AccuracyMetrics;
import com.cardconnect.bolt.ai.model.accuracy.AccuracyMetrics.ClassificationMetrics;
import com.cardconnect.bolt.ai.model.accuracy.AccuracyMetrics.ConfusionMatrix;
import com.cardconnect.bolt.ai.model.accuracy.FeedbackLoopData;
import com.cardconnect.bolt.ai.model.accuracy.FeedbackLoopData.*;
import com.cardconnect.bolt.ai.repository.AIPredictionAuditRepository;
import com.cardconnect.bolt.ai.repository.OfflineBatchMetricsRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Comprehensive service for tracking prediction accuracy, recording outcomes,
 * generating feedback loops, and conducting A/B tests between AI and rule-based models.
 */
@Service
@Transactional
@Slf4j
public class PredictionAccuracyService {

    private final AIPredictionAuditRepository auditRepository;
    private final OfflineBatchMetricsRepository metricsRepository;
    private final MeterRegistry meterRegistry;

    // Threshold for determining actual health status from metrics
    private static final double ERROR_RATE_WARNING = 0.05;
    private static final double ERROR_RATE_CRITICAL = 0.10;
    private static final double HIGH_CONFIDENCE_THRESHOLD = 0.8;

    public PredictionAccuracyService(
            AIPredictionAuditRepository auditRepository,
            OfflineBatchMetricsRepository metricsRepository,
            MeterRegistry meterRegistry) {
        this.auditRepository = auditRepository;
        this.metricsRepository = metricsRepository;
        this.meterRegistry = meterRegistry;
    }

    // ==================== Outcome Recording ====================

    /**
     * Record actual outcome for a prediction after the prediction window has passed
     */
    @Transactional
    public AIPredictionAudit recordOutcome(Long predictionId, HealthStatus actualOutcome, String notes) {
        AIPredictionAudit audit = auditRepository.findById(predictionId)
            .orElseThrow(() -> new IllegalArgumentException("Prediction not found: " + predictionId));

        audit.setActualOutcome(actualOutcome);
        audit.setOutcomeTimestamp(LocalDateTime.now());
        audit.setIsCorrect(audit.getPredictedStatus() == actualOutcome);
        audit.setEvaluationNotes(notes);

        AIPredictionAudit saved = auditRepository.save(audit);

        // Record metrics
        meterRegistry.counter("ai.prediction.outcome.recorded",
            Tags.of("predicted", audit.getPredictedStatus().name(),
                    "actual", actualOutcome.name(),
                    "correct", String.valueOf(saved.getIsCorrect()))).increment();

        log.info("Recorded outcome for prediction {}: predicted={}, actual={}, correct={}",
            predictionId, audit.getPredictedStatus(), actualOutcome, saved.getIsCorrect());

        return saved;
    }

    /**
     * Automatically evaluate predictions whose time horizon has passed
     * Runs every hour to check and evaluate pending predictions
     */
    @Scheduled(cron = "${ai.accuracy.evaluation-cron:0 0 * * * *}") // Every hour
    @Transactional
    public void evaluatePendingPredictions() {
        log.info("Starting automatic evaluation of pending predictions...");

        // Find predictions that should be evaluated (prediction time + horizon has passed)
        LocalDateTime threshold = LocalDateTime.now().minusHours(6); // Default 6-hour horizon
        List<AIPredictionAudit> pending = auditRepository.findPendingEvaluationBefore(threshold);

        if (pending.isEmpty()) {
            log.debug("No pending predictions to evaluate");
            return;
        }

        int evaluated = 0;
        for (AIPredictionAudit audit : pending) {
            try {
                HealthStatus actualStatus = determineActualStatus(audit);
                if (actualStatus != null) {
                    recordOutcome(audit.getId(), actualStatus, "Auto-evaluated based on metrics");
                    evaluated++;
                }
            } catch (Exception e) {
                log.warn("Failed to evaluate prediction {}: {}", audit.getId(), e.getMessage());
            }
        }

        log.info("Automatic evaluation complete: {} predictions evaluated", evaluated);
    }

    /**
     * Determine actual health status based on metrics at the time window
     */
    private HealthStatus determineActualStatus(AIPredictionAudit audit) {
        LocalDateTime predictionTime = audit.getPredictionTime();
        int horizon = audit.getTimeHorizon() != null ? audit.getTimeHorizon() : 6;

        LocalDateTime windowStart = predictionTime;
        LocalDateTime windowEnd = predictionTime.plusHours(horizon);

        // Get metrics for the prediction window
        List<BatchMetrics> metrics = metricsRepository.findByTimestampBetween(windowStart, windowEnd);

        if (metrics.isEmpty()) {
            return null; // Cannot determine
        }

        // Calculate error rate
        long totalProcessed = metrics.stream().mapToLong(BatchMetrics::getProcessedCount).sum();
        long totalErrors = metrics.stream().mapToLong(BatchMetrics::getErrorCount).sum();
        double errorRate = totalProcessed > 0 ? (double) totalErrors / totalProcessed : 0;

        // Store actual error rate
        audit.setActualErrorRate(errorRate);

        // Determine status
        if (errorRate >= ERROR_RATE_CRITICAL) {
            return HealthStatus.CRITICAL;
        } else if (errorRate >= ERROR_RATE_WARNING) {
            return HealthStatus.WARNING;
        } else {
            return HealthStatus.HEALTHY;
        }
    }

    // ==================== Accuracy Metrics ====================

    /**
     * Calculate comprehensive accuracy metrics for a time period
     */
    public AccuracyMetrics calculateAccuracyMetrics(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<AIPredictionAudit> predictions = auditRepository
            .findByPredictionTimeAfterAndActualOutcomeNotNull(since);

        if (predictions.isEmpty()) {
            return AccuracyMetrics.builder()
                .calculatedAt(LocalDateTime.now())
                .analysisWindow(days + " days")
                .totalPredictions(0L)
                .evaluatedPredictions(0L)
                .insights(List.of("No evaluated predictions found in the time period"))
                .build();
        }

        // Basic counts
        long total = auditRepository.countByPredictionTimeAfter(since);
        long evaluated = predictions.size();
        long correct = predictions.stream().filter(p -> Boolean.TRUE.equals(p.getIsCorrect())).count();
        double overallAccuracy = (double) correct / evaluated;
        double avgConfidence = predictions.stream()
            .mapToDouble(AIPredictionAudit::getConfidence).average().orElse(0);

        // Calculate per-status metrics
        Map<HealthStatus, ClassificationMetrics> metricsByStatus = new HashMap<>();
        List<HealthStatus> statuses = Arrays.asList(HealthStatus.HEALTHY, HealthStatus.WARNING,
            HealthStatus.CRITICAL, HealthStatus.UNKNOWN);

        for (HealthStatus status : statuses) {
            metricsByStatus.put(status, calculateClassificationMetrics(predictions, status));
        }

        // Calculate weighted and macro averages
        double weightedPrecision = 0, weightedRecall = 0, weightedF1 = 0;
        double macroPrecision = 0, macroRecall = 0, macroF1 = 0;
        long totalSupport = 0;
        int statusCount = 0;

        for (ClassificationMetrics cm : metricsByStatus.values()) {
            if (cm.getSupport() > 0) {
                weightedPrecision += cm.getPrecision() * cm.getSupport();
                weightedRecall += cm.getRecall() * cm.getSupport();
                weightedF1 += cm.getF1Score() * cm.getSupport();
                totalSupport += cm.getSupport();

                macroPrecision += cm.getPrecision();
                macroRecall += cm.getRecall();
                macroF1 += cm.getF1Score();
                statusCount++;
            }
        }

        if (totalSupport > 0) {
            weightedPrecision /= totalSupport;
            weightedRecall /= totalSupport;
            weightedF1 /= totalSupport;
        }
        if (statusCount > 0) {
            macroPrecision /= statusCount;
            macroRecall /= statusCount;
            macroF1 /= statusCount;
        }

        // Build confusion matrix
        ConfusionMatrix confusionMatrix = buildConfusionMatrix(predictions, statuses);

        // Calculate accuracy trend
        double accuracyTrend = calculateAccuracyTrend(days);

        // Calculate confidence calibration
        double calibration = calculateConfidenceCalibration(predictions);

        // Generate insights
        List<String> insights = generateAccuracyInsights(overallAccuracy, metricsByStatus,
            accuracyTrend, calibration);
        List<String> recommendations = generateAccuracyRecommendations(overallAccuracy,
            metricsByStatus, calibration);

        // Record metrics
        recordAccuracyMetrics(overallAccuracy, weightedF1, calibration);

        return AccuracyMetrics.builder()
            .calculatedAt(LocalDateTime.now())
            .analysisWindow(days + " days")
            .totalPredictions(total)
            .evaluatedPredictions(evaluated)
            .correctPredictions(correct)
            .overallAccuracy(overallAccuracy)
            .averageConfidence(avgConfidence)
            .metricsByStatus(metricsByStatus)
            .weightedPrecision(weightedPrecision)
            .weightedRecall(weightedRecall)
            .weightedF1Score(weightedF1)
            .macroPrecision(macroPrecision)
            .macroRecall(macroRecall)
            .macroF1Score(macroF1)
            .confusionMatrix(confusionMatrix)
            .accuracyTrend(accuracyTrend)
            .confidenceCalibration(calibration)
            .insights(insights)
            .recommendations(recommendations)
            .build();
    }

    private ClassificationMetrics calculateClassificationMetrics(
            List<AIPredictionAudit> predictions, HealthStatus status) {

        long truePositives = predictions.stream()
            .filter(p -> p.getPredictedStatus() == status && p.getActualOutcome() == status)
            .count();

        long falsePositives = predictions.stream()
            .filter(p -> p.getPredictedStatus() == status && p.getActualOutcome() != status)
            .count();

        long falseNegatives = predictions.stream()
            .filter(p -> p.getPredictedStatus() != status && p.getActualOutcome() == status)
            .count();

        long trueNegatives = predictions.stream()
            .filter(p -> p.getPredictedStatus() != status && p.getActualOutcome() != status)
            .count();

        long support = truePositives + falseNegatives; // Actual instances of this class

        double precision = (truePositives + falsePositives) > 0 ?
            (double) truePositives / (truePositives + falsePositives) : 0;
        double recall = (truePositives + falseNegatives) > 0 ?
            (double) truePositives / (truePositives + falseNegatives) : 0;
        double f1 = (precision + recall) > 0 ?
            2 * (precision * recall) / (precision + recall) : 0;
        double specificity = (trueNegatives + falsePositives) > 0 ?
            (double) trueNegatives / (trueNegatives + falsePositives) : 0;
        double accuracy = predictions.size() > 0 ?
            (double) (truePositives + trueNegatives) / predictions.size() : 0;

        return ClassificationMetrics.builder()
            .status(status)
            .truePositives(truePositives)
            .falsePositives(falsePositives)
            .falseNegatives(falseNegatives)
            .trueNegatives(trueNegatives)
            .support(support)
            .precision(precision)
            .recall(recall)
            .f1Score(f1)
            .specificity(specificity)
            .accuracy(accuracy)
            .build();
    }

    private ConfusionMatrix buildConfusionMatrix(List<AIPredictionAudit> predictions,
            List<HealthStatus> labels) {
        int size = labels.size();
        int[][] matrix = new int[size][size];

        for (AIPredictionAudit p : predictions) {
            int row = labels.indexOf(p.getPredictedStatus());
            int col = labels.indexOf(p.getActualOutcome());
            if (row >= 0 && col >= 0) {
                matrix[row][col]++;
            }
        }

        return ConfusionMatrix.builder()
            .matrix(matrix)
            .labels(labels)
            .build();
    }

    private double calculateAccuracyTrend(int days) {
        // Compare recent accuracy to older accuracy
        LocalDateTime now = LocalDateTime.now();
        int halfPeriod = days / 2;

        List<AIPredictionAudit> recent = auditRepository.findEvaluatedBetween(
            now.minusDays(halfPeriod), now);
        List<AIPredictionAudit> older = auditRepository.findEvaluatedBetween(
            now.minusDays(days), now.minusDays(halfPeriod));

        if (recent.isEmpty() || older.isEmpty()) {
            return 0.0;
        }

        double recentAccuracy = (double) recent.stream()
            .filter(p -> Boolean.TRUE.equals(p.getIsCorrect())).count() / recent.size();
        double olderAccuracy = (double) older.stream()
            .filter(p -> Boolean.TRUE.equals(p.getIsCorrect())).count() / older.size();

        return recentAccuracy - olderAccuracy; // Positive = improving
    }

    private double calculateConfidenceCalibration(List<AIPredictionAudit> predictions) {
        // Perfect calibration: when confidence=0.8, accuracy should be 80%
        // Calculate Expected Calibration Error (ECE)

        int numBins = 10;
        double[] binAccuracy = new double[numBins];
        double[] binConfidence = new double[numBins];
        int[] binCount = new int[numBins];

        for (AIPredictionAudit p : predictions) {
            int bin = Math.min((int) (p.getConfidence() * numBins), numBins - 1);
            binConfidence[bin] += p.getConfidence();
            binAccuracy[bin] += Boolean.TRUE.equals(p.getIsCorrect()) ? 1 : 0;
            binCount[bin]++;
        }

        double ece = 0;
        int totalCount = predictions.size();

        for (int i = 0; i < numBins; i++) {
            if (binCount[i] > 0) {
                double avgConf = binConfidence[i] / binCount[i];
                double avgAcc = binAccuracy[i] / binCount[i];
                ece += ((double) binCount[i] / totalCount) * Math.abs(avgAcc - avgConf);
            }
        }

        // Return calibration score (1 - ECE, so higher is better)
        return 1.0 - ece;
    }

    private List<String> generateAccuracyInsights(double accuracy,
            Map<HealthStatus, ClassificationMetrics> metrics, double trend, double calibration) {
        List<String> insights = new ArrayList<>();

        if (accuracy >= 0.9) {
            insights.add("Excellent overall accuracy at " + String.format("%.1f%%", accuracy * 100));
        } else if (accuracy >= 0.8) {
            insights.add("Good accuracy at " + String.format("%.1f%%", accuracy * 100));
        } else if (accuracy >= 0.7) {
            insights.add("Moderate accuracy at " + String.format("%.1f%%", accuracy * 100) + " - room for improvement");
        } else {
            insights.add("⚠️ Low accuracy at " + String.format("%.1f%%", accuracy * 100) + " - needs attention");
        }

        if (trend > 0.05) {
            insights.add("📈 Accuracy is improving (+%.1f%% recently)".formatted(trend * 100));
        } else if (trend < -0.05) {
            insights.add("📉 Accuracy is declining (%.1f%% recently)".formatted(trend * 100));
        }

        if (calibration >= 0.9) {
            insights.add("Confidence scores are well-calibrated");
        } else if (calibration < 0.8) {
            insights.add("⚠️ Confidence calibration needs improvement");
        }

        // Find weakest class
        Optional<Map.Entry<HealthStatus, ClassificationMetrics>> weakest = metrics.entrySet().stream()
            .filter(e -> e.getValue().getSupport() > 0)
            .min(Comparator.comparing(e -> e.getValue().getF1Score()));

        weakest.ifPresent(e -> {
            if (e.getValue().getF1Score() < 0.7) {
                insights.add("Weakest performance on " + e.getKey() + " status (F1: " +
                    String.format("%.2f", e.getValue().getF1Score()) + ")");
            }
        });

        return insights;
    }

    private List<String> generateAccuracyRecommendations(double accuracy,
            Map<HealthStatus, ClassificationMetrics> metrics, double calibration) {
        List<String> recommendations = new ArrayList<>();

        if (accuracy < 0.8) {
            recommendations.add("Consider retraining the model or adjusting prompts");
            recommendations.add("Review recent prediction errors for patterns");
        }

        if (calibration < 0.85) {
            recommendations.add("Implement confidence recalibration");
        }

        // Check for class imbalance issues
        ClassificationMetrics critical = metrics.get(HealthStatus.CRITICAL);
        if (critical != null && critical.getRecall() < 0.8 && critical.getSupport() > 0) {
            recommendations.add("⚠️ CRITICAL status recall is low - increase sensitivity for critical predictions");
        }

        if (recommendations.isEmpty()) {
            recommendations.add("Continue current monitoring and evaluation practices");
        }

        return recommendations;
    }

    private void recordAccuracyMetrics(double accuracy, double f1, double calibration) {
        meterRegistry.gauge("ai.prediction.accuracy.overall", accuracy);
        meterRegistry.gauge("ai.prediction.accuracy.f1.weighted", f1);
        meterRegistry.gauge("ai.prediction.accuracy.calibration", calibration);
    }

    // ==================== A/B Testing ====================

    /**
     * Compare AI vs rule-based model performance
     */
    public ABTestResult compareModels(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);

        // Get performance for each model
        ModelPerformance aiPerformance = calculateModelPerformance("AI", since);
        ModelPerformance ruleBasedPerformance = calculateModelPerformance("RULE_BASED", since);

        // Calculate differences
        double accuracyDiff = safeSubtract(aiPerformance.getAccuracy(), ruleBasedPerformance.getAccuracy());
        double f1Diff = safeSubtract(aiPerformance.getF1Score(), ruleBasedPerformance.getF1Score());
        double confDiff = safeSubtract(aiPerformance.getAverageConfidence(), ruleBasedPerformance.getAverageConfidence());

        // Determine winner
        String winner = determineWinner(aiPerformance, ruleBasedPerformance);
        double winMargin = Math.abs(accuracyDiff);

        // Calculate per-status comparison
        Map<HealthStatus, StatusComparison> statusComparison = calculateStatusComparison(since);

        // Calculate statistical significance (simplified)
        double pValue = calculatePValue(aiPerformance, ruleBasedPerformance);
        boolean isSignificant = pValue < 0.05;
        double effectSize = calculateEffectSize(aiPerformance, ruleBasedPerformance);

        // Generate insights
        List<String> insights = generateABTestInsights(aiPerformance, ruleBasedPerformance, winner, isSignificant);
        List<String> recommendations = generateABTestRecommendations(aiPerformance, ruleBasedPerformance, winner);

        return ABTestResult.builder()
            .testPeriodStart(since)
            .testPeriodEnd(LocalDateTime.now())
            .analysisWindow(days + " days")
            .aiModelPerformance(aiPerformance)
            .ruleBasedPerformance(ruleBasedPerformance)
            .accuracyDifference(accuracyDiff)
            .f1ScoreDifference(f1Diff)
            .confidenceDifference(confDiff)
            .pValue(pValue)
            .isStatisticallySignificant(isSignificant)
            .effectSize(effectSize)
            .winner(winner)
            .winMargin(winMargin)
            .insights(insights)
            .recommendations(recommendations)
            .comparisonByStatus(statusComparison)
            .build();
    }

    private ModelPerformance calculateModelPerformance(String modelType, LocalDateTime since) {
        List<AIPredictionAudit> predictions = auditRepository.findEvaluatedByModelType(modelType, since);
        List<AIPredictionAudit> allPredictions = auditRepository.findByModelTypeAndPredictionTimeAfter(modelType, since);

        long total = allPredictions.size();
        long evaluated = predictions.size();
        long correct = predictions.stream().filter(p -> Boolean.TRUE.equals(p.getIsCorrect())).count();
        Long errorCount = auditRepository.countErrorsByModelType(modelType, since);

        double accuracy = evaluated > 0 ? (double) correct / evaluated : 0;
        Double avgConfidence = auditRepository.averageConfidenceByModelType(modelType, since);
        Double avgResponseTime = auditRepository.averageResponseTimeByModelType(modelType, since);

        // Calculate precision, recall, F1 (simplified - treating as binary: correct vs incorrect)
        double precision = accuracy; // Simplified
        double recall = accuracy;    // Simplified
        double f1 = accuracy;        // Simplified for overall

        // Per-status accuracy
        Map<HealthStatus, Double> accuracyByStatus = new HashMap<>();
        for (HealthStatus status : HealthStatus.values()) {
            List<AIPredictionAudit> statusPreds = predictions.stream()
                .filter(p -> p.getPredictedStatus() == status)
                .collect(Collectors.toList());
            if (!statusPreds.isEmpty()) {
                double statusAcc = (double) statusPreds.stream()
                    .filter(p -> Boolean.TRUE.equals(p.getIsCorrect())).count() / statusPreds.size();
                accuracyByStatus.put(status, statusAcc);
            }
        }

        return ModelPerformance.builder()
            .modelType(modelType)
            .totalPredictions(total)
            .evaluatedPredictions(evaluated)
            .correctPredictions(correct)
            .accuracy(accuracy)
            .precision(precision)
            .recall(recall)
            .f1Score(f1)
            .averageConfidence(avgConfidence != null ? avgConfidence : 0)
            .averageResponseTimeMs(avgResponseTime != null ? avgResponseTime : 0)
            .errorCount(errorCount != null ? errorCount : 0)
            .accuracyByStatus(accuracyByStatus)
            .build();
    }

    private Map<HealthStatus, StatusComparison> calculateStatusComparison(LocalDateTime since) {
        Map<HealthStatus, StatusComparison> result = new HashMap<>();

        for (HealthStatus status : HealthStatus.values()) {
            List<AIPredictionAudit> aiPreds = auditRepository.findEvaluatedByModelType("AI", since)
                .stream().filter(p -> p.getPredictedStatus() == status).collect(Collectors.toList());
            List<AIPredictionAudit> rulePreds = auditRepository.findEvaluatedByModelType("RULE_BASED", since)
                .stream().filter(p -> p.getPredictedStatus() == status).collect(Collectors.toList());

            double aiAcc = aiPreds.isEmpty() ? 0 :
                (double) aiPreds.stream().filter(p -> Boolean.TRUE.equals(p.getIsCorrect())).count() / aiPreds.size();
            double ruleAcc = rulePreds.isEmpty() ? 0 :
                (double) rulePreds.stream().filter(p -> Boolean.TRUE.equals(p.getIsCorrect())).count() / rulePreds.size();

            String better = aiAcc > ruleAcc ? "AI" : (ruleAcc > aiAcc ? "RULE_BASED" : "TIE");

            result.put(status, StatusComparison.builder()
                .status(status)
                .aiAccuracy(aiAcc)
                .ruleBasedAccuracy(ruleAcc)
                .aiF1Score(aiAcc) // Simplified
                .ruleBasedF1Score(ruleAcc)
                .aiCount((long) aiPreds.size())
                .ruleBasedCount((long) rulePreds.size())
                .betterModel(better)
                .build());
        }

        return result;
    }

    private String determineWinner(ModelPerformance ai, ModelPerformance ruleBased) {
        double aiScore = safeValue(ai.getAccuracy()) * 0.6 + safeValue(ai.getF1Score()) * 0.4;
        double ruleScore = safeValue(ruleBased.getAccuracy()) * 0.6 + safeValue(ruleBased.getF1Score()) * 0.4;

        if (Math.abs(aiScore - ruleScore) < 0.02) return "TIE";
        return aiScore > ruleScore ? "AI" : "RULE_BASED";
    }

    private double calculatePValue(ModelPerformance ai, ModelPerformance ruleBased) {
        // Simplified p-value calculation using normal approximation
        // In production, use proper statistical test
        long n1 = ai.getEvaluatedPredictions();
        long n2 = ruleBased.getEvaluatedPredictions();

        if (n1 < 30 || n2 < 30) return 1.0; // Not enough data

        double p1 = safeValue(ai.getAccuracy());
        double p2 = safeValue(ruleBased.getAccuracy());
        double pooled = (p1 * n1 + p2 * n2) / (n1 + n2);

        if (pooled <= 0 || pooled >= 1) return 1.0;

        double se = Math.sqrt(pooled * (1 - pooled) * (1.0/n1 + 1.0/n2));
        if (se <= 0) return 1.0;

        double z = Math.abs(p1 - p2) / se;

        // Approximate p-value from z-score
        return 2 * (1 - normalCdf(z));
    }

    private double normalCdf(double z) {
        // Approximation of normal CDF
        return 0.5 * (1 + Math.tanh(Math.sqrt(2/Math.PI) * (z + 0.044715 * Math.pow(z, 3))));
    }

    private double calculateEffectSize(ModelPerformance ai, ModelPerformance ruleBased) {
        // Cohen's d (simplified)
        double diff = safeValue(ai.getAccuracy()) - safeValue(ruleBased.getAccuracy());
        double pooledStd = 0.2; // Assumed standard deviation
        return Math.abs(diff / pooledStd);
    }

    private List<String> generateABTestInsights(ModelPerformance ai, ModelPerformance ruleBased,
            String winner, boolean isSignificant) {
        List<String> insights = new ArrayList<>();

        if ("AI".equals(winner)) {
            insights.add("AI model outperforms rule-based by " +
                String.format("%.1f%%", (safeValue(ai.getAccuracy()) - safeValue(ruleBased.getAccuracy())) * 100));
        } else if ("RULE_BASED".equals(winner)) {
            insights.add("Rule-based model outperforms AI by " +
                String.format("%.1f%%", (safeValue(ruleBased.getAccuracy()) - safeValue(ai.getAccuracy())) * 100));
        } else {
            insights.add("Both models perform similarly");
        }

        if (isSignificant) {
            insights.add("✓ Results are statistically significant");
        } else {
            insights.add("⚠️ Results are not statistically significant - need more data");
        }

        if (ai.getAverageResponseTimeMs() < ruleBased.getAverageResponseTimeMs()) {
            insights.add("AI model is faster (avg " + String.format("%.0f", ai.getAverageResponseTimeMs()) + "ms)");
        }

        return insights;
    }

    private List<String> generateABTestRecommendations(ModelPerformance ai, ModelPerformance ruleBased,
            String winner) {
        List<String> recommendations = new ArrayList<>();

        if ("AI".equals(winner) && safeValue(ai.getAccuracy()) >= 0.85) {
            recommendations.add("Continue using AI model as primary");
        } else if ("RULE_BASED".equals(winner)) {
            recommendations.add("Consider improving AI model or increasing rule-based usage");
        }

        if (ai.getErrorCount() > ai.getTotalPredictions() * 0.1) {
            recommendations.add("⚠️ High AI error rate - investigate model stability");
        }

        if (ai.getEvaluatedPredictions() < 50) {
            recommendations.add("Collect more data for reliable comparison");
        }

        return recommendations;
    }

    // ==================== Feedback Loop ====================

    /**
     * Generate feedback loop data for model improvement
     */
    public FeedbackLoopData generateFeedbackLoopData(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);

        // Get high-confidence errors
        List<AIPredictionAudit> highConfErrors = auditRepository.findHighConfidenceErrors(
            HIGH_CONFIDENCE_THRESHOLD, since);

        List<HighConfidenceError> hcErrors = highConfErrors.stream()
            .limit(10)
            .map(this::toHighConfidenceError)
            .collect(Collectors.toList());

        // Calculate error patterns
        List<ErrorPattern> errorPatterns = calculateErrorPatterns(since);

        // Calculate misclassification patterns
        List<MisclassificationPattern> misclassPatterns = calculateMisclassificationPatterns(since);

        // Generate feature insights
        List<FeatureInsight> featureInsights = generateFeatureInsights(since);

        // Generate improvement recommendations
        List<ImprovementRecommendation> improvements = generateImprovementRecommendations(
            errorPatterns, misclassPatterns, hcErrors);

        // Detect drift
        DriftAnalysis driftAnalysis = detectDrift(days);

        return FeedbackLoopData.builder()
            .generatedAt(LocalDateTime.now())
            .analysisWindow(days + " days")
            .errorPatterns(errorPatterns)
            .highConfidenceErrors(hcErrors)
            .misclassificationPatterns(misclassPatterns)
            .featureInsights(featureInsights)
            .improvementRecommendations(improvements)
            .driftAnalysis(driftAnalysis)
            .build();
    }

    private HighConfidenceError toHighConfidenceError(AIPredictionAudit audit) {
        List<String> causes = new ArrayList<>();
        if (audit.getActualErrorRate() != null && audit.getPredictedErrorRate() != null) {
            if (audit.getActualErrorRate() > audit.getPredictedErrorRate()) {
                causes.add("Underestimated error rate");
            } else {
                causes.add("Overestimated error rate");
            }
        }
        causes.add("Review input metrics quality");

        return HighConfidenceError.builder()
            .predictionId(audit.getId())
            .predictionTime(audit.getPredictionTime())
            .predicted(audit.getPredictedStatus())
            .actual(audit.getActualOutcome())
            .confidence(audit.getConfidence())
            .reasoning(audit.getReasoning())
            .possibleCauses(causes)
            .build();
    }

    private List<ErrorPattern> calculateErrorPatterns(LocalDateTime since) {
        List<ErrorPattern> patterns = new ArrayList<>();

        for (HealthStatus predicted : HealthStatus.values()) {
            for (HealthStatus actual : HealthStatus.values()) {
                if (predicted != actual) {
                    Long count = auditRepository.countMisclassifications(predicted, actual, since);
                    if (count != null && count > 0) {
                        List<AIPredictionAudit> examples = auditRepository.findMisclassifications(
                            predicted, actual, since);

                        double avgConf = examples.stream()
                            .mapToDouble(AIPredictionAudit::getConfidence)
                            .average().orElse(0);

                        patterns.add(ErrorPattern.builder()
                            .predicted(predicted)
                            .actual(actual)
                            .occurrenceCount(count)
                            .averageConfidence(avgConf)
                            .likelyCause(identifyLikelyCause(predicted, actual))
                            .build());
                    }
                }
            }
        }

        patterns.sort((a, b) -> Long.compare(b.getOccurrenceCount(), a.getOccurrenceCount()));
        return patterns.stream().limit(5).collect(Collectors.toList());
    }

    private String identifyLikelyCause(HealthStatus predicted, HealthStatus actual) {
        if (predicted == HealthStatus.HEALTHY && actual == HealthStatus.WARNING) {
            return "Model too optimistic - increase sensitivity to warning signs";
        }
        if (predicted == HealthStatus.HEALTHY && actual == HealthStatus.CRITICAL) {
            return "Critical miss - model failed to detect serious issues";
        }
        if (predicted == HealthStatus.CRITICAL && actual == HealthStatus.HEALTHY) {
            return "False alarm - model too pessimistic";
        }
        if (predicted == HealthStatus.WARNING && actual == HealthStatus.CRITICAL) {
            return "Underestimated severity";
        }
        return "Threshold calibration needed";
    }

    private List<MisclassificationPattern> calculateMisclassificationPatterns(LocalDateTime since) {
        List<AIPredictionAudit> errors = auditRepository.findByPredictionTimeAfterAndActualOutcomeNotNull(since)
            .stream()
            .filter(p -> Boolean.FALSE.equals(p.getIsCorrect()))
            .collect(Collectors.toList());

        if (errors.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Long> patternCounts = errors.stream()
            .collect(Collectors.groupingBy(
                p -> p.getPredictedStatus() + "->" + p.getActualOutcome(),
                Collectors.counting()));

        long totalErrors = errors.size();

        return patternCounts.entrySet().stream()
            .map(e -> {
                String[] parts = e.getKey().split("->");
                return MisclassificationPattern.builder()
                    .fromStatus(HealthStatus.valueOf(parts[0]))
                    .toStatus(HealthStatus.valueOf(parts[1]))
                    .count(e.getValue())
                    .percentageOfErrors((double) e.getValue() / totalErrors * 100)
                    .description(parts[0] + " misclassified as " + parts[1])
                    .build();
            })
            .sorted((a, b) -> Long.compare(b.getCount(), a.getCount()))
            .collect(Collectors.toList());
    }

    private List<FeatureInsight> generateFeatureInsights(LocalDateTime since) {
        List<FeatureInsight> insights = new ArrayList<>();

        // Analyze correlation between error rate and prediction errors
        List<AIPredictionAudit> predictions = auditRepository.findByPredictionTimeAfterAndActualOutcomeNotNull(since);

        // Check if high confidence correlates with correctness
        double highConfCorrect = predictions.stream()
            .filter(p -> p.getConfidence() >= 0.8)
            .filter(p -> Boolean.TRUE.equals(p.getIsCorrect()))
            .count();
        double highConfTotal = predictions.stream()
            .filter(p -> p.getConfidence() >= 0.8)
            .count();

        if (highConfTotal > 0) {
            double ratio = highConfCorrect / highConfTotal;
            if (ratio < 0.85) {
                insights.add(FeatureInsight.builder()
                    .featureName("Confidence Score")
                    .correlationWithError(1 - ratio)
                    .insight("High confidence predictions are not reliable enough")
                    .recommendation("Recalibrate confidence scoring")
                    .build());
            }
        }

        return insights;
    }

    private List<ImprovementRecommendation> generateImprovementRecommendations(
            List<ErrorPattern> errorPatterns,
            List<MisclassificationPattern> misclassPatterns,
            List<HighConfidenceError> hcErrors) {

        List<ImprovementRecommendation> recommendations = new ArrayList<>();

        // Check for critical misses
        boolean hasCriticalMiss = errorPatterns.stream()
            .anyMatch(e -> e.getPredicted() == HealthStatus.HEALTHY &&
                          e.getActual() == HealthStatus.CRITICAL);

        if (hasCriticalMiss) {
            recommendations.add(ImprovementRecommendation.builder()
                .area("THRESHOLDS")
                .priority("HIGH")
                .recommendation("Lower the threshold for CRITICAL detection to reduce misses")
                .expectedImprovementPercent(10.0)
                .rationale("Critical issues were predicted as HEALTHY")
                .build());
        }

        // Check for too many false alarms
        long falseAlarms = errorPatterns.stream()
            .filter(e -> e.getPredicted() == HealthStatus.CRITICAL &&
                        e.getActual() == HealthStatus.HEALTHY)
            .mapToLong(ErrorPattern::getOccurrenceCount)
            .sum();

        if (falseAlarms > 5) {
            recommendations.add(ImprovementRecommendation.builder()
                .area("MODEL")
                .priority("MEDIUM")
                .recommendation("Reduce false CRITICAL predictions")
                .expectedImprovementPercent(5.0)
                .rationale(falseAlarms + " false CRITICAL alarms detected")
                .build());
        }

        // Check high-confidence errors
        if (!hcErrors.isEmpty()) {
            recommendations.add(ImprovementRecommendation.builder()
                .area("PROMPT")
                .priority("HIGH")
                .recommendation("Review and improve prompts for edge cases")
                .expectedImprovementPercent(8.0)
                .rationale(hcErrors.size() + " high-confidence errors detected")
                .build());
        }

        if (recommendations.isEmpty()) {
            recommendations.add(ImprovementRecommendation.builder()
                .area("MODEL")
                .priority("LOW")
                .recommendation("Continue monitoring - no significant issues detected")
                .expectedImprovementPercent(0.0)
                .rationale("Model performing within acceptable parameters")
                .build());
        }

        return recommendations;
    }

    private DriftAnalysis detectDrift(int days) {
        // Compare recent performance to baseline
        int halfPeriod = days / 2;
        LocalDateTime now = LocalDateTime.now();

        List<AIPredictionAudit> recent = auditRepository.findEvaluatedBetween(
            now.minusDays(halfPeriod), now);
        List<AIPredictionAudit> baseline = auditRepository.findEvaluatedBetween(
            now.minusDays(days), now.minusDays(halfPeriod));

        if (recent.isEmpty() || baseline.isEmpty()) {
            return DriftAnalysis.builder()
                .driftDetected(false)
                .driftScore(0.0)
                .driftType("NONE")
                .description("Insufficient data for drift detection")
                .recommendations(List.of("Collect more data"))
                .build();
        }

        double recentAccuracy = (double) recent.stream()
            .filter(p -> Boolean.TRUE.equals(p.getIsCorrect())).count() / recent.size();
        double baselineAccuracy = (double) baseline.stream()
            .filter(p -> Boolean.TRUE.equals(p.getIsCorrect())).count() / baseline.size();

        double drift = baselineAccuracy - recentAccuracy;
        boolean driftDetected = Math.abs(drift) > 0.1; // 10% change

        String driftType = "NONE";
        if (driftDetected) {
            driftType = drift > 0 ? "CONCEPT_DRIFT" : "DATA_DRIFT";
        }

        List<String> recs = new ArrayList<>();
        if (driftDetected) {
            recs.add("Investigate recent changes in data patterns");
            recs.add("Consider retraining or adjusting model");
        } else {
            recs.add("Continue normal monitoring");
        }

        return DriftAnalysis.builder()
            .driftDetected(driftDetected)
            .driftScore(Math.abs(drift))
            .driftType(driftType)
            .driftStartDate(driftDetected ? now.minusDays(halfPeriod) : null)
            .description(driftDetected ?
                "Accuracy changed by " + String.format("%.1f%%", drift * 100) :
                "No significant drift detected")
            .recommendations(recs)
            .build();
    }

    // ==================== Utility Methods ====================

    private double safeValue(Double value) {
        return value != null ? value : 0.0;
    }

    private double safeSubtract(Double a, Double b) {
        return safeValue(a) - safeValue(b);
    }
}

