package com.ca06.api.auth;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.ca06.api.user.AppUser;

@Service
public class TokenService {

    private final SecretKey signingKey;
    private final long tokenTtlHours;

    public TokenService(
            @Value("${app.auth.jwt-secret}") String jwtSecret,
            @Value("${app.auth.token-ttl-hours:8}") long tokenTtlHours) {
        if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT_SECRET deve possuir pelo menos 32 bytes.");
        }
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.tokenTtlHours = Math.max(1, tokenTtlHours);
    }

    public String issue(AppUser user) {
        Date issuedAt = new Date();
        Date expiration = new Date(issuedAt.getTime() + tokenTtlHours * 60 * 60 * 1000);
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("name", user.getName())
                .claim("role", user.getRole())
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(signingKey)
                .compact();
    }

    public AuthenticatedUser authenticate(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return new AuthenticatedUser(
                    Long.valueOf(claims.getSubject()),
                    claims.get("name", String.class),
                    claims.get("role", String.class));
        } catch (Exception exception) {
            return null;
        }
    }
}
