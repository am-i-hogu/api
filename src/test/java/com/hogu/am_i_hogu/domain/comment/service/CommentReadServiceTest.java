package com.hogu.am_i_hogu.domain.comment.service;

import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.common.util.CursorCodec;
import com.hogu.am_i_hogu.domain.comment.dto.CommentCursor;
import com.hogu.am_i_hogu.domain.comment.dto.request.CursorRequest;
import com.hogu.am_i_hogu.domain.comment.exception.CommentErrorCode;
import com.hogu.am_i_hogu.domain.comment.repository.CommentHelpfulMarkRepository;
import com.hogu.am_i_hogu.domain.comment.repository.CommentRepository;
import com.hogu.am_i_hogu.domain.post.domain.Post;
import com.hogu.am_i_hogu.domain.post.repository.PostRepository;
import com.hogu.am_i_hogu.domain.user.domain.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CommentReadServiceTest {

    private final CommentRepository commentRepository = mock(CommentRepository.class);
    private final PostRepository postRepository = mock(PostRepository.class);
    private final CommentHelpfulMarkRepository commentHelpfulMarkRepository = mock(CommentHelpfulMarkRepository.class);
    private final CursorCodec cursorCodec = mock(CursorCodec.class);
    private final CommentReadService service = new CommentReadService(
            cursorCodec,
            commentRepository,
            commentHelpfulMarkRepository,
            postRepository
    );

    /**
     * 집단지성 조회 실패 테스트:
     * 유효하지 않은 정렬 기준과 cursor로 비회원이 조회 요청을 보내고,
     * - (1) INVALID_PARAM_VALUE 오류가 발생하는지 확인
     * - (2) sortBy, cursor 오류가 함께 반환되는지 확인
     */
    @Test
    void readThrowsInvalidParamValueWhenSortByAndCursorAreInvalid() {
        LocalDateTime now = LocalDateTime.now();
        Post post = createPost(100L, 1L, false, now);

        when(postRepository.findById(100L)).thenReturn(Optional.of(post));
        when(cursorCodec.decode("invalid-cursor", CommentCursor.class)).thenThrow(new IllegalStateException());

        CursorRequest request = new CursorRequest("INVALID", 5, "invalid-cursor");

        assertThatThrownBy(() -> service.read(null, 100L, request))
                .isInstanceOfSatisfying(CustomException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(CommentErrorCode.INVALID_PARAM_VALUE);
                    assertThat(exception.getErrors()).hasSize(2);
                    assertThat(exception.getErrors().get(0).getField()).isEqualTo("sortBy");
                    assertThat(exception.getErrors().get(0).getCode()).isEqualTo("INVALID_SORTING");
                    assertThat(exception.getErrors().get(1).getField()).isEqualTo("cursor");
                    assertThat(exception.getErrors().get(1).getCode()).isEqualTo("INVALID_CURSOR");
                });
    }

    /**
     * 집단지성 조회 실패 테스트:
     * 다중 정렬 기준과 유효하지 않은 cursor로 비회원이 조회 요청을 보내고,
     * - (1) INVALID_PARAM_VALUE 오류가 발생하는지 확인
     * - (2) sortBy, cursor 오류가 함께 반환되는지 확인
     */
    @Test
    void readThrowsInvalidParamValueWhenSortByIsMultipleAndCursorIsInvalid() {
        LocalDateTime now = LocalDateTime.now();
        Post post = createPost(100L, 1L, false, now);

        when(postRepository.findById(100L)).thenReturn(Optional.of(post));
        when(cursorCodec.decode("invalid-cursor", CommentCursor.class)).thenThrow(new IllegalStateException());

        CursorRequest request = new CursorRequest("LATEST,HELPFUL", 5, "invalid-cursor");

        assertThatThrownBy(() -> service.read(null, 100L, request))
                .isInstanceOfSatisfying(CustomException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(CommentErrorCode.INVALID_PARAM_VALUE);
                    assertThat(exception.getErrors()).hasSize(2);
                    assertThat(exception.getErrors().get(0).getField()).isEqualTo("sortBy");
                    assertThat(exception.getErrors().get(0).getCode()).isEqualTo("MULTIPLE_SORTING");
                    assertThat(exception.getErrors().get(1).getField()).isEqualTo("cursor");
                    assertThat(exception.getErrors().get(1).getCode()).isEqualTo("INVALID_CURSOR");
                });
    }

    private Post createPost(Long postId, Long writerUserId, boolean isDeleted, LocalDateTime now) {
        User writer = new User(writerUserId, "writer" + writerUserId, isDeleted, now);
        Post post = Post.builder()
                .id(postId)
                .writer(writer)
                .category(null)
                .title("title")
                .content("content")
                .createdAt(now)
                .updatedAt(now)
                .build();

        if (isDeleted) {
            post.delete(now);
        }

        return post;
    }
}
