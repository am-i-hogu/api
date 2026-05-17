package com.hogu.am_i_hogu.domain.user.dto;

import java.time.LocalDateTime;

public record MyPostCursor(
        LocalDateTime createdAt,
        Long postId
) {
}
