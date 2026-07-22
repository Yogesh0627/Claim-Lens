/**
 * Token persistence. localStorage is the source of truth for the axios layer (interceptors read it
 * synchronously); the auth slice mirrors it for the UI. Keep the two in sync via setTokens/clearTokens.
 */

const ACCESS_KEY = "cl.accessToken";
const REFRESH_KEY = "cl.refreshToken";
// Impersonation: the platform admin's own tokens are stashed here while acting inside a tenant.
const IMP_BACKUP_KEY = "cl.imp.backup";
const IMP_TENANT_KEY = "cl.imp.tenant";

export interface StoredTokens {
  accessToken: string;
  refreshToken: string;
}

const isBrowser = typeof window !== "undefined";

export function getAccessToken(): string | null {
  return isBrowser ? window.localStorage.getItem(ACCESS_KEY) : null;
}

export function getRefreshToken(): string | null {
  return isBrowser ? window.localStorage.getItem(REFRESH_KEY) : null;
}

export function getTokens(): StoredTokens | null {
  const accessToken = getAccessToken();
  const refreshToken = getRefreshToken();
  if (!accessToken || !refreshToken) return null;
  return { accessToken, refreshToken };
}

export function setTokens(tokens: StoredTokens): void {
  if (!isBrowser) return;
  window.localStorage.setItem(ACCESS_KEY, tokens.accessToken);
  window.localStorage.setItem(REFRESH_KEY, tokens.refreshToken);
}

export function clearTokens(): void {
  if (!isBrowser) return;
  window.localStorage.removeItem(ACCESS_KEY);
  window.localStorage.removeItem(REFRESH_KEY);
  window.localStorage.removeItem(IMP_BACKUP_KEY);
  window.localStorage.removeItem(IMP_TENANT_KEY);
}

/**
 * Enter tenant impersonation: stash the platform admin's own tokens and switch to the tenant-scoped
 * access token. No refresh token during impersonation — it's short-lived by design.
 */
export function enterImpersonation(accessToken: string, tenantName: string): void {
  if (!isBrowser) return;
  const current = getTokens();
  if (current) window.localStorage.setItem(IMP_BACKUP_KEY, JSON.stringify(current));
  window.localStorage.setItem(IMP_TENANT_KEY, tenantName);
  window.localStorage.setItem(ACCESS_KEY, accessToken);
  window.localStorage.removeItem(REFRESH_KEY);
}

/** Exit impersonation: restore the platform admin's tokens. Returns false if not impersonating. */
export function exitImpersonation(): boolean {
  if (!isBrowser) return false;
  const backup = window.localStorage.getItem(IMP_BACKUP_KEY);
  window.localStorage.removeItem(IMP_BACKUP_KEY);
  window.localStorage.removeItem(IMP_TENANT_KEY);
  if (!backup) return false;
  const tokens = JSON.parse(backup) as StoredTokens;
  setTokens(tokens);
  return true;
}

export function isImpersonating(): boolean {
  return isBrowser && window.localStorage.getItem(IMP_BACKUP_KEY) !== null;
}

export function getImpersonationTenant(): string | null {
  return isBrowser ? window.localStorage.getItem(IMP_TENANT_KEY) : null;
}
