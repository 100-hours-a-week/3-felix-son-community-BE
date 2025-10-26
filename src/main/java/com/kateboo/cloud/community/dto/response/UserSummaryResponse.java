package com.kateboo.cloud.community.dto.response;

import com.kateboo.cloud.community.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryResponse {

    private UUID userId;
    private String nickname;
    private String profileImageUrl;

    public static UserSummaryResponse from(User user) {
        if (user == null) {
            return null;
        }

        return UserSummaryResponse.builder()
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .build();
    }
}