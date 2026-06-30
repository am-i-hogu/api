package com.hogu.am_i_hogu.domain.post.service;

import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.common.exception.ErrorResponse;
import com.hogu.am_i_hogu.common.storage.S3StorageService;
import com.hogu.am_i_hogu.common.util.TsidGenerator;
import com.hogu.am_i_hogu.domain.post.domain.Category;
import com.hogu.am_i_hogu.domain.post.domain.ImageAsset;
import com.hogu.am_i_hogu.domain.post.domain.Post;
import com.hogu.am_i_hogu.domain.post.dto.request.PostCreateRequest;
import com.hogu.am_i_hogu.domain.post.dto.request.PostImageRequest;
import com.hogu.am_i_hogu.domain.post.dto.response.PostCreateResponse;
import com.hogu.am_i_hogu.domain.post.exception.PostErrorCode;
import com.hogu.am_i_hogu.domain.post.repository.CategoryRepository;
import com.hogu.am_i_hogu.domain.post.repository.ImageAssetRepository;
import com.hogu.am_i_hogu.domain.post.repository.PostRepository;
import com.hogu.am_i_hogu.domain.user.domain.User;
import com.hogu.am_i_hogu.domain.user.exception.UserErrorCode;
import com.hogu.am_i_hogu.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PostCreateService {

    private final TsidGenerator tsidGenerator;
    private final S3StorageService s3StorageService;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PostRepository postRepository;
    private final ImageAssetRepository imageAssetRepository;

    /**
     * 게시물 생성 요청을 검증한 뒤 게시물과 이미지 메타데이터를 저장한다.
     *
     * @param userId 게시물을 작성하는 인증 사용자 id
     * @param request 게시물 생성 요청 본문
     * @return 생성된 게시물 id를 담은 응답 DTO
     */
    @Transactional
    public PostCreateResponse create(Long userId, PostCreateRequest request) {
        // 요청 본문과 필드 유효성을 먼저 검증해 DB 변경 전에 명세의 오류 응답을 확정한다.
        validate(request);

        LocalDateTime now = LocalDateTime.now();

        // 게시물 작성자와 카테고리는 posts 테이블의 FK 대상이므로 저장 전에 실제 row를 조회한다.
        User writer = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
        Category category = categoryRepository.findById(request.categories().get(0)).orElseThrow();
        List<PostImageRequest> images = request.images();

        // posts.id는 TSID로 생성한다.
        Post post = Post.builder()
                .id(tsidGenerator.nextId())
                .writer(writer)
                .category(category)
                .title(request.title())
                .content(request.content())
                .createdAt(now)
                .updatedAt(now)
                .build();
        Post savedPost = postRepository.save(post);

        List<ImageAsset> imageAssets = createImageAssets(writer, savedPost, images, now);
        imageAssetRepository.saveAll(imageAssets);

        return new PostCreateResponse(savedPost.getId());
    }

    /**
     * 게시물 생성 요청 body와 각 필드의 유효성을 검증한다.
     *
     * @param request 게시물 생성 요청 본문
     * @throws CustomException body가 없거나 필드 유효성 검사에 실패한 경우
     */
    private void validate(PostCreateRequest request) {
        // body 자체가 없으면 필드별 오류보다 EMPTY_REQUEST_BODY를 우선 반환한다.
        if (request == null) {
            throw new CustomException(PostErrorCode.EMPTY_REQUEST_BODY);
        }

        // 여러 필드 오류를 한 번에 errors 배열로 내려주므로 순서대로 errors 배열에 채운다.
        List<ErrorResponse.ErrorDetail> errors = new ArrayList<>();
        validateTitle(request, errors);
        validateCategories(request, errors);
        validateContent(request, errors);
        validateImages(request, errors);

        if (!errors.isEmpty()) {
            throw new CustomException(PostErrorCode.INVALID_INPUT_VALUE, errors);
        }
    }

    /**
     * 게시물 제목의 필수 여부와 길이 제한을 검증한다.
     *
     * @param request 게시물 생성 요청 본문
     * @param errors 누적할 필드별 오류 목록
     */
    private void validateTitle(PostCreateRequest request, List<ErrorResponse.ErrorDetail> errors) {
        if (request.title() == null || request.title().isBlank()) {
            errors.add(new ErrorResponse.ErrorDetail("title", "EMPTY_TITLE"));
            return;
        }
        if (request.title().length() > 50) {
            errors.add(new ErrorResponse.ErrorDetail("title", "TITLE_LENGTH_EXCEEDED"));
        }
    }

    /**
     * 게시물 카테고리가 하나만 선택됐고 실제 존재하는 코드인지 검증한다.
     *
     * @param request 게시물 생성 요청 본문
     * @param errors 누적할 필드별 오류 목록
     */
    private void validateCategories(PostCreateRequest request, List<ErrorResponse.ErrorDetail> errors) {
        if (request.categories() == null || request.categories().isEmpty()) {
            errors.add(new ErrorResponse.ErrorDetail("categories", "EMPTY_CATEGORIES"));
            return;
        }
        if (request.categories().size() > 1) {
            errors.add(new ErrorResponse.ErrorDetail("categories", "MULTIPLE_CATEGORIES"));
            return;
        }

        String categoryCode = request.categories().get(0);
        if (categoryCode == null || categoryCode.isBlank() || !categoryRepository.existsById(categoryCode)) {
            errors.add(new ErrorResponse.ErrorDetail("categories", "INVALID_CATEGORIES"));
        }
    }

    /**
     * 게시물 본문이 비어 있거나 공백만 있는지 검증한다.
     *
     * @param request 게시물 생성 요청 본문
     * @param errors 누적할 필드별 오류 목록
     */
    private void validateContent(PostCreateRequest request, List<ErrorResponse.ErrorDetail> errors) {
        if (request.content() == null || request.content().isBlank()) {
            errors.add(new ErrorResponse.ErrorDetail("content", "EMPTY_CONTENT"));
        }
    }

    /**
     * 이미지 개수, URL 형식, 썸네일 지정 여부를 검증한다.
     *
     * @param request 게시물 생성 요청 본문
     * @param errors 누적할 필드별 오류 목록
     */
    private void validateImages(PostCreateRequest request, List<ErrorResponse.ErrorDetail> errors) {
        if (request.images() == null) {
            errors.add(new ErrorResponse.ErrorDetail("images", "EMPTY_IMAGE_URL"));
            return;
        }

        // 이미지는 필수 필드지만 빈 배열은 "이미지 없음"으로 처리한다.
        List<PostImageRequest> images = request.images();
        if (images.size() > 5) {
            errors.add(new ErrorResponse.ErrorDetail("images", "IMAGE_COUNT_EXCEEDED"));
        }

        Set<String> imageUrls = new HashSet<>();
        int thumbnailCount = 0;
        for (PostImageRequest image : images) {
            if (image == null) {
                errors.add(new ErrorResponse.ErrorDetail("images", "EMPTY_IMAGE_URL"));
                continue;
            }

            String imageUrl = image.imageUrl();
            if (imageUrl == null || imageUrl.isBlank()) {
                errors.add(new ErrorResponse.ErrorDetail("images", "EMPTY_IMAGE_URL"));
                continue;
            }
            if (!isValidImageUrl(imageUrl)) {
                errors.add(new ErrorResponse.ErrorDetail("images", "INVALID_IMAGE_URL"));
            }
            if (!imageUrls.add(imageUrl)) {
                errors.add(new ErrorResponse.ErrorDetail("images", "DUPLICATE_IMAGE_URL"));
            }
            if (image.order() == null) {
                errors.add(new ErrorResponse.ErrorDetail("images", "EMPTY_IMAGE_ORDER"));
            }
            if (Boolean.TRUE.equals(image.isThumbnail())) {
                thumbnailCount++;
            }
        }

        // 이미지가 하나라도 있으면 목록/상세 대표 이미지 결정을 위해 썸네일 1개를 요구한다.
        if (!images.isEmpty() && thumbnailCount == 0) {
            errors.add(new ErrorResponse.ErrorDetail("images", "EMPTY_THUMBNAIL"));
        }

        // 썸네일이 2개 이상이라면 에러를 반환한다.
        if (thumbnailCount > 1) {
            errors.add(new ErrorResponse.ErrorDetail("images", "MULTIPLE_THUMBNAIL"));
        }
    }

    /**
     * 이미지 URL이 http 또는 https URL 형식인지 확인한다.
     *
     * @param imageUrl 검증할 이미지 URL
     * @return 유효한 http/https URL이면 true, 아니면 false
     */
    private boolean isValidImageUrl(String imageUrl) {
        // URL 문자열 검증은 S3 연동 전 임시 이미지 URL과 실제 http/https URL을 모두 허용한다.
        try {
            URI uri = new URI(imageUrl);
            return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    /**
     * 게시물 이미지 URL의 storage 존재 여부와 중복 저장 여부를 검증한 뒤 이미지 metadata entity를 생성한다.
     *
     * @param writer 게시물 작성자
     * @param post 이미지가 연결될 게시물
     * @param images 게시물 생성 요청에 포함된 이미지 목록
     * @param now 이미지 metadata 생성 시각
     * @return 저장할 이미지 metadata entity 목록
     * @throws CustomException 이미지 URL이 storage에 없거나 이미 다른 게시물 이미지로 저장된 경우
     */
    private List<ImageAsset> createImageAssets(
            User writer,
            Post post,
            List<PostImageRequest> images,
            LocalDateTime now
    ) {
        if (images.isEmpty()) {
            return Collections.emptyList();
        }

        List<ErrorResponse.ErrorDetail> errors = new ArrayList<>();
        List<ImageAsset> imageAssets = new ArrayList<>();
        for (PostImageRequest image : images) {
            Optional<S3StorageService.ImageMetadata> metadata = s3StorageService.findImageMetadata(image.imageUrl());
            if (metadata.isEmpty() || metadata.get().contentType() == null || metadata.get().contentType().isBlank()) {
                errors.add(new ErrorResponse.ErrorDetail("images", "INVALID_IMAGE_URL"));
                continue;
            }
            if (imageAssetRepository.existsByUrl(image.imageUrl())) {
                errors.add(new ErrorResponse.ErrorDetail("images", "INVALID_IMAGE_URL"));
                continue;
            }
            imageAssets.add(ImageAsset.builder()
                    .id(tsidGenerator.nextId())
                    .uploadedByUser(writer)
                    .post(post)
                    .url(image.imageUrl())
                    .contentType(metadata.get().contentType())
                    .sizeBytes(metadata.get().sizeBytes())
                    .isThumbnail(Boolean.TRUE.equals(image.isThumbnail()))
                    .sortOrder(image.order())
                    .createdAt(now)
                    .build());
        }

        if (!errors.isEmpty()) {
            throw new CustomException(PostErrorCode.INVALID_INPUT_VALUE, errors);
        }

        return imageAssets;
    }
}
