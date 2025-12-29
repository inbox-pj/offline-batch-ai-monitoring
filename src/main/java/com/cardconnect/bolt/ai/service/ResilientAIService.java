package com.cardconnect.bolt.ai.service;

import com.cardconnect.bolt.ai.model.AIPredictionResult;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Resilient AI Service with Circuit Breaker, Retry, and Timeout
 * Provides fault tolerance for AI API calls
 */
@Service
@Slf4j
public class ResilientAIService {

    private final Optional<OfflineBatchAIAnalysisService> aiAnalysisService;
    private final OfflineBatchTrendAnalysisService ruleBasedService;

    @Autowired
    public ResilientAIService(
            @Autowired(required = false) OfflineBatchAIAnalysisService aiAnalysisService,
            OfflineBatchTrendAnalysisService ruleBasedService) {
        this.aiAnalysisService = Optional.ofNullable(aiAnalysisService);
        this.ruleBasedService = ruleBasedService;
    }

    /**
     * Analyze with circuit breaker, retry, and timeout protection
     */
    @CircuitBreaker(name = "ai-service", fallbackMethod = "fallbackAnalysis")
    @Retry(name = "ai-service", fallbackMethod = "fallbackAnalysis")
    @TimeLimiter(name = "ai-service", fallbackMethod = "fallbackAnalysisAsync")
    public CompletableFuture<AIPredictionResult> analyzeAsync() {
        log.debug("Attempting AI analysis with resilience patterns");
        if (aiAnalysisService.isEmpty()) {
            return CompletableFuture.completedFuture(ruleBasedService.analyzeTrends());
        }
        return CompletableFuture.supplyAsync(() -> aiAnalysisService.get().analyzeTrendsAndPredict());
    }

    /**
     * Synchronous version with circuit breaker
     */
    @CircuitBreaker(name = "ai-service", fallbackMethod = "fallbackAnalysis")
    @Retry(name = "ai-service", fallbackMethod = "fallbackAnalysis")
    public AIPredictionResult analyze() {
        log.debug("Attempting AI analysis (sync) with resilience patterns");
        if (aiAnalysisService.isEmpty()) {
            log.info("AI service not available, using rule-based analysis");
            return ruleBasedService.analyzeTrends();
        }
        return aiAnalysisService.get().analyzeTrendsAndPredict();
    }

    /**
     * Fallback method for synchronous calls
     */
    private AIPredictionResult fallbackAnalysis(Exception e) {
        log.warn("AI service failed, using rule-based fallback. Error: {}", e.getMessage());
        return ruleBasedService.analyzeTrends();
    }

    /**
     * Fallback method for async calls
     */
    private CompletableFuture<AIPredictionResult> fallbackAnalysisAsync(Exception e) {
        log.warn("AI service failed (async), using rule-based fallback. Error: {}", e.getMessage());
        return CompletableFuture.completedFuture(ruleBasedService.analyzeTrends());
    }
}

