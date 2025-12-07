package com.kateboo.cloud.community.dto.response;

import com.kateboo.cloud.community.entity.PostStats;
import lombok.*;
import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostStatsResponse {

    private Integer likesCount;
    private Long viewsCount;
    private Integer commentCount;
    private Instant updatedAt;

    public static PostStatsResponse from(PostStats stats) {
        return PostStatsResponse.builder()
                .likesCount(stats.getLikesCount())
                .viewsCount(stats.getViewsCount())
                .commentCount(stats.getCommentCount())
                .updatedAt(stats.getUpdatedAt())
                .build();
    }
}