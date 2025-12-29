package com.cardconnect.bolt.ai.controller;

import com.cardconnect.bolt.ai.model.AIPredictionAudit;
import com.cardconnect.bolt.ai.model.HealthStatus;
import com.cardconnect.bolt.ai.model.accuracy.ABTestResult;
import com.cardconnect.bolt.ai.model.accuracy.AccuracyMetrics;
import com.cardconnect.bolt.ai.model.accuracy.FeedbackLoopData;
import com.cardconnect.bolt.ai.service.PredictionAccuracyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST API controller for prediction accuracy tracking and A/B testing
 */
@RestController
@RequestMapping("/api/ai/accuracy")
@Slf4j
@Tag(name = "Prediction Accuracy", description = "Prediction accuracy tracking, A/B testing, and feedback loop endpoints")
public class PredictionAccuracyController {

    private final PredictionAccuracyService accuracyService;

    public PredictionAccuracyController(PredictionAccuracyService accuracyService) {
        this.accuracyService = accuracyService;
    }

    // ==================== Outcome Recording ====================

    /**
     * POST /api/ai/accuracy/outcomes/{predictionId}
     * Record actual outcome for a prediction
     */
    @PostMapping("/outcomes/{predictionId}")
    @Operation(
        summary = "Record Prediction Outcome",
        description = "Record the actual outcome after the prediction window has passed"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Outcome recorded successfully",
                content = @Content(schema = @Schema(implementation = AIPredictionAudit.class))),
        @ApiResponse(responseCode = "404", description = "Prediction not found"),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<AIPredictionAudit> recordOutcome(
            @Parameter(description = "Prediction ID", required = true)
            @PathVariable Long predictionId,
            @Valid @RequestBody OutcomeRequest request) {

        log.info("Recording outcome for prediction {}: {}", predictionId, request.getActualOutcome());

        try {
            AIPredictionAudit result = accuracyService.recordOutcome(
                predictionId, request.getActualOutcome(), request.getNotes());
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.warn("Prediction not found: {}", predictionId);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * POST /api/ai/accuracy/evaluate
     * Trigger evaluation of pending predictions
     */
    @PostMapping("/evaluate")
    @Operation(
        summary = "Evaluate Pending Predictions",
        description = "Manually trigger evaluation of predictions whose time horizon has passed"
    )
    @ApiResponse(responseCode = "200", description = "Evaluation completed")
    public ResponseEntity<Map<String, String>> triggerEvaluation() {
        log.info("Manual evaluation triggered");
        accuracyService.evaluatePendingPredictions();

        Map<String, String> response = new HashMap<>();
        response.put("status", "Evaluation completed");
        return ResponseEntity.ok(response);
    }

    // ==================== Accuracy Metrics ====================

    /**
     * GET /api/ai/accuracy/metrics
     * Get comprehensive accuracy metrics
     */
    @GetMapping("/metrics")
    @Operation(
        summary = "Get Accuracy Metrics",
        description = "Get comprehensive accuracy metrics including precision, recall, F1 score, and confusion matrix"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Metrics retrieved successfully",
                content = @Content(schema = @Schema(implementation = AccuracyMetrics.class)))
    })
    public ResponseEntity<AccuracyMetrics> getAccuracyMetrics(
            @Parameter(description = "Number of days to analyze (default: 30)")
            @RequestParam(defaultValue = "30") @Min(1) @Max(365) int days) {

        log.info("Retrieving accuracy metrics for {} days", days);
        AccuracyMetrics metrics = accuracyService.calculateAccuracyMetrics(days);
        return ResponseEntity.ok(metrics);
    }

