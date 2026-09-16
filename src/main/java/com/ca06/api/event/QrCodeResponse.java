package com.ca06.api.event;

public record QrCodeResponse(
        String type,
        Long eventId,
        String eventName,
        String qrToken,
        String payload,
        String imageData) {
}
