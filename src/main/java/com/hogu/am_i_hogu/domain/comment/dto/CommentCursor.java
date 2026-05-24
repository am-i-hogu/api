package com.hogu.am_i_hogu.domain.comment.dto;

import java.time.LocalDateTime;

public record CommentCursor(
        LocalDateTime createdAt,
        Long totalHelpfulCount,
        Long commentId
) {
}
