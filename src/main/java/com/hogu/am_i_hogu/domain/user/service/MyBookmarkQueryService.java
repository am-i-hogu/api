package com.hogu.am_i_hogu.domain.user.service;

import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.common.exception.ErrorResponse;
import com.hogu.am_i_hogu.common.pagination.CursorCodec;
import com.hogu.am_i_hogu.common.pagination.CursorRequest;
import com.hogu.am_i_hogu.domain.comment.dto.PostCommentCount;
import com.hogu.am_i_hogu.domain.comment.repository.CommentRepository;
import com.hogu.am_i_hogu.domain.post.repository.PostBookmarkRepository;
import com.hogu.am_i_hogu.domain.user.dto.MyBookmarkCursor;
import com.hogu.am_i_hogu.domain.user.dto.MyBookmarkSummary;
import com.hogu.am_i_hogu.domain.user.dto.response.MyBookmarkListResponse;
import com.hogu.am_i_hogu.domain.user.dto.response.MyPostItemResponse;
import com.hogu.am_i_hogu.domain.user.dto.response.VoteSummary;
import com.hogu.am_i_hogu.domain.user.exception.UserErrorCode;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MyBookmarkQueryService {

    private static final int DEFAULT_PAGE_SIZE = 5;
    private static final int MAX_PAGE_SIZE = 15;

    private final CursorCodec cursorCodec;
    private final PostBookmarkRepository postBookmarkRepository;
    private final CommentRepository commentRepository;

    public MyBookmarkQueryService(CursorCodec cursorCodec, PostBookmarkRepository postBookmarkRepository, CommentRepository commentRepository) {
        this.cursorCodec = cursorCodec;
        this.postBookmarkRepository = postBookmarkRepository;
        this.commentRepository = commentRepository;
    }

    /**
     * 본인이 북마크한 게시물 리스트 조회
     * 각 게시물마다 [게시물 id, 제목, 생성 일시, 투표 요약, 댓글 수] 포함
     *
     * @param userId        조회 요청한 사용자 id
     * @param cursorRequest cursor 정보(bookmark 생성 일시, post id 포함)
     * @return 조회된 게시물 리스트
     */
    public MyBookmarkListResponse getMyBookmarks(Long userId, CursorRequest cursorRequest) {
        int pageSize = normalizePageSize(cursorRequest.pageSize());

        LocalDateTime cursorCreatedAt = null;
        Long cursorPostId = null;

        if (cursorRequest.cursor() != null && !cursorRequest.cursor().isBlank()) {
            try {
                MyBookmarkCursor decodedCursor = cursorCodec.decode(cursorRequest.cursor(), MyBookmarkCursor.class);
                cursorCreatedAt = decodedCursor.createdAt();
                cursorPostId = decodedCursor.postId();
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

        List<MyBookmarkSummary> queriedBookmarks = postBookmarkRepository.findMyBookmarks(
                userId,
                cursorCreatedAt,
                cursorPostId,
                PageRequest.of(0, pageSize + 1)
        );

        boolean hasNext = queriedBookmarks.size() > pageSize;
        List<MyBookmarkSummary> bookmarkSummaries = hasNext
                ? queriedBookmarks.subList(0, pageSize)
                : queriedBookmarks;

        Map<Long, Long> commentCounts = getCommentCounts(bookmarkSummaries);

        List<MyPostItemResponse> posts = bookmarkSummaries.stream()
                .map(summary -> new MyPostItemResponse(
                        summary.postId(),
                        summary.title(),
                        summary.postCreatedAt(),
                        toVoteSummary(summary.hoguCount(), summary.notHoguCount()),
                        commentCounts.getOrDefault(summary.postId(), 0L)
                ))
                .toList();

        String nextCursor = null;
        if (hasNext && !bookmarkSummaries.isEmpty()) {
            MyBookmarkSummary last = bookmarkSummaries.get(bookmarkSummaries.size() - 1);
            nextCursor = cursorCodec.encode(
                    new MyBookmarkCursor(last.bookmarkCreatedAt(), last.postId())
            );
        }

        return new MyBookmarkListResponse(posts, hasNext, nextCursor);
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

    // 각 게시글 댓글 수를 조회 후 Map(게시글ID : 댓글 수) 형태로 변환
    private Map<Long, Long> getCommentCounts(List<MyBookmarkSummary> bookmarkSummaries) {
        List<Long> postIds = bookmarkSummaries.stream()
                .map(MyBookmarkSummary::postId)
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

    // 'HOGU'와 'NOT_HOGU' 수를 비교하여 투표 결과 생성
    private VoteSummary toVoteSummary(long hoguCount, long notHoguCount) {
        if (hoguCount == 0 && notHoguCount == 0) {
            return VoteSummary.NONE;
        }
        if (hoguCount > notHoguCount) {
            return VoteSummary.HOGU;
        }
        if (hoguCount < notHoguCount) {
            return VoteSummary.NOT_HOGU;
        }
        return VoteSummary.TIE;
    }
}
