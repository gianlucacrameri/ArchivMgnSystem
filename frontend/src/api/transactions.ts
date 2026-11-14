import { apiGet, apiSend, apiSendForm } from "./client";
import type { PhotoTarget, StockPrecision, TransactionDTO, TransactionStatus, TransactionType } from "../types";
import { prepareImageForUpload } from "../utils/resizeImage";

/**
 * Reicht einen Antrag für ein Material ein: Bestand ändern (ADD/USE mit Menge),
 * ein neues Foto vorschlagen oder beides zusammen. "PHOTO" = nur ein Foto.
 * Wirksam wird alles erst nach Freigabe durch einen Admin.
 */
export async function submitRequest(input: {
  itemId: number;
  type: TransactionType;
  quantity?: number;
  imageFile?: File | null;
  target?: PhotoTarget;
  note?: string;
}): Promise<void> {
  const form = new FormData();
  form.set("itemId", String(input.itemId));
  form.set("type", input.type);
  if (input.type !== "PHOTO" && input.quantity !== undefined) form.set("quantity", String(input.quantity));
  if (input.imageFile) {
    form.set("imageFile", await prepareImageForUpload(input.imageFile));
    form.set("target", input.target ?? "ITEM");
  }
  if (input.note) form.set("note", input.note);
  await apiSendForm("POST", "/api/transactions/request", form);
}

/** Schlägt vor, ein neues Material ins Archiv aufzunehmen. Angelegt wird es erst nach Freigabe. */
export async function requestNewItem(input: {
  name: string;
  place: string;
  quantity: number;
  stockPrecision: StockPrecision;
  categoryId: number;
  description?: string;
  imageFile?: File | null;
  note?: string;
}): Promise<void> {
  const form = new FormData();
  form.set("name", input.name);
  form.set("place", input.place);
  form.set("quantity", String(input.quantity));
  form.set("stockPrecision", input.stockPrecision);
  form.set("categoryId", String(input.categoryId));
  if (input.description) form.set("description", input.description);
  if (input.imageFile) form.set("imageFile", await prepareImageForUpload(input.imageFile));
  if (input.note) form.set("note", input.note);
  await apiSendForm("POST", "/api/transactions/new-item", form);
}

/** Schlägt vor, ein Material zu löschen. Gelöscht wird es erst nach Freigabe. */
export async function requestItemDeletion(itemId: number, note?: string): Promise<void> {
  const form = new FormData();
  form.set("itemId", String(itemId));
  if (note) form.set("note", note);
  await apiSendForm("POST", "/api/transactions/delete-item", form);
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
