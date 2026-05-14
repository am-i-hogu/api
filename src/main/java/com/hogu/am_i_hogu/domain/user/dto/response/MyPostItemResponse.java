package com.hogu.am_i_hogu.domain.user.dto.response;

import java.time.LocalDateTime;

public record MyPostItemResponse(
        Long postId,
        String title,
        LocalDateTime createdAt,
        String voteSummary,
        long commentCount
) {
}
