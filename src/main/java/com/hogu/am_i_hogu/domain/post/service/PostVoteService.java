package com.hogu.am_i_hogu.domain.post.service;

import com.hogu.am_i_hogu.common.exception.CommonErrorCode;
import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.domain.post.domain.Post;
import com.hogu.am_i_hogu.domain.post.domain.PostVoteId;
import com.hogu.am_i_hogu.domain.post.dto.request.PostVoteRequest;
import com.hogu.am_i_hogu.domain.post.dto.response.PostVoteResponse;
import com.hogu.am_i_hogu.domain.post.exception.PostErrorCode;
import com.hogu.am_i_hogu.domain.post.repository.PostRepository;
import com.hogu.am_i_hogu.domain.post.repository.PostVoteRepository;
import com.hogu.am_i_hogu.domain.user.domain.UserHoguStat;
import com.hogu.am_i_hogu.domain.user.repository.UserHoguStatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PostVoteService {
    private static final String HOGU_VOTE = "HOGU";
    private static final String NOT_HOGU_VOTE = "NOT_HOGU";
    private static final String NONE_VOTE = "NONE";

    private final PostRepository postRepository;
    private final PostVoteRepository postVoteRepository;
    private final UserHoguStatRepository userHoguStatRepository;

    @Transactional
    public PostVoteResponse vote(Long userId, Long postId, PostVoteRequest request) {
        validateVoteRequest(request);

        Post post = getPostOrThrow(postId);
        validateNotDeleted(post);
        validateNotWriter(post, userId);

        LocalDateTime now = LocalDateTime.now();
        postVoteRepository.upsertVote(userId, postId, request.myVote(), now);

        updateWriterHoguStat(post.getWriter().getId(), now);

        return getVoteResponse(postId, request.myVote());
    }

    @Transactional
    public PostVoteResponse cancel(Long userId, Long postId) {
        Post post = getPostOrThrow(postId);
        validateNotDeleted(post);
        validateNotWriter(post, userId);

        LocalDateTime now = LocalDateTime.now();
        postVoteRepository.findById(new PostVoteId(userId, postId))
                .ifPresent(postVote -> postVote.updateVote(NONE_VOTE, now));

        updateWriterHoguStat(post.getWriter().getId(), now);

        return getVoteResponse(postId, NONE_VOTE);
    }

    private void validateVoteRequest(PostVoteRequest request) {
        if (request == null) {
            throw new CustomException(PostErrorCode.EMPTY_REQUEST_BODY);
        }
        if (!HOGU_VOTE.equals(request.myVote()) && !NOT_HOGU_VOTE.equals(request.myVote())) {
            throw new CustomException(PostErrorCode.INVALID_MYVOTE);
        }
    }

    private Post getPostOrThrow(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(PostErrorCode.POST_NOT_FOUND));
    }

    private void validateNotDeleted(Post post) {
        if (post.isDeleted()) {
            throw new CustomException(PostErrorCode.POST_ALREADY_DELETED);
        }
    }

    private void validateNotWriter(Post post, Long userId) {
        if (post.getWriter().getId().equals(userId)) {
            throw new CustomException(CommonErrorCode.FORBIDDEN_ACCESS);
        }
    }

    private void updateWriterHoguStat(Long writerUserId, LocalDateTime now) {
        int hoguVoteCount = Math.toIntExact(
                postVoteRepository.countByWriterUserIdAndMyVote(writerUserId, HOGU_VOTE)
        );
        int totalVoteCount = Math.toIntExact(
                postVoteRepository.countEffectiveVotesByWriterUserId(writerUserId)
        );

        UserHoguStat stat = userHoguStatRepository.findById(writerUserId)
                .orElseGet(() -> new UserHoguStat(writerUserId, now));
        stat.updateVoteStats(hoguVoteCount, totalVoteCount, now);
        userHoguStatRepository.save(stat);
    }

    private PostVoteResponse getVoteResponse(Long postId, String myVote) {
        int yesVotes = Math.toIntExact(postVoteRepository.countByPostIdAndMyVote(postId, HOGU_VOTE));
        int noVotes = Math.toIntExact(postVoteRepository.countByPostIdAndMyVote(postId, NOT_HOGU_VOTE));

        return new PostVoteResponse(yesVotes + noVotes, yesVotes, noVotes, myVote);
    }
}
