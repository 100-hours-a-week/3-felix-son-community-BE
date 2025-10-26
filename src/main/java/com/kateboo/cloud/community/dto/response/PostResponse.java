package com.kateboo.cloud.community.dto.response;

import com.kateboo.cloud.community.entity.Post;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostResponse {

    private UUID postId;
    private String title;
    private String body;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private UserSummaryResponse user;

    @Builder.Default
    private List<PostImageResponse> images = new ArrayList<>();

    private PostStatsResponse stats;

    public static PostResponse from(Post post) {
        if (post == null) {
            return null;
        }

        // PostImage 변환
        List<PostImageResponse> imageResponses = new ArrayList<>();
        if (post.getPostImages() != null && !post.getPostImages().isEmpty()) {
            imageResponses = post.getPostImages().stream()
                    .map(PostImageResponse::from)
                    .collect(Collectors.toList());
        }

        return PostResponse.builder()
                .postId(post.getPostId())
                .title(post.getTitle())
                .body(post.getBody())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .user(UserSummaryResponse.from(post.getUser()))
                .images(imageResponses)
                .stats(PostStatsResponse.from(post.getPostStats()))
                .build();
    }
}