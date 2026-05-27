package com.hogu.am_i_hogu.domain.comment.controller;

import com.hogu.am_i_hogu.common.exception.ErrorResponse;
import com.hogu.am_i_hogu.common.openapi.OpenApiDescription;
import com.hogu.am_i_hogu.domain.comment.dto.request.CommentCreateRequest;
import com.hogu.am_i_hogu.domain.comment.dto.request.CommentUpdateRequest;
import com.hogu.am_i_hogu.domain.comment.dto.request.CursorRequest;
import com.hogu.am_i_hogu.domain.comment.dto.response.CommentCreateResponse;
import com.hogu.am_i_hogu.domain.comment.dto.response.CommentHelpfulResponse;
import com.hogu.am_i_hogu.domain.comment.dto.response.CommentReadResponse;
import com.hogu.am_i_hogu.domain.comment.dto.response.CommentUpdateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

@Tag(name = "Comment", description = "집단지성 API")
public interface CommentApiDoc {

    @Operation(
            operationId = "getComments",
            summary = "집단지성 목록 조회",
            description = "게시물의 집단지성 목록을 무한 스크롤 방식으로 조회한다. Authorization 헤더(Access Token)를 포함하여 요청하면 '내 집단지성 여부(isMine)' 및 '유익해요 누름 여부(isHelpful)'가 유저에 맞게 판별되어 반환된다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CommentReadResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "comments": [
                                            {
                                              "commentId": 1234,
                                              "content": "호구같아요.",
                                              "isMine": false,
                                              "writer": {
                                                "nickname": "민돌이",
                                                "profileImageUrl": "https://...",
                                                "isPostWriter": false
                                              },
                                              "createdAt": "2026-03-31T11:49:05",
                                              "updatedAt": "2026-03-31T11:49:05",
                                              "isDeleted": false,
                                              "isHelpful": false,
                                              "totalHelpfulCount": 32,
                                              "parentId": 12,
                                              "depth": 1
                                            }
                                          ],
                                          "hasNext": false,
                                          "nextCursor": "SDLEI1J3787"
                                        }
                                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (postId 타입 오류 또는 조회 파라미터 오류)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "WRONG_POSTID_TYPE",
                                            summary = "postId 타입 오류",
                                            value = "{\"code\":\"WRONG_POSTID_TYPE\"}"
                                    ),
                                    @ExampleObject(
                                            name = "INVALID_PARAM_VALUE",
                                            summary = "파라미터 오류 (정렬/커서)",
                                            value = """
                                                {
                                                  "code": "INVALID_PARAM_VALUE",
                                                  "errors": [
                                                    {
                                                      "field": "cursor",
                                                      "code": "INVALID_CURSOR"
                                                    },
                                                    {
                                                      "field": "sortBy",
                                                      "code": "MULTIPLE_SORTING"
                                                    }
                                                  ]
                                                }
                                                """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "게시물을 찾을 수 없음 (삭제되었거나 존재하지 않음)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "POST_NOT_FOUND", summary = "존재하지 않는 게시물", value = "{\"code\":\"POST_NOT_FOUND\"}"),
                                    @ExampleObject(name = "POST_ALREADY_DELETED", summary = "삭제된 게시물", value = "{\"code\":\"POST_ALREADY_DELETED\"}")
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "SERVER_ERROR", value = "{\"code\":\"SERVER_ERROR\"}"),
                            }
                    )
            )
    })
    ResponseEntity<CommentReadResponse> read(
            Authentication authentication,

            @Parameter(
                    name = "postId",
                    description = "게시물 고유 식별자",
                    in = ParameterIn.PATH,
                    required = true
            )
            Long postId,

            @ParameterObject
            CursorRequest cursorRequest
    );

    @Operation(
            operationId = "createComment",
            summary = "집단지성 생성",
            description = "특정 게시물에 새로운 집단지성을 작성한다. Authorization 헤더(Access Token)가 필수로 요구된다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "생성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CommentCreateResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "commentId": 1234,
                                          "content": "호구같아요.",
                                          "isMine": false,
                                          "writer": {
                                            "nickname": "민돌이",
                                            "profileImageUrl": "https://...",
                                            "isPostWriter": false
                                          },
                                          "createdAt": "2026-03-31T11:49:05",
                                          "updatedAt": "2026-03-31T11:49:05",
                                          "isHelpful": false,
                                          "totalHelpfulCount": 32,
                                          "parentId": 12,
                                          "depth": 1
                                        }
                                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (타입 오류, 바디 누락, 입력값 정책 위반)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "WRONG_POSTID_TYPE", summary = "postId 타입 오류", value = "{\"code\":\"WRONG_POSTID_TYPE\"}"),
                                    @ExampleObject(name = "EMPTY_REQUEST_BODY", summary = "요청 바디 비어있음", value = "{\"code\":\"EMPTY_REQUEST_BODY\"}"),
                                    @ExampleObject(name = "WRONG_PARENTID_TYPE", summary = "parentId 타입 오류", value = "{\"code\":\"WRONG_PARENTID_TYPE\"}"),
                                    @ExampleObject(
                                            name = "INVALID_INPUT_VALUE",
                                            summary = "입력값 정책 위반 (공백, 길이/뎁스 초과)",
                                            value = """
                                                {
                                                  "code": "INVALID_INPUT_VALUE",
                                                  "errors": [
                                                    {
                                                      "field": "content",
                                                      "code": "EMPTY_CONTENT"
                                                    },
                                                    {
                                                      "field": "depth",
                                                      "code": "DEPTH_EXCEEDED"
                                                    }
                                                  ]
                                                }
                                                """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 (토큰 누락 및 유효하지 않음)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                @ExampleObject(name = "ACCESS_TOKEN_EXPIRED", value = "{\"code\":\"ACCESS_TOKEN_EXPIRED\"}"),
                                @ExampleObject(name = "EMPTY_ACCESS_TOKEN", value = "{\"code\":\"EMPTY_ACCESS_TOKEN\"}"),
                                @ExampleObject(name = "INVALID_ACCESS_TOKEN", value = "{\"code\":\"INVALID_ACCESS_TOKEN\"}")
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "게시물, 부모 집단지성, 또는 사용자를 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "POST_ALREADY_DELETED", summary = "삭제된 게시물", value = "{\"code\":\"POST_ALREADY_DELETED\"}"),
                                    @ExampleObject(name = "POST_NOT_FOUND", summary = "존재하지 않는 게시물", value = "{\"code\":\"POST_NOT_FOUND\"}"),
                                    @ExampleObject(name = "PARENT_ALREADY_DELETED", summary = "삭제된 부모 집단지성", value = "{\"code\":\"PARENT_ALREADY_DELETED\"}"),
                                    @ExampleObject(name = "PARENT_NOT_FOUND", summary = "존재하지 않는 부모 집단지성", value = "{\"code\":\"PARENT_NOT_FOUND\"}"),
                                    @ExampleObject(name = "USER_NOT_FOUND", summary = "존재하지 않는 사용자", value = "{\"code\":\"USER_NOT_FOUND\"}")
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "SERVER_ERROR", value = "{\"code\":\"SERVER_ERROR\"}"),
                            }
                    )
            )
    })
    ResponseEntity<CommentCreateResponse> create(
            Authentication authentication,

            @Parameter(
                    name = "postId",
                    description = "집단지성을 작성하려는 게시물의 고유 식별자",
                    in = ParameterIn.PATH,
                    required = true
            )
            Long postId,

            @RequestBody(
                    description = "집단지성 생성 데이터 (대댓글이 아닐 경우 parentId는 null)"
            )
            CommentCreateRequest request
    );

    @Operation(
            operationId = "updateComment",
            summary = "집단지성 수정",
            description = "특정 게시물에 작성된 자신의 집단지성을 수정한다. 해당 집단지의 작성자(소유자)만 수정할 수 있으며, Authorization 헤더(Access Token)가 필수로 요구된다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "수정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CommentUpdateResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "commentId": 1234,
                                          "content": "호구같아요.",
                                          "isMine": false,
                                          "writer": {
                                            "nickname": "민돌이",
                                            "profileImageUrl": "https://...",
                                            "isPostWriter": false
                                          },
                                          "createdAt": "2026-03-31T11:49:05",
                                          "updatedAt": "2026-03-31T11:49:05",
                                          "isHelpful": false,
                                          "totalHelpfulCount": 32,
                                          "parentId": 12,
                                          "depth": 1
                                        }
                                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (타입 오류, 바디 누락, 본문 길이 및 공백 정책 위반)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "WRONG_POSTID_TYPE", summary = "postId 타입 오류", value = "{\"code\":\"WRONG_POSTID_TYPE\"}"),
                                    @ExampleObject(name = "WRONG_COMMENTID_TYPE", summary = "commentId 타입 오류", value = "{\"code\":\"WRONG_COMMENTID_TYPE\"}"),
                                    @ExampleObject(name = "EMPTY_REQUEST_BODY", summary = "요청 바디 비어있음", value = "{\"code\":\"EMPTY_REQUEST_BODY\"}"),
                                    @ExampleObject(name = "EMPTY_CONTENT", summary = "본문이 비어있거나 공백", value = "{\"code\":\"EMPTY_CONTENT\"}"),
                                    @ExampleObject(name = "CONTENT_LENGTH_EXCEEDED", summary = "본문 길이 초과", value = "{\"code\":\"CONTENT_LENGTH_EXCEEDED\"}")
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 (토큰 누락 및 유효하지 않음)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "ACCESS_TOKEN_EXPIRED", value = "{\"code\":\"ACCESS_TOKEN_EXPIRED\"}"),
                                    @ExampleObject(name = "EMPTY_ACCESS_TOKEN", value = "{\"code\":\"EMPTY_ACCESS_TOKEN\"}"),
                                    @ExampleObject(name = "INVALID_ACCESS_TOKEN", value = "{\"code\":\"INVALID_ACCESS_TOKEN\"}")
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음 (요청한 유저가 해당 집단지성의 작성자가 아님)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "FORBIDDEN_ACCESS", summary = "작성자 불일치", value = "{\"code\":\"FORBIDDEN_ACCESS\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "게시물 또는 집단지성을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "POST_ALREADY_DELETED", summary = "삭제된 게시물", value = "{\"code\":\"POST_ALREADY_DELETED\"}"),
                                    @ExampleObject(name = "POST_NOT_FOUND", summary = "존재하지 않는 게시물", value = "{\"code\":\"POST_NOT_FOUND\"}"),
                                    @ExampleObject(name = "COMMENT_ALREADY_DELETED", summary = "삭제된 집단지성", value = "{\"code\":\"COMMENT_ALREADY_DELETED\"}"),
                                    @ExampleObject(name = "COMMENT_NOT_FOUND", summary = "존재하지 않는 집단지성", value = "{\"code\":\"COMMENT_NOT_FOUND\"}")
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "SERVER_ERROR", value = "{\"code\":\"SERVER_ERROR\"}"),
                            }
                    )
            )
    })
    ResponseEntity<CommentUpdateResponse> update(
            Authentication authentication,

            @Parameter(
                    name = "postId",
                    description = "게시물 고유 식별자",
                    in = ParameterIn.PATH,
                    required = true
            )
            Long postId,

            @Parameter(
                    name = "commentId",
                    description = "수정할 집단지성 고유 식별자",
                    in = ParameterIn.PATH,
                    required = true
            )
            Long commentId,

            @RequestBody(
                    description = "수정할 집단지성 본문 데이터"
            )
            CommentUpdateRequest request
    );

    @Operation(
            operationId = "deleteComment",
            summary = "집단지성 삭제",
            description = "특정 게시물에 작성된 자신의 집단지성을 삭제합니다. 해당 집단지성의 작성자(소유자)만 삭제할 수 있으며, Authorization 헤더(Access Token)가 필수로 요구됩니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "삭제 성공 (응답 본문 없음)"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (postId 또는 commentId 타입 오류)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "WRONG_POSTID_TYPE", summary = "postId 타입 오류", value = "{\"code\":\"WRONG_POSTID_TYPE\"}"),
                                    @ExampleObject(name = "WRONG_COMMENTID_TYPE", summary = "commentId 타입 오류", value = "{\"code\":\"WRONG_COMMENTID_TYPE\"}")
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 (토큰 누락 및 유효하지 않음)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "ACCESS_TOKEN_EXPIRED", value = "{\"code\":\"ACCESS_TOKEN_EXPIRED\"}"),
                                    @ExampleObject(name = "EMPTY_ACCESS_TOKEN", value = "{\"code\":\"EMPTY_ACCESS_TOKEN\"}"),
                                    @ExampleObject(name = "INVALID_ACCESS_TOKEN", value = "{\"code\":\"INVALID_ACCESS_TOKEN\"}")
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음 (요청한 유저가 해당 집단지의 작성자가 아님)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "FORBIDDEN_ACCESS",
                                    summary = "작성자 불일치",
                                    value = "{\"code\":\"FORBIDDEN_ACCESS\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "게시물 또는 집단지성을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "POST_ALREADY_DELETED", summary = "삭제된 게시물", value = "{\"code\":\"POST_ALREADY_DELETED\"}"),
                                    @ExampleObject(name = "POST_NOT_FOUND", summary = "존재하지 않는 게시물", value = "{\"code\":\"POST_NOT_FOUND\"}"),
                                    @ExampleObject(name = "COMMENT_ALREADY_DELETED", summary = "삭제된 집단지성", value = "{\"code\":\"COMMENT_ALREADY_DELETED\"}"),
                                    @ExampleObject(name = "COMMENT_NOT_FOUND", summary = "존재하지 않는 집단지성", value = "{\"code\":\"COMMENT_NOT_FOUND\"}")
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "SERVER_ERROR", value = "{\"code\":\"SERVER_ERROR\"}"),
                            }
                    )
            )
    })
    ResponseEntity<Void> delete(
            Authentication authentication,

            @Parameter(
                    name = "postId",
                    description = "게시물 고유 식별자",
                    in = ParameterIn.PATH,
                    required = true
            )
            Long postId,

            @Parameter(
                    name = "commentId",
                    description = "삭제할 집단지성 고유 식별자",
                    in = ParameterIn.PATH,
                    required = true
            )
            Long commentId
    );

    @Operation(
            operationId = "createCommentHelpful",
            summary = "집단지성 유익해요 등록",
            description = "특정 집단지성에 '유익해요'를 등록한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CommentHelpfulResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "totalHelpfulCount": 123,
                                          "isHelpful": true
                                        }
                                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (postId 또는 commentId 타입 오류)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "WRONG_POSTID_TYPE", value = "{\"code\":\"WRONG_POSTID_TYPE\"}"),
                                    @ExampleObject(name = "WRONG_COMMENTID_TYPE", value = "{\"code\":\"WRONG_COMMENTID_TYPE\"}")
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 (토큰 누락 및 유효하지 않음)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "ACCESS_TOKEN_EXPIRED", value = "{\"code\":\"ACCESS_TOKEN_EXPIRED\"}"),
                                    @ExampleObject(name = "EMPTY_ACCESS_TOKEN", value = "{\"code\":\"EMPTY_ACCESS_TOKEN\"}"),
                                    @ExampleObject(name = "INVALID_ACCESS_TOKEN", value = "{\"code\":\"INVALID_ACCESS_TOKEN\"}")
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음 (본인 집단지성에는 유익해요 불가)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "FORBIDDEN_ACCESS", value = "{\"code\":\"FORBIDDEN_ACCESS\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "게시물 또는 집단지을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "POST_ALREADY_DELETED", value = "{\"code\":\"POST_ALREADY_DELETED\"}"),
                                    @ExampleObject(name = "POST_NOT_FOUND", value = "{\"code\":\"POST_NOT_FOUND\"}"),
                                    @ExampleObject(name = "COMMENT_ALREADY_DELETED", value = "{\"code\":\"COMMENT_ALREADY_DELETED\"}"),
                                    @ExampleObject(name = "COMMENT_NOT_FOUND", value = "{\"code\":\"COMMENT_NOT_FOUND\"}")
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "중복 요청 (이미 유익해요가 선택된 상태)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "DUPLICATE_REQUEST", value = "{\"code\":\"DUPLICATE_REQUEST\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "SERVER_ERROR", value = "{\"code\":\"SERVER_ERROR\"}"),
                            }
                    )
            )
    })
    ResponseEntity<CommentHelpfulResponse> createHelpful(
            Authentication authentication,

            @Parameter(
                    name = "postId",
                    description = "게시물 고유 식별자",
                    in = ParameterIn.PATH,
                    required = true
            )
            Long postId,

            @Parameter(
                    name = "commentId",
                    description = "집단지성 고유 식별자",
                    in = ParameterIn.PATH,
                    required = true
            )
            Long commentId
    );

    @Operation(
            operationId = "deleteCommentHelpful",
            summary = "집단지성 유익해요 취소",
            description = "특정 집단지성에 등록한 '유익해요'를 취소한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CommentHelpfulResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "totalHelpfulCount": 123,
                                          "isHelpful": false
                                        }
                                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (postId 또는 commentId 타입 오류)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "WRONG_POSTID_TYPE", value = "{\"code\":\"WRONG_POSTID_TYPE\"}"),
                                    @ExampleObject(name = "WRONG_COMMENTID_TYPE", value = "{\"code\":\"WRONG_COMMENTID_TYPE\"}")
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 (토큰 누락 및 유효하지 않음)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "ACCESS_TOKEN_EXPIRED", value = "{\"code\":\"ACCESS_TOKEN_EXPIRED\"}"),
                                    @ExampleObject(name = "EMPTY_ACCESS_TOKEN", value = "{\"code\":\"EMPTY_ACCESS_TOKEN\"}"),
                                    @ExampleObject(name = "INVALID_ACCESS_TOKEN", value = "{\"code\":\"INVALID_ACCESS_TOKEN\"}")
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "게시물 또는 집단지성을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "POST_ALREADY_DELETED", value = "{\"code\":\"POST_ALREADY_DELETED\"}"),
                                    @ExampleObject(name = "POST_NOT_FOUND", value = "{\"code\":\"POST_NOT_FOUND\"}"),
                                    @ExampleObject(name = "COMMENT_ALREADY_DELETED", value = "{\"code\":\"COMMENT_ALREADY_DELETED\"}"),
                                    @ExampleObject(name = "COMMENT_NOT_FOUND", value = "{\"code\":\"COMMENT_NOT_FOUND\"}")
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "SERVER_ERROR", value = "{\"code\":\"SERVER_ERROR\"}"),
                            }
                    )
            )
    })
    ResponseEntity<CommentHelpfulResponse> deleteHelpful(
            Authentication authentication,

            @Parameter(
                    name = "postId",
                    description = "게시물 고유 식별자",
                    in = ParameterIn.PATH,
                    required = true
            )
            Long postId,

            @Parameter(
                    name = "commentId",
                    description = "집단지성 고유 식별자",
                    in = ParameterIn.PATH,
                    required = true
            )
            Long commentId
    );
}
