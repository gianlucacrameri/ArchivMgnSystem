package com.archiv.ArchivMgnSystem.services;

import com.archiv.ArchivMgnSystem.dtos.EventRequest;
import com.archiv.ArchivMgnSystem.dtos.Response;
import com.archiv.ArchivMgnSystem.dtos.ReservationRequest;

public interface EventService {

    Response createEvent(EventRequest eventRequest);

    Response getAllEvents();

    Response getEventById(Long id);

    Response deleteEvent(Long id);

    Response addReservation(Long eventId, ReservationRequest reservationRequest);

    Response removeReservation(Long eventId, Long reservationId);

    Response setReservationCollected(Long eventId, Long reservationId, boolean collected);

    Response requestToJoin(Long eventId);

    Response approveParticipant(Long eventId, Long participantId);

    Response removeParticipant(Long eventId, Long participantId);
}
