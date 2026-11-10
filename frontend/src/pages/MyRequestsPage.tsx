import { useEffect, useState } from "react";
import type { TransactionDTO, TransactionStatus } from "../types";
import { fetchMyTransactions } from "../api/transactions";
import { ApiError } from "../api/client";
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

  useEffect(() => {
    fetchMyTransactions()
      .then(setRequests)
      .catch((err) => showError(err instanceof ApiError ? err.message : "Anträge konnten nicht geladen werden"))
      .finally(() => setLoading(false));
  }, [showError]);

  return (
    <div className="page">
      <div className="page__header">
        <h1>Meine Anträge</h1>
      </div>

      {loading && <p className="hint">Lädt…</p>}
      {!loading && requests.length === 0 && (
        <p className="hint">
          Du hast noch keine Bestandsänderung beantragt. Öffne im Archiv ein Material, um eine zu beantragen.
        </p>
      )}

      <ul className="request-list">
        {requests.map((t) => {
          const status = statusLabels[t.transactionStatus];
          return (
            <li key={t.id} className="request-card">
              <div className="request-card__info">
                <strong>
                  {t.transactionType === "ADD" ? "+ " : "− "}
                  {t.totalItems} × {t.item?.name ?? "Gelöschtes Material"}
                </strong>
                <span className="event-card__meta">Beantragt am {formatDateTime(t.createdAt)}</span>
                {t.note && <span className="event-card__meta">Notiz: {t.note}</span>}
              </div>
              <span className={`badge ${status.className}`}>{status.text}</span>
            </li>
          );
        })}
      </ul>
    </div>
  );
}
