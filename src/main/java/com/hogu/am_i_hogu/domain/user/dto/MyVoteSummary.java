package com.hogu.am_i_hogu.domain.user.dto;

import java.time.LocalDateTime;

public record MyVoteSummary(
        String myVote,
        LocalDateTime createdAt,
        Long postId,
        String postTitle,
        boolean postIsDeleted
) {
}
