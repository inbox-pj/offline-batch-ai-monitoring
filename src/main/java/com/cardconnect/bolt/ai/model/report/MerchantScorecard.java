package com.cardconnect.bolt.ai.model.report;

import com.cardconnect.bolt.ai.model.HealthStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Merchant Scorecard with individual health scores and metrics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantScorecard {

    private String merchId;
    private String hsn;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private LocalDateTime generatedAt;

    // Overall Score (0-100)
    private double overallScore;
    private HealthStatus healthStatus;
    private String healthGrade; // A, B, C, D, F

    // Key Metrics
    private long totalBatches;
    private long totalTransactionsProcessed;
    private long totalErrors;
    private double errorRate;
    private double averageProcessingTimeMs;

    // Score Components
    private ScoreBreakdown scoreBreakdown;

    // Historical Trend
    private List<DailyScore> dailyScores;
    private TrendDirection overallTrend;

    // Comparison with Peers
    private PeerComparison peerComparison;

    // Issues and Recommendations
    private List<Issue> activeIssues;
    private List<String> recommendations;

    // Risk Assessment
    private RiskLevel riskLevel;
    private List<String> riskFactors;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreBreakdown {
        private double errorRateScore;      // 0-25 points
        private double processingTimeScore; // 0-25 points
        private double reliabilityScore;    // 0-25 points
        private double volumeScore;         // 0-25 points
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyScore {
        private LocalDate date;
        private double score;
        private double errorRate;
        private double avgProcessingTimeMs;
        private long batchCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PeerComparison {
        private double percentileRank;
        private double averagePeerScore;
        private double scoreDifferenceFromAverage;
        private int totalMerchantsCompared;
        private String comparisonSummary;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Issue {
        private String issueType;
        private String description;
        private IssueSeverity severity;
        private LocalDateTime firstDetected;
        private int occurrenceCount;
    }

    public enum TrendDirection {
        IMPROVING,
        STABLE,
        DEGRADING,
        UNKNOWN
    }

    public enum RiskLevel {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    public enum IssueSeverity {
        INFO,
        WARNING,
        ERROR,
        CRITICAL
    }
}

