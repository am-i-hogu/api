package com.hogu.am_i_hogu.domain.user.dto;

import java.time.LocalDateTime;

public record MyPostSummary(
        Long postId,
        String title,
        LocalDateTime createdAt,
        long hoguCount,
        long notHoguCount
) {
}
