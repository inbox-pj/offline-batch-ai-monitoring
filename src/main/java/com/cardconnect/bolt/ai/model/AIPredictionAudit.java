package com.cardconnect.bolt.ai.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "ai_prediction_audit")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
//@EntityListeners(AuditingEntityListener.class)
public class AIPredictionAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "prediction_time", nullable = false)
    private LocalDateTime predictionTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "predicted_status", nullable = false)
    private HealthStatus predictedStatus;

    @Column(nullable = false)
    private Double confidence;

    @Column(name = "time_horizon_hours", nullable = false)
    private Integer timeHorizon;

    @ElementCollection
    @CollectionTable(name = "ai_prediction_findings", joinColumns = @JoinColumn(name = "prediction_id"))
    @Column(name = "finding")
    private List<String> keyFindings;

    @ElementCollection
    @CollectionTable(name = "ai_prediction_recommendations", joinColumns = @JoinColumn(name = "prediction_id"))
    @Column(name = "recommendation")
    private List<String> recommendations;

    @Column(columnDefinition = "TEXT")
    private String reasoning;

    @Column(name = "input_metrics_count")
    private Integer inputMetricsCount;

    @Column(name = "prompt_version", length = 10)
    private String promptVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "actual_outcome")
    private HealthStatus actualOutcome;

    @Column(name = "outcome_timestamp")
    private LocalDateTime outcomeTimestamp;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    // ======= A/B Testing Fields =======

    /**
     * Model type: "AI" or "RULE_BASED"
     */
    @Column(name = "model_type", length = 20)
    @Builder.Default
    private String modelType = "AI";

    /**
     * Whether this prediction is part of an A/B test
     */
    @Column(name = "is_ab_test")
    @Builder.Default
    private Boolean isAbTest = false;

    /**
     * A/B test group identifier
     */
    @Column(name = "ab_test_group", length = 50)
    private String abTestGroup;

    // ======= Enhanced Tracking Fields =======

    /**
     * Merchant ID if this is a merchant-specific prediction
     */
    @Column(name = "merch_id", length = 20)
    private String merchId;

    /**
     * Response time of the prediction in milliseconds
     */
    @Column(name = "response_time_ms")
    private Long responseTimeMs;

    /**
     * Whether the prediction failed/had an error
     */
    @Column(name = "is_error")
    @Builder.Default
    private Boolean isError = false;

    /**
     * Error message if prediction failed
     */
    @Column(name = "error_message", length = 500)
    private String errorMessage;

    /**
     * Risk score for this prediction
     */
    @Column(name = "risk_score")
    private Double riskScore;

    /**
     * Actual error rate at outcome time
     */
    @Column(name = "actual_error_rate")
    private Double actualErrorRate;

    /**
     * Predicted error rate (if available)
     */
    @Column(name = "predicted_error_rate")
    private Double predictedErrorRate;

    /**
     * Notes/comments about outcome evaluation
     */
    @Column(name = "evaluation_notes", length = 500)
    private String evaluationNotes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (predictionTime == null) {
            predictionTime = LocalDateTime.now();
        }
        if (modelType == null) {
            modelType = "AI";
        }
    }
}
