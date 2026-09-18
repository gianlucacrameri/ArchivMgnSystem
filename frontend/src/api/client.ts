import type { ApiResponse } from "../types";

export const API_BASE_URL: string =
  (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? "http://localhost:8080";

const TOKEN_KEY = "archiv.token";
const ROLE_KEY = "archiv.role";

export class ApiError extends Error {
  status: number;

  constructor(message: string, status: number) {
    super(message);
    this.status = status;
  }
}

export function getToken(): string | null {
  try {
    return localStorage.getItem(TOKEN_KEY);
  } catch {
    return null;
  }
}

export function getStoredRole(): string | null {
  try {
    return localStorage.getItem(ROLE_KEY);
  } catch {
    return null;
  }
}

export function storeAuth(token: string, role: string): void {
  try {
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(ROLE_KEY, role);
  } catch {
    // Private-Browsing o.ä. – Session bleibt dann nur im Speicher dieses Tabs.
  }
}

export function clearAuth(): void {
  try {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(ROLE_KEY);
  } catch {
    // ignore
  }
}

/** Wird von AuthProvider gesetzt, um bei 401 automatisch auszuloggen. */
let unauthorizedHandler: (() => void) | null = null;
export function setUnauthorizedHandler(handler: (() => void) | null): void {
  unauthorizedHandler = handler;
}

function authHeaders(): Record<string, string> {
  const token = getToken();
  return token ? { Authorization: `Bearer ${token}` } : {};
}

async function parseResponse(res: Response): Promise<ApiResponse> {
  const text = await res.text();
  let body: ApiResponse = {};
  if (text) {
    try {
      body = JSON.parse(text) as ApiResponse;
    } catch {
      body = { message: text };
    }
  }

  if (res.status === 401 && unauthorizedHandler) {
    unauthorizedHandler();
  }

  if (!res.ok) {
    throw new ApiError(body.message ?? `Fehler ${res.status}`, res.status);
  }

  return body;
}

export async function apiGet(path: string): Promise<ApiResponse> {
  const res = await fetch(`${API_BASE_URL}${path}`, {
    headers: { ...authHeaders() },
  });
  return parseResponse(res);
}

/** Für Endpunkte, die nicht die einheitliche ApiResponse-Hülle verwenden. */
export async function apiGetRaw<T>(path: string): Promise<T> {
  const res = await fetch(`${API_BASE_URL}${path}`, {
    headers: { ...authHeaders() },
  });
  if (res.status === 401 && unauthorizedHandler) {
    unauthorizedHandler();
  }
  if (!res.ok) {
    throw new ApiError(`Fehler ${res.status}`, res.status);
  }
  return (await res.json()) as T;
}

export async function apiSend(
  method: "POST" | "PUT" | "DELETE",
  path: string,
  body?: unknown
): Promise<ApiResponse> {
  const res = await fetch(`${API_BASE_URL}${path}`, {
    method,
    headers: {
      "Content-Type": "application/json",
      ...authHeaders(),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  return parseResponse(res);
}

export async function apiSendForm(
  method: "POST" | "PUT",
  path: string,
  formData: FormData
): Promise<ApiResponse> {
  const res = await fetch(`${API_BASE_URL}${path}`, {
    method,
    headers: { ...authHeaders() },
    body: formData,
  });
  return parseResponse(res);
}

/**
 * Item-Fotos hängen hinter dem Login (JWT), ein <img src="..."> kann den
 * Authorization-Header nicht mitschicken. Deshalb laden wir das Bild per
 * fetch und geben eine Blob-URL zurück, die als src verwendet werden kann.
 * Der Aufrufer muss die Blob-URL bei Bedarf mit URL.revokeObjectURL wieder freigeben.
 */
export async function fetchAuthorizedImage(imageUrl: string): Promise<string> {
  const url = imageUrl.startsWith("http") ? imageUrl : `${API_BASE_URL}${imageUrl}`;
  const res = await fetch(url, { headers: { ...authHeaders() } });
  if (!res.ok) {
    throw new ApiError("Bild konnte nicht geladen werden", res.status);
  }
  const blob = await res.blob();
  return URL.createObjectURL(blob);
}
