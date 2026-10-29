const NETWORK_ERROR = 'Unable to reach the server. Check your connection and try again.';

export async function request(path, options = {}) {
  let response;
  try {
    response = await fetch(path, {
      credentials: 'include',
      ...options,
      headers: {
        Accept: 'application/json',
        ...(options.body ? { 'Content-Type': 'application/json' } : {}),
        ...options.headers,
      },
    });
  } catch {
    throw new Error(NETWORK_ERROR);
  }

  const raw = await response.text();
  let data = null;

  if (raw) {
    try {
      data = JSON.parse(raw);
    } catch {
      data = raw;
    }
  }

  if (!response.ok) {
    // The backend returns { timestamp, status, error, message, path } for every error.
    const message = typeof data === 'string' ? data : data?.message;
    const error = new Error(message || `Request failed with status ${response.status}`);
    error.status = response.status;
    throw error;
  }

  return data;
}
