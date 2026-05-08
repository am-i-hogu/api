package com.hogu.am_i_hogu.domain.post.service;

import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.domain.post.domain.Post;
import com.hogu.am_i_hogu.domain.post.domain.PostBookmark;
import com.hogu.am_i_hogu.domain.post.exception.PostErrorCode;
import com.hogu.am_i_hogu.domain.post.repository.PostBookmarkRepository;
import com.hogu.am_i_hogu.domain.post.repository.PostRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PostBookmarkServiceTest {

    private final PostRepository postRepository = mock(PostRepository.class);
    private final PostBookmarkRepository postBookmarkRepository = mock(PostBookmarkRepository.class);
    private final PostBookmarkService postBookmarkService = new PostBookmarkService(
            postRepository,
            postBookmarkRepository
    );

    @Test
    void createBookmarkConvertsDuplicateKeyViolationToDuplicateRequest() {
        Long userId = 1L;
        Long postId = 1234L;
        Post post = Post.builder()
                .id(postId)
                .build();

        when(postRepository.findById(postId))
                .thenReturn(Optional.of(post));
        when(postBookmarkRepository.saveAndFlush(any(PostBookmark.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate bookmark"));

        assertThatThrownBy(() -> postBookmarkService.create(userId, postId))
                .isInstanceOfSatisfying(CustomException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(PostErrorCode.DUPLICATE_REQUEST));
    }
}
