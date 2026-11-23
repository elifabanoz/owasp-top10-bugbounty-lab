import type { Profile } from './types';

export async function api<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`/api${path}`, { credentials: 'include', ...init });
  if (!response.ok) {
    let message = `Request failed (${response.status})`;
    try { message = (await response.json()).error ?? message; } catch { /* empty response */ }
    throw new Error(message);
  }
  const body = await response.text();
  return body ? JSON.parse(body) as T : undefined as T;
}

export async function write<T>(path: string, init: RequestInit, profile: Profile): Promise<T> {
  const headers = new Headers(init.headers);
  if (profile === 'secure') {
    const csrf = await api<{ token: string }>('/auth/csrf');
    headers.set('X-XSRF-TOKEN', csrf.token);
  }
  return api<T>(path, { ...init, headers });
}
