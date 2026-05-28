package com.hogu.am_i_hogu.domain.comment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CommentUpdateRequest", description = "집단지성 수정 요청")
public record CommentUpdateRequest(
        @Schema(
                description = "집단지성 내용. 공백만 허용되지 않으며 최대 300자까지 입력할 수 있다.",
                example = "호구같아요."
        )
        String content
) {
}
