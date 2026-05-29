package com.hogu.am_i_hogu.domain.comment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(
        name = "CommentUpdateResponse",
        description = "집단지성 수정 응답",
        requiredProperties = {"commentId", "content", "isMine", "writer", "createdAt", "updatedAt", "isHelpful", "totalHelpfulCount", "parentId", "depth"}
)
public record CommentUpdateResponse(
        @Schema(description = "집단지성 ID")
        Long commentId,
        @Schema(description = "집단지성 내용")
        String content,
        @Schema(description = "내가 작성한 집단지성 여부. 수정 성공 시 항상 true")
        boolean isMine,
        @Schema(description = "작성자 정보")
        CommentWriterResponse writer,
        @Schema(description = "생성 시각")
        LocalDateTime createdAt,
        @Schema(description = "수정 시각")
        LocalDateTime updatedAt,
        @Schema(description = "현재 사용자의 유익해요 선택 여부. 수정 성공 시 항상 false")
        boolean isHelpful,
        @Schema(description = "총 유익해요 수")
        long totalHelpfulCount,
        @Schema(description = "부모 집단지성 ID. 최상위 집단지성인 경우 null", nullable = true)
        Long parentId,
        @Schema(description = "집단지성 깊이. 최상위는 0, 대댓글은 1")
        int depth
) {
}
