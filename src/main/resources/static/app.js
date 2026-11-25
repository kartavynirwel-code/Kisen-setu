'use strict';

// All text from the server (names, messages, reasons) is inserted with textContent, never as HTML.

const TOKEN_KEY = 'kisansetu.token';
const USER_KEY = 'kisansetu.user';
const ROLE_KEY = 'kisansetu.role';
const PAGE_SIZE = 20;

const $ = (id) => document.getElementById(id);

const state = { page: 0, totalPages: 1, batchId: null };

function el(tag, text, className) {
  const node = document.createElement(tag);
  if (text !== undefined && text !== null) node.textContent = String(text);
  if (className) node.className = className;
  return node;
}

function token() { return sessionStorage.getItem(TOKEN_KEY); }
function isAdmin() { return sessionStorage.getItem(ROLE_KEY) === 'BANK_ADMIN'; }

class ApiError extends Error {
  constructor(status, message) { super(message); this.status = status; }
}

async function api(path, options = {}) {
  const headers = Object.assign({}, options.headers || {});
  if (token()) headers.Authorization = 'Bearer ' + token();
  const response = await fetch(path, Object.assign({}, options, { headers }));
  if (response.status === 401 && path !== '/api/auth/login') {
    logout();
    throw new ApiError(401, 'Your session has ended. Please log in again.');
  }
  if (!response.ok) {
    let message = 'Request failed (' + response.status + ')';
    try {
      const body = await response.json();
      if (body && body.detail) message = body.detail;
    } catch (ignored) { /* body was not JSON */ }
    throw new ApiError(response.status, message);
  }
  return response;
}

/* ---------- session / navigation ---------- */

function showLoggedIn() {
  $('login-page').classList.add('hidden');
  $('app-pages').classList.remove('hidden');
  $('userbar').classList.remove('hidden');
  $('whoami').textContent = sessionStorage.getItem(USER_KEY) + ' (' + sessionStorage.getItem(ROLE_KEY) + ')';
  document.querySelectorAll('.admin-only').forEach((n) => n.classList.toggle('hidden', !isAdmin()));
  showTab('upload');
}

function logout() {
  sessionStorage.clear();
  $('app-pages').classList.add('hidden');
  $('userbar').classList.add('hidden');
  $('login-page').classList.remove('hidden');
  $('password').value = '';
}

function showTab(name) {
  $('upload-page').classList.toggle('hidden', name !== 'upload');
  $('claims-page').classList.toggle('hidden', name !== 'claims');
  $('tab-upload').classList.toggle('active', name === 'upload');
  $('tab-claims').classList.toggle('active', name === 'claims');
  if (name === 'claims') loadClaims().catch(showClaimsError);
}

async function onLogin(event) {
  event.preventDefault();
  $('login-error').textContent = '';
  try {
    const response = await api('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: $('username').value, password: $('password').value }),
    });
    const body = await response.json();
    sessionStorage.setItem(TOKEN_KEY, body.token);
    sessionStorage.setItem(USER_KEY, body.username);
    sessionStorage.setItem(ROLE_KEY, body.role);
    showLoggedIn();
  } catch (e) {
    $('login-error').textContent = e.message;
  }
}

/* ---------- upload page ---------- */

function summaryItem(label, value) {
  const wrap = el('div');
  wrap.appendChild(el('dt', label));
  wrap.appendChild(el('dd', value));
  return wrap;
}

function renderSummary(batch) {
  const list = $('summary');
  list.replaceChildren(
    summaryItem('Batch', '#' + batch.id),
    summaryItem('Rows', batch.totalRows),
    summaryItem('Valid', batch.validRows),
    summaryItem('With errors', batch.errorRows),
    summaryItem('Status', batch.status === 'COMMITTED' ? 'Saved' : 'Checked only'));
}

function renderErrors(errors) {
  const table = $('errors-table');
  const body = table.querySelector('tbody');
  body.replaceChildren();
  for (const e of errors) {
    const row = document.createElement('tr');
    row.appendChild(el('td', e.row));
    row.appendChild(el('td', e.field));
    row.appendChild(el('td', e.message));
    row.appendChild(el('td', e.suggestion || ''));
    body.appendChild(row);
  }
  table.classList.toggle('hidden', errors.length === 0);
}

