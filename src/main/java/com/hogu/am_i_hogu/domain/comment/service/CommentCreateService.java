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

        User writer = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
        Comment savedComment = createComment(post, writer, parent, request.content());

        return toCommentCreateResponse(post, writer, savedComment);
    }

    private Post getPostOrThrow(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(PostErrorCode.POST_NOT_FOUND));

        if (post.isDeleted()) {
            throw new CustomException(PostErrorCode.POST_ALREADY_DELETED);
        }

        return post;
    }

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

    private CommentCreateResponse toCommentCreateResponse(
            Post post,
            User writer,
            Comment comment
    ) {
        boolean isPostWriter = writer.getId().equals(post.getWriter().getId());
        int helpfulMarkCount = getHelpfulMarkCount(comment.getId());
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

    private int getHelpfulMarkCount(Long commentId) {
        return Math.toIntExact(commentHelpfulMarkRepository.countById_CommentId(commentId));
    }
}
