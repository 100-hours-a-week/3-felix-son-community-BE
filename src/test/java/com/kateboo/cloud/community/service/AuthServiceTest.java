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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private User makeDummyUser(UUID userId) {
        return User.builder()
                .userId(userId)
                .email("user@example.com")
                .nickname("nickname")
                .passwordHash("hashed-password")
                .profileImageUrl("http://image.url/avatar.png")
                .isActive(true)
                .build();
    }

    private RefreshToken makeDummyRefreshToken(User user) {
        return RefreshToken.builder()
                .refreshId(1L)
                .token("refresh-token-string")
                .user(user)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(1))
                .revokedAt(null)
                .build();
    }

    @Nested
    @DisplayName("signup()")
    class SignupTest {
        @Test
        void 회원가입_성공() {
            SignupRequest request = new SignupRequest(
                    "email@example.com",
                    "password123",
                    "newNickname",
                    "http://profile.url/image.png"
            );

            when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
            when(userRepository.existsByNickname(request.getNickname())).thenReturn(false);

            User savedUser = makeDummyUser(UUID.randomUUID());
            when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded-password");
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            when(jwtTokenProvider.generateToken(savedUser.getUserId(), savedUser.getEmail()))
                    .thenReturn("access-token");
            when(jwtTokenProvider.getAccessTokenExpirationInSeconds()).thenReturn(3600L);
            when(refreshTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            AuthResponse response = authService.signup(request);

            assertThat(response.getAccessToken()).isEqualTo("access-token");
            assertThat(response.getUserId()).isEqualTo(savedUser.getUserId());
            verify(userRepository).save(any(User.class));
        }

        @Test
        void 이메일_중복예외() {
            SignupRequest request = new SignupRequest(
                    "email@example.com",
                    "password123",
                    "newNickname",
                    "http://profile.url/image.png"
            );

            when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

            assertThatThrownBy(() -> authService.signup(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("이미 사용 중인 이메일입니다");
        }

        @Test
        void 닉네임_중복예외() {
            SignupRequest request = new SignupRequest(
                    "email@example.com",
                    "password123",
                    "newNickname",
                    "http://profile.url/image.png"
            );

            when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
            when(userRepository.existsByNickname(request.getNickname())).thenReturn(true);

            assertThatThrownBy(() -> authService.signup(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("이미 사용 중인 닉네임입니다");
        }
    }

    @Nested
    @DisplayName("login()")
    class LoginTest {
        @Test
        void 정상_로그인() {
            LoginRequest request = new LoginRequest("user@example.com", "password");
            User user = makeDummyUser(UUID.randomUUID());

            when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(request.getPassword(), user.getPasswordHash())).thenReturn(true);
            when(jwtTokenProvider.generateToken(user.getUserId(), user.getEmail())).thenReturn("access-token");
            when(jwtTokenProvider.getAccessTokenExpirationInSeconds()).thenReturn(3600L);
            when(refreshTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            AuthResponse response = authService.login(request);

            assertThat(response.getAccessToken()).isEqualTo("access-token");
            assertThat(response.getUserId()).isEqualTo(user.getUserId());
            assertThat(response.getAccountRestored()).isFalse();
        }

        @Test
        void 비밀번호_불일치_예외() {
            LoginRequest request = new LoginRequest("user@example.com", "wrongpass");
            User user = makeDummyUser(UUID.randomUUID());

            when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(request.getPassword(), user.getPasswordHash())).thenReturn(false);

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("이메일 또는 비밀번호가 올바르지 않습니다");
        }

        @Test
        void 탈퇴_계정_7일이내_복구_후_로그인() {
            LoginRequest request = new LoginRequest("user@example.com", "password");
            User user = makeDummyUser(UUID.randomUUID());
            user.setIsActive(false);
            user.setDeactivatedAt(LocalDateTime.now().minusDays(3));

            when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(request.getPassword(), user.getPasswordHash())).thenReturn(true);
            when(userRepository.save(user)).thenReturn(user);
            when(jwtTokenProvider.generateToken(user.getUserId(), user.getEmail())).thenReturn("access-token");
            when(jwtTokenProvider.getAccessTokenExpirationInSeconds()).thenReturn(3600L);
            when(refreshTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            AuthResponse response = authService.login(request);

            assertThat(response.getAccountRestored()).isTrue();
            assertThat(response.getAccessToken()).isEqualTo("access-token");
            verify(userRepository).save(user);
        }

        @Test
        void 탈퇴_계정_7일_초과_로그인_실패() {
            LoginRequest request = new LoginRequest("user@example.com", "password");
            User user = makeDummyUser(UUID.randomUUID());
            user.setIsActive(false);
            user.setDeactivatedAt(LocalDateTime.now().minusDays(10));

            when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(request.getPassword(), user.getPasswordHash())).thenReturn(true);

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("이메일 또는 비밀번호가 올바르지 않습니다");
        }
    }

    @Nested
    @DisplayName("refresh()")
    class RefreshTest {
        @Test
        void 토큰_갱신_성공() {
            RefreshTokenRequest request = new RefreshTokenRequest("refresh-token-string");
            User user = makeDummyUser(UUID.randomUUID());
            RefreshToken refreshToken = makeDummyRefreshToken(user);

            when(refreshTokenRepository.findByToken("refresh-token-string")).thenReturn(Optional.of(refreshToken));
            when(jwtTokenProvider.generateToken(user.getUserId(), user.getEmail())).thenReturn("new-access-token");
            when(jwtTokenProvider.getAccessTokenExpirationInSeconds()).thenReturn(3600L);
            when(refreshTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            AuthResponse response = authService.refresh(request);

            assertThat(response.getAccessToken()).isEqualTo("new-access-token");
            assertThat(response.getUserId()).isEqualTo(user.getUserId());
        }

        @Test
        void 만료된_토큰_예외() {
            RefreshTokenRequest request = new RefreshTokenRequest("refresh-token-string");
            User user = makeDummyUser(UUID.randomUUID());
            RefreshToken expiredToken = makeDummyRefreshToken(user);
            expiredToken.setExpiresAt(LocalDateTime.now().minusDays(1));

            when(refreshTokenRepository.findByToken("refresh-token-string")).thenReturn(Optional.of(expiredToken));

            assertThatThrownBy(() -> authService.refresh(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("만료된 RefreshToken입니다");
            verify(refreshTokenRepository).delete(expiredToken);
        }

        @Test
        void 취소된_토큰_예외() {
            RefreshTokenRequest request = new RefreshTokenRequest("refresh-token-string");
            User user = makeDummyUser(UUID.randomUUID());
            RefreshToken revokedToken = makeDummyRefreshToken(user);
            revokedToken.setRevokedAt(LocalDateTime.now());

            when(refreshTokenRepository.findByToken("refresh-token-string")).thenReturn(Optional.of(revokedToken));

            assertThatThrownBy(() -> authService.refresh(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("취소된 RefreshToken입니다");
        }
    }

    @Nested
    @DisplayName("logout()")
    class LogoutTest {
        @Test
        void 정상_로그아웃() {
            String token = "refresh-token-string";
            User user = makeDummyUser(UUID.randomUUID());
            RefreshToken refreshToken = makeDummyRefreshToken(user);

            when(refreshTokenRepository.findByToken(token)).thenReturn(Optional.of(refreshToken));

            authService.logout(token);

            assertThat(refreshToken.getRevokedAt()).isNotNull();
            verify(refreshTokenRepository).findByToken(token);
        }

        @Test
        void 유효하지않은_토큰_예외() {
            when(refreshTokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.logout("invalid-token"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("유효하지 않은 RefreshToken입니다");
        }
    }
}
