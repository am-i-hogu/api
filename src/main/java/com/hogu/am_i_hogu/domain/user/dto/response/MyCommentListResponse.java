package com.hogu.am_i_hogu.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(
        name = "MyCommentListResponse",
        description = "내 댓글 목록 응답",
        requiredProperties = {"comments", "hasNext", "nextCursor"}
)
public record MyCommentListResponse(
        @ArraySchema(arraySchema = @Schema(description = "댓글 목록"))
        List<MyCommentItemResponse> comments,
        @Schema(description = "다음 페이지 존재 여부")
        boolean hasNext,
        @Schema(description = "다음 페이지 커서", nullable = true)
        String nextCursor
) {
}
