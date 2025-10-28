package com.kateboo.cloud.community.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;

    private UUID userId;
    private String email;
    private String nickname;
    private String profileImageUrl;

    /**
     * ✅ 계정 복구 여부 (7일 이내 탈퇴 계정 재로그인)
     * true: 방금 계정이 복구됨
     * false: 정상 로그인
     */
    @Builder.Default
    private Boolean accountRestored = false;
}