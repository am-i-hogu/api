package com.hogu.am_i_hogu.domain.comment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CommentWriterResponse", description = "집단지성 작성자 정보")
public record CommentWriterResponse(
        @Schema(description = "작성자 닉네임")
        String nickname,
        @Schema(description = "작성자 프로필 이미지 URL", nullable = true)
        String profileImageUrl,
        @Schema(description = "게시물 작성자 여부")
        boolean isPostWriter
) {
}
