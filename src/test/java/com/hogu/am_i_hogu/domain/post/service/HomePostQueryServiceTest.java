package com.hogu.am_i_hogu.domain.post.service;

import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.common.util.CursorCodec;
import com.hogu.am_i_hogu.domain.post.dto.HomePostCursor;
import com.hogu.am_i_hogu.domain.post.dto.request.HomePostSearchRequest;
import com.hogu.am_i_hogu.domain.post.exception.PostErrorCode;
import com.hogu.am_i_hogu.domain.post.repository.CategoryRepository;
import com.hogu.am_i_hogu.domain.post.repository.HomePostQueryRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class HomePostQueryServiceTest {

    private final CursorCodec cursorCodec = mock(CursorCodec.class);
    private final CategoryRepository categoryRepository = mock(CategoryRepository.class);
    private final HomePostQueryRepository homePostQueryRepository = mock(HomePostQueryRepository.class);
    private final HomePostQueryService homePostQueryService = new HomePostQueryService(
            cursorCodec,
            categoryRepository,
            homePostQueryRepository
    );

    // 실패 케이스: cursor 디코딩에 실패하면 INVALID_CURSOR를 반환하고 DB 조회를 수행하지 않는다.
    @Test
    void getHomePostsThrowsInvalidParamValueWhenCursorIsInvalid() {
        when(cursorCodec.decode("invalid-cursor", HomePostCursor.class))
                .thenThrow(new IllegalStateException("Failed to decode cursor"));
        assertThatThrownBy(() -> homePostQueryService.getHomePosts(
                null,
                new HomePostSearchRequest(null, null, null, 5, "invalid-cursor")
        )).isInstanceOfSatisfying(CustomException.class, exception -> {
            assertThat(exception.getErrorCode()).isEqualTo(PostErrorCode.INVALID_PARAM_VALUE);
            assertThat(exception.getErrors().get(0).getField()).isEqualTo("cursor");
            assertThat(exception.getErrors().get(0).getCode()).isEqualTo("INVALID_CURSOR");
        });
        verifyNoInteractions(homePostQueryRepository);
    }

    // 실패 케이스: keyword가 공백뿐이면 EMPTY_KEYWORD를 반환하고 DB 조회를 수행하지 않는다.
    @Test
    void getHomePostsThrowsInvalidParamValueWhenKeywordIsBlank() {
        assertThatThrownBy(() -> homePostQueryService.getHomePosts(
                null,
                new HomePostSearchRequest("   ", null, null, 5, null)
        )).isInstanceOfSatisfying(CustomException.class, exception -> {
            assertThat(exception.getErrorCode()).isEqualTo(PostErrorCode.INVALID_PARAM_VALUE);
            assertThat(exception.getErrors().get(0).getField()).isEqualTo("keyword");
            assertThat(exception.getErrors().get(0).getCode()).isEqualTo("EMPTY_KEYWORD");
        });
        verifyNoInteractions(homePostQueryRepository);
    }

    // 실패 케이스: 존재하지 않는 category면 INVALID_CATEGORIES를 반환하고 DB 조회를 수행하지 않는다.
    @Test
    void getHomePostsThrowsInvalidParamValueWhenCategoryDoesNotExist() {
        when(categoryRepository.findAllById(any()))
                .thenReturn(List.of());
        assertThatThrownBy(() -> homePostQueryService.getHomePosts(
                null,
                new HomePostSearchRequest(null, "UNKNOWN", null, 5, null)
        )).isInstanceOfSatisfying(CustomException.class, exception -> {
            assertThat(exception.getErrorCode()).isEqualTo(PostErrorCode.INVALID_PARAM_VALUE);
            assertThat(exception.getErrors().get(0).getField()).isEqualTo("categories");
            assertThat(exception.getErrors().get(0).getCode()).isEqualTo("INVALID_CATEGORIES");
        });
        verifyNoInteractions(homePostQueryRepository);
    }

    // 실패 케이스: 정의되지 않은 sortBy면 INVALID_SORTING을 반환하고 DB 조회를 수행하지 않는다.
    @Test
    void getHomePostsThrowsInvalidParamValueWhenSortByIsInvalid() {
        assertThatThrownBy(() -> homePostQueryService.getHomePosts(
                null,
                new HomePostSearchRequest(null, null, "UNKNOWN", 5, null)
        )).isInstanceOfSatisfying(CustomException.class, exception -> {
            assertThat(exception.getErrorCode()).isEqualTo(PostErrorCode.INVALID_PARAM_VALUE);
            assertThat(exception.getErrors().get(0).getField()).isEqualTo("sortBy");
            assertThat(exception.getErrors().get(0).getCode()).isEqualTo("INVALID_SORTING");
        });
        verifyNoInteractions(homePostQueryRepository);
    }

    // 실패 케이스: sortBy가 comma로 여러 개 전달되면 MULTIPLE_SORTING을 반환하고 DB 조회를 수행하지 않는다.
    @Test
    void getHomePostsThrowsInvalidParamValueWhenSortByHasMultipleValues() {
        assertThatThrownBy(() -> homePostQueryService.getHomePosts(
                null,
                new HomePostSearchRequest(null, null, "LATEST,MOST_VIEWED", 5, null)
        )).isInstanceOfSatisfying(CustomException.class, exception -> {
            assertThat(exception.getErrorCode()).isEqualTo(PostErrorCode.INVALID_PARAM_VALUE);
            assertThat(exception.getErrors().get(0).getField()).isEqualTo("sortBy");
            assertThat(exception.getErrors().get(0).getCode()).isEqualTo("MULTIPLE_SORTING");
        });
        verifyNoInteractions(homePostQueryRepository);
    }
}
