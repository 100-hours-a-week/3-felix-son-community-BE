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
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;

    // ✅ 탈퇴 후 복구 가능 기간 (7일)
    private static final int DEACTIVATION_GRACE_PERIOD_DAYS = 7;

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
     * ✅ 회원 탈퇴 - 소프트 삭제
     * is_active를 false로 변경하고, deactivatedAt 기록
     * 7일 이내 재로그인 시 자동 복구 가능
     */
    @Transactional
    public void softDeleteAccount(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        // 이미 비활성화된 계정인지 확인
        if (!user.getIsActive()) {
            throw new IllegalStateException("이미 탈퇴 처리된 계정입니다.");
        }

        // 소프트 삭제: is_active를 false로, 탈퇴 시점 기록
        user.setIsActive(false);
        user.setDeactivatedAt(LocalDateTime.now());  // ✅ 탈퇴 시점 기록

        // 모든 RefreshToken 삭제 (로그아웃 처리)
        refreshTokenRepository.deleteByUser_UserId(userId);

        log.warn("회원 탈퇴 처리 (소프트 삭제) - userId: {}, email: {}", userId, user.getEmail());
        log.info("계정 복구는 {}일 이내 재로그인 시 가능합니다. {}일 후 자동으로 영구 삭제됩니다.",
                DEACTIVATION_GRACE_PERIOD_DAYS, DEACTIVATION_GRACE_PERIOD_DAYS);
    }

    /**
     * ✅ 로그인 시 자동 복구 체크
     * - 7일 이내: 자동 복구
     * - 7일 지남: 예외 발생 (로그인 불가)
     *
     * @param email 로그인 이메일
     * @return User 엔티티 (활성 또는 복구된 계정)
     * @throws IllegalArgumentException 계정이 없거나 7일이 지난 경우
     */
    @Transactional
    public User checkAndRestoreIfNeeded(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 활성 계정이면 그대로 반환
        if (user.getIsActive()) {
            return user;
        }

        // 비활성 계정인 경우
        if (user.getDeactivatedAt() != null) {
            LocalDateTime expiryDate = user.getDeactivatedAt()
                    .plusDays(DEACTIVATION_GRACE_PERIOD_DAYS);

            // 7일 이내면 자동 복구
            if (LocalDateTime.now().isBefore(expiryDate)) {
                user.setIsActive(true);
                user.setDeactivatedAt(null);  // 복구 시 탈퇴 시점 초기화
                userRepository.save(user);

                log.info("계정 자동 복구 완료 - userId: {}, email: {}", user.getUserId(), user.getEmail());
                return user;
            } else {
                // 7일 지났으면 로그인 불가
                log.warn("복구 기간 만료 - userId: {}, email: {}, deactivatedAt: {}",
                        user.getUserId(), user.getEmail(), user.getDeactivatedAt());
                throw new IllegalArgumentException("탈퇴 후 " + DEACTIVATION_GRACE_PERIOD_DAYS + "일이 지나 계정이 영구 삭제 예정입니다. 로그인할 수 없습니다.");
            }
        }

        // deactivatedAt이 null인 비활성 계정 (예외 상황)
        log.error("비정상 상태 - 비활성 계정이지만 탈퇴 시점이 기록되지 않음: userId={}", user.getUserId());
        throw new IllegalStateException("비활성 계정입니다. 관리자에게 문의하세요.");
    }

    /**
     * ✅ 계정 수동 복구 (선택사항)
     * 로그인 시 자동 복구되므로 별도 API가 필요 없을 수 있음
     * 7일 이내만 복구 가능
     */
    @Transactional
    public UserResponse restoreAccount(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        // 이미 활성화된 계정인지 확인
        if (user.getIsActive()) {
            throw new IllegalStateException("이미 활성화된 계정입니다.");
        }

        // 복구 가능 기간 체크
        if (user.getDeactivatedAt() != null) {
            LocalDateTime expiryDate = user.getDeactivatedAt()
                    .plusDays(DEACTIVATION_GRACE_PERIOD_DAYS);

            if (LocalDateTime.now().isAfter(expiryDate)) {
                log.warn("복구 기간 만료 - userId: {}, deactivatedAt: {}", userId, user.getDeactivatedAt());
                throw new IllegalStateException("복구 기간(" + DEACTIVATION_GRACE_PERIOD_DAYS + "일)이 지났습니다.");
            }
        }

        // 계정 복구
        user.setIsActive(true);
        user.setDeactivatedAt(null);  // 탈퇴 시점 초기화

        log.info("계정 수동 복구 완료 - userId: {}, email: {}", userId, user.getEmail());

        return UserResponse.from(user);
    }

    /**
     * ✅ 7일 지난 비활성 계정 영구 삭제 (스케줄러용)
     * 매일 자동 실행되어 만료된 계정을 DB에서 완전히 삭제
     */
    @Transactional
    public void deleteExpiredAccounts() {
        LocalDateTime cutoffDate = LocalDateTime.now()
                .minusDays(DEACTIVATION_GRACE_PERIOD_DAYS);

        // 7일 전에 비활성화된 계정 조회
        List<User> expiredUsers = userRepository
                .findByIsActiveFalseAndDeactivatedAtBefore(cutoffDate);

        if (expiredUsers.isEmpty()) {
            log.info("영구 삭제 대상 계정 없음");
            return;
        }

        log.warn("영구 삭제 대상 계정: {} 개", expiredUsers.size());

        for (User user : expiredUsers) {
            try {
                // RefreshToken 먼저 삭제
                refreshTokenRepository.deleteByUser_UserId(user.getUserId());

                // 사용자 영구 삭제
                userRepository.delete(user);

                log.error("계정 영구 삭제 완료 - userId: {}, email: {}, deactivatedAt: {}",
                        user.getUserId(), user.getEmail(), user.getDeactivatedAt());
            } catch (Exception e) {
                log.error("계정 삭제 중 오류 발생 - userId: {}, error: {}",
                        user.getUserId(), e.getMessage(), e);
            }
        }

        log.info("영구 삭제 완료 - 총 {} 개 계정 삭제됨", expiredUsers.size());
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