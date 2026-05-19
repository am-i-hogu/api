package com.hogu.am_i_hogu.domain.post.dto;

public record PostVoteCounts(
        Long hoguVoteCount,
        Long notHoguVoteCount,
        Long votedPostCount
) {
    public PostVoteCounts(Long hoguVoteCount, Long notHoguVoteCount) {
        this(hoguVoteCount, notHoguVoteCount, 0L);
    }

    public PostVoteCounts {
        hoguVoteCount = hoguVoteCount == null ? 0L : hoguVoteCount;
        notHoguVoteCount = notHoguVoteCount == null ? 0L : notHoguVoteCount;
        votedPostCount = votedPostCount == null ? 0L : votedPostCount;
    }

    public long totalVoteCount() {
        return hoguVoteCount + notHoguVoteCount;
    }
}
