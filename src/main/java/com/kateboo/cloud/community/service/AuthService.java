package com.kateboo.cloud.community.service;

import com.kateboo.cloud.community.dto.request.LoginRequest;
import com.kateboo.cloud.community.dto.request.SignupRequest;
import com.kateboo.cloud.community.dto.response.AuthResponse;
import com.kateboo.cloud.community.entity.RefreshToken;
import com.kateboo.cloud.community.entity.User;
import com.kateboo.cloud.community.exception.BadRequestException;
import com.kateboo.cloud.community.exception.ConflictException;
import com.kateboo.cloud.community.exception.NotFoundException;
import com.kateboo.cloud.community.repository.RefreshTokenRepository;
import com.kateboo.cloud.community.repository.UserRepository;
import com.kateboo.cloud.community.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*(),.?\":{}|<>]).{8,20}$"
    );
    private static final Pattern NICKNAME_WHITESPACE_PATTERN = Pattern.compile("\\s");

    @Value("${jwt.refresh-token.expiration}")
    private long refreshTokenExpiration;

    private static final int DEACTIVATION_GRACE_PERIOD_DAYS = 7;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AuthResponse signup(SignupRequest request) {
        log.info("회원가입 시도 - 이메일: {}, 닉네임: {}", request.getEmail(), request.getNickname());

        validateProfileImage(request.getProfileImageUrl());

        validateEmail(request.getEmail());

        validatePassword(request.getPassword());

        validateNickname(request.getNickname());

        User user = User.builder()
                .email(request.getEmail().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname().trim())
                .profileImageUrl(request.getProfileImageUrl().trim())
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("회원가입 성공 - userId: {}, 이메일: {}", savedUser.getUserId(), savedUser.getEmail());

        return generateAuthResponse(savedUser, false);
    }

    private void validateProfileImage(String profileImageUrl) {

        if (profileImageUrl == null || profileImageUrl.trim().isEmpty()) {
            throw new BadRequestException("프로필 사진을 추가해주세요");
        }

        if (profileImageUrl.length() > 500) {
            throw new BadRequestException("프로필 이미지 URL은 500자 이하여야 합니다");
        }
    }

    private void validateEmail(String email) {

        if (email == null || email.trim().isEmpty()) {
            throw new BadRequestException("이메일을 입력해주세요");
        }

        String trimmedEmail = email.trim();

        if (!EMAIL_PATTERN.matcher(trimmedEmail).matches()) {
            throw new BadRequestException("올바른 이메일 주소 형식을 입력해주세요");
        }

        if (trimmedEmail.length() > 254) {
            throw new BadRequestException("이메일은 254자 이하여야 합니다");
        }

        if (userRepository.existsByEmail(trimmedEmail)) {
            log.warn("이메일 중복 감지: {}", trimmedEmail);
            throw new ConflictException("중복된 이메일입니다");
        }
    }

    private void validatePassword(String password) {

        if (password == null || password.isEmpty()) {
            throw new BadRequestException("비밀번호를 입력해주세요");
        }

        if (password.length() < 8 || password.length() > 20) {
            throw new BadRequestException("비밀번호는 8자 이상, 20자 이하이며, 대문자, 소문자, 숫자, 특수문자를 각각 최소 1개 포함해야합니다");
        }

        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new BadRequestException("비밀번호는 8자 이상, 20자 이하이며, 대문자, 소문자, 숫자, 특수문자를 각각 최소 1개 포함해야합니다");
        }
    }

    private void validateNickname(String nickname) {

        if (nickname == null || nickname.trim().isEmpty()) {
            throw new BadRequestException("닉네임을 입력해주세요");
        }

        String trimmedNickname = nickname.trim();

        if (NICKNAME_WHITESPACE_PATTERN.matcher(trimmedNickname).find()) {
            throw new BadRequestException("띄어쓰기를 없애주세요");
        }

        if (trimmedNickname.length() > 10) {
            throw new BadRequestException("닉네임은 최대 10자까지 작성 가능합니다");
        }

        if (userRepository.existsByNickname(trimmedNickname)) {
            log.warn("닉네임 중복 감지: {}", trimmedNickname);
            throw new ConflictException("중복된 닉네임입니다");
        }
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
            Instant deactivatedAt = user.getDeactivatedAt();

            if (deactivatedAt == null) {
                log.error("비활성 계정이지만 deactivated_at이 null: userId={}", user.getUserId());
                throw new BadRequestException("이메일 또는 비밀번호가 올바르지 않습니다");
            }

            Instant now = Instant.now();
            Instant recoveryDeadline = deactivatedAt.plus(DEACTIVATION_GRACE_PERIOD_DAYS, ChronoUnit.DAYS);

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

        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            log.warn("만료된 RefreshToken: userId={}", refreshToken.getUser().getUserId());
            refreshTokenRepository.delete(refreshToken);
            throw new BadRequestException("만료된 RefreshToken입니다");
        }

        if (refreshToken.getRevokedAt() != null) {
            log.warn("취소된 RefreshToken: userId={}", refreshToken.getUser().getUserId());
            throw new BadRequestException("취소된 RefreshToken입니다");
        }

        User user = refreshToken.getUser();

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
            refreshToken.setRevokedAt(Instant.now());
            log.info("로그아웃: userId={}", refreshToken.getUser().getUserId());
        } else {
            log.warn("로그아웃 시도: 유효하지 않은 Refresh Token");
        }
    }

    private AuthResponse generateAuthResponse(User user, boolean accountRestored) {

        String accessToken = jwtTokenProvider.generateToken(user.getUserId(), user.getEmail());

        String refreshTokenValue = UUID.randomUUID().toString();

        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenValue)
                .user(user)
                .expiresAt(Instant.now().plusSeconds(refreshTokenExpiration / 1000))
                .build();

        refreshTokenRepository.save(refreshToken);

        log.info("토큰 발급: userId={}, accountRestored={}", user.getUserId(), accountRestored);

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

    public User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
    }
}