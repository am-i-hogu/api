package com.hogu.am_i_hogu.domain.comment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(name = "CommentUpdateResponse", description = "댓글 수정 응답")
public record CommentUpdateResponse(
        @Schema(description = "댓글 ID")
        Long commentId,
        @Schema(description = "댓글 내용")
        String content,
        @Schema(description = "내 댓글 여부")
        boolean isMine,
        @Schema(description = "작성자 정보")
        CommentWriterResponse writer,
        @Schema(description = "생성 시각")
        LocalDateTime createdAt,
        @Schema(description = "수정 시각")
        LocalDateTime updatedAt,
        @Schema(description = "유익해요 선택 여부")
        boolean isHelpful,
        @Schema(description = "총 유익해요 수")
        long totalHelpfulCount,
        @Schema(description = "부모 댓글 ID", nullable = true)
        Long parentId,
        @Schema(description = "댓글 깊이")
        int depth
) {
}
