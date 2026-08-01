package com.example.shortener.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A shortened link plus its recorded clicks. */
public class Link {

    private final String code;
    private final String longUrl;
    private final Instant createdAt;
    private final Instant expiresAt; // nullable
    private final List<Click> clicks = Collections.synchronizedList(new ArrayList<>());

    public Link(String code, String longUrl, Instant createdAt, Instant expiresAt) {
        this.code = code;
        this.longUrl = longUrl;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public String code() { return code; }

    public String longUrl() { return longUrl; }

    public Instant createdAt() { return createdAt; }

    public Instant expiresAt() { return expiresAt; }

    public List<Click> clicks() { return clicks; }

    public boolean isExpired(Instant now) { return expiresAt != null && !now.isBefore(expiresAt); }

    public void addClick(Click click) { clicks.add(click); }
}