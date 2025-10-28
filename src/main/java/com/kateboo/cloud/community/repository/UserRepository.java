package com.kateboo.cloud.community.repository;

import com.kateboo.cloud.community.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // 이메일 중복 체크
    boolean existsByEmail(String email);

    // 닉네임 중복 체크
    boolean existsByNickname(String nickname);

    //
    // 활성 사용자만 조회 (로그인용)
    Optional<User> findByEmailAndIsActiveTrue(String email);

    Optional<User> findByEmail(String email);
    /**
     * ✅ 7일 지난 비활성 계정 조회
     * 스케줄러에서 자동 삭제할 계정을 찾을 때 사용
     *
     * @param cutoffDate 기준 일시 (현재 시간 - 7일)
     * @return 만료된 비활성 계정 목록
     */
    List<User> findByIsActiveFalseAndDeactivatedAtBefore(LocalDateTime cutoffDate);
}