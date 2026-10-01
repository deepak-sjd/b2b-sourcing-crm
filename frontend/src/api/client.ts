import axios, { AxiosError, type InternalAxiosRequestConfig } from "axios";
import type { ApiResponse } from "@/types/api";
import type { AuthResponse } from "@/types/auth";
import { getAuthTokens, setAuthTokens, clearAuthTokens } from "./tokenStorage";

/**
 * Base URL resolution: in local dev, Vite's server.proxy forwards /api to the
 * backend (see vite.config.ts) so VITE_API_BASE_URL can stay empty. In a
 * client deployment, set VITE_API_BASE_URL at build time to that client's
 * backend origin — nothing else in the app needs to change per client.
 */
const baseURL = import.meta.env.VITE_API_BASE_URL || "/api/v1";

export const apiClient = axios.create({
  baseURL,
  headers: { "Content-Type": "application/json" },
});

apiClient.interceptors.request.use((config) => {
  const tokens = getAuthTokens();
  if (tokens?.accessToken) {
    config.headers.Authorization = `Bearer ${tokens.accessToken}`;
  }
  return config;
});

// --- 401 handling: try exactly one silent refresh, then give up and clear session. ---
let refreshInFlight: Promise<string | null> | null = null;

async function refreshAccessToken(): Promise<string | null> {
  const tokens = getAuthTokens();
  if (!tokens?.refreshToken) return null;

  try {
    const response = await axios.post<ApiResponse<AuthResponse>>(
      `${baseURL}/auth/refresh`,
      { refreshToken: tokens.refreshToken },
    );
    const auth = response.data.data;
    setAuthTokens({ accessToken: auth.accessToken, refreshToken: auth.refreshToken });
    return auth.accessToken;
  } catch {
    clearAuthTokens();
    return null;
  }
}

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as (InternalAxiosRequestConfig & { _retried?: boolean }) | undefined;

    const isAuthEndpoint = original?.url?.includes("/auth/");
    if (error.response?.status === 401 && original && !original._retried && !isAuthEndpoint) {
      original._retried = true;
      refreshInFlight ??= refreshAccessToken();
      const newToken = await refreshInFlight;
      refreshInFlight = null;

      if (newToken) {
        original.headers.Authorization = `Bearer ${newToken}`;
        return apiClient(original);
      }
      // No valid session left — send the app back to login.
      window.dispatchEvent(new CustomEvent("auth:logout"));
    }
    return Promise.reject(error);
  },
);

/** Every backend error response still follows the standard JSON error shape
 * (see GlobalExceptionHandler) — this pulls the human-readable message out of
 * whichever shape the backend used, for consistent toast messages everywhere. */
export function extractErrorMessage(error: unknown, fallback = "Something went wrong"): string {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as
      | { message?: string; error?: string; errors?: Record<string, string> }
      | undefined;
    if (data?.errors && Object.keys(data.errors).length > 0) {
      return Object.values(data.errors)[0] ?? fallback;
    }
    return data?.message ?? data?.error ?? error.message ?? fallback;
  }
  if (error instanceof Error) return error.message;
  return fallback;
}
