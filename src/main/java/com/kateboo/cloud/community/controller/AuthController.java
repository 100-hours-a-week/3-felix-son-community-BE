package com.kateboo.cloud.community.controller;

import com.kateboo.cloud.community.dto.request.LoginRequest;
import com.kateboo.cloud.community.dto.request.SignupRequest;
import com.kateboo.cloud.community.dto.response.AuthResponse;
import com.kateboo.cloud.community.dto.response.TokenResponse;
import com.kateboo.cloud.community.exception.UnauthorizedException;
import com.kateboo.cloud.community.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request, HttpServletResponse response) {
        AuthResponse authResponse = authService.signup(request);

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", authResponse.getRefreshToken())
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(7 * 24 * 60 *60)
                .sameSite("Lax")
                .build();

        response.addHeader("Set-Cookie", refreshCookie.toString());

        return ResponseEntity.status(HttpStatus.CREATED).body(authResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody @Valid LoginRequest request,
            HttpServletResponse response) {

        log.info("로그인 요청: email={}", request.getEmail());

        AuthResponse loginResponse = authService.login(request);

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", loginResponse.getRefreshToken())
                .httpOnly(true)              // JavaScript 접근 불가
                .secure(false)               // 개발: false, 프로덕션: true (HTTPS)
                .path("/")                   // 모든 경로에서 전송
                .maxAge(7 * 24 * 60 * 60)    // 7일
                .sameSite("Lax")          // CSRF 방어
                .build();

        response.addHeader("Set-Cookie", refreshCookie.toString());

        loginResponse.setRefreshToken(null);

        log.info("로그인 성공: userId={}", loginResponse.getUserId());

        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response) {

        log.info("토큰 갱신 요청");

        if (refreshToken == null || refreshToken.isEmpty()) {
            log.warn("Refresh Token이 없습니다");
            throw new UnauthorizedException("Refresh token not found");
        }

        AuthResponse authResponse = authService.refresh(refreshToken);

        ResponseCookie newRefreshCookie = ResponseCookie.from("refreshToken", authResponse.getRefreshToken())
                .httpOnly(true)
                .secure(false)  // 개발: false, 프로덕션: true
                .path("/")
                .maxAge(7 * 24 * 60 * 60)
                .sameSite("Strict")
                .build();

        response.addHeader("Set-Cookie", newRefreshCookie.toString());

        TokenResponse tokenResponse = new TokenResponse(authResponse.getAccessToken());

        log.info("토큰 갱신 성공");

        return ResponseEntity.ok(tokenResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response) {

        log.info("로그아웃 요청");

        if (refreshToken != null && !refreshToken.isEmpty()) {
            authService.logout(refreshToken);
        }

        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();

        response.addHeader("Set-Cookie", deleteCookie.toString());

        log.info("로그아웃 완료");

        return ResponseEntity.ok().build();
    }
}