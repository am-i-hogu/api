package com.hogu.am_i_hogu.domain.user.service;

import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.common.exception.ErrorResponse;
import com.hogu.am_i_hogu.common.util.CursorCodec;
import com.hogu.am_i_hogu.domain.user.dto.request.CursorRequest;
import com.hogu.am_i_hogu.domain.comment.dto.PostCommentCount;
import com.hogu.am_i_hogu.domain.comment.repository.CommentRepository;
import com.hogu.am_i_hogu.domain.post.repository.PostRepository;
import com.hogu.am_i_hogu.domain.user.dto.MyPostCursor;
import com.hogu.am_i_hogu.domain.user.dto.MyPostSummary;
import com.hogu.am_i_hogu.domain.user.dto.response.MyPostItemResponse;
import com.hogu.am_i_hogu.domain.user.dto.response.MyPostListResponse;
import com.hogu.am_i_hogu.domain.user.exception.UserErrorCode;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MyPostQueryService {

    private static final int DEFAULT_PAGE_SIZE = 5;
    private static final int MAX_PAGE_SIZE = 15;

    private static final String HOGU_VOTE = "HOGU";
    private static final String NOT_HOGU_VOTE = "NOT_HOGU";
    private static final String NONE_VOTE = "NONE";
    private static final String TIE_VOTE = "TIE";

    private final CursorCodec cursorCodec;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    public MyPostQueryService(
            CursorCodec cursorCodec,
            PostRepository postRepository,
            CommentRepository commentRepository
    ) {
        this.cursorCodec = cursorCodec;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
    }

    /**
     * 본인이 작성한 게시물 리스트 조회
     * 각 게시물마다 [게시물 id, 제목, 생성 일시, 투표 요약, 댓글 수] 포함
     *
     * @param userId        조회 요청한 사용자 id
     * @param cursorRequest cursor 정보(post 생성 일시, post id 포함)
     * @return 조회된 게시물 리스트
     */
    public MyPostListResponse getMyPosts(Long userId, CursorRequest cursorRequest) {
        int pageSize = normalizePageSize(cursorRequest.pageSize());
        MyPostCursor cursor = decodeCursor(cursorRequest.cursor());
        List<MyPostSummary> queriedPosts = findPosts(userId, cursor, pageSize);

        boolean hasNext = queriedPosts.size() > pageSize;
        List<MyPostSummary> postSummaries = hasNext
                ? queriedPosts.subList(0, pageSize)
                : queriedPosts;

        Map<Long, Long> commentCounts = getCommentCounts(postSummaries);
        List<MyPostItemResponse> posts = mapToResponses(postSummaries, commentCounts);
        String nextCursor = createNextCursor(hasNext, postSummaries);

        return new MyPostListResponse(posts, hasNext, nextCursor);
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
    private MyPostCursor decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }

        try {
            return cursorCodec.decode(cursor, MyPostCursor.class);
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

    // cursor 정보와 pageSize를 이용해 작성한 게시물 목록을 조회
    private List<MyPostSummary> findPosts(Long userId, MyPostCursor cursor, int pageSize) {
        LocalDateTime cursorCreatedAt = cursor == null ? null : cursor.createdAt();
        Long cursorPostId = cursor == null ? null : cursor.postId();

        return postRepository.findMyPosts(
                userId,
                cursorCreatedAt,
                cursorPostId,
                PageRequest.of(0, pageSize + 1)
        );
    }

    // 각 게시글 댓글 수를 조회 후 Map(게시글ID : 댓글 수) 형태로 변환
    private Map<Long, Long> getCommentCounts(List<MyPostSummary> postSummary) {
        List<Long> postIds = postSummary.stream()
                .map(MyPostSummary::postId)
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

    // 조회한 게시물 요약 정보와 댓글 수를 최종 응답 DTO 리스트로 변환
    private List<MyPostItemResponse> mapToResponses(
            List<MyPostSummary> postSummaries,
            Map<Long, Long> commentCounts
    ) {
        return postSummaries.stream()
                .map(summary -> new MyPostItemResponse(
                        summary.postId(),
                        summary.title(),
                        summary.category(),
                        summary.createdAt(),
                        toVoteSummary(summary.hoguCount(), summary.notHoguCount()),
                        commentCounts.getOrDefault(summary.postId(), 0L)
                ))
                .toList();
    }

    // 마지막 게시물 정보를 기준으로 다음 페이지 요청에 사용할 nextCursor 생성
    private String createNextCursor(boolean hasNext, List<MyPostSummary> postSummaries) {
        if (!hasNext || postSummaries.isEmpty()) {
            return null;
        }

        MyPostSummary last = postSummaries.get(postSummaries.size() - 1);
        return cursorCodec.encode(new MyPostCursor(last.createdAt(), last.postId()));
    }

    // 'HOGU'와 'NOT_HOGU' 수를 비교하여 투표 결과 생성
    private String toVoteSummary(long hoguCount, long notHoguCount) {
        if (hoguCount == 0 && notHoguCount == 0) {
            return NONE_VOTE;
        }
        if (hoguCount > notHoguCount) {
            return HOGU_VOTE;
        }
        if (hoguCount < notHoguCount) {
            return NOT_HOGU_VOTE;
        }
        return TIE_VOTE;
    }
}
