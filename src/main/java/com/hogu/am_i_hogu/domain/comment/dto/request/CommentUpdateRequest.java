package com.hogu.am_i_hogu.domain.comment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CommentUpdateRequest", description = "댓글 수정 요청")
public record CommentUpdateRequest(
        @Schema(description = "댓글 내용")
        String content
) {
}
