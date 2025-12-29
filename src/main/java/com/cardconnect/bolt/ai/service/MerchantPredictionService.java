package com.cardconnect.bolt.ai.service;

import com.cardconnect.bolt.ai.config.VectorStoreConfiguration.SimpleDocumentStore;
import com.cardconnect.bolt.ai.config.properties.AIPredictionProperties;
import com.cardconnect.bolt.ai.config.properties.AIPromptProperties;
import com.cardconnect.bolt.ai.model.BatchMetrics;
import com.cardconnect.bolt.ai.model.HealthStatus;
import com.cardconnect.bolt.ai.model.TrendAnalysis;
import com.cardconnect.bolt.ai.model.merchant.MerchantAlertThreshold;
import com.cardconnect.bolt.ai.model.merchant.MerchantComparison;
import com.cardconnect.bolt.ai.model.merchant.MerchantComparison.MerchantComparisonDetail;
import com.cardconnect.bolt.ai.model.merchant.MerchantMetricsSummary;
import com.cardconnect.bolt.ai.model.merchant.MerchantPrediction;
import com.cardconnect.bolt.ai.model.merchant.MerchantRiskRanking;
import com.cardconnect.bolt.ai.model.merchant.MerchantRiskRanking.MerchantRiskEntry;
import com.cardconnect.bolt.ai.repository.MerchantAlertThresholdRepository;
import com.cardconnect.bolt.ai.repository.OfflineBatchMetricsRepository;
import com.cardconnect.bolt.ai.util.AIInputValidator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for merchant-specific AI predictions and analysis
 * Provides per-merchant health predictions, comparisons, and risk rankings
 */
@Service
@Transactional
@Slf4j
@ConditionalOnProperty(name = "ai.prediction.enabled", havingValue = "true")
public class MerchantPredictionService {

    private final ChatClient chatClient;
    private final OfflineBatchMetricsRepository metricsRepository;
    private final MerchantAlertThresholdRepository thresholdRepository;
    private final SimpleDocumentStore documentStore;
    private final EmbeddingModel embeddingModel;
    private final AIInputValidator inputValidator;
    private final ObjectMapper objectMapper;
    private final AIPromptProperties promptProperties;
    private final AIPredictionProperties predictionProperties;

    private static final String MERCHANT_PREDICTION_SYSTEM_PROMPT = """
        You are an expert system health analyst specializing in payment processing for individual merchants.
        Your task is to analyze offline batch processing metrics for a specific merchant and predict potential issues.
        
        Consider:
        - Error rate patterns specific to this merchant
        - Processing time trends compared to their historical baseline
        - Volume patterns and anomalies
        - Comparison to system-wide averages
        - Time-of-day patterns
        
        Provide predictions in JSON format with the following structure:
        {
            "predictedStatus": "HEALTHY|WARNING|CRITICAL|UNKNOWN",
            "confidence": 0.0-1.0,
            "riskScore": 0.0-1.0,
            "keyFindings": ["finding1", "finding2"],
            "trendAnalysis": {
                "errorRateTrend": "stable|increasing|decreasing",
                "processingTimeTrend": "stable|increasing|decreasing",
                "anomalyDetected": true|false
            },
            "riskFactors": ["factor1", "factor2"],
            "recommendations": ["rec1", "rec2"],
            "reasoning": "Detailed explanation"
        }
        
        Be specific to this merchant's patterns and provide actionable recommendations.
        """;

    public MerchantPredictionService(
            ChatClient.Builder chatClientBuilder,
            OfflineBatchMetricsRepository metricsRepository,
            MerchantAlertThresholdRepository thresholdRepository,
            SimpleDocumentStore documentStore,
            EmbeddingModel embeddingModel,
            AIInputValidator inputValidator,
            ObjectMapper objectMapper,
            AIPromptProperties promptProperties,
            AIPredictionProperties predictionProperties) {
        this.chatClient = chatClientBuilder.build();
        this.metricsRepository = metricsRepository;
        this.thresholdRepository = thresholdRepository;
        this.documentStore = documentStore;
        this.embeddingModel = embeddingModel;
        this.inputValidator = inputValidator;
        this.objectMapper = objectMapper;
        this.promptProperties = promptProperties;
        this.predictionProperties = predictionProperties;
    }

