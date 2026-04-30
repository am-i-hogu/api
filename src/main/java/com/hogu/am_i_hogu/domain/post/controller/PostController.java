package com.hogu.am_i_hogu.domain.post.controller;

import com.hogu.am_i_hogu.domain.post.dto.request.PostCreateRequest;
import com.hogu.am_i_hogu.domain.post.dto.response.PostCreateResponse;
import com.hogu.am_i_hogu.domain.post.service.PostCreateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
public class PostController {

    private final PostCreateService postCreateService;

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
