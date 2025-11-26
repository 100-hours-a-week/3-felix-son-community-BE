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

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyInfo(@CurrentUser UUID userId) {
        UserResponse response = userService.getMyInfo(userId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateMyProfile(
            @CurrentUser UUID userId,
            @Valid @RequestBody ProfileUpdateRequest request) {
        UserResponse response = userService.updateMyProfile(userId, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            @CurrentUser UUID userId,
            @Valid @RequestBody PasswordChangeRequest request) {
        userService.changePassword(userId, request);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/me/deactivate")
    public ResponseEntity<Void> softDeleteAccount(@CurrentUser UUID userId) {
        userService.softDeleteAccount(userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me/restore")
    public ResponseEntity<UserResponse> restoreAccount(@CurrentUser UUID userId) {
        UserResponse response = userService.restoreAccount(userId);
        return ResponseEntity.ok(response);
    }
}