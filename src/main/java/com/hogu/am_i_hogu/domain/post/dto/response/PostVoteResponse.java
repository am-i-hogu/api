package com.hogu.am_i_hogu.domain.post.dto.response;

public record PostVoteResponse(
        Integer totalVotes,
        Integer yesVotes,
        Integer noVotes,
        String myVote
) {
}
