package com.hogu.am_i_hogu.domain.comment.service;

import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.domain.comment.domain.Comment;
import com.hogu.am_i_hogu.domain.comment.dto.request.CommentUpdateRequest;
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

public class CommentUpdateServiceTest {

    private final CommentRepository commentRepository = mock(CommentRepository.class);
    private final PostRepository postRepository = mock(PostRepository.class);
    private final CommentHelpfulMarkRepository commentHelpfulMarkRepository = mock(CommentHelpfulMarkRepository.class);
    private final CommentUpdateService service = new CommentUpdateService(
            postRepository,
            commentRepository,
            commentHelpfulMarkRepository
    );

    /**
     * 집단지성 수정 실패 테스트:
     * 요청 body 없이 수정 요청을 보내고,
     * EMPTY_REQUEST_BODY 오류가 발생하는지 확인
     */
    @Test
    void updateThrowsEmptyRequestBodyWhenRequestIsNull() {
        LocalDateTime now = LocalDateTime.now();
        Post post = createPost(100L, 1L, false, now);
        Comment comment = createComment(1000L, post, 1L, null, 0, "content", false, now);

        when(postRepository.findById(100L)).thenReturn(Optional.of(post));
        when(commentRepository.findById(1000L)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> service.update(1L, 100L, 1000L, null))
                .isInstanceOfSatisfying(CustomException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(CommentErrorCode.EMPTY_REQUEST_BODY));
    }

    /**
     * 집단지성 수정 실패 테스트:
     * 비어있는 본문으로 수정 요청을 보내고,
     * EMPTY_CONTENT 오류가 발생하는지 확인
     */
    @Test
    void updateThrowsEmptyContentWhenContentIsBlank() {
        LocalDateTime now = LocalDateTime.now();
        Post post = createPost(100L, 1L, false, now);
        Comment comment = createComment(1000L, post, 1L, null, 0, "content", false, now);

        when(postRepository.findById(100L)).thenReturn(Optional.of(post));
        when(commentRepository.findById(1000L)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> service.update(1L, 100L, 1000L, new CommentUpdateRequest("   ")))
                .isInstanceOfSatisfying(CustomException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(CommentErrorCode.EMPTY_CONTENT));
    }

    /**
     * 집단지성 수정 실패 테스트:
     * 본문 길이가 300자를 초과하는 수정 요청을 보내고,
     * CONTENT_LENGTH_EXCEEDED 오류가 발생하는지 확인
     */
    @Test
    void updateThrowsContentLengthExceededWhenContentLengthExceeds300() {
        LocalDateTime now = LocalDateTime.now();
        Post post = createPost(100L, 1L, false, now);
        Comment comment = createComment(1000L, post, 1L, null, 0, "content", false, now);

        when(postRepository.findById(100L)).thenReturn(Optional.of(post));
        when(commentRepository.findById(1000L)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> service.update(1L, 100L, 1000L, new CommentUpdateRequest("a".repeat(301))))
                .isInstanceOfSatisfying(CustomException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(CommentErrorCode.CONTENT_LENGTH_EXCEEDED));
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
