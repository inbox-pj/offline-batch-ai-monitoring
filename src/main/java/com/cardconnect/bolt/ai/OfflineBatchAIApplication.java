package com.cardconnect.bolt.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Application class for Offline Batch AI Monitoring System
 *
 * This application provides AI-powered predictive monitoring for offline batch processing
 * using Spring AI for intelligent trend analysis and health predictions.
 *
 * Features:
 * - AI-powered trend analysis and predictions
 * - Hybrid approach with rule-based fallback
 * - Real-time health monitoring
 * - Predictive alerts (6-hour forecast)
 * - Comprehensive metrics and audit logging
 *
 * @author CardConnect Development Team
 * @version 1.0.0
 * @since 2024-12-03
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableCaching
public class OfflineBatchAIApplication {

    public static void main(String[] args) {
        SpringApplication.run(OfflineBatchAIApplication.class, args);
    }
}

