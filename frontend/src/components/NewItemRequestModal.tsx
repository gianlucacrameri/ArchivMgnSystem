import { useState, type FormEvent } from "react";
import type { CategoryDTO, StockPrecision } from "../types";
import Modal from "./Modal";
import NumberField from "./NumberField";
import { requestNewItem } from "../api/transactions";
import { ApiError } from "../api/client";
import { useToast } from "../context/ToastContext";

interface Props {
  categories: CategoryDTO[];
  onClose: () => void;
  onRequested: () => void;
}

/** Mitglied schlägt ein neues Material vor; angelegt wird es erst, wenn ein Admin zustimmt. */
export default function NewItemRequestModal({ categories, onClose, onRequested }: Props) {
  const { showSuccess, showError } = useToast();
  const [name, setName] = useState("");
  const [place, setPlace] = useState("");
  const [quantity, setQuantity] = useState<number | "">(1);
  const [stockPrecision, setStockPrecision] = useState<StockPrecision>("EXACT");
  const [categoryId, setCategoryId] = useState<number | "">(categories[0]?.id ?? "");
  const [description, setDescription] = useState("");
  const [imageFile, setImageFile] = useState<File | null>(null);
  const [note, setNote] = useState("");
  const [saving, setSaving] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!categoryId) {
      showError("Bitte eine Kategorie wählen");
      return;
    }
    setSaving(true);
    try {
      await requestNewItem({
        name: name.trim(),
        place: place.trim() || undefined,
        quantity: quantity === "" ? 0 : quantity,
        stockPrecision,
        categoryId,
        description: description.trim() || undefined,
        imageFile,
        note: note.trim() || undefined,
      });
      showSuccess("Antrag wurde eingereicht und wartet auf Freigabe durch einen Admin.");
      onRequested();
    } catch (err) {
      showError(err instanceof ApiError ? err.message : "Antrag konnte nicht gesendet werden");
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal title="Neues Material beantragen" onClose={onClose}>
      <form onSubmit={handleSubmit} className="stacked-form">
        {categories.length === 0 && (
          <p className="form-error" role="alert">
            Es gibt noch keine Kategorie. Ein Admin muss zuerst eine anlegen.
          </p>
        )}
        <label className="field">
          <span>Name</span>
          <input required maxLength={255} value={name} onChange={(e) => setName(e.target.value)} />
        </label>
        <label className="field">
          <span>Lagerort (optional)</span>
          <input maxLength={255} value={place} onChange={(e) => setPlace(e.target.value)} />
        </label>
        <label className="field">
          <span>Kategorie</span>
          <select required value={categoryId} onChange={(e) => setCategoryId(Number(e.target.value))}>
            <option value="" disabled>
              Bitte wählen…
            </option>
            {categories.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
        </label>
        <label className="field">
          <span>Bestand</span>
          <NumberField value={quantity} onChange={setQuantity} min={0} required fallback={0} />
        </label>
        <div className="field">
          <span>Bestand ist...</span>
          <div className="segmented">
            <button
              type="button"
              className={`segmented__option ${stockPrecision === "EXACT" ? "is-active" : ""}`}
              onClick={() => setStockPrecision("EXACT")}
            >
              Genau gezählt
            </button>
            <button
              type="button"
              className={`segmented__option ${stockPrecision === "APPROXIMATE" ? "is-active" : ""}`}
              onClick={() => setStockPrecision("APPROXIMATE")}
            >
              Ungefähr
            </button>
          </div>
        </div>
        <label className="field">
          <span>Beschreibung (optional)</span>
          <textarea maxLength={255} value={description} onChange={(e) => setDescription(e.target.value)} rows={2} />
        </label>
        <label className="field">
          <span>Foto (optional)</span>
          <input type="file" accept="image/*" onChange={(e) => setImageFile(e.target.files?.[0] ?? null)} />
        </label>
        <label className="field">
          <span>Notiz an den Admin (optional)</span>
          <input maxLength={255} value={note} onChange={(e) => setNote(e.target.value)} />
        </label>

        <button type="submit" className="btn btn--primary btn--block" disabled={saving || categories.length === 0}>
          {saving ? "Wird gesendet…" : "Antrag einreichen"}
        </button>
        <p className="hint">Das Material erscheint im Archiv, sobald ein Admin den Antrag freigegeben hat.</p>
      </form>
    </Modal>
  );
}
