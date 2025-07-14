// src/main/java/com/ronak/welcome/service/RateLimiterService.java
package com.ronak.welcome.service.impl; // <<<--- THIS MUST BE THE PACKAGE DECLARATION

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {

    private static final Logger logger = LoggerFactory.getLogger(RateLimiterService.class);

    // Map to store a Bucket for each client (identified by IP address or user ID)
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Value("${app.rate-limit.capacity:100}")
    private long capacity; // Max tokens in the bucket (max requests in a burst)

    @Value("${app.rate-limit.refill-rate:10}")
    private long refillRate; // Tokens added per duration

    @Value("${app.rate-limit.duration-seconds:1}")
    private long durationSeconds; // The duration over which refill-rate applies (e.g., 10 tokens per 1 second)

    @Value("${app.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    /**
     * Attempts to consume a token for the given client.
     *
     * @param clientId The identifier for the client (e.g., IP address, user ID).
     * @return true if a token was consumed (request allowed), false otherwise (request rate-limited).
     */
    public boolean tryConsumeToken(String clientId) {
        if (!rateLimitEnabled) {
            return true; // Rate limiting is disabled, always allow
        }

        // Get or create a bucket for the client
        Bucket bucket = buckets.computeIfAbsent(clientId, this::createNewBucket);

        // Try to consume one token
        boolean consumed = bucket.tryConsume(1);
        if (!consumed) {
            logger.warn("Rate limit exceeded for client: {}", clientId);
        }
        return consumed;
    }

    /**
     * Creates a new token bucket with the configured capacity and refill rate.
     * Uses Bucket4j library for robust token bucket implementation.
     *
     * @param clientId The ID of the client for whom the bucket is created.
     * @return A configured Bucket instance.
     */
    private Bucket createNewBucket(String clientId) {
        logger.info("Creating new rate limit bucket for client: {}", clientId);
        // Define the refill strategy: refillRate tokens every durationSeconds
        Refill refill = Refill.greedy(refillRate, Duration.ofSeconds(durationSeconds));
        // Define the bandwidth: capacity tokens, refilled according to the refill strategy
        Bandwidth limit = Bandwidth.classic(capacity, refill);
        return Bucket4j.builder().addLimit(limit).build();
    }
}
