import { useState, type FormEvent } from "react";
import type { CategoryDTO } from "../types";
import Modal from "./Modal";
import { createItem } from "../api/items";
import { ApiError } from "../api/client";
import { useToast } from "../context/ToastContext";

interface Props {
  categories: CategoryDTO[];
  onClose: () => void;
  onCreated: () => void;
}

export default function CreateItemModal({ categories, onClose, onCreated }: Props) {
  const { showSuccess, showError } = useToast();
  const [name, setName] = useState("");
  const [sku, setSku] = useState("");
  const [place, setPlace] = useState("");
  const [stockQuantity, setStockQuantity] = useState(0);
  const [categoryId, setCategoryId] = useState<number | "">(categories[0]?.id ?? "");
  const [description, setDescription] = useState("");
  const [imageFile, setImageFile] = useState<File | null>(null);
  const [saving, setSaving] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!categoryId) {
      showError("Bitte eine Kategorie wählen");
      return;
    }
    setSaving(true);
    try {
      await createItem({
        name,
        sku,
        place,
        stockQuantity,
        categoryId,
        description: description || undefined,
        imageFile,
      });
      showSuccess("Material wurde angelegt");
      onCreated();
    } catch (err) {
      showError(err instanceof ApiError ? err.message : "Anlegen fehlgeschlagen");
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal title="Neues Material" onClose={onClose}>
      <form onSubmit={handleSubmit} className="stacked-form">
        <label className="field">
          <span>Name</span>
          <input required value={name} onChange={(e) => setName(e.target.value)} />
        </label>
        <label className="field">
          <span>SKU / Artikelnummer</span>
          <input required value={sku} onChange={(e) => setSku(e.target.value)} />
        </label>
        <label className="field">
          <span>Lagerort</span>
          <input required value={place} onChange={(e) => setPlace(e.target.value)} placeholder="z.B. Materialraum Regal 3" />
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
          <span>Anfangsbestand</span>
          <input
            type="number"
            min={0}
            inputMode="numeric"
            required
            value={stockQuantity}
            onChange={(e) => setStockQuantity(Number(e.target.value))}
          />
        </label>
        <label className="field">
          <span>Beschreibung (optional)</span>
          <textarea value={description} onChange={(e) => setDescription(e.target.value)} rows={3} />
        </label>
        <label className="field">
          <span>Foto (optional)</span>
          <input type="file" accept="image/*" onChange={(e) => setImageFile(e.target.files?.[0] ?? null)} />
        </label>

        <button type="submit" className="btn btn--primary btn--block" disabled={saving}>
          {saving ? "Wird angelegt…" : "Material anlegen"}
        </button>
      </form>
    </Modal>
  );
}
