import { apiGet, apiSend, apiSendForm } from "./client";
import type { PhotoTarget, TransactionDTO, TransactionStatus } from "../types";
import { prepareImageForUpload } from "../utils/resizeImage";

export async function requestTransaction(
  type: "add" | "use",
  input: { itemId: number; quantity: number; description?: string; note?: string }
): Promise<void> {
  await apiSend("POST", `/api/transactions/${type}`, input);
}

/** Schlägt ein neues Foto vor; es ersetzt das bisherige erst nach Freigabe durch einen Admin. */
export async function requestPhoto(input: {
  itemId: number;
  target: PhotoTarget;
  imageFile: File;
  note?: string;
}): Promise<void> {
  const form = new FormData();
  form.set("itemId", String(input.itemId));
  form.set("target", input.target);
  form.set("imageFile", await prepareImageForUpload(input.imageFile));
  if (input.note) form.set("note", input.note);
  await apiSendForm("POST", "/api/transactions/photo", form);
}

/** Ein einzelner Antrag mit allen Angaben (nur für Admins). */
export async function fetchTransaction(id: number): Promise<TransactionDTO> {
  const res = await apiGet(`/api/transactions/${id}`);
  if (!res.transaction) throw new Error("Unerwartete Antwort vom Server");
  return res.transaction;
}

export async function fetchTransactions(params: {
  page?: number;
  size?: number;
  filter?: string;
}): Promise<{ transactions: TransactionDTO[]; totalPages: number }> {
  const search = new URLSearchParams();
  search.set("page", String(params.page ?? 0));
  search.set("size", String(params.size ?? 100));
  if (params.filter) search.set("filter", params.filter);

  const res = await apiGet(`/api/transactions/all?${search.toString()}`);
  return { transactions: res.transactions ?? [], totalPages: res.totalPages ?? 1 };
}

/** Die eigenen Anträge des angemeldeten Mitglieds, neueste zuerst. */
export async function fetchMyTransactions(): Promise<TransactionDTO[]> {
  const res = await apiGet("/api/transactions/mine");
  return res.transactions ?? [];
}

export async function updateTransactionStatus(
  id: number,
  status: TransactionStatus
): Promise<void> {
  await apiSend("PUT", `/api/transactions/${id}`, status);
}
