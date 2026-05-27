package com.hogu.am_i_hogu.domain.comment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CommentCreateRequest", description = "댓글 생성 요청")
public record CommentCreateRequest(
        @Schema(description = "부모 댓글 ID", nullable = true)
        Long parentId,
        @Schema(description = "댓글 내용")
        String content
) {
}
