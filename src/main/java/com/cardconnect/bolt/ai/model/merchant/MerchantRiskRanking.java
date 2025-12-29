package com.cardconnect.bolt.ai.model.merchant;

import com.cardconnect.bolt.ai.model.HealthStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Risk ranking of merchants sorted by risk score
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantRiskRanking {

    private LocalDateTime rankingTime;
    private String analysisWindow;
    private Integer totalMerchantsRanked;

    // Ranking list (sorted by risk score, highest first)
    private List<MerchantRiskEntry> rankings;

    // Summary statistics
    private Double avgRiskScore;
    private Double medianRiskScore;
    private Double maxRiskScore;
    private Double minRiskScore;

    // Risk distribution
    private Integer highRiskCount;    // Risk > 0.7
    private Integer mediumRiskCount;  // Risk 0.4 - 0.7
    private Integer lowRiskCount;     // Risk < 0.4

    // AI insights
    private List<String> topRiskFactors;
    private List<String> recommendations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MerchantRiskEntry {
        private Integer rank;
        private String merchId;
        private String hsn;
        private Double riskScore;
        private HealthStatus predictedStatus;
        private Double confidence;

        // Contributing factors to risk score
        private Double errorRateContribution;
        private Double processingTimeContribution;
        private Double trendContribution;
        private Double volumeContribution;

        // Key metrics
        private Double errorRate;
        private Double avgProcessingTimeMs;
        private Long totalBatches;
        private Long totalErrors;

        // Risk factors specific to this merchant
        private List<String> riskFactors;
    }
}

