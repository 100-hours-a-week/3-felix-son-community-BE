package com.kateboo.cloud.community.repository;

import com.kateboo.cloud.community.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {

    /**
     * 게시글 목록 조회
     */
    @EntityGraph(attributePaths = {"user", "postStats", "postImages"})
    Page<Post> findAll(Pageable pageable);

    /**
     * 게시글 상세 조회
     */
    @EntityGraph(attributePaths = {"user", "postStats", "postImages"})
    Optional<Post> findById(UUID postId);

    /**
     * 사용자별 게시글 조회
     */
    @EntityGraph(attributePaths = {"postStats", "postImages"})
    @Query("SELECT p FROM Post p WHERE p.user.userId = :userId ORDER BY p.createdAt DESC")
    Page<Post> findByUser_UserId(@Param("userId") UUID userId, Pageable pageable);
}