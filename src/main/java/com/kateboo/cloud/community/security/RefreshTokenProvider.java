package com.kateboo.cloud.community.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
@Slf4j
public class RefreshTokenProvider {

    private final SecretKey secretKey;
    private final long refreshTokenExpiration;

    public RefreshTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.refresh-token.expiration}") long refreshTokenExpiration) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    /**
     * Refresh Token 생성
     */
    public String generateRefreshToken(UUID userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpiration);

        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    /**
     * Refresh Token에서 사용자 ID 추출
     */
    public UUID getUserIdFromRefreshToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return UUID.fromString(claims.getSubject());
    }

    /**
     * Refresh Token 유효성 검증
     */
    public boolean validateRefreshToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            log.error("Refresh Token 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Refresh Token 만료 시간 반환 (초 단위)
     */
    public long getRefreshTokenExpirationInSeconds() {
        return refreshTokenExpiration / 1000;
    }
}