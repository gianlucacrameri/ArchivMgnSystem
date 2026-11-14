package com.archiv.ArchivMgnSystem.security;

import com.archiv.ArchivMgnSystem.models.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

@Service
public class JwtUtil {

    private static final String TOKEN_VERSION_CLAIM = "ver";

    private static final int MIN_SECRET_BYTES = 32;

    private SecretKey key;

    @Value("${jwt.secret}")
    private String secretJwtString;

    /** Gültigkeit eines Logins in Tagen. Danach muss man sich neu anmelden. */
    @Value("${jwt.expiration-days:30}")
    private long expirationDays;

    @PostConstruct
    private void init() {
        byte[] keyByte = secretJwtString.getBytes(StandardCharsets.UTF_8);
        // Mit einem kurzen Secret liessen sich Login-Tokens durch Ausprobieren fälschen.
        if (keyByte.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("JWT_SECRET ist zu kurz: mindestens " + MIN_SECRET_BYTES
                    + " Zeichen nötig (z.B. mit 'openssl rand -base64 48' erzeugen)");
        }
        this.key = new SecretKeySpec(keyByte, "HmacSHA256");
    }

    public String generateToken(User user) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim(TOKEN_VERSION_CLAIM, user.getTokenVersion())
                .issuedAt(new Date(now))
                .expiration(new Date(now + Duration.ofDays(expirationDays).toMillis()))
                .signWith(key)
                .compact();
    }

    public long getExpirationDays() {
        return expirationDays;
    }

    /** Prüft Signatur und Ablaufdatum; wirft eine JwtException, wenn etwas nicht stimmt. */
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    /**
     * Ein Token gilt nur für genau den Benutzer und genau die Token-Version,
     * für die es ausgestellt wurde. Tokens von vor der Einführung der Version
     * tragen keine und zählen als Version 0.
     */
    public boolean belongsTo(Claims claims, User user) {
        Integer version = claims.get(TOKEN_VERSION_CLAIM, Integer.class);
        return user.getEmail().equals(claims.getSubject())
                && (version == null ? 0 : version) == user.getTokenVersion();
    }
}
