package com.ca06.api.checkin;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ca06.api.auth.AuthenticatedUser;

@RestController
@RequestMapping("/api/events")
public class CheckInController {

    private final EventCheckInService eventCheckInService;

    public CheckInController(EventCheckInService eventCheckInService) {
        this.eventCheckInService = eventCheckInService;
    }

    @PostMapping("/{eventId}/check-in")
    public ResponseEntity<CheckInResponse> checkIn(
            @PathVariable Long eventId,
            @Valid @RequestBody CheckInRequest request,
            Authentication authentication) {
        CheckInResponse response = eventCheckInService.checkIn(
                eventId,
                request,
                (AuthenticatedUser) authentication.getPrincipal());
        return ResponseEntity.status(201).body(response);
    }
}
