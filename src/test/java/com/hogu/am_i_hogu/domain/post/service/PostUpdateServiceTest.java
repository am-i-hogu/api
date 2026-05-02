package com.hogu.am_i_hogu.domain.post.service;

import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.common.util.TsidGenerator;
import com.hogu.am_i_hogu.domain.post.dto.request.PostImageRequest;
import com.hogu.am_i_hogu.domain.post.dto.request.PostUpdateRequest;
import com.hogu.am_i_hogu.domain.post.exception.PostErrorCode;
import com.hogu.am_i_hogu.domain.post.repository.CategoryRepository;
import com.hogu.am_i_hogu.domain.post.repository.ImageAssetRepository;
import com.hogu.am_i_hogu.domain.post.repository.PostRepository;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PostUpdateServiceTest {

    private final TsidGenerator tsidGenerator = mock(TsidGenerator.class);
    private final CategoryRepository categoryRepository = mock(CategoryRepository.class);
    private final PostRepository postRepository = mock(PostRepository.class);
    private final ImageAssetRepository imageAssetRepository = mock(ImageAssetRepository.class);
    private final PostUpdateService postUpdateService = new PostUpdateService(
            tsidGenerator,
            categoryRepository,
            postRepository,
            imageAssetRepository
    );

    // 실패 케이스: categories 배열 안에 null이 들어오면 DB 조회 전에 INVALID_CATEGORIES로 검증 실패한다.
    @Test
    void updateRejectsNullCategoryCode() {
        when(categoryRepository.existsById(null))
                .thenThrow(new IllegalArgumentException("category id must not be null"));

        PostUpdateRequest request = new PostUpdateRequest(
                null,
                Collections.singletonList(null),
                null,
                null
        );

        assertThatThrownBy(() -> postUpdateService.update(1L, 1L, request))
                .isInstanceOfSatisfying(CustomException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(PostErrorCode.INVALID_INPUT_VALUE);
                    assertThat(exception.getErrors()).hasSize(1);
                    assertThat(exception.getErrors().get(0).getField()).isEqualTo("categories");
                    assertThat(exception.getErrors().get(0).getCode()).isEqualTo("INVALID_CATEGORIES");
                });

        verifyNoInteractions(postRepository);
        verifyNoInteractions(imageAssetRepository);
    }

    // 실패 케이스: images 배열 안에 null이 들어오면 NullPointerException이 아니라 필드 오류로 검증 실패한다.
    @Test
    void updateRejectsNullImageItem() {
        PostUpdateRequest request = new PostUpdateRequest(
                null,
                null,
                null,
                Collections.singletonList(null)
        );

        assertThatThrownBy(() -> postUpdateService.update(1L, 1L, request))
                .isInstanceOfSatisfying(CustomException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(PostErrorCode.INVALID_INPUT_VALUE);
                    assertThat(exception.getErrors()).hasSize(2);
                    assertThat(exception.getErrors().get(0).getField()).isEqualTo("images");
                    assertThat(exception.getErrors().get(0).getCode()).isEqualTo("EMPTY_IMAGE_URL");
                    assertThat(exception.getErrors().get(1).getField()).isEqualTo("images");
                    assertThat(exception.getErrors().get(1).getCode()).isEqualTo("EMPTY_THUMBNAIL");
                });

        verifyNoInteractions(categoryRepository);
        verifyNoInteractions(postRepository);
        verifyNoInteractions(imageAssetRepository);
    }

    // 실패 케이스: 이미지 order가 null이면 DB 저장 전에 필드 오류로 검증 실패한다.
    @Test
    void updateRejectsNullImageOrder() {
        PostUpdateRequest request = new PostUpdateRequest(
                null,
                null,
                null,
                List.of(new PostImageRequest(
                        "https://example.com/post-image.jpg",
                        null,
                        true
                ))
        );

        assertThatThrownBy(() -> postUpdateService.update(1L, 1L, request))
                .isInstanceOfSatisfying(CustomException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(PostErrorCode.INVALID_INPUT_VALUE);
                    assertThat(exception.getErrors()).hasSize(1);
                    assertThat(exception.getErrors().get(0).getField()).isEqualTo("images");
                    assertThat(exception.getErrors().get(0).getCode()).isEqualTo("EMPTY_IMAGE_ORDER");
                });

        verifyNoInteractions(categoryRepository);
        verifyNoInteractions(postRepository);
        verifyNoInteractions(imageAssetRepository);
    }
}
