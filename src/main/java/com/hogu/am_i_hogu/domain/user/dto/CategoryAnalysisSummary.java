package com.hogu.am_i_hogu.domain.user.dto;

public record CategoryAnalysisSummary(
        String category,
        Long hoguVoteCount,
        Long totalVoteCount
) {
}
