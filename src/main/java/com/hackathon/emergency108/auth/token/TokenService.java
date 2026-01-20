package com.hackathon.emergency108.auth.token;

import com.hackathon.emergency108.entity.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;

@Service
public class TokenService {

    private static final String SECRET =
            "emergency108-super-secure-secret-key-which-is-long";

    // Token expiry: 100 years (effectively lifetime)
    private static final long EXPIRY_MILLIS = 100L * 365 * 24 * 60 * 60 * 1000;

    private final Key key =
            Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));



    public String generate(AuthTokenPayload payload) {

        Instant now = Instant.now();

        return Jwts.builder()
                .setSubject(String.valueOf(payload.getUserId()))
                .claim("role", payload.getRole().name())
                .setIssuedAt(Date.from(now))
                .setExpiration(new Date(now.toEpochMilli() + EXPIRY_MILLIS))
                .signWith(key)
                .compact();
    }

    public AuthTokenPayload validateAndParse(String token) {

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        Long userId = Long.valueOf(claims.getSubject());
        UserRole role = UserRole.valueOf(claims.get("role", String.class));

        return new AuthTokenPayload(userId, role);
    }
}
