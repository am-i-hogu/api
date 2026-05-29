package com.hogu.am_i_hogu.domain.policy.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(
        name = "PolicyResponse",
        description = "현재 적용 중인 개인정보 처리 방침 응답",
        requiredProperties = {"version", "updatedAt", "content"}
)
public record PolicyResponse(
    @Schema(description = "정책 버전")
    String version,

    @Schema(description = "정책 최종 수정 시각")
    LocalDateTime updatedAt,

    @Schema(description = "정책 HTML 본문")
    String content
) {
}
