package com.cardconnect.bolt.ai.service;

import com.cardconnect.bolt.ai.model.HealthStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.annotation.Observed;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for recording AI-specific metrics for Prometheus/Grafana monitoring.
 * Tracks:
 * - AI prediction requests, latency, and success rates
 * - Token usage and costs
 * - Model performance metrics
 * - Circuit breaker states
 * - Cache hit/miss rates
 */
@Service
@Slf4j
public class AIMetricsService {

    private final MeterRegistry meterRegistry;

    @Value("${ai.metrics.prefix:ai}")
    private String metricsPrefix;

    @Value("${ai.metrics.enabled:true}")
    private boolean metricsEnabled;

    // Counters
    private Counter predictionRequestsTotal;
    private Counter predictionSuccessTotal;
    private Counter predictionFailureTotal;
    private Counter tokensUsedTotal;
    private Counter cacheHitsTotal;
    private Counter cacheMissesTotal;
    private Counter fallbackInvocationsTotal;
    private Counter circuitBreakerOpenTotal;

    // Timers
    private Timer predictionLatency;
    private Timer embeddingLatency;
    private Timer ragRetrievalLatency;

    // Gauges (tracked via AtomicLong)
    private final AtomicLong activePredictions = new AtomicLong(0);
    private final AtomicLong dailyRequestCount = new AtomicLong(0);
    private final AtomicLong dailyCostCents = new AtomicLong(0);
    private final ConcurrentHashMap<String, AtomicLong> predictionsByStatus = new ConcurrentHashMap<>();

    // Distribution Summaries
    private DistributionSummary confidenceDistribution;
    private DistributionSummary tokenUsageDistribution;

    public AIMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void initializeMetrics() {
        if (!metricsEnabled) {
            log.info("AI Metrics disabled");
            return;
        }

        log.info("Initializing AI Metrics with prefix: {}", metricsPrefix);

        // Initialize counters
        predictionRequestsTotal = Counter.builder(metricsPrefix + ".predictions.requests.total")
                .description("Total number of AI prediction requests")
                .tag("type", "prediction")
                .register(meterRegistry);

        predictionSuccessTotal = Counter.builder(metricsPrefix + ".predictions.success.total")
                .description("Total number of successful AI predictions")
                .register(meterRegistry);

        predictionFailureTotal = Counter.builder(metricsPrefix + ".predictions.failure.total")
                .description("Total number of failed AI predictions")
                .register(meterRegistry);

        tokensUsedTotal = Counter.builder(metricsPrefix + ".tokens.used.total")
                .description("Total tokens consumed by AI requests")
                .register(meterRegistry);

        cacheHitsTotal = Counter.builder(metricsPrefix + ".cache.hits.total")
                .description("Total cache hits for AI predictions")
                .register(meterRegistry);

        cacheMissesTotal = Counter.builder(metricsPrefix + ".cache.misses.total")
                .description("Total cache misses for AI predictions")
                .register(meterRegistry);

        fallbackInvocationsTotal = Counter.builder(metricsPrefix + ".fallback.invocations.total")
                .description("Total fallback method invocations")
                .register(meterRegistry);

        circuitBreakerOpenTotal = Counter.builder(metricsPrefix + ".circuit.breaker.open.total")
                .description("Total times circuit breaker opened")
                .register(meterRegistry);

        // Initialize timers
        predictionLatency = Timer.builder(metricsPrefix + ".prediction.latency")
                .description("AI prediction request latency")
                .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                .publishPercentileHistogram()
                .serviceLevelObjectives(
                        Duration.ofMillis(500),
                        Duration.ofSeconds(1),
                        Duration.ofSeconds(5),
                        Duration.ofSeconds(10)
                )
                .register(meterRegistry);

        embeddingLatency = Timer.builder(metricsPrefix + ".embedding.latency")
                .description("Embedding generation latency")
                .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                .register(meterRegistry);

        ragRetrievalLatency = Timer.builder(metricsPrefix + ".rag.retrieval.latency")
                .description("RAG retrieval latency")
                .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                .register(meterRegistry);

        // Initialize gauges
        Gauge.builder(metricsPrefix + ".predictions.active", activePredictions, AtomicLong::get)
                .description("Number of active AI predictions")
                .register(meterRegistry);

        Gauge.builder(metricsPrefix + ".requests.daily", dailyRequestCount, AtomicLong::get)
                .description("Daily AI request count")
                .register(meterRegistry);

        Gauge.builder(metricsPrefix + ".cost.daily.cents", dailyCostCents, AtomicLong::get)
                .description("Daily AI cost in cents")
                .register(meterRegistry);

        // Initialize distribution summaries
        confidenceDistribution = DistributionSummary.builder(metricsPrefix + ".prediction.confidence")
                .description("Distribution of prediction confidence scores")
                .publishPercentiles(0.5, 0.9, 0.95)
                .scale(100) // Convert to percentage
                .register(meterRegistry);

        tokenUsageDistribution = DistributionSummary.builder(metricsPrefix + ".tokens.per.request")
                .description("Token usage per request")
                .publishPercentiles(0.5, 0.9, 0.95)
                .register(meterRegistry);

        // Initialize status gauges
        for (HealthStatus status : HealthStatus.values()) {
            AtomicLong statusCount = new AtomicLong(0);
            predictionsByStatus.put(status.name(), statusCount);
            Gauge.builder(metricsPrefix + ".predictions.by.status", statusCount, AtomicLong::get)
                    .tag("status", status.name())
                    .description("Predictions by health status")
                    .register(meterRegistry);
        }

        log.info("AI Metrics initialized successfully");
    }

