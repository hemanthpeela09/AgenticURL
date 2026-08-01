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