package com.example.orchestrator.llm;

import com.example.shortener.service.LinkService;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * URL Shortener Agent that processes natural language requests to create short URLs.
 * This agent integrates with the LangChain-style orchestration to provide an AI-powered
 * interface for the URL shortening service.
 *
 * Example prompts:
 * - "Shorten https://example.com/very/long/url with alias mylink"
 * - "Create a short URL for https://google.com"
 * - "I want to shorten https://github.com/repo and call it gh-repo"
 */
@Component
public class UrlShortenerAgent {

    private static final Pattern URL_PATTERN = Pattern.compile(
            "(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern ALIAS_PATTERN = Pattern.compile(
            "(?:alias|call it|name it|named?|as|with alias|custom alias)[:\\s]+[\"']?([\\w\\-]+)[\"']?",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern TTL_PATTERN = Pattern.compile(
            "(?:ttl|expire|expires? in|valid for)[:\\s]+(\\d+)\\s*(?:seconds?|secs?|s)?",
            Pattern.CASE_INSENSITIVE
    );

    private final LinkService linkService;
    private final Llm llm;
    private final String baseUrl;

    public UrlShortenerAgent(LinkService linkService, Llm llm,
                             @org.springframework.beans.factory.annotation.Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        this.linkService = linkService;
        this.llm = llm;
        this.baseUrl = baseUrl;
    }

    /**
     * Process a natural language request to shorten a URL.
     * Uses LLM for intent understanding and structured extraction.
     *
     * @param userPrompt Natural language prompt from user
     * @return Result containing the shortened URL info or error
     */
    public ShortenResult processPrompt(String userPrompt) {
        // First, try rule-based extraction (faster, works offline)
        ExtractedParams extracted = extractParamsRuleBased(userPrompt);

        // If rule-based extraction fails to find URL, use LLM
        if (extracted.url == null) {
            extracted = extractParamsWithLlm(userPrompt);
        }

        if (extracted.url == null) {
            return ShortenResult.error("Could not find a valid URL in your request. Please provide a URL to shorten.");
        }

        // Validate URL format
        if (!isValidUrl(extracted.url)) {
            return ShortenResult.error("The URL '" + extracted.url + "' is not valid. Please provide a valid HTTP/HTTPS URL.");
        }

        try {
            // Call the link service to create the short URL
            LinkService.CreateResult result = linkService.create(
                    extracted.url,
                    extracted.alias,
                    extracted.ttlSeconds
            );

            String shortUrl = baseUrl + "/" + result.link().code();

            return ShortenResult.success(
                    result.link().code(),
                    shortUrl,
                    extracted.url,
                    extracted.alias,
                    extracted.ttlSeconds,
                    generateSuccessMessage(result.link().code(), shortUrl, extracted)
            );
        } catch (Exception e) {
            return ShortenResult.error("Failed to create short URL: " + e.getMessage());
        }
    }

    /**
     * Create an AgentExecutor with URL shortening tools.
     */
    public AgentExecutor createAgent() {
        return new AgentExecutor(llm)
                .addTool("extract_url",
                        "Extract URL from user input. Returns the URL found.",
                        this::toolExtractUrl)
                .addTool("extract_alias",
                        "Extract custom alias from user input. Returns the alias or 'none'.",
                        this::toolExtractAlias)
                .addTool("shorten_url",
                        "Create a shortened URL. Input format: 'url=<URL>,alias=<ALIAS_OR_NONE>,ttl=<SECONDS_OR_NONE>'",
                        this::toolShortenUrl)
                .addTool("validate_url",
                        "Check if a URL is valid. Returns 'valid' or 'invalid: reason'",
                        this::toolValidateUrl);
    }

    // ---- Tool implementations ----

    private String toolExtractUrl(String input) {
        Matcher matcher = URL_PATTERN.matcher(input);
        if (matcher.find()) {
            return "Found URL: " + matcher.group(1);
        }
        return "No URL found in input";
    }

    private String toolExtractAlias(String input) {
        Matcher matcher = ALIAS_PATTERN.matcher(input);
        if (matcher.find()) {
            return "Found alias: " + matcher.group(1);
        }
        // Also check for simple patterns like "as myalias" at the end
        Pattern simpleAlias = Pattern.compile("\\s+as\\s+([\\w\\-]+)\\s*$", Pattern.CASE_INSENSITIVE);
        Matcher simpleMatcher = simpleAlias.matcher(input);
        if (simpleMatcher.find()) {
            return "Found alias: " + simpleMatcher.group(1);
        }
        return "No alias specified";
    }

    private String toolShortenUrl(String input) {
        try {
            Map<String, String> params = parseToolInput(input);
            String url = params.get("url");
            String alias = "none".equalsIgnoreCase(params.get("alias")) ? null : params.get("alias");
            Long ttl = "none".equalsIgnoreCase(params.get("ttl")) ? null : Long.parseLong(params.getOrDefault("ttl", "0"));
            if (ttl != null && ttl == 0) ttl = null;

            if (url == null || url.isBlank()) {
                return "Error: URL is required";
            }

            LinkService.CreateResult result = linkService.create(url, alias, ttl);
            String shortUrl = baseUrl + "/" + result.link().code();

            return String.format("Success! Created short URL:\n- Code: %s\n- Short URL: %s\n- Original: %s",
                    result.link().code(), shortUrl, url);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    private String toolValidateUrl(String url) {
        if (url == null || url.isBlank()) {
            return "invalid: URL is empty";
        }
        try {
            java.net.URI uri = new java.net.URI(url.trim());
            if (uri.getScheme() == null || (!uri.getScheme().equals("http") && !uri.getScheme().equals("https"))) {
                return "invalid: URL must use http or https scheme";
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                return "invalid: URL must have a host";
            }
            return "valid";
        } catch (Exception e) {
            return "invalid: " + e.getMessage();
        }
    }

    private Map<String, String> parseToolInput(String input) {
        Map<String, String> params = new HashMap<>();
        String[] parts = input.split(",");
        for (String part : parts) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2) {
                params.put(kv[0].trim().toLowerCase(), kv[1].trim());
            }
        }
        return params;
    }

    // ---- Extraction methods ----

    private ExtractedParams extractParamsRuleBased(String prompt) {
        String url = null;
        String alias = null;
        Long ttl = null;

        // Extract URL
        Matcher urlMatcher = URL_PATTERN.matcher(prompt);
        if (urlMatcher.find()) {
            url = urlMatcher.group(1);
        }

        // Extract alias
        Matcher aliasMatcher = ALIAS_PATTERN.matcher(prompt);
        if (aliasMatcher.find()) {
            alias = aliasMatcher.group(1);
        } else {
            // Try simpler patterns
            Pattern simpleAlias = Pattern.compile("\\s+as\\s+([\\w\\-]+)", Pattern.CASE_INSENSITIVE);
            Matcher simpleMatcher = simpleAlias.matcher(prompt);
            if (simpleMatcher.find()) {
                alias = simpleMatcher.group(1);
            }
        }

        // Extract TTL
        Matcher ttlMatcher = TTL_PATTERN.matcher(prompt);
        if (ttlMatcher.find()) {
            ttl = Long.parseLong(ttlMatcher.group(1));
        }

        return new ExtractedParams(url, alias, ttl);
    }

    private ExtractedParams extractParamsWithLlm(String prompt) {
        String systemPrompt = """
                You are a URL extraction assistant. Extract the following from the user's request:
                1. URL - the web address to shorten
                2. ALIAS - a custom short code (if specified)
                3. TTL - time to live in seconds (if specified)
                
                Respond in this exact format:
                URL: <extracted_url or NONE>
                ALIAS: <extracted_alias or NONE>
                TTL: <seconds or NONE>
                """;

        String response = llm.complete(systemPrompt, prompt);

        String url = extractField(response, "URL");
        String alias = extractField(response, "ALIAS");
        String ttlStr = extractField(response, "TTL");

        Long ttl = null;
        if (ttlStr != null && !ttlStr.equalsIgnoreCase("NONE")) {
            try {
                ttl = Long.parseLong(ttlStr.replaceAll("[^0-9]", ""));
            } catch (NumberFormatException ignored) {}
        }

        return new ExtractedParams(
                url != null && !url.equalsIgnoreCase("NONE") ? url : null,
                alias != null && !alias.equalsIgnoreCase("NONE") ? alias : null,
                ttl
        );
    }

    private String extractField(String response, String fieldName) {
        Pattern pattern = Pattern.compile(fieldName + ":\\s*(.+?)(?:\\n|$)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            String value = matcher.group(1).trim();
            return value.isEmpty() || value.equalsIgnoreCase("NONE") ? null : value;
        }
        return null;
    }

    private boolean isValidUrl(String url) {
        try {
            java.net.URI uri = new java.net.URI(url);
            return uri.getScheme() != null &&
                    (uri.getScheme().equals("http") || uri.getScheme().equals("https")) &&
                    uri.getHost() != null;
        } catch (Exception e) {
            return false;
        }
    }

    private String generateSuccessMessage(String code, String shortUrl, ExtractedParams params) {
        StringBuilder sb = new StringBuilder();
        sb.append("✅ Successfully created short URL!\n\n");
        sb.append("**Short URL:** ").append(shortUrl).append("\n");
        sb.append("**Code:** ").append(code).append("\n");
        sb.append("**Original URL:** ").append(params.url).append("\n");
        if (params.alias != null) {
            sb.append("**Custom Alias:** ").append(params.alias).append("\n");
        }
        if (params.ttlSeconds != null) {
            sb.append("**Expires in:** ").append(params.ttlSeconds).append(" seconds\n");
        }
        return sb.toString();
    }

    // ---- Inner classes ----

    private record ExtractedParams(String url, String alias, Long ttlSeconds) {}

    public record ShortenResult(
            boolean success,
            String code,
            String shortUrl,
            String originalUrl,
            String customAlias,
            Long ttlSeconds,
            String message,
            String error
    ) {
        public static ShortenResult success(String code, String shortUrl, String originalUrl,
                                            String customAlias, Long ttlSeconds, String message) {
            return new ShortenResult(true, code, shortUrl, originalUrl, customAlias, ttlSeconds, message, null);
        }

        public static ShortenResult error(String error) {
            return new ShortenResult(false, null, null, null, null, null, null, error);
        }
    }
}