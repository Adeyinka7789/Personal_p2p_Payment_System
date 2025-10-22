package com.example.ppps.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;  // <-- ADDED THIS IMPORT
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS_PER_MINUTE = 100;
    private final ConcurrentHashMap<String, Integer> requestCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> requestTimestamps = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {  // <-- ADDED ServletException HERE
        String clientIp = request.getRemoteAddr();
        long currentTime = System.currentTimeMillis();
        long minuteAgo = currentTime - 60000;

        // Clean up old entries
        requestTimestamps.entrySet().removeIf(entry -> entry.getValue() < minuteAgo);
        requestCounts.entrySet().removeIf(entry -> entry.getValue() == 0);

        int count = requestCounts.getOrDefault(clientIp, 0);
        long lastRequestTime = requestTimestamps.getOrDefault(clientIp, 0L);

        if (count >= MAX_REQUESTS_PER_MINUTE && currentTime - lastRequestTime < 60000) {
            response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(), "Rate limit exceeded");
            return;
        }

        if (count == 0 || currentTime - lastRequestTime >= 60000) {
            requestCounts.put(clientIp, 1);
            requestTimestamps.put(clientIp, currentTime);
        } else {
            requestCounts.put(clientIp, count + 1);
        }

        filterChain.doFilter(request, response);
    }
}