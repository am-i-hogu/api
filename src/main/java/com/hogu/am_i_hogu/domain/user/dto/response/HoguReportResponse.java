package com.hogu.am_i_hogu.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "HoguReportResponse", description = "호구 보고서 응답")
public record HoguReportResponse(
        @Schema(description = "닉네임")
        String nickname,
        @Schema(description = "프로필 이미지 URL", nullable = true)
        String profileImageUrl,
        @Schema(description = "호구 지수")
        Integer hoguIndex,
        @Schema(
                description = "호구 레벨 코드",
                allowableValues = {"SAFE", "CAUTIOUS", "WARNING", "RISKY", "CRITICAL", "NONE"}
        )
        String hoguLevel,
        @Schema(description = "호구 레벨 한 줄 설명")
        String hoguShortDescription,
        @Schema(description = "호구 레벨 상세 설명")
        String hoguDescription,
        @ArraySchema(arraySchema = @Schema(description = "카테고리별 분석 결과"))
        List<CategoryAnalysisResponse> categoryAnalysis,
        @Schema(description = "전체 게시물 수")
        Integer totalPostCount,
        @Schema(description = "호구 판정 게시물 수")
        Integer hoguPostCount,
        @Schema(description = "호구 아님 판정 게시물 수")
        Integer notHoguPostCount
) {
}
