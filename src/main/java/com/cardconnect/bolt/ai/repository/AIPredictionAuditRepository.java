package com.cardconnect.bolt.ai.repository;

import com.cardconnect.bolt.ai.model.AIPredictionAudit;
import com.cardconnect.bolt.ai.model.HealthStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AIPredictionAuditRepository extends JpaRepository<AIPredictionAudit, Long> {

    List<AIPredictionAudit> findByPredictionTimeAfter(LocalDateTime timestamp);

    List<AIPredictionAudit> findByPredictionTimeBetween(LocalDateTime start, LocalDateTime end);

    List<AIPredictionAudit> findByPredictionTimeAfterAndActualOutcomeNotNull(LocalDateTime timestamp);

    List<AIPredictionAudit> findByPredictedStatus(HealthStatus status);

    List<AIPredictionAudit> findByIsCorrect(Boolean isCorrect);

    Long countByPredictionTimeAfter(LocalDateTime timestamp);

    @Query("SELECT COUNT(a) FROM AIPredictionAudit a WHERE a.predictionTime > :since AND a.isCorrect = true")
    Long countCorrectPredictionsSince(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(a) FROM AIPredictionAudit a WHERE a.predictionTime > :since AND a.isCorrect IS NOT NULL")
    Long countEvaluatedPredictionsSince(@Param("since") LocalDateTime since);

    @Query("SELECT AVG(a.confidence) FROM AIPredictionAudit a WHERE a.predictionTime > :since")
    Double averageConfidenceSince(@Param("since") LocalDateTime since);

    @Query("SELECT a FROM AIPredictionAudit a WHERE a.predictionTime > :since AND a.predictedStatus = :status")
    List<AIPredictionAudit> findByPredictionTimeAfterAndPredictedStatus(
            @Param("since") LocalDateTime since,
            @Param("status") HealthStatus status);

    @Query("SELECT a FROM AIPredictionAudit a WHERE a.actualOutcome IS NULL AND a.predictionTime < :threshold")
    List<AIPredictionAudit> findPendingEvaluationBefore(@Param("threshold") LocalDateTime threshold);

    // ======= A/B Testing Queries =======

    /**
     * Find predictions by model type (AI or RULE_BASED)
     */
    List<AIPredictionAudit> findByModelTypeAndPredictionTimeAfter(String modelType, LocalDateTime timestamp);

    /**
     * Find evaluated predictions by model type
     */
    @Query("SELECT a FROM AIPredictionAudit a WHERE a.modelType = :modelType AND a.predictionTime > :since AND a.actualOutcome IS NOT NULL")
    List<AIPredictionAudit> findEvaluatedByModelType(
            @Param("modelType") String modelType,
            @Param("since") LocalDateTime since);

    /**
     * Count correct predictions by model type
     */
    @Query("SELECT COUNT(a) FROM AIPredictionAudit a WHERE a.modelType = :modelType AND a.predictionTime > :since AND a.isCorrect = true")
    Long countCorrectByModelType(
            @Param("modelType") String modelType,
            @Param("since") LocalDateTime since);

    /**
     * Count evaluated predictions by model type
     */
    @Query("SELECT COUNT(a) FROM AIPredictionAudit a WHERE a.modelType = :modelType AND a.predictionTime > :since AND a.isCorrect IS NOT NULL")
    Long countEvaluatedByModelType(
            @Param("modelType") String modelType,
            @Param("since") LocalDateTime since);

    /**
     * Average confidence by model type
     */
    @Query("SELECT AVG(a.confidence) FROM AIPredictionAudit a WHERE a.modelType = :modelType AND a.predictionTime > :since")
    Double averageConfidenceByModelType(
            @Param("modelType") String modelType,
            @Param("since") LocalDateTime since);

    /**
     * Average response time by model type
     */
    @Query("SELECT AVG(a.responseTimeMs) FROM AIPredictionAudit a WHERE a.modelType = :modelType AND a.predictionTime > :since AND a.responseTimeMs IS NOT NULL")
    Double averageResponseTimeByModelType(
            @Param("modelType") String modelType,
            @Param("since") LocalDateTime since);

    /**
     * Find A/B test predictions
     */
    List<AIPredictionAudit> findByIsAbTestTrueAndPredictionTimeAfter(LocalDateTime timestamp);

    /**
     * Find A/B test predictions by group
     */
    List<AIPredictionAudit> findByAbTestGroupAndPredictionTimeAfter(String abTestGroup, LocalDateTime timestamp);

    // ======= Feedback Loop Queries =======

    /**
     * Find high-confidence errors
     */
    @Query("SELECT a FROM AIPredictionAudit a WHERE a.isCorrect = false AND a.confidence >= :minConfidence AND a.predictionTime > :since ORDER BY a.confidence DESC")
    List<AIPredictionAudit> findHighConfidenceErrors(
            @Param("minConfidence") Double minConfidence,
            @Param("since") LocalDateTime since);

    /**
     * Find misclassifications by predicted and actual status
     */
    @Query("SELECT a FROM AIPredictionAudit a WHERE a.predictedStatus = :predicted AND a.actualOutcome = :actual AND a.predictionTime > :since")
    List<AIPredictionAudit> findMisclassifications(
            @Param("predicted") HealthStatus predicted,
            @Param("actual") HealthStatus actual,
            @Param("since") LocalDateTime since);

    /**
     * Count misclassifications by predicted and actual status
     */
    @Query("SELECT COUNT(a) FROM AIPredictionAudit a WHERE a.predictedStatus = :predicted AND a.actualOutcome = :actual AND a.predictionTime > :since")
    Long countMisclassifications(
            @Param("predicted") HealthStatus predicted,
            @Param("actual") HealthStatus actual,
            @Param("since") LocalDateTime since);

    /**
     * Find predictions by merchant
     */
    List<AIPredictionAudit> findByMerchIdAndPredictionTimeAfter(String merchId, LocalDateTime timestamp);

    /**
     * Find errors (failed predictions)
     */
    List<AIPredictionAudit> findByIsErrorTrueAndPredictionTimeAfter(LocalDateTime timestamp);

    /**
     * Count errors by model type
     */
    @Query("SELECT COUNT(a) FROM AIPredictionAudit a WHERE a.modelType = :modelType AND a.isError = true AND a.predictionTime > :since")
    Long countErrorsByModelType(
            @Param("modelType") String modelType,
            @Param("since") LocalDateTime since);

    // ======= Accuracy Trend Queries =======

    /**
     * Get accuracy over time periods (for trend analysis)
     */
    @Query("SELECT a FROM AIPredictionAudit a WHERE a.predictionTime BETWEEN :start AND :end AND a.isCorrect IS NOT NULL ORDER BY a.predictionTime")
    List<AIPredictionAudit> findEvaluatedBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * Get recent predictions with outcomes for calibration analysis
     */
    @Query("SELECT a FROM AIPredictionAudit a WHERE a.actualOutcome IS NOT NULL AND a.predictionTime > :since ORDER BY a.confidence")
    List<AIPredictionAudit> findForCalibrationAnalysis(@Param("since") LocalDateTime since);
}
