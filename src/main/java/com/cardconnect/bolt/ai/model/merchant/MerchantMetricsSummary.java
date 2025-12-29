package com.cardconnect.bolt.ai.model.merchant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Summary of merchant-specific metrics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantMetricsSummary {

    private String merchId;
    private String hsn;

    // Batch metrics
    private Long totalBatches;
    private Long totalProcessed;
    private Long totalErrors;
    private Double errorRate;

    // Processing time metrics
    private Double avgProcessingTimeMs;
    private Double minProcessingTimeMs;
    private Double maxProcessingTimeMs;
    private Double p95ProcessingTimeMs;

    // Trend indicators
    private Double errorRateChange;      // Percentage change from previous period
    private Double processingTimeChange; // Percentage change from previous period
    private Double volumeChange;         // Percentage change in batch volume

    // Time range
    private String analysisWindow;
    private Long dataPointCount;
}

