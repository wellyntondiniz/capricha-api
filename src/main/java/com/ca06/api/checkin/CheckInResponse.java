package com.ca06.api.checkin;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

public record CheckInResponse(
        Long id,
        Long eventId,
        String eventName,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime checkedInAt,
        String message) {
}
