import { apiGetRaw } from "./client";
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
