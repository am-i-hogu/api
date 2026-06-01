package com.hogu.am_i_hogu.domain.post.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "PostWriterResponse",
        description = "게시물 작성자 정보",
        requiredProperties = {"nickname", "profileImageUrl"}
)
public record PostWriterResponse(
        @Schema(description = "닉네임")
        String nickname,

        @Schema(description = "프로필 이미지 URL", nullable = true)
        String profileImageUrl
) {
}
