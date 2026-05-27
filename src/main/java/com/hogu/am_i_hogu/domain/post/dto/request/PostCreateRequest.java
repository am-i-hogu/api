package com.hogu.am_i_hogu.domain.post.dto.request;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "PostCreateRequest", description = "게시물 생성 요청")
public record PostCreateRequest(
        @Schema(description = "게시물 제목")
        String title,

        @ArraySchema(
                arraySchema = @Schema(description = "카테고리 코드 목록"),
                schema = @Schema(
                        type = "string",
                        allowableValues = {"USED_TRADE", "WORK", "PURCHASE", "CONTRACT", "DATING", "ETC"}
                )
        )
        List<String> categories,

        @Schema(description = "게시물 본문")
        String content,

        @ArraySchema(arraySchema = @Schema(description = "이미지 목록"))
        List<PostImageRequest> images
) {
}
