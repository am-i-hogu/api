package com.hogu.am_i_hogu.domain.post.service;

import com.hogu.am_i_hogu.common.util.TsidGenerator;
import com.hogu.am_i_hogu.domain.post.domain.Category;
import com.hogu.am_i_hogu.domain.post.domain.ImageAsset;
import com.hogu.am_i_hogu.domain.post.domain.Post;
import com.hogu.am_i_hogu.domain.post.dto.request.PostCreateRequest;
import com.hogu.am_i_hogu.domain.post.dto.request.PostImageRequest;
import com.hogu.am_i_hogu.domain.post.repository.CategoryRepository;
import com.hogu.am_i_hogu.domain.post.repository.ImageAssetRepository;
import com.hogu.am_i_hogu.domain.post.repository.PostRepository;
import com.hogu.am_i_hogu.domain.user.domain.User;
import com.hogu.am_i_hogu.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostCreateServiceTest {

    private final TsidGenerator tsidGenerator = mock(TsidGenerator.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final CategoryRepository categoryRepository = mock(CategoryRepository.class);
    private final PostRepository postRepository = mock(PostRepository.class);
    private final ImageAssetRepository imageAssetRepository = mock(ImageAssetRepository.class);
    private final PostCreateService postCreateService = new PostCreateService(
            tsidGenerator,
            userRepository,
            categoryRepository,
            postRepository,
            imageAssetRepository
    );

    // 정상 케이스: 게시물 생성 시 이미지 자산은 URL 순서로 잠금 조회하고 요청 순서대로 게시물에 연결한다.
    @Test
    void createLocksUploadedImagesInStableOrderAndAttachesInRequestOrder() {
        LocalDateTime now = LocalDateTime.now();
        User writer = new User(1L, "hogu", false, now);
        Category category = mock(Category.class);
        ImageAsset firstByUrl = uploadedImage(101L, writer, "https://cdn.example.com/a.jpg", now);
        ImageAsset secondByUrl = uploadedImage(102L, writer, "https://cdn.example.com/b.jpg", now);

        when(categoryRepository.existsById("USED_TRADE")).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(writer));
        when(categoryRepository.findById("USED_TRADE")).thenReturn(Optional.of(category));
        when(tsidGenerator.nextId()).thenReturn(11L);
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(imageAssetRepository.findAllWithLockByUrlInOrderByUrlAsc(List.of(
                "https://cdn.example.com/a.jpg",
                "https://cdn.example.com/b.jpg"
        ))).thenReturn(List.of(firstByUrl, secondByUrl));

        PostCreateRequest request = new PostCreateRequest(
                "title",
                List.of("USED_TRADE"),
                "content",
                List.of(
                        new PostImageRequest("https://cdn.example.com/b.jpg", 0, true),
                        new PostImageRequest("https://cdn.example.com/a.jpg", 1, false)
                )
        );

        postCreateService.create(1L, request);

        verify(imageAssetRepository).findAllWithLockByUrlInOrderByUrlAsc(List.of(
                "https://cdn.example.com/a.jpg",
                "https://cdn.example.com/b.jpg"
        ));
        assertThat(secondByUrl.getPost().getId()).isEqualTo(11L);
        assertThat(secondByUrl.isThumbnail()).isTrue();
        assertThat(secondByUrl.getSortOrder()).isZero();
        assertThat(firstByUrl.getPost().getId()).isEqualTo(11L);
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
