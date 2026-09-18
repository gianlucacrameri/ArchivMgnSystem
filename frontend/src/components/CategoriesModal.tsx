import { useState, type FormEvent } from "react";
import type { CategoryDTO } from "../types";
import Modal from "./Modal";
import { createCategory, deleteCategory, renameCategory } from "../api/categories";
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
  const [saving, setSaving] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editingName, setEditingName] = useState("");

  async function handleCreate(e: FormEvent) {
    e.preventDefault();
    if (!newName.trim()) return;
    setSaving(true);
    try {
      await createCategory(newName.trim());
      setNewName("");
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
          <input value={newName} onChange={(e) => setNewName(e.target.value)} placeholder="z.B. Bälle" />
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
                <span className="pick-list__info">{c.name}</span>
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
