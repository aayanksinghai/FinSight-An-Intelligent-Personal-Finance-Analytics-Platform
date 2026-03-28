import type { PasswordPolicyResponse } from '../types/api';

export const DEFAULT_PASSWORD_POLICY: PasswordPolicyResponse = {
  regex: '^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$',
  hint: 'Use at least 8 characters with uppercase, lowercase, number, and special character.',
  minLength: 8,
};

export function getPasswordPolicyHint(policy?: PasswordPolicyResponse | null): string {
  return policy?.hint ?? DEFAULT_PASSWORD_POLICY.hint;
}

export function isStrongPassword(
  password: string,
  policy: PasswordPolicyResponse = DEFAULT_PASSWORD_POLICY,
): boolean {
  try {
    const regex = new RegExp(policy.regex ?? DEFAULT_PASSWORD_POLICY.regex);
    return regex.test(password ?? '');
  } catch {
    const fallback = new RegExp(DEFAULT_PASSWORD_POLICY.regex);
    return fallback.test(password ?? '');
  }
}
