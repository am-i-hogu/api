package com.hogu.am_i_hogu.domain.post.dto.request;

import java.util.List;

public record PostCreateRequest(
        String title,
        List<String> categories,
        String content,
        List<PostImageRequest> images
) {
}
