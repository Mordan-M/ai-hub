// ─── API Base ───────────────────────────────────────────────
// In dev, Vite proxy forwards /api and /lowcode to localhost:9090
// In production build, set BASE to your server origin
export const BASE = ''

export const ENDPOINTS = {
  login:       ()           => `${BASE}/api/v1/user/login`,
  register:    ()           => `${BASE}/api/v1/user/register`,
  logout:      ()           => `${BASE}/api/v1/user/logout`,
  me:          ()           => `${BASE}/api/v1/user/me`,
  apps:        ()           => `${BASE}/api/v1/lowcode/apps`,
  app:         id           => `${BASE}/api/v1/lowcode/apps/${id}`,
  conversations: (id, page, size) => `${BASE}/api/v1/lowcode/apps/${id}/conversations?page=${page}&size=${size}`,
  generate:    id           => `${BASE}/api/v1/lowcode/apps/${id}/generate`,
  taskStream:  (aId, tId)  => `${BASE}/api/v1/lowcode/apps/${aId}/tasks/${tId}/stream`,
  taskStatus:  (aId, tId)  => `${BASE}/api/v1/lowcode/apps/${aId}/tasks/${tId}`,
  preview:     id           => `${BASE}/lowcode/preview/${id}`,
  download:    id           => `${BASE}/lowcode/preview/download/${id}`,
  deploy:              id           => `${BASE}/api/v1/lowcode/apps/deploy/${id}`,
  getGeneratedInfo:    id           => `${BASE}/api/v1/lowcode/apps/${id}/generated-info`,
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
