package com.kateboo.cloud.community.service;

import com.kateboo.cloud.community.dto.request.PasswordChangeRequest;
import com.kateboo.cloud.community.dto.request.ProfileUpdateRequest;
import com.kateboo.cloud.community.dto.response.UserResponse;
import com.kateboo.cloud.community.entity.User;
import com.kateboo.cloud.community.repository.RefreshTokenRepository;
import com.kateboo.cloud.community.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * 내 정보 조회
     */
    public UserResponse getMyInfo(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        return UserResponse.from(user);
    }

    /**
     * 내 프로필 수정 (닉네임, 프로필 이미지)
     */
    @Transactional
    public UserResponse updateMyProfile(UUID userId, ProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        // 닉네임 변경
        if (request.getNickname() != null && !request.getNickname().isBlank()) {
            // 현재 닉네임과 다른 경우에만 중복 체크
            if (!user.getNickname().equals(request.getNickname())) {
                if (userRepository.existsByNickname(request.getNickname())) {
                    throw new IllegalArgumentException("이미 사용 중인 닉네임입니다");
                }
                user.setNickname(request.getNickname());
                log.info("닉네임 변경 완료 - userId: {}, 새 닉네임: {}", userId, request.getNickname());
            }
        }

        // 프로필 이미지 변경
        if (request.getProfileImageUrl() != null && !request.getProfileImageUrl().isBlank()) {
            user.setProfileImageUrl(request.getProfileImageUrl());
            log.info("프로필 이미지 변경 완료 - userId: {}", userId);
        }

        return UserResponse.from(user);
    }

    /**
     * 비밀번호 변경
     */
    @Transactional
    public void changePassword(UUID userId, PasswordChangeRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("새 비밀번호와 확인 비밀번호가 일치하지 않습니다.");
        }

        // 새 비밀번호로 변경
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));

        log.info("비밀번호 변경 완료 - userId: {}", userId);
    }

    /**
     * 회원 탈퇴 - 1단계: 소프트 삭제 (PATCH)
     * is_active를 false로 변경하고, 모든 RefreshToken 삭제
     */
    @Transactional
    public void softDeleteAccount(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        // 이미 비활성화된 계정인지 확인
        if (!user.getIsActive()) {
            throw new IllegalStateException("이미 탈퇴 처리된 계정입니다. 완전 삭제를 원하시면 DELETE 요청을 보내주세요.");
        }

        // 소프트 삭제: is_active를 false로
        user.setIsActive(false);

        // 모든 RefreshToken 삭제 (로그아웃 처리)
        refreshTokenRepository.deleteByUser_UserId(userId);

        log.warn("회원 탈퇴 처리 (소프트 삭제) - userId: {}, email: {}", userId, user.getEmail());
        log.info("계정 복구는 30일 이내 가능합니다.");
    }

    /**
     * 회원 탈퇴 - 2단계: 하드 삭제 (DELETE)
     * is_active가 false인 경우에만 DB에서 완전 삭제
     */
    @Transactional
    public void hardDeleteAccount(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        // is_active가 true면 먼저 소프트 삭제 요청해야 함
        if (user.getIsActive()) {
            throw new IllegalStateException("먼저 회원 탈퇴(PATCH)를 진행해주세요. 바로 삭제는 불가능합니다.");
        }

        // 하드 삭제: DB에서 완전 삭제
        // cascade나 ON DELETE 설정에 따라 연관 데이터도 처리됨
        userRepository.delete(user);

        log.error("회원 완전 삭제 (하드 삭제) - userId: {}, email: {}", userId, user.getEmail());
        log.warn("이 작업은 되돌릴 수 없습니다!");
    }

    /**
     * 계정 복구
     * is_active를 false에서 true로 변경
     */
    @Transactional
    public UserResponse restoreAccount(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        // 이미 활성화된 계정인지 확인
        if (user.getIsActive()) {
            throw new IllegalStateException("이미 활성화된 계정입니다.");
        }

        // 계정 복구
        user.setIsActive(true);

        log.info("계정 복구 완료 - userId: {}, email: {}", userId, user.getEmail());

        return UserResponse.from(user);
    }

    /**
     * 기존 메서드 (deprecated) - 하위 호환성을 위해 유지
     */
    @Deprecated
    @Transactional
    public void deactivateAccount(UUID userId) {
        softDeleteAccount(userId);
    }
}