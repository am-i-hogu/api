package com.hogu.am_i_hogu.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(
        name = "MyVoteListResponse",
        description = "내 투표 목록 응답",
        requiredProperties = {"votes", "hasNext", "nextCursor"}
)
public record MyVoteListResponse(
        @ArraySchema(arraySchema = @Schema(description = "투표 목록"))
        List<MyVoteItemResponse> votes,
        @Schema(description = "다음 페이지 존재 여부")
        boolean hasNext,
        @Schema(description = "다음 페이지 커서", nullable = true)
        String nextCursor
) {
}
