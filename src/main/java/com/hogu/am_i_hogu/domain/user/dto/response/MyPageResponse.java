package com.hogu.am_i_hogu.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "MyPageResponse", description = "마이페이지 응답")
public record MyPageResponse(
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
        String hoguShortDescription
) {
}
