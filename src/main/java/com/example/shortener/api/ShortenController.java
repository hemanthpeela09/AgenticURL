package com.example.shortener.api;

import com.example.shortener.api.Dtos.ShortenRequest;
import com.example.shortener.api.Dtos.ShortenResponse;
import com.example.shortener.api.Dtos.StatsResponse;
import com.example.shortener.domain.Link;
import com.example.shortener.service.LinkService;
import com.example.shortener.service.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ShortenController {

    private final LinkService service;
    private final RateLimiter rateLimiter;
    private final String baseUrl;

    public ShortenController(LinkService service, RateLimiter rateLimiter,
                             @Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        this.service = service;
        this.rateLimiter = rateLimiter;
        this.baseUrl = baseUrl;
    }

    @PostMapping("/shorten")
    public ResponseEntity<ShortenResponse> shorten(@RequestBody ShortenRequest req,
                                                   HttpServletRequest http) {
        if (!rateLimiter.allow(clientKey(http))) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "rate limit exceeded");
        }
        Link link = service.create(req.url(), req.customAlias(), req.ttlSeconds()).link();
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(link));
    }

    @GetMapping("/stats/{code}")
    public StatsResponse stats(@PathVariable String code) {
        LinkService.Stats s = service.stats(code);
        return new StatsResponse(s.code(), s.longUrl(), s.createdAt(), s.expiresAt(),
                s.totalClicks(), s.clicksByDay(), s.referrers(), s.lastAccessed());
    }

    @GetMapping("/urls")
    public java.util.List<ShortenResponse> listAllUrls() {
        return service.listAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private ShortenResponse toResponse(Link link) {
        return new ShortenResponse(
                link.code(),
                baseUrl + "/" + link.code(),
                link.longUrl(),
                link.createdAt(),
                link.expiresAt());
    }

    private String clientKey(HttpServletRequest http) {
        String fwd = http.getHeader("X-Forwarded-For");
        return fwd != null && !fwd.isBlank() ? fwd.split(",")[0].trim() : http.getRemoteAddr();
    }
}