package com.kateboo.cloud.community.repository;

import com.kateboo.cloud.community.entity.Post;
import com.kateboo.cloud.community.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DataJpaTest
public class PostRepositoryTest {
    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("게시글 저장 테스트")
    void savePost(){
        // given
        User user = User.builder().nickname("testedUser").build();
        userRepository.save(user);

        Post post = Post.builder().title("테스트 제목").body("테스트 내용").build();

        // when
        Post savedPost = postRepository.save(post);

        // then
        assertThat(savedPost.getPostId()).isNotNull();
        assertThat(savedPost.getTitle()).isEqualTo("테스트 제목");

    }

}
