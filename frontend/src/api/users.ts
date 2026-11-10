import { apiGet, apiGetRaw, apiSend } from "./client";
import type { UserDTO, UserRole } from "../types";

export interface CurrentUser {
  id: number;
  username: string;
  email: string;
  phoneNumber: string;
  role: UserRole;
  /** true = ein Admin hat ein vorläufiges Passwort vergeben, das noch nicht ersetzt wurde. */
  passwordResetRequired?: boolean;
}

export async function fetchCurrentUser(): Promise<CurrentUser> {
  return apiGetRaw<CurrentUser>("/api/users/current");
}

/**
 * Das Login-Token hängt an E-Mail und Passwort. Nach einer Änderung liefert
 * der Server deshalb ein neues Token zurück, mit dem man angemeldet bleibt.
 */
export async function updateOwnProfile(
  id: number,
  input: { username?: string; email?: string; phoneNumber?: string; password?: string; currentPassword?: string }
): Promise<{ token?: string; role?: UserRole }> {
  const res = await apiSend("PUT", `/api/users/update/${id}`, input);
  return { token: res.token, role: res.role };
}

/** Macht alle Anmeldungen dieses Kontos ungültig - auf jedem Gerät. */
export async function logoutEverywhere(): Promise<void> {
  await apiSend("POST", "/api/users/logout-everywhere");
}

// ---------- Mitgliederverwaltung (nur Admins) ----------

export async function fetchUsers(): Promise<UserDTO[]> {
  const res = await apiGet("/api/users/all");
  return res.users ?? [];
}

export async function approveUser(id: number): Promise<void> {
  await apiSend("PUT", `/api/users/${id}/approve`);
}

export async function updateUserRole(id: number, role: UserRole): Promise<void> {
  await apiSend("PUT", `/api/users/update/${id}`, { role });
}

/** Vergibt ein vorläufiges Passwort und gibt es einmalig zurück. */
export async function resetUserPassword(id: number): Promise<string> {
  const res = await apiSend("POST", `/api/users/${id}/reset-password`);
  if (!res.temporaryPassword) throw new Error("Unerwartete Antwort vom Server");
  return res.temporaryPassword;
}

export async function deleteUser(id: number): Promise<void> {
  await apiSend("DELETE", `/api/users/delete/${id}`);
}
