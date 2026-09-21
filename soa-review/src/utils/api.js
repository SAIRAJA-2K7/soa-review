const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

export async function callApi(path, options = {}) {
  const token = sessionStorage.getItem('ts-token');
  const headers = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...(options.headers || {}),
  };
  const response = await fetch(`${API_URL}${path}`, { ...options, headers });
  if (!response.ok) {
    const body = await response.text();
    throw new Error(body || `Request failed with status ${response.status}`);
  }
  if (response.status === 204) return null;

  const contentType = response.headers.get('content-type') || '';
  if (contentType.includes('application/json')) {
    return response.json();
  }

  return response.text();
}

export function normalizeUser(response) {
  return {
    id: response.id || response.email,
    name: response.name || response.email,
    email: response.email,
    role: response.role === 'ADMIN' ? 'recruiter' : 'candidate',
    verified: typeof response.verified === 'boolean' ? response.verified : false,
    onboarded: typeof response.onboarded === 'boolean' ? response.onboarded : false,
  };
}
