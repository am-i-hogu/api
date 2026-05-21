package com.hogu.am_i_hogu.domain.post.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record HomePostItemResponse(
        Long postId,
        boolean isBookmarked,
        List<String> categories,
        String title,
        LocalDateTime createdAt,
        Integer viewCount,
        String contentPreview,
        String thumbnailUrl,
        Long totalVoteCount,
        Long commentCount,
        PostWriterResponse writer
) {
}
