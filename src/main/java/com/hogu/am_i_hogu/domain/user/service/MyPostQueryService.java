package com.hogu.am_i_hogu.domain.user.service;

import com.hogu.am_i_hogu.common.pagination.CursorCodec;
import com.hogu.am_i_hogu.common.pagination.CursorRequest;
import com.hogu.am_i_hogu.domain.comment.dto.PostCommentCount;
import com.hogu.am_i_hogu.domain.comment.repository.CommentRepository;
import com.hogu.am_i_hogu.domain.post.repository.PostRepository;
import com.hogu.am_i_hogu.domain.user.domain.VoteSummary;
import com.hogu.am_i_hogu.domain.user.dto.MyPostCursor;
import com.hogu.am_i_hogu.domain.user.dto.MyPostSummary;
import com.hogu.am_i_hogu.domain.user.dto.response.MyPostItemResponse;
import com.hogu.am_i_hogu.domain.user.dto.response.MyPostListResponse;
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

        LocalDateTime cursorCreatedAt = null;
        Long cursorPostId = null;
        if (cursorRequest.cursor() != null && !cursorRequest.cursor().isBlank()) {
            MyPostCursor decodedCursor = cursorCodec.decode(cursorRequest.cursor(), MyPostCursor.class);
            cursorCreatedAt = decodedCursor.createdAt();
            cursorPostId = decodedCursor.postId();
        }

        List<MyPostSummary> queriedPosts = postRepository.findMyPosts(
                userId,
                cursorCreatedAt,
                cursorPostId,
                PageRequest.of(0, pageSize + 1)
        );

        boolean hasNext = queriedPosts.size() > pageSize;
        List<MyPostSummary> postSummaries = hasNext ? queriedPosts.subList(0, pageSize) : queriedPosts;

        Map<Long, Long> commentCounts = getCommentCounts(postSummaries);

        List<MyPostItemResponse> posts = postSummaries.stream()
                .map(summary -> new MyPostItemResponse(
                        summary.postId(),
                        summary.title(),
                        summary.createdAt(),
                        toVoteSummary(summary.hoguCount(), summary.notHoguCount()),
                        commentCounts.getOrDefault(summary.postId(), 0L)
                ))
                .toList();

        String nextCursor = null;
        if (hasNext && !postSummaries.isEmpty()) {
            MyPostSummary last = postSummaries.get(postSummaries.size()-1);
            nextCursor = cursorCodec.encode(new MyPostCursor(last.createdAt(), last.postId()));
        }

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