async function onUpload(event) {
  event.preventDefault();
  $('upload-message').textContent = '';
  const file = $('file').files[0];
  if (!file) return;
  const form = new FormData();
  form.append('file', file);
  const dryRun = $('dry-run').checked ? 'true' : 'false';
  const submit = event.target.querySelector('button[type="submit"]');
  submit.disabled = true;
  try {
    const created = await (await api('/api/batches?dryRun=' + dryRun, { method: 'POST', body: form })).json();
    const errors = await (await api('/api/batches/' + created.id + '/errors')).json();
    state.batchId = created.id;
    renderSummary(created);
    renderErrors(errors);
    $('download-report').classList.toggle('hidden', errors.length === 0);
    $('result').classList.remove('hidden');
  } catch (e) {
    $('upload-message').textContent = e.message;
  } finally {
    submit.disabled = false;
  }
}

async function onDownloadReport() {
  if (state.batchId === null) return;
  try {
    const response = await api('/api/batches/' + state.batchId + '/error-report');
    const url = URL.createObjectURL(await response.blob());
    const link = document.createElement('a');
    link.href = url;
    link.download = 'batch-' + state.batchId + '-errors.xlsx';
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
  } catch (e) {
    $('upload-message').textContent = e.message;
  }
}

/* ---------- claims page ---------- */

function showClaimsError(e) { $('claims-message').textContent = e.message; }

function statusCell(status) {
  return el('td', status, 'status-' + status);
}

function changeControls(claim) {
  const wrap = el('div', null, 'change');
  const select = document.createElement('select');
  for (const s of ['PENDING', 'APPROVED', 'REJECTED']) {
    const option = el('option', s);
    option.value = s;
    option.selected = s === claim.status;
    select.appendChild(option);
  }
  const reason = document.createElement('input');
  reason.placeholder = 'Reason (required if rejected)';
  reason.maxLength = 500;
  reason.value = claim.rejectionReason || '';
  const save = el('button', 'Save');
  save.type = 'button';
  save.addEventListener('click', async () => {
    $('claims-message').textContent = '';
    save.disabled = true;
    try {
      await api('/api/claims/' + claim.id + '/status', {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ status: select.value, rejectionReason: select.value === 'REJECTED' ? reason.value : null }),
      });
      await loadClaims();
    } catch (e) {
      showClaimsError(e);
    } finally {
      save.disabled = false;
    }
  });
  wrap.append(select, reason, save);
  return wrap;
}

function renderClaims(data) {
  const body = $('claims-table').querySelector('tbody');
  body.replaceChildren();
  for (const c of data.items) {
    const row = document.createElement('tr');
    row.appendChild(el('td', c.id));
    row.appendChild(el('td', c.kccAccountNo));
    row.appendChild(el('td', c.farmerName));
    row.appendChild(el('td', c.claimType));
    row.appendChild(el('td', c.financialYear));
    row.appendChild(el('td', c.loanAmount));
    row.appendChild(el('td', c.claimAmount));
    row.appendChild(statusCell(c.status));
    row.appendChild(el('td', c.rejectionReason || ''));
    if (isAdmin()) {
      const cell = document.createElement('td');
      cell.appendChild(changeControls(c));
      row.appendChild(cell);
    }
    body.appendChild(row);
  }
  state.totalPages = Math.max(1, Math.ceil(data.total / data.size));
  $('page-info').textContent = data.total + ' claims, page ' + (data.page + 1) + ' of ' + state.totalPages;
  $('prev').disabled = data.page <= 0;
  $('next').disabled = data.page + 1 >= state.totalPages;
}

async function loadClaims() {
  $('claims-message').textContent = '';
  const status = $('status-filter').value;
  const query = '?page=' + state.page + '&size=' + PAGE_SIZE + (status ? '&status=' + encodeURIComponent(status) : '');
  const data = await (await api('/api/claims' + query)).json();
  renderClaims(data);
}

/* ---------- wiring ---------- */

function init() {
  $('login-form').addEventListener('submit', onLogin);
  $('logout').addEventListener('click', logout);
  $('tab-upload').addEventListener('click', () => showTab('upload'));
  $('tab-claims').addEventListener('click', () => showTab('claims'));
  $('upload-form').addEventListener('submit', onUpload);
  $('download-report').addEventListener('click', onDownloadReport);
  $('status-filter').addEventListener('change', () => { state.page = 0; loadClaims().catch(showClaimsError); });
  $('prev').addEventListener('click', () => { state.page = Math.max(0, state.page - 1); loadClaims().catch(showClaimsError); });
  $('next').addEventListener('click', () => { state.page += 1; loadClaims().catch(showClaimsError); });
  if (token()) showLoggedIn();
}

document.addEventListener('DOMContentLoaded', init);
