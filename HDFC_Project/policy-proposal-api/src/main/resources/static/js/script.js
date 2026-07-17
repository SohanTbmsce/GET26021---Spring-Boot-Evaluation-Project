/**
 * script.js
 * Single-page enterprise dashboard logic.
 * Vanilla JS + Fetch API integration to Spring Boot REST APIs.
 */

import {
  $,
  qs,
  qsa,
  debounce,
  formatNumber,
  formatMoney,
  safeTrim,
  clearFieldErrors,
  setFieldError,
  showToast,
  nowDateTime,
  showSpinner,
  hideSpinner,
  setText,
  setHidden,
} from './utils.js';

import {
  checkBackend,
  getCustomers,
  createCustomer,
  createProposal,
  getProposal,
  submitProposal,
  getAudits,
} from './api.js';

function setBackendState({ connected, error } = {}) {
  const dot = qs('#backendDot');
  const stateBadge = qs('#backendState');
  const backendText = qs('#backendText');

  if (!dot || !stateBadge || !backendText) return;

  if (connected) {
    dot.style.background = 'var(--success)';
    stateBadge.classList.remove('badge--danger');
    stateBadge.classList.add('badge--success');
    stateBadge.textContent = 'Connected';
    backendText.textContent = 'Backend Status';
  } else {
    dot.style.background = 'var(--danger)';
    stateBadge.classList.remove('badge--success');
    stateBadge.classList.add('badge--danger');
    stateBadge.textContent = 'Disconnected';
    backendText.textContent = 'Backend Status';
    if (error) {
      // don't spam toast: just reflect last error subtly
      stateBadge.title = error;
    }
  }
}

function updateHeaderDateTime() {
  const { date, time } = nowDateTime();
  setText('headerDate', date);
  setText('headerTime', time);
}

function setActiveSidebar(targetId) {
  const links = qsa('#sidebarNav .nav-link');
  links.forEach(a => {
    if (a.dataset.target === targetId) a.classList.add('active');
    else a.classList.remove('active');
  });
}

