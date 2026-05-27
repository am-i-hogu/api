package com.hogu.am_i_hogu.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(name = "MyBookmarkItemResponse", description = "내 북마크 게시물 항목")
public record MyBookmarkItemResponse(
        @Schema(description = "게시물 ID")
        Long postId,
        @Schema(description = "게시물 제목")
        String title,
        @Schema(
                description = "카테고리 코드",
                allowableValues = {"USED_TRADE", "WORK", "PURCHASE", "CONTRACT", "DATING", "ETC"}
        )
        String category,
        @Schema(description = "북마크 생성 시각")
        LocalDateTime createdAt,
        @Schema(
                description = "투표 요약",
                allowableValues = {"HOGU", "NOT_HOGU", "NONE", "TIE"}
        )
        String voteSummary,
        @Schema(description = "댓글 수")
        long commentCount,
        @Schema(description = "게시물 삭제 여부")
        boolean isDeleted
) {
}
