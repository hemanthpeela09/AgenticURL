package com.example.shortener.store;

import com.example.shortener.domain.Link;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryLinkStore implements LinkStore {

    private final Map<String, Link> links = new ConcurrentHashMap<>();
    private final Map<String, String> byUrl = new ConcurrentHashMap<>();
    private final AtomicLong counter = new AtomicLong(0);

    @Override
    public long nextId() { return counter.incrementAndGet(); }

    @Override
    public boolean exists(String code) { return links.containsKey(code); }

    @Override
    public void save(Link link) {
        links.put(link.code(), link);
        byUrl.putIfAbsent(link.longUrl(), link.code());
    }

    @Override
    public Optional<Link> get(String code) { return Optional.ofNullable(links.get(code)); }

    @Override
    public Optional<String> codeForUrl(String longUrl) { return Optional.ofNullable(byUrl.get(longUrl)); }

    @Override
    public long count() { return links.size(); }

    @Override
    public List<Link> findAll(){
        return new ArrayList<>(links.values());
    }
}