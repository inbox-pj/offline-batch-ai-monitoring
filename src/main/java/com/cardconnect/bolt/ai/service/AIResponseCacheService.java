package com.cardconnect.bolt.ai.service;

import com.cardconnect.bolt.ai.model.AIPredictionResult;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.function.Supplier;

/**
 * Caching service for AI responses to reduce costs and improve performance
 */
@Service
@Slf4j
public class AIResponseCacheService {

    private final Cache<String, AIPredictionResult> predictionCache;

    @Value("${ai.prediction.cache.ttl.minutes:5}")
    private int cacheTtlMinutes;

    public AIResponseCacheService() {
        this.predictionCache = Caffeine.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(Duration.ofMinutes(5))
            .recordStats()
            .build();
    }

    /**
     * Get cached prediction or execute analysis function
     */
    public AIPredictionResult getCachedOrAnalyze(Supplier<AIPredictionResult> analysisFunction) {
        String cacheKey = generateCacheKey();

        AIPredictionResult cached = predictionCache.getIfPresent(cacheKey);
        if (cached != null) {
            log.debug("Returning cached AI prediction");
            return cached;
        }

        AIPredictionResult result = analysisFunction.get();
        predictionCache.put(cacheKey, result);

        return result;
    }

    /**
     * Invalidate cache (e.g., when significant changes detected)
     */
    public void invalidateCache() {
        predictionCache.invalidateAll();
        log.info("AI prediction cache invalidated");
    }

    /**
     * Generate cache key based on current hour
     */
    private String generateCacheKey() {
        LocalDateTime now = LocalDateTime.now();
        return String.format("prediction_%s_%d",
            now.toLocalDate(),
            now.getHour());
    }

    /**
     * Log cache statistics periodically
     */
    @Scheduled(fixedRate = 60000) // Every minute
    public void logCacheStats() {
        CacheStats stats = predictionCache.stats();
        if (stats.requestCount() > 0) {
            log.debug("AI Cache Stats - Hit Rate: {:.2f}%, Requests: {}, Hits: {}, Misses: {}",
                stats.hitRate() * 100,
                stats.requestCount(),
                stats.hitCount(),
                stats.missCount());
        }
    }
}

