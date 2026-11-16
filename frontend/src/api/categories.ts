import { apiGet, apiSend } from "./client";
import type { CategoryDTO } from "../types";

export async function fetchCategories(): Promise<CategoryDTO[]> {
  const res = await apiGet("/api/categories/all");
  return res.categories ?? [];
}

/** Ohne Angabe zu "perishable" entscheidet der Server anhand des Namens (Essen, Getränke, ...). */
export async function createCategory(name: string, perishable?: boolean): Promise<CategoryDTO> {
  const res = await apiSend("POST", "/api/categories/add", { name, perishable });
  if (!res.category) throw new Error("Unerwartete Antwort vom Server");
  return res.category;
}

export async function renameCategory(id: number, name: string): Promise<void> {
  await apiSend("PUT", `/api/categories/update/${id}`, { name });
}

export async function setCategoryPerishable(category: CategoryDTO, perishable: boolean): Promise<void> {
  await apiSend("PUT", `/api/categories/update/${category.id}`, { name: category.name, perishable });
}

export async function deleteCategory(id: number): Promise<void> {
  await apiSend("DELETE", `/api/categories/delete/${id}`);
}
