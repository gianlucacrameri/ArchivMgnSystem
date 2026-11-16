import { useState, type FormEvent } from "react";
import type { CategoryDTO } from "../types";
import Modal from "./Modal";
import { createCategory, deleteCategory, renameCategory, setCategoryPerishable } from "../api/categories";
import { looksPerishable } from "../utils/expiry";
import { ApiError } from "../api/client";
import { useToast } from "../context/ToastContext";

interface Props {
  categories: CategoryDTO[];
  onClose: () => void;
  onChanged: () => void;
}

export default function CategoriesModal({ categories, onClose, onChanged }: Props) {
  const { showSuccess, showError } = useToast();
  const [newName, setNewName] = useState("");
  // Vorschlag aus dem Namen (Essen, Getränke, ...), bis der Admin das Häkchen selbst setzt.
  const [newPerishable, setNewPerishable] = useState<boolean | null>(null);
  const newPerishableValue = newPerishable ?? looksPerishable(newName);
  const [saving, setSaving] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editingName, setEditingName] = useState("");

  async function handleCreate(e: FormEvent) {
    e.preventDefault();
    if (!newName.trim()) return;
    setSaving(true);
    try {
      await createCategory(newName.trim(), newPerishableValue);
      setNewName("");
      setNewPerishable(null);
      showSuccess("Kategorie angelegt");
      onChanged();
    } catch (err) {
      showError(err instanceof ApiError ? err.message : "Kategorie konnte nicht angelegt werden");
    } finally {
      setSaving(false);
    }
  }

  async function handleRename(id: number) {
    if (!editingName.trim()) return;
    try {
      await renameCategory(id, editingName.trim());
      setEditingId(null);
      showSuccess("Kategorie umbenannt");
      onChanged();
    } catch (err) {
      showError(err instanceof ApiError ? err.message : "Umbenennen fehlgeschlagen");
    }
  }

  async function handleTogglePerishable(category: CategoryDTO, perishable: boolean) {
    try {
      await setCategoryPerishable(category, perishable);
      onChanged();
    } catch (err) {
      showError(err instanceof ApiError ? err.message : "Änderung fehlgeschlagen");
    }
  }

  async function handleDelete(category: CategoryDTO) {
    if (!window.confirm(`Kategorie "${category.name}" wirklich löschen? Zugehöriges Material wird mitgelöscht.`)) {
      return;
    }
    try {
      await deleteCategory(category.id);
      showSuccess("Kategorie gelöscht");
      onChanged();
    } catch (err) {
      showError(err instanceof ApiError ? err.message : "Löschen fehlgeschlagen");
    }
  }

  return (
    <Modal title="Kategorien verwalten" onClose={onClose}>
      <form onSubmit={handleCreate} className="stacked-form">
        <label className="field">
          <span>Neue Kategorie</span>
          <input value={newName} onChange={(e) => setNewName(e.target.value)} />
        </label>
        <label className="check-field">
          <input
            type="checkbox"
            checked={newPerishableValue}
            onChange={(e) => setNewPerishable(e.target.checked)}
          />
          <span>Material kann ablaufen (Ablaufdatum möglich)</span>
        </label>
        <button type="submit" className="btn btn--primary" disabled={saving}>
          {saving ? "Wird angelegt…" : "+ Kategorie anlegen"}
        </button>
      </form>

      <hr className="divider" />

      <ul className="pick-list">
        {categories.map((c) => (
          <li key={c.id}>
            {editingId === c.id ? (
              <div className="pick-list__item pick-list__item--selected">
                <input value={editingName} onChange={(e) => setEditingName(e.target.value)} autoFocus />
                <button type="button" className="btn btn--link" onClick={() => handleRename(c.id)}>
                  Speichern
                </button>
                <button type="button" className="btn btn--link" onClick={() => setEditingId(null)}>
                  Abbrechen
                </button>
              </div>
            ) : (
              <div className="pick-list__item pick-list__item--selected">
                <span className="pick-list__info">
                  <strong>{c.name}</strong>
                  <label className="check-field check-field--small">
                    <input
                      type="checkbox"
                      checked={!!c.perishable}
                      onChange={(e) => handleTogglePerishable(c, e.target.checked)}
                    />
                    <span>mit Ablaufdatum</span>
                  </label>
                </span>
                <button
                  type="button"
                  className="btn btn--link"
                  onClick={() => {
                    setEditingId(c.id);
                    setEditingName(c.name);
                  }}
                >
                  Umbenennen
                </button>
                <button type="button" className="btn btn--link btn--danger-text" onClick={() => handleDelete(c)}>
                  Löschen
                </button>
              </div>
            )}
          </li>
        ))}
        {categories.length === 0 && <li className="hint">Noch keine Kategorien.</li>}
      </ul>
    </Modal>
  );
}
