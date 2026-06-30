package com.hogu.am_i_hogu.domain.post.service;

import com.hogu.am_i_hogu.common.exception.CommonErrorCode;
import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.common.exception.ErrorResponse;
import com.hogu.am_i_hogu.common.storage.S3StorageService;
import com.hogu.am_i_hogu.common.util.TsidGenerator;
import com.hogu.am_i_hogu.domain.post.domain.Category;
import com.hogu.am_i_hogu.domain.post.domain.ImageAsset;
import com.hogu.am_i_hogu.domain.post.domain.Post;
import com.hogu.am_i_hogu.domain.post.dto.request.PostImageRequest;
import com.hogu.am_i_hogu.domain.post.dto.request.PostUpdateRequest;
import com.hogu.am_i_hogu.domain.post.dto.response.PostUpdateResponse;
import com.hogu.am_i_hogu.domain.post.exception.PostErrorCode;
import com.hogu.am_i_hogu.domain.post.repository.CategoryRepository;
import com.hogu.am_i_hogu.domain.post.repository.ImageAssetRepository;
import com.hogu.am_i_hogu.domain.post.repository.PostRepository;
import com.hogu.am_i_hogu.domain.user.domain.User;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostUpdateService {
    private final TsidGenerator tsidGenerator;
    private final S3StorageService s3StorageService;
    private final CategoryRepository categoryRepository;
    private final PostRepository postRepository;
    private final ImageAssetRepository imageAssetRepository;

    /**
     * 게시물 수정 요청을 검증하고, 요청된 이미지가 있으면 업로드 image asset 연결을 교체한다.
     *
     * @param postId 수정할 게시물 id
     * @param userId 수정을 요청하는 인증 사용자 id
     * @param request 게시물 수정 요청 본문
     * @return 수정된 게시물 id를 담은 응답 DTO
     * @throws CustomException 요청 검증에 실패하거나 수정 권한 또는 대상 게시물 조건을 만족하지 못한 경우
     */
    @Transactional
    public PostUpdateResponse update(Long postId, Long userId, PostUpdateRequest request) {
        // 요청 본문과 필드 유효성을 먼저 검증해 DB 변경 전에 명세의 오류 응답을 확정한다.
        validate(request);

        // 삭제된 게시물인지 조회한다.
        Post post = postRepository.findByIdWithLock(postId)
                .orElseThrow(() -> new CustomException(PostErrorCode.POST_NOT_FOUND));

        // 삭제된 게시물이라면 POST_ALREADY_DELETED 에러를 반환한다.
        if (post.isDeleted()) {
            throw new CustomException(PostErrorCode.POST_ALREADY_DELETED);
        }

        // 작성자가 아닌 유저가 수정 요청을 보냈다면 FORBIDDEN_ACCESS 에러를 반환한다.
        if (!post.getWriter().getId().equals(userId)) {
            throw new CustomException(CommonErrorCode.FORBIDDEN_ACCESS);
        }

        LocalDateTime now = LocalDateTime.now();

        Category category = null;
        if (request.categories() != null) {
            category = categoryRepository.findById(request.categories().get(0))
                    .orElseThrow(() -> new CustomException(PostErrorCode.INVALID_INPUT_VALUE));
        }

        post.update(request.title(), category, request.content(), now);

        // 수정된 이미지가 있다면 기존 이미지 row를 삭제하고 요청 이미지를 새 image asset으로 저장한다.
        if (request.images() != null) {
            List<ImageAsset> previousImageAssets = imageAssetRepository.findByPost_IdOrderBySortOrderAsc(postId);
            Set<String> previousImageUrls = previousImageAssets.stream()
                    .map(ImageAsset::getUrl)
                    .collect(Collectors.toSet());
            List<ImageAsset> imageAssets = createImageAssets(post.getWriter(), post, request.images(), previousImageUrls, now);
            imageAssetRepository.deleteAll(previousImageAssets);
            imageAssetRepository.flush();
            imageAssetRepository.saveAll(imageAssets);
        }

        return new PostUpdateResponse(post.getId());
    }

    /**
     * 게시물 수정 요청 body와 전달된 필드의 유효성을 검증한다.
     *
     * @param request 게시물 수정 요청 본문
     * @throws CustomException body가 없거나 전달된 필드의 유효성 검사에 실패한 경우
     */
    private void validate(PostUpdateRequest request) {
        // body 자체가 없으면 필드별 오류보다 EMPTY_REQUEST_BODY를 우선 반환한다.
        if (request == null) {
            throw new CustomException(PostErrorCode.EMPTY_REQUEST_BODY);
        }
        // 최소 1개의 필드가 포함되지 않았을 경우 EMPTY_REQUEST_BODY를 반환한다.
        if (request.title() == null
                && request.categories() == null
                && request.content() == null
                && request.images() == null) {
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
     * @param request 게시물 수정 요청 본문
     * @param errors 누적할 필드별 오류 목록
     */
    private void validateTitle(PostUpdateRequest request, List<ErrorResponse.ErrorDetail> errors) {
        if (request.title() == null) {
            return;
        }
        if (request.title().isBlank()) {
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
     * @param request 게시물 수정 요청 본문
     * @param errors 누적할 필드별 오류 목록
     */
    private void validateCategories(PostUpdateRequest request, List<ErrorResponse.ErrorDetail> errors) {
        if (request.categories() == null) {
            return;
        }
        if (request.categories().isEmpty()) {
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
     * @param request 게시물 수정 요청 본문
     * @param errors 누적할 필드별 오류 목록
     */
    private void validateContent(PostUpdateRequest request, List<ErrorResponse.ErrorDetail> errors) {
        if (request.content() == null) {
            return;
        }
        if (request.content().isBlank()) {
            errors.add(new ErrorResponse.ErrorDetail("content", "EMPTY_CONTENT"));
        }
    }

    /**
     * 이미지 개수, URL 형식, 썸네일 지정 여부를 검증한다.
     *
     * @param request 게시물 수정 요청 본문
     * @param errors 누적할 필드별 오류 목록
     */
    private void validateImages(PostUpdateRequest request, List<ErrorResponse.ErrorDetail> errors) {
        if (request.images() == null) {
            return;
        }

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

    private List<ImageAsset> createImageAssets(
            User writer,
            Post post,
            List<PostImageRequest> images,
            Set<String> previousImageUrls,
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
            if (!previousImageUrls.contains(image.imageUrl()) && imageAssetRepository.existsByUrl(image.imageUrl())) {
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
