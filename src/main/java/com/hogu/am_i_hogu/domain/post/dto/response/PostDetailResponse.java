package com.hogu.am_i_hogu.domain.post.dto.response;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(
        name = "PostDetailResponse",
        description = "게시물 상세 응답",
        requiredProperties = {"postId", "isMine", "isBookmarked", "categories", "title", "createdAt", "updatedAt", "viewCount", "content", "images", "vote", "writer"}
)
public record PostDetailResponse(
        @Schema(description = "게시물 ID")
        Long postId,

        @Schema(description = "내가 작성한 글 여부")
        Boolean isMine,

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

        @Schema(description = "수정 시각")
        LocalDateTime updatedAt,

        @Schema(description = "조회 수")
        Integer viewCount,

        @Schema(description = "게시물 본문")
        String content,

        @ArraySchema(
                arraySchema = @Schema(description = "이미지 목록"),
                schema = @Schema(implementation = PostImageResponse.class)
        )
        List<PostImageResponse> images,

        @Schema(description = "투표 정보")
        PostVoteResponse vote,

        @Schema(description = "작성자 정보")
        PostWriterResponse writer
) {
}
