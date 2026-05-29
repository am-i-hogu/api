package com.hogu.am_i_hogu.domain.comment.dto.response;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(
        name = "CommentReadResponse",
        description = "집단지성 목록 응답",
        requiredProperties = {"comments", "hasNext", "nextCursor"}
)
public record CommentReadResponse(
        @ArraySchema(arraySchema = @Schema(description = "집단지성 목록"))
        List<CommentItemResponse> comments,
        @Schema(description = "다음 페이지 존재 여부")
        boolean hasNext,
        @Schema(description = "다음 페이지 커서. hasNext가 false이면 null", nullable = true)
        String nextCursor
) {
}
