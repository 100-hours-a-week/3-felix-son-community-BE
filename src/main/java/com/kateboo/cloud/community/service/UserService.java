package com.kateboo.cloud.community.service;

import com.kateboo.cloud.community.dto.request.PasswordChangeRequest;
import com.kateboo.cloud.community.dto.request.ProfileUpdateRequest;
import com.kateboo.cloud.community.dto.response.UserResponse;
import com.kateboo.cloud.community.entity.User;
import com.kateboo.cloud.community.exception.BadRequestException;
import com.kateboo.cloud.community.exception.NotFoundException;
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

    private static final int DEACTIVATION_GRACE_PERIOD_DAYS = 7;

    public UserResponse getMyInfo(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다"));

        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateMyProfile(UUID userId, ProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다"));

        if (request.getNickname() != null && !request.getNickname().isBlank()) {
            if (!user.getNickname().equals(request.getNickname())) {
                if (userRepository.existsByNickname(request.getNickname())) {
                    throw new BadRequestException("이미 사용 중인 닉네임입니다");
                }
                user.setNickname(request.getNickname());
                log.info("닉네임 변경 완료 - userId: {}, 새 닉네임: {}", userId, request.getNickname());
            }
        }

        if (request.getProfileImageUrl() != null && !request.getProfileImageUrl().isBlank()) {
            user.setProfileImageUrl(request.getProfileImageUrl());
            log.info("프로필 이미지 변경 완료 - userId: {}", userId);
        }

        return UserResponse.from(user);
    }

    @Transactional
    public void changePassword(UUID userId, PasswordChangeRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다"));

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("새 비밀번호와 확인 비밀번호가 일치하지 않습니다.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));

        log.info("비밀번호 변경 완료 - userId: {}", userId);
    }

    @Transactional
    public void softDeleteAccount(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다"));

        if (!user.getIsActive()) {
            throw new BadRequestException("이미 탈퇴 처리된 계정입니다.");
        }

        user.setIsActive(false);
        user.setDeactivatedAt(LocalDateTime.now());

        refreshTokenRepository.deleteByUser_UserId(userId);

        log.warn("회원 탈퇴 처리 (소프트 삭제) - userId: {}, email: {}", userId, user.getEmail());
        log.info("계정 복구는 {}일 이내 재로그인 시 가능합니다. {}일 후 자동으로 영구 삭제됩니다.",
                DEACTIVATION_GRACE_PERIOD_DAYS, DEACTIVATION_GRACE_PERIOD_DAYS);
    }

    @Transactional
    public User checkAndRestoreIfNeeded(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        if (user.getIsActive()) {
            return user;
        }

        if (user.getDeactivatedAt() != null) {
            LocalDateTime expiryDate = user.getDeactivatedAt()
                    .plusDays(DEACTIVATION_GRACE_PERIOD_DAYS);

            if (LocalDateTime.now().isBefore(expiryDate)) {
                user.setIsActive(true);
                user.setDeactivatedAt(null);
                userRepository.save(user);

                log.info("계정 자동 복구 완료 - userId: {}, email: {}", user.getUserId(), user.getEmail());
                return user;
            } else {
                log.warn("복구 기간 만료 - userId: {}, email: {}, deactivatedAt: {}",
                        user.getUserId(), user.getEmail(), user.getDeactivatedAt());
                throw new BadRequestException("탈퇴 후 " + DEACTIVATION_GRACE_PERIOD_DAYS + "일이 지나 계정이 영구 삭제 예정입니다. 로그인할 수 없습니다.");
            }
        }

        log.error("비정상 상태 - 비활성 계정이지만 탈퇴 시점이 기록되지 않음: userId={}", user.getUserId());
        throw new BadRequestException("비활성 계정입니다. 관리자에게 문의하세요.");
    }

    @Transactional
    public UserResponse restoreAccount(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다"));

        if (user.getIsActive()) {
            throw new BadRequestException("이미 활성화된 계정입니다.");
        }

        if (user.getDeactivatedAt() != null) {
            LocalDateTime expiryDate = user.getDeactivatedAt()
                    .plusDays(DEACTIVATION_GRACE_PERIOD_DAYS);

            if (LocalDateTime.now().isAfter(expiryDate)) {
                log.warn("복구 기간 만료 - userId: {}, deactivatedAt: {}", userId, user.getDeactivatedAt());
                throw new BadRequestException("복구 기간(" + DEACTIVATION_GRACE_PERIOD_DAYS + "일)이 지났습니다.");
            }
        }

        user.setIsActive(true);
        user.setDeactivatedAt(null);

        log.info("계정 수동 복구 완료 - userId: {}, email: {}", userId, user.getEmail());

        return UserResponse.from(user);
    }

    @Transactional
    public void deleteExpiredAccounts() {
        LocalDateTime cutoffDate = LocalDateTime.now()
                .minusDays(DEACTIVATION_GRACE_PERIOD_DAYS);

        List<User> expiredUsers = userRepository
                .findByIsActiveFalseAndDeactivatedAtBefore(cutoffDate);

        if (expiredUsers.isEmpty()) {
            log.info("영구 삭제 대상 계정 없음");
            return;
        }

        log.warn("영구 삭제 대상 계정: {} 개", expiredUsers.size());

        for (User user : expiredUsers) {
            try {
                refreshTokenRepository.deleteByUser_UserId(user.getUserId());

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

}