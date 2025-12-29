package com.cardconnect.bolt.ai.config;

import com.cardconnect.bolt.ai.config.properties.OllamaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for AI services using Spring AI with Ollama
 * Consolidated to use Spring AI only
 * All values are externalized - no hardcoded configuration
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class AIConfiguration {

    private final OllamaProperties ollamaProperties;

    /**
     * Spring AI Chat Client Builder
     * Uses auto-configured OllamaChatModel from spring-ai-starter-model-ollama
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(ChatModel.class)
    public ChatClient.Builder chatClientBuilder(ChatModel chatModel) {
        log.info("Creating ChatClient.Builder with Spring AI Ollama ChatModel: model={}",
                ollamaProperties.getChat().getModel());
        return ChatClient.builder(chatModel);
    }
}
