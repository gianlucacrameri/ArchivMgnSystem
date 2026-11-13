import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { fetchActivityLog, type ActivityLogEntry } from "../api/admin";
import { ApiError } from "../api/client";
import { useToast } from "../context/ToastContext";
import { formatDateTime } from "../utils/describeRequest";

const PAGE_SIZE = 50;

const categories: { value: string; label: string }[] = [
  { value: "", label: "Alle Bereiche" },
  { value: "BESTAND", label: "Bestand" },
  { value: "FOTO", label: "Fotos" },
  { value: "MATERIAL", label: "Material" },
  { value: "KATEGORIE", label: "Kategorien" },
  { value: "EVENT", label: "Events" },
  { value: "MITGLIED", label: "Mitglieder" },
];

const categoryLabels: Record<string, string> = {
  BESTAND: "Bestand",
  FOTO: "Foto",
  MATERIAL: "Material",
  KATEGORIE: "Kategorie",
  EVENT: "Event",
  MITGLIED: "Mitglied",
};

/** Verlauf des Archivs: wer hat wann was getan (nur für Admins). */
export default function HistoryPage() {
  const { showError } = useToast();
  const [entries, setEntries] = useState<ActivityLogEntry[]>([]);
  const [total, setTotal] = useState(0);
  const [search, setSearch] = useState("");
  const [category, setCategory] = useState("");
  const [visibleCount, setVisibleCount] = useState(PAGE_SIZE);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    const timeout = window.setTimeout(async () => {
      setLoading(true);
      try {
        const result = await fetchActivityLog({
          page: 0,
          size: visibleCount,
          category: category || undefined,
          search: search.trim() || undefined,
        });
        if (cancelled) return;
        setEntries(result.entries);
        setTotal(result.totalElements);
      } catch (err) {
        if (!cancelled) showError(err instanceof ApiError ? err.message : "Verlauf konnte nicht geladen werden");
      } finally {
        if (!cancelled) setLoading(false);
      }
    }, 250);
    return () => {
      cancelled = true;
      window.clearTimeout(timeout);
    };
  }, [search, category, visibleCount, showError]);

  return (
    <div className="page">
      <div className="page__header">
        <h1>Verlauf</h1>
        <Link to="/requests" className="btn btn--secondary">
          Offene Anfragen
        </Link>
      </div>
      <p className="hint">Alles, was im Archiv geändert wurde – neueste Einträge zuerst.</p>

      <div className="filter-bar">
        <input
          type="search"
          placeholder="Im Verlauf suchen…"
          value={search}
          onChange={(e) => {
            setSearch(e.target.value);
            setVisibleCount(PAGE_SIZE);
          }}
          className="filter-bar__search"
          inputMode="search"
        />
        <select
          value={category}
          onChange={(e) => {
            setCategory(e.target.value);
            setVisibleCount(PAGE_SIZE);
          }}
          className="filter-bar__select"
          aria-label="Bereich"
        >
          {categories.map((c) => (
            <option key={c.value} value={c.value}>
              {c.label}
            </option>
          ))}
        </select>
      </div>

      {loading && entries.length === 0 && <p className="hint">Lädt…</p>}
      {!loading && entries.length === 0 && <p className="hint">Keine Einträge.</p>}

      <ul className="history-list">
        {entries.map((entry) => (
          <li key={entry.id} className="history-entry">
            <span className="history-entry__meta">
              {formatDateTime(entry.createdAt)} · {entry.actorName}
              <span className="badge badge--neutral">{categoryLabels[entry.category] ?? entry.category}</span>
            </span>
            <span className="history-entry__message">{entry.message}</span>
          </li>
        ))}
      </ul>

      {entries.length > 0 && entries.length < total && (
        <div className="load-more">
          <p className="hint">
            {entries.length} von {total} Einträgen
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
    </div>
  );
}
