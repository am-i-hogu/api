package com.hogu.am_i_hogu.domain.comment.service;

import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.common.exception.ErrorResponse;
import com.hogu.am_i_hogu.common.util.TsidGenerator;
import com.hogu.am_i_hogu.domain.comment.domain.Comment;
import com.hogu.am_i_hogu.domain.comment.dto.request.CommentCreateRequest;
import com.hogu.am_i_hogu.domain.comment.dto.response.CommentCreateResponse;
import com.hogu.am_i_hogu.domain.comment.dto.response.CommentWriterResponse;
import com.hogu.am_i_hogu.domain.comment.exception.CommentErrorCode;
import com.hogu.am_i_hogu.domain.comment.repository.CommentHelpfulMarkRepository;
import com.hogu.am_i_hogu.domain.comment.repository.CommentRepository;
import com.hogu.am_i_hogu.domain.post.domain.Post;
import com.hogu.am_i_hogu.domain.post.exception.PostErrorCode;
import com.hogu.am_i_hogu.domain.post.repository.PostRepository;
import com.hogu.am_i_hogu.domain.user.domain.User;
import com.hogu.am_i_hogu.domain.user.exception.UserErrorCode;
import com.hogu.am_i_hogu.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class CommentCreateService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final TsidGenerator tsidGenerator;
    private final CommentHelpfulMarkRepository commentHelpfulMarkRepository;

    public CommentCreateService(
            CommentRepository commentRepository,
            PostRepository postRepository,
            UserRepository userRepository,
            TsidGenerator tsidGenerator,
            CommentHelpfulMarkRepository commentHelpfulMarkRepository
    ) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.tsidGenerator = tsidGenerator;
        this.commentHelpfulMarkRepository = commentHelpfulMarkRepository;
    }

    /**
     * 집단지성 생성
     *
     * @param userId    생성 시도한 유저 id
     * @param postId    집단지성이 포함될 게시물 id
     * @param request   집단지성 생성 정보(부모 집단지성 정보, 내용)
     * @return  생성된 집단지성 정보
     */
    @Transactional
    public CommentCreateResponse create(
            Long userId,
            Long postId,
            CommentCreateRequest request
    ) {
        Post post = getPostOrThrow(postId);

        if (request == null) {
            throw new CustomException(CommentErrorCode.EMPTY_REQUEST_BODY);
        }
        Comment parent = getParent(request);
        validateRequest(request, parent);

        User writer = getWriter(userId);
        Comment savedComment = createComment(post, writer, parent, request.content());

        return toCommentCreateResponse(post, writer, savedComment);
    }

    // post가 존재한다면 불러오고, 존재하지 않거나 삭제된 게시물이면 오류 코드 반환
    private Post getPostOrThrow(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(PostErrorCode.POST_NOT_FOUND));

        if (post.isDeleted()) {
            throw new CustomException(PostErrorCode.POST_ALREADY_DELETED);
        }

        return post;
    }

    // 부모 집단지성이 존재한다면 불러오고, 존재하지 않거나 삭제된 집단지성이면 오류 코드 반환
    private Comment getParent(CommentCreateRequest request) {
        if (request.parentId() == null) {
            return null;
        }

        Comment parent = commentRepository.findById(request.parentId())
                .orElseThrow(() -> new CustomException(CommentErrorCode.PARENT_NOT_FOUND));

        if (parent.isDeleted()) {
            throw new CustomException(CommentErrorCode.PARENT_ALREADY_DELETED);
        }

        return parent;
    }

    // 요청 body(depth, content) 검증
    private void validateRequest(CommentCreateRequest request, Comment parent) {
        List<ErrorResponse.ErrorDetail> errors = new ArrayList<>();

        String content = request.content();
        if (content == null || content.isBlank()) {
            errors.add(new ErrorResponse.ErrorDetail("content", "EMPTY_CONTENT"));
        } else if (content.length() > 300) {
            errors.add(new ErrorResponse.ErrorDetail("content", "CONTENT_LENGTH_EXCEEDED"));
        }

        if (parent != null && parent.getDepth() > 0) {
            errors.add(new ErrorResponse.ErrorDetail("depth", "DEPTH_EXCEEDED"));
        }

        if (!errors.isEmpty()) {
            throw new CustomException(CommentErrorCode.INVALID_INPUT_VALUE, errors);
        }
    }

    // 집단지성 생성을 요청한 유저가 존재한다면 불러오고, 존재하지 않는다면 오류 코드 반환
    private User getWriter(Long userId) {
        return userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
    }

    // 집단지성 entity 생성
    private Comment createComment(
            Post post,
            User writer,
            Comment parent,
            String content
    ) {
        LocalDateTime now = LocalDateTime.now();
        Long commentId = tsidGenerator.nextId();

        Comment comment = new Comment(
                commentId,
                post,
                writer,
                parent,
                parent == null ? 0 : parent.getDepth() + 1,
                content,
                now
        );

        return commentRepository.save(comment);
    }

    // 서비스 응답 생성하여 반환
    private CommentCreateResponse toCommentCreateResponse(
            Post post,
            User writer,
            Comment comment
    ) {
        boolean isPostWriter = writer.getId().equals(post.getWriter().getId());
        long helpfulMarkCount = getHelpfulMarkCount(comment.getId());
        Long parentId = comment.getParentComment() == null
                ? null
                : comment.getParentComment().getId();

        return new CommentCreateResponse(
                comment.getId(),
                comment.getContent(),
                true,
                new CommentWriterResponse(
                        writer.getNickname(),
                        writer.getProfileImageUrl(),
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

    // 집단지성의 유익해요 수 조회해 반환
    private long getHelpfulMarkCount(Long commentId) {
        return commentHelpfulMarkRepository.countById_CommentId(commentId);
    }
}
