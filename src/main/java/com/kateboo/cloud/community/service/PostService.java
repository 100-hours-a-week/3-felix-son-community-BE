package com.kateboo.cloud.community.service;

import com.kateboo.cloud.community.dto.request.PostRequest;
import com.kateboo.cloud.community.dto.response.LikeResponse;
import com.kateboo.cloud.community.dto.response.PageResponse;
import com.kateboo.cloud.community.dto.response.PostResponse;
import com.kateboo.cloud.community.entity.*;
import com.kateboo.cloud.community.exception.BadRequestException;
import com.kateboo.cloud.community.exception.ForbiddenException;
import com.kateboo.cloud.community.exception.NotFoundException;
import com.kateboo.cloud.community.repository.PostLikeRepository;
import com.kateboo.cloud.community.repository.PostRepository;
import com.kateboo.cloud.community.repository.PostStatsRepository;
import com.kateboo.cloud.community.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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
    private final PostStatsRepository postStatsRepository;
    private final UserRepository userRepository;
    private final PostLikeRepository postLikeRepository;

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> getPosts(Pageable pageable) {
        Page<Post> posts = postRepository.findAll(pageable);
        return PageResponse.of(posts, PostResponse::from);
    }

    @Transactional
    public PostResponse getPost(UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("게시글을 찾을 수 없습니다"));

        this.incrementViewCountAsync(postId);
        return PostResponse.from(post);
    }

    @Transactional
    public PostResponse createPost(UUID userId, PostRequest request) {
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BadRequestException("제목은 필수입니다");
        }
        if (request.getTitle().length() > 26) {
            throw new BadRequestException("제목은 최대 26자까지 가능합니다");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다"));

        Post post = Post.builder()
                .title(request.getTitle())
                .body(request.getBody())
                .user(user)
                .build();

        PostStats postStats = PostStats.builder()
                .post(post)
                .likesCount(0)
                .viewsCount(0L)
                .commentCount(0)
                .build();
        post.setPostStats(postStats);

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

        Post savedPost = postRepository.save(post);
        log.info("게시글 작성 완료: postId={}, userId={}", savedPost.getPostId(), userId);

        return PostResponse.from(savedPost);
    }

    @Transactional
    public PostResponse updatePost(UUID userId, UUID postId, PostRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("게시글을 찾을 수 없습니다"));

        if (!post.getUser().getUserId().equals(userId)) {
            throw new ForbiddenException("본인의 게시글만 수정할 수 있습니다");
        }

        post.setTitle(request.getTitle());
        post.setBody(request.getBody());
        post.setUpdatedAt(LocalDateTime.now());

        if (request.getImageUrls() != null) {
            post.getPostImages().clear();
            postRepository.flush();
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

    @Transactional
    public void deletePost(UUID userId, UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("게시글을 찾을 수 없습니다"));

        if (!post.getUser().getUserId().equals(userId)) {
            throw new ForbiddenException("본인의 게시글만 삭제할 수 있습니다");
        }

        postRepository.delete(post);
        log.info("게시글 삭제 완료: postId={}, userId={}", postId, userId);
    }

    @Transactional
    public LikeResponse toggleLike(UUID userId, UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("게시글을 찾을 수 없습니다"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다"));

        Optional<PostLike> existingLike = postLikeRepository
                .findByPost_PostIdAndUser_UserId(postId, userId);

        boolean isLiked;

        if (existingLike.isPresent()) {
            postLikeRepository.delete(existingLike.get());
            post.getPostStats().decrementLikesCount();
            isLiked = false;
            log.info("좋아요 취소: userId={}, postId={}", userId, postId);
        } else {
            PostLike newLike = PostLike.builder()
                    .post(post)
                    .user(user)
                    .build();
            postLikeRepository.save(newLike);
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

    @Async("viewCountExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void incrementViewCountAsync(UUID postId){
        try{
            postStatsRepository.incrementViewsCount(postId);
            log.debug("조회수 증가 완료: postId={}", postId);
        } catch(Exception e){
            log.error("조회수 증가 실패: postId={}, error={}", postId, e.getMessage());
        }
    }

}
