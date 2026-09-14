package com.ca06.api.event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import com.ca06.api.auth.AuthenticatedUser;
import com.ca06.api.checkin.EventCheckIn;
import com.ca06.api.checkin.EventCheckInRepository;
import com.ca06.api.exception.ApiException;
import com.ca06.api.time.AppTime;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final EventCheckInRepository checkInRepository;
    private final AppTime appTime;

    public EventService(
            EventRepository eventRepository,
            EventCheckInRepository checkInRepository,
            AppTime appTime) {
        this.eventRepository = eventRepository;
        this.checkInRepository = checkInRepository;
        this.appTime = appTime;
    }

    public List<EventResponse> listForUser(AuthenticatedUser user) {
        LocalDateTime now = appTime.now();
        return eventRepository.findAll().stream()
                .map(event -> toResponse(event, user.id(), now))
                .toList();
    }

    public EventResponse findForUser(Long eventId, AuthenticatedUser user) {
        Event event = find(eventId);
        return toResponse(event, user.id(), appTime.now());
    }

    public Event find(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ApiException(
                        404,
                        "EVENT_NOT_FOUND",
                        "Evento não encontrado."));
    }

    public EventResponse create(CreateEventRequest request) {
        validateDates(request);
        Event event = new Event(
                request.name().trim(),
                request.description() == null ? null : request.description().trim(),
                request.startsAt(),
                request.endsAt(),
                request.location().trim(),
                request.checkInStartsAt(),
                request.checkInEndsAt());
        return toResponse(eventRepository.save(event), null, appTime.now());
    }

    private EventResponse toResponse(Event event, Long userId, LocalDateTime now) {
        EventCheckIn checkIn = userId == null
                ? null
                : checkInRepository.findByUserIdAndEventId(userId, event.getId()).orElse(null);
        EventStatus status = event.status(now);
        return new EventResponse(
                event.getId(),
                event.getName(),
                event.getDescription(),
                event.getStartsAt(),
                event.getEndsAt(),
                event.getLocation(),
                status.name().toLowerCase(Locale.ROOT),
                event.isCheckInAvailable(now),
                checkIn != null,
                checkIn == null ? null : checkIn.getCheckedInAt());
    }

    private void validateDates(CreateEventRequest request) {
        if (!request.endsAt().isAfter(request.startsAt())) {
            throw new ApiException(400, "INVALID_EVENT_DATES", "O fim do evento deve ser posterior ao início.");
        }
        if (request.checkInStartsAt() != null
                && request.checkInEndsAt() != null
                && !request.checkInEndsAt().isAfter(request.checkInStartsAt())) {
            throw new ApiException(400, "INVALID_CHECK_IN_WINDOW", "A janela de check-in é inválida.");
        }
    }
}
