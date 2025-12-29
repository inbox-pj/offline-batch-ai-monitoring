package com.cardconnect.bolt.ai.model.merchant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Custom alert thresholds per merchant
 * Allows different alert configurations for different merchants based on their specific needs
 */
@Entity
@Table(name = "merchant_alert_thresholds",
       uniqueConstraints = @UniqueConstraint(columnNames = {"merch_id", "hsn"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantAlertThreshold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merch_id", nullable = false, length = 20)
    private String merchId;

    @Column(name = "hsn", length = 50)
    private String hsn;

    @Column(name = "merchant_name", length = 100)
    private String merchantName;

    // Error rate thresholds (as decimal, e.g., 0.05 = 5%)
    @Column(name = "error_rate_warning_threshold")
    @Builder.Default
    private Double errorRateWarningThreshold = 0.02; // 2%

    @Column(name = "error_rate_critical_threshold")
    @Builder.Default
    private Double errorRateCriticalThreshold = 0.05; // 5%

    // Processing time thresholds (in milliseconds)
    @Column(name = "processing_time_warning_ms")
    @Builder.Default
    private Long processingTimeWarningMs = 5000L; // 5 seconds

    @Column(name = "processing_time_critical_ms")
    @Builder.Default
    private Long processingTimeCriticalMs = 10000L; // 10 seconds

    // Volume thresholds
    @Column(name = "min_daily_batches")
    @Builder.Default
    private Integer minDailyBatches = 10; // Minimum expected batches per day

    @Column(name = "max_daily_errors")
    @Builder.Default
    private Integer maxDailyErrors = 100; // Maximum allowed errors per day

    // Risk score thresholds
    @Column(name = "risk_score_warning_threshold")
    @Builder.Default
    private Double riskScoreWarningThreshold = 0.4;

    @Column(name = "risk_score_critical_threshold")
    @Builder.Default
    private Double riskScoreCriticalThreshold = 0.7;

    // Alert configuration
    @Column(name = "alerts_enabled")
    @Builder.Default
    private Boolean alertsEnabled = true;

    @Column(name = "email_notification_enabled")
    @Builder.Default
    private Boolean emailNotificationEnabled = false;

    @Column(name = "slack_notification_enabled")
    @Builder.Default
    private Boolean slackNotificationEnabled = false;

    @Column(name = "notification_email", length = 255)
    private String notificationEmail;

    @Column(name = "notification_slack_channel", length = 100)
    private String notificationSlackChannel;

    // Priority level for this merchant (higher = more important)
    @Column(name = "priority_level")
    @Builder.Default
    private Integer priorityLevel = 1;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Check if error rate exceeds warning threshold
     */
    public boolean isErrorRateWarning(double errorRate) {
        return errorRate >= errorRateWarningThreshold && errorRate < errorRateCriticalThreshold;
    }

    /**
     * Check if error rate exceeds critical threshold
     */
    public boolean isErrorRateCritical(double errorRate) {
        return errorRate >= errorRateCriticalThreshold;
    }

    /**
     * Check if processing time exceeds warning threshold
     */
    public boolean isProcessingTimeWarning(long processingTimeMs) {
        return processingTimeMs >= processingTimeWarningMs && processingTimeMs < processingTimeCriticalMs;
    }

    /**
     * Check if processing time exceeds critical threshold
     */
    public boolean isProcessingTimeCritical(long processingTimeMs) {
        return processingTimeMs >= processingTimeCriticalMs;
    }

    /**
     * Check if risk score exceeds warning threshold
     */
    public boolean isRiskScoreWarning(double riskScore) {
        return riskScore >= riskScoreWarningThreshold && riskScore < riskScoreCriticalThreshold;
    }

    /**
     * Check if risk score exceeds critical threshold
     */
    public boolean isRiskScoreCritical(double riskScore) {
        return riskScore >= riskScoreCriticalThreshold;
    }
}

