package com.hogu.am_i_hogu.domain.post.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "ImageUploadResponse",
        description = "이미지 업로드 응답",
        requiredProperties = {"imageUrl"}
)
public record ImageUploadResponse(
        @Schema(description = "이미지가 저장된 url")
        String imageUrl
) {
}
