export function extractApiError(error, fallback = 'Request failed.') {
  const payload = error?.response?.data;

  if (typeof payload === 'string' && payload.trim()) {
    return payload;
  }

  if (payload?.message) {
    return payload.message;
  }

  if (payload?.error) {
    return payload.error;
  }

  if (Array.isArray(payload?.errors) && payload.errors.length > 0) {
    return payload.errors.join(', ');
  }

  return fallback;
}

