package com.hogu.am_i_hogu.domain.user.service;

import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.common.exception.ErrorResponse;
import com.hogu.am_i_hogu.common.pagination.CursorCodec;
import com.hogu.am_i_hogu.common.pagination.CursorRequest;
import com.hogu.am_i_hogu.domain.post.repository.PostVoteRepository;
import com.hogu.am_i_hogu.domain.user.dto.MyVoteCursor;
import com.hogu.am_i_hogu.domain.user.dto.MyVoteSummary;
import com.hogu.am_i_hogu.domain.user.dto.response.*;
import com.hogu.am_i_hogu.domain.user.exception.UserErrorCode;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MyVoteQueryService {

    private static final int DEFAULT_PAGE_SIZE = 5;
    private static final int MAX_PAGE_SIZE = 15;

    private final CursorCodec cursorCodec;
    private final PostVoteRepository postVoteRepository;

    public MyVoteQueryService(
            CursorCodec cursorCodec,
            PostVoteRepository postVoteRepository
    ) {
        this.cursorCodec = cursorCodec;
        this.postVoteRepository = postVoteRepository;
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

        LocalDateTime cursorCreatedAt = null;
        Long cursorPostId = null;

        if (cursorRequest.cursor() != null && !cursorRequest.cursor().isBlank()) {
            try {
                MyVoteCursor decodedCursor = cursorCodec.decode(cursorRequest.cursor(), MyVoteCursor.class);
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

        List<MyVoteSummary> queriedVotes = postVoteRepository.findMyVotes(
                userId,
                cursorCreatedAt,
                cursorPostId,
                PageRequest.of(0, pageSize + 1)
        );

        boolean hasNext = queriedVotes.size() > pageSize;
        List<MyVoteSummary> voteSummaries = hasNext
                ? queriedVotes.subList(0, pageSize)
                : queriedVotes;

        List<MyVoteItemResponse> votes = voteSummaries.stream()
                .map(summary -> new MyVoteItemResponse(
                        summary.myVote(),
                        summary.createdAt(),
                        new MyVotePostResponse(
                                summary.postId(),
                                summary.postTitle(),
                                summary.postIsDeleted()
                        )
                ))
                .toList();

        String nextCursor = null;
        if (hasNext && !voteSummaries.isEmpty()) {
            MyVoteSummary last = voteSummaries.get(voteSummaries.size() - 1);
            nextCursor = cursorCodec.encode(
                    new MyVoteCursor(last.createdAt(), last.postId())
            );
        }

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
}
