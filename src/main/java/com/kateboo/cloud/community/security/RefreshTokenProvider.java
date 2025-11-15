package com.kateboo.cloud.community.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Component
@Slf4j
public class RefreshTokenProvider {

    private final byte[] secretKeyBytes;
    private final long refreshTokenExpiration;

    public RefreshTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.refresh-token.expiration}") long refreshTokenExpiration) {

        this.secretKeyBytes = Base64.getDecoder().decode(secret);
        this.refreshTokenExpiration = refreshTokenExpiration;

        log.info("Refresh TokenProvider 초기화 완료 (JJWT 0.11.2 + Base64)");
    }

    public String generateRefreshToken(UUID userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpiration);

        return Jwts.builder()
                .setSubject(userId.toString())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS256, secretKeyBytes)
                .compact();
    }

    public UUID getUserIdFromRefreshToken(String token) {
        Claims claims = parseClaims(token);
        return UUID.fromString(claims.getSubject());
    }

    public boolean validateRefreshToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            log.error("Refresh Token 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .setSigningKey(secretKeyBytes)
                .parseClaimsJws(token)
                .getBody();
    }

    public long getRefreshTokenExpirationInSeconds() {
        return refreshTokenExpiration / 1000;
    }
}