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
import java.util.stream.Collectors;

@Service
public class CommentReadService {

    public enum SortBy {
        LATEST,
        HELPFUL
    }

    private record QueryOptions(SortBy sortBy, int pageSize, CommentCursor cursor) {}
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

    /**
     * 집단지성 조회
     *
     * @param userId    조회 요청한 유저 id(필수 X)
     * @param postId    집단지성이 포함된 게시물 id
     * @param request   쿼리 파라미터(sortBy, pageSize, cursor)
     * @return  조회된 집단지성 리스트
     */
    public CommentReadResponse read(Long userId, Long postId, CursorRequest request) {
        validatePost(postId);
        QueryOptions readOptions = resolveQueryOptions(request);

        /**
         * 집단지성 정보 조회:
         * - (1) pageSize만큼의 부모 집단지성을 조회
         * - (2) 불러온 부모 집단지성과 연결된 자식 집단지성 모두 조회
         * - (3) 부모 집단지성, 자식 집단지성 정렬 - 부모 집단지성은 sortBy에 따라 정렬하고, 자식 집단지성은 부모 집단지성 하위에 오래된 순으로 정렬
         * - (4) 불러온 집단지성들의 유익해요 수 조회
         */
        Slice<CommentInfo> parentComments = getParentCommentsBySort(postId, readOptions);
        List<CommentInfo> childComments = getChildComments(parentComments.comments());
        List<CommentInfo> orderedComments = mergeParentAndChildren(parentComments.comments(), childComments);
        Set<Long> helpfulCommentIds = getHelpfulCommentIds(userId, orderedComments);

        List<CommentItemResponse> comments = toResponse(orderedComments, userId, helpfulCommentIds);
        String nextCursor = createNextCursor(parentComments.hasNext(), parentComments.comments(), readOptions.sortBy());

        return new CommentReadResponse(comments, parentComments.hasNext(), nextCursor);

    }

