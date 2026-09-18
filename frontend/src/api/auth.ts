import { apiSend } from "./client";
import type { UserRole } from "../types";

export interface LoginResult {
  token: string;
  role: UserRole;
}

export async function login(email: string, password: string): Promise<LoginResult> {
  const res = await apiSend("POST", "/api/auth/login", { email, password });
  if (!res.token || !res.role) {
    throw new Error("Unerwartete Antwort vom Server");
  }
  return { token: res.token, role: res.role };
}

export async function register(input: {
  name: string;
  email: string;
  password: string;
  phoneNumber: string;
}): Promise<void> {
  await apiSend("POST", "/api/auth/register", input);
}
