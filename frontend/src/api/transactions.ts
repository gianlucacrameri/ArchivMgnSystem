import { apiGet, apiSend } from "./client";
import type { TransactionDTO, TransactionStatus } from "../types";

export async function requestTransaction(
  type: "add" | "use",
  input: { itemId: number; quantity: number; description?: string; note?: string }
): Promise<void> {
  await apiSend("POST", `/api/transactions/${type}`, input);
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

export async function updateTransactionStatus(
  id: number,
  status: TransactionStatus
): Promise<void> {
  await apiSend("PUT", `/api/transactions/${id}`, status);
}
