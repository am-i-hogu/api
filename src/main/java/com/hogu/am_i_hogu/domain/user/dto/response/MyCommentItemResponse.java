package com.hogu.am_i_hogu.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(
        name = "MyCommentItemResponse",
        description = "내 댓글 항목",
        requiredProperties = {"commentId", "content", "createdAt", "post"}
)
public record MyCommentItemResponse(
        @Schema(description = "댓글 ID")
        Long commentId,
        @Schema(description = "댓글 내용")
        String content,
        @Schema(description = "댓글 작성 시각")
        LocalDateTime createdAt,
        @Schema(description = "댓글이 달린 게시물 정보")
        MyCommentPostResponse post
) {
}
