package com.hogu.am_i_hogu.domain.post.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "PostVoteRequest",
        description = "게시물 투표 요청",
        requiredProperties = {"myVote"}
)
public record PostVoteRequest(
        @Schema(description = "투표 값", allowableValues = {"HOGU", "NOT_HOGU"})
        String myVote
) {
}
