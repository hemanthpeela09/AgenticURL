package com.example.shortener;

public record Link(String code, String longUrl, String alias, Long ttlSeconds) {
}
