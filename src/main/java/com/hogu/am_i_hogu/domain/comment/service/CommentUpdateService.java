package com.hogu.am_i_hogu.domain.comment.service;

import com.hogu.am_i_hogu.common.exception.CommonErrorCode;
import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.domain.comment.domain.Comment;
import com.hogu.am_i_hogu.domain.comment.dto.request.CommentUpdateRequest;
import com.hogu.am_i_hogu.domain.comment.dto.response.CommentUpdateResponse;
import com.hogu.am_i_hogu.domain.comment.dto.response.CommentWriterResponse;
import com.hogu.am_i_hogu.domain.comment.exception.CommentErrorCode;
import com.hogu.am_i_hogu.domain.comment.repository.CommentHelpfulMarkRepository;
import com.hogu.am_i_hogu.domain.comment.repository.CommentRepository;
import com.hogu.am_i_hogu.domain.post.domain.Post;
import com.hogu.am_i_hogu.domain.post.exception.PostErrorCode;
import com.hogu.am_i_hogu.domain.post.repository.PostRepository;
import com.hogu.am_i_hogu.domain.user.domain.User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class CommentUpdateService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final CommentHelpfulMarkRepository commentHelpfulMarkRepository;

    public CommentUpdateService(
            PostRepository postRepository,
            CommentRepository commentRepository,
            CommentHelpfulMarkRepository commentHelpfulMarkRepository
    ) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.commentHelpfulMarkRepository = commentHelpfulMarkRepository;
    }

    public CommentUpdateResponse update(Long userId, Long postId, Long commentId, CommentUpdateRequest request) {
        Post post = getPostOrThrow(postId);
        Comment comment = getCommentOrThrow(commentId);

        validateWriter(userId, comment.getWriter().getId());
        validateRequest(request);

        Comment updatedComment = updateComment(comment, request.content());

        return toCommentUpdateResponse(post, updatedComment);
    }

    private Post getPostOrThrow(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(PostErrorCode.POST_NOT_FOUND));

        if (post.isDeleted()) {
            throw new CustomException(PostErrorCode.POST_ALREADY_DELETED);
        }

        return post;
    }

    private Comment getCommentOrThrow(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(CommentErrorCode.COMMENT_NOT_FOUND));

        if (comment.isDeleted()) {
            throw new CustomException(CommentErrorCode.COMMENT_ALREADY_DELETED);
        }

        return comment;
    }

    private void validateWriter(Long requestedUserId, Long writerId) {
        if (!requestedUserId.equals(writerId)) {
            throw new CustomException(CommonErrorCode.FORBIDDEN_ACCESS);
        }
    }

    private void validateRequest(CommentUpdateRequest request) {
        if (request == null) {
            throw new CustomException(CommentErrorCode.EMPTY_REQUEST_BODY);
        }

        String content = request.content();
        if (content == null || content.isBlank()) {
            throw new CustomException(CommentErrorCode.EMPTY_CONTENT);
        }
        if (content.length() > 300) {
            throw new CustomException(CommentErrorCode.CONTENT_LENGTH_EXCEEDED);
        }
    }

    private Comment updateComment(Comment comment, String content) {
        LocalDateTime now = LocalDateTime.now();
        comment.update(content, now);

        return commentRepository.save(comment);
    }

    private CommentUpdateResponse toCommentUpdateResponse(Post post, Comment comment) {
        User writer = comment.getWriter();
        boolean isPostWriter = writer.getId().equals(post.getWriter().getId());
        long helpfulMarkCount = getHelpfulMarkCount(comment.getId());
        Long parentId = comment.getParentComment() == null
                ? null
                : comment.getParentComment().getId();

        return new CommentUpdateResponse(
                comment.getId(),
                comment.getContent(),
                true,
                new CommentWriterResponse(
                    comment.getWriter().getNickname(),
                    comment.getWriter().getProfileImageUrl(),
                    isPostWriter
                ),
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                false,
                helpfulMarkCount,
                parentId,
                comment.getDepth()
        );
    }

    private long getHelpfulMarkCount(Long commentId) {
        return commentHelpfulMarkRepository.countById_CommentId(commentId);
    }
}
