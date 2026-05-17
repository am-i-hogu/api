package com.hogu.am_i_hogu.domain.user.dto.response;

import java.time.LocalDateTime;

public record MyCommentItemResponse(
        Long commentId,
        String content,
        LocalDateTime createdAt,
        MyCommentPostResponse post
) {
}
