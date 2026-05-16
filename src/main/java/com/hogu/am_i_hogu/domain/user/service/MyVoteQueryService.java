package com.hogu.am_i_hogu.domain.user.service;

import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.common.exception.ErrorResponse;
import com.hogu.am_i_hogu.common.pagination.CursorCodec;
import com.hogu.am_i_hogu.common.pagination.CursorRequest;
import com.hogu.am_i_hogu.domain.comment.dto.PostCommentCount;
import com.hogu.am_i_hogu.domain.comment.repository.CommentRepository;
import com.hogu.am_i_hogu.domain.post.repository.PostVoteRepository;
import com.hogu.am_i_hogu.domain.user.dto.MyVoteCursor;
import com.hogu.am_i_hogu.domain.user.dto.MyVoteSummary;
import com.hogu.am_i_hogu.domain.user.dto.response.MyVoteItemResponse;
import com.hogu.am_i_hogu.domain.user.dto.response.MyVoteListResponse;
import com.hogu.am_i_hogu.domain.user.dto.response.MyVotePostResponse;
import com.hogu.am_i_hogu.domain.user.exception.UserErrorCode;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MyVoteQueryService {

    private static final int DEFAULT_PAGE_SIZE = 5;
    private static final int MAX_PAGE_SIZE = 15;

    private final CursorCodec cursorCodec;
    private final PostVoteRepository postVoteRepository;
    private final CommentRepository commentRepository;

    public MyVoteQueryService(
            CursorCodec cursorCodec,
            PostVoteRepository postVoteRepository,
            CommentRepository commentRepository
    ) {
        this.cursorCodec = cursorCodec;
        this.postVoteRepository = postVoteRepository;
        this.commentRepository = commentRepository;
    }

    /**
     * 본인이 참여한 투표 리스트 조회
     *
     * @param userId        조회 요청한 사용자 id
     * @param cursorRequest cursor 정보(투표 생성 일시, post id 포함)
     * @return 조회된 투표 리스트
     */
    public MyVoteListResponse getMyVotes(Long userId, CursorRequest cursorRequest) {
        int pageSize = normalizePageSize(cursorRequest.pageSize());
        MyVoteCursor cursor = decodeCursor(cursorRequest.cursor());
        List<MyVoteSummary> queriedVotes = findVotes(userId, cursor, pageSize);

        boolean hasNext = queriedVotes.size() > pageSize;
        List<MyVoteSummary> voteSummaries = hasNext
                ? queriedVotes.subList(0, pageSize)
                : queriedVotes;

        Map<Long, Long> commentCounts = getCommentCounts(voteSummaries);
        List<MyVoteItemResponse> votes = mapToResponses(voteSummaries, commentCounts);
        String nextCursor = createNextCursor(hasNext, voteSummaries);

        return new MyVoteListResponse(votes, hasNext, nextCursor);
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
    private MyVoteCursor decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }

        try {
            return cursorCodec.decode(cursor, MyVoteCursor.class);
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

    // cursor 정보와 pageSize를 이용해 참여한 투표 목록을 조회
    private List<MyVoteSummary> findVotes(Long userId, MyVoteCursor cursor, int pageSize) {
        LocalDateTime cursorCreatedAt = cursor == null ? null : cursor.createdAt();
        Long cursorPostId = cursor == null ? null : cursor.postId();

        return postVoteRepository.findMyVotes(
                userId,
                cursorCreatedAt,
                cursorPostId,
                PageRequest.of(0, pageSize + 1)
        );
    }

    // 각 게시글 댓글 수를 조회 후 Map(게시글ID : 댓글 수) 형태로 변환
    private Map<Long, Long> getCommentCounts(List<MyVoteSummary> voteSummaries) {
        List<Long> postIds = voteSummaries.stream()
                .map(MyVoteSummary::postId)
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

    // 조회한 투표 요약 정보와 댓글 수를 최종 응답 DTO 리스트로 변환
    private List<MyVoteItemResponse> mapToResponses(
            List<MyVoteSummary> voteSummaries,
            Map<Long, Long> commentCounts
    ) {
        return voteSummaries.stream()
                .map(summary -> new MyVoteItemResponse(
                        summary.myVote(),
                        summary.createdAt(),
                        new MyVotePostResponse(
                                summary.postId(),
                                summary.postTitle(),
                                summary.postCategory(),
                                commentCounts.getOrDefault(summary.postId(), 0L),
                                summary.postIsDeleted()
                        )
                ))
                .toList();
    }

    // 마지막 투표 정보를 기준으로 다음 페이지 요청에 사용할 nextCursor 생성
    private String createNextCursor(boolean hasNext, List<MyVoteSummary> voteSummaries) {
        if (!hasNext || voteSummaries.isEmpty()) {
            return null;
        }

        MyVoteSummary last = voteSummaries.get(voteSummaries.size() - 1);
        return cursorCodec.encode(new MyVoteCursor(last.createdAt(), last.postId()));
    }
}
