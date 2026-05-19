package com.hogu.am_i_hogu.domain.comment.controller;

import com.hogu.am_i_hogu.domain.comment.dto.request.CommentCreateRequest;
import com.hogu.am_i_hogu.domain.comment.dto.response.CommentCreateResponse;
import com.hogu.am_i_hogu.domain.comment.service.CommentCreateService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
public class CommentController {

    private final CommentCreateService commentCreateService;

    public CommentController(CommentCreateService commentCreateService) {
        this.commentCreateService = commentCreateService;
    }

    /**
     * [CI-002] 집단지성 생성
     *
     * @param authentication    사용자 인증 정보
     * @param postId            집단지성이 포함될 게시물 id
     * @param request           집단지성 생성 정보(부모 댓글 정보, 내용)
     * @return  생성된 집단지성 정보
     */
    @PostMapping("/{postId}/comments")
    public ResponseEntity<CommentCreateResponse> create(
            Authentication authentication,
            @PathVariable Long postId,
            @RequestBody(required = false) CommentCreateRequest request
    ) {
        Long userId = Long.valueOf(authentication.getName());
        CommentCreateResponse response = commentCreateService.create(userId, postId, request);

        return ResponseEntity.ok(response);
    }
}
