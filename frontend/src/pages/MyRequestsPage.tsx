import { useCallback, useEffect, useState } from "react";
import type { CategoryDTO, TransactionDTO, TransactionStatus } from "../types";
import { fetchCategories } from "../api/categories";
import NewItemRequestModal from "../components/NewItemRequestModal";
import DeleteRequestModal from "../components/DeleteRequestModal";
import { fetchMyTransactions } from "../api/transactions";
import { ApiError } from "../api/client";
import { describeRequest } from "../utils/describeRequest";
import { useToast } from "../context/ToastContext";

function formatDateTime(value: string): string {
  return new Date(value).toLocaleString("de-CH", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

const statusLabels: Record<TransactionStatus, { text: string; className: string }> = {
  PENDING: { text: "Wartet auf Freigabe", className: "badge--neutral" },
  COMPLETED: { text: "Freigegeben", className: "badge--success" },
  CANCELED: { text: "Abgelehnt", className: "badge--warning" },
};

export default function MyRequestsPage() {
  const { showError } = useToast();
  const [requests, setRequests] = useState<TransactionDTO[]>([]);
  const [loading, setLoading] = useState(true);

  const [categories, setCategories] = useState<CategoryDTO[]>([]);
  const [dialog, setDialog] = useState<"new" | "delete" | null>(null);

  const load = useCallback(() => {
    fetchMyTransactions()
      .then(setRequests)
      .catch((err) => showError(err instanceof ApiError ? err.message : "Anträge konnten nicht geladen werden"))
      .finally(() => setLoading(false));
  }, [showError]);

  useEffect(() => {
    load();
  }, [load]);

  async function openNewItemDialog() {
    try {
      setCategories(await fetchCategories());
      setDialog("new");
    } catch (err) {
      showError(err instanceof ApiError ? err.message : "Kategorien konnten nicht geladen werden");
    }
  }

  return (
    <div className="page">
      <div className="page__header">
        <h1>Meine Anträge</h1>
        <div className="button-row">
          <button type="button" className="btn btn--primary" onClick={openNewItemDialog}>
            + Neues Material
          </button>
          <button type="button" className="btn btn--secondary" onClick={() => setDialog("delete")}>
            Löschung beantragen
          </button>
        </div>
      </div>
      <p className="hint">
        Hier beantragst du, ein neues Material aufzunehmen oder eines zu löschen. Bestand und Fotos änderst du
        direkt beim Material im Archiv. Ein Admin entscheidet über jeden Antrag.
      </p>

      {loading && <p className="hint">Lädt…</p>}
      {!loading && requests.length === 0 && (
        <p className="hint">
Du hast noch nichts beantragt.
        </p>
      )}

      <ul className="request-list">
        {requests.map((t) => {
          const status = statusLabels[t.transactionStatus];
          return (
            <li key={t.id} className="request-card">
              <div className="request-card__info">
                <strong>{describeRequest(t, "gelöschtes Material")}</strong>
                <span className="event-card__meta">Beantragt am {formatDateTime(t.createdAt)}</span>
                {t.note && <span className="event-card__meta">Notiz: {t.note}</span>}
              </div>
              <span className={`badge ${status.className}`}>{status.text}</span>
            </li>
          );
        })}
      </ul>

      {dialog === "new" && (
        <NewItemRequestModal
          categories={categories}
          onClose={() => setDialog(null)}
          onRequested={() => {
            setDialog(null);
            load();
          }}
        />
      )}
      {dialog === "delete" && (
        <DeleteRequestModal
          onClose={() => setDialog(null)}
          onRequested={() => {
            setDialog(null);
            load();
          }}
        />
      )}
    </div>
  );
}
