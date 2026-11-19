/** Access JWT stays in memory. The refresh token is held in a server-issued HttpOnly cookie. */
export const session = { token: null, user: null };
let refreshing = null;
export function clearSession() { session.token = null; session.user = null; }
function accept(data) { session.token = data.accessToken; session.user = data.user; return data; }
async function raw(path, options = {}) {
  const headers = new Headers(options.headers || {});
  if (session.token) headers.set('Authorization', `Bearer ${session.token}`);
  if (options.body && !(options.body instanceof FormData)) headers.set('Content-Type', 'application/json');
  const response = await fetch(path, { ...options, headers, credentials: 'same-origin' });
  return response;
}
async function failure(response) {
  let data; try { data = await response.json(); } catch { data = { message: `Ошибка запроса (${response.status})` }; }
  const fields = Object.entries(data.fields || {}).map(([k, v]) => `${k}: ${v}`).join('; ');
  const error = new Error([data.message, fields].filter(Boolean).join('. '));
  error.status = response.status; error.code = data.code;
  error.correlationId = data.correlationId || response.headers.get('X-Correlation-ID');
  throw error;
}
export async function refresh() {
  if (!refreshing) refreshing = (async () => {
    const r = await raw('/api/auth/refresh', { method: 'POST' });
    if (!r.ok) { clearSession(); return failure(r); }
    return accept(await r.json());
  })().finally(() => { refreshing = null; });
  return refreshing;
}
export async function login(email, password) {
  const r = await raw('/api/auth/login', { method: 'POST', body: JSON.stringify({ email, password, clientType: 'WEB' }) });
  if (!r.ok) return failure(r);
  return accept(await r.json());
}
export async function api(path, { method = 'GET', body, headers, blob = false } = {}) {
  const options = { method, headers, body: body instanceof FormData ? body : body === undefined ? undefined : JSON.stringify(body) };
  let r = await raw(path, options);
  if (r.status === 401 && session.token) {
    try { await refresh(); r = await raw(path, options); }
    catch (e) { window.dispatchEvent(new Event('session-expired')); throw e; }
  }
  if (!r.ok) {
    if (r.status === 401) { clearSession(); window.dispatchEvent(new Event('session-expired')); }
    return failure(r);
  }
  if (r.status === 204) return null;
  return blob ? r.blob() : r.json();
}
export async function logout() {
  try { const r = await raw('/api/auth/logout', { method: 'POST' }); if (!r.ok) await failure(r); }
  finally { clearSession(); }
}
export function query(values) {
  const q = new URLSearchParams();
  for (const [k, v] of Object.entries(values)) if (v !== '' && v !== null && v !== undefined) q.set(k, v);
  return q.toString();
}
export async function download(path, filename) {
  const blob = await api(path, { blob: true }); downloadBlob(blob, filename);
}
export function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob); const a = document.createElement('a');
  a.href = url; a.download = filename; document.body.append(a); a.click(); a.remove(); setTimeout(() => URL.revokeObjectURL(url), 1000);
}
