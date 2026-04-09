// ─── Auth ────────────────────────────────────────────────────────────────────

export interface AuthTokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}

export interface RegisterResponse {
  email: string;
  message: string;
}

export interface PasswordPolicyResponse {
  regex: string;
  hint: string;
  minLength: number;
}

export interface PasswordResetResponse {
  message: string;
  resetToken: string;
  expiresInSeconds: number;
}

// ─── Profile ─────────────────────────────────────────────────────────────────

export interface UserProfileResponse {
  email: string;
  fullName: string | null;
  city: string | null;
  ageGroup: string | null;
  monthlyIncome: number | null;
  profileConfigured: boolean;
  createdAt: string;
}

export interface UserSecurityResponse {
  email: string;
  accountCreatedAt: string;
  lastProfileUpdateAt: string | null;
  profileConfigured: boolean;
  hasPassword: boolean;
}

export interface UpdateUserProfileRequest {
  fullName?: string | null;
  city?: string | null;
  ageGroup?: string | null;
  monthlyIncome?: number | null;
}

// ─── Admin ───────────────────────────────────────────────────────────────────

export interface AdminUserResponse {
  email: string;
  role: string;
  active: boolean;
  createdAt: string;
  deactivatedAt: string | null;
}

export interface Page<T> {
  content: T[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

// ─── JWT Payload ─────────────────────────────────────────────────────────────

export interface JwtPayload {
  sub: string;
  role: string;
  iat: number;
  exp: number;
  iss: string;
}
