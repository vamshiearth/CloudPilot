package com.cloudpilot.backend.auth;

import com.cloudpilot.backend.users.User;
import com.cloudpilot.backend.tenants.Tenant;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    private final String secret;
    private final long expiration;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expiration) {

        this.secret = secret;
        this.expiration = expiration;
    }

        public String generateToken(
            User user,
            Tenant tenant) {

        Date now = new Date();
        Date expirationDate =
                new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("tenantId", tenant.getId())
                .issuedAt(now)
                .expiration(expirationDate)
                .signWith(getSigningKey())
                .compact();
    }

    public Long extractTenantId(String token) {

        Object value =
                extractAllClaims(token)
                        .get("tenantId");

        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException(
                    "JWT does not contain a valid tenantId"
            );
        }

        return number.longValue();
    }

    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, User user) {

        String email = extractEmail(token);

        return email.equalsIgnoreCase(user.getEmail())
                && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractAllClaims(token)
                .getExpiration()
                .before(new Date());
    }

    private Claims extractAllClaims(String token) {

        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {

        byte[] keyBytes =
                Decoders.BASE64.decode(secret);

        return Keys.hmacShaKeyFor(keyBytes);
    }
}