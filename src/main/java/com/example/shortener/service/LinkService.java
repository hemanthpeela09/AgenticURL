package com.example.shortener.service;

import com.example.shortener.api.ApiException;
import com.example.shortener.domain.Click;
import com.example.shortener.domain.Link;
import com.example.shortener.domain.ShortCode;
import com.example.shortener.store.LinkStore;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

@Service
public class LinkService {

    private final LinkStore store;

    public LinkService(LinkStore store) {
        this.store = store;
    }

    public record CreateResult(Link link) {}

    public CreateResult create(String url, String customAlias, Long ttlSeconds) {
        validateUrl(url);

        Instant now = Instant.now();
        Instant expiresAt = ttlSeconds != null ? now.plusSeconds(ttlSeconds) : null;

        String code;
        if (customAlias != null && !customAlias.isBlank()) {
            if (store.exists(customAlias)) {
                throw new ApiException(HttpStatus.CONFLICT, "alias already in use");
            }
            code = customAlias;
        } else {
            // de-duplicate: same URL, no alias/ttl -> reuse existing live code (FR-6)
            if (ttlSeconds == null) {
                Optional<String> existing = store.codeForUrl(url);
                if (existing.isPresent()) {
                    Optional<Link> link = store.get(existing.get());
                    if (link.isPresent() && !link.get().isExpired(now)) {
                        return new CreateResult(link.get());
                    }
                }
            }

            code = ShortCode.encode(store.nextId());
            while (store.exists(code)) {
                code = ShortCode.encode(store.nextId());
            }
        }

        Link link = new Link(code, url, now, expiresAt);
        store.save(link);
        return new CreateResult(link);
    }

    /** Resolve a live link and record a click; throws 404/410 as appropriate. */
    public Link resolveAndRecord(String code, String referrer) {
        Link link = store.get(code)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "unknown code"));
        if (link.isExpired(Instant.now())) {
            throw new ApiException(HttpStatus.GONE, "link expired");
        }
        link.addClick(new Click(Instant.now(), referrer));
        return link;
    }

    public Stats stats(String code) {
        Link link = store.get(code)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "unknown code"));
        Map<String, Integer> byDay = new TreeMap<>();
        Map<String, Integer> referrers = new LinkedHashMap<>();
        Instant last = null;
        List<Click> clicks = List.copyOf(link.clicks());
        for (Click c : clicks) {
            String day = LocalDate.ofInstant(c.timestamp(), ZoneOffset.UTC).toString();
            byDay.merge(day, 1, Integer::sum);
            referrers.merge(c.referrer(), 1, Integer::sum);
            if (last == null || c.timestamp().isAfter(last)) {
                last = c.timestamp();
            }
        }
        return new Stats(link.code(), link.longUrl(), link.createdAt(), link.expiresAt(),
                clicks.size(), byDay, referrers, last);
    }

    public long count() { return store.count(); }

    private void validateUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "url is required");
        }
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            if (scheme == null || !(scheme.equals("http") || scheme.equals("https"))
                    || uri.getHost() == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "url must be an absolute http(s) URL");
            }
        } catch (URISyntaxException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "malformed url");
        }
    }

    public record Stats(
            String code,
            String longUrl,
            Instant createdAt,
            Instant expiresAt,
            int totalClicks,
            Map<String, Integer> clicksByDay,
            Map<String, Integer> referrers,
            Instant lastAccessed) {
    }
}