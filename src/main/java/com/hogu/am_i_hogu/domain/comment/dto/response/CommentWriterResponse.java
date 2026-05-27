package com.hogu.am_i_hogu.domain.comment.dto.response;

public record CommentWriterResponse(
        String nickname,
        String profileImageUrl,
        boolean isPostWriter
) {
}
