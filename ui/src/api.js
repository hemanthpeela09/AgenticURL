// Thin API client. All calls go through handleResponse to surface HTTP errors.
const BASE = import.meta.env.VITE_API_BASE || '';

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
    return fetch(`${BASE}/api/shorten`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
    }).then(handleResponse);
}

export function stats(code) {
    return fetch(`${BASE}/api/stats/${encodeURIComponent(code)}`).then(handleResponse);

}

export async function listScenarios() {
    return fetch(`${BASE}/api/orchestrator/scenarios`).then(handleResponse);
}

export async function runScenario(scenario, approval = 'auto') {
    const q =  new URLSearchParams({ scenario, approval }).toString();
    return fetch(`${BASE}/api/orchestrator/run?${q}`, { method: 'POST'}).then(handleResponse);
}

// ==========================================
// LangChain / Spring AI Endpoints
// ==========================================

/**
 * Get LLM backend information
 */
export function getLlmInfo() {
    return fetch(`${BASE}/api/llm/info`).then(handleResponse);
}

/**
 * Simple chat completion
 */
export function chat(message, systemPrompt = null) {
    return fetch(`${BASE}/api/llm/chat`, {
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

    const eventSource = new EventSource(`${BASE}/api/llm/chat/stream?${params}`);

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
    return fetch(`${BASE}/api/llm/chain/sdlc`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ requirement }),
    }).then(handleResponse);
}

/**
 * Execute custom LLM chain
 */
export function runCustomChain(context, steps) {
    return fetch(`${BASE}/api/llm/chain/custom`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ context, steps }),
    }).then(handleResponse);
}

/**
 * Run SDLC agent with tools
 */
export function runSdlcAgent(input) {
    return fetch(`${BASE}/api/llm/agent/sdlc`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ input }),
    }).then(handleResponse);
}

/**
 * Format a prompt template
 */
export function formatTemplate(template, variables) {
    return fetch(`${BASE}/api/llm/template/format`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ template, variables }),
    }).then(handleResponse);
}