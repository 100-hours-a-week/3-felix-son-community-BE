package com.kateboo.cloud.community.controller;

import com.kateboo.cloud.community.dto.request.PasswordChangeRequest;
import com.kateboo.cloud.community.dto.request.ProfileUpdateRequest;
import com.kateboo.cloud.community.dto.response.UserResponse;
import com.kateboo.cloud.community.security.CurrentUser;
import com.kateboo.cloud.community.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 내 정보 조회
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyInfo(@CurrentUser UUID userId) {
        UserResponse response = userService.getMyInfo(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 내 프로필 수정 (닉네임, 프로필 이미지)
     */
    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateMyProfile(
            @CurrentUser UUID userId,
            @Valid @RequestBody ProfileUpdateRequest request) {
        UserResponse response = userService.updateMyProfile(userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 비밀번호 변경
     */
    @PatchMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            @CurrentUser UUID userId,
            @Valid @RequestBody PasswordChangeRequest request) {
        userService.changePassword(userId, request);
        return ResponseEntity.ok().build();
    }

    /**
     * 회원 탈퇴 - 1단계: 소프트 삭제
     * PATCH /api/users/me/deactivate
     * is_active를 false로 변경 (복구 가능)
     */
    @PatchMapping("/me/deactivate")
    public ResponseEntity<Void> softDeleteAccount(@CurrentUser UUID userId) {
        userService.softDeleteAccount(userId);
        return ResponseEntity.noContent().build();
    }


    /**
     * 계정 복구
     * PATCH /api/users/me/restore
     * is_active를 true로 변경
     */
    @PatchMapping("/me/restore")
    public ResponseEntity<UserResponse> restoreAccount(@CurrentUser UUID userId) {
        UserResponse response = userService.restoreAccount(userId);
        return ResponseEntity.ok(response);
    }
}