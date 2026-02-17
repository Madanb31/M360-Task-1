package com.madan.M360_Task_1.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    // Create the signing key from secret string
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    // GENERATE token
    public String generateToken(String username, String role) {
        return Jwts.builder()
                .subject(username)                              // who is this token for
                .claim("role", role)                            // store role inside token
                .issuedAt(new Date())                           // when was it created
                .expiration(new Date(System.currentTimeMillis() + expiration))  // when does it expire
                .signWith(getSigningKey())                      // sign with secret key
                .compact();                                     // build the token string
    }

    // EXTRACT username from token
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    // EXTRACT role from token
    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    // VALIDATE token
    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);  // if this doesn't throw, token is valid
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Helper: extract all data from token
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())    // verify signature
                .build()
                .parseSignedClaims(token)       // parse the token
                .getPayload();                  // get the data
    }
}
