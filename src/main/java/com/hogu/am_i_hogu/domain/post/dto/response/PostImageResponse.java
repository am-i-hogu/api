package com.hogu.am_i_hogu.domain.post.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "PostImageResponse",
        description = "게시물 이미지 정보",
        requiredProperties = {"imageUrl", "order", "isThumbnail"}
)
public record PostImageResponse(
        @Schema(description = "이미지 URL")
        String imageUrl,

        @Schema(description = "정렬 순서")
        Integer order,

        @Schema(description = "썸네일 여부")
        boolean isThumbnail
) {
}