function scrollToSection(targetId) {
  const el = qs(`#${targetId}`);
  if (!el) return;
  el.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

function getTableFilteredRows(rows, query) {
  const q = query.toLowerCase();
  if (!q) return rows;
  return rows.filter(r => r._searchText.includes(q));
}

function extractCustomerRows(customers) {
  // Shape is determined by backend CustomerResponse/Customer model; we render safely.
  return (customers || []).map(c => {
    const customerId = c.customerId ?? c.id ?? c.customer_id ?? c.customerID ?? '';
    const name = c.name ?? '';
    const age = c.age ?? '';
    const pan = c.pan ?? c.PAN ?? '';

    const row = { customerId, name, age, pan };
    row._searchText = `${customerId} ${name} ${pan}`.toLowerCase();
    return row;
  });
}

function extractAuditRows(audits) {
  return (audits || []).map(a => {
    const auditId = a.auditId ?? a.id ?? a.audit_id ?? '';
    const action = a.action ?? '';
    const message = a.message ?? a.msg ?? '';
    const timestamp = a.timestamp ?? a.timeStamp ?? a.createdAt ?? a.dateTime ?? '';
    return {
      auditId,
      action,
      message,
      timestamp,
      _searchText: `${auditId} ${action} ${message}`.toLowerCase(),
    };
  });
}

function extractProposalDetails(p) {
  const customerId = p.customerId ?? p.customer_id ?? '';
  const proposalId = p.proposalId ?? p.id ?? p.proposal_id ?? '';
  const sumAssured = p.sumAssured ?? p.sum_assured ?? '';
  const premium = p.premium ?? '';
  const policyTerm = p.policyTerm ?? p.term ?? '';
  const nominee = p.nominee ?? '';
  const paymentFrequency = p.paymentFrequency ?? p.payment_frequency ?? '';
  const status = p.status ?? p.proposalStatus ?? '';
  const policyNumber = p.policyNumber ?? p.policy_no ?? p.policyNumberGenerated ?? '';

  return {
    customerId,
    proposalId,
    sumAssured,
    premium,
    policyTerm,
    nominee,
    paymentFrequency,
    status,
    policyNumber,
  };
}

function statusBadge(status) {
  const s = safeTrim(status).toUpperCase();
  let cls = 'badge--neutral';
  if (s.includes('SUBMIT')) cls = 'badge--success';
  else if (s.includes('DRAFT')) cls = 'badge--warning';
  else if (s.includes('APPROV') || s.includes('ACTIVE')) cls = 'badge--success';

  return `<span class="badge ${cls}">${escapeHtml(s || '—')}</span>`;
}

function escapeHtml(str) {
  return (str ?? '')
    .toString()
    .replaceAll('&', '&amp;')
    .replaceAll('<', '<')
    .replaceAll('>', '>')
    .replaceAll('"', '"')
    .replaceAll("'", '&#039;');
}

function setTimelineStep(stepKey) {
  // stepKey: customer|proposal|validation|submission|policy|audit
  const stepMap = {
    customer: 'customer',
    proposal: 'proposal',
    validation: 'validation',
    submission: 'submission',
    policy: 'policy',
    audit: 'audit',
  };

  const key = stepMap[stepKey];
  if (!key) return;

  const steps = qsa('.workflow__step');
  steps.forEach(s => s.classList.remove('completed'));

  // Mark completed up to the key
  const order = ['customer', 'proposal', 'validation', 'submission', 'policy', 'audit'];
  const idx = order.indexOf(key);
  order.forEach((k, i) => {
    if (i <= idx) {
      const el = qs(`.workflow__step[data-step="${k}"]`);
      if (el) el.classList.add('completed');
      const icon = qs(`.workflow__step[data-step="${k}"] .workflow__icon`);
      if (icon) icon.textContent = '✓';
    }
  });
}

function resetTimeline() {
  qsa('.workflow__step').forEach(s => {
    s.classList.remove('completed');
    const icon = qs('.workflow__icon', s);
    if (icon) icon.textContent = s.dataset.step === 'customer' ? '1' :
      s.dataset.step === 'proposal' ? '2' :
        s.dataset.step === 'validation' ? '3' :
          s.dataset.step === 'submission' ? '4' :
            s.dataset.step === 'policy' ? '5' : '6';
  });
}

function updateCreateProposalCards(proposal) {
  const p = proposal ? extractProposalDetails(proposal) : null;

  setText('createdProposalId', p?.proposalId ?? '—');
  const statusWrap = qs('#createdProposalStatus');
  if (statusWrap) statusWrap.innerHTML = statusBadge(p?.status);

  setText('createdProposalPolicyNumber', p?.policyNumber ?? '—');

  // If status indicates policy number generated, reflect timeline policy/audit completed.
  // Otherwise default to earlier state.
  const s = (p?.status ?? '').toString().toUpperCase();
  if (s.includes('SUBMITTED') || p?.policyNumber) setTimelineStep('audit');
  else if (p?.proposalId) setTimelineStep('proposal');
}

function updateSearchedProposalCards(proposal) {
  const p = proposal ? extractProposalDetails(proposal) : null;

  setText('detailCustomerId', p?.customerId ?? '—');
  setText('detailProposalId', p?.proposalId ?? '—');
  setText('detailSumAssured', p?.sumAssured ? formatMoney(p.sumAssured) : '—');
  setText('detailPremium', p?.premium ? formatMoney(p.premium) : '—');
  setText('detailTerm', p?.policyTerm ?? '—');
  setText('detailNominee', p?.nominee ?? '—');
  setText('detailPaymentFrequency', p?.paymentFrequency ?? '—');

  const statusEl = qs('#detailStatus');
  if (statusEl) statusEl.innerHTML = statusBadge(p?.status);

  setText('detailPolicyNumber', p?.policyNumber ?? '—');
}

function updateSubmitResultCards(proposal) {
  const p = proposal ? extractProposalDetails(proposal) : null;
  setText('submitPolicyNumber', p?.policyNumber ?? '—');
  const statusEl = qs('#submitStatus');
  if (statusEl) statusEl.innerHTML = statusBadge(p?.status);

  const timelineEl = qs('#submitTimelineStatus');
  if (timelineEl) timelineEl.innerHTML = statusBadge(p?.status);

  const s = (p?.status ?? '').toString().toUpperCase();
  if (s.includes('SUBMITTED') || p?.policyNumber) setTimelineStep('audit');
  else if (p?.proposalId) setTimelineStep('submission');
}

function setEmptyState(elId, empty) {
  const el = qs(`#${elId}`);
  if (!el) return;
  el.style.display = empty ? 'block' : 'none';
}

async function refreshStatsAndTimelineFromLatest() {
  // No dedicated stats endpoint provided; compute from lists.
  const [customers, audits] = await Promise.all([
    getCustomers().catch(() => []),
    getAudits().catch(() => []),
  ]);

  const customersCount = Array.isArray(customers) ? customers.length : 0;

  const auditsCount = Array.isArray(audits) ? audits.length : 0;

  setText('statTotalCustomers', customersCount);
  setText('statAuditRecords', auditsCount);

  // For proposals totals we have only GET /proposals/{id} and no list endpoint.
  // We will approximate using audit/action messages only.
  // If backend has additional data in audits, it will reflect.
  // Otherwise we keep proposals cards updated after create/search/submit actions.
  // Here: keep what script already set, but ensure audit count is live.
}

async function refreshCustomersTable() {
  const tbody = qs('#customersTbody');
  if (!tbody) return;

  const searchInput = qs('#customerSearch');
  const query = safeTrim(searchInput?.value);

  const all = await getCustomers().catch(() => []);
  const rows = extractCustomerRows(all);

  const filtered = getTableFilteredRows(
    rows,
    query
  );

  // Pagination (client-side)
  const pageInfo = qs('#customersPageInfo');
  const prevBtn = qs('#customersPrev');
  const nextBtn = qs('#customersNext');

  const pageSize = 8;
  const total = filtered.length;
  const totalPages = Math.max(1, Math.ceil(total / pageSize));

  let page = window.__custPage || 1;
  page = Math.min(page, totalPages);

  window.__custPage = page;

  const start = (page - 1) * pageSize;
  const end = start + pageSize;
  const pageRows = filtered.slice(start, end);

  tbody.innerHTML = '';
  pageRows.forEach(r => {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>${escapeHtml(r.customerId)}</td>
      <td>${escapeHtml(r.name)}</td>
      <td>${escapeHtml(r.age)}</td>
      <td>${escapeHtml(r.pan)}</td>
    `;
    tbody.appendChild(tr);
  });

  setEmptyState('customersEmpty', filtered.length === 0);

  pageInfo.textContent = `Page ${page} of ${totalPages}`;
  prevBtn.disabled = page <= 1;
  nextBtn.disabled = page >= totalPages;
}

async function refreshAuditsTable() {
  const tbody = qs('#auditsTbody');
  if (!tbody) return;

  const searchInput = qs('#auditSearch');
  const sortBy = qs('#auditSortBy')?.value || 'timestamp_desc';
  const query = safeTrim(searchInput?.value);

  const all = await getAudits().catch(() => []);
  const rows = extractAuditRows(all);

  const filtered = getTableFilteredRows(rows, query);

  const sorters = {
    'timestamp_desc': (a, b) => String(b.timestamp).localeCompare(String(a.timestamp)),
    'timestamp_asc': (a, b) => String(a.timestamp).localeCompare(String(b.timestamp)),
    'auditId_asc': (a, b) => String(a.auditId).localeCompare(String(b.auditId)),
    'auditId_desc': (a, b) => String(b.auditId).localeCompare(String(a.auditId)),
  };

  filtered.sort(sorters[sortBy] || sorters.timestamp_desc);

  tbody.innerHTML = '';
  filtered.forEach(r => {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>${escapeHtml(r.auditId)}</td>
      <td>${escapeHtml(r.action)}</td>
      <td>${escapeHtml(r.message)}</td>
      <td>${escapeHtml(r.timestamp)}</td>
    `;
    tbody.appendChild(tr);
  });

  setEmptyState('auditsEmpty', filtered.length === 0);

  setText('statAuditRecords', all?.length ?? 0);
}

async function initDateTime() {
  updateHeaderDateTime();
  setInterval(updateHeaderDateTime, 1000);
}

function initNavigation() {
  const navLinks = qsa('#sidebarNav .nav-link');
  navLinks.forEach(a => {
    a.addEventListener('click', e => {
      e.preventDefault();
      const target = a.dataset.target;
      if (!target) return;
      setActiveSidebar(target);
      scrollToSection(target);
    });
  });

  // Highlight active section on scroll
  const sections = qsa('.section[id]');
  const observer = new IntersectionObserver(entries => {
    const visible = entries
      .filter(e => e.isIntersecting)
      .sort((a, b) => (b.intersectionRatio ?? 0) - (a.intersectionRatio ?? 0))[0];
    if (!visible) return;

    const id = visible.target.id;
    setActiveSidebar(id);
  }, { root: null, threshold: [0.2, 0.35, 0.5] });

  sections.forEach(s => observer.observe(s));
}

function validatePAN(pan) {
  const v = safeTrim(pan).toUpperCase();
  return /^[A-Z]{5}[0-9]{4}[A-Z]{1}$/.test(v);
}

function wireCustomerModule() {
  const form = qs('#customerForm');
  const resetBtn = qs('#btnResetCustomer');
  const saveBtn = qs('#btnSaveCustomer');

  if (!form) return;

  resetBtn?.addEventListener('click', () => {
    form.reset();
    clearFieldErrors(form);
  });

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    clearFieldErrors(form);

    const name = safeTrim(qs('#custName')?.value);
    const age = Number(qs('#custAge')?.value);
    const pan = safeTrim(qs('#custPan')?.value).toUpperCase();

    let ok = true;

    if (!name) { setFieldError(form, 'name', 'Name is required.'); ok = false; }
    if (!Number.isFinite(age) || age <= 0) { setFieldError(form, 'age', 'Age must be a valid positive number.'); ok = false; }
    if (!validatePAN(pan)) { setFieldError(form, 'pan', 'PAN must be in format ABCDE1234F.'); ok = false; }

    if (!ok) return;

    saveBtn.disabled = true;
    try {
      const result = await createCustomer({ name, age, pan });
      showToast({ title: 'Customer Saved', message: `Customer created successfully.`, type: 'success' });

      window.__custPage = 1;
      await refreshCustomersTable();
      await refreshStatsAndTimelineFromLatest();
    } catch (err) {
      showToast({ title: 'Save Failed', message: err?.message || 'Could not save customer.', type: 'error' });
    } finally {
      saveBtn.disabled = false;
    }
  });
}