    /**
     * GET /api/ai/accuracy/summary
     * Get a quick summary of accuracy metrics
     */
    @GetMapping("/summary")
    @Operation(
        summary = "Get Accuracy Summary",
        description = "Get a quick summary of key accuracy metrics"
    )
    @ApiResponse(responseCode = "200", description = "Summary retrieved successfully")
    public ResponseEntity<Map<String, Object>> getAccuracySummary(
            @Parameter(description = "Number of days to analyze (default: 7)")
            @RequestParam(defaultValue = "7") @Min(1) @Max(90) int days) {

        log.info("Retrieving accuracy summary for {} days", days);
        AccuracyMetrics metrics = accuracyService.calculateAccuracyMetrics(days);

        Map<String, Object> summary = new HashMap<>();
        summary.put("analysisWindow", metrics.getAnalysisWindow());
        summary.put("totalPredictions", metrics.getTotalPredictions());
        summary.put("evaluatedPredictions", metrics.getEvaluatedPredictions());
        summary.put("correctPredictions", metrics.getCorrectPredictions());
        summary.put("overallAccuracy", metrics.getOverallAccuracy());
        summary.put("weightedF1Score", metrics.getWeightedF1Score());
        summary.put("confidenceCalibration", metrics.getConfidenceCalibration());
        summary.put("accuracyTrend", metrics.getAccuracyTrend());
        summary.put("insights", metrics.getInsights());

        return ResponseEntity.ok(summary);
    }

    // ==================== A/B Testing ====================

