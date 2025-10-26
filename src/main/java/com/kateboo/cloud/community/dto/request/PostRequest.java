package com.kateboo.cloud.community.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostRequest {
    @NotBlank(message = "제목은 필수입니다")
    @Size(min = 1, max = 26, message = "제목은 1-26자 사이여야 합니다")
    private String title;

    private String body;

    private List<String> imageUrls;
}
