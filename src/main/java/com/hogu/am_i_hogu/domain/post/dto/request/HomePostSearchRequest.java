package com.hogu.am_i_hogu.domain.post.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "HomePostSearchRequest", description = "홈 게시물 조회 조건")
public record HomePostSearchRequest(
        @Schema(description = "검색 키워드", nullable = true)
        String keyword,

        @Schema(
                description = "쉼표로 구분된 카테고리 코드들",
                allowableValues = {"USED_TRADE", "WORK", "PURCHASE", "CONTRACT", "DATING", "ETC"},
                nullable = true
        )
        String categories,

        @Schema(
                description = "정렬 기준",
                allowableValues = {"LATEST", "MOST_VIEWED", "MOST_COMMENTED", "MOST_PARTICIPATED"},
                defaultValue = "LATEST",
                nullable = true
        )
        String sortBy,

        @Schema(description = "페이지 크기", defaultValue = "5", nullable = true)
        Integer pageSize,

        @Schema(description = "다음 페이지 커서", nullable = true)
        String cursor
) {
}
