package com.phoenix.bookingservice.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class InternalServiceTokenProvider {

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    @Value("${security.jwt.issuer}")
    private String jwtIssuer;

    @Value("${security.internal.service-id:booking-service}")
    private String serviceId;

    @Value("${security.internal.service-token-ttl-seconds:300}")
    private long serviceTokenTtlSeconds;

    public String createServiceToken() {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(Math.max(serviceTokenTtlSeconds, 60));

        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .issuer(jwtIssuer)
                .subject(serviceId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .claim("typ", "service")
                .signWith(key)
                .compact();
    }
}
