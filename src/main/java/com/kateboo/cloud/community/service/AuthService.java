package com.kateboo.cloud.community.service;

import com.kateboo.cloud.community.dto.request.LoginRequest;
import com.kateboo.cloud.community.dto.request.SignupRequest;
import com.kateboo.cloud.community.entity.User;
import com.kateboo.cloud.community.exception.BadRequestException;
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
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private static final int DEACTIVATION_GRACE_PERIOD_DAYS = 7;

    @Transactional
    public UUID signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("이미 사용 중인 이메일입니다");
        }
        if (userRepository.existsByNickname(request.getNickname())) {
            throw new BadRequestException("이미 사용 중인 닉네임입니다");
        }
        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .profileImageUrl(request.getProfileImageUrl())
                .isActive(true)
                .build();
        User savedUser = userRepository.save(user);
        return savedUser.getUserId();
    }

    @Transactional
    public UUID login(LoginRequest request) {
        log.info("로그인 시도: email={}", request.getEmail());
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.error("로그인 실패: 이메일을 찾을 수 없음 - {}", request.getEmail());
                    return new BadRequestException("이메일 또는 비밀번호가 올바르지 않습니다");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.error("로그인 실패: 비밀번호 불일치 - email={}", request.getEmail());
            throw new BadRequestException("이메일 또는 비밀번호가 올바르지 않습니다");
        }
        if (!user.getIsActive()) {
            LocalDateTime deactivatedAt = user.getDeactivatedAt();
            if (deactivatedAt == null) {
                log.error("비활성 계정이지만 deactivated_at이 null: userId={}", user.getUserId());
                throw new BadRequestException("이메일 또는 비밀번호가 올바르지 않습니다");
            }
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime recoveryDeadline = deactivatedAt.plusDays(DEACTIVATION_GRACE_PERIOD_DAYS);
            if (now.isAfter(recoveryDeadline)) {
                log.error("로그인 실패: 복구 기간 만료 - userId={}, deactivatedAt={}", user.getUserId(), deactivatedAt);
                throw new BadRequestException("이메일 또는 비밀번호가 올바르지 않습니다");
            }
            log.info("계정 복구 시작: userId={}, email={}, 탈퇴일={}", user.getUserId(), user.getEmail(), deactivatedAt);
            user.setIsActive(true);
            user.setDeactivatedAt(null);
            userRepository.save(user);
            log.info("계정 복구 완료: userId={}, email={}", user.getUserId(), user.getEmail());
        }
        log.info("로그인 성공: userId={}, email={}", user.getUserId(), user.getEmail());
        return user.getUserId(); // 세션 저장용 반환
    }
}
