package com.hogu.am_i_hogu.domain.post.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "PostCreateResponse",
        description = "게시물 생성 응답",
        requiredProperties = {"postId"}
)
public record PostCreateResponse(
        @Schema(description = "생성된 게시물 식별자")
        Long postId
) {
}
