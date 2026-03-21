package com.phoenix.bookingservice.security;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtTokenService {

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    @Value("${security.jwt.issuer}")
    private String jwtIssuer;

    public CustomUserPrincipal parseToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

            Jws<Claims> parsedToken = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);

            Claims claims = parsedToken.getPayload();

            String issuer = claims.getIssuer();
            if (issuer == null || !issuer.equals(jwtIssuer)) {
                throw new JwtException("Invalid token issuer");
            }

            String userId = claims.getSubject();
            String email = claims.get("email", String.class);

            Object rawPermissions = claims.get("permissions");
            List<String> permissions = new ArrayList<>();

            if (rawPermissions instanceof List<?> rawList) {
                for (Object item : rawList) {
                    if (item != null) {
                        permissions.add(String.valueOf(item));
                    }
                }
            }

            return new CustomUserPrincipal(userId, email, permissions);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new JwtException("Invalid JWT token", ex);
        }
    }
}