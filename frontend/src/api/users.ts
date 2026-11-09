import { apiGetRaw, apiSend } from "./client";
import type { UserRole } from "../types";

export interface CurrentUser {
  id: number;
  username: string;
  email: string;
  phoneNumber: string;
  role: UserRole;
}

export async function fetchCurrentUser(): Promise<CurrentUser> {
  return apiGetRaw<CurrentUser>("/api/users/current");
}

export async function updateOwnProfile(
  id: number,
  input: { username?: string; email?: string; phoneNumber?: string; password?: string }
): Promise<void> {
  await apiSend("PUT", `/api/users/update/${id}`, input);
}
