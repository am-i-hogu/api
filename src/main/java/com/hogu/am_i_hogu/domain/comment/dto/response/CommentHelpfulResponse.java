package com.hogu.am_i_hogu.domain.comment.dto.response;

public record CommentHelpfulResponse(
        long totalHelpfulCount,
        boolean isHelpful
) {
}