    // ============= Recording Methods =============

    /**
     * Record start of a prediction request
     */
    public Timer.Sample startPredictionTimer() {
        activePredictions.incrementAndGet();
        predictionRequestsTotal.increment();
        dailyRequestCount.incrementAndGet();
        return Timer.start(meterRegistry);
    }

    /**
     * Record successful prediction completion
     */
    @Observed(name = "ai.prediction.record", contextualName = "record-prediction-success")
    public void recordPredictionSuccess(Timer.Sample sample, HealthStatus status, double confidence) {
        sample.stop(predictionLatency);
        activePredictions.decrementAndGet();
        predictionSuccessTotal.increment();

        if (status != null) {
            AtomicLong statusCount = predictionsByStatus.get(status.name());
            if (statusCount != null) {
                statusCount.incrementAndGet();
            }
        }

        confidenceDistribution.record(confidence);

        log.debug("Recorded successful prediction: status={}, confidence={}", status, confidence);
    }

    /**
     * Record failed prediction
     */
    public void recordPredictionFailure(Timer.Sample sample, String reason) {
        sample.stop(predictionLatency);
        activePredictions.decrementAndGet();
        predictionFailureTotal.increment();

        Counter.builder(metricsPrefix + ".prediction.failure.reason")
                .tag("reason", sanitizeTag(reason))
                .register(meterRegistry)
                .increment();

        log.debug("Recorded prediction failure: reason={}", reason);
    }

    /**
     * Record token usage
     */
    public void recordTokenUsage(int promptTokens, int completionTokens, String model) {
        int totalTokens = promptTokens + completionTokens;
        tokensUsedTotal.increment(totalTokens);
        tokenUsageDistribution.record(totalTokens);

        Counter.builder(metricsPrefix + ".tokens.prompt")
                .tag("model", sanitizeTag(model))
                .register(meterRegistry)
                .increment(promptTokens);

        Counter.builder(metricsPrefix + ".tokens.completion")
                .tag("model", sanitizeTag(model))
                .register(meterRegistry)
                .increment(completionTokens);

        log.debug("Recorded token usage: prompt={}, completion={}, model={}",
                promptTokens, completionTokens, model);
    }

    /**
     * Record AI cost
     */
    public void recordCost(double costCents, String model) {
        dailyCostCents.addAndGet((long) costCents);

        Counter.builder(metricsPrefix + ".cost.total.cents")
                .tag("model", sanitizeTag(model))
                .register(meterRegistry)
                .increment(costCents);
    }

    /**
     * Record cache hit
     */
    public void recordCacheHit() {
        cacheHitsTotal.increment();
    }

    /**
     * Record cache miss
     */
    public void recordCacheMiss() {
        cacheMissesTotal.increment();
    }

    /**
     * Record fallback invocation
     */
    public void recordFallbackInvocation(String reason) {
        fallbackInvocationsTotal.increment();

        Counter.builder(metricsPrefix + ".fallback.by.reason")
                .tag("reason", sanitizeTag(reason))
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record circuit breaker state change
     */
    public void recordCircuitBreakerOpen(String circuitName) {
        circuitBreakerOpenTotal.increment();

        Counter.builder(metricsPrefix + ".circuit.breaker.open.by.name")
                .tag("circuit", sanitizeTag(circuitName))
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record embedding generation
     */
    public Timer.Sample startEmbeddingTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordEmbeddingComplete(Timer.Sample sample, int dimensions) {
        sample.stop(embeddingLatency);

        Counter.builder(metricsPrefix + ".embeddings.generated")
                .tag("dimensions", String.valueOf(dimensions))
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record RAG retrieval
     */
    public Timer.Sample startRagRetrievalTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordRagRetrievalComplete(Timer.Sample sample, int documentsRetrieved) {
        sample.stop(ragRetrievalLatency);

        DistributionSummary.builder(metricsPrefix + ".rag.documents.retrieved")
                .register(meterRegistry)
                .record(documentsRetrieved);
    }

    /**
     * Reset daily counters (called by scheduler)
     */
    public void resetDailyCounters() {
        dailyRequestCount.set(0);
        dailyCostCents.set(0);
        log.info("Daily AI metrics counters reset");
    }

    /**
     * Get current daily request count
     */
    public long getDailyRequestCount() {
        return dailyRequestCount.get();
    }

    /**
     * Get current daily cost
     */
    public long getDailyCostCents() {
        return dailyCostCents.get();
    }

    /**
     * Sanitize tag values for metrics
     */
    private String sanitizeTag(String value) {
        if (value == null) return "unknown";
        return value.toLowerCase()
                .replaceAll("[^a-z0-9_-]", "_")
                .substring(0, Math.min(value.length(), 50));
    }
}

