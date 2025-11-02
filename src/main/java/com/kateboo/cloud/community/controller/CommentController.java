package com.kateboo.cloud.community.controller;

import com.kateboo.cloud.community.dto.request.CommentRequest;
import com.kateboo.cloud.community.dto.response.CommentResponse;
import com.kateboo.cloud.community.dto.response.PageResponse;
import com.kateboo.cloud.community.service.CommentService;
import com.kateboo.cloud.community.util.SessionUtil;
import jakarta.servlet.http.HttpServletRequest;
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

    /**
     * 댓글 목록 조회
     */
    @GetMapping("/post/{postId}")
    public ResponseEntity<PageResponse<CommentResponse>> getComments(
            @PathVariable UUID postId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {
        PageResponse<CommentResponse> response = commentService.getComments(postId, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * 댓글 작성(세션인증설정)
     */
    @PostMapping("/post/{postId}")
    public ResponseEntity<CommentResponse> createComment(
            HttpServletRequest request,
            @PathVariable UUID postId,
            @Valid @RequestBody CommentRequest commentRequest) {
        UUID userId = SessionUtil.getUserIdFromSession(request);
        CommentResponse response = commentService.createComment(userId, postId, commentRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 댓글 수정(세션인증설정)
     */
    @PutMapping("/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(
            HttpServletRequest request,
            @PathVariable UUID commentId,
            @Valid @RequestBody CommentRequest commentRequest) {
        UUID userId = SessionUtil.getUserIdFromSession(request);
        CommentResponse response = commentService.updateComment(userId, commentId, commentRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * 댓글 삭제(세션인증설정)
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            HttpServletRequest request,
            @PathVariable UUID commentId) {
        UUID userId = SessionUtil.getUserIdFromSession(request);
        commentService.deleteComment(userId, commentId);
        return ResponseEntity.noContent().build();
    }
}