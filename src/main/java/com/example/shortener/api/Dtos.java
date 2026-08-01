package com.example.shortener.api;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.Map;

/** Request/response DTOs mirroring specs/02-openapi.yaml. */
public final class Dtos {

    private Dtos() {
    }

    public record ShortenRequest(
            @NotBlank String url,
            String customAlias,
            Long ttlSeconds) {
    }

    public record ShortenResponse(
            String code,
            String shortUrl,
            String longUrl,
            Instant createdAt,
            Instant expiresAt) {
    }

    public record StatsResponse(
            String code,
            String longUrl,
            Instant createdAt,
            Instant expiresAt,
            int totalClicks,
            Map<String, Integer> clicksByDay,
            Map<String, Integer> referrers,
            Instant lastAccessed) {
    }

    public record HealthResponse(String status, long links) {
    }

    public record ErrorResponse(String detail) {
    }
}