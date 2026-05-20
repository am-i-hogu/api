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

    /**
     * 집단지성 삭제
     *
     * @param userId    삭제 요청한 유저 id
     * @param postId    삭제될 집단지성이 포함된 게시물 id
     * @param commentId 삭제될 집단지성 id
     */
    @Transactional
    public void delete(Long userId, Long postId, Long commentId) {
        validatePost(postId);
        Comment comment = getCommentOrThrow(commentId);

        validateCommentBelongsToPost(postId, comment);
        validateWriter(userId, comment.getWriter().getId());

        comment.delete(LocalDateTime.now());
    }

    // postId로 게시물 정보를 조회하고, 존재하지 않거나 삭제된 게시물이면 오류 코드 반환
    private void validatePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(PostErrorCode.POST_NOT_FOUND));

        if (post.isDeleted()) {
            throw new CustomException(PostErrorCode.POST_ALREADY_DELETED);
        }
    }

    // 집단지성이 존재한다면 불러오고, 존재하지 않거나 삭제된 집단지성이면 오류 코드 반환
    private Comment getCommentOrThrow(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(CommentErrorCode.COMMENT_NOT_FOUND));

        if (comment.isDeleted()) {
            throw new CustomException(CommentErrorCode.COMMENT_ALREADY_DELETED);
        }

        return comment;
    }

    // 게시물과 집단지성 관계 검증
    private void validateCommentBelongsToPost(Long postId, Comment comment) {
        if (!comment.getPost().getId().equals(postId)) {
            throw new CustomException(CommentErrorCode.COMMENT_NOT_FOUND);
        }
    }

    // 요청 유저 정보와 작성자 정보가 일치하는지 검증
    private void validateWriter(Long requestedUserId, Long writerId) {
        if (!requestedUserId.equals(writerId)) {
            throw new CustomException(CommonErrorCode.FORBIDDEN_ACCESS);
        }
    }
}
