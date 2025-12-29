package com.cardconnect.bolt.ai.controller;

import com.cardconnect.bolt.ai.model.merchant.MerchantAlertThreshold;
import com.cardconnect.bolt.ai.model.merchant.MerchantComparison;
import com.cardconnect.bolt.ai.model.merchant.MerchantPrediction;
import com.cardconnect.bolt.ai.model.merchant.MerchantRiskRanking;
import com.cardconnect.bolt.ai.model.merchant.MerchantRiskRanking.MerchantRiskEntry;
import com.cardconnect.bolt.ai.repository.MerchantAlertThresholdRepository;
import com.cardconnect.bolt.ai.service.MerchantPredictionService;
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
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * REST API controller for merchant-specific AI predictions
 * Provides endpoints for per-merchant predictions, comparisons, and risk rankings
 */
@RestController
@RequestMapping("/api/ai/merchant")
@Slf4j
@Tag(name = "Merchant Predictions", description = "Merchant-specific AI health predictions and risk analysis")
public class MerchantPredictionController {

    private final Optional<MerchantPredictionService> merchantPredictionService;
    private final MerchantAlertThresholdRepository thresholdRepository;

    @Autowired
    public MerchantPredictionController(
            @Autowired(required = false) MerchantPredictionService merchantPredictionService,
            MerchantAlertThresholdRepository thresholdRepository) {
        this.merchantPredictionService = Optional.ofNullable(merchantPredictionService);
        this.thresholdRepository = thresholdRepository;
    }

    // ==================== Prediction Endpoints ====================

