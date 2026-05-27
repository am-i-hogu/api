package com.hogu.am_i_hogu.domain.comment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CommentCursorRequest", description = "댓글 목록 조회 조건")
public record CursorRequest(
        @Schema(
                description = "정렬 기준",
                allowableValues = {"LATEST", "HELPFUL"},
                defaultValue = "LATEST",
                nullable = true
        )
        String sortBy,
        @Schema(description = "페이지 크기", defaultValue = "5", nullable = true)
        Integer pageSize,
        @Schema(description = "다음 페이지 커서", nullable = true)
        String cursor
) {
}
