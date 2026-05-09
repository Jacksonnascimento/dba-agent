import { API_BASE_URL } from "@/lib/config";
import { getToken } from "@/lib/auth-storage";

export type ApiError = {
  status: number;
  message: string;
  details?: unknown;
};

async function parseError(res: Response): Promise<ApiError> {
  let body: unknown = undefined;
  try {
    body = await res.json();
  } catch {
    // ignore
  }
  const message =
    (body as any)?.message ||
    (body as any)?.error ||
    (typeof body === "string" ? body : null) ||
    res.statusText ||
    "Request failed";

  return { status: res.status, message, details: body };
}

export async function apiFetch<T>(
  path: string,
  init?: RequestInit & { auth?: boolean }
): Promise<T> {
  const url = `${API_BASE_URL}${path.startsWith("/") ? path : `/${path}`}`;
  const headers = new Headers(init?.headers);
  headers.set("Accept", "application/json");

  if (init?.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const auth = init?.auth ?? true;
  if (auth) {
    const token = getToken();
    if (token) headers.set("Authorization", `Bearer ${token}`);
  }

  const res = await fetch(url, { ...init, headers });
  if (!res.ok) {
    throw await parseError(res);
  }

  if (res.status === 204) {
    return undefined as T;
  }
  return (await res.json()) as T;
}

