import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import type { EventDTO } from "../types";
import { fetchEvents } from "../api/events";
import CreateEventModal from "../components/CreateEventModal";
import { ApiError } from "../api/client";
import { useToast } from "../context/ToastContext";

function parseLocalDate(isoDate: string): Date {
  const [year, month, day] = isoDate.split("-").map(Number);
  return new Date(year, month - 1, day);
}

function formatRange(startDate: string, endDate: string): string {
  const dateFmt: Intl.DateTimeFormatOptions = { day: "2-digit", month: "2-digit", year: "numeric" };
  const start = parseLocalDate(startDate).toLocaleDateString("de-CH", dateFmt);
  if (startDate === endDate) return start;
  const end = parseLocalDate(endDate).toLocaleDateString("de-CH", dateFmt);
  return `${start} – ${end}`;
}

export default function EventsPage() {
  const { showError } = useToast();
  const [events, setEvents] = useState<EventDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setEvents(await fetchEvents());
    } catch (err) {
      showError(err instanceof ApiError ? err.message : "Events konnten nicht geladen werden");
    } finally {
      setLoading(false);
    }
  }, [showError]);

  useEffect(() => {
    load();
  }, [load]);

  return (
    <div className="page">
      <div className="page__header">
        <h1>Events</h1>
        <button type="button" className="btn btn--primary" onClick={() => setShowCreate(true)}>
          + Neuer Event
        </button>
      </div>

      {loading && <p className="hint">Lädt…</p>}
      {!loading && events.length === 0 && <p className="hint">Noch keine Events geplant.</p>}

      <ul className="event-list">
        {events.map((event) => (
          <li key={event.id}>
            <Link to={`/events/${event.id}`} className="event-card">
              <div className="event-card__body">
                <h3>{event.name}</h3>
                <p className="event-card__meta">{formatRange(event.startDate, event.endDate)}</p>
                {event.createdByName && <p className="event-card__meta">von {event.createdByName}</p>}
              </div>
              <span className="badge badge--neutral">
                {event.reservations?.length ?? 0} Material{(event.reservations?.length ?? 0) === 1 ? "" : "ien"}
              </span>
            </Link>
          </li>
        ))}
      </ul>

      {showCreate && (
        <CreateEventModal
          onClose={() => setShowCreate(false)}
          onCreated={() => {
            setShowCreate(false);
            load();
          }}
        />
      )}
    </div>
  );
}
