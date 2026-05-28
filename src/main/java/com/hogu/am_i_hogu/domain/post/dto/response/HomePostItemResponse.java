package com.hogu.am_i_hogu.domain.post.dto.response;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(name = "HomePostItemResponse", description = "홈 게시물 요약")
public record HomePostItemResponse(
        @Schema(description = "게시물 ID")
        Long postId,

        @Schema(description = "북마크 여부")
        boolean isBookmarked,

        @ArraySchema(
                arraySchema = @Schema(description = "카테고리 코드 목록"),
                schema = @Schema(
                        type = "string",
                        allowableValues = {"USED_TRADE", "WORK", "PURCHASE", "CONTRACT", "DATING", "ETC"}
                )
        )
        List<String> categories,

        @Schema(description = "게시물 제목")
        String title,

        @Schema(description = "생성 시각")
        LocalDateTime createdAt,

        @Schema(description = "조회 수")
        Integer viewCount,

        @Schema(description = "미리보기 본문")
        String contentPreview,

        @Schema(description = "썸네일 URL", nullable = true)
        String thumbnailUrl,

        @Schema(description = "총 투표 수")
        Long totalVoteCount,

        @Schema(description = "댓글 수")
        Long commentCount,

        @Schema(description = "작성자 정보")
        PostWriterResponse writer
) {
}
