package com.example.shortener.domain;

import java.time.Instant;

/** A single recorded visit to a short link. */
public record Click(Instant timestamp, String referrer) {
    public Click {
        if (referrer == null || referrer.isBlank()) {
            referrer = "direct";
        }
    }
}