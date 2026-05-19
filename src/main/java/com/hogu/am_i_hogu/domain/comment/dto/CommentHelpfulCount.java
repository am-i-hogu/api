package com.hogu.am_i_hogu.domain.comment.dto;

public record CommentHelpfulCount(
        Long commentId,
        Long totalHelpfulCount
) {
}
