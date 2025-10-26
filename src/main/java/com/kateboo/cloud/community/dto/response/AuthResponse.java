package com.kateboo.cloud.community.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;        // "Bearer"
    private Long expiresIn;          // AccessToken 만료 시간 (초)

    // 사용자 정보
    private UUID userId;
    private String email;
    private String nickname;
    private String profileImageUrl;
}