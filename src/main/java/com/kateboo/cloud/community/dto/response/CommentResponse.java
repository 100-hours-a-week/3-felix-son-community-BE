package com.kateboo.cloud.community.dto.response;

import com.kateboo.cloud.community.entity.Comment;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {

    private UUID commentId;
    private String body;
    private UserSummaryResponse user;  // ✅ 변경: UserResponse → UserSummaryResponse
    private UUID postId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CommentResponse from(Comment comment) {
        return CommentResponse.builder()
                .commentId(comment.getCommentId())
                .body(comment.getBody())
                .user(comment.getUser() != null ? UserSummaryResponse.from(comment.getUser()) : null)  // ✅ 변경
                .postId(comment.getPost() != null ? comment.getPost().getPostId() : null)
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}