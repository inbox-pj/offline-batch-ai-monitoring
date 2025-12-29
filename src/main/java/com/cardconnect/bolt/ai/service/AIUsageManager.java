package com.cardconnect.bolt.ai.service;

import com.cardconnect.bolt.ai.config.properties.AIPredictionProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages AI usage limits and cost tracking
 * All limits are configurable via properties
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AIUsageManager {

    private final AtomicInteger dailyRequestCount = new AtomicInteger(0);
    private final AtomicLong dailyCostCents = new AtomicLong(0);

    private final AIPredictionProperties predictionProperties;

    @Scheduled(cron = "${ai.usage.reset-cron:0 0 0 * * *}") // Every day at midnight (configurable)
    public void resetDailyCounters() {
        int requests = dailyRequestCount.getAndSet(0);
        long cost = dailyCostCents.getAndSet(0);

        log.info("Daily AI usage counters reset. Previous: requests={}, cost={}¢", requests, cost);
    }

    public boolean canMakeRequest() {
        int maxDailyRequests = predictionProperties.getMaxDailyRequests();
        long maxDailyCostCents = predictionProperties.getMaxDailyCostCents();

        boolean withinRequestLimit = dailyRequestCount.get() < maxDailyRequests;
        boolean withinCostLimit = dailyCostCents.get() < maxDailyCostCents;

        if (!withinRequestLimit) {
            log.warn("Daily request limit reached: {}/{}", dailyRequestCount.get(), maxDailyRequests);
        }

        if (!withinCostLimit) {
            log.warn("Daily cost limit reached: {}¢/{}¢", dailyCostCents.get(), maxDailyCostCents);
        }

        return withinRequestLimit && withinCostLimit;
    }

    public void recordRequest(int tokenCount) {
        int maxDailyRequests = predictionProperties.getMaxDailyRequests();
        long maxDailyCostCents = predictionProperties.getMaxDailyCostCents();

        int requests = dailyRequestCount.incrementAndGet();

        // Estimate cost: ~$0.01 per 1K tokens for GPT-4 equivalent
        long costCents = ((long) tokenCount) / 100;
        long totalCost = dailyCostCents.addAndGet(costCents);

        log.debug("AI request recorded: tokens={}, cost={}¢, daily total: requests={}, cost={}¢",
            tokenCount, costCents, requests, totalCost);

        // Warn if approaching limits (90% threshold)
        if (requests > maxDailyRequests * 0.9) {
            log.warn("Approaching daily request limit: {}/{}", requests, maxDailyRequests);
        }

        if (totalCost > maxDailyCostCents * 0.9) {
            log.warn("Approaching daily cost limit: {}¢/{}¢", totalCost, maxDailyCostCents);
        }
    }

    public int getDailyRequestCount() {
        return dailyRequestCount.get();
    }

    public long getDailyCostCents() {
        return dailyCostCents.get();
    }

    public int getRemainingRequests() {
        return Math.max(0, predictionProperties.getMaxDailyRequests() - dailyRequestCount.get());
    }

    public long getRemainingBudgetCents() {
        return Math.max(0, predictionProperties.getMaxDailyCostCents() - dailyCostCents.get());
    }
}

