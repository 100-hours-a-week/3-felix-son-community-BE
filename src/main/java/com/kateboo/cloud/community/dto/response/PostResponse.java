package com.kateboo.cloud.community.dto.response;

import com.kateboo.cloud.community.entity.Post;
import lombok.*;

import java.time.Instant;
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
    private Instant createdAt;
    private Instant updatedAt;

    private UserSummaryResponse user;

    @Builder.Default
    private List<PostImageResponse> images = new ArrayList<>();

    private PostStatsResponse stats;

    private String formattedViewsCount;
    private String formattedCommentsCount;
    private String formattedLikesCount;

    public static PostResponse from(Post post) {
        if (post == null) {
            return null;
        }

        List<PostImageResponse> imageResponses = new ArrayList<>();
        if (post.getPostImages() != null && !post.getPostImages().isEmpty()) {
            imageResponses = post.getPostImages().stream()
                    .map(PostImageResponse::from)
                    .collect(Collectors.toList());
        }

        PostStatsResponse statsResponse = PostStatsResponse.from(post.getPostStats());

        PostResponse response = PostResponse.builder()
                .postId(post.getPostId())
                .title(post.getTitle())
                .body(post.getBody())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .user(UserSummaryResponse.from(post.getUser()))
                .images(imageResponses)
                .stats(statsResponse)
                .build();

        response.formattedViewsCount = formatCount(post.getPostStats().getViewsCount());
        response.formattedCommentsCount = formatCount(post.getPostStats().getCommentCount());
        response.formattedLikesCount = formatCount(post.getPostStats().getLikesCount());

        return response;
    }

    private static String formatCount(Number count) {
        if (count == null) {
            return "0";
        }
        int value = count.intValue();

        if (value < 1000) return String.valueOf(value);
        if (value < 10_000) return String.format("%.1fk", value / 1000.0);
        if (value < 100_000) return String.format("%dk", value / 1000);
        return String.format("%dk+", value / 1000);
    }
}
