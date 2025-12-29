package com.cardconnect.bolt.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIPredictionResult {
    private HealthStatus predictedStatus;
    private Double confidence;
    private Integer timeHorizon;

    @Builder.Default
    private List<String> keyFindings = new ArrayList<>();

    private TrendAnalysis trendAnalysis;

    @Builder.Default
    private List<String> riskFactors = new ArrayList<>();

    @Builder.Default
    private List<String> recommendations = new ArrayList<>();

    private String reasoning;
    private String error;

    public static AIPredictionResult insufficientData() {
        return AIPredictionResult.builder()
            .predictedStatus(HealthStatus.UNKNOWN)
            .confidence(0.0)
            .error("Insufficient historical data for AI analysis")
            .build();
    }

    public static AIPredictionResult error(String message) {
        return AIPredictionResult.builder()
            .predictedStatus(HealthStatus.UNKNOWN)
            .confidence(0.0)
            .error(message)
            .build();
    }
}
