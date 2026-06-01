package com.hogu.am_i_hogu.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import org.openapitools.jackson.nullable.JsonNullable;

@Schema(
        name = "UpdateProfileRequest",
        description = "프로필 수정 요청. 최소 1개 이상의 필드는 반드시 포함되어야 한다."
)
public record UpdateProfileRequest (
        @Schema(description = "설정하고자 하는 닉네임", nullable = true)
        String nickname,

        @Schema(
                type = "string",
                nullable = true,
                description = "프로필 이미지 URL. 필드 미포함 시 유지, null이면 삭제, 문자열이면 업데이트"
        )
        JsonNullable<String> profileImageUrl
) {
}
