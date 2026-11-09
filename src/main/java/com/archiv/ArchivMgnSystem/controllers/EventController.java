package com.archiv.ArchivMgnSystem.controllers;

import com.archiv.ArchivMgnSystem.dtos.EventRequest;
import com.archiv.ArchivMgnSystem.dtos.Response;
import com.archiv.ArchivMgnSystem.dtos.ReservationRequest;
import com.archiv.ArchivMgnSystem.services.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping
    public ResponseEntity<Response> createEvent(@RequestBody @Valid EventRequest eventRequest) {
        return ResponseEntity.ok(eventService.createEvent(eventRequest));
    }

    @GetMapping
    public ResponseEntity<Response> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Response> getEventById(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Response> deleteEvent(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.deleteEvent(id));
    }

    @PostMapping("/{id}/reservations")
    public ResponseEntity<Response> addReservation(
            @PathVariable Long id,
            @RequestBody @Valid ReservationRequest reservationRequest) {
        return ResponseEntity.ok(eventService.addReservation(id, reservationRequest));
    }

    @DeleteMapping("/{id}/reservations/{reservationId}")
    public ResponseEntity<Response> removeReservation(
            @PathVariable Long id,
            @PathVariable Long reservationId) {
        return ResponseEntity.ok(eventService.removeReservation(id, reservationId));
    }

    @PutMapping("/{id}/reservations/{reservationId}/collected")
    public ResponseEntity<Response> setReservationCollected(
            @PathVariable Long id,
            @PathVariable Long reservationId,
            @RequestBody boolean collected) {
        return ResponseEntity.ok(eventService.setReservationCollected(id, reservationId, collected));
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<Response> requestToJoin(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.requestToJoin(id));
    }

    @PutMapping("/{id}/participants/{participantId}/approve")
    public ResponseEntity<Response> approveParticipant(
            @PathVariable Long id,
            @PathVariable Long participantId) {
        return ResponseEntity.ok(eventService.approveParticipant(id, participantId));
    }

    @DeleteMapping("/{id}/participants/{participantId}")
    public ResponseEntity<Response> removeParticipant(
            @PathVariable Long id,
            @PathVariable Long participantId) {
        return ResponseEntity.ok(eventService.removeParticipant(id, participantId));
    }
}
