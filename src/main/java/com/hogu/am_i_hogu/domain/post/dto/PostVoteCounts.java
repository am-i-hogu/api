package com.hogu.am_i_hogu.domain.post.dto;

public record PostVoteCounts(
        Long hoguVoteCount,
        Long notHoguVoteCount
) {
    public PostVoteCounts {
        hoguVoteCount = hoguVoteCount == null ? 0L : hoguVoteCount;
        notHoguVoteCount = notHoguVoteCount == null ? 0L : notHoguVoteCount;
    }

    public long totalVoteCount() {
        return hoguVoteCount + notHoguVoteCount;
    }
}
