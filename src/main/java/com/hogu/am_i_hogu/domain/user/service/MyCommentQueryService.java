package com.hogu.am_i_hogu.domain.user.service;

import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.common.exception.ErrorResponse;
import com.hogu.am_i_hogu.common.util.CursorCodec;
import com.hogu.am_i_hogu.domain.user.dto.request.CursorRequest;
import com.hogu.am_i_hogu.domain.comment.repository.CommentRepository;
import com.hogu.am_i_hogu.domain.comment.dto.PostCommentCount;
import com.hogu.am_i_hogu.domain.user.dto.MyCommentCursor;
import com.hogu.am_i_hogu.domain.user.dto.MyCommentSummary;
import com.hogu.am_i_hogu.domain.user.dto.response.MyCommentItemResponse;
import com.hogu.am_i_hogu.domain.user.dto.response.MyCommentListResponse;
import com.hogu.am_i_hogu.domain.user.dto.response.MyCommentPostResponse;
import com.hogu.am_i_hogu.domain.user.exception.UserErrorCode;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MyCommentQueryService {

    private static final int DEFAULT_PAGE_SIZE = 5;
    private static final int MAX_PAGE_SIZE = 15;

    private final CursorCodec cursorCodec;
    private final CommentRepository commentRepository;

    public MyCommentQueryService(
            CursorCodec cursorCodec,
            CommentRepository commentRepository
    ) {
        this.cursorCodec = cursorCodec;
        this.commentRepository = commentRepository;
    }

    /**
     * 본인이 작성한 댓글 리스트 조회
     * 각 댓글마다 [댓글 id, 내용, 생성 일시, 관련 게시물 정보(게시물 id, 제목, 삭제 여부) 포함
     *
     * @param userId        조회 요청한 사용자 id
     * @param cursorRequest cursor 정보(comment 생성 일시, comment id 포함)
     * @return 조회된 댓글 리스트
     */
    public MyCommentListResponse getMyComments(Long userId, CursorRequest cursorRequest) {
        int pageSize = normalizePageSize(cursorRequest.pageSize());
        MyCommentCursor cursor = decodeCursor(cursorRequest.cursor());
        List<MyCommentSummary> queriedComments = findComments(userId, cursor, pageSize);

        boolean hasNext = queriedComments.size() > pageSize;
        List<MyCommentSummary> commentSummaries = hasNext
                ? queriedComments.subList(0, pageSize)
                : queriedComments;

        Map<Long, Long> commentCounts = getCommentCounts(commentSummaries);
        List<MyCommentItemResponse> comments = mapToResponses(commentSummaries, commentCounts);
        String nextCursor = createNextCursor(hasNext, commentSummaries);

        return new MyCommentListResponse(comments, hasNext, nextCursor);
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

    // cursor 문자열을 디코딩하고, 유효하지 않은 경우 INVALID_PARAM_VALUE 예외로 변환
    private MyCommentCursor decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }

        try {
            return cursorCodec.decode(cursor, MyCommentCursor.class);
        } catch (IllegalStateException e) {
            throw new CustomException(
                    UserErrorCode.INVALID_PARAM_VALUE,
                    List.of(new ErrorResponse.ErrorDetail(
                            "cursor",
                            "INVALID_CURSOR"
                    ))
            );
        }
    }

    // cursor 정보와 pageSize를 이용해 작성한 댓글 목록을 조회
    private List<MyCommentSummary> findComments(Long userId, MyCommentCursor cursor, int pageSize) {
        LocalDateTime cursorCreatedAt = cursor == null ? null : cursor.createdAt();
        Long cursorCommentId = cursor == null ? null : cursor.commentId();

        return commentRepository.findMyComments(
                userId,
                cursorCreatedAt,
                cursorCommentId,
                PageRequest.of(0, pageSize + 1)
        );
    }

    // 각 게시글 댓글 수를 조회 후 Map(게시글ID : 댓글 수) 형태로 변환
    private Map<Long, Long> getCommentCounts(List<MyCommentSummary> commentSummaries) {
        List<Long> postIds = commentSummaries.stream()
                .map(MyCommentSummary::postId)
                .toList();

        if (postIds.isEmpty()) {
            return Map.of();
        }

        return commentRepository.countCommentsGroupedByPostIds(postIds).stream()
                .collect(Collectors.toMap(
                        PostCommentCount::postId,
                        PostCommentCount::commentCount
                ));
    }

    // 조회한 댓글 요약 정보와 댓글 수를 최종 응답 DTO 리스트로 변환
    private List<MyCommentItemResponse> mapToResponses(
            List<MyCommentSummary> commentSummaries,
            Map<Long, Long> commentCounts
    ) {
        return commentSummaries.stream()
                .map(summary -> new MyCommentItemResponse(
                        summary.commentId(),
                        summary.content(),
                        summary.createdAt(),
                        new MyCommentPostResponse(
                                summary.postId(),
                                summary.postTitle(),
                                summary.postCategory(),
                                commentCounts.getOrDefault(summary.postId(), 0L),
                                summary.postIsDeleted()
                        )
                ))
                .toList();
    }

    // 마지막 댓글 정보를 기준으로 다음 페이지 요청에 사용할 nextCursor 생성
    private String createNextCursor(boolean hasNext, List<MyCommentSummary> commentSummaries) {
        if (!hasNext || commentSummaries.isEmpty()) {
            return null;
        }

        MyCommentSummary last = commentSummaries.get(commentSummaries.size() - 1);
        return cursorCodec.encode(new MyCommentCursor(last.createdAt(), last.commentId()));
    }
}
