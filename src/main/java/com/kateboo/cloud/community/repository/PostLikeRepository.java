package com.kateboo.cloud.community.repository;

import com.kateboo.cloud.community.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    /**
     * 특정 게시글에 특정 사용자가 좋아요를 눌렀는지 조회
     */
    Optional<PostLike> findByPost_PostIdAndUser_UserId(UUID postId, UUID userId);

    /**
     * 특정 게시글의 좋아요 개수
     */
    int countByPost_PostId(UUID postId);

    /**
     * 특정 게시글에 특정 사용자가 좋아요를 눌렀는지 여부
     */
    boolean existsByPost_PostIdAndUser_UserId(UUID postId, UUID userId);

    /**
     * 특정 게시글의 모든 좋아요 삭제 (게시글 삭제 시)
     */
    void deleteByPost_PostId(UUID postId);
}