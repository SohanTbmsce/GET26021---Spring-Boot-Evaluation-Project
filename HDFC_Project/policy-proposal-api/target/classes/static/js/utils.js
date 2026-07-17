/**
 * utils.js
 * Reusable helpers for the dashboard UI.
 */

export function $(id) {
  return document.getElementById(id);
}

export function qs(selector, root = document) {
  return root.querySelector(selector);
}

export function qsa(selector, root = document) {
  return Array.from(root.querySelectorAll(selector));
}

export function clamp(n, min, max) {
  return Math.max(min, Math.min(max, n));
}

export function formatNumber(n) {
  const num = Number(n);
  if (!Number.isFinite(num)) return '—';
  return num.toLocaleString(undefined, { maximumFractionDigits: 0 });
}

export function formatMoney(n) {
  const num = Number(n);
  if (!Number.isFinite(num)) return '—';
  return num.toLocaleString(undefined, { style: 'currency', currency: 'INR', maximumFractionDigits: 0 });
}

export function safeTrim(s) {
  return (s ?? '').toString().trim();
}

export function debounce(fn, delay = 250) {
  let t = null;
  return (...args) => {
    clearTimeout(t);
    t = setTimeout(() => fn(...args), delay);
  };
}

export function setText(idOrEl, text) {
  if (!idOrEl) return;
  if (typeof idOrEl === 'string') {
    const el = document.getElementById(idOrEl);
    if (el) el.textContent = text;
  } else {
    idOrEl.textContent = text;
  }
}

export function setHidden(el, hidden) {
  if (!el) return;
  el.hidden = !!hidden;
}

export function nowDateTime() {
  const d = new Date();
  const date = d.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: '2-digit' });
  const time = d.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit', second: '2-digit' });
  return { date, time };
}

/**
 * Simple Toast system.
 * index.html contains #toastHost
 */
export function showToast({ title = 'Update', message = '', type = 'success', ttl = 3500 } = {}) {
  const host = qs('#toastHost');
  if (!host) return;

  const toast = document.createElement('div');
  toast.className = `toast toast--${type}`;

  const safeTitle = safeTrim(title) || 'Update';
  const safeMsg = safeTrim(message);

  toast.innerHTML = `
    <div class="toast__top">
      <div class="toast__title">${escapeHtml(safeTitle)}</div>
      <button class="toast__close" type="button" aria-label="Close toast">✕</button>
    </div>
    <div class="toast__msg">${escapeHtml(safeMsg)}</div>
  `;

  const closeBtn = qs('.toast__close', toast);
  closeBtn.addEventListener('click', () => toast.remove());

  host.appendChild(toast);

  setTimeout(() => {
    if (toast.isConnected) toast.remove();
  }, ttl);
}

export function escapeHtml(str) {
  return str
    .replaceAll('&', '&amp;')
    .replaceAll('<', '<')
    .replaceAll('>', '>')
    .replaceAll('"', '"')
    .replaceAll("'", '&#039;');
}

/**
 * Field validation UX.
 * Inputs have a corresponding .field-error[data-error-for="fieldName"] element.
 */
export function clearFieldErrors(formEl) {
  if (!formEl) return;
  const errors = qsa('.field-error', formEl);
  errors.forEach(e => (e.textContent = ''));
}

export function setFieldError(formEl, fieldName, message) {
  if (!formEl) return;
  const err = qs(`.field-error[data-error-for="${fieldName}"]`, formEl);
  if (err) err.textContent = message;
}

/**
 * Global spinner UX.
 * index.html contains #globalSpinner
 */
export function showSpinner() {
    const spinner = document.getElementById("globalSpinner");
    if (!spinner) return;

    spinner.hidden = false;
    spinner.style.display = "grid";
}

export function hideSpinner() {
    const spinner = document.getElementById("globalSpinner");
    if (!spinner) return;

    spinner.hidden = true;
    spinner.style.display = "none";
}
