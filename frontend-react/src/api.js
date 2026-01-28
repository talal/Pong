import { auth } from './auth.js'

export async function apiFetch(input, init = {}) {
  const headers = new Headers(init.headers || {})
  const t = auth.token
  if (t) headers.set('Authorization', 'Bearer ' + t)
  return fetch(input, { ...init, headers })
}
