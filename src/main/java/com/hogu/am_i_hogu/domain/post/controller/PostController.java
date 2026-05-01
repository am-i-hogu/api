package com.hogu.am_i_hogu.domain.post.controller;

import com.hogu.am_i_hogu.domain.post.dto.request.PostCreateRequest;
import com.hogu.am_i_hogu.domain.post.dto.response.PostCreateResponse;
import com.hogu.am_i_hogu.domain.post.dto.response.PostDetailResponse;
import com.hogu.am_i_hogu.domain.post.service.PostCreateService;
import com.hogu.am_i_hogu.domain.post.service.PostDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
public class PostController {
    private final PostCreateService postCreateService;
    private final PostDetailService postDetailService;

    @GetMapping("/{postId}")
    public ResponseEntity<PostDetailResponse> getPostById(@PathVariable Long postId, Authentication authentication) {
        Long viewerUserId = authentication == null ? null : Long.valueOf(authentication.getName());
        PostDetailResponse response = postDetailService.getDetail(postId, viewerUserId);

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<PostCreateResponse> createPost(
            Authentication authentication,
            @RequestBody(required = false) PostCreateRequest request
    ) {
        Long userId = Long.valueOf(authentication.getName());
        PostCreateResponse response = postCreateService.create(userId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
