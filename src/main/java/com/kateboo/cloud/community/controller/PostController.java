package com.kateboo.cloud.community.controller;

import com.kateboo.cloud.community.dto.request.PostRequest;
import com.kateboo.cloud.community.dto.response.LikeResponse;
import com.kateboo.cloud.community.dto.response.PageResponse;
import com.kateboo.cloud.community.dto.response.PostResponse;
import com.kateboo.cloud.community.security.CurrentUser;
import com.kateboo.cloud.community.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @GetMapping
    public ResponseEntity<PageResponse<PostResponse>> getPosts(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<PostResponse> response = postService.getPosts(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostResponse> getPost(@PathVariable UUID postId) {
        PostResponse response = postService.getPost(postId);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<PostResponse> createPost(
            @CurrentUser UUID userId,  // 변경!
            @Valid @RequestBody PostRequest request) {
        PostResponse response = postService.createPost(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{postId}")
    public ResponseEntity<PostResponse> updatePost(
            @CurrentUser UUID userId,  // 변경!
            @PathVariable UUID postId,
            @Valid @RequestBody PostRequest request) {
        PostResponse response = postService.updatePost(userId, postId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @CurrentUser UUID userId,  // 변경!
            @PathVariable UUID postId) {
        postService.deletePost(userId, postId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<LikeResponse> toggleLike(
            @CurrentUser UUID userId,  // 변경!
            @PathVariable UUID postId) {
        LikeResponse response = postService.toggleLike(userId, postId);
        return ResponseEntity.ok(response);
    }
}