package com.cardconnect.bolt.ai.service;

import com.cardconnect.bolt.ai.model.AIPredictionAudit;
import com.cardconnect.bolt.ai.model.HealthStatus;
import com.cardconnect.bolt.ai.model.accuracy.ABTestResult;
import com.cardconnect.bolt.ai.model.accuracy.AccuracyMetrics;
import com.cardconnect.bolt.ai.model.accuracy.FeedbackLoopData;
import com.cardconnect.bolt.ai.repository.AIPredictionAuditRepository;
import com.cardconnect.bolt.ai.repository.OfflineBatchMetricsRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Prediction Accuracy Service Tests")
class PredictionAccuracyServiceTest {

    @Mock
    private AIPredictionAuditRepository auditRepository;

    @Mock
    private OfflineBatchMetricsRepository metricsRepository;

    private MeterRegistry meterRegistry;
    private PredictionAccuracyService accuracyService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        accuracyService = new PredictionAccuracyService(
            auditRepository, metricsRepository, meterRegistry);
    }

    @Nested
    @DisplayName("Outcome Recording Tests")
    class OutcomeRecordingTests {

        @Test
        @DisplayName("Should record outcome correctly")
        void testRecordOutcome() {
            // Arrange
            AIPredictionAudit audit = createAudit(1L, HealthStatus.WARNING, 0.85);
            when(auditRepository.findById(1L)).thenReturn(Optional.of(audit));
            when(auditRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // Act
            AIPredictionAudit result = accuracyService.recordOutcome(1L, HealthStatus.WARNING, "Test");

            // Assert
            assertEquals(HealthStatus.WARNING, result.getActualOutcome());
            assertTrue(result.getIsCorrect());
            assertNotNull(result.getOutcomeTimestamp());
            assertEquals("Test", result.getEvaluationNotes());
            verify(auditRepository).save(any());
        }

        @Test
        @DisplayName("Should mark incorrect prediction")
        void testRecordIncorrectOutcome() {
            // Arrange
            AIPredictionAudit audit = createAudit(1L, HealthStatus.HEALTHY, 0.90);
            when(auditRepository.findById(1L)).thenReturn(Optional.of(audit));
            when(auditRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // Act
            AIPredictionAudit result = accuracyService.recordOutcome(1L, HealthStatus.CRITICAL, "Missed critical");

            // Assert
            assertEquals(HealthStatus.CRITICAL, result.getActualOutcome());
            assertFalse(result.getIsCorrect());
        }

        @Test
        @DisplayName("Should throw exception for missing prediction")
        void testRecordOutcomeMissingPrediction() {
            // Arrange
            when(auditRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(IllegalArgumentException.class, () ->
                accuracyService.recordOutcome(999L, HealthStatus.HEALTHY, null));
        }
    }

    @Nested
    @DisplayName("Accuracy Metrics Tests")
    class AccuracyMetricsTests {

        @Test
        @DisplayName("Should calculate overall accuracy correctly")
        void testOverallAccuracy() {
            // Arrange
            List<AIPredictionAudit> predictions = Arrays.asList(
                createEvaluatedAudit(1L, HealthStatus.HEALTHY, HealthStatus.HEALTHY, true),
                createEvaluatedAudit(2L, HealthStatus.HEALTHY, HealthStatus.HEALTHY, true),
                createEvaluatedAudit(3L, HealthStatus.WARNING, HealthStatus.WARNING, true),
                createEvaluatedAudit(4L, HealthStatus.WARNING, HealthStatus.HEALTHY, false),
                createEvaluatedAudit(5L, HealthStatus.CRITICAL, HealthStatus.CRITICAL, true)
            );

            when(auditRepository.findByPredictionTimeAfterAndActualOutcomeNotNull(any()))
                .thenReturn(predictions);
            when(auditRepository.countByPredictionTimeAfter(any())).thenReturn(5L);
            when(auditRepository.findEvaluatedBetween(any(), any())).thenReturn(predictions);
            when(auditRepository.findForCalibrationAnalysis(any())).thenReturn(predictions);

            // Act
            AccuracyMetrics metrics = accuracyService.calculateAccuracyMetrics(7);

            // Assert
            assertEquals(5L, metrics.getEvaluatedPredictions());
            assertEquals(4L, metrics.getCorrectPredictions()); // 4 out of 5 correct
            assertEquals(0.8, metrics.getOverallAccuracy(), 0.01);
        }

        @Test
        @DisplayName("Should handle empty predictions")
        void testEmptyPredictions() {
            // Arrange
            when(auditRepository.findByPredictionTimeAfterAndActualOutcomeNotNull(any()))
                .thenReturn(new ArrayList<>());

            // Act
            AccuracyMetrics metrics = accuracyService.calculateAccuracyMetrics(7);

            // Assert
            assertEquals(0L, metrics.getTotalPredictions());
            assertEquals(0L, metrics.getEvaluatedPredictions());
            assertNotNull(metrics.getInsights());
        }

        @Test
        @DisplayName("Should calculate precision and recall")
        void testPrecisionRecall() {
            // Arrange - 2 true positives, 1 false positive, 1 false negative for WARNING
            List<AIPredictionAudit> predictions = Arrays.asList(
                createEvaluatedAudit(1L, HealthStatus.WARNING, HealthStatus.WARNING, true),  // TP
                createEvaluatedAudit(2L, HealthStatus.WARNING, HealthStatus.WARNING, true),  // TP
                createEvaluatedAudit(3L, HealthStatus.WARNING, HealthStatus.HEALTHY, false), // FP
                createEvaluatedAudit(4L, HealthStatus.HEALTHY, HealthStatus.WARNING, false)  // FN
            );

            when(auditRepository.findByPredictionTimeAfterAndActualOutcomeNotNull(any()))
                .thenReturn(predictions);
            when(auditRepository.countByPredictionTimeAfter(any())).thenReturn(4L);
            when(auditRepository.findEvaluatedBetween(any(), any())).thenReturn(predictions);
            when(auditRepository.findForCalibrationAnalysis(any())).thenReturn(predictions);

            // Act
            AccuracyMetrics metrics = accuracyService.calculateAccuracyMetrics(7);

            // Assert
            AccuracyMetrics.ClassificationMetrics warningMetrics =
                metrics.getMetricsByStatus().get(HealthStatus.WARNING);

            // Precision = TP / (TP + FP) = 2 / 3 = 0.667
            assertEquals(0.667, warningMetrics.getPrecision(), 0.01);

            // Recall = TP / (TP + FN) = 2 / 3 = 0.667
            assertEquals(0.667, warningMetrics.getRecall(), 0.01);
        }

        @Test
        @DisplayName("Should calculate F1 score")
        void testF1Score() {
            // F1 = 2 * (precision * recall) / (precision + recall)
            double precision = 0.667;
            double recall = 0.667;
            double expectedF1 = 2 * (precision * recall) / (precision + recall);

            assertEquals(0.667, expectedF1, 0.01);
        }
    }

    @Nested
    @DisplayName("A/B Testing Tests")
    class ABTestingTests {

        @Test
        @DisplayName("Should compare AI and rule-based models")
        void testModelComparison() {
            // Arrange
            List<AIPredictionAudit> aiPredictions = Arrays.asList(
                createEvaluatedAudit(1L, HealthStatus.HEALTHY, HealthStatus.HEALTHY, true, "AI"),
                createEvaluatedAudit(2L, HealthStatus.WARNING, HealthStatus.WARNING, true, "AI"),
                createEvaluatedAudit(3L, HealthStatus.CRITICAL, HealthStatus.CRITICAL, true, "AI")
            );

            List<AIPredictionAudit> rulePredictions = Arrays.asList(
                createEvaluatedAudit(4L, HealthStatus.HEALTHY, HealthStatus.HEALTHY, true, "RULE_BASED"),
                createEvaluatedAudit(5L, HealthStatus.WARNING, HealthStatus.CRITICAL, false, "RULE_BASED")
            );

            when(auditRepository.findEvaluatedByModelType(eq("AI"), any()))
                .thenReturn(aiPredictions);
            when(auditRepository.findEvaluatedByModelType(eq("RULE_BASED"), any()))
                .thenReturn(rulePredictions);
            when(auditRepository.findByModelTypeAndPredictionTimeAfter(anyString(), any()))
                .thenReturn(aiPredictions);
            when(auditRepository.countErrorsByModelType(anyString(), any())).thenReturn(0L);
            when(auditRepository.averageConfidenceByModelType(anyString(), any())).thenReturn(0.8);
            when(auditRepository.averageResponseTimeByModelType(anyString(), any())).thenReturn(100.0);

            // Act
            ABTestResult result = accuracyService.compareModels(30);

            // Assert
            assertNotNull(result);
            assertNotNull(result.getAiModelPerformance());
            assertNotNull(result.getRuleBasedPerformance());
            assertEquals(1.0, result.getAiModelPerformance().getAccuracy(), 0.01); // 3/3
        }

        @Test
        @DisplayName("Should determine correct winner")
        void testWinnerDetermination() {
            // AI: 90% accuracy, Rule: 70% accuracy
            // Winner should be AI

            List<AIPredictionAudit> aiPredictions = new ArrayList<>();
            for (int i = 0; i < 9; i++) {
                aiPredictions.add(createEvaluatedAudit((long) i,
                    HealthStatus.HEALTHY, HealthStatus.HEALTHY, true, "AI"));
            }
            aiPredictions.add(createEvaluatedAudit(9L,
                HealthStatus.HEALTHY, HealthStatus.WARNING, false, "AI"));

            List<AIPredictionAudit> rulePredictions = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                rulePredictions.add(createEvaluatedAudit((long) (i + 10),
                    HealthStatus.HEALTHY, HealthStatus.HEALTHY, true, "RULE_BASED"));
            }
            for (int i = 0; i < 3; i++) {
                rulePredictions.add(createEvaluatedAudit((long) (i + 17),
                    HealthStatus.HEALTHY, HealthStatus.WARNING, false, "RULE_BASED"));
            }

            when(auditRepository.findEvaluatedByModelType(eq("AI"), any()))
                .thenReturn(aiPredictions);
            when(auditRepository.findEvaluatedByModelType(eq("RULE_BASED"), any()))
                .thenReturn(rulePredictions);
            when(auditRepository.findByModelTypeAndPredictionTimeAfter(anyString(), any()))
                .thenReturn(aiPredictions);
            when(auditRepository.countErrorsByModelType(anyString(), any())).thenReturn(0L);
            when(auditRepository.averageConfidenceByModelType(anyString(), any())).thenReturn(0.8);
            when(auditRepository.averageResponseTimeByModelType(anyString(), any())).thenReturn(100.0);

            // Act
            ABTestResult result = accuracyService.compareModels(30);

            // Assert
            assertEquals("AI", result.getWinner());
            assertTrue(result.getAccuracyDifference() > 0); // AI better
        }
    }

    @Nested
    @DisplayName("Feedback Loop Tests")
    class FeedbackLoopTests {

        @Test
        @DisplayName("Should identify high-confidence errors")
        void testHighConfidenceErrors() {
            // Arrange
            List<AIPredictionAudit> highConfErrors = Arrays.asList(
                createHighConfidenceError(1L, HealthStatus.HEALTHY, HealthStatus.CRITICAL, 0.95)
            );

            when(auditRepository.findHighConfidenceErrors(anyDouble(), any()))
                .thenReturn(highConfErrors);
            when(auditRepository.countMisclassifications(any(), any(), any())).thenReturn(0L);
            when(auditRepository.findByPredictionTimeAfterAndActualOutcomeNotNull(any()))
                .thenReturn(new ArrayList<>());
            when(auditRepository.findEvaluatedBetween(any(), any())).thenReturn(new ArrayList<>());

            // Act
            FeedbackLoopData data = accuracyService.generateFeedbackLoopData(30);

            // Assert
            assertNotNull(data);
            assertNotNull(data.getHighConfidenceErrors());
            assertFalse(data.getHighConfidenceErrors().isEmpty());
            assertEquals(HealthStatus.HEALTHY, data.getHighConfidenceErrors().get(0).getPredicted());
            assertEquals(HealthStatus.CRITICAL, data.getHighConfidenceErrors().get(0).getActual());
        }

        @Test
        @DisplayName("Should detect model drift")
        void testDriftDetection() {
            // Arrange - Recent predictions are less accurate than baseline
            List<AIPredictionAudit> recentPredictions = Arrays.asList(
                createEvaluatedAudit(1L, HealthStatus.HEALTHY, HealthStatus.HEALTHY, true),
                createEvaluatedAudit(2L, HealthStatus.HEALTHY, HealthStatus.WARNING, false),
                createEvaluatedAudit(3L, HealthStatus.WARNING, HealthStatus.HEALTHY, false)
            ); // 33% accuracy

            List<AIPredictionAudit> baselinePredictions = Arrays.asList(
                createEvaluatedAudit(4L, HealthStatus.HEALTHY, HealthStatus.HEALTHY, true),
                createEvaluatedAudit(5L, HealthStatus.WARNING, HealthStatus.WARNING, true),
                createEvaluatedAudit(6L, HealthStatus.CRITICAL, HealthStatus.CRITICAL, true)
            ); // 100% accuracy

            // Mock repository to return different predictions for different time ranges
            when(auditRepository.findHighConfidenceErrors(anyDouble(), any()))
                .thenReturn(new ArrayList<>());
            when(auditRepository.countMisclassifications(any(), any(), any())).thenReturn(0L);
            when(auditRepository.findByPredictionTimeAfterAndActualOutcomeNotNull(any()))
                .thenReturn(new ArrayList<>());
            when(auditRepository.findEvaluatedBetween(any(), any()))
                .thenReturn(recentPredictions)
                .thenReturn(baselinePredictions);

            // Act
            FeedbackLoopData data = accuracyService.generateFeedbackLoopData(30);

            // Assert
            assertNotNull(data);
            assertNotNull(data.getDriftAnalysis());
            // Drift should be detected due to significant accuracy difference
        }
    }

    @Nested
    @DisplayName("Confusion Matrix Tests")
    class ConfusionMatrixTests {

        @Test
        @DisplayName("Should build correct confusion matrix")
        void testConfusionMatrix() {
            // Arrange
            List<AIPredictionAudit> predictions = Arrays.asList(
                createEvaluatedAudit(1L, HealthStatus.HEALTHY, HealthStatus.HEALTHY, true),
                createEvaluatedAudit(2L, HealthStatus.HEALTHY, HealthStatus.WARNING, false),
                createEvaluatedAudit(3L, HealthStatus.WARNING, HealthStatus.WARNING, true),
                createEvaluatedAudit(4L, HealthStatus.CRITICAL, HealthStatus.CRITICAL, true)
            );

            when(auditRepository.findByPredictionTimeAfterAndActualOutcomeNotNull(any()))
                .thenReturn(predictions);
            when(auditRepository.countByPredictionTimeAfter(any())).thenReturn(4L);
            when(auditRepository.findEvaluatedBetween(any(), any())).thenReturn(predictions);
            when(auditRepository.findForCalibrationAnalysis(any())).thenReturn(predictions);

            // Act
            AccuracyMetrics metrics = accuracyService.calculateAccuracyMetrics(7);

            // Assert
            assertNotNull(metrics.getConfusionMatrix());
            assertNotNull(metrics.getConfusionMatrix().getMatrix());
            assertEquals(4, metrics.getConfusionMatrix().getLabels().size());
        }
    }

    // ==================== Helper Methods ====================

    private AIPredictionAudit createAudit(Long id, HealthStatus predicted, double confidence) {
        return AIPredictionAudit.builder()
            .id(id)
            .predictionTime(LocalDateTime.now().minusHours(12))
            .predictedStatus(predicted)
            .confidence(confidence)
            .timeHorizon(6)
            .modelType("AI")
            .build();
    }

    private AIPredictionAudit createEvaluatedAudit(Long id, HealthStatus predicted,
            HealthStatus actual, boolean correct) {
        return createEvaluatedAudit(id, predicted, actual, correct, "AI");
    }

    private AIPredictionAudit createEvaluatedAudit(Long id, HealthStatus predicted,
            HealthStatus actual, boolean correct, String modelType) {
        return AIPredictionAudit.builder()
            .id(id)
            .predictionTime(LocalDateTime.now().minusHours(12))
            .predictedStatus(predicted)
            .actualOutcome(actual)
            .isCorrect(correct)
            .confidence(0.85)
            .timeHorizon(6)
            .modelType(modelType)
            .outcomeTimestamp(LocalDateTime.now().minusHours(6))
            .build();
    }

    private AIPredictionAudit createHighConfidenceError(Long id, HealthStatus predicted,
            HealthStatus actual, double confidence) {
        return AIPredictionAudit.builder()
            .id(id)
            .predictionTime(LocalDateTime.now().minusHours(12))
            .predictedStatus(predicted)
            .actualOutcome(actual)
            .isCorrect(false)
            .confidence(confidence)
            .timeHorizon(6)
            .modelType("AI")
            .reasoning("High confidence but wrong")
            .build();
    }
}