    /**
     * Get AI prediction for a specific merchant
     */
    @Cacheable(value = "merchantPredictions", key = "#merchId", unless = "#result.error != null")
    public MerchantPrediction predictMerchantHealth(String merchId) {
        try {
            log.info("Generating AI prediction for merchant: {}", merchId);

            int analysisWindowHours = predictionProperties.getAnalysisWindowHours();
            LocalDateTime since = LocalDateTime.now().minusHours(analysisWindowHours);

            List<BatchMetrics> metrics = metricsRepository.findByMerchIdAndTimestampBetween(
                merchId, since, LocalDateTime.now());

            if (metrics.isEmpty()) {
                return MerchantPrediction.insufficientData(merchId);
            }

            inputValidator.validateMetrics(metrics);

            // Get merchant-specific thresholds or use defaults
            MerchantAlertThreshold thresholds = thresholdRepository.findByMerchId(merchId)
                .orElse(getDefaultThresholds(merchId));

            // Build merchant metrics summary
            MerchantMetricsSummary summary = buildMetricsSummary(merchId, metrics, analysisWindowHours);

            // Get system-wide metrics for comparison
            List<BatchMetrics> systemMetrics = metricsRepository.findByTimestampAfter(since);
            DoubleSummaryStatistics systemErrorStats = calculateSystemErrorStats(systemMetrics);
            DoubleSummaryStatistics systemTimeStats = calculateSystemTimeStats(systemMetrics);

            // Build context for AI
            String metricsContext = buildMerchantMetricsContext(merchId, metrics, summary,
                    systemErrorStats, systemTimeStats, thresholds);

            // Get AI prediction
            String userPrompt = String.format("""
                Analyze the following metrics for merchant %s and predict health status for the next %d hours.
                
                %s
                
                Provide your analysis in the JSON format specified in the system prompt.
                Focus on merchant-specific patterns and risks.
                """, merchId, predictionProperties.getForecastHorizonHours(), metricsContext);

            Prompt prompt = new Prompt(List.of(
                new SystemMessage(MERCHANT_PREDICTION_SYSTEM_PROMPT),
                new UserMessage(userPrompt)
            ));

            ChatResponse response = chatClient.prompt(prompt).call().chatResponse();
            String aiResponse = response.getResult().getOutput().getText();

            MerchantPrediction result = parseMerchantPrediction(aiResponse, merchId);
            result.setMetricsSummary(summary);
            result.setPredictionTime(LocalDateTime.now());
            result.setTimeHorizon(predictionProperties.getForecastHorizonHours());

            // Adjust prediction based on custom thresholds
            adjustPredictionWithThresholds(result, summary, thresholds);

            log.info("Merchant {} prediction: {} (risk: {}, confidence: {})",
                    merchId, result.getPredictedStatus(),
                    String.format("%.2f", result.getRiskScore()),
                    String.format("%.2f", result.getConfidence()));

            return result;

        } catch (Exception e) {
            log.error("Failed to generate prediction for merchant: {}", merchId, e);
            return MerchantPrediction.error(merchId, "Prediction failed: " + e.getMessage());
        }
    }

    /**
     * Get predictions for all active merchants
     */
    public List<MerchantPrediction> predictAllMerchants() {
        int analysisWindowHours = predictionProperties.getAnalysisWindowHours();
        LocalDateTime since = LocalDateTime.now().minusHours(analysisWindowHours);

        List<String> activeMerchants = metricsRepository.findDistinctMerchIdsSince(since);

        log.info("Generating predictions for {} active merchants", activeMerchants.size());

        return activeMerchants.stream()
            .map(this::predictMerchantHealth)
            .collect(Collectors.toList());
    }

