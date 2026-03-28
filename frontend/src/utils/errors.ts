import type { AxiosError } from 'axios';

interface ApiErrorBody {
  message?: string;
  error?: string;
  errors?: string[];
}

export function extractApiError(error: unknown, fallback = 'Request failed.'): string {
  const axiosError = error as AxiosError<ApiErrorBody | string>;
  const payload = axiosError?.response?.data;

  if (typeof payload === 'string' && (payload as string).trim()) {
    return payload as string;
  }

  if (typeof payload === 'object' && payload !== null) {
    const obj = payload as ApiErrorBody;
    if (obj.message) return obj.message;
    if (obj.error) return obj.error;
    if (Array.isArray(obj.errors) && obj.errors.length > 0) {
      return obj.errors.join(', ');
    }
  }

  return fallback;
}
