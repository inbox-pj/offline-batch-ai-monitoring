package com.cardconnect.bolt.ai.model.merchant;

import com.cardconnect.bolt.ai.model.HealthStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Comparison of health metrics across multiple merchants
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantComparison {

    private LocalDateTime comparisonTime;
    private String analysisWindow;
    private Integer totalMerchantsAnalyzed;

    // Distribution summary
    private Integer healthyCount;
    private Integer warningCount;
    private Integer criticalCount;
    private Integer unknownCount;

    // Detailed comparisons
    private List<MerchantComparisonDetail> merchantDetails;

    // Insights from AI
    private List<String> insights;
    private List<String> commonIssues;
    private List<String> recommendations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MerchantComparisonDetail {
        private String merchId;
        private String hsn;
        private HealthStatus healthStatus;
        private Double riskScore;
        private Double errorRate;
        private Double avgProcessingTimeMs;
        private Long totalBatches;
        private Long totalErrors;

        // Ranking indicators
        private Integer errorRateRank;        // 1 = worst
        private Integer processingTimeRank;   // 1 = slowest
        private Integer volumeRank;           // 1 = highest volume
        private Integer overallRiskRank;      // 1 = highest risk

        // Comparison to average
        private Double errorRateVsAverage;      // Percentage above/below average
        private Double processingTimeVsAverage; // Percentage above/below average
    }
}