    // postId로 게시물 정보를 조회하고, 존재하지 않거나 삭제된 게시물이면 오류 코드 반환
    private void validatePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(PostErrorCode.POST_NOT_FOUND));

        if (post.isDeleted()) {
            throw new CustomException(PostErrorCode.POST_ALREADY_DELETED);
        }
    }

    /**
     * 쿼리 파라미터 처리 후 반환:
     * - (1) pageSize 검증 및 조정
     * - (2) sortBy 검증 및 결정
     * - (3) cursor 검증 및 디코딩
     *
     * @param request 쿼리 파라미터
     * @return 검증 및 확정된 쿼리 파라미터 정보
     */
    private QueryOptions resolveQueryOptions(CursorRequest request) {
        int pageSize = normalizePageSize(request.pageSize());

        List<ErrorResponse.ErrorDetail> errors = new ArrayList<>();
        SortBy sortBy = resolveSortBy(request.sortBy(), errors);
        CommentCursor cursor = validateCursor(request.cursor(), sortBy, errors);

        if (!errors.isEmpty()) {
            throw new CustomException(CommentErrorCode.INVALID_PARAM_VALUE, errors);
        }

        return new QueryOptions(sortBy, pageSize, cursor);
    }

    // pageSize 검증하여 유효한 범위 내 값으로 조정해 반환
    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null) {
            return DEFAULT_PAGE_SIZE;
        }
        if (pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }

        return pageSize < MAX_PAGE_SIZE ? pageSize : MAX_PAGE_SIZE;
    }

    // sortBy 검증하여 유효한 정렬 방식으로 확정해 반환
    private SortBy resolveSortBy(String sortBy, List<ErrorResponse.ErrorDetail> errors) {
        if (sortBy == null || sortBy.isBlank()) {
            return SortBy.LATEST;
        }
        if (sortBy.contains(",")) {
            errors.add(new ErrorResponse.ErrorDetail("sortBy", "MULTIPLE_SORTING"));
            return null;
        }

        return switch(sortBy) {
            case "LATEST" -> SortBy.LATEST;
            case "HELPFUL" -> SortBy.HELPFUL;

            default -> {
                errors.add(new ErrorResponse.ErrorDetail("sortBy", "INVALID_SORTING"));
                yield null;
            }
        };
    }

    // 다음 쿼리에 불러온 집단지성이 있는지 확인 후 Slice 정보 반환
    private Slice<CommentInfo> sliceComments(List<CommentInfo> queriedComments, int pageSize) {
        boolean hasNext = queriedComments.size() > pageSize;
        List<CommentInfo> comments = hasNext
                ? queriedComments.subList(0, pageSize)
                : queriedComments;

        return new Slice<>(comments, hasNext);
    }

    // 부모 집단지성 리스트 하위의 모든 자식 집단지성 리스트 조회해 반환
    private List<CommentInfo> getChildComments(List<CommentInfo> parentComments) {
        List<Long> parentIds = parentComments.stream()
                .map(CommentInfo::commentId)
                .toList();
        if (parentIds.isEmpty()) {
            return List.of();
        }

        return commentRepository.findChildCommentsByParentIds(parentIds);
    }

    // 부모 집단지성과 자식 집단지성 그룹핑하여 정렬된 집단지성 리스트 반환
    private List<CommentInfo> mergeParentAndChildren(List<CommentInfo> parentComments, List<CommentInfo> childComments) {
        Map<Long, List<CommentInfo>> childrenByParentId = childComments.stream()
                .collect(Collectors.groupingBy(CommentInfo::parentId));

        List<CommentInfo> ordered = new ArrayList<>();
        for (CommentInfo parent : parentComments) {
            ordered.add(parent);
            ordered.addAll(childrenByParentId.getOrDefault(parent.commentId(), List.of()));
        }

        return ordered;
    }

    // cursor 정보 디코딩 및 검증하여 디코딩된 cursor 또는 오류 코드 반환
    private CommentCursor validateCursor(String cursor, SortBy sortBy, List<ErrorResponse.ErrorDetail> errors) {
        CommentCursor decodedCursor = decodeCursor(cursor, errors);
        if (!errors.isEmpty()) {
            return null;
        }
        if (decodedCursor == null) {
            return null;
        }

        switch (sortBy) {
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

    // 요청으로 들어온 정렬 방식에 따라 부모 집단지성 조회해 반환
    private Slice<CommentInfo> getParentCommentsBySort(Long postId, QueryOptions readOptions) {
        SortBy sortBy = readOptions.sortBy();
        int pageSize = readOptions.pageSize();
        CommentCursor cursor = readOptions.cursor();

        LocalDateTime cursorCreatedAt = cursor == null ? null : cursor.createdAt();
        Long cursorHelpfulCount = cursor == null ? null : cursor.totalHelpfulCount();
        Long cursorCommentId = cursor == null ? null : cursor.commentId();

        List<CommentInfo> queriedParents = switch (sortBy) {
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

        return sliceComments(queriedParents, pageSize);
    }

    // 사용자가 유익해요를 누른 집단지성 id 반환
    private Set<Long> getHelpfulCommentIds(Long userId, List<CommentInfo> comments) {
        if (userId == null) {
            return Set.of();
        }

        List<Long> commentIds = comments.stream()
                .map(CommentInfo::commentId)
                .toList();

        return commentHelpfulMarkRepository.findHelpfulCommentIdsByUserIdAndCommentIds(userId, commentIds);
    }

    // 각 집단지성 정보 생성하여 반환
    private CommentItemResponse toCommentItemResponse(
            CommentInfo comment,
            Long userId,
            Set<Long> helpfulCommentIds
    ) {
        boolean isDeleted = comment.isDeleted();
        String content = isDeleted ? null : comment.content();
        boolean isMine = !isDeleted && userId != null && userId.equals(comment.writerId());
        boolean isHelpful = !isDeleted && helpfulCommentIds.contains(comment.commentId());
        long totalHelpfulCount = comment.totalHelpfulCount();

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

    // 서비스 응답 생성하여 반환
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
            case LATEST -> cursorCodec.encode(new CommentCursor(last.createdAt(), null, last.commentId()));
            case HELPFUL -> cursorCodec.encode(new CommentCursor(null, last.totalHelpfulCount(), last.commentId()));
        };
    }
}
