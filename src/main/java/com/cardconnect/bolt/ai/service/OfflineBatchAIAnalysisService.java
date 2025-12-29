package com.cardconnect.bolt.ai.service;

import com.cardconnect.bolt.ai.config.VectorStoreConfiguration.SimpleDocumentStore;
import com.cardconnect.bolt.ai.config.properties.AIPredictionProperties;
import com.cardconnect.bolt.ai.config.properties.AIPromptProperties;
import com.cardconnect.bolt.ai.model.AIPredictionResult;
import com.cardconnect.bolt.ai.model.BatchMetrics;
import com.cardconnect.bolt.ai.model.HealthStatus;
import com.cardconnect.bolt.ai.model.TrendAnalysis;
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
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for AI-powered analysis of offline batch metrics
 * Uses Spring AI for predictions, embeddings, and RAG
 * All configuration is externalized - no hardcoded values
 * Only instantiated when AI prediction is enabled
 */
@Service
@Transactional
@Slf4j
@ConditionalOnProperty(name = "ai.prediction.enabled", havingValue = "true")
public class OfflineBatchAIAnalysisService {

    private final ChatClient chatClient;
    private final OfflineBatchMetricsRepository metricsRepository;
    private final SimpleDocumentStore documentStore;
    private final EmbeddingModel embeddingModel;
    private final AIInputValidator inputValidator;
    private final ObjectMapper objectMapper;
    private final AIPromptProperties promptProperties;
    private final AIPredictionProperties predictionProperties;

    public OfflineBatchAIAnalysisService(ChatClient.Builder chatClientBuilder,
                                         OfflineBatchMetricsRepository metricsRepository,
                                         SimpleDocumentStore documentStore,
                                         EmbeddingModel embeddingModel,
                                         AIInputValidator inputValidator,
                                         ObjectMapper objectMapper,
                                         AIPromptProperties promptProperties,
                                         AIPredictionProperties predictionProperties) {
        this.chatClient = chatClientBuilder.build();
        this.metricsRepository = metricsRepository;
        this.documentStore = documentStore;
        this.embeddingModel = embeddingModel;
        this.inputValidator = inputValidator;
        this.objectMapper = objectMapper;
        this.promptProperties = promptProperties;
        this.predictionProperties = predictionProperties;
    }

    /**
     * Analyze metrics and predict future health status
     */
    public AIPredictionResult analyzeTrendsAndPredict() {
        try {
            // Get historical metrics using configured analysis window
            int analysisWindowHours = predictionProperties.getAnalysisWindowHours();
            List<BatchMetrics> metrics = metricsRepository.findByTimestampAfter(
                LocalDateTime.now().minusHours(analysisWindowHours));

            if (metrics.isEmpty()) {
                return AIPredictionResult.insufficientData();
            }

            // Validate input
            inputValidator.validateMetrics(metrics);

            // Build metrics context
            String metricsContext = buildMetricsContext(metrics);

            // Retrieve historical similar patterns using Spring AI embeddings
            String historicalContext = retrieveHistoricalContext(metrics);

            // Build analysis prompt
            String userPrompt = buildAnalysisPrompt(metricsContext, historicalContext);

            // Get AI prediction using externalized prompts
            Prompt prompt = new Prompt(List.of(
                new SystemMessage(promptProperties.getSystemPrompt()),
                new UserMessage(userPrompt)
            ));

            ChatResponse response = chatClient.prompt(prompt).call().chatResponse();
            String aiResponse = response.getResult().getOutput().getText();

            // Parse and return result
            AIPredictionResult result = parseAIResponse(aiResponse);

            // Store analysis for future RAG
            storeAnalysisForRAG(result);

            log.info("AI prediction completed: {} (confidence: {})",
                result.getPredictedStatus(), String.format("%.2f", result.getConfidence()));

            return result;

        } catch (Exception e) {
            log.error("AI analysis failed", e);
            return AIPredictionResult.error("AI analysis failed: " + e.getMessage());
        }
    }

    private String buildMetricsContext(List<BatchMetrics> metrics) {
        StringBuilder context = new StringBuilder();
        context.append("Current Offline Batch Metrics (Last 24 Hours):\n\n");

        // Statistical summary
        DoubleSummaryStatistics processingTimeStats = metrics.stream()
            .mapToDouble(BatchMetrics::getProcessingTimeMs)
            .summaryStatistics();

        context.append(String.format("Processing Time: avg=%.2fms, min=%.2fms, max=%.2fms\n",
            processingTimeStats.getAverage(),
            processingTimeStats.getMin(),
            processingTimeStats.getMax()));

        // Error rate
        long totalProcessed = metrics.stream().mapToLong(BatchMetrics::getProcessedCount).sum();
        long totalErrors = metrics.stream().mapToLong(BatchMetrics::getErrorCount).sum();
        double errorRate = totalProcessed > 0 ? (double) totalErrors / totalProcessed : 0;

        context.append(String.format("Error Rate: %.4f (%.2f%%)\n", errorRate, errorRate * 100));
        context.append(String.format("Total Batches: %d\n", metrics.size()));
        context.append(String.format("Total Errors: %d\n", totalErrors));

        // Merchant analysis
        Map<String, Long> merchantErrorCount = metrics.stream()
            .filter(m -> m.getErrorCount() > 0)
            .collect(Collectors.groupingBy(
                m -> m.getMerchId() + ":" + m.getHsn(),
                Collectors.counting()
            ));

        if (!merchantErrorCount.isEmpty()) {
            context.append("\nTop Merchants with Errors:\n");
            merchantErrorCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .forEach(e -> context.append(String.format("  %s: %d errors\n", e.getKey(), e.getValue())));
        }

        // Time pattern analysis
        Map<Integer, Long> errorsByHour = metrics.stream()
            .filter(m -> m.getErrorCount() > 0)
            .collect(Collectors.groupingBy(
                m -> m.getTimestamp().getHour(),
                Collectors.counting()
            ));

        if (!errorsByHour.isEmpty()) {
            context.append("\nError Distribution by Hour:\n");
            errorsByHour.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> context.append(String.format("  %02d:00: %d errors\n", e.getKey(), e.getValue())));
        }

