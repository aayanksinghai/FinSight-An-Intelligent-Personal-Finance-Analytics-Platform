export const DEFAULT_PASSWORD_POLICY = {
  regex: '^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$',
  hint: 'Use at least 8 characters with uppercase, lowercase, number, and special character.',
  minLength: 8
};

export function getPasswordPolicyHint(policy) {
  return policy?.hint || DEFAULT_PASSWORD_POLICY.hint;
}

export function isStrongPassword(password, policy = DEFAULT_PASSWORD_POLICY) {
  try {
    const regex = new RegExp(policy?.regex || DEFAULT_PASSWORD_POLICY.regex);
    return regex.test(password || '');
  } catch {
    const fallbackRegex = new RegExp(DEFAULT_PASSWORD_POLICY.regex);
    return fallbackRegex.test(password || '');
  }
}

