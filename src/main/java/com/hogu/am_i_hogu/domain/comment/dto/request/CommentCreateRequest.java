package com.hogu.am_i_hogu.domain.comment.dto.request;

public record CommentCreateRequest(
        Long parentId,
        String content
) {
}
