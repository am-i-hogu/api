package com.hogu.am_i_hogu.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CategoryAnalysisResponse", description = "카테고리별 호구 분석 응답")
public record CategoryAnalysisResponse(
        @Schema(
                description = "카테고리 코드",
                allowableValues = {"USED_TRADE", "WORK", "PURCHASE", "CONTRACT", "DATING", "ETC"}
        )
        String category,
        @Schema(description = "카테고리별 호구 지수", example = "60")
        Integer hoguIndex,
        @Schema(
                description = "카테고리별 호구 레벨 코드",
                allowableValues = {"SAFE", "CAUTIOUS", "WARNING", "RISKY", "CRITICAL", "NONE"}
        )
        String hoguLevel
) {
}
