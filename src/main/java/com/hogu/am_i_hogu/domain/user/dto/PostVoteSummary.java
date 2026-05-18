package com.hogu.am_i_hogu.domain.user.dto;

public record PostVoteSummary(
        Long postId,
        Long hoguVoteCount,
        Long notHoguVoteCount
) {
}
