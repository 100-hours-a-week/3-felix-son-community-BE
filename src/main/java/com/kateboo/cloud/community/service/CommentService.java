package com.kateboo.cloud.community.service;

import com.kateboo.cloud.community.dto.request.CommentRequest;
import com.kateboo.cloud.community.dto.response.CommentResponse;
import com.kateboo.cloud.community.dto.response.PageResponse;
import com.kateboo.cloud.community.entity.Comment;
import com.kateboo.cloud.community.entity.Post;
import com.kateboo.cloud.community.entity.User;
import com.kateboo.cloud.community.exception.NotFoundException;
import com.kateboo.cloud.community.exception.ForbiddenException;
import com.kateboo.cloud.community.repository.CommentRepository;
import com.kateboo.cloud.community.repository.PostRepository;
import com.kateboo.cloud.community.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public PageResponse<CommentResponse> getComments(UUID postId, Pageable pageable) {
        Page<Comment> comments = commentRepository.findByPost_PostId(postId, pageable);
        return PageResponse.of(comments, CommentResponse::from);
    }

    @Transactional
    public CommentResponse createComment(UUID userId, UUID postId, CommentRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("게시글을 찾을 수 없습니다"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다"));

        Comment comment = Comment.builder()
                .body(request.getBody())
                .post(post)
                .user(user)
                .build();

        Comment savedComment = commentRepository.save(comment);

        post.getPostStats().incrementCommentCount();

        log.info("댓글 작성 완료: commentId={}, postId={}, userId={}",
                savedComment.getCommentId(), postId, userId);

        return CommentResponse.from(savedComment);
    }

    @Transactional
    public CommentResponse updateComment(UUID userId, UUID commentId, CommentRequest request) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("댓글을 찾을 수 없습니다"));

        if (!comment.getUser().getUserId().equals(userId)) {
            throw new ForbiddenException("본인의 댓글만 수정할 수 있습니다");
        }

        comment.setBody(request.getBody());
        comment.setUpdatedAt(Instant.now());

        log.info("댓글 수정 완료: commentId={}, userId={}", commentId, userId);

        return CommentResponse.from(comment);
    }

    @Transactional
    public void deleteComment(UUID userId, UUID commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("댓글을 찾을 수 없습니다"));

        if (!comment.getUser().getUserId().equals(userId)) {
            throw new ForbiddenException("본인의 댓글만 삭제할 수 있습니다");
        }

        Post post = comment.getPost();
        commentRepository.delete(comment);

        post.getPostStats().decrementCommentCount();

        log.info("댓글 삭제 완료: commentId={}, postId={}, userId={}",
                commentId, post.getPostId(), userId);
    }
}
