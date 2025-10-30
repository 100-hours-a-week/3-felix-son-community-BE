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

}