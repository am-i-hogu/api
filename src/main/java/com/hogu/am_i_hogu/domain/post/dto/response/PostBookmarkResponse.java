package com.hogu.am_i_hogu.domain.post.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "PostBookmarkResponse",
        description = "게시물 북마크 응답",
        requiredProperties = {"isBookmarked"}
)
public record PostBookmarkResponse(
        @Schema(description = "북마크 등록 여부")
        boolean isBookmarked
) {
}
