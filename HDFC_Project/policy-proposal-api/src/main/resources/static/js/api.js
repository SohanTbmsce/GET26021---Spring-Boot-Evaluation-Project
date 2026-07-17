/**
 * api.js
 * Fetch API wrapper for backend integration.
 * Uses ONLY relative URLs (e.g., /customers).
 */

import { showSpinner, hideSpinner } from './utils.js';

const API = {
  baseUrl: '', // relative usage
};

function getErrorMessage(err) {
  if (!err) return 'Unknown error';
  if (typeof err === 'string') return err;

  // fetch() errors typically come as Error objects
  if (err.message) return err.message;

  return 'Request failed';
}

function normalizeErrorResponse(body) {
  // backend may return { message } or { error } or { ... }
  if (!body || typeof body !== 'object') return { message: 'Request failed' };
  return {
    message: body.message || body.error || body.title || JSON.stringify(body),
  };
}

async function requestJson(url, { method = 'GET', body = null } = {}) {
  const started = performance.now();
  const resp = await fetch(url, {
    method,
    headers: body ? { 'Content-Type': 'application/json' } : undefined,
    body: body ? JSON.stringify(body) : undefined,
  });

  const elapsedMs = performance.now() - started;

  let parsed = null;
  const ct = resp.headers.get('content-type') || '';
  if (ct.includes('application/json')) {
    parsed = await resp.json();
  } else {
    // In case backend returns plain text
    const text = await resp.text().catch(() => '');
    parsed = text ? { message: text } : null;
  }

  return { resp, data: parsed, elapsedMs };
}

async function callWithMonitor(url, { method = 'GET', body = null } = {}) {
  // Update floating widget if present
  const monitor = window.__apiMonitor;
  if (monitor && typeof monitor.beforeApiCall === 'function') {
    monitor.beforeApiCall(url, method);
  }

  const started = performance.now();
  showSpinner();

  try {
    const { resp, data, elapsedMs } = await requestJson(url, { method, body });

    const apiStatus = monitor?.afterApiCall
      ? monitor.afterApiCall(url, method, resp.status, elapsedMs)
      : undefined;

    if (!resp.ok) {
      const normalized = normalizeErrorResponse(data);
      const message = normalized.message || `HTTP ${resp.status}`;
      throw new Error(message);
    }

    return { data, httpStatus: resp.status, elapsedMs };
  } finally {
    const _elapsed = performance.now() - started;
    // spinner is always hidden even when error thrown
    hideSpinner();
  }
}

/**
 * Backend health check
 * We don't have a dedicated health endpoint in your list.
 * We'll just call /customers to infer connectivity.
 */
export async function checkBackend() {
  try {
    const res = await callWithMonitor('/customers', { method: 'GET' });
    return { connected: true, responseSample: res.data };
  } catch (e) {
    return { connected: false, error: getErrorMessage(e) };
  }
}

/** GET /customers */
export async function getCustomers() {
  const res = await callWithMonitor('/customers', { method: 'GET' });
  return res.data;
}

/** POST /customers */
export async function createCustomer({ name, age, pan }) {
  const payload = { name, age, pan };
  const res = await callWithMonitor('/customers', { method: 'POST', body: payload });
  return res.data;
}

/** POST /proposals */
export async function createProposal({
  customerId,
  sumAssured,
  policyTerm,
  premium,
  nominee,
  paymentFrequency,
}) {
  const payload = {
    customerId,
    sumAssured,
    term: policyTerm,
    premium,
    nominee,
    paymentFrequency,
  };
  const res = await callWithMonitor('/proposals', { method: 'POST', body: payload });
  return res.data;
}

/** GET /proposals/{id} */
export async function getProposal(id) {
  const res = await callWithMonitor(`/proposals/${encodeURIComponent(id)}`, { method: 'GET' });
  return res.data;
}

/** POST /proposals/{id}/submit */
export async function submitProposal(id) {
  const res = await callWithMonitor(`/proposals/${encodeURIComponent(id)}/submit`, { method: 'POST' });
  return res.data;
}

/** GET /audits */
export async function getAudits() {
  const res = await callWithMonitor('/audits', { method: 'GET' });
  return res.data;
}
