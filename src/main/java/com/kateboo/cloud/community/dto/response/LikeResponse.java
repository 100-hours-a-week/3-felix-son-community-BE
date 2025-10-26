package com.kateboo.cloud.community.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LikeResponse {

    private boolean isLiked;      // 현재 좋아요 상태
    private int likesCount;       // 총 좋아요 개수
}