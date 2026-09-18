import { apiGet, apiSend } from "./client";
import type { CategoryDTO } from "../types";

export async function fetchCategories(): Promise<CategoryDTO[]> {
  const res = await apiGet("/api/categories/all");
  return res.categories ?? [];
}

export async function createCategory(name: string): Promise<void> {
  await apiSend("POST", "/api/categories/add", { name });
}

export async function renameCategory(id: number, name: string): Promise<void> {
  await apiSend("PUT", `/api/categories/update/${id}`, { name });
}

export async function deleteCategory(id: number): Promise<void> {
  await apiSend("DELETE", `/api/categories/delete/${id}`);
}
