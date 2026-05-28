package com.hogu.am_i_hogu.domain.post.dto.request;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "PostUpdateRequest", description = "게시물 수정 요청")
public record PostUpdateRequest(
        @Schema(description = "게시물 제목")
        String title,

        @ArraySchema(
                minItems = 1,
                maxItems = 1,
                arraySchema = @Schema(description = "변경할 카테고리 코드 목록. 1개만 허용"),
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
