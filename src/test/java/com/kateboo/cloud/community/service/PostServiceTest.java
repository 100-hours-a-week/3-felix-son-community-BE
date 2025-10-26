package com.kateboo.cloud.community.service;

import com.kateboo.cloud.community.dto.request.PostRequest;
import com.kateboo.cloud.community.dto.response.PostResponse;
import com.kateboo.cloud.community.entity.Post;
import com.kateboo.cloud.community.entity.PostImage;
import com.kateboo.cloud.community.entity.PostStats;
import com.kateboo.cloud.community.entity.User;
import com.kateboo.cloud.community.repository.PostLikeRepository;
import com.kateboo.cloud.community.repository.PostRepository;
import com.kateboo.cloud.community.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostService 게시글 생성 테스트")
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostLikeRepository postLikeRepository;

    @InjectMocks
    private PostService postService;

    @Test
    @DisplayName("게시글 생성 - 이미지 없음")
    void createPost_WithoutImages_Success() {
        // given
        UUID userId = UUID.randomUUID();
        String title = "테스트 제목";
        String body = "테스트 본문";

        PostRequest request = PostRequest.builder()
                .title(title)
                .body(body)
                .build();

        User mockUser = User.builder()
                .userId(userId)
                .nickname("testuser")
                .build();

        Post savedPost = Post.builder()
                .postId(UUID.randomUUID())
                .title(title)
                .body(body)
                .user(mockUser)
                .build();

        PostStats postStats = PostStats.builder()
                .post(savedPost)
                .likesCount(0)
                .viewsCount(0L)
                .commentCount(0)
                .build();
        savedPost.setPostStats(postStats);

        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(postRepository.save(any())).willReturn(savedPost);

        // when
        PostResponse response = postService.createPost(userId, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo(title);
        assertThat(response.getBody()).isEqualTo(body);

        verify(userRepository, times(1)).findById(userId);
        verify(postRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("게시글 생성 - 이미지 포함")
    void createPost_WithImages_Success() {
        // given
        UUID userId = UUID.randomUUID();
        String title = "이미지 포함 게시글";
        String body = "이미지가 포함된 게시글입니다";
        List<String> imageUrls = Arrays.asList(
                "https://example.com/image1.jpg",
                "https://example.com/image2.jpg"
        );

        PostRequest request = PostRequest.builder()
                .title(title)
                .body(body)
                .imageUrls(imageUrls)
                .build();

        User mockUser = User.builder()
                .userId(userId)
                .nickname("testuser")
                .build();

        ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);

        Post savedPost = Post.builder()
                .postId(UUID.randomUUID())
                .title(title)
                .body(body)
                .user(mockUser)
                .build();

        PostStats postStats = PostStats.builder()
                .post(savedPost)
                .likesCount(0)
                .viewsCount(0L)
                .commentCount(0)
                .build();
        savedPost.setPostStats(postStats);

        // 이미지 추가
        for (int i = 0; i < imageUrls.size(); i++) {
            PostImage image = PostImage.builder()
                    .imageUrl(imageUrls.get(i))
                    .orderNo(i)
                    .post(savedPost)
                    .build();
            savedPost.getPostImages().add(image);
        }

        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(postRepository.save(any())).willReturn(savedPost);

        // when
        PostResponse response = postService.createPost(userId, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo(title);
        assertThat(response.getBody()).isEqualTo(body);

        verify(userRepository, times(1)).findById(userId);
        verify(postRepository, times(1)).save(postCaptor.capture());

        Post capturedPost = postCaptor.getValue();
        assertThat(capturedPost.getPostImages()).hasSize(2);
        assertThat(capturedPost.getPostImages().get(0).getImageUrl()).isEqualTo(imageUrls.get(0));
        assertThat(capturedPost.getPostImages().get(0).getOrderNo()).isEqualTo(0);
        assertThat(capturedPost.getPostImages().get(1).getImageUrl()).isEqualTo(imageUrls.get(1));
        assertThat(capturedPost.getPostImages().get(1).getOrderNo()).isEqualTo(1);
    }

    @Test
    @DisplayName("게시글 생성 실패 - 사용자를 찾을 수 없음")
    void createPost_UserNotFound_ThrowsException() {
        // given
        UUID userId = UUID.randomUUID();
        PostRequest request = PostRequest.builder()
                .title("테스트 제목")
                .body("테스트 본문")
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postService.createPost(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용자를 찾을 수 없습니다");

        verify(userRepository, times(1)).findById(userId);
        verify(postRepository, times(0)).save(any());
    }

    @Test
    @DisplayName("게시글 생성 - PostStats 자동 생성 확인")
    void createPost_PostStatsAutoCreated_Success() {
        // given
        UUID userId = UUID.randomUUID();
        PostRequest request = PostRequest.builder()
                .title("통계 확인 게시글")
                .body("PostStats가 자동으로 생성되는지 확인")
                .build();

        User mockUser = User.builder()
                .userId(userId)
                .nickname("testuser")
                .build();

        ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);

        Post savedPost = Post.builder()
                .postId(UUID.randomUUID())
                .title(request.getTitle())
                .body(request.getBody())
                .user(mockUser)
                .build();

        PostStats postStats = PostStats.builder()
                .post(savedPost)
                .likesCount(0)
                .viewsCount(0L)
                .commentCount(0)
                .build();
        savedPost.setPostStats(postStats);

        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(postRepository.save(any())).willReturn(savedPost);

        // when
        PostResponse response = postService.createPost(userId, request);

        // then
        verify(postRepository, times(1)).save(postCaptor.capture());

        Post capturedPost = postCaptor.getValue();
        assertThat(capturedPost.getPostStats()).isNotNull();
        assertThat(capturedPost.getPostStats().getLikesCount()).isEqualTo(0);
        assertThat(capturedPost.getPostStats().getViewsCount()).isEqualTo(0L);
        assertThat(capturedPost.getPostStats().getCommentCount()).isEqualTo(0);
    }
}