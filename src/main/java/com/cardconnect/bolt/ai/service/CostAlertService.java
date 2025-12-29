package com.cardconnect.bolt.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Cost Alert Service
 * Monitors AI usage and sends alerts when approaching budget limits
 */
@Service
@Slf4j
public class CostAlertService {

    private final AIUsageManager usageManager;

    @Value("${ai.prediction.max.daily.cost.cents:10000}")
    private long maxDailyCostCents;

    @Value("${ai.prediction.max.daily.requests:1000}")
    private int maxDailyRequests;

    public CostAlertService(AIUsageManager usageManager) {
        this.usageManager = usageManager;
    }

    /**
     * Check cost thresholds every hour
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    public void checkCostThresholds() {
        checkCostThreshold();
        checkRequestThreshold();
    }

    private void checkCostThreshold() {
        long currentCost = usageManager.getDailyCostCents();
        double usagePercentage = (double) currentCost / maxDailyCostCents * 100;

        if (usagePercentage >= 90) {
            log.error("🚨 CRITICAL: AI cost at {:.1f}% ({}/{}¢) - IMMEDIATE ACTION REQUIRED",
                usagePercentage, currentCost, maxDailyCostCents);
            sendCriticalAlert(currentCost, maxDailyCostCents, usagePercentage);
        } else if (usagePercentage >= 80) {
            log.warn("⚠️  WARNING: AI cost at {:.1f}% ({}/{}¢) - Approaching limit",
                usagePercentage, currentCost, maxDailyCostCents);
            sendWarningAlert(currentCost, maxDailyCostCents, usagePercentage);
        } else if (usagePercentage >= 50 && usagePercentage % 10 == 0) {
            log.info("💰 AI cost at {:.1f}% ({}/{}¢)",
                usagePercentage, currentCost, maxDailyCostCents);
        }
    }

    private void checkRequestThreshold() {
        int currentRequests = usageManager.getDailyRequestCount();
        double usagePercentage = (double) currentRequests / maxDailyRequests * 100;

        if (usagePercentage >= 90) {
            log.error("🚨 CRITICAL: AI requests at {:.1f}% ({}/{}) - IMMEDIATE ACTION REQUIRED",
                usagePercentage, currentRequests, maxDailyRequests);
        } else if (usagePercentage >= 80) {
            log.warn("⚠️  WARNING: AI requests at {:.1f}% ({}/{}) - Approaching limit",
                usagePercentage, currentRequests, maxDailyRequests);
        }
    }

    private void sendCriticalAlert(long currentCost, long maxCost, double percentage) {
        log.error("╔═══════════════════════════════════════════════════════════╗");
        log.error("║  🚨 CRITICAL: AI COST ALERT                                ║");
        log.error("╠═══════════════════════════════════════════════════════════╣");
        log.error("  Current Cost:     {}¢", currentCost);
        log.error("  Maximum Budget:   {}¢", maxCost);
        log.error("  Usage:            {:.1f}%", percentage);
        log.error("  Remaining Budget: {}¢", maxCost - currentCost);
        log.error("  ");
        log.error("  ACTIONS REQUIRED:");
        log.error("  1. Review AI usage patterns");
        log.error("  2. Consider increasing budget");
        log.error("  3. Optimize token usage");
        log.error("  4. Enable more aggressive caching");
        log.error("╚═══════════════════════════════════════════════════════════╝");

        // TODO: Send email/Slack notification
        // notificationService.sendCriticalAlert(...);
    }

    private void sendWarningAlert(long currentCost, long maxCost, double percentage) {
        log.warn("╔═══════════════════════════════════════════════════════════╗");
        log.warn("║  ⚠️  WARNING: AI COST ALERT                                ║");
        log.warn("╠═══════════════════════════════════════════════════════════╣");
        log.warn("  Current Cost:     {}¢", currentCost);
        log.warn("  Maximum Budget:   {}¢", maxCost);
        log.warn("  Usage:            {:.1f}%", percentage);
        log.warn("  Remaining Budget: {}¢", maxCost - currentCost);
        log.warn("  ");
        log.warn("  RECOMMENDATIONS:");
        log.warn("  - Monitor usage closely");
        log.warn("  - Review optimization opportunities");
        log.warn("╚═══════════════════════════════════════════════════════════╝");

        // TODO: Send email/Slack notification
        // notificationService.sendWarningAlert(...);
    }
}