    /**
     * Compare health metrics across all merchants
     */
    @Cacheable(value = "merchantComparison", key = "'comparison'")
    public MerchantComparison compareMerchants() {
        try {
            int analysisWindowHours = predictionProperties.getAnalysisWindowHours();
            LocalDateTime since = LocalDateTime.now().minusHours(analysisWindowHours);

            List<String> activeMerchants = metricsRepository.findDistinctMerchIdsSince(since);

            if (activeMerchants.isEmpty()) {
                return MerchantComparison.builder()
                    .comparisonTime(LocalDateTime.now())
                    .analysisWindow(analysisWindowHours + " hours")
                    .totalMerchantsAnalyzed(0)
                    .merchantDetails(Collections.emptyList())
                    .build();
            }

            // Calculate metrics for each merchant
            List<MerchantComparisonDetail> details = new ArrayList<>();
            Map<String, Double> errorRates = new HashMap<>();
            Map<String, Double> processingTimes = new HashMap<>();

            for (String merchId : activeMerchants) {
                List<BatchMetrics> metrics = metricsRepository.findByMerchIdAndTimestampBetween(
                    merchId, since, LocalDateTime.now());

                if (!metrics.isEmpty()) {
                    MerchantMetricsSummary summary = buildMetricsSummary(merchId, metrics, analysisWindowHours);

                    errorRates.put(merchId, summary.getErrorRate());
                    processingTimes.put(merchId, summary.getAvgProcessingTimeMs());

                    MerchantComparisonDetail detail = MerchantComparisonDetail.builder()
                        .merchId(merchId)
                        .hsn(metrics.get(0).getHsn())
                        .errorRate(summary.getErrorRate())
                        .avgProcessingTimeMs(summary.getAvgProcessingTimeMs())
                        .totalBatches(summary.getTotalBatches())
                        .totalErrors(summary.getTotalErrors())
                        .build();

                    details.add(detail);
                }
            }

            // Calculate rankings
            calculateRankings(details, errorRates, processingTimes);

            // Calculate averages for comparison
            double avgErrorRate = errorRates.values().stream().mapToDouble(d -> d).average().orElse(0);
            double avgProcessingTime = processingTimes.values().stream().mapToDouble(d -> d).average().orElse(0);

            for (MerchantComparisonDetail detail : details) {
                detail.setErrorRateVsAverage(avgErrorRate > 0 ?
                    ((detail.getErrorRate() - avgErrorRate) / avgErrorRate) * 100 : 0);
                detail.setProcessingTimeVsAverage(avgProcessingTime > 0 ?
                    ((detail.getAvgProcessingTimeMs() - avgProcessingTime) / avgProcessingTime) * 100 : 0);

                // Calculate health status and risk score
                double riskScore = calculateRiskScore(detail.getErrorRate(),
                    detail.getAvgProcessingTimeMs(), avgErrorRate, avgProcessingTime);
                detail.setRiskScore(riskScore);
                detail.setHealthStatus(determineHealthStatus(riskScore));
            }

            // Sort by risk score descending
            details.sort((a, b) -> Double.compare(b.getRiskScore(), a.getRiskScore()));

            // Assign overall risk rank
            for (int i = 0; i < details.size(); i++) {
                details.get(i).setOverallRiskRank(i + 1);
            }

            // Get AI insights for comparison
            List<String> insights = generateComparisonInsights(details);

            // Count by health status
            Map<HealthStatus, Long> statusCounts = details.stream()
                .collect(Collectors.groupingBy(MerchantComparisonDetail::getHealthStatus, Collectors.counting()));

            return MerchantComparison.builder()
                .comparisonTime(LocalDateTime.now())
                .analysisWindow(analysisWindowHours + " hours")
                .totalMerchantsAnalyzed(details.size())
                .healthyCount(statusCounts.getOrDefault(HealthStatus.HEALTHY, 0L).intValue())
                .warningCount(statusCounts.getOrDefault(HealthStatus.WARNING, 0L).intValue())
                .criticalCount(statusCounts.getOrDefault(HealthStatus.CRITICAL, 0L).intValue())
                .unknownCount(statusCounts.getOrDefault(HealthStatus.UNKNOWN, 0L).intValue())
                .merchantDetails(details)
                .insights(insights)
                .build();

        } catch (Exception e) {
            log.error("Failed to compare merchants", e);
            return MerchantComparison.builder()
                .comparisonTime(LocalDateTime.now())
                .totalMerchantsAnalyzed(0)
                .insights(List.of("Error: " + e.getMessage()))
                .build();
        }
    }

