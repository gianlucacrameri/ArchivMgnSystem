import { useEffect, useState, type FormEvent } from "react";
import type { ItemDTO } from "../types";
import Modal from "./Modal";
import AuthenticatedImage from "./AuthenticatedImage";
import { fetchItems } from "../api/items";
import { requestItemDeletion } from "../api/transactions";
import { ApiError } from "../api/client";
import { useToast } from "../context/ToastContext";

interface Props {
  /** Ist das Material schon bekannt (Aufruf aus dem Artikelfenster), entfällt die Suche. */
  item?: ItemDTO;
  onClose: () => void;
  onRequested: () => void;
}

/** Mitglied schlägt vor, ein Material zu löschen; gelöscht wird erst, wenn ein Admin zustimmt. */
export default function DeleteRequestModal({ item, onClose, onRequested }: Props) {
  const { showSuccess, showError } = useToast();
  const [search, setSearch] = useState("");
  const [results, setResults] = useState<ItemDTO[]>([]);
  const [selected, setSelected] = useState<ItemDTO | null>(item ?? null);
  const [note, setNote] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (selected) return;
    const timeout = window.setTimeout(async () => {
      try {
        const page = await fetchItems({ filter: search || undefined, size: 20 });
        setResults(page.items);
      } catch {
        setResults([]);
      }
    }, 250);
    return () => window.clearTimeout(timeout);
  }, [search, selected]);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!selected) return;
    setSaving(true);
    try {
      await requestItemDeletion(selected.id, note.trim() || undefined);
      showSuccess("Antrag wurde eingereicht und wartet auf Freigabe durch einen Admin.");
      onRequested();
    } catch (err) {
      showError(err instanceof ApiError ? err.message : "Antrag konnte nicht gesendet werden");
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal title="Löschung beantragen" onClose={onClose}>
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
            {results.map((result) => (
              <li key={result.id}>
                <button type="button" className="pick-list__item" onClick={() => setSelected(result)}>
                  <AuthenticatedImage src={result.imageUrl} alt="" size="thumb" className="pick-list__thumb" />
                  <span className="pick-list__info">
                    <strong>{result.name}</strong>
                    <small>
                      {result.sku} · {result.place}
                    </small>
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
              <small>
                {selected.sku} · {selected.stockQuantity} Stk. · {selected.place}
              </small>
            </span>
            {!item && (
              <button type="button" className="btn btn--link" onClick={() => setSelected(null)}>
                Ändern
              </button>
            )}
          </div>

          <label className="field">
            <span>Begründung</span>
            <input required maxLength={255} value={note} onChange={(e) => setNote(e.target.value)} />
          </label>

          <button type="submit" className="btn btn--danger btn--block" disabled={saving}>
            {saving ? "Wird gesendet…" : "Löschung beantragen"}
          </button>
          <p className="hint">Das Material bleibt im Archiv, bis ein Admin der Löschung zugestimmt hat.</p>
        </form>
      )}
    </Modal>
  );
}
