/**
 * Hilfen rund ums Ablaufdatum. Der Server liefert es als Datum mit Uhrzeit
 * 00:00 ("2027-03-12T00:00:00"); gerechnet und angezeigt wird nur der Tag.
 */

/** Ab so vielen Tagen vor dem Ablauf wird im Archiv darauf hingewiesen. */
const SOON_DAYS = 30;

/** Vorschlag beim Anlegen einer Kategorie; der Server verwendet dieselbe Liste als Startwert. */
const PERISHABLE_KEYWORDS = [
  "essen", "getränk", "getraenk", "trinken", "lebensmittel", "verpflegung", "nahrung", "speise",
  "snack", "süss", "suess", "proviant", "food", "drink", "verbrauch", "apotheke", "sanität", "sanitaet",
  "erste hilfe", "medizin",
];

export function looksPerishable(categoryName: string): boolean {
  const name = categoryName.toLowerCase();
  return PERISHABLE_KEYWORDS.some((keyword) => name.includes(keyword));
}

/** "2027-03-12T00:00:00" oder "2027-03-12" → "2027-03-12" (Wert für ein Datumsfeld). */
export function toDateInputValue(value?: string | null): string {
  return value ? value.slice(0, 10) : "";
}

export function formatExpiryDate(value: string): string {
  const [year, month, day] = value.slice(0, 10).split("-").map(Number);
  return new Date(year, month - 1, day).toLocaleDateString("de-CH", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });
}

export type ExpiryStatus = "expired" | "soon" | "ok";

export function expiryStatus(value: string): ExpiryStatus {
  const [year, month, day] = value.slice(0, 10).split("-").map(Number);
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const days = Math.round((new Date(year, month - 1, day).getTime() - today.getTime()) / 86_400_000);
  if (days < 0) return "expired";
  return days <= SOON_DAYS ? "soon" : "ok";
}
