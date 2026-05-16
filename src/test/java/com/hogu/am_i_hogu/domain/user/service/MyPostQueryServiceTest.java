package com.hogu.am_i_hogu.domain.user.service;

import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.common.pagination.CursorCodec;
import com.hogu.am_i_hogu.common.pagination.CursorRequest;
import com.hogu.am_i_hogu.domain.comment.repository.CommentRepository;
import com.hogu.am_i_hogu.domain.post.repository.PostRepository;
import com.hogu.am_i_hogu.domain.user.dto.MyPostCursor;
import com.hogu.am_i_hogu.domain.user.exception.UserErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

public class MyPostQueryServiceTest {

    private final CursorCodec cursorCodec = mock(CursorCodec.class);
    private final PostRepository postRepository = mock(PostRepository.class);
    private final CommentRepository commentRepository = mock(CommentRepository.class);
    private final MyPostQueryService myPostQueryService = new MyPostQueryService(
            cursorCodec, postRepository, commentRepository
    );

    /**
     * 유효하지 않은 cursor인 경우 테스트:
     * 유효하지 않은 cursor로 요청을 했을 시
     * INVALID_PARAM_VALUE 예외가 발생하는지 확인
     */
    @Test
    void getMyPostsThrowsInvalidParamValueWhenCursorIsInvalid() {
        when(cursorCodec.decode("invalid-cursor", MyPostCursor.class))
                .thenThrow(new IllegalStateException("Failed to decode cursor"));

        assertThatThrownBy(() -> myPostQueryService.getMyPosts(
                1L,
                new CursorRequest(5, "invalid-cursor")
        )).isInstanceOfSatisfying(CustomException.class, exception -> {
            assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.INVALID_PARAM_VALUE);
            assertThat(exception.getErrors().get(0).getField()).isEqualTo("cursor");
            assertThat(exception.getErrors().get(0).getCode()).isEqualTo("INVALID_CURSOR");
        });

        verifyNoInteractions(postRepository);
        verifyNoInteractions(commentRepository);
    }
}
