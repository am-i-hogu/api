package com.hogu.am_i_hogu.domain.post.service;

import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.domain.post.domain.ImageAsset;
import com.hogu.am_i_hogu.domain.post.domain.Post;
import com.hogu.am_i_hogu.domain.post.domain.PostVote;
import com.hogu.am_i_hogu.domain.post.dto.response.PostDetailResponse;
import com.hogu.am_i_hogu.domain.post.dto.response.PostVoteResponse;
import com.hogu.am_i_hogu.domain.post.dto.response.PostWriterResponse;
import com.hogu.am_i_hogu.domain.post.exception.PostErrorCode;
import com.hogu.am_i_hogu.domain.post.repository.ImageAssetRepository;
import com.hogu.am_i_hogu.domain.post.repository.PostRepository;
import com.hogu.am_i_hogu.domain.post.repository.PostVoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostDetailService {
    private static final String HOGU_VOTE = "HOGU";
    private static final String NOT_HOGU_VOTE = "NOT_HOGU";
    private static final String NONE_VOTE = "NONE";

    private final PostRepository postRepository;
    private final ImageAssetRepository imageAssetRepository;
    private final PostVoteRepository postVoteRepository;

    /**
     * 게시글 상세 정보를 조회한다.
     *
     * @param postId 조회할 게시글 ID
     * @param viewerUserId 현재 조회 중인 사용자 ID, 비회원이면 null
     * @return 게시글 상세 응답
     */
    public PostDetailResponse getDetail(Long postId, Long viewerUserId) {
        Post post = getPostOrThrow(postId);
        validateNotDeleted(post);

        List<String> imageUrls = getImageUrls(postId);
        boolean isMine = isMine(post, viewerUserId);
        PostVoteResponse vote = getVoteResponse(postId, viewerUserId);

        return getPostDetailResponse(post, imageUrls, isMine, vote);
    }

    /**
     * 게시글 ID로 게시글을 조회한다.
     *
     * @param postId 조회할 게시글 ID
     * @return 조회된 게시글 엔티티
     */
    private Post getPostOrThrow(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(PostErrorCode.POST_NOT_FOUND));
    }

    /**
     * 삭제된 게시글인지 확인한다.
     *
     * @param post 삭제 여부를 확인할 게시글
     */
    private void validateNotDeleted(Post post) {
        if (post.isDeleted()) {
            throw new CustomException(PostErrorCode.POST_ALREADY_DELETED);
        }
    }

    /**
     * 게시글에 연결된 이미지 URL 목록을 정렬 순서대로 조회한다.
     *
     * @param postId 이미지를 조회할 게시글 ID
     * @return 정렬된 이미지 URL 목록
     */
    private List<String> getImageUrls(Long postId) {
        return imageAssetRepository.findByPost_IdOrderBySortOrderAsc(postId)
                .stream()
                .map(ImageAsset::getUrl)
                .toList();
    }

    /**
     * 게시글의 투표 집계와 현재 조회자의 투표 값을 조회한다.
     *
     * @param postId 투표를 조회할 게시글 ID
     * @param viewerUserId 현재 조회 중인 사용자 ID, 비회원이면 null
     * @return 투표 집계 응답
     */
    private PostVoteResponse getVoteResponse(Long postId, Long viewerUserId) {
        int yesVotes = Math.toIntExact(postVoteRepository.countByPostIdAndMyVote(postId, HOGU_VOTE));
        int noVotes = Math.toIntExact(postVoteRepository.countByPostIdAndMyVote(postId, NOT_HOGU_VOTE));
        String myVote = viewerUserId == null
                ? NONE_VOTE
                : postVoteRepository.findByPostIdAndUserId(postId, viewerUserId)
                        .map(PostVote::getMyVote)
                        .orElse(NONE_VOTE);

        return new PostVoteResponse(yesVotes + noVotes, yesVotes, noVotes, myVote);
    }

    /**
     * 현재 조회자가 게시글 작성자인지 확인한다.
     *
     * @param post 조회한 게시글
     * @param viewerUserId 현재 조회 중인 사용자 ID, 비회원이면 null
     * @return 작성자 본인이면 true, 아니면 false
     */
    private boolean isMine(Post post, Long viewerUserId) {
        return viewerUserId != null && post.getWriter().getId().equals(viewerUserId);
    }

    /**
     * 게시글 엔티티와 부가 조회 정보를 게시글 상세 응답 DTO로 변환한다.
     *
     * @param post 조회한 게시글
     * @param imageUrls 게시글 이미지 URL 목록
     * @param isMine 현재 조회자가 작성자인지 여부
     * @return 게시글 상세 응답
     */
    private PostDetailResponse getPostDetailResponse(
            Post post,
            List<String> imageUrls,
            boolean isMine,
            PostVoteResponse vote
    ) {
        return new PostDetailResponse(
                post.getId(),
                isMine,
                List.of(post.getCategory().getCode()),
                post.getTitle(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                post.getViewCount(),
                post.getContent(),
                imageUrls,
                vote,
                new PostWriterResponse(
                        post.getWriter().getNickname(),
                        post.getWriter().getProfileImageUrl()
                )
        );
    }
}
