"use client";

import React, { createContext, useContext, useMemo, useState, useEffect } from "react";
import { apiFetch } from "@/lib/api";
import { clearToken, getToken, setToken, getUser, setUser } from "@/lib/auth-storage";
import type { LoginResponse } from "@/lib/types";

type UserData = { role: string; name: string; email: string };

type AuthContextValue = {
  token: string | null;
  user: UserData | null;
  isAuthenticated: boolean;
  isAdmin: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [token, setTokenState] = useState<string | null>(null);
  const [user, setUserState] = useState<UserData | null>(null);

  useEffect(() => {
    setTokenState(getToken());
    setUserState(getUser());
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      token,
      user,
      isAuthenticated: Boolean(token),
      isAdmin: user?.role === "ROLE_ADMIN",
      login: async (email: string, password: string) => {
        const res = await apiFetch<LoginResponse>("/auth/login", {
          method: "POST",
          auth: false,
          body: JSON.stringify({ email, password }),
        });
        setToken(res.token);
        setUser({ role: res.role, name: res.name, email: res.email });
        setTokenState(res.token);
        setUserState({ role: res.role, name: res.name, email: res.email });
      },
      logout: () => {
        clearToken();
        setTokenState(null);
        setUserState(null);
      },
    }),
    [token, user]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}

