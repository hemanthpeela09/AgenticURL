package com.example.shortener;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class LinkServiceTest {
    @Test
    void createShortLinkReturnsPlaceholder() {
        LinkService service = new LinkService();
        assertNotNull(service.createShortLink("https://example.com"));
    }
}
