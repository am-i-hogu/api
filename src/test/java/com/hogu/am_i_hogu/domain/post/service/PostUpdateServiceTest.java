package com.hogu.am_i_hogu.domain.post.service;

import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.domain.post.domain.Category;
import com.hogu.am_i_hogu.domain.post.domain.ImageAsset;
import com.hogu.am_i_hogu.domain.post.domain.Post;
import com.hogu.am_i_hogu.domain.post.dto.request.PostImageRequest;
import com.hogu.am_i_hogu.domain.post.dto.request.PostUpdateRequest;
import com.hogu.am_i_hogu.domain.post.exception.PostErrorCode;
import com.hogu.am_i_hogu.domain.post.repository.CategoryRepository;
import com.hogu.am_i_hogu.domain.post.repository.ImageAssetRepository;
import com.hogu.am_i_hogu.domain.post.repository.PostRepository;
import com.hogu.am_i_hogu.domain.user.domain.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PostUpdateServiceTest {

    private final CategoryRepository categoryRepository = mock(CategoryRepository.class);
    private final PostRepository postRepository = mock(PostRepository.class);
    private final ImageAssetRepository imageAssetRepository = mock(ImageAssetRepository.class);
    private final PostUpdateService postUpdateService = new PostUpdateService(
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

    // 정상 케이스: 게시물 수정 시 이미지 자산은 URL 순서로 잠금 조회하고 요청 순서대로 다시 연결한다.
    @Test
    void updateLocksUploadedImagesInStableOrderAndAttachesInRequestOrder() {
        LocalDateTime now = LocalDateTime.now();
        User writer = new User(1L, "hogu", false, now);
        Post post = Post.builder()
                .id(11L)
                .writer(writer)
                .category(mock(Category.class))
                .title("old title")
                .content("old content")
                .createdAt(now)
                .updatedAt(now)
                .build();
        ImageAsset firstByUrl = uploadedImage(101L, writer, "https://cdn.example.com/a.jpg", now);
        ImageAsset secondByUrl = uploadedImage(102L, writer, "https://cdn.example.com/b.jpg", now);

        when(postRepository.findById(11L)).thenReturn(Optional.of(post));
        when(imageAssetRepository.findAllWithLockByUrlInOrderByUrlAsc(List.of(
                "https://cdn.example.com/a.jpg",
                "https://cdn.example.com/b.jpg"
        ))).thenReturn(List.of(firstByUrl, secondByUrl));
        when(imageAssetRepository.findByPost_IdOrderBySortOrderAsc(11L)).thenReturn(List.of());

        PostUpdateRequest request = new PostUpdateRequest(
                null,
                null,
                null,
                List.of(
                        new PostImageRequest("https://cdn.example.com/b.jpg", 0, true),
                        new PostImageRequest("https://cdn.example.com/a.jpg", 1, false)
                )
        );

        postUpdateService.update(11L, 1L, request);

        verify(imageAssetRepository).findAllWithLockByUrlInOrderByUrlAsc(List.of(
                "https://cdn.example.com/a.jpg",
                "https://cdn.example.com/b.jpg"
        ));
        assertThat(secondByUrl.getPost()).isSameAs(post);
        assertThat(secondByUrl.isThumbnail()).isTrue();
        assertThat(secondByUrl.getSortOrder()).isZero();
        assertThat(firstByUrl.getPost()).isSameAs(post);
        assertThat(firstByUrl.isThumbnail()).isFalse();
        assertThat(firstByUrl.getSortOrder()).isEqualTo(1);
    }

    private ImageAsset uploadedImage(Long id, User uploader, String url, LocalDateTime now) {
        return ImageAsset.builder()
                .id(id)
                .uploadedByUser(uploader)
                .url(url)
                .contentType("image/jpeg")
                .sizeBytes(10L)
                .isThumbnail(false)
                .sortOrder(0)
                .createdAt(now)
                .build();
    }
}