    /**
     * Get risk ranking of all merchants
     */
    @Cacheable(value = "merchantRiskRanking", key = "'ranking'")
    public MerchantRiskRanking getMerchantRiskRanking() {
        try {
            List<MerchantPrediction> predictions = predictAllMerchants();

            if (predictions.isEmpty()) {
                return MerchantRiskRanking.builder()
                    .rankingTime(LocalDateTime.now())
                    .totalMerchantsRanked(0)
                    .rankings(Collections.emptyList())
                    .build();
            }

            // Sort by risk score (highest first)
            predictions.sort((a, b) -> Double.compare(
                b.getRiskScore() != null ? b.getRiskScore() : 0,
                a.getRiskScore() != null ? a.getRiskScore() : 0));

            List<MerchantRiskEntry> rankings = new ArrayList<>();
            int rank = 1;

            for (MerchantPrediction pred : predictions) {
                if (pred.getError() != null) continue;

                MerchantMetricsSummary summary = pred.getMetricsSummary();

                MerchantRiskEntry entry = MerchantRiskEntry.builder()
                    .rank(rank++)
                    .merchId(pred.getMerchId())
                    .hsn(pred.getHsn())
                    .riskScore(pred.getRiskScore())
                    .predictedStatus(pred.getPredictedStatus())
                    .confidence(pred.getConfidence())
                    .riskFactors(pred.getRiskFactors())
                    .build();

                if (summary != null) {
                    entry.setErrorRate(summary.getErrorRate());
                    entry.setAvgProcessingTimeMs(summary.getAvgProcessingTimeMs());
                    entry.setTotalBatches(summary.getTotalBatches());
                    entry.setTotalErrors(summary.getTotalErrors());

                    // Calculate contribution factors
                    calculateRiskContributions(entry, summary);
                }

                rankings.add(entry);
            }

            // Calculate statistics
            DoubleSummaryStatistics riskStats = rankings.stream()
                .mapToDouble(MerchantRiskEntry::getRiskScore)
                .summaryStatistics();

            double medianRisk = calculateMedian(rankings.stream()
                .map(MerchantRiskEntry::getRiskScore)
                .collect(Collectors.toList()));

            int highRiskCount = (int) rankings.stream().filter(r -> r.getRiskScore() > 0.7).count();
            int mediumRiskCount = (int) rankings.stream().filter(r -> r.getRiskScore() >= 0.4 && r.getRiskScore() <= 0.7).count();
            int lowRiskCount = (int) rankings.stream().filter(r -> r.getRiskScore() < 0.4).count();

            // Get top risk factors across all merchants
            List<String> topRiskFactors = rankings.stream()
                .filter(r -> r.getRiskFactors() != null)
                .flatMap(r -> r.getRiskFactors().stream())
                .collect(Collectors.groupingBy(f -> f, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

            return MerchantRiskRanking.builder()
                .rankingTime(LocalDateTime.now())
                .analysisWindow(predictionProperties.getAnalysisWindowHours() + " hours")
                .totalMerchantsRanked(rankings.size())
                .rankings(rankings)
                .avgRiskScore(riskStats.getAverage())
                .medianRiskScore(medianRisk)
                .maxRiskScore(riskStats.getMax())
                .minRiskScore(riskStats.getMin())
                .highRiskCount(highRiskCount)
                .mediumRiskCount(mediumRiskCount)
                .lowRiskCount(lowRiskCount)
                .topRiskFactors(topRiskFactors)
                .build();

        } catch (Exception e) {
            log.error("Failed to generate risk ranking", e);
            return MerchantRiskRanking.builder()
                .rankingTime(LocalDateTime.now())
                .totalMerchantsRanked(0)
                .rankings(Collections.emptyList())
                .build();
        }
    }

    /**
     * Get top N at-risk merchants
     */
    public List<MerchantRiskEntry> getTopRiskMerchants(int limit) {
        MerchantRiskRanking ranking = getMerchantRiskRanking();
        return ranking.getRankings().stream()
            .limit(limit)
            .collect(Collectors.toList());
    }

    // ==================== Helper Methods ====================

    private MerchantMetricsSummary buildMetricsSummary(String merchId, List<BatchMetrics> metrics, int windowHours) {
        long totalProcessed = metrics.stream().mapToLong(BatchMetrics::getProcessedCount).sum();
        long totalErrors = metrics.stream().mapToLong(BatchMetrics::getErrorCount).sum();
        double errorRate = totalProcessed > 0 ? (double) totalErrors / totalProcessed : 0;

        DoubleSummaryStatistics timeStats = metrics.stream()
            .mapToDouble(BatchMetrics::getProcessingTimeMs)
            .summaryStatistics();

        // Calculate P95
        List<Long> sortedTimes = metrics.stream()
            .map(BatchMetrics::getProcessingTimeMs)
            .sorted()
            .collect(Collectors.toList());
        int p95Index = (int) Math.ceil(sortedTimes.size() * 0.95) - 1;
        double p95 = sortedTimes.isEmpty() ? 0 : sortedTimes.get(Math.max(0, Math.min(p95Index, sortedTimes.size() - 1)));

        String hsn = metrics.isEmpty() ? null : metrics.get(0).getHsn();

        return MerchantMetricsSummary.builder()
            .merchId(merchId)
            .hsn(hsn)
            .totalBatches((long) metrics.size())
            .totalProcessed(totalProcessed)
            .totalErrors(totalErrors)
            .errorRate(errorRate)
            .avgProcessingTimeMs(timeStats.getAverage())
            .minProcessingTimeMs(timeStats.getMin())
            .maxProcessingTimeMs(timeStats.getMax())
            .p95ProcessingTimeMs(p95)
            .analysisWindow(windowHours + " hours")
            .dataPointCount((long) metrics.size())
            .build();
    }

    private String buildMerchantMetricsContext(String merchId, List<BatchMetrics> metrics,
            MerchantMetricsSummary summary, DoubleSummaryStatistics systemErrorStats,
            DoubleSummaryStatistics systemTimeStats, MerchantAlertThreshold thresholds) {

        StringBuilder context = new StringBuilder();
        context.append(String.format("Merchant ID: %s\n", merchId));
        context.append(String.format("Analysis Window: Last %s\n\n", summary.getAnalysisWindow()));

        context.append("=== Merchant Metrics Summary ===\n");
        context.append(String.format("Total Batches: %d\n", summary.getTotalBatches()));
        context.append(String.format("Total Processed: %d\n", summary.getTotalProcessed()));
        context.append(String.format("Total Errors: %d\n", summary.getTotalErrors()));
        context.append(String.format("Error Rate: %.4f (%.2f%%)\n", summary.getErrorRate(), summary.getErrorRate() * 100));
        context.append(String.format("Avg Processing Time: %.2fms\n", summary.getAvgProcessingTimeMs()));
        context.append(String.format("P95 Processing Time: %.2fms\n", summary.getP95ProcessingTimeMs()));

        context.append("\n=== Comparison to System Average ===\n");
        context.append(String.format("System Avg Error Rate: %.4f\n", systemErrorStats.getAverage()));
        context.append(String.format("System Avg Processing Time: %.2fms\n", systemTimeStats.getAverage()));

        double errorRateVsSystem = systemErrorStats.getAverage() > 0 ?
            ((summary.getErrorRate() - systemErrorStats.getAverage()) / systemErrorStats.getAverage()) * 100 : 0;
        double timeVsSystem = systemTimeStats.getAverage() > 0 ?
            ((summary.getAvgProcessingTimeMs() - systemTimeStats.getAverage()) / systemTimeStats.getAverage()) * 100 : 0;

        context.append(String.format("Error Rate vs System: %+.1f%%\n", errorRateVsSystem));
        context.append(String.format("Processing Time vs System: %+.1f%%\n", timeVsSystem));

        context.append("\n=== Custom Thresholds ===\n");
        context.append(String.format("Error Rate Warning: %.2f%%, Critical: %.2f%%\n",
            thresholds.getErrorRateWarningThreshold() * 100, thresholds.getErrorRateCriticalThreshold() * 100));
        context.append(String.format("Processing Time Warning: %dms, Critical: %dms\n",
            thresholds.getProcessingTimeWarningMs(), thresholds.getProcessingTimeCriticalMs()));

        // Time distribution
        context.append("\n=== Error Distribution by Hour ===\n");
        Map<Integer, Long> errorsByHour = metrics.stream()
            .filter(m -> m.getErrorCount() > 0)
            .collect(Collectors.groupingBy(m -> m.getTimestamp().getHour(), Collectors.counting()));

        errorsByHour.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(e -> context.append(String.format("  %02d:00: %d errors\n", e.getKey(), e.getValue())));

        return context.toString();
    }

    private MerchantPrediction parseMerchantPrediction(String aiResponse, String merchId) {
        try {
            String jsonStr = aiResponse;
            if (aiResponse.contains("```json")) {
                jsonStr = aiResponse.substring(
                    aiResponse.indexOf("```json") + 7,
                    aiResponse.lastIndexOf("```")
                ).trim();
            }

            JsonNode root = objectMapper.readTree(jsonStr);

            MerchantPrediction result = new MerchantPrediction();
            result.setMerchId(merchId);
            result.setPredictedStatus(HealthStatus.valueOf(root.get("predictedStatus").asText()));
            result.setConfidence(root.get("confidence").asDouble());
            result.setRiskScore(root.has("riskScore") ? root.get("riskScore").asDouble() : 0.0);

            List<String> keyFindings = new ArrayList<>();
            if (root.has("keyFindings")) {
                root.get("keyFindings").forEach(node -> keyFindings.add(node.asText()));
            }
            result.setKeyFindings(keyFindings);

            if (root.has("trendAnalysis")) {
                JsonNode trendNode = root.get("trendAnalysis");
                TrendAnalysis trendAnalysis = new TrendAnalysis();
                trendAnalysis.setErrorRateTrend(trendNode.get("errorRateTrend").asText());
                trendAnalysis.setProcessingTimeTrend(trendNode.get("processingTimeTrend").asText());
                trendAnalysis.setAnomalyDetected(trendNode.get("anomalyDetected").asBoolean());
                result.setTrendAnalysis(trendAnalysis);
            }

            List<String> riskFactors = new ArrayList<>();
            if (root.has("riskFactors")) {
                root.get("riskFactors").forEach(node -> riskFactors.add(node.asText()));
            }
            result.setRiskFactors(riskFactors);

            List<String> recommendations = new ArrayList<>();
            if (root.has("recommendations")) {
                root.get("recommendations").forEach(node -> recommendations.add(node.asText()));
            }
            result.setRecommendations(recommendations);

            result.setReasoning(root.has("reasoning") ? root.get("reasoning").asText() : null);

            return result;

        } catch (Exception e) {
            log.error("Failed to parse AI response for merchant: {}", merchId, e);
            return MerchantPrediction.error(merchId, "Failed to parse AI response: " + e.getMessage());
        }
    }

    private void adjustPredictionWithThresholds(MerchantPrediction prediction,
            MerchantMetricsSummary summary, MerchantAlertThreshold thresholds) {
        if (summary == null) return;

        // Check if current metrics exceed thresholds
        boolean isCritical = thresholds.isErrorRateCritical(summary.getErrorRate()) ||
                           thresholds.isProcessingTimeCritical(summary.getAvgProcessingTimeMs().longValue());
        boolean isWarning = thresholds.isErrorRateWarning(summary.getErrorRate()) ||
                          thresholds.isProcessingTimeWarning(summary.getAvgProcessingTimeMs().longValue());

        // Upgrade status if thresholds exceeded
        if (isCritical && prediction.getPredictedStatus() != HealthStatus.CRITICAL) {
            prediction.setPredictedStatus(HealthStatus.CRITICAL);
            prediction.getRiskFactors().add("Custom threshold exceeded: CRITICAL level");
        } else if (isWarning && prediction.getPredictedStatus() == HealthStatus.HEALTHY) {
            prediction.setPredictedStatus(HealthStatus.WARNING);
            prediction.getRiskFactors().add("Custom threshold exceeded: WARNING level");
        }

        // Adjust risk score based on thresholds
        if (thresholds.isRiskScoreCritical(prediction.getRiskScore())) {
            if (prediction.getPredictedStatus() != HealthStatus.CRITICAL) {
                prediction.setPredictedStatus(HealthStatus.CRITICAL);
            }
        } else if (thresholds.isRiskScoreWarning(prediction.getRiskScore())) {
            if (prediction.getPredictedStatus() == HealthStatus.HEALTHY) {
                prediction.setPredictedStatus(HealthStatus.WARNING);
            }
        }
    }

    private MerchantAlertThreshold getDefaultThresholds(String merchId) {
        return MerchantAlertThreshold.builder()
            .merchId(merchId)
            .build();
    }

    private DoubleSummaryStatistics calculateSystemErrorStats(List<BatchMetrics> metrics) {
        Map<String, List<BatchMetrics>> byMerchant = metrics.stream()
            .collect(Collectors.groupingBy(BatchMetrics::getMerchId));

        return byMerchant.values().stream()
            .mapToDouble(list -> {
                long total = list.stream().mapToLong(BatchMetrics::getProcessedCount).sum();
                long errors = list.stream().mapToLong(BatchMetrics::getErrorCount).sum();
                return total > 0 ? (double) errors / total : 0;
            })
            .summaryStatistics();
    }

    private DoubleSummaryStatistics calculateSystemTimeStats(List<BatchMetrics> metrics) {
        return metrics.stream()
            .mapToDouble(BatchMetrics::getProcessingTimeMs)
            .summaryStatistics();
    }

    private double calculateRiskScore(double errorRate, double processingTimeMs,
            double avgErrorRate, double avgProcessingTime) {
        // Weighted risk score calculation
        double errorWeight = 0.5;
        double timeWeight = 0.3;
        double volumeWeight = 0.2;

        // Error rate contribution (normalized to 0-1)
        double errorScore = Math.min(1.0, errorRate / 0.1); // 10% error = max score

        // Processing time contribution (normalized)
        double timeDeviation = avgProcessingTime > 0 ?
            (processingTimeMs - avgProcessingTime) / avgProcessingTime : 0;
        double timeScore = Math.max(0, Math.min(1.0, 0.5 + (timeDeviation * 0.5)));

        // Volume contribution (placeholder - could be enhanced)
        double volumeScore = 0.3;

        return (errorScore * errorWeight) + (timeScore * timeWeight) + (volumeScore * volumeWeight);
    }

    private HealthStatus determineHealthStatus(double riskScore) {
        if (riskScore >= 0.7) return HealthStatus.CRITICAL;
        if (riskScore >= 0.4) return HealthStatus.WARNING;
        return HealthStatus.HEALTHY;
    }

    private void calculateRankings(List<MerchantComparisonDetail> details,
            Map<String, Double> errorRates, Map<String, Double> processingTimes) {

        // Rank by error rate (highest first)
        List<String> byErrorRate = errorRates.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        // Rank by processing time (slowest first)
        List<String> byProcessingTime = processingTimes.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        for (MerchantComparisonDetail detail : details) {
            detail.setErrorRateRank(byErrorRate.indexOf(detail.getMerchId()) + 1);
            detail.setProcessingTimeRank(byProcessingTime.indexOf(detail.getMerchId()) + 1);
        }
    }

    private void calculateRiskContributions(MerchantRiskEntry entry, MerchantMetricsSummary summary) {
        double totalRisk = entry.getRiskScore();
        if (totalRisk <= 0) return;

        // Estimate contributions based on metrics
        double errorContrib = Math.min(1.0, summary.getErrorRate() / 0.1) * 0.5;
        double timeContrib = summary.getAvgProcessingTimeMs() > 5000 ? 0.3 :
            (summary.getAvgProcessingTimeMs() / 5000) * 0.3;
        double trendContrib = 0.1;
        double volumeContrib = 0.1;

        entry.setErrorRateContribution(errorContrib);
        entry.setProcessingTimeContribution(timeContrib);
        entry.setTrendContribution(trendContrib);
        entry.setVolumeContribution(volumeContrib);
    }

    private double calculateMedian(List<Double> values) {
        if (values.isEmpty()) return 0;
        List<Double> sorted = values.stream().sorted().collect(Collectors.toList());
        int middle = sorted.size() / 2;
        if (sorted.size() % 2 == 0) {
            return (sorted.get(middle - 1) + sorted.get(middle)) / 2.0;
        }
        return sorted.get(middle);
    }

    private List<String> generateComparisonInsights(List<MerchantComparisonDetail> details) {
        List<String> insights = new ArrayList<>();

        if (details.isEmpty()) {
            insights.add("No merchant data available for comparison");
            return insights;
        }

        // Top risk merchant
        MerchantComparisonDetail topRisk = details.get(0);
        insights.add(String.format("Highest risk merchant: %s with risk score %.2f",
            topRisk.getMerchId(), topRisk.getRiskScore()));

        // Count by status
        long criticalCount = details.stream()
            .filter(d -> d.getHealthStatus() == HealthStatus.CRITICAL).count();
        if (criticalCount > 0) {
            insights.add(String.format("%d merchant(s) in CRITICAL status require immediate attention", criticalCount));
        }

        // High error rate merchants
        long highErrorCount = details.stream()
            .filter(d -> d.getErrorRate() > 0.05).count();
        if (highErrorCount > 0) {
            insights.add(String.format("%d merchant(s) have error rate above 5%%", highErrorCount));
        }

        // Processing time outliers
        double avgTime = details.stream().mapToDouble(MerchantComparisonDetail::getAvgProcessingTimeMs).average().orElse(0);
        long slowMerchants = details.stream()
            .filter(d -> d.getAvgProcessingTimeMs() > avgTime * 2).count();
        if (slowMerchants > 0) {
            insights.add(String.format("%d merchant(s) have processing times 2x above average", slowMerchants));
        }

        return insights;
    }
}

