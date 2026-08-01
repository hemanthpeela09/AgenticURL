package com.example.shortener.api;

import com.example.shortener.api.Dtos.HealthResponse;
import com.example.shortener.domain.Link;
import com.example.shortener.service.LinkService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
public class RedirectController {

    private final LinkService service;

    public RedirectController(LinkService service) { this.service = service; }

    @GetMapping("/health")
    public HealthResponse health() { return new HealthResponse("ok", service.count()); }

    // Constrain to the base62/alias charset (no dots or slashes) so that
    // static resources (/index.html, /assets/*, /favicon.ico) and the SPA
    // welcome page are not swallowed by this catch-all mapping.
    @GetMapping("/{code:[A-Za-z0-9_-]+}")
    public ResponseEntity<Void> redirect(@PathVariable String code, HttpServletRequest http) {
        String referrer = http.getHeader(HttpHeaders.REFERER);
        Link link = service.resolveAndRecord(code, referrer);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(link.longUrl()))
                .build();
    }
}