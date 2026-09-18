import { useCallback, useEffect, useState } from "react";
import type { TransactionDTO } from "../types";
import { fetchTransactions, updateTransactionStatus } from "../api/transactions";
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

export default function RequestsPage() {
  const { showError, showSuccess } = useToast();
  const [pending, setPending] = useState<TransactionDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [processingId, setProcessingId] = useState<number | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const { transactions } = await fetchTransactions({ filter: "PENDING", size: 200 });
      setPending(transactions.filter((t) => t.transactionStatus === "PENDING"));
    } catch (err) {
      showError(err instanceof ApiError ? err.message : "Anfragen konnten nicht geladen werden");
    } finally {
      setLoading(false);
    }
  }, [showError]);

  useEffect(() => {
    load();
  }, [load]);

  async function decide(id: number, status: "COMPLETED" | "CANCELED") {
    setProcessingId(id);
    try {
      await updateTransactionStatus(id, status);
      showSuccess(status === "COMPLETED" ? "Antrag freigegeben" : "Antrag abgelehnt");
      setPending((prev) => prev.filter((t) => t.id !== id));
    } catch (err) {
      showError(err instanceof ApiError ? err.message : "Aktion fehlgeschlagen");
    } finally {
      setProcessingId(null);
    }
  }

  return (
    <div className="page">
      <div className="page__header">
        <h1>Offene Bestandsänderungen</h1>
      </div>

      {loading && <p className="hint">Lädt…</p>}
      {!loading && pending.length === 0 && <p className="hint">Keine offenen Anfragen. 🎉</p>}

      <ul className="request-list">
        {pending.map((t) => (
          <li key={t.id} className="request-card">
            <div className="request-card__info">
              <strong>
                {t.transactionType === "ADD" ? "+ " : "− "}
                {t.totalItems} × {t.item?.name ?? "Unbekanntes Material"}
              </strong>
              <span className="event-card__meta">
                Beantragt von {t.user?.username ?? "?"} am {formatDateTime(t.createdAt)}
              </span>
              {t.note && <span className="event-card__meta">Notiz: {t.note}</span>}
            </div>
            <div className="button-row">
              <button
                type="button"
                className="btn btn--primary"
                disabled={processingId === t.id}
                onClick={() => decide(t.id, "COMPLETED")}
              >
                Freigeben
              </button>
              <button
                type="button"
                className="btn btn--secondary"
                disabled={processingId === t.id}
                onClick={() => decide(t.id, "CANCELED")}
              >
                Ablehnen
              </button>
            </div>
          </li>
        ))}
      </ul>
    </div>
  );
}