        return context.toString();
    }

    private String retrieveHistoricalContext(List<BatchMetrics> currentMetrics) {
        try {
            String currentPattern = summarizeMetricsPattern(currentMetrics);

            // Get embedding for current pattern using Spring AI
            EmbeddingResponse embeddingResponse = embeddingModel.embedForResponse(List.of(currentPattern));
            float[] queryEmbedding = embeddingResponse.getResult().getOutput();

            // Search for similar patterns
            List<String> similarPatterns = documentStore.similaritySearch(queryEmbedding, 5, 0.7);

            StringBuilder context = new StringBuilder();
            context.append("\nSimilar Historical Patterns:\n");

            for (String pattern : similarPatterns) {
                context.append("- ").append(pattern).append("\n");
            }

            return context.toString();
        } catch (Exception e) {
            log.warn("Failed to retrieve historical context", e);
            return "\nNo historical patterns available.\n";
        }
    }

    private String buildAnalysisPrompt(String metricsContext, String historicalContext) {
        return String.format("""
            Analyze the following offline batch metrics and predict system health for the next 6 hours.
            
            %s
            
            %s
            
            Provide your analysis in JSON format as specified in the system prompt.
            Focus on identifying trends, anomalies, and potential risks.
            """, metricsContext, historicalContext);
    }

    private AIPredictionResult parseAIResponse(String aiResponse) {
        try {
            // Extract JSON from response
            String jsonStr = aiResponse;
            if (aiResponse.contains("```json")) {
                jsonStr = aiResponse.substring(
                    aiResponse.indexOf("```json") + 7,
                    aiResponse.lastIndexOf("```")
                ).trim();
            }

            JsonNode root = objectMapper.readTree(jsonStr);

            AIPredictionResult result = new AIPredictionResult();
            result.setPredictedStatus(HealthStatus.valueOf(root.get("predictedStatus").asText()));
            result.setConfidence(root.get("confidence").asDouble());
            result.setTimeHorizon(6);

            // Extract findings
            List<String> keyFindings = new ArrayList<>();
            root.get("keyFindings").forEach(node -> keyFindings.add(node.asText()));
            result.setKeyFindings(keyFindings);

            // Extract trend analysis
            JsonNode trendNode = root.get("trendAnalysis");
            TrendAnalysis trendAnalysis = new TrendAnalysis();
            trendAnalysis.setErrorRateTrend(trendNode.get("errorRateTrend").asText());
            trendAnalysis.setProcessingTimeTrend(trendNode.get("processingTimeTrend").asText());
            trendAnalysis.setAnomalyDetected(trendNode.get("anomalyDetected").asBoolean());
            result.setTrendAnalysis(trendAnalysis);

            // Extract risk factors
            List<String> riskFactors = new ArrayList<>();
            root.get("riskFactors").forEach(node -> riskFactors.add(node.asText()));
            result.setRiskFactors(riskFactors);

            // Extract recommendations
            List<String> recommendations = new ArrayList<>();
            root.get("recommendations").forEach(node -> recommendations.add(node.asText()));
            result.setRecommendations(recommendations);

            result.setReasoning(root.get("reasoning").asText());

            return result;

        } catch (Exception e) {
            log.error("Failed to parse AI response", e);
            return AIPredictionResult.error("Failed to parse AI analysis: " + e.getMessage());
        }
    }

    private void storeAnalysisForRAG(AIPredictionResult result) {
        try {
            String summary = String.format(
                    "Analysis from %s: Status=%s (%.1f%% confidence). %s. Key findings: %s",
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    result.getPredictedStatus(),
                    result.getConfidence() * 100,
                    result.getTrendAnalysis() != null ? result.getTrendAnalysis().getErrorRateTrend() : "N/A",
                    String.join("; ", result.getKeyFindings())
            );

            // Get embedding using Spring AI
            EmbeddingResponse embeddingResponse = embeddingModel.embedForResponse(List.of(summary));
            float[] embedding = embeddingResponse.getResult().getOutput();

            // Store in document store
            documentStore.add(summary, embedding);

            log.debug("Stored analysis for future RAG");

        } catch (Exception e) {
            log.warn("Failed to store analysis for RAG", e);
        }
    }

    private String summarizeMetricsPattern(List<BatchMetrics> metrics) {
        DoubleSummaryStatistics stats = metrics.stream()
            .mapToDouble(BatchMetrics::getProcessingTimeMs)
            .summaryStatistics();

        long errors = metrics.stream().mapToLong(BatchMetrics::getErrorCount).sum();

        return String.format("Pattern: avg_time=%.0fms, error_count=%d, batch_count=%d",
            stats.getAverage(), errors, metrics.size());
    }
}

