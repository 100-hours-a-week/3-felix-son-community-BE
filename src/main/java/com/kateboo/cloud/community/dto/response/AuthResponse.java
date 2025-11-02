package com.kateboo.cloud.community.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken; // 백엔드 내부용, 쿠키 설정 후 null로 변경
    private UUID userId;
    private String email;
    private String nickname;
    private String profileImageUrl;
    private Boolean accountRestored;
}