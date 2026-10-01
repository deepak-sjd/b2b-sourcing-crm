const STORAGE_KEY = "doctutorials_crm.auth";

export interface StoredTokens {
  accessToken: string;
  refreshToken: string;
}

export function getAuthTokens(): StoredTokens | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as StoredTokens) : null;
  } catch {
    return null;
  }
}

export function setAuthTokens(tokens: StoredTokens): void {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(tokens));
}

export function clearAuthTokens(): void {
  localStorage.removeItem(STORAGE_KEY);
}
