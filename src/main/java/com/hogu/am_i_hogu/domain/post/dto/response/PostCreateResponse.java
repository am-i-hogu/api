package com.hogu.am_i_hogu.domain.post.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PostCreateResponse", description = "게시물 생성 응답")
public record PostCreateResponse(Long postId) {
}
