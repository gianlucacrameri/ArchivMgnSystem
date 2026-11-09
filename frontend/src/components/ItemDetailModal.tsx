import { useState, type FormEvent } from "react";
import type { CategoryDTO, ItemDTO, StockPrecision, TransactionType } from "../types";
import Modal from "./Modal";
import NumberField from "./NumberField";
import AuthenticatedImage from "./AuthenticatedImage";
import { requestTransaction } from "../api/transactions";
import { deleteItem, updateItem } from "../api/items";
import { ApiError } from "../api/client";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";

interface Props {
  item: ItemDTO;
  categories: CategoryDTO[];
  onClose: () => void;
  onChanged: () => void;
}

export default function ItemDetailModal({ item, categories, onClose, onChanged }: Props) {
  const { isAdmin } = useAuth();
  const { showSuccess, showError } = useToast();

  const [type, setType] = useState<TransactionType>("ADD");
  const [quantity, setQuantity] = useState<number | "">(1);
  const [note, setNote] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const [editMode, setEditMode] = useState(false);
  const [editName, setEditName] = useState(item.name);
  const [editPlace, setEditPlace] = useState(item.place);
  const [editCategoryId, setEditCategoryId] = useState(item.categoryId);
  const [editDescription, setEditDescription] = useState(item.description ?? "");
  const [editStockPrecision, setEditStockPrecision] = useState<StockPrecision>(item.stockPrecision ?? "EXACT");
  const [editImageFile, setEditImageFile] = useState<File | null>(null);
  const [editLocationImageFile, setEditLocationImageFile] = useState<File | null>(null);
  const [savingEdit, setSavingEdit] = useState(false);

  async function submitRequest(e: FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    try {
      await requestTransaction(type === "ADD" ? "add" : "use", {
        itemId: item.id,
        quantity: quantity === "" ? 1 : quantity,
        note: note || undefined,
      });
      showSuccess("Antrag wurde eingereicht und wartet auf Freigabe durch einen Admin.");
      onClose();
    } catch (err) {
      showError(err instanceof ApiError ? err.message : "Antrag konnte nicht gesendet werden");
    } finally {
      setSubmitting(false);
    }
  }

  async function saveEdit(e: FormEvent) {
    e.preventDefault();
    setSavingEdit(true);
    try {
      await updateItem({
        itemId: item.id,
        name: editName,
        place: editPlace,
        categoryId: editCategoryId,
        description: editDescription,
        stockPrecision: editStockPrecision,
        imageFile: editImageFile,
        locationImageFile: editLocationImageFile,
      });
      showSuccess("Material aktualisiert");
      setEditMode(false);
      onChanged();
    } catch (err) {
      showError(err instanceof ApiError ? err.message : "Speichern fehlgeschlagen");
    } finally {
      setSavingEdit(false);
    }
  }

  async function handleDelete() {
    if (!window.confirm(`"${item.name}" wirklich endgültig löschen?`)) return;
    try {
      await deleteItem(item.id);
      showSuccess("Material gelöscht");
      onChanged();
      onClose();
    } catch (err) {
      showError(err instanceof ApiError ? err.message : "Löschen fehlgeschlagen");
    }
  }

  return (
    <Modal title={item.name} onClose={onClose}>
      <div className="item-detail__image">
        <AuthenticatedImage src={item.imageUrl} alt={item.name} className="item-detail__img" />
      </div>

      {item.locationImageUrl && !editMode && (
        <>
          <p className="hint">Lagerort-Foto</p>
          <div className="item-detail__image">
            <AuthenticatedImage src={item.locationImageUrl} alt={`Lagerort von ${item.name}`} className="item-detail__img" />
          </div>
        </>
      )}

      {!editMode && (
        <>
          <dl className="item-detail__facts">
            <div>
              <dt>Lagerort</dt>
              <dd>{item.place}</dd>
            </div>
            <div>
              <dt>Bestand</dt>
              <dd>
                {item.stockQuantity} Stk.{" "}
                <span className="hint">({item.stockPrecision === "APPROXIMATE" ? "ungefähr" : "genau"})</span>
              </dd>
            </div>
            <div>
              <dt>Artikelnummer</dt>
              <dd>{item.sku}</dd>
            </div>
          </dl>
          {item.description && <p className="item-detail__description">{item.description}</p>}

          <hr className="divider" />
          <br />
          <h3 className="section-title">Bestand anpassen</h3>
          <br />
          <form onSubmit={submitRequest} className="stacked-form">
            <div className="segmented">
              <button
                type="button"
                className={`segmented__option ${type === "ADD" ? "is-active" : ""}`}
                onClick={() => setType("ADD")}
              >
                + Hinzufügen
              </button>
              <button
                type="button"
                className={`segmented__option ${type === "USE" ? "is-active" : ""}`}
                onClick={() => setType("USE")}
              >
                − Verbrauchen
              </button>
            </div>

            <label className="field">
              <span>Menge</span>
              <NumberField value={quantity} onChange={setQuantity} min={1} required fallback={1} />
            </label>

            <label className="field">
              <span>Notiz (optional)</span>
              <input value={note} onChange={(e) => setNote(e.target.value)} />
            </label>

            <button type="submit" className="btn btn--primary btn--block" disabled={submitting}>
              {submitting ? "Wird gesendet…" : "Antrag einreichen"}
            </button>
            <p className="hint">Ein Admin muss diese Bestandsänderung noch freigeben.</p>
          </form>

          {isAdmin && (
            <>
              <hr className="divider" />
              <div className="button-row">
                <button type="button" className="btn btn--secondary" onClick={() => setEditMode(true)}>
                  Bearbeiten
                </button>
                <button type="button" className="btn btn--danger" onClick={handleDelete}>
                  Löschen
                </button>
              </div>
            </>
          )}
        </>
      )}

      {editMode && isAdmin && (
        <form onSubmit={saveEdit} className="stacked-form">
          <label className="field">
            <span>Name</span>
            <input required value={editName} onChange={(e) => setEditName(e.target.value)} />
          </label>
          <label className="field">
            <span>Lagerort</span>
            <input required value={editPlace} onChange={(e) => setEditPlace(e.target.value)} />
          </label>
          <label className="field">
            <span>Kategorie</span>
            <select
              value={editCategoryId}
              onChange={(e) => setEditCategoryId(Number(e.target.value))}
            >
              {categories.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
          </label>

          <div className="field">
            <span>Bestand ist...</span>
            <div className="segmented">
              <button
                type="button"
                className={`segmented__option ${editStockPrecision === "EXACT" ? "is-active" : ""}`}
                onClick={() => setEditStockPrecision("EXACT")}
              >
                Genau gezählt
              </button>
              <button
                type="button"
                className={`segmented__option ${editStockPrecision === "APPROXIMATE" ? "is-active" : ""}`}
                onClick={() => setEditStockPrecision("APPROXIMATE")}
              >
                Ungefähr
              </button>
            </div>
          </div>

          <label className="field">
            <span>Beschreibung</span>
            <textarea
              value={editDescription}
              onChange={(e) => setEditDescription(e.target.value)}
              rows={3}
            />
          </label>
          <label className="field">
            <span>Neues Foto des Materials (optional)</span>
            <input
              type="file"
              accept="image/*"
              onChange={(e) => setEditImageFile(e.target.files?.[0] ?? null)}
            />
          </label>
          <label className="field">
            <span>Neues Foto des Lagerorts (optional)</span>
            <input
              type="file"
              accept="image/*"
              onChange={(e) => setEditLocationImageFile(e.target.files?.[0] ?? null)}
            />
          </label>

          <div className="button-row">
            <button type="submit" className="btn btn--primary" disabled={savingEdit}>
              {savingEdit ? "Speichert…" : "Speichern"}
            </button>
            <button type="button" className="btn btn--secondary" onClick={() => setEditMode(false)}>
              Abbrechen
            </button>
          </div>
        </form>
      )}
    </Modal>
  );
}
