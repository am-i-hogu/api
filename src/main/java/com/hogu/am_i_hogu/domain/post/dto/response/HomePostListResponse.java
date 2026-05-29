package com.hogu.am_i_hogu.domain.post.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(
        name = "HomePostListResponse",
        description = "홈 게시물 목록 응답",
        requiredProperties = {"posts", "hasNext", "nextCursor"}
)
public record HomePostListResponse(
        @Schema(description = "총 게시물 수", nullable = true)
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Long totalPostCount,

        @ArraySchema(arraySchema = @Schema(description = "게시물 목록"))
        List<HomePostItemResponse> posts,

        @Schema(description = "다음 페이지 존재 여부")
        boolean hasNext,

        @Schema(description = "다음 페이지 커서", nullable = true)
        String nextCursor
) {
}
