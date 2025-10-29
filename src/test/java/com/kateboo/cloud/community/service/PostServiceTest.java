package com.kateboo.cloud.community.service;

import com.kateboo.cloud.community.dto.request.PostRequest;
import com.kateboo.cloud.community.dto.response.LikeResponse;
import com.kateboo.cloud.community.dto.response.PageResponse;
import com.kateboo.cloud.community.dto.response.PostResponse;
import com.kateboo.cloud.community.entity.*;
import com.kateboo.cloud.community.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PostServiceTest {

    @InjectMocks
    private PostService postService;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostLikeRepository postLikeRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // 테스트용 Post + PostStats 생성 helper
    private Post makeDummyPost(UUID postId, User user) {
        Post post = Post.builder()
                .postId(postId)
                .title("테스트 제목")
                .user(user)
                .postImages(new ArrayList<>())
                .build();

        PostStats stats = PostStats.builder()
                .postId(postId)
                .likesCount(0)
                .viewsCount(0L)
                .commentCount(0)
                .post(post)
                .build();

        post.setPostStats(stats);
        return post;
    }

    @Nested
    @DisplayName("getPosts()")
    class GetPostsTest {
        @Test
        void 게시글_목록_조회_정상() {
            Pageable pageable = PageRequest.of(0, 10);

            User dummyUser = User.builder().userId(UUID.randomUUID()).build();
            Post dummyPost = makeDummyPost(UUID.randomUUID(), dummyUser);

            List<Post> postList = List.of(dummyPost);
            Page<Post> page = new PageImpl<>(postList, pageable, 1);

            when(postRepository.findAll(pageable)).thenReturn(page);

            PageResponse<PostResponse> result = postService.getPosts(pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo(dummyPost.getTitle());
            verify(postRepository, times(1)).findAll(pageable);
        }
    }

    @Nested
    @DisplayName("getPost()")
    class GetPostTest {
        @Test
        void 게시글_상세_조회_정상() {
            UUID postId = UUID.randomUUID();
            User user = User.builder().userId(UUID.randomUUID()).build();
            Post dummyPost = makeDummyPost(postId, user);

            when(postRepository.findById(postId)).thenReturn(Optional.of(dummyPost));

            PostResponse result = postService.getPost(postId);

            assertThat(result.getPostId()).isEqualTo(postId);
            assertThat(dummyPost.getPostStats().getViewsCount()).isEqualTo(1);
            verify(postRepository, times(1)).findById(postId);
        }

        @Test
        void 게시글_상세_조회_없는경우_예외() {
            UUID postId = UUID.randomUUID();
            when(postRepository.findById(postId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> postService.getPost(postId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("게시글을 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("createPost()")
    class CreatePostTest {
        @Test
        void 게시글_작성_정상() {
            UUID userId = UUID.randomUUID();
            User dummyUser = User.builder().userId(userId).build();
            PostRequest request = PostRequest.builder()
                    .title("제목")
                    .body("본문")
                    .imageUrls(List.of("img1", "img2"))
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(dummyUser));
            when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

            PostResponse response = postService.createPost(userId, request);

            assertThat(response.getTitle()).isEqualTo("제목");
            assertThat(response.getImages()).hasSize(2);
            verify(userRepository).findById(userId);
            verify(postRepository).save(any(Post.class));
        }

        @Test
        void 게시글_작성_사용자없음_예외() {
            UUID userId = UUID.randomUUID();
            PostRequest request = PostRequest.builder().title("제목").body("본문").build();

            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> postService.createPost(userId, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("사용자를 찾을 수 없습니다");
            verify(postRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updatePost()")
    class UpdatePostTest {
        @Test
        void 게시글_수정_정상() {
            UUID userId = UUID.randomUUID();
            UUID postId = UUID.randomUUID();

            User user = User.builder().userId(userId).build();
            Post post = makeDummyPost(postId, user);

            PostRequest request = PostRequest.builder()
                    .title("수정제목")
                    .body("수정본문")
                    .imageUrls(List.of("img3"))
                    .build();

            when(postRepository.findById(postId)).thenReturn(Optional.of(post));

            PostResponse response = postService.updatePost(userId, postId, request);

            assertThat(response.getTitle()).isEqualTo("수정제목");
            assertThat(response.getImages()).hasSize(1);
        }

        @Test
        void 게시글_수정_본인글아님_예외() {
            UUID postId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            User writer = User.builder().userId(UUID.randomUUID()).build();
            Post post = makeDummyPost(postId, writer);

            PostRequest request = PostRequest.builder().title("제목").body("본문").build();

            when(postRepository.findById(postId)).thenReturn(Optional.of(post));

            assertThatThrownBy(() -> postService.updatePost(userId, postId, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("본인의 게시글만 수정할 수 있습니다");
        }

        @Test
        void 게시글_수정_게시글없음_예외() {
            UUID postId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            PostRequest request = PostRequest.builder().title("제목").body("본문").build();

            when(postRepository.findById(postId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> postService.updatePost(userId, postId, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("게시글을 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("deletePost()")
    class DeletePostTest {
        @Test
        void 게시글_삭제_정상() {
            UUID userId = UUID.randomUUID();
            UUID postId = UUID.randomUUID();

            User user = User.builder().userId(userId).build();
            Post post = makeDummyPost(postId, user);

            when(postRepository.findById(postId)).thenReturn(Optional.of(post));

            postService.deletePost(userId, postId);

            verify(postRepository).delete(post);
        }

        @Test
        void 게시글_삭제_본인글아님_예외() {
            UUID postId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            User writer = User.builder().userId(UUID.randomUUID()).build();
            Post post = makeDummyPost(postId, writer);

            when(postRepository.findById(postId)).thenReturn(Optional.of(post));

            assertThatThrownBy(() -> postService.deletePost(userId, postId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("본인의 게시글만 삭제할 수 있습니다");
        }

        @Test
        void 게시글_삭제_게시글없음_예외() {
            UUID postId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            when(postRepository.findById(postId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> postService.deletePost(userId, postId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("게시글을 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("toggleLike()")
    class ToggleLikeTest {
        @Test
        void 좋아요_추가_정상() {
            UUID userId = UUID.randomUUID();
            UUID postId = UUID.randomUUID();

            User user = User.builder().userId(userId).build();
            Post post = makeDummyPost(postId, user);

            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(postLikeRepository.findByPost_PostIdAndUser_UserId(postId, userId)).thenReturn(Optional.empty());
            when(postLikeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            LikeResponse response = postService.toggleLike(userId, postId);

            assertThat(response.isLiked()).isTrue();
            assertThat(response.getLikesCount()).isEqualTo(1);
        }

        @Test
        void 좋아요_취소_정상() {
            UUID userId = UUID.randomUUID();
            UUID postId = UUID.randomUUID();

            User user = User.builder().userId(userId).build();
            PostStats stats = PostStats.builder().postId(postId).likesCount(1).viewsCount(0L).commentCount(0).post(null).build();
            Post post = Post.builder().postId(postId).user(user).postStats(stats).build();

            PostLike postLike = PostLike.builder().likeId(1L).user(user).post(post).build();

            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(postLikeRepository.findByPost_PostIdAndUser_UserId(postId, userId)).thenReturn(Optional.of(postLike));

            LikeResponse response = postService.toggleLike(userId, postId);

            assertThat(response.isLiked()).isFalse();
            assertThat(response.getLikesCount()).isEqualTo(0);
            verify(postLikeRepository).delete(postLike);
        }

        @Test
        void 좋아요_토글_게시글없음_예외() {
            UUID userId = UUID.randomUUID();
            UUID postId = UUID.randomUUID();

            when(postRepository.findById(postId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> postService.toggleLike(userId, postId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("게시글을 찾을 수 없습니다");
        }

        @Test
        void 좋아요_토글_사용자없음_예외() {
            UUID userId = UUID.randomUUID();
            UUID postId = UUID.randomUUID();

            PostStats stats = PostStats.builder().postId(postId).likesCount(0).viewsCount(0L).commentCount(0).build();
            Post post = Post.builder().postId(postId).postStats(stats).build();

            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> postService.toggleLike(userId, postId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("사용자를 찾을 수 없습니다");
        }
    }
}
