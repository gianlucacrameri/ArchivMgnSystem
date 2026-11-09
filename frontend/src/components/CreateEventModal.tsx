import { useState, type FormEvent } from "react";
import Modal from "./Modal";
import { createEvent } from "../api/events";
import { ApiError } from "../api/client";
import { useToast } from "../context/ToastContext";
import type { EventDTO } from "../types";

interface Props {
  onClose: () => void;
  onCreated: (event: EventDTO) => void;
}

function toDateInputValue(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

export default function CreateEventModal({ onClose, onCreated }: Props) {
  const { showError, showSuccess } = useToast();
  const today = toDateInputValue(new Date());
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [startDate, setStartDate] = useState(today);
  const [endDate, setEndDate] = useState(today);
  const [saving, setSaving] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setSaving(true);
    try {
      const event = await createEvent({
        name,
        description: description || undefined,
        startDate,
        endDate,
      });
      showSuccess("Event wurde angelegt");
      onCreated(event);
    } catch (err) {
      showError(err instanceof ApiError ? err.message : "Event konnte nicht angelegt werden");
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal title="Neuer Event" onClose={onClose}>
      <form onSubmit={handleSubmit} className="stacked-form">
        <label className="field">
          <span>Name</span>
          <input required value={name} onChange={(e) => setName(e.target.value)} />
        </label>
        <label className="field">
          <span>Von</span>
          <input
            type="date"
            required
            value={startDate}
            onChange={(e) => {
              setStartDate(e.target.value);
              if (e.target.value > endDate) setEndDate(e.target.value);
            }}
          />
        </label>
        <label className="field">
          <span>Bis</span>
          <input type="date" required min={startDate} value={endDate} onChange={(e) => setEndDate(e.target.value)} />
        </label>
        <label className="field">
          <span>Beschreibung (optional)</span>
          <textarea value={description} onChange={(e) => setDescription(e.target.value)} rows={3} />
        </label>

        <button type="submit" className="btn btn--primary btn--block" disabled={saving}>
          {saving ? "Wird angelegt…" : "Event anlegen"}
        </button>
      </form>
    </Modal>
  );
}
