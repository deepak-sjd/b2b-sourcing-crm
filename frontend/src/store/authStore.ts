import { create } from "zustand";
import { persist } from "zustand/middleware";
import type { AuthenticatedUser } from "@/types/auth";
import { clearAuthTokens, setAuthTokens } from "@/api/tokenStorage";

interface AuthState {
  user: AuthenticatedUser | null;
  isAuthenticated: boolean;
  setSession: (user: AuthenticatedUser, accessToken: string, refreshToken: string) => void;
  clearSession: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      isAuthenticated: false,
      setSession: (user, accessToken, refreshToken) => {
        setAuthTokens({ accessToken, refreshToken });
        set({ user, isAuthenticated: true });
      },
      clearSession: () => {
        clearAuthTokens();
        set({ user: null, isAuthenticated: false });
      },
    }),
    {
      name: "doctutorials_crm.session",
      partialize: (state) => ({ user: state.user, isAuthenticated: state.isAuthenticated }),
    },
  ),
);

// A 401 that survives the refresh attempt (see api/client.ts) drops the whole
// app back to /login rather than leaving stale UI state around.
window.addEventListener("auth:logout", () => {
  useAuthStore.getState().clearSession();
});
