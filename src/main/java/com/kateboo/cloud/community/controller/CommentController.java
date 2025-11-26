package com.kateboo.cloud.community.controller;

import com.kateboo.cloud.community.dto.request.CommentRequest;
import com.kateboo.cloud.community.dto.response.CommentResponse;
import com.kateboo.cloud.community.dto.response.PageResponse;
import com.kateboo.cloud.community.security.CurrentUser;
import com.kateboo.cloud.community.service.CommentService;
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
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/post/{postId}")
    public ResponseEntity<PageResponse<CommentResponse>> getComments(
            @PathVariable UUID postId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {
        PageResponse<CommentResponse> response = commentService.getComments(postId, pageable);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/post/{postId}")
    public ResponseEntity<CommentResponse> createComment(
            @CurrentUser UUID userId,
            @PathVariable UUID postId,
            @Valid @RequestBody CommentRequest request) {
        CommentResponse response = commentService.createComment(userId, postId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(
            @CurrentUser UUID userId,
            @PathVariable UUID commentId,
            @Valid @RequestBody CommentRequest request) {
        CommentResponse response = commentService.updateComment(userId, commentId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @CurrentUser UUID userId,
            @PathVariable UUID commentId) {
        commentService.deleteComment(userId, commentId);
        return ResponseEntity.noContent().build();
    }
}