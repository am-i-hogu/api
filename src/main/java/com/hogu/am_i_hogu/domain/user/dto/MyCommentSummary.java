package com.hogu.am_i_hogu.domain.user.dto;

import java.time.LocalDateTime;

public record MyCommentSummary(
        Long commentId,
        String content,
        LocalDateTime createdAt,
        Long postId,
        String postTitle,
        boolean postIsDeleted
) {
}
