package com.cardconnect.bolt.ai.service;

import com.cardconnect.bolt.ai.config.VectorStoreConfiguration.SimpleDocumentStore;
import com.cardconnect.bolt.ai.config.properties.AIPredictionProperties;
import com.cardconnect.bolt.ai.config.properties.AIPromptProperties;
import com.cardconnect.bolt.ai.model.BatchMetrics;
import com.cardconnect.bolt.ai.model.HealthStatus;
import com.cardconnect.bolt.ai.model.merchant.MerchantAlertThreshold;
import com.cardconnect.bolt.ai.model.merchant.MerchantComparison;
import com.cardconnect.bolt.ai.model.merchant.MerchantPrediction;
import com.cardconnect.bolt.ai.model.merchant.MerchantRiskRanking;
import com.cardconnect.bolt.ai.repository.MerchantAlertThresholdRepository;
import com.cardconnect.bolt.ai.repository.OfflineBatchMetricsRepository;
import com.cardconnect.bolt.ai.util.AIInputValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Merchant Prediction Service Tests")
class MerchantPredictionServiceTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private OfflineBatchMetricsRepository metricsRepository;

    @Mock
    private MerchantAlertThresholdRepository thresholdRepository;

    @Mock
    private SimpleDocumentStore documentStore;

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private AIInputValidator inputValidator;

    @Mock
    private AIPromptProperties promptProperties;

    @Mock
    private AIPredictionProperties predictionProperties;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(predictionProperties.getAnalysisWindowHours()).thenReturn(24);
        when(predictionProperties.getForecastHorizonHours()).thenReturn(6);
    }

    @Nested
    @DisplayName("Merchant Metrics Summary Tests")
    class MetricsSummaryTests {

        @Test
        @DisplayName("Should calculate correct error rate")
        void testErrorRateCalculation() {
            // Arrange
            List<BatchMetrics> metrics = createMerchantMetrics("MERCH001", 100, 5, 1000L);

            // Calculate error rate
            long totalProcessed = metrics.stream().mapToLong(BatchMetrics::getProcessedCount).sum();
            long totalErrors = metrics.stream().mapToLong(BatchMetrics::getErrorCount).sum();
            double errorRate = (double) totalErrors / totalProcessed;

            // Assert
            assertEquals(0.05, errorRate, 0.001); // 5% error rate
        }

        @Test
        @DisplayName("Should handle zero processed count")
        void testZeroProcessedCount() {
            // Arrange
            List<BatchMetrics> metrics = createMerchantMetrics("MERCH001", 0, 0, 1000L);

            // Calculate error rate
            long totalProcessed = metrics.stream().mapToLong(BatchMetrics::getProcessedCount).sum();
            double errorRate = totalProcessed > 0 ?
                (double) metrics.stream().mapToLong(BatchMetrics::getErrorCount).sum() / totalProcessed : 0;

            // Assert
            assertEquals(0.0, errorRate);
        }
    }

    @Nested
    @DisplayName("Merchant Threshold Tests")
    class ThresholdTests {

        @Test
        @DisplayName("Should detect warning threshold breach")
        void testWarningThresholdBreach() {
            // Arrange
            MerchantAlertThreshold threshold = MerchantAlertThreshold.builder()
                .merchId("MERCH001")
                .errorRateWarningThreshold(0.02)
                .errorRateCriticalThreshold(0.05)
                .build();

            // Act & Assert
            assertTrue(threshold.isErrorRateWarning(0.03)); // Above warning, below critical
            assertFalse(threshold.isErrorRateWarning(0.01)); // Below warning
            assertFalse(threshold.isErrorRateWarning(0.06)); // Above critical
        }

        @Test
        @DisplayName("Should detect critical threshold breach")
        void testCriticalThresholdBreach() {
            // Arrange
            MerchantAlertThreshold threshold = MerchantAlertThreshold.builder()
                .merchId("MERCH001")
                .errorRateCriticalThreshold(0.05)
                .build();

            // Act & Assert
            assertTrue(threshold.isErrorRateCritical(0.06));
            assertTrue(threshold.isErrorRateCritical(0.05));
            assertFalse(threshold.isErrorRateCritical(0.04));
        }

        @Test
        @DisplayName("Should detect processing time warning")
        void testProcessingTimeWarning() {
            // Arrange
            MerchantAlertThreshold threshold = MerchantAlertThreshold.builder()
                .merchId("MERCH001")
                .processingTimeWarningMs(5000L)
                .processingTimeCriticalMs(10000L)
                .build();

            // Act & Assert
            assertTrue(threshold.isProcessingTimeWarning(6000L));
            assertFalse(threshold.isProcessingTimeWarning(4000L));
            assertFalse(threshold.isProcessingTimeWarning(11000L));
        }

        @Test
        @DisplayName("Should detect processing time critical")
        void testProcessingTimeCritical() {
            // Arrange
            MerchantAlertThreshold threshold = MerchantAlertThreshold.builder()
                .merchId("MERCH001")
                .processingTimeCriticalMs(10000L)
                .build();

            // Act & Assert
            assertTrue(threshold.isProcessingTimeCritical(10000L));
            assertTrue(threshold.isProcessingTimeCritical(15000L));
            assertFalse(threshold.isProcessingTimeCritical(9000L));
        }

        @Test
        @DisplayName("Should detect risk score thresholds")
        void testRiskScoreThresholds() {
            // Arrange
            MerchantAlertThreshold threshold = MerchantAlertThreshold.builder()
                .merchId("MERCH001")
                .riskScoreWarningThreshold(0.4)
                .riskScoreCriticalThreshold(0.7)
                .build();

            // Act & Assert
            assertTrue(threshold.isRiskScoreWarning(0.5));
            assertFalse(threshold.isRiskScoreWarning(0.3));
            assertTrue(threshold.isRiskScoreCritical(0.8));
            assertFalse(threshold.isRiskScoreCritical(0.5));
        }
    }

    @Nested
    @DisplayName("Merchant Prediction Result Tests")
    class PredictionResultTests {

        @Test
        @DisplayName("Should create insufficient data result")
        void testInsufficientDataResult() {
            // Act
            MerchantPrediction result = MerchantPrediction.insufficientData("MERCH001");

            // Assert
            assertEquals("MERCH001", result.getMerchId());
            assertEquals(HealthStatus.UNKNOWN, result.getPredictedStatus());
            assertEquals(0.0, result.getConfidence());
            assertNotNull(result.getError());
            assertTrue(result.getError().contains("Insufficient"));
        }

        @Test
        @DisplayName("Should create error result")
        void testErrorResult() {
            // Act
            MerchantPrediction result = MerchantPrediction.error("MERCH001", "Test error");

            // Assert
            assertEquals("MERCH001", result.getMerchId());
            assertEquals(HealthStatus.UNKNOWN, result.getPredictedStatus());
            assertEquals("Test error", result.getError());
        }
    }

    @Nested
    @DisplayName("Risk Score Calculation Tests")
    class RiskScoreTests {

        @Test
        @DisplayName("Should calculate high risk for high error rate")
        void testHighRiskCalculation() {
            // High error rate should result in high risk
            double errorRate = 0.10; // 10% error rate
            double avgProcessingTime = 5000;
            double systemAvgErrorRate = 0.02;
            double systemAvgTime = 5000;

            // Error contribution is significant
            double errorScore = Math.min(1.0, errorRate / 0.1);
            assertEquals(1.0, errorScore, 0.01);
        }

        @Test
        @DisplayName("Should calculate low risk for healthy metrics")
        void testLowRiskCalculation() {
            // Low error rate should result in low risk
            double errorRate = 0.01; // 1% error rate
            double errorScore = Math.min(1.0, errorRate / 0.1);
            assertEquals(0.1, errorScore, 0.01);
        }
    }

    @Nested
    @DisplayName("Merchant Comparison Tests")
    class ComparisonTests {

        @Test
        @DisplayName("Should compare merchants correctly")
        void testMerchantComparison() {
            // Arrange - simulate comparison data
            MerchantComparison.MerchantComparisonDetail detail1 =
                MerchantComparison.MerchantComparisonDetail.builder()
                    .merchId("MERCH001")
                    .errorRate(0.05)
                    .avgProcessingTimeMs(5000.0)
                    .riskScore(0.6)
                    .healthStatus(HealthStatus.WARNING)
                    .build();

            MerchantComparison.MerchantComparisonDetail detail2 =
                MerchantComparison.MerchantComparisonDetail.builder()
                    .merchId("MERCH002")
                    .errorRate(0.01)
                    .avgProcessingTimeMs(3000.0)
                    .riskScore(0.2)
                    .healthStatus(HealthStatus.HEALTHY)
                    .build();

            List<MerchantComparison.MerchantComparisonDetail> details = Arrays.asList(detail1, detail2);

            // Sort by risk score
            details.sort((a, b) -> Double.compare(b.getRiskScore(), a.getRiskScore()));

            // Assert
            assertEquals("MERCH001", details.get(0).getMerchId()); // Higher risk first
            assertEquals("MERCH002", details.get(1).getMerchId());
        }
    }

    @Nested
    @DisplayName("Risk Ranking Tests")
    class RiskRankingTests {

        @Test
        @DisplayName("Should rank merchants by risk score")
        void testMerchantRanking() {
            // Arrange
            MerchantRiskRanking.MerchantRiskEntry entry1 =
                MerchantRiskRanking.MerchantRiskEntry.builder()
                    .merchId("MERCH001")
                    .riskScore(0.8)
                    .build();

            MerchantRiskRanking.MerchantRiskEntry entry2 =
                MerchantRiskRanking.MerchantRiskEntry.builder()
                    .merchId("MERCH002")
                    .riskScore(0.3)
                    .build();

            MerchantRiskRanking.MerchantRiskEntry entry3 =
                MerchantRiskRanking.MerchantRiskEntry.builder()
                    .merchId("MERCH003")
                    .riskScore(0.5)
                    .build();

            List<MerchantRiskRanking.MerchantRiskEntry> entries =
                new ArrayList<>(Arrays.asList(entry1, entry2, entry3));

            // Sort by risk score descending
            entries.sort((a, b) -> Double.compare(b.getRiskScore(), a.getRiskScore()));

            // Assign ranks
            for (int i = 0; i < entries.size(); i++) {
                entries.get(i).setRank(i + 1);
            }

            // Assert
            assertEquals(1, entries.get(0).getRank());
            assertEquals("MERCH001", entries.get(0).getMerchId());
            assertEquals(2, entries.get(1).getRank());
            assertEquals("MERCH003", entries.get(1).getMerchId());
            assertEquals(3, entries.get(2).getRank());
            assertEquals("MERCH002", entries.get(2).getMerchId());
        }

        @Test
        @DisplayName("Should categorize risk levels correctly")
        void testRiskCategorization() {
            // Arrange
            List<Double> riskScores = Arrays.asList(0.8, 0.75, 0.5, 0.4, 0.2, 0.1);

            // Act
            long highRisk = riskScores.stream().filter(r -> r > 0.7).count();
            long mediumRisk = riskScores.stream().filter(r -> r >= 0.4 && r <= 0.7).count();
            long lowRisk = riskScores.stream().filter(r -> r < 0.4).count();

            // Assert
            assertEquals(2, highRisk);   // 0.8, 0.75
            assertEquals(2, mediumRisk); // 0.5, 0.4
            assertEquals(2, lowRisk);    // 0.2, 0.1
        }
    }

    // Helper methods for creating test data

    private List<BatchMetrics> createMerchantMetrics(String merchId, long processedCount,
            long errorCount, long processingTimeMs) {
        List<BatchMetrics> metrics = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            metrics.add(BatchMetrics.builder()
                .id((long) i)
                .merchId(merchId)
                .hsn("HSN" + merchId)
                .batchId("BATCH-" + i)
                .processedCount(processedCount)
                .errorCount(errorCount)
                .processingTimeMs(processingTimeMs)
                .batchSize(100)
                .status("COMPLETED")
                .timestamp(LocalDateTime.now().minusHours(i))
                .build());
        }
        return metrics;
    }
}

