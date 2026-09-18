import { useCallback, useEffect, useState } from "react";
import type { CategoryDTO, ItemDTO } from "../types";
import { fetchItems } from "../api/items";
import { fetchCategories } from "../api/categories";
import ItemCard from "../components/ItemCard";
import ItemDetailModal from "../components/ItemDetailModal";
import CreateItemModal from "../components/CreateItemModal";
import CategoriesModal from "../components/CategoriesModal";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";
import { ApiError } from "../api/client";

export default function ItemsPage() {
  const { isAdmin } = useAuth();
  const { showError } = useToast();

  const [items, setItems] = useState<ItemDTO[]>([]);
  const [categories, setCategories] = useState<CategoryDTO[]>([]);
  const [search, setSearch] = useState("");
  const [categoryId, setCategoryId] = useState<number | "">("");
  const [loading, setLoading] = useState(true);
  const [selectedItem, setSelectedItem] = useState<ItemDTO | null>(null);
  const [showCreate, setShowCreate] = useState(false);
  const [showCategories, setShowCategories] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [itemsPage, cats] = await Promise.all([
        fetchItems({ filter: search || undefined, categoryId: categoryId || undefined }),
        fetchCategories(),
      ]);
      setItems(itemsPage.items);
      setCategories(cats);
    } catch (err) {
      showError(err instanceof ApiError ? err.message : "Archiv konnte nicht geladen werden");
    } finally {
      setLoading(false);
    }
  }, [search, categoryId, showError]);

  useEffect(() => {
    const timeout = window.setTimeout(load, 250);
    return () => window.clearTimeout(timeout);
  }, [load]);

  return (
    <div className="page">
      <div className="page__header">
        <h1>Materialarchiv</h1>
        {isAdmin && (
          <div className="button-row">
            <button type="button" className="btn btn--secondary" onClick={() => setShowCategories(true)}>
              Kategorien
            </button>
            <button type="button" className="btn btn--primary" onClick={() => setShowCreate(true)}>
              + Neues Material
            </button>
          </div>
        )}
      </div>

      <div className="filter-bar">
        <input
          type="search"
          placeholder="Material suchen…"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="filter-bar__search"
          inputMode="search"
        />
        <select
          value={categoryId}
          onChange={(e) => setCategoryId(e.target.value ? Number(e.target.value) : "")}
          className="filter-bar__select"
        >
          <option value="">Alle Kategorien</option>
          {categories.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </select>
      </div>

      {loading && <p className="hint">Lädt…</p>}
      {!loading && items.length === 0 && <p className="hint">Keine Treffer.</p>}

      <div className="item-grid">
        {items.map((item) => (
          <ItemCard key={item.id} item={item} onClick={() => setSelectedItem(item)} />
        ))}
      </div>

      {selectedItem && (
        <ItemDetailModal
          item={selectedItem}
          categories={categories}
          onClose={() => setSelectedItem(null)}
          onChanged={load}
        />
      )}

      {showCreate && (
        <CreateItemModal
          categories={categories}
          onClose={() => setShowCreate(false)}
          onCreated={() => {
            setShowCreate(false);
            load();
          }}
        />
      )}

      {showCategories && (
        <CategoriesModal
          categories={categories}
          onClose={() => setShowCategories(false)}
          onChanged={load}
        />
      )}
    </div>
  );
}
