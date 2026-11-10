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

/** So viele Artikel werden auf einmal geladen; "Mehr laden" holt die nächsten. */
const PAGE_SIZE = 60;

export default function ItemsPage() {
  const { isAdmin } = useAuth();
  const { showError } = useToast();

  const [items, setItems] = useState<ItemDTO[]>([]);
  const [totalItems, setTotalItems] = useState(0);
  const [visibleCount, setVisibleCount] = useState(PAGE_SIZE);
  const [categories, setCategories] = useState<CategoryDTO[]>([]);
  const [search, setSearch] = useState("");
  const [categoryId, setCategoryId] = useState<number | "">("");
  const [loading, setLoading] = useState(true);
  const [selectedItem, setSelectedItem] = useState<ItemDTO | null>(null);
  const [showCreate, setShowCreate] = useState(false);
  const [showCategories, setShowCategories] = useState(false);

  const loadItems = useCallback(async () => {
    setLoading(true);
    try {
      const itemsPage = await fetchItems({
        filter: search || undefined,
        categoryId: categoryId || undefined,
        size: visibleCount,
      });
      setItems(itemsPage.items);
      setTotalItems(itemsPage.totalElements);
    } catch (err) {
      showError(err instanceof ApiError ? err.message : "Archiv konnte nicht geladen werden");
    } finally {
      setLoading(false);
    }
  }, [search, categoryId, visibleCount, showError]);

  const loadCategories = useCallback(async () => {
    try {
      setCategories(await fetchCategories());
    } catch (err) {
      showError(err instanceof ApiError ? err.message : "Kategorien konnten nicht geladen werden");
    }
  }, [showError]);

  // Kategorien ändern sich selten - nur einmal laden, nicht bei jeder Suche.
  useEffect(() => {
    loadCategories();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    const timeout = window.setTimeout(loadItems, 250);
    return () => window.clearTimeout(timeout);
  }, [loadItems]);

  return (
    <div className="page">
      <div className="page__header">
        <h1>Archiv Turnverein Wettingen</h1>
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
          onChange={(e) => {
            setSearch(e.target.value);
            setVisibleCount(PAGE_SIZE);
          }}
          className="filter-bar__search"
          inputMode="search"
        />
        <select
          value={categoryId}
          onChange={(e) => {
            setCategoryId(e.target.value ? Number(e.target.value) : "");
            setVisibleCount(PAGE_SIZE);
          }}
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

      {loading && items.length === 0 && <p className="hint">Lädt…</p>}
      {!loading && items.length === 0 && <p className="hint">Keine Treffer.</p>}

      <div className="item-grid">
        {items.map((item) => (
          <ItemCard key={item.id} item={item} onClick={() => setSelectedItem(item)} />
        ))}
      </div>

      {items.length > 0 && items.length < totalItems && (
        <div className="load-more">
          <p className="hint">
            {items.length} von {totalItems} Artikeln
          </p>
          <button
            type="button"
            className="btn btn--secondary"
            disabled={loading}
            onClick={() => setVisibleCount((count) => count + PAGE_SIZE)}
          >
            {loading ? "Lädt…" : "Mehr laden"}
          </button>
        </div>
      )}

      {selectedItem && (
        <ItemDetailModal
          item={selectedItem}
          categories={categories}
          onClose={() => setSelectedItem(null)}
          onChanged={loadItems}
        />
      )}

      {showCreate && (
        <CreateItemModal
          categories={categories}
          onClose={() => setShowCreate(false)}
          onCreated={() => {
            setShowCreate(false);
            loadItems();
            loadCategories();
          }}
        />
      )}

      {showCategories && (
        <CategoriesModal
          categories={categories}
          onClose={() => setShowCategories(false)}
          onChanged={() => {
            loadItems();
            loadCategories();
          }}
        />
      )}
    </div>
  );
}
