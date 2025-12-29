package com.cardconnect.bolt.ai.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * API Key Authentication Filter
 * Validates API keys for securing endpoints
 */
@Component
@Slf4j
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";

    @Value("${api.security.api-keys:demo-key-123,demo-key-456}")
    private String apiKeysString;

    @Value("${api.security.enabled:false}")
    private boolean securityEnabled;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestPath = request.getRequestURI();

        // Skip authentication for health and public endpoints
        if (isPublicEndpoint(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        // If security is disabled (dev mode), allow all
        if (!securityEnabled) {
            setAnonymousAuthentication();
            filterChain.doFilter(request, response);
            return;
        }

        String apiKey = request.getHeader(API_KEY_HEADER);

        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("Missing API key for request: {}", requestPath);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Missing API key\"}");
            return;
        }

        if (!isValidApiKey(apiKey)) {
            log.warn("Invalid API key attempted for request: {}", requestPath);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Invalid API key\"}");
            return;
        }

        // Set authentication in SecurityContext after successful API key validation
        setApiKeyAuthentication(apiKey);

        log.debug("API key validated for request: {}", requestPath);
        filterChain.doFilter(request, response);
    }

    private void setApiKeyAuthentication(String apiKey) {
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                "api-client",
                apiKey,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_API_CLIENT"))
            );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void setAnonymousAuthentication() {
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                "anonymous",
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
            );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/actuator/health") ||
               path.startsWith("/actuator/info") ||
               path.startsWith("/h2-console") ||
               path.equals("/") ||
               path.equals("/favicon.ico");
    }

    private boolean isValidApiKey(String apiKey) {
        List<String> validKeys = Arrays.asList(apiKeysString.split(","));
        return validKeys.stream()
            .map(String::trim)
            .anyMatch(key -> key.equals(apiKey));
    }
}

