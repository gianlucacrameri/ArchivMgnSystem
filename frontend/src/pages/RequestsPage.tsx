import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import type { TransactionDTO } from "../types";
import { fetchTransactions, updateTransactionStatus } from "../api/transactions";
import { ApiError } from "../api/client";
import { notifyAdminCountsChanged } from "../api/admin";
import { useToast } from "../context/ToastContext";
import RequestDetailModal from "../components/RequestDetailModal";
import { describeRequest, formatDateTime } from "../utils/describeRequest";

export default function RequestsPage() {
  const { showError, showSuccess } = useToast();
  const [pending, setPending] = useState<TransactionDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [processingId, setProcessingId] = useState<number | null>(null);
  const [selected, setSelected] = useState<TransactionDTO | null>(null);

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
    const request = pending.find((t) => t.id === id);
    if (
      status === "COMPLETED" &&
      request?.transactionType === "DELETE" &&
      !window.confirm(`"${request.item?.name ?? request.subjectName}" wirklich endgültig löschen?`)
    ) {
      return;
    }
    setProcessingId(id);
    try {
      await updateTransactionStatus(id, status);
      showSuccess(status === "COMPLETED" ? "Antrag freigegeben" : "Antrag abgelehnt");
      setPending((prev) => prev.filter((t) => t.id !== id));
      setSelected(null);
      notifyAdminCountsChanged();
    } catch (err) {
      showError(err instanceof ApiError ? err.message : "Aktion fehlgeschlagen");
    } finally {
      setProcessingId(null);
    }
  }

  return (
    <div className="page">
      <div className="page__header">
        <h1>Offene Anfragen</h1>
        <Link to="/history" className="btn btn--secondary">
          Verlauf
        </Link>
      </div>

      {loading && <p className="hint">Lädt…</p>}
      {!loading && pending.length === 0 && <p className="hint">Keine offenen Anfragen.</p>}

      <ul className="request-list">
        {pending.map((t) => (
          <li key={t.id} className="request-card">
            <button type="button" className="request-card__open" onClick={() => setSelected(t)}>
              <span className="request-card__info">
                <strong>{describeRequest(t)}</strong>
                <span className="event-card__meta">
                  Beantragt von {t.user?.username ?? "ehemaligem Mitglied"} am {formatDateTime(t.createdAt)}
                </span>
                {t.note && <span className="event-card__meta">Notiz: {t.note}</span>}
                <span className="request-card__more">Details ansehen</span>
              </span>
            </button>
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

      {selected && (
        <RequestDetailModal
          request={selected}
          processing={processingId === selected.id}
          onDecide={(status) => decide(selected.id, status)}
          onClose={() => setSelected(null)}
        />
      )}
    </div>
  );
}
