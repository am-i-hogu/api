package com.hogu.am_i_hogu.domain.comment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "CommentCreateRequest",
        description = "집단지성 생성 요청. 최상위 집단지성은 parentId를 비워두고, 대댓글은 parentId에 부모 집단지성 ID를 넣는다."
)
public record CommentCreateRequest(
        @Schema(description = "부모 집단지성 ID. 최상위 집단지성인 경우 null", nullable = true, example = "12")
        Long parentId,
        @Schema(
                description = "집단지성 내용. 공백만 허용되지 않으며 최대 300자까지 입력할 수 있다.",
                example = "호구같아요."
        )
        String content
) {
}
