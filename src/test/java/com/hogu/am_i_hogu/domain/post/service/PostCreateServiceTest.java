package com.hogu.am_i_hogu.domain.post.service;

import com.hogu.am_i_hogu.common.storage.S3StorageService;
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
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostCreateServiceTest {

    private final TsidGenerator tsidGenerator = mock(TsidGenerator.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final CategoryRepository categoryRepository = mock(CategoryRepository.class);
    private final PostRepository postRepository = mock(PostRepository.class);
    private final ImageAssetRepository imageAssetRepository = mock(ImageAssetRepository.class);
    private final S3StorageService s3StorageService = mock(S3StorageService.class);
    private final PostCreateService postCreateService = new PostCreateService(
            tsidGenerator,
            s3StorageService,
            userRepository,
            categoryRepository,
            postRepository,
            imageAssetRepository
    );

    // 정상 케이스: 게시물 생성 시 S3에 존재하는 이미지 URL만 image_assets에 새로 저장한다.
    @Test
    void createStoresImageAssetsFromS3MetadataInRequestOrder() {
        LocalDateTime now = LocalDateTime.now();
        User writer = new User(1L, "hogu", false, now);
        Category category = mock(Category.class);

        when(categoryRepository.existsById("USED_TRADE")).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(writer));
        when(categoryRepository.findById("USED_TRADE")).thenReturn(Optional.of(category));
        when(tsidGenerator.nextId()).thenReturn(11L, 101L, 102L);
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(s3StorageService.findImageMetadata("https://cdn.example.com/b.jpg"))
                .thenReturn(Optional.of(new S3StorageService.ImageMetadata("image/jpeg", 20L)));
        when(s3StorageService.findImageMetadata("https://cdn.example.com/a.jpg"))
                .thenReturn(Optional.of(new S3StorageService.ImageMetadata("image/jpeg", 10L)));

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

        verify(imageAssetRepository, never()).findAllWithLockByUrlInOrderByUrlAsc(any());
        ArgumentCaptor<List<ImageAsset>> imageAssetsCaptor = ArgumentCaptor.forClass(List.class);
        verify(imageAssetRepository).saveAll(imageAssetsCaptor.capture());

        List<ImageAsset> imageAssets = imageAssetsCaptor.getValue();
        assertThat(imageAssets).hasSize(2);
        assertThat(imageAssets.get(0).getId()).isEqualTo(101L);
        assertThat(imageAssets.get(0).getUploadedByUser()).isSameAs(writer);
        assertThat(imageAssets.get(0).getPost().getId()).isEqualTo(11L);
        assertThat(imageAssets.get(0).getUrl()).isEqualTo("https://cdn.example.com/b.jpg");
        assertThat(imageAssets.get(0).getContentType()).isEqualTo("image/jpeg");
        assertThat(imageAssets.get(0).getSizeBytes()).isEqualTo(20L);
        assertThat(imageAssets.get(0).isThumbnail()).isTrue();
        assertThat(imageAssets.get(0).getSortOrder()).isZero();
        assertThat(imageAssets.get(1).getId()).isEqualTo(102L);
        assertThat(imageAssets.get(1).getUrl()).isEqualTo("https://cdn.example.com/a.jpg");
        assertThat(imageAssets.get(1).getSizeBytes()).isEqualTo(10L);
        assertThat(imageAssets.get(1).isThumbnail()).isFalse();
        assertThat(imageAssets.get(1).getSortOrder()).isEqualTo(1);
    }
}
