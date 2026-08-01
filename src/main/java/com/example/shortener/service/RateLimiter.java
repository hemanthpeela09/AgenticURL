package com.example.shortener.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/** In-memory sliding-window rate limiter, keyed per client. */
@Component
public class RateLimiter {

    private final int maxRequests;
    private final long windowMillis;
    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    public RateLimiter(
            @Value("${ratelimit.max:120}") int maxRequests,
            @Value("${ratelimit.window-seconds:60}") long windowSeconds) {
        this.maxRequests = maxRequests;
        this.windowMillis = Duration.ofSeconds(windowSeconds).toMillis();
    }

    public boolean allow(String key) {
        long now = System.currentTimeMillis();
        Deque<Long> q = hits.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());
        synchronized (q) {
            while (!q.isEmpty() && q.peekFirst() <= now - windowMillis) {
                q.pollFirst();
            }
            if (q.size() >= maxRequests) {
                return false;
            }
            q.addLast(now);
            return true;
        }
    }
}