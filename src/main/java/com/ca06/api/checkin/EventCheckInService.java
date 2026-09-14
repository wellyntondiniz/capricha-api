package com.ca06.api.checkin;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ca06.api.auth.AuthenticatedUser;
import com.ca06.api.event.Event;
import com.ca06.api.event.EventRepository;
import com.ca06.api.exception.ApiException;
import com.ca06.api.time.AppTime;
import com.ca06.api.user.AppUser;
import com.ca06.api.user.UserRepository;

@Service
public class EventCheckInService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EventCheckInRepository checkInRepository;
    private final AppTime appTime;

    public EventCheckInService(
            EventRepository eventRepository,
            UserRepository userRepository,
            EventCheckInRepository checkInRepository,
            AppTime appTime) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.checkInRepository = checkInRepository;
        this.appTime = appTime;
    }

    @Transactional
    public CheckInResponse checkIn(Long eventId, CheckInRequest request, AuthenticatedUser authenticatedUser) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ApiException(404, "EVENT_NOT_FOUND", "Evento não encontrado."));
        if (!sameToken(event.getCheckInToken(), request.qrToken())) {
            throw new ApiException(400, "INVALID_EVENT_TOKEN", "QR Code inválido para este evento.");
        }

        LocalDateTime now = appTime.now();
        if (!event.isCheckInAvailable(now)) {
            throw new ApiException(409, "CHECK_IN_NOT_AVAILABLE", "Check-in não disponível neste momento.");
        }

        if (checkInRepository.existsByUserIdAndEventId(authenticatedUser.id(), event.getId())) {
            throw new ApiException(409, "ALREADY_CHECKED_IN", "Você já realizou check-in neste evento.");
        }
        AppUser user = userRepository.findById(authenticatedUser.id())
                .orElseThrow(() -> new ApiException(401, "AUTH_REQUIRED", "Sua sessão expirou. Faça login novamente."));

        try {
            EventCheckIn checkIn = checkInRepository.saveAndFlush(new EventCheckIn(user, event, now));
            return new CheckInResponse(
                    checkIn.getId(),
                    event.getId(),
                    event.getName(),
                    checkIn.getCheckedInAt(),
                    "Check-in realizado com sucesso!");
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(409, "ALREADY_CHECKED_IN", "Você já realizou check-in neste evento.");
        }
    }

    @Transactional(readOnly = true)
    public List<ParticipantResponse> listParticipants(Long eventId) {
        return checkInRepository.findAllByEventIdOrderByCheckedInAtAsc(eventId).stream()
                .map(checkIn -> new ParticipantResponse(
                        checkIn.getUser().getId(),
                        checkIn.getUser().getName(),
                        checkIn.getUser().getEmail(),
                        checkIn.getCheckedInAt()))
                .toList();
    }

    private boolean sameToken(String expected, String actual) {
        if (actual == null || actual.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.trim().getBytes(StandardCharsets.UTF_8));
    }
}
