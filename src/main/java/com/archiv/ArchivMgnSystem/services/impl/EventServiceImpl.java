package com.archiv.ArchivMgnSystem.services.impl;

import com.archiv.ArchivMgnSystem.dtos.EventDTO;
import com.archiv.ArchivMgnSystem.dtos.EventRequest;
import com.archiv.ArchivMgnSystem.dtos.Response;
import com.archiv.ArchivMgnSystem.dtos.ReservationDTO;
import com.archiv.ArchivMgnSystem.dtos.ReservationRequest;
import com.archiv.ArchivMgnSystem.enums.UserRole;
import com.archiv.ArchivMgnSystem.exceptions.NotFoundException;
import com.archiv.ArchivMgnSystem.models.Event;
import com.archiv.ArchivMgnSystem.models.Item;
import com.archiv.ArchivMgnSystem.models.Reservation;
import com.archiv.ArchivMgnSystem.models.User;
import com.archiv.ArchivMgnSystem.repositories.EventRepository;
import com.archiv.ArchivMgnSystem.repositories.ItemRepository;
import com.archiv.ArchivMgnSystem.repositories.ReservationRepository;
import com.archiv.ArchivMgnSystem.services.EventService;
import com.archiv.ArchivMgnSystem.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private static final long NO_EXCLUDED_EVENT = -1L;

    private final EventRepository eventRepository;
    private final ReservationRepository reservationRepository;
    private final ItemRepository itemRepository;
    private final UserService userService;

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
        assertCanManage(event);

        eventRepository.delete(event);

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

        return Response.builder()
                .status(200)
                .message("Reservation successfully removed")
                .event(toDto(event))
                .build();
    }

    private Event findEventOrThrow(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event not found"));
    }

    private void assertCanManage(Event event) {
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
                            .createdAt(r.getCreatedAt())
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
                .build();
    }
}
