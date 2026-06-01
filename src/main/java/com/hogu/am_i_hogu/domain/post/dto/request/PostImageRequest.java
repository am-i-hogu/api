package com.hogu.am_i_hogu.domain.post.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "PostImageRequest",
        description = "게시물 이미지 요청",
        requiredProperties = {"imageUrl", "order", "isThumbnail"}
)
public record PostImageRequest(
        @Schema(description = "이미지 URL")
        String imageUrl,

        @Schema(description = "정렬 순서")
        Integer order,

        @Schema(description = "썸네일 여부")
        Boolean isThumbnail
) {
}
