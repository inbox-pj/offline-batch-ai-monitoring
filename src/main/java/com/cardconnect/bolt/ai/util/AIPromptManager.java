package com.cardconnect.bolt.ai.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages AI prompts with versioning support
 */
@Component
@Slf4j
public class AIPromptManager {

    @Value("${ai.prompt.version:v2}")
    private String promptVersion;

    private final Map<String, String> promptTemplates = new HashMap<>();

    @PostConstruct
    public void loadPrompts() {
        // Version 1: Basic analysis
        promptTemplates.put("v1", """
            Analyze offline batch metrics and predict issues.
            Metrics: {{metrics}}
            Provide JSON response with status, confidence, findings.
            """);

        // Version 2: Enhanced with historical context
        promptTemplates.put("v2", """
            You are an expert analyzing offline batch processing.
            
            Current Metrics:
            {{metrics}}
            
            Historical Context:
            {{history}}
            
            Analyze trends, compare with history, predict issues in next 6 hours.
            Focus on: error patterns, processing time anomalies, merchant issues.
            
            Response format: JSON with detailed analysis including:
            - predictedStatus
            - confidence
            - keyFindings
            - trendAnalysis
            - riskFactors
            - recommendations
            - reasoning
            """);

        // Version 3: Chain-of-thought prompting
        promptTemplates.put("v3", """
            Analyze step-by-step:
            
            Step 1: Review current metrics
            {{metrics}}
            
            Step 2: Compare with historical patterns
            {{history}}
            
            Step 3: Identify anomalies and trends
            
            Step 4: Assess risk level
            
            Step 5: Generate predictions and recommendations
            
            Provide detailed reasoning for each step in JSON format.
            """);

        log.info("Loaded {} prompt templates. Active version: {}", promptTemplates.size(), promptVersion);
    }

    public String getPrompt(Map<String, String> variables) {
        String template = promptTemplates.getOrDefault(promptVersion, promptTemplates.get("v1"));
        return renderTemplate(template, variables);
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    private String renderTemplate(String template, Map<String, String> variables) {
        String rendered = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return rendered;
    }
}

