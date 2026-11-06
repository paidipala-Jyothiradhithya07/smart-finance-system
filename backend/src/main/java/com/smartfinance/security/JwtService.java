package com.smartfinance.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component 
public class JwtService {
	 private static final int MIN_SECRET_BYTES = 32;

	    private final SecretKey signingKey;
	    private final long expirationSeconds;

	    public JwtService(@Value("${app.jwt.secret}") String secret,
	                      @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
	        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
	        if (keyBytes.length < MIN_SECRET_BYTES) {
	            throw new IllegalStateException("app.jwt.secret must be at least 32 characters long");
	        }
	        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
	        this.expirationSeconds = Duration.ofMinutes(expirationMinutes).toSeconds();
	    }

	    public String generateToken(AuthenticatedUser user) {
	        Instant now = Instant.now();
	        return Jwts.builder()
	                .subject(user.getUsername())
	                .claim("uid", user.getId())
	                .claim("role", user.getRole().name())
	                .issuedAt(Date.from(now))
	                .expiration(Date.from(now.plusSeconds(expirationSeconds)))
	                .signWith(signingKey)
	                .compact();
	    }

	    /** Verifies signature and expiry. Throws JwtException if the token is invalid. */
	    public Claims parse(String token) throws JwtException {
	        return Jwts.parser()
	                .verifyWith(signingKey)
	                .build()
	                .parseSignedClaims(token)
	                .getPayload();
	    }

	    public long getExpirationSeconds() {
	        return expirationSeconds;
	    }
}
