package com.kateboo.cloud.community.service;

import com.kateboo.cloud.community.dto.request.LoginRequest;
import com.kateboo.cloud.community.dto.request.SignupRequest;
import com.kateboo.cloud.community.dto.response.AuthResponse;
import com.kateboo.cloud.community.entity.RefreshToken;
import com.kateboo.cloud.community.entity.User;
import com.kateboo.cloud.community.exception.BadRequestException;
import com.kateboo.cloud.community.exception.NotFoundException;
import com.kateboo.cloud.community.repository.RefreshTokenRepository;
import com.kateboo.cloud.community.repository.UserRepository;
import com.kateboo.cloud.community.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${jwt.refresh-token.expiration}")
    private long refreshTokenExpiration;

    private static final int DEACTIVATION_GRACE_PERIOD_DAYS = 7;

    @Transactional
    public AuthResponse signup(SignupRequest request) {
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

        // 회원가입 시에는 토큰 발급 안 함 (로그인 필요)
        return AuthResponse.builder()
                .userId(savedUser.getUserId())
                .email(savedUser.getEmail())
                .nickname(savedUser.getNickname())
                .profileImageUrl(savedUser.getProfileImageUrl())
                .accountRestored(false)
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
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

        boolean accountRestored = false;

        if (!user.getIsActive()) {
            LocalDateTime deactivatedAt = user.getDeactivatedAt();

            if (deactivatedAt == null) {
                log.error("비활성 계정이지만 deactivated_at이 null: userId={}", user.getUserId());
                throw new BadRequestException("이메일 또는 비밀번호가 올바르지 않습니다");
            }

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime recoveryDeadline = deactivatedAt.plusDays(DEACTIVATION_GRACE_PERIOD_DAYS);

            if (now.isAfter(recoveryDeadline)) {
                log.error("로그인 실패: 복구 기간 만료 - userId={}, deactivatedAt={}",
                        user.getUserId(), deactivatedAt);
                throw new BadRequestException("이메일 또는 비밀번호가 올바르지 않습니다");
            }

            log.info("계정 복구 시작: userId={}, email={}, 탈퇴일={}",
                    user.getUserId(), user.getEmail(), deactivatedAt);

            user.setIsActive(true);
            user.setDeactivatedAt(null);
            userRepository.save(user);

            accountRestored = true;

            log.info("계정 복구 완료: userId={}, email={}", user.getUserId(), user.getEmail());
        }

        log.info("로그인 성공: userId={}, email={}", user.getUserId(), user.getEmail());

        return generateAuthResponse(user, accountRestored);
    }

    @Transactional
    public AuthResponse refresh(String refreshTokenValue) {
        log.info("토큰 갱신 요청");

        RefreshToken refreshToken = refreshTokenRepository
                .findByToken(refreshTokenValue)
                .orElseThrow(() -> {
                    log.warn("유효하지 않은 RefreshToken");
                    return new NotFoundException("유효하지 않은 RefreshToken입니다");
                });

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("만료된 RefreshToken: userId={}", refreshToken.getUser().getUserId());
            refreshTokenRepository.delete(refreshToken);
            throw new BadRequestException("만료된 RefreshToken입니다");
        }

        if (refreshToken.getRevokedAt() != null) {
            log.warn("취소된 RefreshToken: userId={}", refreshToken.getUser().getUserId());
            throw new BadRequestException("취소된 RefreshToken입니다");
        }

        User user = refreshToken.getUser();

        // ✅ Refresh Token Rotation: 기존 토큰 삭제
        refreshTokenRepository.delete(refreshToken);

        log.info("토큰 갱신 성공: userId={}", user.getUserId());

        return generateAuthResponse(user, false);
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isEmpty()) {
            log.warn("로그아웃 시도: Refresh Token 없음");
            return;
        }

        RefreshToken refreshToken = refreshTokenRepository
                .findByToken(refreshTokenValue)
                .orElse(null);

        if (refreshToken != null) {
            refreshToken.setRevokedAt(LocalDateTime.now());
            log.info("로그아웃: userId={}", refreshToken.getUser().getUserId());
        } else {
            log.warn("로그아웃 시도: 유효하지 않은 Refresh Token");
        }
    }

    /**
     * AuthResponse 생성 (Access Token + Refresh Token)
     */
    private AuthResponse generateAuthResponse(User user, boolean accountRestored) {
        // 1. Access Token 생성
        String accessToken = jwtTokenProvider.generateToken(user.getUserId(), user.getEmail());

        // 2. Refresh Token 생성 (UUID)
        String refreshTokenValue = UUID.randomUUID().toString();

        // 3. Refresh Token DB 저장
        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenValue)
                .user(user)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000))
                .build();

        refreshTokenRepository.save(refreshToken);

        log.info("토큰 발급: userId={}, accountRestored={}", user.getUserId(), accountRestored);

        // 4. AuthResponse 반환
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .userId(user.getUserId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .accountRestored(accountRestored)
                .build();
    }

    /**
     * 사용자 조회 (토큰 갱신 시 사용)
     */
    public User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
    }
}