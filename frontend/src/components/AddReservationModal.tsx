import { useEffect, useState, type FormEvent } from "react";
import Modal from "./Modal";
import NumberField from "./NumberField";
import type { EventDTO, ItemDTO } from "../types";
import { fetchItems } from "../api/items";
import { addReservation } from "../api/events";
import { ApiError } from "../api/client";
import { useToast } from "../context/ToastContext";
import AuthenticatedImage from "./AuthenticatedImage";

interface Props {
  event: EventDTO;
  onClose: () => void;
  onReserved: (event: EventDTO) => void;
}

export default function AddReservationModal({ event, onClose, onReserved }: Props) {
  const { showError, showSuccess } = useToast();
  const [search, setSearch] = useState("");
  const [results, setResults] = useState<ItemDTO[]>([]);
  const [selected, setSelected] = useState<ItemDTO | null>(null);
  const [quantity, setQuantity] = useState<number | "">(1);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    const timeout = window.setTimeout(async () => {
      try {
        const page = await fetchItems({ filter: search || undefined, size: 20 });
        setResults(page.items);
      } catch {
        setResults([]);
      }
    }, 250);
    return () => window.clearTimeout(timeout);
  }, [search]);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!selected) return;
    setSaving(true);
    try {
      const updated = await addReservation(event.id, { itemId: selected.id, quantity: quantity === "" ? 1 : quantity });
      showSuccess(`"${selected.name}" reserviert`);
      onReserved(updated);
    } catch (err) {
      showError(err instanceof ApiError ? err.message : "Reservierung fehlgeschlagen");
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal title="Material reservieren" onClose={onClose}>
      {!selected && (
        <div className="stacked-form">
          <input
            type="search"
            placeholder="Material suchen…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            autoFocus
          />
          <ul className="pick-list">
            {results.map((item) => (
              <li key={item.id}>
                <button type="button" className="pick-list__item" onClick={() => setSelected(item)}>
                  <AuthenticatedImage src={item.imageUrl} alt="" size="thumb" className="pick-list__thumb" />
                  <span className="pick-list__info">
                    <strong>{item.name}</strong>
                    <small>{item.stockQuantity} Stk. verfügbar (Gesamtbestand)</small>
                  </span>
                </button>
              </li>
            ))}
            {results.length === 0 && <li className="hint">Keine Treffer</li>}
          </ul>
        </div>
      )}

      {selected && (
        <form onSubmit={handleSubmit} className="stacked-form">
          <div className="pick-list__item pick-list__item--selected">
            <AuthenticatedImage src={selected.imageUrl} alt="" size="thumb" className="pick-list__thumb" />
            <span className="pick-list__info">
              <strong>{selected.name}</strong>
              <small>{selected.stockQuantity} Stk. Gesamtbestand</small>
            </span>
            <button type="button" className="btn btn--link" onClick={() => setSelected(null)}>
              Ändern
            </button>
          </div>

          <label className="field">
            <span>Menge</span>
            <NumberField value={quantity} onChange={setQuantity} min={1} required fallback={1} />
          </label>

          <button type="submit" className="btn btn--primary btn--block" disabled={saving}>
            {saving ? "Wird reserviert…" : "Reservieren"}
          </button>
        </form>
      )}
    </Modal>
  );
}
