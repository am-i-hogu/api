package com.hogu.am_i_hogu.domain.post.service;

import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.common.exception.ErrorResponse;
import com.hogu.am_i_hogu.common.util.CursorCodec;
import com.hogu.am_i_hogu.domain.post.dto.HomePostCursor;
import com.hogu.am_i_hogu.domain.post.dto.HomePostSummary;
import com.hogu.am_i_hogu.domain.post.dto.request.HomePostSearchRequest;
import com.hogu.am_i_hogu.domain.post.dto.response.HomePostItemResponse;
import com.hogu.am_i_hogu.domain.post.dto.response.HomePostListResponse;
import com.hogu.am_i_hogu.domain.post.dto.response.PostWriterResponse;
import com.hogu.am_i_hogu.domain.post.exception.PostErrorCode;
import com.hogu.am_i_hogu.domain.post.repository.CategoryRepository;
import com.hogu.am_i_hogu.domain.post.repository.HomePostQueryRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class HomePostQueryService {

    private static final int DEFAULT_PAGE_SIZE = 5;
    private static final int MAX_PAGE_SIZE = 15;
    private static final int CONTENT_PREVIEW_LENGTH = 100;
    private static final String ALL_CATEGORY = "ALL";

    private final CursorCodec cursorCodec;
    private final CategoryRepository categoryRepository;
    private final HomePostQueryRepository homePostQueryRepository;

    public HomePostQueryService(
            CursorCodec cursorCodec,
            CategoryRepository categoryRepository,
            HomePostQueryRepository homePostQueryRepository
    ) {
        this.cursorCodec = cursorCodec;
        this.categoryRepository = categoryRepository;
        this.homePostQueryRepository = homePostQueryRepository;
    }

    /**
     * 홈 화면 게시물 목록을 조회한다.
     * 요청 파라미터를 검증하고, pageSize보다 1개 더 조회해 다음 페이지 존재 여부를 계산한다.
     */
    public HomePostListResponse getHomePosts(Long viewerUserId, HomePostSearchRequest request) {
        List<ErrorResponse.ErrorDetail> errors = new ArrayList<>();
        String keyword = normalizeKeyword(request.keyword(), errors);
        List<String> categoryCodes = normalizeCategories(request.categories(), errors);
        HomePostSortBy sortBy = normalizeSortBy(request.sortBy(), errors);
        int pageSize = normalizePageSize(request.pageSize());

        HomePostCursor cursor = null;
        if (errors.isEmpty()) {
            cursor = decodeCursor(request.cursor(), sortBy, errors);
        }
        if (!errors.isEmpty()) {
            throw new CustomException(PostErrorCode.INVALID_PARAM_VALUE, errors);
        }

        List<HomePostSummary> queriedPosts = homePostQueryRepository.findHomePosts(
                viewerUserId,
                keyword,
                categoryCodes,
                sortBy,
                cursor,
                pageSize + 1
        );
        boolean hasNext = queriedPosts.size() > pageSize;
        List<HomePostSummary> summaries = hasNext
                ? queriedPosts.subList(0, pageSize)
                : queriedPosts;

        Long totalPostCount = shouldReturnTotalPostCount(request.categories(), categoryCodes)
                ? homePostQueryRepository.countHomePosts(keyword, categoryCodes)
                : null;

        return new HomePostListResponse(
                totalPostCount,
                mapToResponses(summaries),
                hasNext,
                createNextCursor(hasNext, summaries, sortBy)
        );
    }

    /**
     * 검색어를 정리한다.
     * 검색어가 생략되면 null을 반환하고, 공백뿐이면 keyword 필드 오류를 추가한다.
     */
    private String normalizeKeyword(String rawKeyword, List<ErrorResponse.ErrorDetail> errors) {
        if (rawKeyword == null) {
            return null;
        }
        String keyword = rawKeyword.trim();
        if (keyword.isEmpty()) {
            errors.add(new ErrorResponse.ErrorDetail("keyword", "EMPTY_KEYWORD"));
            return null;
        }
        return keyword;
    }

    /**
     * comma-separated 카테고리 파라미터를 DB 카테고리 코드 목록으로 변환한다.
     * 생략되거나 ALL이면 전체 카테고리 조회로 보고 빈 목록을 반환한다.
     */
    private List<String> normalizeCategories(String rawCategories, List<ErrorResponse.ErrorDetail> errors) {
        if (rawCategories == null || rawCategories.isBlank()) {
            return List.of();
        }

        String[] tokens = rawCategories.split(",");
        Set<String> categoryCodes = new LinkedHashSet<>();
        for (String token : tokens) {
            String category = token.trim().toUpperCase(Locale.ROOT);
            if (category.isEmpty()) {
                errors.add(new ErrorResponse.ErrorDetail("categories", "INVALID_CATEGORIES"));
                return List.of();
            }
            if (ALL_CATEGORY.equals(category)) {
                return List.of();
            }
            categoryCodes.add(category);
        }

        if (categoryCodes.isEmpty()) {
            return List.of();
        }
        long foundCount = categoryRepository.findAllById(categoryCodes).size();
        if (foundCount != categoryCodes.size()) {
            errors.add(new ErrorResponse.ErrorDetail("categories", "INVALID_CATEGORIES"));
            return List.of();
        }
        return List.copyOf(categoryCodes);
    }

    /**
     * 정렬 파라미터를 홈 목록 정렬 enum으로 변환한다.
     * 생략되면 최신순을 기본값으로 사용하고, 복수 값이나 미지원 값은 필드 오류로 기록한다.
     */
    private HomePostSortBy normalizeSortBy(String rawSortBy, List<ErrorResponse.ErrorDetail> errors) {
        if (rawSortBy == null || rawSortBy.isBlank()) {
            return HomePostSortBy.LATEST;
        }
        if (rawSortBy.contains(",")) {
            errors.add(new ErrorResponse.ErrorDetail("sortBy", "MULTIPLE_SORTING"));
            return HomePostSortBy.LATEST;
        }
        try {
            return HomePostSortBy.valueOf(rawSortBy.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            errors.add(new ErrorResponse.ErrorDetail("sortBy", "INVALID_SORTING"));
            return HomePostSortBy.LATEST;
        }
    }

    /**
     * pageSize를 서비스 정책 범위로 보정한다.
     * 값이 없거나 1보다 작으면 기본값을 사용하고, 최대값을 넘으면 최대값으로 제한한다.
     */
    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    /**
     * 요청 cursor를 디코딩하고 현재 정렬 기준과 호환되는지 검증한다.
     * cursor 형식이 잘못됐거나 정렬 기준이 다르면 cursor 필드 오류를 추가한다.
     */
    private HomePostCursor decodeCursor(String rawCursor, HomePostSortBy sortBy, List<ErrorResponse.ErrorDetail> errors) {
        if (rawCursor == null || rawCursor.isBlank()) {
            return null;
        }
        try {
            HomePostCursor cursor = cursorCodec.decode(rawCursor, HomePostCursor.class);
            if (cursor.postId() == null || cursor.createdAt() == null || !sortBy.name().equals(cursor.sortBy())) {
                errors.add(new ErrorResponse.ErrorDetail("cursor", "INVALID_CURSOR"));
                return null;
            }
            validateSortValue(sortBy, cursor, errors);
            return errors.isEmpty() ? cursor : null;
        } catch (IllegalStateException e) {
            errors.add(new ErrorResponse.ErrorDetail("cursor", "INVALID_CURSOR"));
            return null;
        }
    }

    /**
     * 정렬 기준별 cursor 필수 값을 검증한다.
     * 최신순은 생성 시각과 postId만 필요하고, 나머지 정렬은 각 정렬값이 추가로 필요하다.
     */
    private void validateSortValue(
            HomePostSortBy sortBy,
            HomePostCursor cursor,
            List<ErrorResponse.ErrorDetail> errors
    ) {
        boolean invalid = switch (sortBy) {
            case LATEST -> false;
            case MOST_VIEWED -> cursor.viewCount() == null;
            case MOST_COMMENTED -> cursor.commentCount() == null;
            case MOST_PARTICIPATED -> cursor.totalVoteCount() == null;
        };
        if (invalid) {
            errors.add(new ErrorResponse.ErrorDetail("cursor", "INVALID_CURSOR"));
        }
    }

    /**
     * totalPostCount를 응답에 포함할지 판단한다.
     * 명세 기준으로 카테고리 필터가 실제 적용된 경우에만 전체 개수를 반환한다.
     */
    private boolean shouldReturnTotalPostCount(String rawCategories, List<String> categoryCodes) {
        return rawCategories != null && !rawCategories.isBlank() && !categoryCodes.isEmpty();
    }

    /**
     * repository 조회 결과를 API 응답 DTO로 변환한다.
     * 단일 카테고리 정책을 유지하되, 응답 형식은 categories 배열로 맞춘다.
     */
    private List<HomePostItemResponse> mapToResponses(List<HomePostSummary> summaries) {
        return summaries.stream()
                .map(summary -> new HomePostItemResponse(
                        summary.postId(),
                        summary.bookmarked(),
                        List.of(summary.category()),
                        summary.title(),
                        summary.createdAt(),
                        summary.viewCount(),
                        toContentPreview(summary.content()),
                        summary.thumbnailUrl(),
                        summary.totalVoteCount(),
                        summary.commentCount(),
                        new PostWriterResponse(
                                summary.writerNickname(),
                                summary.writerProfileImageUrl()
                        )
                ))
                .toList();
    }

    /**
     * 본문 미리보기를 만든다.
     * 100자 이하면 원문을 그대로 사용하고, 넘으면 앞 100자만 반환한다.
     */
    private String toContentPreview(String content) {
        if (content.length() <= CONTENT_PREVIEW_LENGTH) {
            return content;
        }
        return content.substring(0, CONTENT_PREVIEW_LENGTH);
    }

    /**
     * 다음 페이지 요청에 사용할 cursor를 생성한다.
     * 다음 페이지가 없거나 응답 게시물이 없으면 null을 반환한다.
     */
    private String createNextCursor(boolean hasNext, List<HomePostSummary> summaries, HomePostSortBy sortBy) {
        if (!hasNext || summaries.isEmpty()) {
            return null;
        }

        HomePostSummary last = summaries.get(summaries.size() - 1);
        return cursorCodec.encode(new HomePostCursor(
                sortBy.name(),
                last.postId(),
                last.createdAt(),
                last.viewCount(),
                last.commentCount(),
                last.totalVoteCount()
        ));
    }
}
