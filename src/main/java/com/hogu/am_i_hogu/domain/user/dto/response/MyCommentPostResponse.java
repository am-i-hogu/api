package com.hogu.am_i_hogu.domain.user.dto.response;

public record MyCommentPostResponse(
        Long postId,
        String title,
        boolean isDeleted
) {
}
