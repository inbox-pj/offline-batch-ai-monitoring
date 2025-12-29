package com.cardconnect.bolt.ai.service;

import com.cardconnect.bolt.ai.model.AIPredictionResult;
import com.cardconnect.bolt.ai.model.HealthStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Resilient AI Service Tests")
class ResilientAIServiceTest {

    @Mock
    private OfflineBatchAIAnalysisService aiAnalysisService;

    @Mock
    private OfflineBatchTrendAnalysisService ruleBasedService;

    private ResilientAIService resilientAIService;

    @BeforeEach
    void setUp() {
        // Use the constructor that takes the AI service (it will be wrapped in Optional)
        resilientAIService = new ResilientAIService(aiAnalysisService, ruleBasedService);
    }

    @Test
    @DisplayName("Should return AI prediction when service is available")
    void testSuccessfulAICall() {
        AIPredictionResult expected = AIPredictionResult.builder()
            .predictedStatus(HealthStatus.HEALTHY)
            .confidence(0.9)
            .build();

        when(aiAnalysisService.analyzeTrendsAndPredict()).thenReturn(expected);

        AIPredictionResult result = resilientAIService.analyze();

        assertThat(result).isEqualTo(expected);
        verify(aiAnalysisService).analyzeTrendsAndPredict();
        verify(ruleBasedService, never()).analyzeTrends();
    }

    @Test
    @DisplayName("Should fallback to rule-based when AI service is not present")
    void testFallbackWhenAIServiceNotPresent() {
        // Create a service without AI analysis service
        ResilientAIService serviceWithoutAI = new ResilientAIService(null, ruleBasedService);

        AIPredictionResult fallbackResult = AIPredictionResult.builder()
            .predictedStatus(HealthStatus.WARNING)
            .confidence(0.7)
            .build();

        when(ruleBasedService.analyzeTrends()).thenReturn(fallbackResult);

        AIPredictionResult result = serviceWithoutAI.analyze();

        assertThat(result).isEqualTo(fallbackResult);
        verify(ruleBasedService).analyzeTrends();
    }

    @Test
    @DisplayName("Should fallback to rule-based when AI fails - testing fallback method directly")
    void testFallbackToRuleBased() throws Exception {
        // Note: Resilience4j annotations (@CircuitBreaker, @Retry) require Spring AOP proxies to work.
        // In unit tests without Spring context, we test the fallback method directly using reflection.

        AIPredictionResult fallbackResult = AIPredictionResult.builder()
            .predictedStatus(HealthStatus.WARNING)
            .confidence(0.7)
            .build();

        when(ruleBasedService.analyzeTrends()).thenReturn(fallbackResult);

        // Test the fallback method directly using reflection since it's private
        Method fallbackMethod = ResilientAIService.class.getDeclaredMethod("fallbackAnalysis", Exception.class);
        fallbackMethod.setAccessible(true);

        AIPredictionResult result = (AIPredictionResult) fallbackMethod.invoke(
            resilientAIService,
            new RuntimeException("AI service unavailable")
        );

        assertThat(result).isEqualTo(fallbackResult);
        verify(ruleBasedService).analyzeTrends();
    }
}