    /**
     * GET /api/ai/merchant/{merchId}/predict
     * Get AI prediction for a specific merchant
     */
    @GetMapping("/{merchId}/predict")
    @Operation(
        summary = "Get Merchant Health Prediction",
        description = "Analyzes metrics for a specific merchant and predicts potential health issues"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Prediction successful",
                content = @Content(schema = @Schema(implementation = MerchantPrediction.class))),
        @ApiResponse(responseCode = "503", description = "AI service not available"),
        @ApiResponse(responseCode = "500", description = "Prediction failed")
    })
    public ResponseEntity<MerchantPrediction> getMerchantPrediction(
            @Parameter(description = "Merchant ID", required = true)
            @PathVariable @NotBlank String merchId) {

        if (merchantPredictionService.isEmpty()) {
            log.warn("Merchant prediction requested but AI service is not enabled");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(MerchantPrediction.error(merchId, "AI prediction service is not enabled"));
        }

        try {
            log.info("Received prediction request for merchant: {}", merchId);
            MerchantPrediction result = merchantPredictionService.get().predictMerchantHealth(merchId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Prediction failed for merchant: {}", merchId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(MerchantPrediction.error(merchId, "Prediction failed: " + e.getMessage()));
        }
    }

    /**
     * GET /api/ai/merchant/predict/all
     * Get predictions for all active merchants
     */
    @GetMapping("/predict/all")
    @Operation(
        summary = "Get All Merchant Predictions",
        description = "Analyzes metrics and generates predictions for all active merchants"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Predictions generated successfully"),
        @ApiResponse(responseCode = "503", description = "AI service not available"),
        @ApiResponse(responseCode = "500", description = "Prediction failed")
    })
    public ResponseEntity<List<MerchantPrediction>> getAllMerchantPredictions() {
        if (merchantPredictionService.isEmpty()) {
            log.warn("All merchant predictions requested but AI service is not enabled");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }

        try {
            log.info("Received request for all merchant predictions");
            List<MerchantPrediction> results = merchantPredictionService.get().predictAllMerchants();
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("Failed to generate predictions for all merchants", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== Comparison Endpoints ====================

    /**
     * GET /api/ai/merchant/compare
     * Compare health metrics across all merchants
     */
    @GetMapping("/compare")
    @Operation(
        summary = "Compare Merchant Health",
        description = "Compares health metrics and status across all active merchants"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Comparison generated successfully",
                content = @Content(schema = @Schema(implementation = MerchantComparison.class))),
        @ApiResponse(responseCode = "503", description = "AI service not available"),
        @ApiResponse(responseCode = "500", description = "Comparison failed")
    })
    public ResponseEntity<MerchantComparison> compareMerchants() {
        if (merchantPredictionService.isEmpty()) {
            log.warn("Merchant comparison requested but AI service is not enabled");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }

        try {
            log.info("Received merchant comparison request");
            MerchantComparison result = merchantPredictionService.get().compareMerchants();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Merchant comparison failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== Risk Ranking Endpoints ====================

    /**
     * GET /api/ai/merchant/risk-ranking
     * Get risk ranking of all merchants
     */
    @GetMapping("/risk-ranking")
    @Operation(
        summary = "Get Merchant Risk Ranking",
        description = "Returns all merchants ranked by risk score (highest risk first)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Risk ranking generated successfully",
                content = @Content(schema = @Schema(implementation = MerchantRiskRanking.class))),
        @ApiResponse(responseCode = "503", description = "AI service not available"),
        @ApiResponse(responseCode = "500", description = "Ranking failed")
    })
    public ResponseEntity<MerchantRiskRanking> getMerchantRiskRanking() {
        if (merchantPredictionService.isEmpty()) {
            log.warn("Risk ranking requested but AI service is not enabled");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }

        try {
            log.info("Received merchant risk ranking request");
            MerchantRiskRanking result = merchantPredictionService.get().getMerchantRiskRanking();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Risk ranking failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/ai/merchant/top-risk
     * Get top N at-risk merchants
     */
    @GetMapping("/top-risk")
    @Operation(
        summary = "Get Top At-Risk Merchants",
        description = "Returns the top N merchants with highest risk scores"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Top risk merchants retrieved successfully"),
        @ApiResponse(responseCode = "503", description = "AI service not available"),
        @ApiResponse(responseCode = "500", description = "Request failed")
    })
    public ResponseEntity<List<MerchantRiskEntry>> getTopRiskMerchants(
            @Parameter(description = "Number of merchants to return (default: 10)")
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {

        if (merchantPredictionService.isEmpty()) {
            log.warn("Top risk merchants requested but AI service is not enabled");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }

        try {
            log.info("Received request for top {} at-risk merchants", limit);
            List<MerchantRiskEntry> results = merchantPredictionService.get().getTopRiskMerchants(limit);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("Failed to get top risk merchants", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== Alert Threshold Endpoints ====================

    /**
     * GET /api/ai/merchant/{merchId}/thresholds
     * Get alert thresholds for a specific merchant
     */
    @GetMapping("/{merchId}/thresholds")
    @Operation(
        summary = "Get Merchant Alert Thresholds",
        description = "Returns the custom alert thresholds configured for a specific merchant"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Thresholds retrieved successfully",
                content = @Content(schema = @Schema(implementation = MerchantAlertThreshold.class))),
        @ApiResponse(responseCode = "404", description = "No custom thresholds found for merchant")
    })
    public ResponseEntity<MerchantAlertThreshold> getMerchantThresholds(
            @Parameter(description = "Merchant ID", required = true)
            @PathVariable @NotBlank String merchId) {

        log.info("Retrieving thresholds for merchant: {}", merchId);

        return thresholdRepository.findByMerchId(merchId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/ai/merchant/thresholds
     * Get all merchant alert thresholds
     */
    @GetMapping("/thresholds")
    @Operation(
        summary = "Get All Merchant Thresholds",
        description = "Returns all custom alert thresholds configured for merchants"
    )
    @ApiResponse(responseCode = "200", description = "Thresholds retrieved successfully")
    public ResponseEntity<List<MerchantAlertThreshold>> getAllThresholds() {
        log.info("Retrieving all merchant thresholds");
        return ResponseEntity.ok(thresholdRepository.findAllByOrderByPriorityLevelDesc());
    }

    /**
     * POST /api/ai/merchant/thresholds
     * Create or update alert thresholds for a merchant
     */
    @PostMapping("/thresholds")
    @Operation(
        summary = "Create/Update Merchant Thresholds",
        description = "Creates or updates custom alert thresholds for a merchant"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Thresholds saved successfully",
                content = @Content(schema = @Schema(implementation = MerchantAlertThreshold.class))),
        @ApiResponse(responseCode = "400", description = "Invalid threshold configuration")
    })
    public ResponseEntity<MerchantAlertThreshold> saveThresholds(
            @Valid @RequestBody ThresholdRequest request) {

        log.info("Saving thresholds for merchant: {}", request.getMerchId());

        MerchantAlertThreshold threshold = thresholdRepository.findByMerchId(request.getMerchId())
            .orElse(MerchantAlertThreshold.builder().merchId(request.getMerchId()).build());

        // Update fields from request
        updateThresholdFromRequest(threshold, request);

        MerchantAlertThreshold saved = thresholdRepository.save(threshold);
        return ResponseEntity.ok(saved);
    }

    /**
     * PUT /api/ai/merchant/{merchId}/thresholds
     * Update alert thresholds for a specific merchant
     */
    @PutMapping("/{merchId}/thresholds")
    @Operation(
        summary = "Update Merchant Thresholds",
        description = "Updates custom alert thresholds for a specific merchant"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Thresholds updated successfully"),
        @ApiResponse(responseCode = "404", description = "Merchant thresholds not found"),
        @ApiResponse(responseCode = "400", description = "Invalid threshold configuration")
    })
    public ResponseEntity<MerchantAlertThreshold> updateThresholds(
            @Parameter(description = "Merchant ID", required = true)
            @PathVariable @NotBlank String merchId,
            @Valid @RequestBody ThresholdRequest request) {

        log.info("Updating thresholds for merchant: {}", merchId);

        return thresholdRepository.findByMerchId(merchId)
            .map(threshold -> {
                updateThresholdFromRequest(threshold, request);
                return ResponseEntity.ok(thresholdRepository.save(threshold));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * DELETE /api/ai/merchant/{merchId}/thresholds
     * Delete custom thresholds for a merchant (revert to defaults)
     */
    @DeleteMapping("/{merchId}/thresholds")
    @Operation(
        summary = "Delete Merchant Thresholds",
        description = "Deletes custom alert thresholds for a merchant, reverting to system defaults"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Thresholds deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Merchant thresholds not found")
    })
    public ResponseEntity<Void> deleteThresholds(
            @Parameter(description = "Merchant ID", required = true)
            @PathVariable @NotBlank String merchId) {

        log.info("Deleting thresholds for merchant: {}", merchId);

        if (thresholdRepository.existsByMerchId(merchId)) {
            thresholdRepository.deleteByMerchId(merchId);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    // ==================== Helper Methods ====================

    private void updateThresholdFromRequest(MerchantAlertThreshold threshold, ThresholdRequest request) {
        if (request.getMerchantName() != null) threshold.setMerchantName(request.getMerchantName());
        if (request.getHsn() != null) threshold.setHsn(request.getHsn());
        if (request.getErrorRateWarningThreshold() != null)
            threshold.setErrorRateWarningThreshold(request.getErrorRateWarningThreshold());
        if (request.getErrorRateCriticalThreshold() != null)
            threshold.setErrorRateCriticalThreshold(request.getErrorRateCriticalThreshold());
        if (request.getProcessingTimeWarningMs() != null)
            threshold.setProcessingTimeWarningMs(request.getProcessingTimeWarningMs());
        if (request.getProcessingTimeCriticalMs() != null)
            threshold.setProcessingTimeCriticalMs(request.getProcessingTimeCriticalMs());
        if (request.getMinDailyBatches() != null) threshold.setMinDailyBatches(request.getMinDailyBatches());
        if (request.getMaxDailyErrors() != null) threshold.setMaxDailyErrors(request.getMaxDailyErrors());
        if (request.getRiskScoreWarningThreshold() != null)
            threshold.setRiskScoreWarningThreshold(request.getRiskScoreWarningThreshold());
        if (request.getRiskScoreCriticalThreshold() != null)
            threshold.setRiskScoreCriticalThreshold(request.getRiskScoreCriticalThreshold());
        if (request.getAlertsEnabled() != null) threshold.setAlertsEnabled(request.getAlertsEnabled());
        if (request.getEmailNotificationEnabled() != null)
            threshold.setEmailNotificationEnabled(request.getEmailNotificationEnabled());
        if (request.getSlackNotificationEnabled() != null)
            threshold.setSlackNotificationEnabled(request.getSlackNotificationEnabled());
        if (request.getNotificationEmail() != null) threshold.setNotificationEmail(request.getNotificationEmail());
        if (request.getNotificationSlackChannel() != null)
            threshold.setNotificationSlackChannel(request.getNotificationSlackChannel());
        if (request.getPriorityLevel() != null) threshold.setPriorityLevel(request.getPriorityLevel());
        if (request.getNotes() != null) threshold.setNotes(request.getNotes());
    }

    // ==================== Request/Response DTOs ====================

    @Data
    @Schema(description = "Threshold configuration request")
    public static class ThresholdRequest {
        @NotBlank(message = "Merchant ID is required")
        @Schema(description = "Merchant ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "MERCH001")
        private String merchId;

        @Schema(description = "Merchant name", example = "Acme Corporation")
        private String merchantName;

        @Schema(description = "Hardware Serial Number", example = "HSN12345")
        private String hsn;

        @Schema(description = "Error rate warning threshold (0.0-1.0)", example = "0.02")
        @Min(0) @Max(1)
        private Double errorRateWarningThreshold;

        @Schema(description = "Error rate critical threshold (0.0-1.0)", example = "0.05")
        @Min(0) @Max(1)
        private Double errorRateCriticalThreshold;

        @Schema(description = "Processing time warning threshold in ms", example = "5000")
        @Min(0)
        private Long processingTimeWarningMs;

        @Schema(description = "Processing time critical threshold in ms", example = "10000")
        @Min(0)
        private Long processingTimeCriticalMs;

        @Schema(description = "Minimum expected daily batches", example = "10")
        @Min(0)
        private Integer minDailyBatches;

        @Schema(description = "Maximum allowed daily errors", example = "100")
        @Min(0)
        private Integer maxDailyErrors;

        @Schema(description = "Risk score warning threshold (0.0-1.0)", example = "0.4")
        @Min(0) @Max(1)
        private Double riskScoreWarningThreshold;

        @Schema(description = "Risk score critical threshold (0.0-1.0)", example = "0.7")
        @Min(0) @Max(1)
        private Double riskScoreCriticalThreshold;

        @Schema(description = "Enable alerts for this merchant", example = "true")
        private Boolean alertsEnabled;

        @Schema(description = "Enable email notifications", example = "false")
        private Boolean emailNotificationEnabled;

        @Schema(description = "Enable Slack notifications", example = "true")
        private Boolean slackNotificationEnabled;

        @Schema(description = "Email address for notifications", example = "ops@example.com")
        private String notificationEmail;

        @Schema(description = "Slack channel for notifications", example = "#alerts-merchant")
        private String notificationSlackChannel;

        @Schema(description = "Priority level (higher = more important)", example = "1")
        @Min(1) @Max(10)
        private Integer priorityLevel;

        @Schema(description = "Notes about this merchant", example = "High-volume merchant, requires special attention")
        private String notes;
    }
}

