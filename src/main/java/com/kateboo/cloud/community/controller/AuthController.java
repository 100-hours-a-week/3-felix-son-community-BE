package com.kateboo.cloud.community.controller;

import com.kateboo.cloud.community.dto.request.LoginRequest;
import com.kateboo.cloud.community.dto.request.SignupRequest;
import com.kateboo.cloud.community.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 회원가입
     */
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@Valid @RequestBody SignupRequest request) {
        UUID userId = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body("회원가입 성공. userId: " + userId);
    }

    /**
     * 로그인 - 세션에 사용자 정보 저장
     */
    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest){
        UUID userId = authService.login(request); // UUID 반환

        HttpSession session = httpRequest.getSession(true);
        session.setAttribute("userId", userId); // 세션에 userId 저장

        return ResponseEntity.ok("로그인 성공");
    }

    /**
     * 로그아웃 - 세션 무효화 처리
     */
    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request){
        HttpSession session = request.getSession(false);
        if(session != null){
            session.invalidate();
        }
        return ResponseEntity.ok("로그아웃 성공");
    }
}
