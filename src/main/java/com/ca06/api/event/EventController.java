package com.ca06.api.event;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ca06.api.auth.AuthenticatedUser;
import com.ca06.api.checkin.EventCheckInService;
import com.ca06.api.checkin.ParticipantResponse;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;
    private final QrCodeService qrCodeService;
    private final EventCheckInService eventCheckInService;

    public EventController(
            EventService eventService,
            QrCodeService qrCodeService,
            EventCheckInService eventCheckInService) {
        this.eventService = eventService;
        this.qrCodeService = qrCodeService;
        this.eventCheckInService = eventCheckInService;
    }

    @GetMapping
    public List<EventResponse> list(Authentication authentication) {
        return eventService.listForUser(currentUser(authentication));
    }

    @GetMapping("/{eventId}")
    public EventResponse find(
            @PathVariable Long eventId,
            Authentication authentication) {
        return eventService.findForUser(eventId, currentUser(authentication));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
    public ResponseEntity<EventResponse> create(@Valid @RequestBody CreateEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.create(request));
    }

    @GetMapping("/{eventId}/qr-code")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
    public QrCodeResponse qrCode(@PathVariable Long eventId) {
        return qrCodeService.generate(eventService.find(eventId));
    }

    @GetMapping("/{eventId}/participants")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
    public List<ParticipantResponse> participants(@PathVariable Long eventId) {
        eventService.find(eventId);
        return eventCheckInService.listParticipants(eventId);
    }

    private AuthenticatedUser currentUser(Authentication authentication) {
        return (AuthenticatedUser) authentication.getPrincipal();
    }
}
