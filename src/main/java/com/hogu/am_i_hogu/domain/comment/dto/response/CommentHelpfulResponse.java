package com.hogu.am_i_hogu.domain.comment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CommentHelpfulResponse", description = "댓글 유익해요 응답")
public record CommentHelpfulResponse(
        @Schema(description = "총 유익해요 수")
        long totalHelpfulCount,
        @Schema(description = "유익해요 선택 여부")
        boolean isHelpful
) {
}
