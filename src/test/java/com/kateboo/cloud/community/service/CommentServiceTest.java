package com.kateboo.cloud.community.service;

import com.kateboo.cloud.community.dto.request.CommentRequest;
import com.kateboo.cloud.community.dto.response.CommentResponse;
import com.kateboo.cloud.community.dto.response.PageResponse;
import com.kateboo.cloud.community.entity.Comment;
import com.kateboo.cloud.community.entity.Post;
import com.kateboo.cloud.community.entity.PostStats;
import com.kateboo.cloud.community.entity.User;
import com.kateboo.cloud.community.repository.CommentRepository;
import com.kateboo.cloud.community.repository.PostRepository;
import com.kateboo.cloud.community.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CommentServiceTest {

    @InjectMocks
    private CommentService commentService;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private User makeDummyUser(UUID userId) {
        return User.builder()
                .userId(userId)
                .email("user@example.com")
                .nickname("nickname")
                .build();
    }

    private Post makeDummyPost(UUID postId) {
        PostStats stats = PostStats.builder()
                .postId(postId)
                .commentCount(0)
                .build();

        Post post = Post.builder()
                .postId(postId)
                .postStats(stats)
                .build();

        stats.setPost(post);
        return post;
    }

    private Comment makeDummyComment(UUID commentId, User user, Post post) {
        return Comment.builder()
                .commentId(commentId)
                .body("comment body")
                .user(user)
                .post(post)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("getComments()")
    class GetCommentsTest {
        @Test
        void 댓글_목록_조회_성공() {
            UUID postId = UUID.randomUUID();
            Pageable pageable = PageRequest.of(0, 10);

            User user = makeDummyUser(UUID.randomUUID());
            Post post = makeDummyPost(postId);
            Comment comment = makeDummyComment(UUID.randomUUID(), user, post);

            Page<Comment> commentPage = new PageImpl<>(List.of(comment), pageable, 1);

            when(commentRepository.findByPost_PostId(postId, pageable)).thenReturn(commentPage);

            PageResponse<CommentResponse> response = commentService.getComments(postId, pageable);

            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getContent().get(0).getCommentId()).isEqualTo(comment.getCommentId());
            verify(commentRepository).findByPost_PostId(postId, pageable);
        }
    }

    @Nested
    @DisplayName("createComment()")
    class CreateCommentTest {
        @Test
        void 댓글_작성_성공() {
            UUID userId = UUID.randomUUID();
            UUID postId = UUID.randomUUID();

            User user = makeDummyUser(userId);
            Post post = makeDummyPost(postId);
            CommentRequest request = new CommentRequest("new comment");

            Comment savedComment = makeDummyComment(UUID.randomUUID(), user, post);

            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);

            CommentResponse response = commentService.createComment(userId, postId, request);

            assertThat(response.getCommentId()).isEqualTo(savedComment.getCommentId());
            assertThat(post.getPostStats().getCommentCount()).isEqualTo(1);
            verify(commentRepository).save(any(Comment.class));
        }

        @Test
        void 게시글_없음_예외() {
            UUID userId = UUID.randomUUID();
            UUID postId = UUID.randomUUID();
            CommentRequest request = new CommentRequest("new comment");

            when(postRepository.findById(postId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.createComment(userId, postId, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("게시글을 찾을 수 없습니다");
        }

        @Test
        void 사용자_없음_예외() {
            UUID userId = UUID.randomUUID();
            UUID postId = UUID.randomUUID();
            CommentRequest request = new CommentRequest("new comment");
            Post post = makeDummyPost(postId);

            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.createComment(userId, postId, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("사용자를 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("updateComment()")
    class UpdateCommentTest {
        @Test
        void 댓글_수정_성공() {
            UUID userId = UUID.randomUUID();
            UUID commentId = UUID.randomUUID();

            User user = makeDummyUser(userId);
            Post post = makeDummyPost(UUID.randomUUID());
            Comment comment = makeDummyComment(commentId, user, post);

            CommentRequest request = new CommentRequest("updated comment");

            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

            CommentResponse response = commentService.updateComment(userId, commentId, request);

            assertThat(response.getBody()).isEqualTo("updated comment");
        }

        @Test
        void 댓글_없음_예외() {
            UUID userId = UUID.randomUUID();
            UUID commentId = UUID.randomUUID();
            CommentRequest request = new CommentRequest("updated comment");

            when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.updateComment(userId, commentId, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("댓글을 찾을 수 없습니다");
        }

        @Test
        void 본인_댓글_아님_예외() {
            UUID userId = UUID.randomUUID();
            UUID commentId = UUID.randomUUID();

            User owner = makeDummyUser(UUID.randomUUID());
            Post post = makeDummyPost(UUID.randomUUID());
            Comment comment = makeDummyComment(commentId, owner, post);

            CommentRequest request = new CommentRequest("updated comment");

            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

            assertThatThrownBy(() -> commentService.updateComment(userId, commentId, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("본인의 댓글만 수정할 수 있습니다");
        }
    }

    @Nested
    @DisplayName("deleteComment()")
    class DeleteCommentTest {
        @Test
        void 댓글_삭제_성공() {
            UUID userId = UUID.randomUUID();
            UUID commentId = UUID.randomUUID();

            User user = makeDummyUser(userId);
            Post post = makeDummyPost(UUID.randomUUID());
            post.getPostStats().setCommentCount(1);

            Comment comment = makeDummyComment(commentId, user, post);

            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

            commentService.deleteComment(userId, commentId);

            verify(commentRepository).delete(comment);
            assertThat(post.getPostStats().getCommentCount()).isZero();
        }

        @Test
        void 댓글_없음_예외() {
            UUID userId = UUID.randomUUID();
            UUID commentId = UUID.randomUUID();

            when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.deleteComment(userId, commentId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("댓글을 찾을 수 없습니다");
        }

        @Test
        void 본인_댓글_아님_예외() {
            UUID userId = UUID.randomUUID();
            UUID commentId = UUID.randomUUID();

            User owner = makeDummyUser(UUID.randomUUID());
            Post post = makeDummyPost(UUID.randomUUID());
            Comment comment = makeDummyComment(commentId, owner, post);

            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

            assertThatThrownBy(() -> commentService.deleteComment(userId, commentId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("본인의 댓글만 삭제할 수 있습니다");
        }
    }
}
