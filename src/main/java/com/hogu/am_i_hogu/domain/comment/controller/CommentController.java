package com.hogu.am_i_hogu.domain.comment.controller;

import com.hogu.am_i_hogu.domain.comment.dto.request.CommentCreateRequest;
import com.hogu.am_i_hogu.domain.comment.dto.request.CommentUpdateRequest;
import com.hogu.am_i_hogu.domain.comment.dto.request.CursorRequest;
import com.hogu.am_i_hogu.domain.comment.dto.response.CommentCreateResponse;
import com.hogu.am_i_hogu.domain.comment.dto.response.CommentHelpfulResponse;
import com.hogu.am_i_hogu.domain.comment.dto.response.CommentReadResponse;
import com.hogu.am_i_hogu.domain.comment.dto.response.CommentUpdateResponse;
import org.springframework.http.HttpStatus;
import com.hogu.am_i_hogu.domain.comment.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
public class CommentController {

    private final CommentCreateService commentCreateService;
    private final CommentReadService commentReadService;
    private final CommentUpdateService commentUpdateService;
    private final CommentDeleteService commentDeleteService;
    private final CommentHelpfulService commentHelpfulService;

    public CommentController(
            CommentCreateService commentCreateService,
            CommentReadService commentReadService,
            CommentUpdateService commentUpdateService,
            CommentDeleteService commentDeleteService,
            CommentHelpfulService commentHelpfulService
    ) {
        this.commentCreateService = commentCreateService;
        this.commentReadService = commentReadService;
        this.commentUpdateService = commentUpdateService;
        this.commentDeleteService = commentDeleteService;
        this.commentHelpfulService = commentHelpfulService;
    }

    /**
     * [CI-001] 집단지성 조회
     *
     * @param authentication    사용자 인증 정보(필수 X)
     * @param postId            집단지성이 포함된 게시물 id
     * @param cursorRequest     cursor 정보(sortBy, pageSize, cursor)
     * @return
     */
    @GetMapping("/{postId}/comments")
    public ResponseEntity<CommentReadResponse> read(
            Authentication authentication,
            @PathVariable Long postId,
            @ModelAttribute CursorRequest cursorRequest
    ) {
        Long userId = authentication == null ? null : Long.valueOf(authentication.getName());
        CommentReadResponse response = commentReadService.read(userId, postId, cursorRequest);

        return ResponseEntity.ok(response);
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

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * [CI-003] 집단지성 수정
     * @param authentication    사용자 인증 정보
     * @param postId            집단지성이 포함된 게시물 id
     * @param commentId         수정할 집단지성 id
     * @param request           집단지성 수정 정보(내용)
     * @return 수정된 집단지성 정보
     */
    @PatchMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<CommentUpdateResponse> update(
            Authentication authentication,
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestBody(required = false)CommentUpdateRequest request
    ) {
        Long userId = Long.valueOf(authentication.getName());
        CommentUpdateResponse response = commentUpdateService.update(userId, postId, commentId, request);

        return ResponseEntity.ok(response);
    }

    /**
     * [CI-004] 집단지성 삭제
     *
     * @param authentication    사용자 인증 정보
     * @param postId            집단지성이 포함된 게시물 id
     * @param commentId         삭제할 집단지성 id
     * @return 삭제된 집단지성 정보
     */
    @DeleteMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<Void> delete(
            Authentication authentication,
            @PathVariable Long postId,
            @PathVariable Long commentId
    ) {
        Long userId = Long.valueOf(authentication.getName());
        commentDeleteService.delete(userId, postId, commentId);

        return ResponseEntity.noContent().build();
    }

    /**
     * [CI-005] 유익해요 등록
     *
     * @param authentication    사용자 인증 정보
     * @param postId            유익해요 등록될 집단지성이 포함된 게시물 id
     * @param commentId         유익해요 등록될 집단지성 id
     * @return 집단지성의 총 유익해요 수 및 유익해요 등록 결과
     */
    @PostMapping("/{postId}/comments/{commentId}/helpful")
    public ResponseEntity<CommentHelpfulResponse> createHelpful(
            Authentication authentication,
            @PathVariable Long postId,
            @PathVariable Long commentId
    ) {
        Long userId = Long.valueOf(authentication.getName());
        CommentHelpfulResponse response = commentHelpfulService.createHelpful(userId, postId, commentId);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{postId}/comments/{commentId}/helpful")
    public ResponseEntity<CommentHelpfulResponse> deleteHelpful(
            Authentication authentication,
            @PathVariable Long postId,
            @PathVariable Long commentId
    ) {
        Long userId = Long.valueOf(authentication.getName());
        CommentHelpfulResponse response = commentHelpfulService.deleteHelpful(userId, postId, commentId);

        return ResponseEntity.ok(response);
    }
}
