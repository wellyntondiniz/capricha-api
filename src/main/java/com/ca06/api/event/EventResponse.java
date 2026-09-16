package com.ca06.api.event;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

public record EventResponse(
        Long id,
        String name,
        String description,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime startsAt,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime endsAt,
        String location,
        String status,
        boolean canCheckIn,
        boolean hasCheckedIn,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime checkedInAt) {
}
