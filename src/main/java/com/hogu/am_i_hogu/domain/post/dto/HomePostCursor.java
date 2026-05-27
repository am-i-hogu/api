package com.hogu.am_i_hogu.domain.post.dto;

import java.time.LocalDateTime;

public record HomePostCursor(
        String sortBy,
        Long postId,
        LocalDateTime createdAt,
        Integer viewCount,
        Long commentCount,
        Long totalVoteCount
) {
}
