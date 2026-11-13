package com.archiv.ArchivMgnSystem.services.impl;

import com.archiv.ArchivMgnSystem.dtos.EventDTO;
import com.archiv.ArchivMgnSystem.dtos.EventParticipantDTO;
import com.archiv.ArchivMgnSystem.dtos.EventRequest;
import com.archiv.ArchivMgnSystem.dtos.Response;
import com.archiv.ArchivMgnSystem.dtos.ReservationDTO;
import com.archiv.ArchivMgnSystem.dtos.ReservationRequest;
import com.archiv.ArchivMgnSystem.enums.ParticipantStatus;
import com.archiv.ArchivMgnSystem.enums.UserRole;
import com.archiv.ArchivMgnSystem.exceptions.NotFoundException;
import com.archiv.ArchivMgnSystem.models.Event;
import com.archiv.ArchivMgnSystem.models.EventParticipant;
import com.archiv.ArchivMgnSystem.models.Item;
import com.archiv.ArchivMgnSystem.models.Reservation;
import com.archiv.ArchivMgnSystem.models.User;
import com.archiv.ArchivMgnSystem.repositories.EventParticipantRepository;
import com.archiv.ArchivMgnSystem.repositories.EventRepository;
import com.archiv.ArchivMgnSystem.repositories.ItemRepository;
import com.archiv.ArchivMgnSystem.repositories.ReservationRepository;
import com.archiv.ArchivMgnSystem.services.ActivityLogService;
import com.archiv.ArchivMgnSystem.services.EventService;
import com.archiv.ArchivMgnSystem.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private static final long NO_EXCLUDED_EVENT = -1L;

    private final EventRepository eventRepository;
    private final ReservationRepository reservationRepository;
    private final EventParticipantRepository eventParticipantRepository;
    private final ItemRepository itemRepository;
    private final UserService userService;
    private final ActivityLogService activityLog;

    private static final DateTimeFormatter LOG_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @Override
    public Response createEvent(EventRequest eventRequest) {
        if (eventRequest.getEndDate().isBefore(eventRequest.getStartDate())) {
            throw new IllegalArgumentException("End date must not be before start date");
        }

        User currentUser = userService.getCurrentLoggedInUser();

        Event event = Event.builder()
                .name(eventRequest.getName())
                .description(eventRequest.getDescription())
                .startDate(eventRequest.getStartDate())
                .endDate(eventRequest.getEndDate())
                .createdBy(currentUser)
                .build();

        eventRepository.save(event);
        activityLog.record(ActivityLogService.EVENT, "Event " + describe(event) + " angelegt");

        return Response.builder()
                .status(200)
                .message("Event successfully created")
                .event(toDto(event))
                .build();
    }

    @Override
    public Response getAllEvents() {
        List<Event> events = eventRepository.findAll(Sort.by(Sort.Direction.ASC, "startDate"));

        List<EventDTO> eventDTOS = events.stream().map(this::toDto).toList();

        return Response.builder()
                .status(200)
                .message("success")
                .events(eventDTOS)
                .build();
    }

    @Override
    public Response getEventById(Long id) {
        Event event = findEventOrThrow(id);

        return Response.builder()
                .status(200)
                .message("success")
                .event(toDto(event))
                .build();
    }

    @Override
    public Response deleteEvent(Long id) {
        Event event = findEventOrThrow(id);
        assertIsOwnerOrAdmin(event);

        int reservationCount = event.getReservations().size();
        eventRepository.delete(event);
        activityLog.record(ActivityLogService.EVENT, "Event " + describe(event) + " gelöscht"
                + (reservationCount > 0 ? " (samt " + reservationCount + " Reservierungen)" : ""));

        return Response.builder()
                .status(200)
                .message("Event successfully deleted")
                .build();
    }

    @Override
    public Response addReservation(Long eventId, ReservationRequest reservationRequest) {
        Event event = findEventOrThrow(eventId);
        assertCanManage(event);

        Item item = itemRepository.findById(reservationRequest.getItemId())
                .orElseThrow(() -> new NotFoundException("Item not found"));

        int alreadyReserved = reservationRepository.sumOverlappingReservedQuantity(
                item.getId(), NO_EXCLUDED_EVENT, event.getStartDate(), event.getEndDate());

        int available = item.getStockQuantity() - alreadyReserved;
        if (reservationRequest.getQuantity() > available) {
            throw new IllegalArgumentException(
                    "Not enough available stock for \"" + item.getName() + "\" in this period. Available: " + available);
        }

        Reservation reservation = Reservation.builder()
                .event(event)
                .item(item)
                .quantity(reservationRequest.getQuantity())
                .build();

        event.getReservations().add(reservation);
        reservationRepository.save(reservation);
        activityLog.record(ActivityLogService.EVENT, reservation.getQuantity() + " × \"" + item.getName()
                + "\" für Event " + describe(event) + " reserviert");

        return Response.builder()
                .status(200)
                .message("Material successfully reserved")
                .event(toDto(event))
                .build();
    }

    @Override
    public Response removeReservation(Long eventId, Long reservationId) {
        Event event = findEventOrThrow(eventId);
        assertCanManage(event);

        Reservation reservation = event.getReservations().stream()
                .filter(r -> r.getId().equals(reservationId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Reservation not found"));

        event.getReservations().remove(reservation);
        reservationRepository.delete(reservation);
        activityLog.record(ActivityLogService.EVENT, "Reservierung von " + reservation.getQuantity() + " × \""
                + (reservation.getItem() != null ? reservation.getItem().getName() : "?") + "\" für Event " + describe(event) + " entfernt");

        return Response.builder()
                .status(200)
                .message("Reservation successfully removed")
                .event(toDto(event))
                .build();
    }

    @Override
    public Response setReservationCollected(Long eventId, Long reservationId, boolean collected) {
        Event event = findEventOrThrow(eventId);
        assertCanManage(event);

        Reservation reservation = event.getReservations().stream()
                .filter(r -> r.getId().equals(reservationId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Reservation not found"));

        reservation.setCollected(collected);
        reservationRepository.save(reservation);

        return Response.builder()
                .status(200)
                .message("success")
                .event(toDto(event))
                .build();
    }

    @Override
    public Response requestToJoin(Long eventId) {
        Event event = findEventOrThrow(eventId);
        User currentUser = userService.getCurrentLoggedInUser();

        if (event.getCreatedBy() != null && event.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Du hast dieses Event selbst erstellt");
        }

        Optional<EventParticipant> existing =
                eventParticipantRepository.findByEventIdAndUserId(eventId, currentUser.getId());
        if (existing.isPresent()) {
            throw new IllegalArgumentException(
                    existing.get().getStatus() == ParticipantStatus.APPROVED
                            ? "Du bist bereits Teil dieses Events"
                            : "Du hast bereits eine Anfrage für dieses Event gestellt");
        }

        EventParticipant participant = EventParticipant.builder()
                .event(event)
                .user(currentUser)
                .status(ParticipantStatus.PENDING)
                .build();

        event.getParticipants().add(participant);
        eventParticipantRepository.save(participant);

        return Response.builder()
                .status(200)
                .message("Anfrage wurde gesendet und wartet auf Freigabe durch den Event-Ersteller")
                .event(toDto(event))
                .build();
    }

    @Override
    public Response approveParticipant(Long eventId, Long participantId) {
        Event event = findEventOrThrow(eventId);
        assertIsOwnerOrAdmin(event);

        EventParticipant participant = findParticipantOrThrow(event, participantId);
        participant.setStatus(ParticipantStatus.APPROVED);
        eventParticipantRepository.save(participant);

        return Response.builder()
                .status(200)
                .message("Anfrage freigegeben")
                .event(toDto(event))
                .build();
    }

    @Override
    public Response removeParticipant(Long eventId, Long participantId) {
        Event event = findEventOrThrow(eventId);
        User currentUser = userService.getCurrentLoggedInUser();
        EventParticipant participant = findParticipantOrThrow(event, participantId);

        boolean isSelf = participant.getUser() != null && participant.getUser().getId().equals(currentUser.getId());
        boolean isOwner = event.getCreatedBy() != null && event.getCreatedBy().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;
        if (!isSelf && !isOwner && !isAdmin) {
            throw new AccessDeniedException("You are not allowed to manage this event");
        }

        event.getParticipants().remove(participant);
        eventParticipantRepository.delete(participant);

        return Response.builder()
                .status(200)
                .message("Teilnahme entfernt")
                .event(toDto(event))
                .build();
    }

    private String describe(Event event) {
        String period = event.getStartDate().equals(event.getEndDate())
                ? event.getStartDate().format(LOG_DATE)
                : event.getStartDate().format(LOG_DATE) + "–" + event.getEndDate().format(LOG_DATE);
        return "\"" + event.getName() + "\" (" + period + ")";
    }

    private Event findEventOrThrow(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event not found"));
    }

    private EventParticipant findParticipantOrThrow(Event event, Long participantId) {
        return event.getParticipants().stream()
                .filter(p -> p.getId().equals(participantId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Participant not found"));
    }

    /** Material der Event-Liste verwalten: Ersteller, Admin, oder freigegebene Mithelfer. */
    private void assertCanManage(Event event) {
        User currentUser = userService.getCurrentLoggedInUser();
        boolean isOwner = event.getCreatedBy() != null && event.getCreatedBy().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;
        boolean isApprovedParticipant = event.getParticipants().stream()
                .anyMatch(p -> p.getUser() != null
                        && p.getUser().getId().equals(currentUser.getId())
                        && p.getStatus() == ParticipantStatus.APPROVED);
        if (!isOwner && !isAdmin && !isApprovedParticipant) {
            throw new AccessDeniedException("You are not allowed to manage this event");
        }
    }

    /** Event löschen oder Beitrittsanfragen entscheiden: nur Ersteller oder Admin. */
    private void assertIsOwnerOrAdmin(Event event) {
        User currentUser = userService.getCurrentLoggedInUser();
        boolean isOwner = event.getCreatedBy() != null && event.getCreatedBy().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("You are not allowed to manage this event");
        }
    }

    private EventDTO toDto(Event event) {
        List<ReservationDTO> reservationDTOS = event.getReservations() == null
                ? List.of()
                : event.getReservations().stream()
                    .map(r -> ReservationDTO.builder()
                            .id(r.getId())
                            .itemId(r.getItem().getId())
                            .itemName(r.getItem().getName())
                            .itemImageUrl(r.getItem().getImageUrl())
                            .quantity(r.getQuantity())
                            .collected(r.isCollected())
                            .createdAt(r.getCreatedAt())
                            .build())
                    .toList();

        List<EventParticipantDTO> participantDTOS = event.getParticipants() == null
                ? List.of()
                : event.getParticipants().stream()
                    .map(p -> EventParticipantDTO.builder()
                            .id(p.getId())
                            .userId(p.getUser() != null ? p.getUser().getId() : null)
                            .username(p.getUser() != null ? p.getUser().getUsername() : null)
                            .status(p.getStatus())
                            .build())
                    .toList();

        return EventDTO.builder()
                .id(event.getId())
                .name(event.getName())
                .description(event.getDescription())
                .startDate(event.getStartDate())
                .endDate(event.getEndDate())
                .createdById(event.getCreatedBy() != null ? event.getCreatedBy().getId() : null)
                .createdByName(event.getCreatedBy() != null ? event.getCreatedBy().getUsername() : null)
                .createdAt(event.getCreatedAt())
                .reservations(reservationDTOS)
                .participants(participantDTOS)
                .build();
    }
}
