package com.kateboo.cloud.community.repository;

import com.kateboo.cloud.community.entity.PostStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PostStatsRepository extends JpaRepository<PostStats, UUID> {
    @Modifying(clearAutomatically = true)
    @Query("UPDATE PostStats ps Set ps.viewsCount = ps.viewsCount + 1 WHERE ps.postId = :postId")
    void incrementViewsCount(@Param("postId") UUID postId);

}
