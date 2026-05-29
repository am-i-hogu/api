package com.hogu.am_i_hogu.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "MyVotePostResponse",
        description = "내 투표 대상 게시물 정보",
        requiredProperties = {"postId", "title", "category", "commentCount", "isDeleted"}
)
public record MyVotePostResponse(
        @Schema(description = "게시물 ID")
        Long postId,
        @Schema(description = "게시물 제목")
        String title,
        @Schema(
                description = "카테고리 코드",
                allowableValues = {"USED_TRADE", "WORK", "PURCHASE", "CONTRACT", "DATING", "ETC"}
        )
        String category,
        @Schema(description = "댓글 수")
        long commentCount,
        @Schema(description = "게시물 삭제 여부")
        boolean isDeleted
) {
}
