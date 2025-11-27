package com.kateboo.cloud.community.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "post_stats")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class PostStats {

    @Id
    @Column(name = "post_id", columnDefinition = "BINARY(16)")
    private UUID postId;

    @Column(name = "likes_count", nullable = false)
    @Builder.Default
    private Integer likesCount = 0;

    @Column(name = "views_count", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    @Builder.Default
    private Long viewsCount = 0L;

    @Column(name = "comment_count", nullable = false, columnDefinition = "INT UNSIGNED")
    @Builder.Default
    private Integer commentCount = 0;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "post_id")
    private Post post;


    public void incrementCommentCount() {
        this.commentCount++;
    }

    public void decrementCommentCount() {
        if (this.commentCount > 0) {
            this.commentCount--;
        }
    }

    public void incrementLikesCount() {
        this.likesCount++;
    }

    public void decrementLikesCount() {
        if (this.likesCount > 0) {
            this.likesCount--;
        }
    }
}