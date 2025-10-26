package com.kateboo.cloud.community.dto.response;

import com.kateboo.cloud.community.entity.PostImage;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostImageResponse {

    private Long imageId;
    private String imageUrl;
    private Integer orderNo;

    public static PostImageResponse from(PostImage image) {
        return PostImageResponse.builder()
                .imageId(image.getImageId())
                .imageUrl(image.getImageUrl())
                .orderNo(image.getOrderNo())
                .build();
    }
}