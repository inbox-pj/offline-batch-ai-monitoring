package com.cardconnect.bolt.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI Assistant service using Spring AI ChatClient for conversational AI
 * Only enabled when AI prediction is enabled
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "ai.prediction.enabled", havingValue = "true")
public class OfflineBatchAIAssistantService {

    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT = """
        You are an expert offline batch processing monitoring assistant.
        Your role is to help operations teams understand batch processing health,
        predict potential issues, and provide actionable recommendations.
        
        Always:
        - Be specific and actionable
        - Prioritize critical issues
        - Reference historical patterns when available
        - Explain technical concepts clearly
        - Provide confidence levels for predictions
        """;

    public OfflineBatchAIAssistantService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * Chat with AI assistant
     */
    public String chat(String message) {
        try {
            Prompt prompt = new Prompt(List.of(
                new SystemMessage(SYSTEM_PROMPT),
                new UserMessage(message)
            ));

            return chatClient.prompt(prompt)
                .call()
                .content();
        } catch (Exception e) {
            log.error("AI assistant chat failed", e);
            return "I apologize, but I encountered an error processing your request. Please try again.";
        }
    }

    /**
     * Analyze batch health with metrics context
     */
    public String analyzeBatchHealth(String metrics, int timeHorizon) {
        try {
            String userMessage = String.format("""
                Analyze these offline batch metrics and predict potential issues:
                
                %s
                
                Provide:
                1. Current health assessment
                2. Predicted status in next %d hours
                3. Key risk factors
                4. Specific recommendations
                """, metrics, timeHorizon);

            Prompt prompt = new Prompt(List.of(
                new SystemMessage(SYSTEM_PROMPT),
                new UserMessage(userMessage)
            ));

            return chatClient.prompt(prompt)
                .call()
                .content();
        } catch (Exception e) {
            log.error("AI batch health analysis failed", e);
            return "Unable to analyze batch health at this time. Please try again later.";
        }
    }
}
