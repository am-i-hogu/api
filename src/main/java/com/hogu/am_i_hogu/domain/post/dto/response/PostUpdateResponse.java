package com.hogu.am_i_hogu.domain.post.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PostUpdateResponse", description = "게시물 수정 응답")
public record PostUpdateResponse(Long postId) {
}
