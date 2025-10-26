package com.kateboo.cloud.community.repository;

import com.kateboo.cloud.community.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {

    /**
     * 특정 게시글의 댓글 목록 조회 (Pageable로 정렬 자유롭게)
     * Controller에서 정렬 방향을 지정할 수 있음
     */
    Page<Comment> findByPost_PostId(UUID postId, Pageable pageable);

    /**
     * 특정 사용자의 댓글 목록 조회 (Pageable로 정렬 자유롭게)
     */
    Page<Comment> findByUser_UserId(UUID userId, Pageable pageable);

    /**
     * 특정 게시글의 댓글 수 카운트
     */
    long countByPost_PostId(UUID postId);
}