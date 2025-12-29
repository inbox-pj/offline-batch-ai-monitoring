package com.cardconnect.bolt.ai.controller;

import com.cardconnect.bolt.ai.model.AIPredictionResult;
import com.cardconnect.bolt.ai.service.AIUsageManager;
import com.cardconnect.bolt.ai.service.OfflineBatchAIAssistantService;
import com.cardconnect.bolt.ai.service.OfflineBatchHybridAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * REST API controller for AI-powered offline batch monitoring
 */
@RestController
@RequestMapping("/api/ai")
@Slf4j
@Tag(name = "AI Monitoring", description = "AI-powered offline batch health monitoring and prediction endpoints")
public class OfflineBatchAIController {

    private final OfflineBatchHybridAnalysisService hybridAnalysisService;
    private final Optional<OfflineBatchAIAssistantService> aiAssistantService;
    private final AIUsageManager usageManager;

    @Autowired
    public OfflineBatchAIController(
            OfflineBatchHybridAnalysisService hybridAnalysisService,
            @Autowired(required = false) OfflineBatchAIAssistantService aiAssistantService,
            AIUsageManager usageManager) {
        this.hybridAnalysisService = hybridAnalysisService;
        this.aiAssistantService = Optional.ofNullable(aiAssistantService);
        this.usageManager = usageManager;
    }

    /**
     * GET /api/ai/predict
     * Get AI prediction for offline batch health
     */
    @GetMapping("/predict")
    @Operation(
        summary = "Get AI Health Prediction",
        description = "Analyzes offline batch metrics and predicts potential health issues in the next 6 hours"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Prediction successful",
                content = @Content(schema = @Schema(implementation = AIPredictionResult.class))),
        @ApiResponse(responseCode = "500", description = "Prediction failed")
    })
    public ResponseEntity<AIPredictionResult> getPrediction() {
        try {
            log.info("Received AI prediction request");
            AIPredictionResult result = hybridAnalysisService.analyzeTrendsAndPredict();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("AI prediction request failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(AIPredictionResult.error("Prediction failed: " + e.getMessage()));
        }
    }

    /**
     * POST /api/ai/chat
     * Chat with AI assistant
     */
    @PostMapping("/chat")
    @Operation(
        summary = "Chat with AI Assistant",
        description = "Send a message to the AI assistant for offline batch monitoring insights"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Chat response received",
                content = @Content(schema = @Schema(implementation = ChatResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "500", description = "Chat processing failed")
    })
    public ResponseEntity<ChatResponse> chatWithAssistant(
            @jakarta.validation.Valid @RequestBody ChatRequest request) {
        try {
            if (aiAssistantService.isEmpty()) {
                log.warn("Chat request received but AI assistant is not enabled");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ChatResponse("AI chat is not available. Enable AI prediction to use this feature."));
            }
            log.info("Received chat request: {}", request.getMessage());
            String response = aiAssistantService.get().chat(request.getMessage());
            return ResponseEntity.ok(new ChatResponse(response));
        } catch (Exception e) {
            log.error("Chat request failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ChatResponse("Error processing chat request"));
        }
    }

    /**
     * GET /api/ai/usage
     * Get AI usage statistics
     */
    @GetMapping("/usage")
    @Operation(
        summary = "Get AI Usage Statistics",
        description = "Returns current AI usage metrics including request count and cost"
    )
    @ApiResponse(responseCode = "200", description = "Usage statistics retrieved successfully")
    public ResponseEntity<Map<String, Object>> getUsageStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("dailyRequests", usageManager.getDailyRequestCount());
        stats.put("dailyCostCents", usageManager.getDailyCostCents());
        stats.put("remainingRequests", usageManager.getRemainingRequests());
        stats.put("remainingBudgetCents", usageManager.getRemainingBudgetCents());
        return ResponseEntity.ok(stats);
    }

    /**
     * GET /api/ai/health
     * Health check for AI services
     */
    @GetMapping("/health")
    @Operation(
        summary = "AI Service Health Check",
        description = "Returns the health status of AI services"
    )
    @ApiResponse(responseCode = "200", description = "Health status retrieved successfully")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        health.put("aiEnabled", String.valueOf(usageManager.canMakeRequest()));
        return ResponseEntity.ok(health);
    }

    @Data
    @Schema(description = "Chat request payload")
    public static class ChatRequest {
        @Schema(description = "User message to AI assistant", example = "What is the current batch health?")
        @jakarta.validation.constraints.NotBlank(message = "Message cannot be blank")
        @jakarta.validation.constraints.Size(max = 5000, message = "Message too long (max 5000 characters)")
        private String message;
    }

    @Data
    @Schema(description = "Chat response from AI assistant")
    public static class ChatResponse {
        @Schema(description = "AI assistant's response")
        private final String response;
    }
}

