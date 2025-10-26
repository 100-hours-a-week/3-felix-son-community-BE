package com.kateboo.cloud.community.repository;

import com.kateboo.cloud.community.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // 이메일 중복 체크
    boolean existsByEmail(String email);

    // 닉네임 중복 체크
    boolean existsByNickname(String nickname);

    // 활성 사용자만 조회 (로그인용)
    Optional<User> findByEmailAndIsActiveTrue(String email);
}