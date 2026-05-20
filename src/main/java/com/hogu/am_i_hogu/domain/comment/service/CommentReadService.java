package com.hogu.am_i_hogu.domain.comment.service;

import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.common.exception.ErrorResponse;
import com.hogu.am_i_hogu.common.util.CursorCodec;
import com.hogu.am_i_hogu.domain.comment.dto.CommentCursor;
import com.hogu.am_i_hogu.domain.comment.dto.CommentInfo;
import com.hogu.am_i_hogu.domain.comment.dto.request.CursorRequest;
import com.hogu.am_i_hogu.domain.comment.dto.response.CommentItemResponse;
import com.hogu.am_i_hogu.domain.comment.dto.response.CommentReadResponse;
import com.hogu.am_i_hogu.domain.comment.dto.response.CommentWriterResponse;
import com.hogu.am_i_hogu.domain.comment.exception.CommentErrorCode;
import com.hogu.am_i_hogu.domain.comment.repository.CommentHelpfulMarkRepository;
import com.hogu.am_i_hogu.domain.comment.repository.CommentRepository;
import com.hogu.am_i_hogu.domain.post.domain.Post;
import com.hogu.am_i_hogu.domain.post.exception.PostErrorCode;
import com.hogu.am_i_hogu.domain.post.repository.PostRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class CommentReadService {

    public enum SortBy {
        OLDEST,
        LATEST,
        HELPFUL
    }

    private record QueryOptions(SortBy sortBy, CommentCursor cursor) {}
    private record Slice<T>(List<T> comments, boolean hasNext) {}

    private static final int DEFAULT_PAGE_SIZE = 5;
    private static final int MAX_PAGE_SIZE = 15;

    private final CursorCodec cursorCodec;
    private final CommentRepository commentRepository;
    private final CommentHelpfulMarkRepository commentHelpfulMarkRepository;
    private final PostRepository postRepository;

    public CommentReadService(
            CursorCodec cursorCodec,
            CommentRepository commentRepository,
            CommentHelpfulMarkRepository commentHelpfulMarkRepository,
            PostRepository postRepository
    ) {
        this.cursorCodec = cursorCodec;
        this.commentRepository = commentRepository;
        this.commentHelpfulMarkRepository = commentHelpfulMarkRepository;
        this.postRepository = postRepository;
    }

    public CommentReadResponse read(
            Long userId, Long postId, CursorRequest request
    ) {
        validatePost(postId);

        int pageSize = normalizePageSize(request.pageSize());
        QueryOptions readOptions = resolveQueryOptions(request);

        List<CommentInfo> queriedComments = getCommentsBySort(postId, readOptions.cursor, pageSize, readOptions.sortBy);
        Slice<CommentInfo> pageResult = sliceComments(queriedComments, pageSize);

        Set<Long> helpfulCommentIds = userId == null
                ? Set.of()
                : getHelpfulCommentIds(userId, pageResult.comments);
        List<CommentItemResponse> items = toResponse(pageResult.comments, userId, helpfulCommentIds);

        String nextCursor = createNextCursor(pageResult.hasNext, pageResult.comments, readOptions.sortBy);

        return new CommentReadResponse(items, pageResult.hasNext, nextCursor);

    }

    private void validatePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(PostErrorCode.POST_NOT_FOUND));

        if (post.isDeleted()) {
            throw new CustomException(PostErrorCode.POST_ALREADY_DELETED);
        }
    }

    private QueryOptions resolveQueryOptions(CursorRequest request) {
        List<ErrorResponse.ErrorDetail> errors = new ArrayList<>();

        SortBy sortBy = resolveSortBy(request.sortBy(), errors);
        CommentCursor cursor = validateCursor(request.cursor(), sortBy, errors);

        if (!errors.isEmpty()) {
            throw new CustomException(CommentErrorCode.INVALID_INPUT_VALUE, errors);
        }

        return new QueryOptions(sortBy, cursor);
    }

    // pageSize 검증하여 유효한 범위 내 값으로 조정
    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null) {
            return DEFAULT_PAGE_SIZE;
        }
        if (pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }

        return pageSize < MAX_PAGE_SIZE ? pageSize : MAX_PAGE_SIZE;
    }

    private SortBy resolveSortBy(String sortBy, List<ErrorResponse.ErrorDetail> errors) {
        if (sortBy == null || sortBy.isBlank()) {
            return SortBy.OLDEST;
        }
        if (sortBy.contains(",")) {
            errors.add(new ErrorResponse.ErrorDetail("sortBy", "MULTIPLE_SORTING"));
            return null;
        }

        return switch(sortBy) {
            case "OLDEST" -> SortBy.OLDEST;
            case "LATEST" -> SortBy.LATEST;
            case "HELPFUL" -> SortBy.HELPFUL;

            default -> {
                errors.add(new ErrorResponse.ErrorDetail("sortBy", "INVALID_SORTING"));
                yield null;
            }
        };
    }

    private Slice<CommentInfo> sliceComments(List<CommentInfo> queriedComments, int pageSize) {
        boolean hasNext = queriedComments.size() > pageSize;
        List<CommentInfo> comments = hasNext
                ? queriedComments.subList(0, pageSize)
                : queriedComments;

        return new Slice<>(comments, hasNext);
    }

    private CommentCursor validateCursor(String cursor, SortBy sortBy, List<ErrorResponse.ErrorDetail> errors) {
        CommentCursor decodedCursor = decodeCursor(cursor, errors);
        if (!errors.isEmpty()) {
            return null;
        }
        if (decodedCursor == null) {
            return null;
        }

        switch (sortBy) {
            case OLDEST -> {
                if (decodedCursor.createdAt() == null || decodedCursor.commentId() == null) {
                    errors.add(new ErrorResponse.ErrorDetail("cursor", "INVALID_CURSOR"));
                }
            }
            case LATEST -> {
                if (decodedCursor.createdAt() == null || decodedCursor.commentId() == null) {
                    errors.add(new ErrorResponse.ErrorDetail("cursor", "INVALID_CURSOR"));
                }
            }
            case HELPFUL -> {
                if (decodedCursor.totalHelpfulCount() == null || decodedCursor.commentId() == null) {
                    errors.add(new ErrorResponse.ErrorDetail("cursor", "INVALID_CURSOR"));
                }
            }
        }

        if (!errors.isEmpty()) {
            return null;
        }

        return decodedCursor;
    }

    // cursor 문자열을 디코딩하고, 유효하지 않은 경우 INVALID_PARAM_VALUE 예외로 변환
    private CommentCursor decodeCursor(String cursor, List<ErrorResponse.ErrorDetail> errors) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }

        try {
            return cursorCodec.decode(cursor, CommentCursor.class);
        } catch (IllegalStateException e) {
            errors.add(new ErrorResponse.ErrorDetail("cursor", "INVALID_CURSOR"));
            return null;
        }
    }

    private List<CommentInfo> getCommentsBySort(
            Long postId,
            CommentCursor cursor,
            int pageSize,
            SortBy sortBy
    ) {
        LocalDateTime cursorCreatedAt = cursor == null ? null : cursor.createdAt();
        Long cursorHelpfulCount = cursor == null ? null : cursor.totalHelpfulCount();
        Long cursorCommentId = cursor == null ? null : cursor.commentId();

        return switch (sortBy) {
            case OLDEST -> commentRepository.findParentCommentsByPostIdOrderByOldest(
                    postId,
                    cursorCreatedAt,
                    cursorCommentId,
                    PageRequest.of(0, pageSize + 1)
            );
            case LATEST -> commentRepository.findParentCommentsByPostIdOrderByLatest(
                    postId,
                    cursorCreatedAt,
                    cursorCommentId,
                    PageRequest.of(0, pageSize + 1)
            );
            case HELPFUL -> commentRepository.findParentCommentsByPostIdOrderByHelpful(
                    postId,
                    cursorHelpfulCount,
                    cursorCommentId,
                    PageRequest.of(0, pageSize + 1)
            );
        };
    }

    private Set<Long> getHelpfulCommentIds(Long userId, List<CommentInfo> comments) {
        List<Long> commentIds = comments.stream()
                        .map(CommentInfo::commentId)
                        .toList();

        return commentHelpfulMarkRepository.findHelpfulCommentIdsByUserIdAndCommentIds(userId, commentIds);
    }

    private CommentItemResponse toCommentItemResponse(
            CommentInfo comment,
            Long userId,
            Set<Long> helpfulCommentIds
    ) {
        boolean isDeleted = comment.isDeleted();
        String content = isDeleted ? null : comment.content();
        boolean isMine = !isDeleted && userId != null && userId.equals(comment.writerId());
        boolean isHelpful = !isDeleted && helpfulCommentIds.contains(comment.commentId());
        long totalHelpfulCount = isDeleted ? 0 : comment.totalHelpfulCount();

        return new CommentItemResponse(
                comment.commentId(),
                content,
                isMine,
                new CommentWriterResponse(
                        comment.writerNickname(),
                        comment.writerProfileImageUrl(),
                        comment.isPostWriter()
                ),
                comment.createdAt(),
                comment.updatedAt(),
                isDeleted,
                isHelpful,
                totalHelpfulCount,
                comment.parentId(),
                comment.depth()
        );
    }

    private List<CommentItemResponse> toResponse(
            List<CommentInfo> comments,
            Long userId,
            Set<Long> helpfulCommentIds
    ) {
        return comments.stream()
                .map(comment -> toCommentItemResponse(comment, userId, helpfulCommentIds))
                .toList();
    }

    // 마지막 댓글 정보를 기준으로 다음 페이지 요청에 사용할 nextCursor 생성
    private String createNextCursor(boolean hasNext, List<CommentInfo> comments, SortBy sortBy) {
        if (!hasNext || comments.isEmpty()) {
            return null;
        }

        CommentInfo last = comments.get(comments.size() - 1);
        return switch (sortBy) {
            case OLDEST -> cursorCodec.encode(new CommentCursor(last.createdAt(), null, last.commentId()));
            case LATEST -> cursorCodec.encode(new CommentCursor(last.createdAt(), null, last.commentId()));
            case HELPFUL -> cursorCodec.encode(new CommentCursor(null, last.totalHelpfulCount(), last.commentId()));
        };
    }
}
