package com.hogu.am_i_hogu.domain.user.service;

import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.common.exception.ErrorResponse;
import com.hogu.am_i_hogu.common.pagination.CursorCodec;
import com.hogu.am_i_hogu.common.pagination.CursorRequest;
import com.hogu.am_i_hogu.domain.comment.repository.CommentRepository;
import com.hogu.am_i_hogu.domain.user.dto.MyCommentCursor;
import com.hogu.am_i_hogu.domain.user.dto.MyCommentSummary;
import com.hogu.am_i_hogu.domain.user.dto.MyPostCursor;
import com.hogu.am_i_hogu.domain.user.dto.response.MyCommentItemResponse;
import com.hogu.am_i_hogu.domain.user.dto.response.MyCommentListResponse;
import com.hogu.am_i_hogu.domain.user.dto.response.MyCommentPostResponse;
import com.hogu.am_i_hogu.domain.user.dto.response.MyPostItemResponse;
import com.hogu.am_i_hogu.domain.user.exception.UserErrorCode;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

    public MyCommentListResponse getMyComments(Long userId, CursorRequest cursorRequest) {
        int pageSize = normalizePageSize(cursorRequest.pageSize());

        LocalDateTime cursorCreatedAt = null;
        Long cursorCommentId = null;

        if (cursorRequest.cursor() != null && !cursorRequest.cursor().isBlank()) {
            try {
                MyPostCursor decodedCursor = cursorCodec.decode(cursorRequest.cursor(), MyPostCursor.class);
                cursorCreatedAt = decodedCursor.createdAt();
                cursorCommentId = decodedCursor.postId();
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

        List<MyCommentSummary> queriedComments = commentRepository.findMyComments(
                userId,
                cursorCreatedAt,
                cursorCommentId,
                PageRequest.of(0, pageSize + 1)
        );

        boolean hasNext = queriedComments.size() > pageSize;
        List<MyCommentSummary> commentSummaries = hasNext
                ? queriedComments.subList(0, pageSize)
                : queriedComments;

        List<MyCommentItemResponse> comments = commentSummaries.stream()
                .map(summary -> new MyCommentItemResponse(
                        summary.commentId(),
                        summary.content(),
                        summary.createdAt(),
                        new MyCommentPostResponse(
                                summary.postId(),
                                summary.postTitle(),
                                summary.postIsDeleted()
                        )
                ))
                .toList();

        String nextCursor = null;
        if (hasNext && !commentSummaries.isEmpty()) {
            MyCommentSummary last = commentSummaries.get(commentSummaries.size() - 1);
            nextCursor = cursorCodec.encode(
                    new MyCommentCursor(last.createdAt(), last.commentId())
            );
        }

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
}