function wireProposalCreation() {
  const form = qs('#proposalForm');
  const btn = qs('#btnCreateProposal');
  if (!form) return;

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    clearFieldErrors(form);

    const customerId = Number(qs('#propCustomerId')?.value);
    const sumAssured = Number(qs('#propSumAssured')?.value);
    const policyTerm = Number(qs('#propTerm')?.value);
    const premium = Number(qs('#propPremium')?.value);
    const nominee = safeTrim(qs('#propNominee')?.value);
    const paymentFrequency = qs('#propPaymentFreq')?.value;

    let ok = true;

    if (!Number.isFinite(customerId) || customerId <= 0) { setFieldError(form, 'customerId', 'Customer ID must be a positive number.'); ok = false; }
    if (!Number.isFinite(sumAssured) || sumAssured <= 0) { setFieldError(form, 'sumAssured', 'Sum Assured must be a positive number.'); ok = false; }
    if (!Number.isFinite(policyTerm) || policyTerm <= 0) { setFieldError(form, 'policyTerm', 'Policy Term must be a positive number.'); ok = false; }
    if (!Number.isFinite(premium) || premium < 0) { setFieldError(form, 'premium', 'Premium must be a non-negative number.'); ok = false; }
    if (!nominee) { setFieldError(form, 'nominee', 'Nominee is required.'); ok = false; }
    if (!paymentFrequency) { setFieldError(form, 'paymentFrequency', 'Select payment frequency.'); ok = false; }

    if (!ok) return;

    btn.disabled = true;
    try {
      const result = await createProposal({ customerId, sumAssured, policyTerm, premium, nominee, paymentFrequency });
      showToast({ title: 'Proposal Created', message: 'Proposal created successfully.', type: 'success' });

      // Update create-result cards
      updateCreateProposalCards(result);

      // Best-effort: refresh audits after create (if backend logs it)
      await refreshAuditsTable().catch(() => {});
      // Total proposals card: no list endpoint—update using created proposal status if available.
      window.__lastProposalCreated = result;
      if (result) {
        const prevTotal = Number(qs('#statTotalProposals')?.textContent || '0');
        const nextTotal = Number.isFinite(prevTotal) ? prevTotal + 1 : 1;
        setText('statTotalProposals', nextTotal);
        const st = (result.status ?? result.proposalStatus ?? '').toString().toUpperCase();
        if (st.includes('SUBMITTED')) {
          const prevSub = Number(qs('#statSubmittedProposals')?.textContent || '0');
          setText('statSubmittedProposals', prevSub + 1);
        }
      }
    } catch (err) {
      showToast({ title: 'Create Proposal Failed', message: err?.message || 'Could not create proposal.', type: 'error' });
    } finally {
      btn.disabled = false;
    }
  });
}

