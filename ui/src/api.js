// Thin API client. All calls go through handleResponse to surface HTTP errors.
// Use VITE_API_BASE from build-time, falling back to runtime window.APP_API_BASE.
const BASE = import.meta.env.VITE_API_BASE || window.APP_API_BASE || '';

function apiUrl(path) {
    if (!path.startsWith('/')) {
        path = `/${path}`;
    }
    return `${BASE}${path}`;
}

async function handleResponse(res) {
    if (!res.ok) {
        let detail = `HTTP ${res.status}`;
        try {
            const body = await res.json();
            if (body && body.detail) {
                detail = body.detail;
            }
        } catch (err) {
            // ignore
        }
        throw new Error(detail);
    }
    return res.status ===204 ? null : res.json();
}

export function shorten(payload) {
    const promptParts = [];
    if (payload?.prompt) {
        return fetch(apiUrl('/api/orchestrator/shorten'), {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ prompt: payload.prompt }),
        }).then(handleResponse);
    }

    if (payload?.url) {
        promptParts.push(`Shorten ${payload.url}`);
    }
    if (payload?.customAlias) {
        promptParts.push(`with alias ${payload.customAlias}`);
    }
    if (payload?.ttlSeconds != null && payload.ttlSeconds !== '') {
        promptParts.push(`with ttl ${payload.ttlSeconds}`);
    }

    const prompt = promptParts.join(' ').trim();

    return fetch(apiUrl('/api/orchestrator/shorten'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ prompt }),
    }).then(handleResponse);
}

export function stats(code) {
    return fetch(apiUrl(`/api/stats/${encodeURIComponent(code)}`)).then(handleResponse);
}

export function listAllUrls() {
    return fetch(apiUrl('/api/urls')).then(handleResponse);
}

export async function listScenarios() {
    return fetch(apiUrl('/api/orchestrator/scenarios')).then(handleResponse);
}

export async function runScenario(scenario, approval = 'auto') {
    const q =  new URLSearchParams({ scenario, approval }).toString();
    return fetch(apiUrl(`/api/orchestrator/run?${q}`), { method: 'POST'}).then(handleResponse);
}

// ==========================================
// LangChain / Spring AI Endpoints
// ==========================================

/**
 * Get LLM backend information
 */
export function getLlmInfo() {
    return fetch(apiUrl('/api/llm/info')).then(handleResponse);
}

/**
 * Simple chat completion
 */
export function chat(message, systemPrompt = null) {
    return fetch(apiUrl('/api/llm/chat'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message, systemPrompt }),
    }).then(handleResponse);
}

/**
 * Streaming chat completion using Server-Sent Events
 * @param {string} message - User message
 * @param {string} systemPrompt - System prompt
 * @param {function} onToken - Callback for each token
 * @param {function} onComplete - Callback when stream completes
 * @param {function} onError - Callback for errors
 * @returns {function} - Abort function to cancel the stream
 */
export function streamChat(message, systemPrompt, onToken, onComplete, onError) {
    const params = new URLSearchParams({ message });
    if (systemPrompt) params.append('systemPrompt', systemPrompt);

    const eventSource = new EventSource(apiUrl(`/api/llm/chat/stream?${params}`));

    eventSource.onmessage = (event) => {
        onToken(event.data);
    };

    eventSource.onerror = (error) => {
        eventSource.close();
        if (onError) onError(error);
    };

    eventSource.addEventListener('complete', () => {
        eventSource.close();
        if (onComplete) onComplete();
    });

    // Return abort function
    return () => eventSource.close();
}

/**
 * Execute SDLC chain with LangChain
 */
export function runSdlcChain(requirement) {
    return fetch(apiUrl('/api/llm/chain/sdlc'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ requirement }),
    }).then(handleResponse);
}

/**
 * Execute custom LLM chain
 */
export function runCustomChain(context, steps) {
    return fetch(apiUrl('/api/llm/chain/custom'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ context, steps }),
    }).then(handleResponse);
}

/**
 * Run SDLC agent with tools
 */
export function runSdlcAgent(input) {
    return fetch(apiUrl('/api/llm/agent/sdlc'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ input }),
    }).then(handleResponse);
}

/**
 * Format a prompt template
 */
export function formatTemplate(template, variables) {
    return fetch(apiUrl('/api/llm/template/format'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ template, variables }),
    }).then(handleResponse);
}

/**
 * Shorten a URL using AI/natural language processing.
 * The AI will extract the URL and optional alias from the prompt.
 *
 * Example prompts:
 * - "Shorten https://example.com/long/url with alias mylink"
 * - "Create a short URL for https://google.com"
 * - "I want to shorten https://github.com/repo and call it gh-repo"
 */
export function shortenWithAi(prompt) {
    return fetch(apiUrl('/api/orchestrator/shorten'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ prompt }),
    }).then(handleResponse);
}

/**
 * Shorten a URL using the ReAct agent with full reasoning trace.
 */
export function shortenWithAgent(prompt) {
    return fetch(apiUrl('/api/llm/shorten/agent'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ prompt }),
    }).then(handleResponse);
}

// Blue - Green Metrics
export function getGreenfieldHealth() {
    return fetch(apiUrl('/api/orchestrator/greenfield/health')).then(handleResponse);
}

export function getBlueGreenMetrics() {
    return fetch(apiUrl('/api/orchestrator/metrics')).then(handleResponse);
}