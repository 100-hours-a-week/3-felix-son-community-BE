package com.kateboo.cloud.community.dto.response;

import com.kateboo.cloud.community.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private UUID userId;
    private String email;
    private String nickname;
    private String profileImageUrl;
    private Boolean isActive;
    private LocalDateTime createdAt;

    public static UserResponse from(User user) {
        if (user == null) {
            return null;
        }

        return UserResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}