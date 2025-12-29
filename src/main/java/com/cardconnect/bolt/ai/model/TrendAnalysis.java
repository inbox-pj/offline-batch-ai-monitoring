package com.cardconnect.bolt.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendAnalysis {
    private String errorRateTrend;
    private String processingTimeTrend;
    private boolean anomalyDetected;
    private String anomalyType;
    private Double anomalyScore;
}