function wireProposalSearch() {
  const btn = qs('#btnSearchProposal');
  const input = qs('#searchProposalId');
  if (!btn || !input) return;

  btn.addEventListener('click', async () => {
    const id = safeTrim(input.value);
    if (!id) {
      showToast({ title: 'Missing Proposal ID', message: 'Enter a Proposal ID to search.', type: 'error' });
      return;
    }

    try {
      showSpinner();
      const result = await getProposal(id);
      updateSearchedProposalCards(result);
      setHidden(qs('#proposalDetailsEmpty'), true);
      showToast({ title: 'Proposal Found', message: 'Proposal details loaded successfully.', type: 'success' });

      // Keep latest for submission timeline updates
      window.__lastProposalSearched = result;
    } catch (err) {
      setHidden(qs('#proposalDetailsEmpty'), false);
      showToast({ title: 'Search Failed', message: err?.message || 'Could not fetch proposal.', type: 'error' });
    } finally {
      hideSpinner();
    }
  });
}

function wireSubmitProposal() {
  const btn = qs('#btnSubmitProposal');
  const input = qs('#submitProposalId');
  if (!btn || !input) return;

  btn.addEventListener('click', async () => {
    const id = safeTrim(input.value);
    if (!id) {
      showToast({ title: 'Missing Proposal ID', message: 'Enter a Proposal ID to submit.', type: 'error' });
      return;
    }

    btn.disabled = true;
    try {
      const result = await submitProposal(id);
      showToast({ title: 'Proposal Submitted', message: 'Policy generated successfully (if applicable).', type: 'success' });

      updateSubmitResultCards(result);

      // Refresh stats + audits after submit
      await Promise.all([
        refreshAuditsTable(),
        refreshStatsAndTimelineFromLatest(),
      ]).catch(() => {});

      // Attempt to bump proposal counters
      setText('statSubmittedProposals', Number(qs('#statSubmittedProposals')?.textContent || '0') + 1);
      setText('statTotalProposals', Number(qs('#statTotalProposals')?.textContent || '0') + 1);
    } catch (err) {
      showToast({ title: 'Submit Failed', message: err?.message || 'Could not submit proposal.', type: 'error' });
    } finally {
      btn.disabled = false;
    }
  });
}

