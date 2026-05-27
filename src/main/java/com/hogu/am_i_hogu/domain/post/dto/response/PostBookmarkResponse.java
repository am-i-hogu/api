package com.hogu.am_i_hogu.domain.post.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PostBookmarkResponse", description = "게시물 북마크 응답")
public record PostBookmarkResponse(boolean isBookmarked) {
}
