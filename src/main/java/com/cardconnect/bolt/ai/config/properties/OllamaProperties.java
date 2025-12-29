package com.cardconnect.bolt.ai.config.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for Ollama AI service
 * All values are externalized - NO hardcoded defaults
 * Configure via application.properties with environment variable support
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "spring.ai.ollama")
public class OllamaProperties {

    /**
     * Base URL for Ollama service
     * Configure via: OLLAMA_BASE_URL
     */
    @NotBlank(message = "Ollama base URL is required. Set via spring.ai.ollama.base-url or OLLAMA_BASE_URL")
    private String baseUrl;

    /**
     * Chat model configuration
     */
    @NotNull(message = "Chat configuration is required")
    private ChatConfig chat;

    /**
     * Embedding model name
     * Configure via: OLLAMA_EMBEDDING_MODEL
     */
    @NotBlank(message = "Embedding model name is required. Set via spring.ai.ollama.embedding.model or OLLAMA_EMBEDDING_MODEL")
    private String embeddingModel;

    /**
     * Request timeout in seconds
     * Configure via: OLLAMA_TIMEOUT
     */
    @NotNull(message = "Timeout is required")
    @Min(value = 10, message = "Timeout must be at least 10 seconds")
    @Max(value = 300, message = "Timeout cannot exceed 300 seconds")
    private Integer timeout;

    /**
     * Connection timeout in seconds
     * Configure via: OLLAMA_CONNECTION_TIMEOUT
     */
    @NotNull(message = "Connection timeout is required")
    @Min(value = 5, message = "Connection timeout must be at least 5 seconds")
    @Max(value = 60, message = "Connection timeout cannot exceed 60 seconds")
    private Integer connectionTimeout;

    /**
     * Maximum retry attempts for failed requests
     * Configure via: OLLAMA_MAX_RETRIES
     */
    @NotNull(message = "Max retries is required")
    @Min(0)
    @Max(5)
    private Integer maxRetries;

    @Data
    public static class ChatConfig {
        /**
         * Chat model name (e.g., llama3.2, llama2, mistral)
         * Configure via: OLLAMA_CHAT_MODEL
         */
        @NotBlank(message = "Chat model name is required. Set via spring.ai.ollama.chat.model or OLLAMA_CHAT_MODEL")
        private String model;

        /**
         * Temperature for response generation (0.0 to 1.0)
         * Lower = more deterministic, Higher = more creative
         * Configure via: OLLAMA_CHAT_TEMPERATURE
         */
        @NotNull(message = "Temperature is required")
        @Min(value = 0, message = "Temperature must be between 0 and 1")
        @Max(value = 1, message = "Temperature must be between 0 and 1")
        private Double temperature;

        /**
         * Maximum tokens in response
         * Configure via: OLLAMA_CHAT_MAX_TOKENS
         */
        @NotNull(message = "Max tokens is required")
        @Min(100)
        @Max(32000)
        private Integer maxTokens;

        /**
         * Top-p sampling parameter
         * Configure via: OLLAMA_CHAT_TOP_P
         */
        @NotNull(message = "Top-p is required")
        @Min(0)
        @Max(1)
        private Double topP;
    }
}

