package com.cardconnect.bolt.ai.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limiting Interceptor using Bucket4j
 * Limits API requests per client based on API key or IP address
 */
@Component
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    @Value("${api.rate-limit.requests-per-minute:100}")
    private int requestsPerMinute;

    @Value("${api.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Override
    public boolean preHandle(HttpServletRequest request,
                            HttpServletResponse response,
                            Object handler) throws Exception {

        // Skip rate limiting for health checks
        if (request.getRequestURI().startsWith("/actuator/health")) {
            return true;
        }

        // If rate limiting is disabled (dev mode), allow all
        if (!rateLimitEnabled) {
            return true;
        }

        String key = getClientIdentifier(request);
        Bucket bucket = cache.computeIfAbsent(key, k -> createNewBucket());

        if (bucket.tryConsume(1)) {
            log.debug("Request allowed for client: {}", key);
            return true;
        }

        log.warn("Rate limit exceeded for client: {}", key);
        response.setStatus(429); // Too Many Requests
        response.setContentType("application/json");
        response.getWriter().write(
            "{\"error\":\"Rate limit exceeded\",\"message\":\"Maximum " +
            requestsPerMinute + " requests per minute allowed\"}"
        );
        return false;
    }

    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.classic(
            requestsPerMinute,
            Refill.intervally(requestsPerMinute, Duration.ofMinutes(1))
        );
        return Bucket.builder()
            .addLimit(limit)
            .build();
    }

    private String getClientIdentifier(HttpServletRequest request) {
        // Use API key if present, otherwise use IP address
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            return "key:" + apiKey;
        }

        String ip = request.getRemoteAddr();
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.trim().isEmpty()) {
            ip = forwardedFor.split(",")[0].trim();
        }

        return "ip:" + ip;
    }
}

