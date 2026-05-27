package com.hogu.am_i_hogu.domain.post.dto;

import java.time.LocalDateTime;

public record HomePostSummary(
        Long postId,
        boolean bookmarked,
        String category,
        String title,
        LocalDateTime createdAt,
        Integer viewCount,
        String content,
        String thumbnailUrl,
        Long totalVoteCount,
        Long commentCount,
        String writerNickname,
        String writerProfileImageUrl
) {
}
