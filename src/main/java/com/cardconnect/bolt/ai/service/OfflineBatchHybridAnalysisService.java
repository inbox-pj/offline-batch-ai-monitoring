package com.cardconnect.bolt.ai.service;

import com.cardconnect.bolt.ai.config.properties.AIPredictionProperties;
import com.cardconnect.bolt.ai.model.AIPredictionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Hybrid analysis service that combines AI and rule-based approaches
 * Falls back to rule-based analysis when AI is unavailable or disabled
 * Configuration is injected via properties - no hardcoded values
 */
@Service
@Slf4j
public class OfflineBatchHybridAnalysisService {

    private final Optional<OfflineBatchAIAnalysisService> aiAnalysisService;
    private final OfflineBatchTrendAnalysisService ruleBasedService;
    private final AIUsageManager usageManager;
    private final AIPredictionProperties predictionProperties;

    @Autowired
    public OfflineBatchHybridAnalysisService(
            @Autowired(required = false) OfflineBatchAIAnalysisService aiAnalysisService,
            OfflineBatchTrendAnalysisService ruleBasedService,
            AIUsageManager usageManager,
            AIPredictionProperties predictionProperties) {
        this.aiAnalysisService = Optional.ofNullable(aiAnalysisService);
        this.ruleBasedService = ruleBasedService;
        this.usageManager = usageManager;
        this.predictionProperties = predictionProperties;
    }

    /**
     * Analyze trends using AI if available, otherwise fall back to rule-based analysis
     */
    public AIPredictionResult analyzeTrendsAndPredict() {
        // Check if AI is available and within budget
        if (!predictionProperties.getEnabled() || aiAnalysisService.isEmpty() || !usageManager.canMakeRequest()) {
            log.info("AI analysis unavailable (enabled={}, aiServicePresent={}, canMakeRequest={}), using rule-based analysis",
                    predictionProperties.getEnabled(), aiAnalysisService.isPresent(), usageManager.canMakeRequest());
            return ruleBasedService.analyzeTrends();
        }

        try {
            AIPredictionResult result = aiAnalysisService.get().analyzeTrendsAndPredict();
            usageManager.recordRequest(estimateTokenCount(result));
            return result;
        } catch (Exception e) {
            log.error("AI analysis failed, falling back to rule-based", e);

            // Check if fallback is enabled
            if (predictionProperties.getFallback().getEnabled()) {
                return ruleBasedService.analyzeTrends();
            } else {
                throw e; // Re-throw if fallback is disabled
            }
        }
    }

    private int estimateTokenCount(AIPredictionResult result) {
        // Rough estimation: ~4 characters per token
        int totalChars = result.toString().length();
        return totalChars / 4;
    }
}

