//package com.kateboo.cloud.community.dto.response;
//
//import lombok.*;
//
//import java.util.UUID;
//
//@Getter
//@Setter
//@Builder
//@NoArgsConstructor
//@AllArgsConstructor
//public class LoginResponse {
//
//    private UUID userId;
//    private String email;
//    private String nickname;
//    private String profileImageUrl;
//    private String accessToken;
//    private String refreshToken;
//    private String tokenType;
//
//    public static LoginResponse of(UserResponse user, String accessToken, String refreshToken) {
//        return LoginResponse.builder()
//                .userId(user.getUserId())
//                .email(user.getEmail())
//                .nickname(user.getNickname())
//                .profileImageUrl(user.getProfileImageUrl())
//                .accessToken(accessToken)
//                .refreshToken(refreshToken)
//                .tokenType("Bearer")
//                .build();
//    }
//}