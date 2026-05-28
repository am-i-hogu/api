package com.hogu.am_i_hogu.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "MyPostListResponse", description = "내 게시물 목록 응답")
public record MyPostListResponse(
    @ArraySchema(arraySchema = @Schema(description = "게시물 목록"))
    List<MyPostItemResponse> posts,
    @Schema(description = "다음 페이지 존재 여부")
    boolean hasNext,
    @Schema(description = "다음 페이지 커서", nullable = true)
    String nextCursor
) {
}
