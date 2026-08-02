package com.example.shortener.store;

import com.example.shortener.domain.Link;

import java.util.List;
import java.util.Optional;

/**
 * Storage abstraction for links + clicks. The default implementation is
 * in-memory (zero setup); a JPA/H2/Postgres implementation can be dropped in
 * without touching the API or service layers.
 */
public interface LinkStore {

    long nextId();

    boolean exists(String code);

    void save(Link link);

    Optional<Link> get(String code);

    Optional<String> codeForUrl(String longUrl);

    long count();

    List<Link> findAll();
}