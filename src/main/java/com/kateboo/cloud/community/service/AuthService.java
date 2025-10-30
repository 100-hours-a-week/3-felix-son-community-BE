package com.kateboo.cloud.community.service;

import com.kateboo.cloud.community.dto.request.LoginRequest;
import com.kateboo.cloud.community.dto.request.RefreshTokenRequest;
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

        return generateAuthResponse(savedUser, false);
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

            log.info("계정 복구 완료: userId={}, email={}", user.getUserId(), user.getEmail());
        }

        log.info("로그인 성공: userId={}, email={}", user.getUserId(), user.getEmail());

        return generateAuthResponse(user, false);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository
                .findByToken(request.getRefreshToken())
                .orElseThrow(() -> new NotFoundException("유효하지 않은 RefreshToken입니다"));

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new BadRequestException("만료된 RefreshToken입니다");
        }

        if (refreshToken.getRevokedAt() != null) {
            throw new BadRequestException("취소된 RefreshToken입니다");
        }

        User user = refreshToken.getUser();

        refreshTokenRepository.delete(refreshToken);

        return generateAuthResponse(user, false);
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenRepository
                .findByToken(refreshTokenValue)
                .orElseThrow(() -> new NotFoundException("유효하지 않은 RefreshToken입니다"));

        refreshToken.setRevokedAt(LocalDateTime.now());

        log.info("로그아웃: userId={}", refreshToken.getUser().getUserId());
    }

    private AuthResponse generateAuthResponse(User user, boolean accountRestored) {
        String accessToken = jwtTokenProvider.generateToken(user.getUserId(), user.getEmail());
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
                .accountRestored(accountRestored)
                .build();
    }
}
