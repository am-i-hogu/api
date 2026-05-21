package com.hogu.am_i_hogu.domain.comment.service;

import com.hogu.am_i_hogu.common.exception.CommonErrorCode;
import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.domain.comment.domain.Comment;
import com.hogu.am_i_hogu.domain.comment.domain.CommentHelpfulMark;
import com.hogu.am_i_hogu.domain.comment.domain.CommentHelpfulMarkId;
import com.hogu.am_i_hogu.domain.comment.dto.response.CommentHelpfulResponse;
import com.hogu.am_i_hogu.domain.comment.exception.CommentErrorCode;
import com.hogu.am_i_hogu.domain.comment.repository.CommentHelpfulMarkRepository;
import com.hogu.am_i_hogu.domain.comment.repository.CommentRepository;
import com.hogu.am_i_hogu.domain.post.domain.Post;
import com.hogu.am_i_hogu.domain.post.exception.PostErrorCode;
import com.hogu.am_i_hogu.domain.post.repository.PostRepository;
import com.hogu.am_i_hogu.domain.user.domain.User;
import com.hogu.am_i_hogu.domain.user.exception.UserErrorCode;
import com.hogu.am_i_hogu.domain.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class CommentHelpfulService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final CommentHelpfulMarkRepository commentHelpfulMarkRepository;

    public CommentHelpfulService(
            UserRepository userRepository,
            PostRepository postRepository,
            CommentRepository commentRepository,
            CommentHelpfulMarkRepository commentHelpfulMarkRepository
    ) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.commentHelpfulMarkRepository = commentHelpfulMarkRepository;
    }

    /**
     * 유익해요 등록
     *
     * @param userId    유익해요 등록 요청한 유저 id
     * @param postId    집단지성이 포함된 게시물 id
     * @param commentId 집단지성 id
     * @return 집단지성의 총 유익해요 수 및 유익해요 등록 결과
     */
    @Transactional
    public CommentHelpfulResponse createHelpful(Long userId, Long postId, Long commentId) {
        validate(userId, postId, commentId);
        CommentHelpfulMarkId id = createHelpfulMarkId(userId, commentId);

        saveCommentHelpfulMark(id);

        return new CommentHelpfulResponse(getHelpfulMarkCount(commentId), true);
    }

    @Transactional
    public CommentHelpfulResponse deleteHelpful(Long userId, Long postId, Long commentId) {
        validate(userId, postId, commentId);
        deleteHelpfulMark(userId, commentId);

        return new CommentHelpfulResponse(getHelpfulMarkCount(commentId), false);
    }

    /**
     * 요청 검증:
     * - (1) 존재하는 유저인지 검증
     * - (2) 존재하는 게시물인지 검증
     * - (3) 존재하는 집단지성인지 검증
     * - (4) 게시물-집단지성 관계 검증
     * - (5) 집단지성 작성자가 요청한 것 아닌지 검증
     */
    private void validate(Long userId, Long postId, Long commentId) {
        User user = validateUser(userId);
        validatePost(postId);
        Comment comment = validateComment(commentId);

        validateCommentBelongsToPost(postId, comment);
        validateNotCommentWriter(user.getId(), comment.getWriter().getId());
    }

    // 유저가 존재한다면 불러오고, 존재하지 않거나 삭제된 유저면 오류 코드 반환
    private User validateUser(Long userId) {
        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        return user;
    }

    // 게시물 정보를 조회하고, 존재하지 않거나 삭제된 게시물이면 오류 코드 반환
    private void validatePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(PostErrorCode.POST_NOT_FOUND));

        if (post.isDeleted()) {
            throw new CustomException(PostErrorCode.POST_ALREADY_DELETED);
        }
    }

    // 집단지성이 존재한다면 불러오고, 존재하지 않거나 삭제된 집단지성이면 오류 코드 반환
    private Comment validateComment(Long commentId) {
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

    // 집단지성 작성자 본인인지 검증
    private void validateNotCommentWriter(Long userId, Long commentWriterId) {
        if (userId.equals(commentWriterId)) {
            throw new CustomException(CommonErrorCode.FORBIDDEN_ACCESS);
        }
    }

    // 유익해요 id 생성
    private CommentHelpfulMarkId createHelpfulMarkId(Long userId, Long commentId) {
        CommentHelpfulMarkId id = new CommentHelpfulMarkId(userId, commentId);

        if (commentHelpfulMarkRepository.existsById(id)) {
            throw new CustomException(CommentErrorCode.DUPLICATE_REQUEST);
        }

        return id;
    }

    private void deleteHelpfulMark(Long userId, Long commentId) {
        CommentHelpfulMarkId id = new CommentHelpfulMarkId(userId, commentId);

        if (commentHelpfulMarkRepository.existsById(id)) {
            commentHelpfulMarkRepository.deleteById(id);
        }
    }

    // 유익해요 저장
    private void saveCommentHelpfulMark(CommentHelpfulMarkId id) {
        try {
            commentHelpfulMarkRepository.saveAndFlush(new CommentHelpfulMark(id, LocalDateTime.now()));
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(CommentErrorCode.DUPLICATE_REQUEST);
        }
    }

    // 집단지성의 유익해요 수 조회해 반환
    private long getHelpfulMarkCount(Long commentId) {
        return commentHelpfulMarkRepository.countById_CommentId(commentId);
    }
}
