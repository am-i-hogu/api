package com.hogu.am_i_hogu.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(
        name = "MyVoteItemResponse",
        description = "내 투표 항목",
        requiredProperties = {"myVote", "createdAt", "post"}
)
public record MyVoteItemResponse(
        @Schema(
                description = "내 투표 값",
                allowableValues = {"HOGU", "NOT_HOGU"}
        )
        String myVote,
        @Schema(description = "투표 시각")
        LocalDateTime createdAt,
        @Schema(description = "투표한 게시물 정보")
        MyVotePostResponse post
) {
}
