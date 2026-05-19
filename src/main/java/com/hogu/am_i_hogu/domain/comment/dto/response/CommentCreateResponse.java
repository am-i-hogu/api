package com.hogu.am_i_hogu.domain.comment.dto.response;

import java.time.LocalDateTime;

public record CommentCreateResponse(
        Long commentId,
        String content,
        boolean isMine,
        CommentWriterResponse writer,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean isHelpful,
        long totalHelpfulCount,
        Long parentId,
        int depth
) {
}
