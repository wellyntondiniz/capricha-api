package com.ca06.api.event;

import java.time.LocalDateTime;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateEventRequest(
        @NotBlank String name,
        String description,
        @NotNull LocalDateTime startsAt,
        @NotNull LocalDateTime endsAt,
        @NotBlank String location,
        LocalDateTime checkInStartsAt,
        LocalDateTime checkInEndsAt) {
}
