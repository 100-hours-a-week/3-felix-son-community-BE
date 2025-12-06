package com.kateboo.cloud.community.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        log.debug("JWT 필터 체크: {}", path);

        // 인증이 필요 없는 경로
        return path.startsWith("/api/auth/") ||
                path.startsWith("/uploads/") ||
                path.startsWith("/terms/") ||
                path.startsWith("/css/") ||
                path.startsWith("/js/") ||
                path.startsWith("/images/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = extractToken(request);

            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                UUID userId = jwtTokenProvider.getUserIdFromToken(jwt);

                // ✅ Request Attribute에 userId 저장
                request.setAttribute("userId", userId);
                log.debug("인증 성공: userId={}", userId);
            }

            // ⭐ 정상 처리 - 다음 필터로 진행
            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            // ⭐ 토큰 만료
            log.error("JWT 토큰 만료: {}", e.getMessage());
            sendUnauthorizedError(response, "토큰이 만료되었습니다.");
            return;

        } catch (JwtException e) {
            // ⭐ JWT 검증 실패
            log.error("JWT 검증 실패: {}", e.getMessage());
            sendUnauthorizedError(response, "유효하지 않은 토큰입니다.");
            return;

        } catch (Exception e) {
            // ⭐ 기타 예외
            log.error("JWT 인증 처리 중 오류: {}", e.getMessage(), e);
            sendUnauthorizedError(response, "인증 처리 중 오류가 발생했습니다.");
            return;
        }
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private void sendUnauthorizedError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");


        response.setHeader("Access-Control-Allow-Origin", "http://localhost:3000");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type");

        String errorJson = String.format(
                "{\"status\":401,\"message\":\"%s\",\"timestamp\":\"%s\"}",
                message,
                LocalDateTime.now()
        );

        response.getWriter().write(errorJson);
    }
}