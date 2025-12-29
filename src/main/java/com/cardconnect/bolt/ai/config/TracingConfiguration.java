package com.cardconnect.bolt.ai.config;

import io.micrometer.tracing.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.tracing.ConditionalOnEnabledTracing;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Configuration for distributed tracing using OpenTelemetry.
 * Traces are exported to Jaeger via OTLP protocol.
 *
 * Spring Boot auto-configures OpenTelemetry when:
 * - management.tracing.enabled=true (default)
 * - micrometer-tracing-bridge-otel is on classpath
 * - OTEL_EXPORTER_OTLP_ENDPOINT is configured
 *
 * This class just logs the configuration and provides documentation.
 */
@Configuration
@Slf4j
@ConditionalOnEnabledTracing
public class TracingConfiguration {

    @Value("${spring.application.name:offline-batch-ai-monitoring}")
    private String applicationName;

    @Value("${management.otlp.tracing.endpoint:http://localhost:4318/v1/traces}")
    private String otlpEndpoint;

    @Value("${management.tracing.sampling.probability:1.0}")
    private double samplingProbability;

    private final Tracer tracer;

    public TracingConfiguration(Tracer tracer) {
        this.tracer = tracer;
    }

    @PostConstruct
    public void logTracingConfiguration() {
        log.info("=== Distributed Tracing Configuration ===");
        log.info("Application Name: {}", applicationName);
        log.info("OTLP Endpoint: {}", otlpEndpoint);
        log.info("Sampling Probability: {}", samplingProbability);
        log.info("Tracer Implementation: {}", tracer.getClass().getSimpleName());
        log.info("==========================================");
    }
}
