package com.cardconnect.bolt.ai.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration for Observability including:
 * - Prometheus metrics
 * - OpenTelemetry tracing
 * - Micrometer observations
 * - Custom AI metrics
 */
@Configuration
@Slf4j
public class ObservabilityConfiguration {

    @Value("${spring.application.name:offline-batch-ai-monitoring}")
    private String applicationName;

    @Value("${METRICS_ENV:development}")
    private String environment;

    /**
     * Customize MeterRegistry with common tags for all metrics
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config()
                .commonTags(List.of(
                        Tag.of("application", applicationName),
                        Tag.of("environment", environment),
                        Tag.of("framework", "spring-boot"),
                        Tag.of("ai-enabled", "true")
                ));
    }

    /**
     * Enable @Timed annotation support for method-level metrics
     */
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        log.info("Initializing TimedAspect for @Timed annotation support");
        return new TimedAspect(registry);
    }

    /**
     * Enable @Observed annotation support for method-level observations
     */
    @Bean
    public ObservedAspect observedAspect(ObservationRegistry observationRegistry) {
        log.info("Initializing ObservedAspect for @Observed annotation support");
        return new ObservedAspect(observationRegistry);
    }
}

