const TOKEN_KEY = "dba_agent_jwt";
const USER_KEY = "dba_agent_user";

export function getToken(): string | null {
  if (typeof window === "undefined") return null;
  return window.localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string) {
  window.localStorage.setItem(TOKEN_KEY, token);
}

export function getUser(): { role: string; name: string; email: string } | null {
  if (typeof window === "undefined") return null;
  const user = window.localStorage.getItem(USER_KEY);
  return user ? JSON.parse(user) : null;
}

export function setUser(user: { role: string; name: string; email: string }) {
  window.localStorage.setItem(USER_KEY, JSON.stringify(user));
}

export function clearToken() {
  window.localStorage.removeItem(TOKEN_KEY);
  window.localStorage.removeItem(USER_KEY);
}

