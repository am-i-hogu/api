package com.hogu.am_i_hogu.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "CheckNicknameResponse",
        description = "닉네임 중복 검사 응답",
        requiredProperties = {"isAvailable"}
)
public record CheckNicknameResponse(
        @Schema(description = "닉네임 사용 가능 여부")
        boolean isAvailable
) {
}
