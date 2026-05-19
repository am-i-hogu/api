package com.hogu.am_i_hogu.domain.user.dto;

public record UserInfoSummary(
        String nickname,
        String profileImageUrl,
        Integer votedPostCount,
        Integer hoguIndex
) {
}
