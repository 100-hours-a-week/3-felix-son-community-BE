package com.kateboo.cloud.community.service;

import com.kateboo.cloud.community.dto.request.CommentRequest;
import com.kateboo.cloud.community.dto.request.PostRequest;
import com.kateboo.cloud.community.dto.response.CommentResponse;
import com.kateboo.cloud.community.dto.response.PageResponse;
import com.kateboo.cloud.community.dto.response.PostResponse;
import com.kateboo.cloud.community.entity.User;
import com.kateboo.cloud.community.exception.ForbiddenException;
import com.kateboo.cloud.community.exception.NotFoundException;
import com.kateboo.cloud.community.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CommentServiceTest {

    @Autowired
    private CommentService commentService;

    @Autowired
    private PostService postService;

    @Autowired
    private UserRepository userRepository;

    private UUID testUserId;
    private UUID anotherUserId;
    private UUID testPostId;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        User user1 = User.builder()
                .email("test@example.com")
                .nickname("테스터")
                .passwordHash("encoded")
                .profileImageUrl("https://example.com/profile.jpg")
                .isActive(true)
                .build();
        testUserId = userRepository.save(user1).getUserId();

        User user2 = User.builder()
                .email("another@example.com")
                .nickname("다른사람")
                .passwordHash("encoded")
                .profileImageUrl("https://example.com/profile2.jpg")
                .isActive(true)
                .build();
        anotherUserId = userRepository.save(user2).getUserId();

        // 테스트 게시글 생성
        PostRequest postRequest = new PostRequest();
        postRequest.setTitle("테스트 게시글");
        postRequest.setBody("테스트 내용");
        PostResponse post = postService.createPost(testUserId, postRequest);
        testPostId = post.getPostId();
    }

    @Test
    @DisplayName("댓글 작성 성공")
    void createComment_Success() {
        // given
        CommentRequest request = new CommentRequest();
        request.setBody("테스트 댓글입니다");

        // when
        CommentResponse response = commentService.createComment(
                testUserId, testPostId, request
        );

        // then
        assertThat(response).isNotNull();
        assertThat(response.getBody()).isEqualTo("테스트 댓글입니다");
        assertThat(response.getUser().getUserId()).isEqualTo(testUserId);

        // 게시글 댓글 수 증가 확인
        PostResponse post = postService.getPost(testPostId);
        assertThat(post.getStats().getCommentCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("댓글 작성 실패 - 존재하지 않는 게시글")
    void createComment_Fail_PostNotFound() {
        // given
        UUID invalidPostId = UUID.randomUUID();
        CommentRequest request = new CommentRequest();
        request.setBody("테스트 댓글");

        // when & then
        assertThatThrownBy(() -> commentService.createComment(
                testUserId, invalidPostId, request
        ))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("게시글을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("댓글 목록 조회 성공")
    void getComments_Success() {
        // given
        for (int i = 0; i < 3; i++) {
            CommentRequest request = new CommentRequest();
            request.setBody("댓글 " + i);
            commentService.createComment(testUserId, testPostId, request);
        }

        Pageable pageable = PageRequest.of(0, 10);

        // when
        PageResponse<CommentResponse> response = commentService.getComments(
                testPostId, pageable
        );

        // then
        assertThat(response.getContent()).hasSize(3);
        assertThat(response.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("댓글 수정 성공")
    void updateComment_Success() {
        // given
        CommentRequest createRequest = new CommentRequest();
        createRequest.setBody("원본 댓글");
        CommentResponse created = commentService.createComment(
                testUserId, testPostId, createRequest
        );

        CommentRequest updateRequest = new CommentRequest();
        updateRequest.setBody("수정된 댓글");

        // when
        CommentResponse response = commentService.updateComment(
                testUserId, created.getCommentId(), updateRequest
        );

        // then
        assertThat(response.getBody()).isEqualTo("수정된 댓글");
        assertThat(response.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("댓글 수정 실패 - 권한 없음")
    void updateComment_Fail_Forbidden() {
        // given
        CommentRequest createRequest = new CommentRequest();
        createRequest.setBody("원본 댓글");
        CommentResponse created = commentService.createComment(
                testUserId, testPostId, createRequest
        );

        CommentRequest updateRequest = new CommentRequest();
        updateRequest.setBody("수정 시도");

        // when & then
        assertThatThrownBy(() -> commentService.updateComment(
                anotherUserId, created.getCommentId(), updateRequest
        ))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("본인의 댓글만 수정할 수 있습니다");
    }

    @Test
    @DisplayName("댓글 삭제 성공 - 게시글 댓글 수 감소")
    void deleteComment_Success() {
        // given
        CommentRequest request = new CommentRequest();
        request.setBody("테스트 댓글");
        CommentResponse created = commentService.createComment(
                testUserId, testPostId, request
        );

        // when
        commentService.deleteComment(testUserId, created.getCommentId());

        // then
        PostResponse post = postService.getPost(testPostId);
        assertThat(post.getStats().getCommentCount()).isZero();
    }

    @Test
    @DisplayName("댓글 삭제 실패 - 권한 없음")
    void deleteComment_Fail_Forbidden() {
        // given
        CommentRequest request = new CommentRequest();
        request.setBody("테스트 댓글");
        CommentResponse created = commentService.createComment(
                testUserId, testPostId, request
        );

        // when & then
        assertThatThrownBy(() -> commentService.deleteComment(
                anotherUserId, created.getCommentId()
        ))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("본인의 댓글만 삭제할 수 있습니다");
    }
}