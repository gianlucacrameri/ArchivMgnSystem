import { useCallback, useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import type { EventDTO } from "../types";
import { deleteEvent, fetchEvent, removeReservation } from "../api/events";
import AuthenticatedImage from "../components/AuthenticatedImage";
import AddReservationModal from "../components/AddReservationModal";
import { ApiError } from "../api/client";
import { useToast } from "../context/ToastContext";
import { useAuth } from "../context/AuthContext";

function formatDate(isoDate: string): string {
  const [year, month, day] = isoDate.split("-").map(Number);
  return new Date(year, month - 1, day).toLocaleDateString("de-CH", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });
}

export default function EventDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { showError, showSuccess } = useToast();
  const { isAdmin, userId } = useAuth();

  const [event, setEvent] = useState<EventDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [showAddReservation, setShowAddReservation] = useState(false);

  const load = useCallback(async () => {
    if (!id) return;
    setLoading(true);
    try {
      setEvent(await fetchEvent(Number(id)));
    } catch (err) {
      showError(err instanceof ApiError ? err.message : "Event konnte nicht geladen werden");
    } finally {
      setLoading(false);
    }
  }, [id, showError]);

  useEffect(() => {
    load();
  }, [load]);

  if (loading) return <div className="page"><p className="hint">Lädt…</p></div>;
  if (!event) return <div className="page"><p className="hint">Event nicht gefunden.</p></div>;

  const canManage = isAdmin || (userId !== null && userId === event.createdById);

  async function handleRemoveReservation(reservationId: number) {
    if (!event) return;
    if (!window.confirm("Reservierung wirklich entfernen?")) return;
    try {
      setEvent(await removeReservation(event.id, reservationId));
      showSuccess("Reservierung entfernt");
    } catch (err) {
      showError(err instanceof ApiError ? err.message : "Konnte nicht entfernt werden");
    }
  }

  async function handleDeleteEvent() {
    if (!event) return;
    if (!window.confirm(`Event "${event.name}" wirklich löschen?`)) return;
    try {
      await deleteEvent(event.id);
      showSuccess("Event gelöscht");
      navigate("/events");
    } catch (err) {
      showError(err instanceof ApiError ? err.message : "Löschen fehlgeschlagen");
    }
  }

  return (
    <div className="page">
      <div className="page__header">
        <div>
          <h1>{event.name}</h1>
          <p className="event-card__meta">
            {event.startDate === event.endDate
              ? formatDate(event.startDate)
              : `${formatDate(event.startDate)} – ${formatDate(event.endDate)}`}
          </p>
          {event.createdByName && <p className="event-card__meta">Erstellt von {event.createdByName}</p>}
        </div>
      </div>

      {event.description && <p className="item-detail__description">{event.description}</p>}

      <div className="page__header">
        <h2 className="section-title">Reserviertes Material</h2>
        {canManage && (
          <button type="button" className="btn btn--primary" onClick={() => setShowAddReservation(true)}>
            + Material reservieren
          </button>
        )}
      </div>

      {(event.reservations?.length ?? 0) === 0 && <p className="hint">Noch kein Material reserviert.</p>}

      <ul className="reservation-list">
        {event.reservations?.map((r) => (
          <li key={r.id} className="reservation-row">
            <AuthenticatedImage src={r.itemImageUrl} alt={r.itemName} className="reservation-row__thumb" />
            <div className="reservation-row__info">
              <strong>{r.itemName}</strong>
              <span>{r.quantity} Stk.</span>
            </div>
            {canManage && (
              <button
                type="button"
                className="btn btn--link btn--danger-text"
                onClick={() => handleRemoveReservation(r.id)}
              >
                Entfernen
              </button>
            )}
          </li>
        ))}
      </ul>

      {canManage && (
        <>
          <hr className="divider" />
          <button type="button" className="btn btn--danger" onClick={handleDeleteEvent}>
            Event löschen
          </button>
        </>
      )}

      {showAddReservation && (
        <AddReservationModal
          event={event}
          onClose={() => setShowAddReservation(false)}
          onReserved={(updated) => {
            setEvent(updated);
            setShowAddReservation(false);
          }}
        />
      )}
    </div>
  );
}
