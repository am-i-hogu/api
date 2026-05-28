package com.hogu.am_i_hogu.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(name = "MyPostItemResponse", description = "내 게시물 항목")
public record MyPostItemResponse(
        @Schema(description = "게시물 ID")
        Long postId,
        @Schema(description = "게시물 제목")
        String title,
        @Schema(
                description = "카테고리 코드",
                allowableValues = {"USED_TRADE", "WORK", "PURCHASE", "CONTRACT", "DATING", "ETC"}
        )
        String category,
        @Schema(description = "게시물 작성 시각")
        LocalDateTime createdAt,
        @Schema(
                description = "투표 요약",
                allowableValues = {"HOGU", "NOT_HOGU", "NONE", "TIE"}
        )
        String voteSummary,
        @Schema(description = "댓글 수")
        long commentCount
) {
}
