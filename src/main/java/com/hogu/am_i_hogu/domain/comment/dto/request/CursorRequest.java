package com.hogu.am_i_hogu.domain.comment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CommentCursorRequest", description = "집단지성 목록 조회 조건")
public record CursorRequest(
        @Schema(
                description = "정렬 기준. LATEST는 최신순, HELPFUL은 유익해요 많은 순",
                allowableValues = {"LATEST", "HELPFUL"},
                defaultValue = "LATEST",
                nullable = true
        )
        String sortBy,
        @Schema(description = "페이지 크기. 최소 1, 최대 15이며 생략 시 기본값은 5", defaultValue = "5", nullable = true)
        Integer pageSize,
        @Schema(description = "다음 페이지 커서. 이전 응답의 nextCursor 값을 그대로 사용", nullable = true)
        String cursor
) {
}
