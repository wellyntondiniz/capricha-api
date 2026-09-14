package com.ca06.api.auth;

public record LoginResponse(
        String token,
        Long userId,
        String name,
        String role) {
}
