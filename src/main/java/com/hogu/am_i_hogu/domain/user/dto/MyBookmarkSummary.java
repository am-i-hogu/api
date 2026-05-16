package com.hogu.am_i_hogu.domain.user.dto;

import java.time.LocalDateTime;

public record MyBookmarkSummary(
        Long postId,
        String title,
        String category,
        LocalDateTime postCreatedAt,
        LocalDateTime bookmarkCreatedAt,
        long hoguCount,
        long notHoguCount,
        boolean postIsDeleted
) {
}
