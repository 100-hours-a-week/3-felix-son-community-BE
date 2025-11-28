package com.kateboo.cloud.community.service;

import com.kateboo.cloud.community.dto.request.LoginRequest;
import com.kateboo.cloud.community.dto.request.SignupRequest;
import com.kateboo.cloud.community.dto.response.AuthResponse;
import com.kateboo.cloud.community.entity.User;
import com.kateboo.cloud.community.exception.BadRequestException;
import com.kateboo.cloud.community.exception.ConflictException;
import com.kateboo.cloud.community.repository.RefreshTokenRepository;
import com.kateboo.cloud.community.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private SignupRequest validSignupRequest;

    @BeforeEach
    void setUp() {
        validSignupRequest = new SignupRequest(
                "test@example.com",
                "TestPass123!",
                "테스터",
                "https://example.com/profile.jpg"
        );
    }

    @Test
    @DisplayName("회원가입 성공")
    void signup_Success() {
        // when
        AuthResponse response = authService.signup(validSignupRequest);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getNickname()).isEqualTo("테스터");
        assertThat(response.getAccessToken()).isNotNull();
        assertThat(response.getRefreshToken()).isNotNull();
        assertThat(response.isAccountRestored()).isFalse();

        // DB 확인
        User savedUser = userRepository.findByEmail("test@example.com").orElseThrow();
        assertThat(savedUser.getIsActive()).isTrue();
        assertThat(passwordEncoder.matches("TestPass123!", savedUser.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("회원가입 실패 - 중복 이메일")
    void signup_Fail_DuplicateEmail() {
        // given
        authService.signup(validSignupRequest);

        // when & then
        assertThatThrownBy(() -> authService.signup(validSignupRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessage("중복된 이메일입니다");
    }

    @Test
    @DisplayName("회원가입 실패 - 잘못된 이메일 형식")
    void signup_Fail_InvalidEmailFormat() {
        // given
        SignupRequest invalidRequest = new SignupRequest(
                "invalid-email",
                "TestPass123!",
                "테스터",
                "https://example.com/profile.jpg"
        );

        // when & then
        assertThatThrownBy(() -> authService.signup(invalidRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("올바른 이메일 주소 형식을 입력해주세요");
    }

    @Test
    @DisplayName("회원가입 실패 - 비밀번호 형식 오류 (대문자 없음)")
    void signup_Fail_InvalidPassword() {
        // given
        SignupRequest invalidRequest = new SignupRequest(
                "test@example.com",
                "testpass123!",  // 대문자 없음
                "테스터",
                "https://example.com/profile.jpg"
        );

        // when & then
        assertThatThrownBy(() -> authService.signup(invalidRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("비밀번호는 8자 이상, 20자 이하이며");
    }

    @Test
    @DisplayName("회원가입 실패 - 닉네임 공백 포함")
    void signup_Fail_NicknameWithWhitespace() {
        // given
        SignupRequest invalidRequest = new SignupRequest(
                "test@example.com",
                "TestPass123!",
                "테스 터",  // 공백 포함
                "https://example.com/profile.jpg"
        );

        // when & then
        assertThatThrownBy(() -> authService.signup(invalidRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("띄어쓰기를 없애주세요");
    }

    @Test
    @DisplayName("회원가입 실패 - 프로필 이미지 없음")
    void signup_Fail_NoProfileImage() {
        // given
        SignupRequest invalidRequest = new SignupRequest(
                "test@example.com",
                "TestPass123!",
                "테스터",
                ""  // 빈 문자열
        );

        // when & then
        assertThatThrownBy(() -> authService.signup(invalidRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("프로필 사진을 추가해주세요");
    }

    @Test
    @DisplayName("로그인 성공")
    void login_Success() {
        // given
        authService.signup(validSignupRequest);
        LoginRequest loginRequest = new LoginRequest(
                "test@example.com",
                "TestPass123!"
        );

        // when
        AuthResponse response = authService.login(loginRequest);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getAccessToken()).isNotNull();
        assertThat(response.getRefreshToken()).isNotNull();
        assertThat(response.isAccountRestored()).isFalse();
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 비밀번호")
    void login_Fail_WrongPassword() {
        // given
        authService.signup(validSignupRequest);
        LoginRequest loginRequest = new LoginRequest(
                "test@example.com",
                "WrongPass123!"
        );

        // when & then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("이메일 또는 비밀번호가 올바르지 않습니다");
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 이메일")
    void login_Fail_EmailNotFound() {
        // given
        LoginRequest loginRequest = new LoginRequest(
                "notexist@example.com",
                "TestPass123!"
        );

        // when & then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("이메일 또는 비밀번호가 올바르지 않습니다");
    }

    @Test
    @DisplayName("토큰 갱신 성공")
    void refresh_Success() {
        // given
        AuthResponse signupResponse = authService.signup(validSignupRequest);
        String refreshToken = signupResponse.getRefreshToken();

        // when
        AuthResponse response = authService.refresh(refreshToken);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isNotNull();
        assertThat(response.getRefreshToken()).isNotNull();
        assertThat(response.getRefreshToken()).isNotEqualTo(refreshToken);  // 새 토큰 발급
    }

    @Test
    @DisplayName("로그아웃 성공")
    void logout_Success() {
        // given
        AuthResponse signupResponse = authService.signup(validSignupRequest);
        String refreshToken = signupResponse.getRefreshToken();

        // when
        authService.logout(refreshToken);

        // then
        // 취소된 토큰으로 갱신 시도하면 실패해야 함
        assertThatThrownBy(() -> authService.refresh(refreshToken))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("취소된 RefreshToken입니다");
    }
}