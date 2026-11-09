import { apiGet, apiSend } from "./client";
import type { EventDTO } from "../types";

export async function fetchEvents(): Promise<EventDTO[]> {
  const res = await apiGet("/api/events");
  return res.events ?? [];
}

export async function fetchEvent(id: number): Promise<EventDTO> {
  const res = await apiGet(`/api/events/${id}`);
  if (!res.event) throw new Error("Event nicht gefunden");
  return res.event;
}

export async function createEvent(input: {
  name: string;
  description?: string;
  startDate: string;
  endDate: string;
}): Promise<EventDTO> {
  const res = await apiSend("POST", "/api/events", input);
  if (!res.event) throw new Error("Unerwartete Antwort vom Server");
  return res.event;
}

export async function deleteEvent(id: number): Promise<void> {
  await apiSend("DELETE", `/api/events/${id}`);
}

export async function addReservation(
  eventId: number,
  input: { itemId: number; quantity: number }
): Promise<EventDTO> {
  const res = await apiSend("POST", `/api/events/${eventId}/reservations`, input);
  if (!res.event) throw new Error("Unerwartete Antwort vom Server");
  return res.event;
}

export async function removeReservation(eventId: number, reservationId: number): Promise<EventDTO> {
  const res = await apiSend("DELETE", `/api/events/${eventId}/reservations/${reservationId}`);
  if (!res.event) throw new Error("Unerwartete Antwort vom Server");
  return res.event;
}

export async function setReservationCollected(
  eventId: number,
  reservationId: number,
  collected: boolean
): Promise<EventDTO> {
  const res = await apiSend("PUT", `/api/events/${eventId}/reservations/${reservationId}/collected`, collected);
  if (!res.event) throw new Error("Unerwartete Antwort vom Server");
  return res.event;
}

export async function requestToJoinEvent(eventId: number): Promise<EventDTO> {
  const res = await apiSend("POST", `/api/events/${eventId}/join`);
  if (!res.event) throw new Error("Unerwartete Antwort vom Server");
  return res.event;
}

export async function approveParticipant(eventId: number, participantId: number): Promise<EventDTO> {
  const res = await apiSend("PUT", `/api/events/${eventId}/participants/${participantId}/approve`);
  if (!res.event) throw new Error("Unerwartete Antwort vom Server");
  return res.event;
}

export async function removeParticipant(eventId: number, participantId: number): Promise<EventDTO> {
  const res = await apiSend("DELETE", `/api/events/${eventId}/participants/${participantId}`);
  if (!res.event) throw new Error("Unerwartete Antwort vom Server");
  return res.event;
}
