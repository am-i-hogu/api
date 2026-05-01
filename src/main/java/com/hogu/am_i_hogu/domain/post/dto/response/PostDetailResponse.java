package com.hogu.am_i_hogu.domain.post.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record PostDetailResponse(
        Long PostId,
        Boolean isMine,
        List<String> categories,
        String title,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Integer viewCount,
        String content,
        List<String> images,
        PostVoteResponse vote,
        PostWriterResponse writer
) {
}
