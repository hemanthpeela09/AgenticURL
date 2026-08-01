package com.example.shortener;

import com.example.shortener.service.LinkService;
import com.example.shortener.store.InMemoryLinkStore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class LinkServiceTest {
    @Test
    void createShortLinkReturnsCreatedRecord() {
        LinkService service = new LinkService(new InMemoryLinkStore());
        LinkService.CreateResult result = service.create("https://example.com", null, null);

        assertNotNull(result);
        assertNotNull(result.link());
    }
}
