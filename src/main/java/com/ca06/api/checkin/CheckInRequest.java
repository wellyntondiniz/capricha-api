package com.ca06.api.checkin;

import jakarta.validation.constraints.NotBlank;

public record CheckInRequest(@NotBlank String qrToken) {
}
