package com.kateboo.cloud.community.service;

import com.kateboo.cloud.community.dto.request.PasswordChangeRequest;
import com.kateboo.cloud.community.dto.request.ProfileUpdateRequest;
import com.kateboo.cloud.community.dto.response.UserResponse;
import com.kateboo.cloud.community.entity.User;
import com.kateboo.cloud.community.repository.RefreshTokenRepository;
import com.kateboo.cloud.community.repository.UserRepository;
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

class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

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
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("getMyInfo()")
    class GetMyInfoTest {
        @Test
        void 정상_조회() {
            UUID userId = UUID.randomUUID();
            User user = makeDummyUser(userId);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            UserResponse response = userService.getMyInfo(userId);

            assertThat(response.getUserId()).isEqualTo(userId);
            assertThat(response.getEmail()).isEqualTo(user.getEmail());
            verify(userRepository).findById(userId);
        }

        @Test
        void 사용자_없음_예외() {
            UUID userId = UUID.randomUUID();
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getMyInfo(userId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("사용자를 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("updateMyProfile()")
    class UpdateMyProfileTest {
        @Test
        void 닉네임_변경_정상() {
            UUID userId = UUID.randomUUID();
            User user = makeDummyUser(userId);

            ProfileUpdateRequest request = new ProfileUpdateRequest("newNickname", null);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.existsByNickname("newNickname")).thenReturn(false);

            UserResponse response = userService.updateMyProfile(userId, request);

            assertThat(response.getNickname()).isEqualTo("newNickname");
            verify(userRepository).existsByNickname("newNickname");
        }

        @Test
        void 닉네임_중복_예외() {
            UUID userId = UUID.randomUUID();
            User user = makeDummyUser(userId);

            ProfileUpdateRequest request = new ProfileUpdateRequest("existingNickname", null);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.existsByNickname("existingNickname")).thenReturn(true);

            assertThatThrownBy(() -> userService.updateMyProfile(userId, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("이미 사용 중인 닉네임입니다");
        }

        @Test
        void 프로필이미지_변경_정상() {
            UUID userId = UUID.randomUUID();
            User user = makeDummyUser(userId);

            ProfileUpdateRequest request = new ProfileUpdateRequest(null, "http://new.image/url.png");
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            UserResponse response = userService.updateMyProfile(userId, request);

            assertThat(response.getProfileImageUrl()).isEqualTo("http://new.image/url.png");
        }
    }

    @Nested
    @DisplayName("changePassword()")
    class ChangePasswordTest {
        @Test
        void 비밀번호_변경_정상() {
            UUID userId = UUID.randomUUID();
            User user = makeDummyUser(userId);

            PasswordChangeRequest request = new PasswordChangeRequest("newpass123", "newpass123");

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(passwordEncoder.encode("newpass123")).thenReturn("encoded-password");

            userService.changePassword(userId, request);

            verify(passwordEncoder).encode("newpass123");
            assertThat(user.getPasswordHash()).isEqualTo("encoded-password");
        }

        @Test
        void 비밀번호_불일치_예외() {
            UUID userId = UUID.randomUUID();
            User user = makeDummyUser(userId);

            PasswordChangeRequest request = new PasswordChangeRequest("newpass123", "different");

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> userService.changePassword(userId, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("새 비밀번호와 확인 비밀번호가 일치하지 않습니다.");
        }
    }

    @Nested
    @DisplayName("softDeleteAccount()")
    class SoftDeleteAccountTest {
        @Test
        void 계정_소프트삭제_정상() {
            UUID userId = UUID.randomUUID();
            User user = makeDummyUser(userId);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            userService.softDeleteAccount(userId);

            assertThat(user.getIsActive()).isFalse();
            assertThat(user.getDeactivatedAt()).isNotNull();
            verify(refreshTokenRepository).deleteByUser_UserId(userId);
        }

        @Test
        void 이미_비활성_계정_예외() {
            UUID userId = UUID.randomUUID();
            User user = makeDummyUser(userId);
            user.setIsActive(false);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> userService.softDeleteAccount(userId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("이미 탈퇴 처리된 계정입니다.");
        }
    }

    @Nested
    @DisplayName("checkAndRestoreIfNeeded()")
    class CheckAndRestoreTest {
        @Test
        void 활성_계정_그대로_반환() {
            User user = makeDummyUser(UUID.randomUUID());

            when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

            User result = userService.checkAndRestoreIfNeeded(user.getEmail());

            assertThat(result).isEqualTo(user);
            verify(userRepository, never()).save(any());
        }

        @Test
        void 비활성_계정_7일_이내_자동복구() {
            User user = makeDummyUser(UUID.randomUUID());
            user.setIsActive(false);
            user.setDeactivatedAt(LocalDateTime.now().minusDays(3));

            when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
            when(userRepository.save(user)).thenReturn(user);

            User result = userService.checkAndRestoreIfNeeded(user.getEmail());

            assertThat(result.getIsActive()).isTrue();
            assertThat(result.getDeactivatedAt()).isNull();
            verify(userRepository).save(user);
        }

        @Test
        void 비활성_계정_7일_초과_예외() {
            User user = makeDummyUser(UUID.randomUUID());
            user.setIsActive(false);
            user.setDeactivatedAt(LocalDateTime.now().minusDays(10));

            when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> userService.checkAndRestoreIfNeeded(user.getEmail()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("탈퇴 후 7일이 지나 계정이 영구 삭제 예정입니다");
        }

        @Test
        void deactivatedAt_없음_예외처리() {
            User user = makeDummyUser(UUID.randomUUID());
            user.setIsActive(false);
            user.setDeactivatedAt(null);

            when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> userService.checkAndRestoreIfNeeded(user.getEmail()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("비활성 계정입니다. 관리자에게 문의하세요.");
        }

        @Test
        void 사용자_없음_예외() {
            when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.checkAndRestoreIfNeeded("nonexistent@example.com"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("사용자를 찾을 수 없습니다.");
        }
    }


    @Nested
    @DisplayName("restoreAccount()")
    class RestoreAccountTest {
        @Test
        void 수동_복구_성공() {
            UUID userId = UUID.randomUUID();
            User user = makeDummyUser(userId);
            user.setIsActive(false);
            user.setDeactivatedAt(LocalDateTime.now().minusDays(3));

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            UserResponse response = userService.restoreAccount(userId);

            assertThat(response.getUserId()).isEqualTo(userId);
            assertThat(user.getIsActive()).isTrue();
            assertThat(user.getDeactivatedAt()).isNull();
        }

        @Test
        void 이미_활성화된_계정_예외() {
            UUID userId = UUID.randomUUID();
            User user = makeDummyUser(userId);
            user.setIsActive(true);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> userService.restoreAccount(userId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("이미 활성화된 계정입니다.");
        }

        @Test
        void 복구_기간_지남_예외() {
            UUID userId = UUID.randomUUID();
            User user = makeDummyUser(userId);
            user.setIsActive(false);
            user.setDeactivatedAt(LocalDateTime.now().minusDays(10));

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> userService.restoreAccount(userId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("복구 기간(7일)이 지났습니다.");
        }

        @Test
        void 사용자_없음_예외() {
            UUID userId = UUID.randomUUID();
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.restoreAccount(userId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("사용자를 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("deleteExpiredAccounts()")
    class DeleteExpiredAccountsTest {
        @Test
        void 영구삭제_대상_없음() {
            when(userRepository.findByIsActiveFalseAndDeactivatedAtBefore(any())).thenReturn(java.util.Collections.emptyList());

            userService.deleteExpiredAccounts();

            verify(userRepository, never()).delete(any());
            verify(refreshTokenRepository, never()).deleteByUser_UserId(any());
        }

        @Test
        void 영구삭제_정상() {
            UUID userId = UUID.randomUUID();

            User user = makeDummyUser(userId);
            user.setIsActive(false);
            user.setDeactivatedAt(LocalDateTime.now().minusDays(10));

            when(userRepository.findByIsActiveFalseAndDeactivatedAtBefore(any())).thenReturn(java.util.List.of(user));

            userService.deleteExpiredAccounts();

            verify(refreshTokenRepository).deleteByUser_UserId(userId);
            verify(userRepository).delete(user);
        }
    }
}
