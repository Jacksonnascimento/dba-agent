"use client";

import React, { createContext, useContext, useMemo, useState } from "react";
import { apiFetch } from "@/lib/api";
import { clearToken, getToken, setToken } from "@/lib/auth-storage";
import type { LoginResponse } from "@/lib/types";

type AuthContextValue = {
  token: string | null;
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [token, setTokenState] = useState<string | null>(() => getToken());

  const value = useMemo<AuthContextValue>(
    () => ({
      token,
      isAuthenticated: Boolean(token),
      login: async (email: string, password: string) => {
        const res = await apiFetch<LoginResponse>("/auth/login", {
          method: "POST",
          auth: false,
          body: JSON.stringify({ email, password }),
        });
        setToken(res.token);
        setTokenState(res.token);
      },
      logout: () => {
        clearToken();
        setTokenState(null);
      },
    }),
    [token]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}

