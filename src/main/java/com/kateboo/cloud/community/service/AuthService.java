package com.kateboo.cloud.community.service;

import com.kateboo.cloud.community.dto.request.LoginRequest;
import com.kateboo.cloud.community.dto.request.RefreshTokenRequest;
import com.kateboo.cloud.community.dto.request.SignupRequest;
import com.kateboo.cloud.community.dto.response.AuthResponse;
import com.kateboo.cloud.community.entity.RefreshToken;
import com.kateboo.cloud.community.entity.User;
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

    // ✅ 탈퇴 후 복구 가능 기간 (7일)
    private static final int DEACTIVATION_GRACE_PERIOD_DAYS = 7;

    /**
     * 회원가입
     */
    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다");
        }

        if (userRepository.existsByNickname(request.getNickname())) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다");
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .profileImageUrl(request.getProfileImageUrl())
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);

        return generateAuthResponse(savedUser, false);  // 회원가입은 복구 아님
    }

    /**
     * 로그인 (계정 복구 로직 포함)
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("로그인 시도: email={}", request.getEmail());

        // ✅ 1단계: 사용자 조회 (활성/비활성 모두)
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.error("로그인 실패: 이메일을 찾을 수 없음 - {}", request.getEmail());
                    return new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다");
                });

        // ✅ 2단계: 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.error("로그인 실패: 비밀번호 불일치 - email={}", request.getEmail());
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다");
        }

        // ✅ 3단계: 탈퇴 계정 복구 처리
        boolean accountRestored = false;  // 복구 여부 플래그

        if (!user.getIsActive()) {
            LocalDateTime deactivatedAt = user.getDeactivatedAt();

            if (deactivatedAt == null) {
                log.error("비활성 계정이지만 deactivated_at이 null: userId={}", user.getUserId());
                throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다");
            }

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime recoveryDeadline = deactivatedAt.plusDays(DEACTIVATION_GRACE_PERIOD_DAYS);

            // 7일 이내인지 확인
            if (now.isAfter(recoveryDeadline)) {
                log.error("로그인 실패: 복구 기간 만료 - userId={}, deactivatedAt={}",
                        user.getUserId(), deactivatedAt);
                throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다");
            }

            // ✅ 계정 복구
            log.info("계정 복구 시작: userId={}, email={}, 탈퇴일={}",
                    user.getUserId(), user.getEmail(), deactivatedAt);

            user.setIsActive(true);
            user.setDeactivatedAt(null);
            userRepository.save(user);

            accountRestored = true;  // ✅ 복구 플래그 설정

            log.info("✅ 계정 복구 완료: userId={}, email={}", user.getUserId(), user.getEmail());
        }

        log.info("로그인 성공: userId={}, email={}, accountRestored={}",
                user.getUserId(), user.getEmail(), accountRestored);

        return generateAuthResponse(user, accountRestored);  // ✅ 복구 여부 전달
    }

    /**
     * AccessToken 갱신
     */
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository
                .findByToken(request.getRefreshToken())
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 RefreshToken입니다"));

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new IllegalArgumentException("만료된 RefreshToken입니다");
        }

        if (refreshToken.getRevokedAt() != null) {
            throw new IllegalArgumentException("취소된 RefreshToken입니다");
        }

        User user = refreshToken.getUser();

        refreshTokenRepository.delete(refreshToken);
        return generateAuthResponse(user, false);  // 리프레시는 복구 아님
    }

    /**
     * 로그아웃
     */
    @Transactional
    public void logout(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenRepository
                .findByToken(refreshTokenValue)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 RefreshToken입니다"));

        refreshToken.setRevokedAt(LocalDateTime.now());

        log.info("로그아웃: userId={}", refreshToken.getUser().getUserId());
    }

    /**
     * ✅ AuthResponse 생성 (AccessToken + RefreshToken + 복구 여부)
     */
    private AuthResponse generateAuthResponse(User user, boolean accountRestored) {
        // AccessToken 생성
        String accessToken = jwtTokenProvider.generateToken(
                user.getUserId(),
                user.getEmail()
        );

        // RefreshToken 생성
        String refreshTokenValue = UUID.randomUUID().toString();

        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenValue)
                .user(user)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000))
                .build();

        refreshTokenRepository.save(refreshToken);

        log.info("토큰 발급: userId={}, accountRestored={}", user.getUserId(), accountRestored);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpirationInSeconds())
                .userId(user.getUserId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .accountRestored(accountRestored)  // ✅ 복구 여부 포함
                .build();
    }

}