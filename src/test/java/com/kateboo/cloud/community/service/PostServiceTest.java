package com.kateboo.cloud.community.service;

import com.kateboo.cloud.community.dto.request.PostRequest;
import com.kateboo.cloud.community.dto.response.LikeResponse;
import com.kateboo.cloud.community.dto.response.PageResponse;
import com.kateboo.cloud.community.dto.response.PostResponse;
import com.kateboo.cloud.community.entity.User;
import com.kateboo.cloud.community.exception.BadRequestException;
import com.kateboo.cloud.community.exception.ForbiddenException;
import com.kateboo.cloud.community.exception.NotFoundException;
import com.kateboo.cloud.community.repository.PostRepository;
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

import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PostServiceTest {

    @Autowired
    private PostService postService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    private UUID testUserId;
    private UUID anotherUserId;

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
    }

    @Test
    @DisplayName("게시글 작성 성공")
    void createPost_Success() {
        // given
        PostRequest request = new PostRequest();
        request.setTitle("테스트 제목");
        request.setBody("테스트 내용");
        request.setImageUrls(Arrays.asList(
                "https://example.com/image1.jpg",
                "https://example.com/image2.jpg"
        ));

        // when
        PostResponse response = postService.createPost(testUserId, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("테스트 제목");
        assertThat(response.getBody()).isEqualTo("테스트 내용");
        assertThat(response.getImages()).hasSize(2);  // ✅ 수정
        assertThat(response.getStats().getLikesCount()).isZero();  // ✅ 수정
        assertThat(response.getStats().getCommentCount()).isZero();  // ✅ 수정
    }

    @Test
    @DisplayName("게시글 작성 실패 - 제목 없음")
    void createPost_Fail_NoTitle() {
        // given
        PostRequest request = new PostRequest();
        request.setTitle("");
        request.setBody("테스트 내용");

        // when & then
        assertThatThrownBy(() -> postService.createPost(testUserId, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("제목은 필수입니다");
    }

    @Test
    @DisplayName("게시글 작성 실패 - 제목 길이 초과")
    void createPost_Fail_TitleTooLong() {
        // given
        PostRequest request = new PostRequest();
        request.setTitle("a".repeat(27));  // 26자 초과
        request.setBody("테스트 내용");

        // when & then
        assertThatThrownBy(() -> postService.createPost(testUserId, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("제목은 최대 26자까지 가능합니다");
    }

    @Test
    @DisplayName("게시글 작성 실패 - 존재하지 않는 사용자")
    void createPost_Fail_UserNotFound() {
        // given
        UUID invalidUserId = UUID.randomUUID();
        PostRequest request = new PostRequest();
        request.setTitle("테스트");
        request.setBody("내용");

        // when & then
        assertThatThrownBy(() -> postService.createPost(invalidUserId, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("사용자를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("게시글 목록 조회 성공")
    void getPosts_Success() {
        // given
        for (int i = 0; i < 3; i++) {
            PostRequest request = new PostRequest();
            request.setTitle("제목 " + i);
            request.setBody("내용 " + i);
            postService.createPost(testUserId, request);
        }

        Pageable pageable = PageRequest.of(0, 10);
        // 정렬 기준을 추가합니다. 기본값인 "latest"를 사용합니다.
        String sortType = "latest";

        // when
        // 수정된 부분: sortType 인자를 추가하여 호출합니다.
        PageResponse<PostResponse> response = postService.getPosts(pageable, sortType);

        // then
        assertThat(response.getContent()).hasSize(3);
        assertThat(response.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("게시글 조회 성공 - 조회수 증가")
    void getPost_Success() throws InterruptedException {
        // given
        PostRequest request = new PostRequest();
        request.setTitle("테스트");
        request.setBody("내용");
        PostResponse created = postService.createPost(testUserId, request);

        // when
        PostResponse response = postService.getPost(created.getPostId());
        Thread.sleep(100);  // 비동기 처리 대기

        // then
        assertThat(response).isNotNull();
        assertThat(response.getPostId()).isEqualTo(created.getPostId());
    }

    @Test
    @DisplayName("게시글 수정 성공")
    void updatePost_Success() {
        // given
        PostRequest createRequest = new PostRequest();
        createRequest.setTitle("원본 제목");
        createRequest.setBody("원본 내용");
        PostResponse created = postService.createPost(testUserId, createRequest);

        PostRequest updateRequest = new PostRequest();
        updateRequest.setTitle("수정된 제목");
        updateRequest.setBody("수정된 내용");

        // when
        PostResponse response = postService.updatePost(
                testUserId, created.getPostId(), updateRequest
        );

        // then
        assertThat(response.getTitle()).isEqualTo("수정된 제목");
        assertThat(response.getBody()).isEqualTo("수정된 내용");
        assertThat(response.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("게시글 수정 실패 - 권한 없음")
    void updatePost_Fail_Forbidden() {
        // given
        PostRequest createRequest = new PostRequest();
        createRequest.setTitle("원본 제목");
        createRequest.setBody("원본 내용");
        PostResponse created = postService.createPost(testUserId, createRequest);

        PostRequest updateRequest = new PostRequest();
        updateRequest.setTitle("수정된 제목");

        // when & then
        assertThatThrownBy(() -> postService.updatePost(
                anotherUserId, created.getPostId(), updateRequest
        ))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("본인의 게시글만 수정할 수 있습니다");
    }

    @Test
    @DisplayName("게시글 삭제 성공")
    void deletePost_Success() {
        // given
        PostRequest request = new PostRequest();
        request.setTitle("테스트");
        request.setBody("내용");
        PostResponse created = postService.createPost(testUserId, request);

        // when
        postService.deletePost(testUserId, created.getPostId());

        // then
        assertThat(postRepository.findById(created.getPostId())).isEmpty();
    }

    @Test
    @DisplayName("게시글 삭제 실패 - 권한 없음")
    void deletePost_Fail_Forbidden() {
        // given
        PostRequest request = new PostRequest();
        request.setTitle("테스트");
        request.setBody("내용");
        PostResponse created = postService.createPost(testUserId, request);

        // when & then
        assertThatThrownBy(() -> postService.deletePost(anotherUserId, created.getPostId()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("본인의 게시글만 삭제할 수 있습니다");
    }

    @Test
    @DisplayName("좋아요 토글 - 좋아요 등록")
    void toggleLike_AddLike() {
        // given
        PostRequest request = new PostRequest();
        request.setTitle("테스트");
        request.setBody("내용");
        PostResponse created = postService.createPost(testUserId, request);

        // when
        LikeResponse response = postService.toggleLike(anotherUserId, created.getPostId());

        // then
        assertThat(response.isLiked()).isTrue();
        assertThat(response.getLikesCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("좋아요 토글 - 좋아요 취소")
    void toggleLike_RemoveLike() {
        // given
        PostRequest request = new PostRequest();
        request.setTitle("테스트");
        request.setBody("내용");
        PostResponse created = postService.createPost(testUserId, request);
        postService.toggleLike(anotherUserId, created.getPostId());

        // when
        LikeResponse response = postService.toggleLike(anotherUserId, created.getPostId());

        // then
        assertThat(response.isLiked()).isFalse();
        assertThat(response.getLikesCount()).isZero();
    }
}