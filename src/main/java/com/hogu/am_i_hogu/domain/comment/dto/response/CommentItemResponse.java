package com.hogu.am_i_hogu.domain.comment.dto.response;

import java.time.LocalDateTime;

public record CommentItemResponse(
        Long commentId,
        String content,
        boolean isMine,
        CommentWriterResponse writer,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean isDeleted,
        boolean isHelpful,
        long totalHelpfulCount,
        Long parentId,
        int depth
) {
}