function wireCustomerSearchAndPagination() {
  const search = qs('#customerSearch');
  const prevBtn = qs('#customersPrev');
  const nextBtn = qs('#customersNext');

  if (!search) return;

  search.addEventListener('input', debounce(async () => {
    window.__custPage = 1;
    await refreshCustomersTable();
  }, 180));

  prevBtn?.addEventListener('click', async () => {
    window.__custPage = Math.max(1, (window.__custPage || 1) - 1);
    await refreshCustomersTable();
  });

  nextBtn?.addEventListener('click', async () => {
    window.__custPage = (window.__custPage || 1) + 1;
    await refreshCustomersTable();
  });
}

function wireAuditSearchAndSort() {
  const search = qs('#auditSearch');
  const sort = qs('#auditSortBy');

  if (search) {
    search.addEventListener('input', debounce(async () => {
      await refreshAuditsTable();
    }, 180));
  }
  if (sort) {
    sort.addEventListener('change', async () => {
      await refreshAuditsTable();
    });
  }
}

function bootstrapApiMonitor() {
  // api.js updates window.__apiMonitor if available; provide handlers here
  const monitor = {
    beforeApiCall: (url, method) => {
      setText('monitorLastApi', `${method} ${url}`);
      setText('monitorHttpStatus', '—');
      setText('monitorResponseTime', '—');
    },
    afterApiCall: (url, method, httpStatus, elapsedMs) => {
      setText('monitorHttpStatus', httpStatus ?? '—');
      setText('monitorResponseTime', elapsedMs ? `${Math.round(elapsedMs)} ms` : '—');
      setText('monitorLastApi', `${method} ${url}`);
    }
  };

  window.__apiMonitor = monitor;
}

async function initAll() {
  // Spinner hidden by default
  hideSpinner();
  resetTimeline();

  bootstrapApiMonitor();
  initDateTime();
  initNavigation();

  // Setup empty defaults
  setHidden(qs('#proposalDetailsEmpty'), false);
  setHidden(qs('#customersEmpty'), true);
  setHidden(qs('#auditsEmpty'), true);

  // Initial API connectivity check
  const health = await checkBackend();
  setBackendState(health);

  // Update dashboard stats + tables
  try {
    // customers list + table
    await refreshCustomersTable();
    await refreshStatsAndTimelineFromLatest();
  } catch {
    // ignore; toasts are handled elsewhere
  }

  try {
    await refreshAuditsTable();
  } catch {
    // ignore
  }

  // Wire UI modules
  wireCustomerModule();
  wireCustomerSearchAndPagination();
  wireProposalCreation();
  wireProposalSearch();
  wireSubmitProposal();
  wireAuditSearchAndSort();

  // Keep stats cards in sync when user returns to sections
  // (no additional endpoints; relies on refresh after create/submit)
}

document.addEventListener('DOMContentLoaded', () => {
  initAll().catch(() => {
    setBackendState({ connected: false, error: 'Initialization failed' });
  });
});