    /**
     * GET /api/ai/accuracy/ab-test
     * Compare AI vs rule-based model performance
     */
    @GetMapping("/ab-test")
    @Operation(
        summary = "Get A/B Test Results",
        description = "Compare AI model performance against rule-based model"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "A/B test results retrieved successfully",
                content = @Content(schema = @Schema(implementation = ABTestResult.class)))
    })
    public ResponseEntity<ABTestResult> getABTestResults(
            @Parameter(description = "Number of days to analyze (default: 30)")
            @RequestParam(defaultValue = "30") @Min(1) @Max(365) int days) {

        log.info("Retrieving A/B test results for {} days", days);
        ABTestResult result = accuracyService.compareModels(days);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/ai/accuracy/ab-test/summary
     * Get quick summary of A/B test
     */
    @GetMapping("/ab-test/summary")
    @Operation(
        summary = "Get A/B Test Summary",
        description = "Get a quick summary of AI vs rule-based comparison"
    )
    @ApiResponse(responseCode = "200", description = "Summary retrieved successfully")
    public ResponseEntity<Map<String, Object>> getABTestSummary(
            @Parameter(description = "Number of days to analyze (default: 7)")
            @RequestParam(defaultValue = "7") @Min(1) @Max(90) int days) {

        log.info("Retrieving A/B test summary for {} days", days);
        ABTestResult result = accuracyService.compareModels(days);

        Map<String, Object> summary = new HashMap<>();
        summary.put("analysisWindow", result.getAnalysisWindow());
        summary.put("winner", result.getWinner());
        summary.put("winMargin", result.getWinMargin());
        summary.put("isStatisticallySignificant", result.getIsStatisticallySignificant());

        if (result.getAiModelPerformance() != null) {
            summary.put("aiAccuracy", result.getAiModelPerformance().getAccuracy());
            summary.put("aiPredictions", result.getAiModelPerformance().getEvaluatedPredictions());
        }
        if (result.getRuleBasedPerformance() != null) {
            summary.put("ruleBasedAccuracy", result.getRuleBasedPerformance().getAccuracy());
            summary.put("ruleBasedPredictions", result.getRuleBasedPerformance().getEvaluatedPredictions());
        }

        summary.put("accuracyDifference", result.getAccuracyDifference());
        summary.put("insights", result.getInsights());
        summary.put("recommendations", result.getRecommendations());

        return ResponseEntity.ok(summary);
    }

    // ==================== Feedback Loop ====================

    /**
     * GET /api/ai/accuracy/feedback
     * Get feedback loop data for model improvement
     */
    @GetMapping("/feedback")
    @Operation(
        summary = "Get Feedback Loop Data",
        description = "Get analysis of prediction errors and recommendations for model improvement"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Feedback data retrieved successfully",
                content = @Content(schema = @Schema(implementation = FeedbackLoopData.class)))
    })
    public ResponseEntity<FeedbackLoopData> getFeedbackLoopData(
            @Parameter(description = "Number of days to analyze (default: 30)")
            @RequestParam(defaultValue = "30") @Min(1) @Max(365) int days) {

        log.info("Generating feedback loop data for {} days", days);
        FeedbackLoopData data = accuracyService.generateFeedbackLoopData(days);
        return ResponseEntity.ok(data);
    }

    /**
     * GET /api/ai/accuracy/feedback/errors
     * Get high-confidence errors for review
     */
    @GetMapping("/feedback/errors")
    @Operation(
        summary = "Get High-Confidence Errors",
        description = "Get predictions that had high confidence but were incorrect"
    )
    @ApiResponse(responseCode = "200", description = "Errors retrieved successfully")
    public ResponseEntity<Map<String, Object>> getHighConfidenceErrors(
            @Parameter(description = "Number of days to analyze (default: 7)")
            @RequestParam(defaultValue = "7") @Min(1) @Max(90) int days) {

        log.info("Retrieving high-confidence errors for {} days", days);
        FeedbackLoopData data = accuracyService.generateFeedbackLoopData(days);

        Map<String, Object> response = new HashMap<>();
        response.put("analysisWindow", data.getAnalysisWindow());
        response.put("highConfidenceErrors", data.getHighConfidenceErrors());
        response.put("errorPatterns", data.getErrorPatterns());

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/ai/accuracy/feedback/drift
     * Get model drift analysis
     */
    @GetMapping("/feedback/drift")
    @Operation(
        summary = "Get Drift Analysis",
        description = "Detect if the model's performance is drifting over time"
    )
    @ApiResponse(responseCode = "200", description = "Drift analysis retrieved successfully")
    public ResponseEntity<FeedbackLoopData.DriftAnalysis> getDriftAnalysis(
            @Parameter(description = "Number of days to analyze (default: 30)")
            @RequestParam(defaultValue = "30") @Min(1) @Max(365) int days) {

        log.info("Retrieving drift analysis for {} days", days);
        FeedbackLoopData data = accuracyService.generateFeedbackLoopData(days);
        return ResponseEntity.ok(data.getDriftAnalysis());
    }

    /**
     * GET /api/ai/accuracy/feedback/recommendations
     * Get improvement recommendations
     */
    @GetMapping("/feedback/recommendations")
    @Operation(
        summary = "Get Improvement Recommendations",
        description = "Get actionable recommendations for improving model accuracy"
    )
    @ApiResponse(responseCode = "200", description = "Recommendations retrieved successfully")
    public ResponseEntity<Map<String, Object>> getImprovementRecommendations(
            @Parameter(description = "Number of days to analyze (default: 30)")
            @RequestParam(defaultValue = "30") @Min(1) @Max(365) int days) {

        log.info("Retrieving improvement recommendations for {} days", days);
        FeedbackLoopData data = accuracyService.generateFeedbackLoopData(days);

        Map<String, Object> response = new HashMap<>();
        response.put("analysisWindow", data.getAnalysisWindow());
        response.put("improvementRecommendations", data.getImprovementRecommendations());
        response.put("featureInsights", data.getFeatureInsights());
        response.put("misclassificationPatterns", data.getMisclassificationPatterns());

        return ResponseEntity.ok(response);
    }

    // ==================== Request/Response DTOs ====================

    @Data
    @Schema(description = "Outcome recording request")
    public static class OutcomeRequest {
        @NotNull(message = "Actual outcome is required")
        @Schema(description = "The actual health status that occurred", required = true,
                example = "WARNING")
        private HealthStatus actualOutcome;

        @Schema(description = "Optional notes about the outcome", example = "Error spike detected at 3pm")
        private String notes;
    }
}

