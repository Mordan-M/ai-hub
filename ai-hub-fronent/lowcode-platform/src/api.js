// ─── API Base ───────────────────────────────────────────────
// In dev, Vite proxy forwards /api and /lowcode to localhost:9090
// In production build, set BASE to your server origin
export const BASE = ''

export const ENDPOINTS = {
  login:       ()           => `${BASE}/ai-hub/user/login`,
  register:    ()           => `${BASE}/ai-hub/user/register`,
  logout:      ()           => `${BASE}/ai-hub/user/logout`,
  me:          ()           => `${BASE}/ai-hub/user/me`,
  apps:        ()           => `${BASE}/ai-hub/lowcode/apps`,
  app:         id           => `${BASE}/ai-hub/lowcode/apps/${id}`,
  conversations: (id, page, size) => `${BASE}/ai-hub/lowcode/apps/${id}/conversations?page=${page}&size=${size}`,
  generate:    id           => `${BASE}/ai-hub/lowcode/apps/${id}/generate`,
  taskStream:  (aId, tId)  => `${BASE}/ai-hub/lowcode/apps/${aId}/tasks/${tId}/stream`,
  taskStatus:  (aId, tId)  => `${BASE}/ai-hub/lowcode/apps/${aId}/tasks/${tId}`,
  preview:     id           => `${BASE}/lowcode/preview/${id}`,
  download:    id           => `${BASE}/ai-hub/lowcode/apps/download/${id}`,
  deploy:              id           => `${BASE}/ai-hub/lowcode/apps/deploy/${id}`,
  getGeneratedInfo:    id           => `${BASE}/ai-hub/lowcode/apps/${id}/generated-info`,
  deployedUrl:         id           => `${BASE}/lowcode/deploy/${id}/index.html`,
}

function getToken() {
  return localStorage.getItem('lf_token') || ''
}

export async function request(url, options = {}) {
  const token = getToken()
  const headers = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...(options.headers || {}),
  }
  const res = await fetch(url, { ...options, headers })
  if (options.blob) return res
  const data = await res.json()
  return data
}
