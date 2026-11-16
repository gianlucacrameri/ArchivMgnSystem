import { useState, type FormEvent } from "react";
import type { CategoryDTO, StockPrecision } from "../types";
import Modal from "./Modal";
import NumberField from "./NumberField";
import { createItem } from "../api/items";
import { createCategory } from "../api/categories";
import { ApiError } from "../api/client";
import { useToast } from "../context/ToastContext";
import { looksPerishable } from "../utils/expiry";

interface Props {
  categories: CategoryDTO[];
  onClose: () => void;
  onCreated: () => void;
}

export default function CreateItemModal({ categories, onClose, onCreated }: Props) {
  const { showSuccess, showError } = useToast();
  const [name, setName] = useState("");
  const [place, setPlace] = useState("");
  const [stockQuantity, setStockQuantity] = useState<number | "">(0);
  const [stockPrecision, setStockPrecision] = useState<StockPrecision>("EXACT");
  const [categoryId, setCategoryId] = useState<number | "">(categories[0]?.id ?? "");
  const [creatingCategory, setCreatingCategory] = useState(categories.length === 0);
  const [newCategoryName, setNewCategoryName] = useState("");
  const [description, setDescription] = useState("");
  const [expirationDate, setExpirationDate] = useState("");
  const [newCategoryPerishable, setNewCategoryPerishable] = useState<boolean | null>(null);
  const [imageFile, setImageFile] = useState<File | null>(null);
  const [locationImageFile, setLocationImageFile] = useState<File | null>(null);
  const [saving, setSaving] = useState(false);

  // Ein Ablaufdatum gibt es nur für Kategorien, deren Material verderben kann.
  const newCategoryPerishableValue = newCategoryPerishable ?? looksPerishable(newCategoryName);
  const perishable = creatingCategory
    ? newCategoryPerishableValue
    : !!categories.find((c) => c.id === categoryId)?.perishable;

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();

    let resolvedCategoryId = categoryId;

    if (creatingCategory) {
      if (!newCategoryName.trim()) {
        showError("Bitte einen Namen für die neue Kategorie eingeben");
        return;
      }
    } else if (!categoryId) {
      showError("Bitte eine Kategorie wählen");
      return;
    }

    setSaving(true);
    try {
      if (creatingCategory) {
        const created = await createCategory(newCategoryName.trim(), newCategoryPerishableValue);
        resolvedCategoryId = created.id;
      }

      await createItem({
        name,
        place,
        stockQuantity: stockQuantity === "" ? 0 : stockQuantity,
        stockPrecision,
        categoryId: resolvedCategoryId as number,
        description: description || undefined,
        expirationDate: perishable && expirationDate ? expirationDate : undefined,
        imageFile,
        locationImageFile,
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
          <span>Lagerort</span>
          <input required value={place} onChange={(e) => setPlace(e.target.value)} />
        </label>

        <div className="field">
          <span>Kategorie</span>
          {!creatingCategory && (
            <>
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
              <button
                type="button"
                className="btn btn--link"
                onClick={() => {
                  setCreatingCategory(true);
                  setNewCategoryName("");
                }}
              >
                + Neue Kategorie erstellen
              </button>
            </>
          )}
          {creatingCategory && (
            <>
              <input
                required
                value={newCategoryName}
                onChange={(e) => setNewCategoryName(e.target.value)}
                autoFocus
              />
              <label className="check-field">
                <input
                  type="checkbox"
                  checked={newCategoryPerishableValue}
                  onChange={(e) => setNewCategoryPerishable(e.target.checked)}
                />
                <span>Material kann ablaufen (Ablaufdatum möglich)</span>
              </label>
              {categories.length > 0 && (
                <button type="button" className="btn btn--link" onClick={() => setCreatingCategory(false)}>
                  Bestehende Kategorie wählen
                </button>
              )}
            </>
          )}
        </div>

        {perishable && (
          <label className="field">
            <span>Ablaufdatum (optional)</span>
            <input type="date" value={expirationDate} onChange={(e) => setExpirationDate(e.target.value)} />
          </label>
        )}

        <label className="field">
          <span>Anfangsbestand</span>
          <NumberField value={stockQuantity} onChange={setStockQuantity} min={0} required fallback={0} />
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
          <textarea value={description} onChange={(e) => setDescription(e.target.value)} rows={3} />
        </label>
        <label className="field">
          <span>Foto des Materials (optional)</span>
          <input type="file" accept="image/*" onChange={(e) => setImageFile(e.target.files?.[0] ?? null)} />
        </label>
        <label className="field">
          <span>Foto des Lagerorts (optional)</span>
          <input type="file" accept="image/*" onChange={(e) => setLocationImageFile(e.target.files?.[0] ?? null)} />
        </label>

        <button type="submit" className="btn btn--primary btn--block" disabled={saving}>
          {saving ? "Wird angelegt…" : "Material anlegen"}
        </button>
      </form>
    </Modal>
  );
}
