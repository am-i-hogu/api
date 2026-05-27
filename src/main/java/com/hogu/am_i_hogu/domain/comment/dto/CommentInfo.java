package com.hogu.am_i_hogu.domain.comment.dto;

import java.time.LocalDateTime;

public record CommentInfo(
        Long commentId,
        String content,
        Long writerId,
        String writerNickname,
        String writerProfileImageUrl,
        boolean isPostWriter,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean isDeleted,
        Long parentId,
        int depth,
        long totalHelpfulCount
) {
}
