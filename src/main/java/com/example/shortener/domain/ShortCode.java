package com.example.shortener.domain;

import java.util.Set;
import java.util.regex.Pattern;

/** Collision-free base62 short-code generation + custom alias validation. */
public final class ShortCode {

    private static final String ALPHABET =
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = ALPHABET.length();

    public static final Set<String> RESERVED =
            Set.of("api", "docs", "health", "stats", "openapi", "redoc", "swagger-ui");

    private static final Pattern ALIAS = Pattern.compile("^[A-Za-z0-9_-]{3,32}$");

    private ShortCode() {
    }

    /** Encode a non-negative integer as a base62 string. */
    public static String encode(long n) {
        if (n < 0) {
            throw new IllegalArgumentException("id must be non-negative");
        }
        if (n == 0) {
            return String.valueOf(ALPHABET.charAt(0));
        }
        StringBuilder sb = new StringBuilder();
        while (n > 0) {
            int rem = (int) (n % BASE);
            sb.append(ALPHABET.charAt(rem));
            n /= BASE;
        }
        return sb.reverse().toString();
    }

    public static boolean isValidAlias(String alias) {
        return alias != null
                && ALIAS.matcher(alias).matches()
                && !RESERVED.contains(alias.toLowerCase());
    }
}