package com.hogu.am_i_hogu.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UserCursorRequest", description = "사용자 내역 조회 커서 요청")
public record CursorRequest(
        @Schema(description = "페이지 크기", defaultValue = "5", nullable = true)
        Integer pageSize,
        @Schema(description = "다음 페이지 커서", nullable = true)
        String cursor
) {
}
