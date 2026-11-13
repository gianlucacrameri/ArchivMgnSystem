import type { TransactionDTO } from "../types";

/** Kurztext eines Antrags, z.B. "+ 3 × Fussball" oder "Neues Foto für Fussball". */
export function describeRequest(t: TransactionDTO, unknownItem = "Unbekanntes Material"): string {
  const name = t.item?.name ?? unknownItem;
  if (t.transactionType === "PHOTO") {
    return `${t.photoTarget === "LOCATION" ? "Neues Lagerort-Foto" : "Neues Foto"} für ${name}`;
  }
  return `${t.transactionType === "ADD" ? "+" : "−"} ${t.totalItems} × ${name}`;
}

export function formatDateTime(value: string): string {
  return new Date(value).toLocaleString("de-CH", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}
