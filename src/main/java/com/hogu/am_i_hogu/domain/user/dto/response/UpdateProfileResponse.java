package com.hogu.am_i_hogu.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "UpdateProfileResponse",
        description = "프로필 수정 응답",
        requiredProperties = {"id", "nickname", "profileImageUrl"}
)
public record UpdateProfileResponse (
        @Schema(description = "유저를 구분하는 고유의 id")
        Long id,

        @Schema(description = "유저가 설정한 닉네임")
        String nickname,

        @Schema(description = "프로필 이미지 URL. 프로필 이미지가 삭제되었거나 없는 경우 null", nullable = true)
        String profileImageUrl
) {
}
