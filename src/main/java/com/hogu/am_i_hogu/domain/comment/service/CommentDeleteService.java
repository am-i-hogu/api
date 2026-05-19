package com.hogu.am_i_hogu.domain.comment.service;

import com.hogu.am_i_hogu.common.exception.CommonErrorCode;
import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.domain.comment.domain.Comment;
import com.hogu.am_i_hogu.domain.comment.exception.CommentErrorCode;
import com.hogu.am_i_hogu.domain.comment.repository.CommentRepository;
import com.hogu.am_i_hogu.domain.post.domain.Post;
import com.hogu.am_i_hogu.domain.post.exception.PostErrorCode;
import com.hogu.am_i_hogu.domain.post.repository.PostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class CommentDeleteService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    public CommentDeleteService(
            PostRepository postRepository,
            CommentRepository commentRepository
    ) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
    }

    @Transactional
    public void delete(Long userId, Long postId, Long commentId) {
        getPostOrThrow(postId);
        Comment comment = getCommentOrThrow(commentId);

        validateCommentBelongsToPost(postId, comment);
        validateWriter(userId, comment.getWriter().getId());

        comment.delete(LocalDateTime.now());
    }

    private void getPostOrThrow(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(PostErrorCode.POST_NOT_FOUND));

        if (post.isDeleted()) {
            throw new CustomException(PostErrorCode.POST_ALREADY_DELETED);
        }
    }

    private Comment getCommentOrThrow(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(CommentErrorCode.COMMENT_NOT_FOUND));

        if (comment.isDeleted()) {
            throw new CustomException(CommentErrorCode.COMMENT_ALREADY_DELETED);
        }

        return comment;
    }

    private void validateCommentBelongsToPost(Long postId, Comment comment) {
        if (!comment.getPost().getId().equals(postId)) {
            throw new CustomException(CommentErrorCode.COMMENT_NOT_FOUND);
        }
    }

    private void validateWriter(Long requestedUserId, Long writerId) {
        if (!requestedUserId.equals(writerId)) {
            throw new CustomException(CommonErrorCode.FORBIDDEN_ACCESS);
        }
    }
}
