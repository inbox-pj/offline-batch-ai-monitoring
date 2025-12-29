package com.cardconnect.bolt.ai.model.merchant;

import com.cardconnect.bolt.ai.model.HealthStatus;
import com.cardconnect.bolt.ai.model.TrendAnalysis;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * AI prediction result for a specific merchant
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantPrediction {

    private String merchId;
    private String hsn;
    private HealthStatus predictedStatus;
    private Double confidence;
    private Integer timeHorizon;
    private Double riskScore;

    @Builder.Default
    private List<String> keyFindings = new ArrayList<>();

    private TrendAnalysis trendAnalysis;

    @Builder.Default
    private List<String> riskFactors = new ArrayList<>();

    @Builder.Default
    private List<String> recommendations = new ArrayList<>();

    private String reasoning;
    private String error;
    private LocalDateTime predictionTime;

    // Merchant-specific metrics summary
    private MerchantMetricsSummary metricsSummary;

    public static MerchantPrediction insufficientData(String merchId) {
        return MerchantPrediction.builder()
            .merchId(merchId)
            .predictedStatus(HealthStatus.UNKNOWN)
            .confidence(0.0)
            .riskScore(0.0)
            .error("Insufficient historical data for merchant: " + merchId)
            .predictionTime(LocalDateTime.now())
            .build();
    }

    public static MerchantPrediction error(String merchId, String message) {
        return MerchantPrediction.builder()
            .merchId(merchId)
            .predictedStatus(HealthStatus.UNKNOWN)
            .confidence(0.0)
            .riskScore(0.0)
            .error(message)
            .predictionTime(LocalDateTime.now())
            .build();
    }
}

