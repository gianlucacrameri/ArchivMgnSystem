import { useCallback, useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import type { EventDTO } from "../types";
import {
  approveParticipant,
  deleteEvent,
  fetchEvent,
  removeParticipant,
  removeReservation,
  requestToJoinEvent,
  setReservationCollected,
} from "../api/events";
import AuthenticatedImage from "../components/AuthenticatedImage";
import AddReservationModal from "../components/AddReservationModal";
import { IconCheck } from "../components/Icons";
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

  const isOwner = userId !== null && userId === event.createdById;
  const myParticipation = event.participants?.find((p) => p.userId === userId);
  const isApprovedHelper = myParticipation?.status === "APPROVED";
  const canManage = isAdmin || isOwner || isApprovedHelper;
  const canManageEvent = isAdmin || isOwner;
  const canRequestToJoin = !isOwner && !isAdmin && !myParticipation;
  const pendingParticipants = event.participants?.filter((p) => p.status === "PENDING") ?? [];
  const approvedParticipants = event.participants?.filter((p) => p.status === "APPROVED") ?? [];

  async function handleToggleCollected(reservationId: number, collected: boolean) {
    if (!event) return;
    try {
      setEvent(await setReservationCollected(event.id, reservationId, collected));
    } catch (err) {
      showError(err instanceof ApiError ? err.message : "Konnte nicht aktualisiert werden");
    }
  }

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

  async function handleJoinRequest() {
    if (!event) return;
    try {
      setEvent(await requestToJoinEvent(event.id));
      showSuccess("Anfrage gesendet und wartet auf Freigabe");
    } catch (err) {
      showError(err instanceof ApiError ? err.message : "Anfrage fehlgeschlagen");
    }
  }

  async function handleApprove(participantId: number) {
    if (!event) return;
    try {
      setEvent(await approveParticipant(event.id, participantId));
      showSuccess("Anfrage freigegeben");
    } catch (err) {
      showError(err instanceof ApiError ? err.message : "Freigeben fehlgeschlagen");
    }
  }

  async function handleRemoveParticipant(participantId: number) {
    if (!event) return;
    try {
      setEvent(await removeParticipant(event.id, participantId));
    } catch (err) {
      showError(err instanceof ApiError ? err.message : "Aktion fehlgeschlagen");
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

      {(event.reservations?.length ?? 0) > 0 && (
        <p className="hint">
          {event.reservations!.filter((r) => r.collected).length} von {event.reservations!.length} geholt
        </p>
      )}

      <ul className="reservation-list">
        {event.reservations?.map((r) => (
          <li key={r.id} className={`reservation-row ${r.collected ? "reservation-row--collected" : ""}`}>
            {canManage ? (
              <input
                type="checkbox"
                checked={r.collected}
                onChange={(e) => handleToggleCollected(r.id, e.target.checked)}
                aria-label={`${r.itemName} als geholt markieren`}
              />
            ) : (
              r.collected && <IconCheck className="reservation-row__check" />
            )}
            <AuthenticatedImage src={r.itemImageUrl} alt={r.itemName} size="thumb" className="reservation-row__thumb" />
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

      <hr className="divider" />

      <h2 className="section-title">Mithelfer</h2>

      {canRequestToJoin && (
        <div className="stacked-form">
          <p className="hint">Du kannst anfragen, bei diesem Event mitzuhelfen und die Materialliste mitzupflegen.</p>
          <button type="button" className="btn btn--secondary" onClick={handleJoinRequest}>
            Anfrage stellen
          </button>
        </div>
      )}

      {myParticipation?.status === "PENDING" && (
        <p className="hint">Deine Anfrage wartet auf Freigabe durch den Event-Ersteller.</p>
      )}

      {isApprovedHelper && (
        <div className="reservation-row">
          <div className="reservation-row__info">
            <strong>Du hilfst bei diesem Event mit</strong>
          </div>
          <button
            type="button"
            className="btn btn--link btn--danger-text"
            onClick={() => handleRemoveParticipant(myParticipation!.id)}
          >
            Nicht mehr mithelfen
          </button>
        </div>
      )}

      {canManageEvent && pendingParticipants.length > 0 && (
        <>
          <p className="hint">Offene Anfragen</p>
          <ul className="reservation-list">
            {pendingParticipants.map((p) => (
              <li key={p.id} className="reservation-row">
                <div className="reservation-row__info">
                  <strong>{p.username}</strong>
                </div>
                <div className="button-row">
                  <button type="button" className="btn btn--link" onClick={() => handleApprove(p.id)}>
                    Freigeben
                  </button>
                  <button
                    type="button"
                    className="btn btn--link btn--danger-text"
                    onClick={() => handleRemoveParticipant(p.id)}
                  >
                    Ablehnen
                  </button>
                </div>
              </li>
            ))}
          </ul>
        </>
      )}

      {approvedParticipants.length > 0 && (
        <>
          <p className="hint">Helfen aktuell mit</p>
          <ul className="reservation-list">
            {approvedParticipants.map((p) => (
              <li key={p.id} className="reservation-row">
                <div className="reservation-row__info">
                  <strong>{p.username}</strong>
                </div>
                {canManageEvent && (
                  <button
                    type="button"
                    className="btn btn--link btn--danger-text"
                    onClick={() => handleRemoveParticipant(p.id)}
                  >
                    Entfernen
                  </button>
                )}
              </li>
            ))}
          </ul>
        </>
      )}

      {canManageEvent && (
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
