package com.hogu.am_i_hogu.domain.user.dto;

public record MyPageSummary(
        String nickname,
        String profileImageUrl,
        Integer totalVoteCount,
        Integer hoguIndex
) {
}
