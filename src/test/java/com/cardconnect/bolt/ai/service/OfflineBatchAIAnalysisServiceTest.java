package com.cardconnect.bolt.ai.service;

import com.cardconnect.bolt.ai.model.AIPredictionResult;
import com.cardconnect.bolt.ai.model.BatchMetrics;
import com.cardconnect.bolt.ai.model.HealthStatus;
import com.cardconnect.bolt.ai.repository.OfflineBatchMetricsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Offline Batch AI Analysis Service Tests")
class OfflineBatchAIAnalysisServiceTest {

    @Mock
    private OfflineBatchMetricsRepository metricsRepository;

    private OfflineBatchTrendAnalysisService trendAnalysisService;

    @BeforeEach
    void setUp() {
        trendAnalysisService = new OfflineBatchTrendAnalysisService(metricsRepository);
    }

    @Test
    @DisplayName("Should return HEALTHY status for normal metrics")
    void testHealthyMetrics() {
        // Arrange
        List<BatchMetrics> healthyMetrics = createHealthyMetrics();
        when(metricsRepository.findByTimestampAfter(any())).thenReturn(healthyMetrics);
        when(metricsRepository.findByTimestampBetween(any(), any())).thenReturn(healthyMetrics);

        // Act
        AIPredictionResult result = trendAnalysisService.analyzeTrends();

        // Assert
        assertNotNull(result);
        assertEquals(HealthStatus.HEALTHY, result.getPredictedStatus());
        assertTrue(result.getConfidence() > 0);
        assertNotNull(result.getKeyFindings());
        assertFalse(result.getKeyFindings().isEmpty());
    }

    @Test
    @DisplayName("Should return WARNING status for elevated error rate")
    void testWarningMetrics() {
        // Arrange
        List<BatchMetrics> warningMetrics = createWarningMetrics();
        when(metricsRepository.findByTimestampAfter(any())).thenReturn(warningMetrics);
        when(metricsRepository.findByTimestampBetween(any(), any())).thenReturn(createHealthyMetrics());

        // Act
        AIPredictionResult result = trendAnalysisService.analyzeTrends();

        // Assert
        assertNotNull(result);
        assertEquals(HealthStatus.WARNING, result.getPredictedStatus());
        assertNotNull(result.getRecommendations());
        assertFalse(result.getRecommendations().isEmpty());
    }

    @Test
    @DisplayName("Should return CRITICAL status for high error rate")
    void testCriticalMetrics() {
        // Arrange
        List<BatchMetrics> criticalMetrics = createCriticalMetrics();
        when(metricsRepository.findByTimestampAfter(any())).thenReturn(criticalMetrics);
        when(metricsRepository.findByTimestampBetween(any(), any())).thenReturn(createHealthyMetrics());

        // Act
        AIPredictionResult result = trendAnalysisService.analyzeTrends();

        // Assert
        assertNotNull(result);
        assertEquals(HealthStatus.CRITICAL, result.getPredictedStatus());
        assertNotNull(result.getRiskFactors());
        assertFalse(result.getRiskFactors().isEmpty());
    }

    @Test
    @DisplayName("Should return insufficient data result for empty metrics")
    void testInsufficientData() {
        // Arrange
        when(metricsRepository.findByTimestampAfter(any())).thenReturn(new ArrayList<>());

        // Act
        AIPredictionResult result = trendAnalysisService.analyzeTrends();

        // Assert
        assertNotNull(result);
        assertEquals(HealthStatus.UNKNOWN, result.getPredictedStatus());
        assertNotNull(result.getError());
    }

    private List<BatchMetrics> createHealthyMetrics() {
        List<BatchMetrics> metrics = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < 24; i++) {
            BatchMetrics metric = BatchMetrics.builder()
                .timestamp(now.minusHours(i))
                .merchId("MERCH001")
                .hsn("HSN001")
                .processedCount(1000L)
                .errorCount(5L) // 0.5% error rate
                .processingTimeMs(2500L)
                .batchSize(1000)
                .build();
            metrics.add(metric);
        }

        return metrics;
    }

    private List<BatchMetrics> createWarningMetrics() {
        List<BatchMetrics> metrics = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < 24; i++) {
            BatchMetrics metric = BatchMetrics.builder()
                .timestamp(now.minusHours(i))
                .merchId("MERCH001")
                .hsn("HSN001")
                .processedCount(1000L)
                .errorCount(60L) // 6% error rate
                .processingTimeMs(3500L)
                .batchSize(1060)
                .build();
            metrics.add(metric);
        }

        return metrics;
    }

    private List<BatchMetrics> createCriticalMetrics() {
        List<BatchMetrics> metrics = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < 24; i++) {
            BatchMetrics metric = BatchMetrics.builder()
                .timestamp(now.minusHours(i))
                .merchId("MERCH001")
                .hsn("HSN001")
                .processedCount(1000L)
                .errorCount(150L) // 15% error rate
                .processingTimeMs(8000L)
                .batchSize(1150)
                .build();
            metrics.add(metric);
        }

        return metrics;
    }
}

