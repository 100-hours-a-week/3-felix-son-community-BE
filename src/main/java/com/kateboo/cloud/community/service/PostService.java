package com.kateboo.cloud.community.service;

import com.kateboo.cloud.community.dto.request.PostRequest;
import com.kateboo.cloud.community.dto.response.LikeResponse;
import com.kateboo.cloud.community.dto.response.PageResponse;
import com.kateboo.cloud.community.dto.response.PostResponse;
import com.kateboo.cloud.community.entity.*;
import com.kateboo.cloud.community.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostLikeRepository postLikeRepository;

    /**
     * 게시글 목록 조회
     */
    @Transactional(readOnly = true)
    public PageResponse<PostResponse> getPosts(Pageable pageable) {
        Page<Post> posts = postRepository.findAll(pageable);
        return PageResponse.of(posts, PostResponse::from);
    }

    /**
     * 게시글 상세 조회
     */
    @Transactional
    public PostResponse getPost(UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다"));

        // 조회수 증가
        post.getPostStats().incrementViewsCount();

        return PostResponse.from(post);
    }

    /**
     * 게시글 작성(PostStats 자동 생성)
     */
    @Transactional
    public PostResponse createPost(UUID userId, PostRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        // Post 생성
        Post post = Post.builder()
                .title(request.getTitle())
                .body(request.getBody())
                .user(user)
                .build();

        // PostStats 생성 및 연결
        PostStats postStats = PostStats.builder()
                .post(post)
                .likesCount(0)
                .viewsCount(0L)
                .commentCount(0)
                .build();
        post.setPostStats(postStats);

        // PostImage 추가
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            for (int i = 0; i < request.getImageUrls().size(); i++) {
                PostImage image = PostImage.builder()
                        .imageUrl(request.getImageUrls().get(i))
                        .orderNo(i)
                        .post(post)
                        .build();
                post.getPostImages().add(image);
            }
        }

        // 저장
        Post savedPost = postRepository.save(post);

        log.info("게시글 작성 완료: postId={}, userId={}", savedPost.getPostId(), userId);

        return PostResponse.from(savedPost);
    }

    /**
     * 게시글 수정
     */
    @Transactional
    public PostResponse updatePost(UUID userId, UUID postId, PostRequest request) {
        // 1. 게시글 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다"));

        // 2. 권한 확인
        if (!post.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("본인의 게시글만 수정할 수 있습니다");
        }

        // 3. 기본 정보 수정
        post.setTitle(request.getTitle());
        post.setBody(request.getBody());
        post.setUpdatedAt(LocalDateTime.now());

        // 4. 이미지 수정 (orphanRemoval 대응)
        if (request.getImageUrls() != null) {
            // 기존 이미지 모두 제거
            post.getPostImages().clear();

            // flush로 삭제 반영
            postRepository.flush();

            // 새 이미지 추가
            for (int i = 0; i < request.getImageUrls().size(); i++) {
                PostImage image = PostImage.builder()
                        .imageUrl(request.getImageUrls().get(i))
                        .orderNo(i)
                        .post(post)
                        .build();
                post.getPostImages().add(image);
            }
        }

        log.info("게시글 수정 완료: postId={}, userId={}", postId, userId);

        return PostResponse.from(post);
    }

    /**
     * 게시글 삭제
     */
    @Transactional
    public void deletePost(UUID userId, UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다"));

        if (!post.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("본인의 게시글만 삭제할 수 있습니다");
        }

        postRepository.delete(post);
        log.info("게시글 삭제 완료: postId={}, userId={}", postId, userId);
    }

    /**
     * 게시글 좋아요 토글
     */
    @Transactional
    public LikeResponse toggleLike(UUID userId, UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        // 이미 좋아요를 눌렀는지 확인
        Optional<PostLike> existingLike = postLikeRepository
                .findByPost_PostIdAndUser_UserId(postId, userId);

        boolean isLiked;

        if (existingLike.isPresent()) {
            // 좋아요 취소
            postLikeRepository.delete(existingLike.get());

            // 좋아요 수 감소
            post.getPostStats().decrementLikesCount();

            isLiked = false;
            log.info("좋아요 취소: userId={}, postId={}", userId, postId);
        } else {
            // 좋아요 추가
            PostLike newLike = PostLike.builder()
                    .post(post)
                    .user(user)
                    .build();

            postLikeRepository.save(newLike);

            // 좋아요 수 증가
            post.getPostStats().incrementLikesCount();

            isLiked = true;
            log.info("좋아요 등록: userId={}, postId={}", userId, postId);
        }

        int likesCount = post.getPostStats().getLikesCount();

        return LikeResponse.builder()
                .isLiked(isLiked)
                .likesCount(likesCount)
                .build();
    }
}