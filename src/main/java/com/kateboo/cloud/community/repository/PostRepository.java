package com.kateboo.cloud.community.repository;

import com.kateboo.cloud.community.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {

    // 최신순 정렬
    @EntityGraph(attributePaths = {"user", "postStats", "postImages"})
    @Query("SELECT DISTINCT p FROM Post p LEFT JOIN FETCH p.postStats LEFT JOIN FETCH p.user ORDER BY p.createdAt DESC")
    Page<Post> findAllOrderByCreatedAtDesc(Pageable pageable);

    // 조회수순 정렬
    @EntityGraph(attributePaths = {"user", "postStats", "postImages"})
    @Query("SELECT DISTINCT p FROM Post p LEFT JOIN FETCH p.postStats ps LEFT JOIN FETCH p.user ORDER BY ps.viewsCount DESC, p.createdAt DESC")
    Page<Post> findAllOrderByViewsCountDesc(Pageable pageable);

    // 좋아요순 정렬
    @EntityGraph(attributePaths = {"user", "postStats", "postImages"})
    @Query("SELECT DISTINCT p FROM Post p LEFT JOIN FETCH p.postStats ps LEFT JOIN FETCH p.user ORDER BY ps.likesCount DESC, p.createdAt DESC")
    Page<Post> findAllOrderByLikesCountDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"user", "postStats", "postImages"})
    Page<Post> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"user", "postStats", "postImages"})
    Optional<Post> findById(UUID postId);
}