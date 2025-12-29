package com.cardconnect.bolt.ai.controller;

import com.cardconnect.bolt.ai.model.report.*;
import com.cardconnect.bolt.ai.service.AnalyticsReportingService;
import com.cardconnect.bolt.ai.service.ReportExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST API controller for Analytics and Reporting
 */
@RestController
@RequestMapping("/api/reports")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Analytics & Reporting", description = "Endpoints for generating analytics reports and exporting data")
public class AnalyticsReportController {

    private final AnalyticsReportingService analyticsService;
    private final ReportExportService exportService;

    // ========================================================================
    // DAILY SUMMARY REPORT
    // ========================================================================

    @GetMapping("/daily")
    @Operation(
        summary = "Get Daily Summary Report",
        description = "Generate a daily summary report for a specific date with key metrics, health status, and alerts"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Report generated successfully",
                content = @Content(schema = @Schema(implementation = DailySummaryReport.class))),
        @ApiResponse(responseCode = "400", description = "Invalid date format"),
        @ApiResponse(responseCode = "500", description = "Report generation failed")
    })
    public ResponseEntity<DailySummaryReport> getDailySummary(
            @Parameter(description = "Date for the report (defaults to today)", example = "2024-12-29")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            LocalDate reportDate = date != null ? date : LocalDate.now();
            log.info("Generating daily summary report for: {}", reportDate);
            DailySummaryReport report = analyticsService.generateDailySummary(reportDate);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            log.error("Failed to generate daily summary report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/daily/export")
    @Operation(
        summary = "Export Daily Summary Report to CSV",
        description = "Download daily summary report as a CSV file"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "CSV export successful"),
        @ApiResponse(responseCode = "500", description = "Export failed")
    })
    public ResponseEntity<byte[]> exportDailySummaryToCsv(
            @Parameter(description = "Date for the report", example = "2024-12-29")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            LocalDate reportDate = date != null ? date : LocalDate.now();
            DailySummaryReport report = analyticsService.generateDailySummary(reportDate);
            byte[] csvData = exportService.exportDailySummaryToCsv(report);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment",
                    "daily-summary-" + reportDate + ".csv");

            return new ResponseEntity<>(csvData, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Failed to export daily summary report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========================================================================
    // WEEKLY TREND REPORT
    // ========================================================================

    @GetMapping("/weekly")
    @Operation(
        summary = "Get Weekly Trend Report",
        description = "Generate a weekly trend report with day-by-day breakdown and week-over-week comparison"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Report generated successfully",
                content = @Content(schema = @Schema(implementation = WeeklyTrendReport.class))),
        @ApiResponse(responseCode = "400", description = "Invalid date format"),
        @ApiResponse(responseCode = "500", description = "Report generation failed")
    })
    public ResponseEntity<WeeklyTrendReport> getWeeklyTrend(
            @Parameter(description = "Start date of the week (defaults to beginning of current week)", example = "2024-12-23")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekOf) {
        try {
            LocalDate weekStart = weekOf != null ? weekOf : LocalDate.now().minusDays(LocalDate.now().getDayOfWeek().getValue() - 1);
            log.info("Generating weekly trend report for week starting: {}", weekStart);
            WeeklyTrendReport report = analyticsService.generateWeeklyTrend(weekStart);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            log.error("Failed to generate weekly trend report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/weekly/export")
    @Operation(
        summary = "Export Weekly Trend Report to CSV",
        description = "Download weekly trend report as a CSV file"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "CSV export successful"),
        @ApiResponse(responseCode = "500", description = "Export failed")
    })
    public ResponseEntity<byte[]> exportWeeklyTrendToCsv(
            @Parameter(description = "Start date of the week", example = "2024-12-23")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekOf) {
        try {
            LocalDate weekStart = weekOf != null ? weekOf : LocalDate.now().minusDays(LocalDate.now().getDayOfWeek().getValue() - 1);
            WeeklyTrendReport report = analyticsService.generateWeeklyTrend(weekStart);
            byte[] csvData = exportService.exportWeeklyTrendToCsv(report);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment",
                    "weekly-trend-" + weekStart + ".csv");

            return new ResponseEntity<>(csvData, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Failed to export weekly trend report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========================================================================
    // MERCHANT SCORECARD
    // ========================================================================

    @GetMapping("/merchants/scorecard/{merchId}")
    @Operation(
        summary = "Get Merchant Scorecard",
        description = "Generate a detailed scorecard for a specific merchant with health scores, trends, and recommendations"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Scorecard generated successfully",
                content = @Content(schema = @Schema(implementation = MerchantScorecard.class))),
        @ApiResponse(responseCode = "404", description = "Merchant not found"),
        @ApiResponse(responseCode = "500", description = "Scorecard generation failed")
    })
    public ResponseEntity<MerchantScorecard> getMerchantScorecard(
            @Parameter(description = "Merchant ID", example = "MERCH001")
            @PathVariable String merchId,
            @Parameter(description = "Hardware Serial Number (HSN)", example = "HSN12345")
            @RequestParam(required = false, defaultValue = "") String hsn,
            @Parameter(description = "Number of days to analyze (default: 30)", example = "30")
            @RequestParam(required = false, defaultValue = "30") int days) {
        try {
            log.info("Generating merchant scorecard for: {}:{} over {} days", merchId, hsn, days);
            MerchantScorecard scorecard = analyticsService.generateMerchantScorecard(merchId, hsn, days);
            return ResponseEntity.ok(scorecard);
        } catch (Exception e) {
            log.error("Failed to generate merchant scorecard", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/merchants/scorecard/{merchId}/export")
    @Operation(
        summary = "Export Merchant Scorecard to CSV",
        description = "Download merchant scorecard as a CSV file"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "CSV export successful"),
        @ApiResponse(responseCode = "500", description = "Export failed")
    })
    public ResponseEntity<byte[]> exportMerchantScorecardToCsv(
            @PathVariable String merchId,
            @RequestParam(required = false, defaultValue = "") String hsn,
            @RequestParam(required = false, defaultValue = "30") int days) {
        try {
            MerchantScorecard scorecard = analyticsService.generateMerchantScorecard(merchId, hsn, days);
            byte[] csvData = exportService.exportMerchantScorecardToCsv(scorecard);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment",
                    "merchant-scorecard-" + merchId + ".csv");

            return new ResponseEntity<>(csvData, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Failed to export merchant scorecard", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/merchants/scorecards")
    @Operation(
        summary = "Get All Merchant Scorecards",
        description = "Generate scorecards for all merchants, ranked by score"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Scorecards generated successfully"),
        @ApiResponse(responseCode = "500", description = "Scorecard generation failed")
    })
    public ResponseEntity<List<MerchantScorecard>> getAllMerchantScorecards(
            @Parameter(description = "Number of days to analyze (default: 30)", example = "30")
            @RequestParam(required = false, defaultValue = "30") int days) {
        try {
            log.info("Generating scorecards for all merchants over {} days", days);
            List<MerchantScorecard> scorecards = analyticsService.getAllMerchantScorecards(days);
            return ResponseEntity.ok(scorecards);
        } catch (Exception e) {
            log.error("Failed to generate all merchant scorecards", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/merchants/scorecards/export")
    @Operation(
        summary = "Export All Merchant Scorecards to CSV",
        description = "Download all merchant scorecards as a CSV file"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "CSV export successful"),
        @ApiResponse(responseCode = "500", description = "Export failed")
    })
    public ResponseEntity<byte[]> exportAllMerchantScorecardsToCsv(
            @RequestParam(required = false, defaultValue = "30") int days) {
        try {
            List<MerchantScorecard> scorecards = analyticsService.getAllMerchantScorecards(days);
            byte[] csvData = exportService.exportAllMerchantScorecardsToCsv(scorecards);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment",
                    "all-merchant-scorecards-" + LocalDate.now() + ".csv");

            return new ResponseEntity<>(csvData, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Failed to export all merchant scorecards", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/merchants/at-risk")
    @Operation(
        summary = "Get At-Risk Merchants",
        description = "Get list of merchants with HIGH or CRITICAL risk levels"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "At-risk merchants retrieved successfully"),
        @ApiResponse(responseCode = "500", description = "Failed to retrieve at-risk merchants")
    })
    public ResponseEntity<List<MerchantScorecard>> getAtRiskMerchants(
            @RequestParam(required = false, defaultValue = "30") int days) {
        try {
            log.info("Getting at-risk merchants over {} days", days);
            List<MerchantScorecard> allScorecards = analyticsService.getAllMerchantScorecards(days);
            List<MerchantScorecard> atRisk = allScorecards.stream()
                    .filter(s -> s.getRiskLevel() == MerchantScorecard.RiskLevel.HIGH
                            || s.getRiskLevel() == MerchantScorecard.RiskLevel.CRITICAL)
                    .toList();
            return ResponseEntity.ok(atRisk);
        } catch (Exception e) {
            log.error("Failed to get at-risk merchants", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========================================================================
    // PREDICTION ACCURACY REPORT
    // ========================================================================

    @GetMapping("/accuracy")
    @Operation(
        summary = "Get Prediction Accuracy Report",
        description = "Generate a report on AI prediction accuracy with insights and recommendations"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Report generated successfully",
                content = @Content(schema = @Schema(implementation = PredictionAccuracyReport.class))),
        @ApiResponse(responseCode = "500", description = "Report generation failed")
    })
    public ResponseEntity<PredictionAccuracyReport> getPredictionAccuracyReport(
            @Parameter(description = "Number of days to analyze (default: 30)", example = "30")
            @RequestParam(required = false, defaultValue = "30") int days) {
        try {
            log.info("Generating prediction accuracy report for last {} days", days);
            PredictionAccuracyReport report = analyticsService.generatePredictionAccuracyReport(days);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            log.error("Failed to generate prediction accuracy report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/accuracy/export")
    @Operation(
        summary = "Export Prediction Accuracy Report to CSV",
        description = "Download prediction accuracy report as a CSV file"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "CSV export successful"),
        @ApiResponse(responseCode = "500", description = "Export failed")
    })
    public ResponseEntity<byte[]> exportPredictionAccuracyToCsv(
            @RequestParam(required = false, defaultValue = "30") int days) {
        try {
            PredictionAccuracyReport report = analyticsService.generatePredictionAccuracyReport(days);
            byte[] csvData = exportService.exportPredictionAccuracyToCsv(report);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment",
                    "prediction-accuracy-" + LocalDate.now() + ".csv");

            return new ResponseEntity<>(csvData, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Failed to export prediction accuracy report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========================================================================
    // SUMMARY DASHBOARD
    // ========================================================================

    @GetMapping("/dashboard")
    @Operation(
        summary = "Get Dashboard Summary",
        description = "Get a quick summary of key metrics for dashboard display"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Dashboard data retrieved successfully"),
        @ApiResponse(responseCode = "500", description = "Failed to retrieve dashboard data")
    })
    public ResponseEntity<DashboardSummary> getDashboardSummary() {
        try {
            log.info("Generating dashboard summary");

            DailySummaryReport todayReport = analyticsService.generateDailySummary(LocalDate.now());
            PredictionAccuracyReport accuracyReport = analyticsService.generatePredictionAccuracyReport(7);
            List<MerchantScorecard> atRiskMerchants = analyticsService.getAllMerchantScorecards(7).stream()
                    .filter(s -> s.getRiskLevel() == MerchantScorecard.RiskLevel.HIGH
                            || s.getRiskLevel() == MerchantScorecard.RiskLevel.CRITICAL)
                    .limit(5)
                    .toList();

            DashboardSummary dashboard = DashboardSummary.builder()
                    .todaysBatches(todayReport.getTotalBatches())
                    .todaysTransactions(todayReport.getTotalTransactionsProcessed())
                    .todaysErrors(todayReport.getTotalErrors())
                    .todaysErrorRate(todayReport.getErrorRate())
                    .currentHealthStatus(todayReport.getOverallHealthStatus())
                    .predictionAccuracy(accuracyReport.getOverallAccuracy())
                    .totalPredictionsToday(todayReport.getTotalPredictions())
                    .atRiskMerchantCount(atRiskMerchants.size())
                    .criticalAlertsCount(todayReport.getCriticalAlerts() != null ? todayReport.getCriticalAlerts().size() : 0)
                    .warningsCount(todayReport.getWarnings() != null ? todayReport.getWarnings().size() : 0)
                    .topAtRiskMerchants(atRiskMerchants.stream()
                            .map(m -> m.getMerchId() + " (" + m.getHealthGrade() + ")")
                            .toList())
                    .build();

            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            log.error("Failed to generate dashboard summary", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @Schema(description = "Dashboard summary for quick overview")
    public static class DashboardSummary {
        private long todaysBatches;
        private long todaysTransactions;
        private long todaysErrors;
        private double todaysErrorRate;
        private com.cardconnect.bolt.ai.model.HealthStatus currentHealthStatus;
        private Double predictionAccuracy;
        private int totalPredictionsToday;
        private int atRiskMerchantCount;
        private int criticalAlertsCount;
        private int warningsCount;
        private List<String> topAtRiskMerchants;
    }
}

