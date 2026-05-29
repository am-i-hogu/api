package com.hogu.am_i_hogu.domain.post.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "PostVoteResponse",
        description = "게시물 투표 응답",
        requiredProperties = {"totalVotes", "yesVotes", "noVotes", "myVote"}
)
public record PostVoteResponse(
        @Schema(description = "총 투표 수")
        Integer totalVotes,

        @Schema(description = "호구예요 수")
        Integer yesVotes,

        @Schema(description = "호구아니에요 수")
        Integer noVotes,

        @Schema(
                description = "내 투표 값",
                allowableValues = {"HOGU", "NOT_HOGU", "NONE"}
        )
        String myVote
) {
}
