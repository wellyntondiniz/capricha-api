package com.ca06.api.checkin;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

public record ParticipantResponse(
        Long userId,
        String name,
        String email,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime checkedInAt) {
}
