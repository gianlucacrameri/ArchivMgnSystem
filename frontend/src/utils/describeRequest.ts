import type { TransactionDTO } from "../types";

/** Kurztext eines Antrags, z.B. "+ 3 × Fussball" oder "Neues Foto für Fussball". */
export function describeRequest(t: TransactionDTO, unknownItem = "Unbekanntes Material"): string {
  const name = t.item?.name ?? t.subjectName ?? unknownItem;
  if (t.transactionType === "NEW") return `Neues Material: ${t.subjectName ?? name}`;
  if (t.transactionType === "DELETE") return `Löschen: ${name}`;
  if (t.transactionType === "PHOTO") {
    return `${t.photoTarget === "LOCATION" ? "Neues Lagerort-Foto" : "Neues Foto"} für ${name}`;
  }
  const photo = t.proposedImageUrl ? (t.photoTarget === "LOCATION" ? ", mit neuem Lagerort-Foto" : ", mit neuem Foto") : "";
  return `${t.transactionType === "ADD" ? "+" : "−"} ${t.totalItems} × ${name}${photo}`;
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
