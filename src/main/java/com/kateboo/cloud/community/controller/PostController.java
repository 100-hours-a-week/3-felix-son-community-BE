package com.kateboo.cloud.community.controller;

import com.kateboo.cloud.community.dto.request.PostRequest;
import com.kateboo.cloud.community.dto.response.LikeResponse;
import com.kateboo.cloud.community.dto.response.PageResponse;
import com.kateboo.cloud.community.dto.response.PostResponse;
import com.kateboo.cloud.community.service.PostService;
import com.kateboo.cloud.community.util.SessionUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    /**
     * 게시글 목록 조회 (인증 불필요)
     */
    @GetMapping
    public ResponseEntity<PageResponse<PostResponse>> getPosts(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<PostResponse> response = postService.getPosts(pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * 게시글 상세 조회 (인증 불필요)
     */
    @GetMapping("/{postId}")
    public ResponseEntity<PostResponse> getPost(@PathVariable UUID postId) {
        PostResponse response = postService.getPost(postId);
        return ResponseEntity.ok(response);
    }

    /**
     * 게시글 작성 (인증 필요)
     */
    @PostMapping
    public ResponseEntity<PostResponse> createPost(
            HttpServletRequest request,
            @Valid @RequestBody PostRequest postRequest) {
        UUID userId = SessionUtil.getUserIdFromSession(request);
        PostResponse response = postService.createPost(userId, postRequest);
        return ResponseEntity.status(201).body(response);
    }

    /**
     * 게시글 수정 (인증 필요)
     */
    @PutMapping("/{postId}")
    public ResponseEntity<PostResponse> updatePost(
            HttpServletRequest request,
            @PathVariable UUID postId,
            @Valid @RequestBody PostRequest postRequest) {
        UUID userId = SessionUtil.getUserIdFromSession(request);
        PostResponse response = postService.updatePost(userId, postId, postRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * 게시글 삭제 (인증 필요)
     */
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            HttpServletRequest request,
            @PathVariable UUID postId) {
        UUID userId = SessionUtil.getUserIdFromSession(request);
        postService.deletePost(userId, postId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 게시글 좋아요 토글 (인증 필요)
     */
    @PostMapping("/{postId}/like")
    public ResponseEntity<LikeResponse> toggleLike(
            HttpServletRequest request,
            @PathVariable UUID postId) {
        UUID userId = SessionUtil.getUserIdFromSession(request);
        LikeResponse response = postService.toggleLike(userId, postId);
        return ResponseEntity.ok(response);
    }


}