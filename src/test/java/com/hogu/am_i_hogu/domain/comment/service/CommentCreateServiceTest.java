package com.hogu.am_i_hogu.domain.comment.service;

import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.common.util.TsidGenerator;
import com.hogu.am_i_hogu.domain.comment.domain.Comment;
import com.hogu.am_i_hogu.domain.comment.dto.request.CommentCreateRequest;
import com.hogu.am_i_hogu.domain.comment.exception.CommentErrorCode;
import com.hogu.am_i_hogu.domain.comment.repository.CommentHelpfulMarkRepository;
import com.hogu.am_i_hogu.domain.comment.repository.CommentRepository;
import com.hogu.am_i_hogu.domain.post.domain.Post;
import com.hogu.am_i_hogu.domain.post.repository.PostRepository;
import com.hogu.am_i_hogu.domain.user.domain.User;
import com.hogu.am_i_hogu.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CommentCreateServiceTest {

    private final CommentRepository commentRepository = mock(CommentRepository.class);
    private final PostRepository postRepository = mock(PostRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final CommentHelpfulMarkRepository commentHelpfulMarkRepository = mock(CommentHelpfulMarkRepository.class);
    private final TsidGenerator tsidGenerator = mock(TsidGenerator.class);
    private final CommentCreateService service = new CommentCreateService(
            commentRepository,
            postRepository,
            userRepository,
            tsidGenerator,
            commentHelpfulMarkRepository
    );

    /**
     * 집단지성 생성 실패 테스트:
     * depth 1인 집단지성에 대해 본문 길이가 300자를 초과하는 집단지성 생성 요청을 보내고,
     * - (1) INVALID_INPUT_VALUE 오류가 발생하는지 확인
     * - (2) content, depth 오류가 함께 반환되는지 확인
     */
    @Test
    void createThrowsInvalidInputValueWhenContentLengthExceeds300AndDepthExceeds1() {
        LocalDateTime now = LocalDateTime.now();
        Post post = createPost(100L, 1L, false, now);
        Comment parent = createComment(1000L, post, 1L, null, 1, "parent content", false, now);

        when(postRepository.findById(100L)).thenReturn(Optional.of(post));
        when(commentRepository.findById(1000L)).thenReturn(Optional.of(parent));

        CommentCreateRequest request = new CommentCreateRequest(1000L, "a".repeat(301));

        assertThatThrownBy(() -> service.create(1L, 100L, request))
                .isInstanceOfSatisfying(CustomException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(CommentErrorCode.INVALID_INPUT_VALUE);
                    assertThat(exception.getErrors()).hasSize(2);
                    assertThat(exception.getErrors().get(0).getField()).isEqualTo("content");
                    assertThat(exception.getErrors().get(0).getCode()).isEqualTo("CONTENT_LENGTH_EXCEEDED");
                    assertThat(exception.getErrors().get(1).getField()).isEqualTo("depth");
                    assertThat(exception.getErrors().get(1).getCode()).isEqualTo("DEPTH_EXCEEDED");
                });
    }

    /**
     * 집단지성 생성 실패 테스트:
     * depth 1인 집단지성에 대해 비어있는 본문으로 집단지성 생성 요청을 보내고,
     * - (1) INVALID_INPUT_VALUE 오류가 발생하는지 확인
     * - (2) content, depth 오류가 함께 반환되는지 확인
     */
    @Test
    void createThrowsInvalidInputValueWhenContentIsEmptyAndDepthExceeds1() {
        LocalDateTime now = LocalDateTime.now();
        Post post = createPost(100L, 1L, false, now);
        Comment parent = createComment(1000L, post, 1L, null, 1, "parent content", false, now);

        when(postRepository.findById(100L)).thenReturn(Optional.of(post));
        when(commentRepository.findById(1000L)).thenReturn(Optional.of(parent));

        CommentCreateRequest request = new CommentCreateRequest(1000L, "   ");

        assertThatThrownBy(() -> service.create(1L, 100L, request))
                .isInstanceOfSatisfying(CustomException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(CommentErrorCode.INVALID_INPUT_VALUE);
                    assertThat(exception.getErrors()).hasSize(2);
                    assertThat(exception.getErrors().get(0).getField()).isEqualTo("content");
                    assertThat(exception.getErrors().get(0).getCode()).isEqualTo("EMPTY_CONTENT");
                    assertThat(exception.getErrors().get(1).getField()).isEqualTo("depth");
                    assertThat(exception.getErrors().get(1).getCode()).isEqualTo("DEPTH_EXCEEDED");
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

    private Comment createComment(
            Long commentId,
            Post post,
            Long writerUserId,
            Comment parent,
            int depth,
            String content,
            boolean isDeleted,
            LocalDateTime now
    ) {
        User writer = new User(writerUserId, "writer" + writerUserId, false, now);
        Comment comment = new Comment(
                commentId,
                post,
                writer,
                parent,
                depth,
                content,
                now
        );

        if (isDeleted) {
            comment.delete(now);
        }

        return comment;
    }
}
