package com.surendra.userservice.service;

import org.springframework.stereotype.Service;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.Jwts;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import io.jsonwebtoken.Claims;
@Service
public class JwtService {
    @Value("${jwt.secret}")
    private String secretKey;
    @Value("${jwt.expiration}")
    private long jwtExpiration;
    private SecretKey getSigningKey() {

        return Keys.hmacShaKeyFor(
                secretKey.getBytes(StandardCharsets.UTF_8)
        );
    }
    private Claims extractAllClaims(String token) {

        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    public String generateToken(String email) {

        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey())
                .compact();
    }
    public String extractEmail(String token) {

        return extractAllClaims(token)
                .getSubject();
    }
    public Date extractExpiration(String token) {

        return extractAllClaims(token)
                .getExpiration();
    }
    private boolean isTokenExpired(String token) {

        return extractExpiration(token)
                .before(new Date());
    }

    public boolean validateToken(String token, String email) {

        String extractedEmail = extractEmail(token);

        return extractedEmail.equals(email)
                && !isTokenExpired(token);
    }
}

