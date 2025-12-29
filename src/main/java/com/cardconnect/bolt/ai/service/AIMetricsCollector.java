package com.cardconnect.bolt.ai.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Collects and records metrics for AI predictions
 */
@Service
@Slf4j
public class AIMetricsCollector {

    private final MeterRegistry meterRegistry;
    private final Counter aiRequestCounter;
    private final Counter aiErrorCounter;
    private final Timer aiResponseTimer;
    private final DistributionSummary aiConfidenceDistribution;
    private final DistributionSummary aiTokenUsage;

    public AIMetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.aiRequestCounter = Counter.builder("ai.predictions.requests")
            .description("Total AI prediction requests")
            .tag("component", "offline-batch")
            .register(meterRegistry);

        this.aiErrorCounter = Counter.builder("ai.predictions.errors")
            .description("AI prediction errors")
            .tag("component", "offline-batch")
            .register(meterRegistry);

        this.aiResponseTimer = Timer.builder("ai.predictions.response.time")
            .description("AI prediction response time")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry);

        this.aiConfidenceDistribution = DistributionSummary.builder("ai.predictions.confidence")
            .description("AI prediction confidence scores")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry);

        this.aiTokenUsage = DistributionSummary.builder("ai.token.usage")
            .description("AI token usage per request")
            .baseUnit("tokens")
            .register(meterRegistry);
    }

    public void recordPrediction(String predictedStatus, double confidence, long durationMs, int tokenCount) {
        aiRequestCounter.increment();
        aiResponseTimer.record(durationMs, TimeUnit.MILLISECONDS);
        aiConfidenceDistribution.record(confidence);
        aiTokenUsage.record(tokenCount);

        // Track by predicted status
        meterRegistry.counter("ai.predictions.by.status",
            "status", predictedStatus).increment();

        log.debug("Recorded AI prediction metrics: status={}, confidence={:.2f}, duration={}ms, tokens={}",
            predictedStatus, confidence, durationMs, tokenCount);
    }

    public void recordError(Exception e) {
        aiErrorCounter.increment();
        meterRegistry.counter("ai.predictions.errors.by.type",
            "type", e.getClass().getSimpleName()).increment();

        log.warn("Recorded AI prediction error: {}", e.getMessage());
    }
}

