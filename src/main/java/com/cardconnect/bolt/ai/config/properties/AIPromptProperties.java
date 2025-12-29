package com.cardconnect.bolt.ai.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for AI prompts
 * All prompts are externalized - NO hardcoded defaults
 * Configure via application.properties or external prompt files
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "ai.prompt")
public class AIPromptProperties {

    /**
     * Prompt version for tracking changes
     * Configure via: AI_PROMPT_VERSION
     */
    @NotBlank(message = "Prompt version is required")
    private String version;

    /**
     * Maximum prompt length
     * Configure via: AI_PROMPT_MAX_LENGTH
     */
    @NotNull(message = "Max prompt length is required")
    private Integer maxLength;

    /**
     * System prompt for batch analysis
     * Configure via: AI_SYSTEM_PROMPT or file
     */
    @NotBlank(message = "System prompt is required. Set via ai.prompt.system-prompt or AI_SYSTEM_PROMPT")
    private String systemPrompt;

    /**
     * User prompt template for batch analysis
     * Configure via: AI_USER_PROMPT_TEMPLATE
     */
    @NotBlank(message = "User prompt template is required")
    private String userPromptTemplate;

    /**
     * Prompt for chat interactions
     * Configure via: AI_CHAT_SYSTEM_PROMPT
     */
    @NotBlank(message = "Chat system prompt is required")
    private String chatSystemPrompt;
}

