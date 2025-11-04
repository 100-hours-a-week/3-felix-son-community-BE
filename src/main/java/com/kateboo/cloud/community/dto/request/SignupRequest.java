package com.kateboo.cloud.community.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequest {

    @NotBlank(message = "이메일을 입력해주세요")
    @Email(message = "올바른 이메일 주소 형식을 입력해주세요")
    @Size(max = 254, message = "이메일은 254자 이하여야 합니다")
    private String email;

    @NotBlank(message = "비밀번호를 입력해주세요")
    @Size(min = 8, max = 20, message = "비밀번호는 8자 이상, 20자 이하여야 합니다")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*(),.?\":{}|<>]).{8,20}$",
            message = "비밀번호는 8자 이상, 20자 이하이며, 대문자, 소문자, 숫자, 특수문자를 각각 최소 1개 포함해야합니다"
    )
    private String password;

    @NotBlank(message = "닉네임을 입력해주세요")
    @Size(min = 1, max = 10, message = "닉네임은 최대 10자까지 작성 가능합니다")
    @Pattern(
            regexp = "^\\S+$",
            message = "띄어쓰기를 없애주세요"
    )
    private String nickname;

    @NotBlank(message = "프로필 사진을 추가해주세요")
    @Size(max = 500, message = "프로필 이미지 URL은 500자 이하여야 합니다")
    private String profileImageUrl;
}