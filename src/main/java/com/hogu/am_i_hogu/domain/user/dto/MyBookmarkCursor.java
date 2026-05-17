package com.hogu.am_i_hogu.domain.user.dto;

import java.time.LocalDateTime;

public record MyBookmarkCursor(
        LocalDateTime createdAt,
        Long postId
) {
}
