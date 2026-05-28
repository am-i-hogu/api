package com.hogu.am_i_hogu.domain.user.controller;

import com.hogu.am_i_hogu.common.exception.ErrorResponse;
import com.hogu.am_i_hogu.domain.user.dto.request.CursorRequest;
import com.hogu.am_i_hogu.domain.user.dto.request.UpdateProfileRequest;
import com.hogu.am_i_hogu.domain.user.dto.response.*;
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
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "User", description = "유저 API")
public interface UserApiDoc {

    @Operation(
            operationId = "updateProfile",
            summary = "프로필 수정",
            description = "사용자의 닉네임 또는 프로필 이미지를 수정한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UpdateProfileResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "SUCCESS_WITH_IMAGE_UPDATE",
                                            summary = "이미지 업데이트 하는 경우",
                                            value = """
                                                {
                                                  "id": 1,
                                                  "nickname": "hogu123",
                                                  "profileImageUrl": "https://..."
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "SUCCESS_WITH_IMAGE_DELETE",
                                            summary = "이미지 삭제하는 경우",
                                            value = """
                                                {
                                                  "id": 1,
                                                  "nickname": "hogu123",
                                                  "profileImageUrl": null
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "SUCCESS_WITHOUT_IMAGE",
                                            summary = "이미지 유지하는 경우",
                                            value = """
                                                {
                                                  "id": 1,
                                                  "nickname": "hogu123"
                                                }
                                                """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = """
                        잘못된 요청으로 인해 실패한다. 다음 오류 코드가 발생할 수 있다:
                        * `EMPTY_REQUEST_BODY`: 요청 필드가 비어있음
                        * `INVALID_INPUT_VALUE`: 요청 필드 유효성 검사 실패 (닉네임 길이 초과, 공백, 특수문자 및 이미지 URL 오류 등)
                        """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "EMPTY_REQUEST_BODY",
                                            value = "{\"code\":\"EMPTY_REQUEST_BODY\"}"
                                    ),
                                    @ExampleObject(
                                            name = "INVALID_INPUT_VALUE_1",
                                            value = """
                                                {
                                                  "code": "INVALID_INPUT_VALUE",
                                                  "errors": [
                                                    {
                                                      "field": "nickname",
                                                      "code": "EMPTY_NICKNAME"
                                                    },
                                                    {
                                                      "field": "images",
                                                      "code": "INVALID_IMAGE_URL"
                                                    }
                                                  ]
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "INVALID_INPUT_VALUE_2",
                                            value = """
                                                {
                                                  "code": "INVALID_INPUT_VALUE",
                                                  "errors": [
                                                    {
                                                      "field": "nickname",
                                                      "code": "SPECIAL_CHAR_NICKNAME"
                                                    },
                                                    {
                                                      "field": "nickname",
                                                      "code": "NICKNAME_LENGTH_EXCEEDED"
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
                    description = """
                            access token 관련 오류로 실패한다. 다음 오류 코드가 발생할 수 있다:
                            * `ACCESS_TOKEN_EXPIRED`: access token이 만료된 경우
                            * `EMPTY_ACCESS_TOKEN`: access token이 없는 경우
                            * `INVALID_ACCESS_TOKEN`: access token이 유효하지 않은 경우
                            """,
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
                    description = """
                        사용자를 찾을 수 없어 실패한다. 다음 오류 코드가 발생할 수 있다:
                        * `USER_NOT_FOUND`: 존재하지 않는 사용자인 경우
                        """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "USER_NOT_FOUND", value = "{\"code\":\"USER_NOT_FOUND\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = """
                        충돌로 인해 실패한다. 다음 오류 코드가 발생할 수 있다:
                        * `DUPLICATE_NICKNAME`: 닉네임이 중복된 경우
                        """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "DUPLICATE_NICKNAME", value = "{\"code\":\"DUPLICATE_NICKNAME\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = """
                    서버 오류로 실패한다. 다음 오류 코드가 발생할 수 있다:
                    * `SERVER_ERROR`: 서버 오류
                    """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "SERVER_ERROR", value = "{\"code\":\"SERVER_ERROR\"}"),
                            }
                    )
            )
    })
    ResponseEntity<UpdateProfileResponse> updateProfile(
            Authentication authentication,
            @RequestBody(required = false) UpdateProfileRequest request
    );

    @Operation(
            operationId = "checkNickname",
            summary = "닉네임 중복 검사",
            description = "사용하고자 하는 닉네임의 중복 여부 및 유효성을 검사한다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CheckNicknameResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "AVAILABLE",
                                            summary = "사용 가능",
                                            value = """
                                                {
                                                  "isAvailable": true
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "DUPLICATE",
                                            summary = "사용 불가능 (중복)",
                                            value = """
                                                {
                                                  "isAvailable": false
                                                }
                                                """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = """
                        잘못된 요청으로 인해 실패한다. 다음 오류 코드가 발생할 수 있다:
                        * 상위 오류 코드: `INVALID_PARAM_VALUE`
                        * field: `nickname`
                        * 하위 오류 코드:
                            * `EMPTY_NICKNAME`: 닉네임이 공백으로만 이루어져 있거나 비어있는 경우
                            * `SPECIAL_CHAR_NICKNAME`: 닉네임에 특수문자가 포함되어 있는 경우
                            * `NICKNAME_LENGTH_EXCEEDED`: 닉네임 길이가 부족하거나 초과된 경우
                        """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "EMPTY_NICKNAME",
                                            value = """
                                                {
                                                  "code": "INVALID_PARAM_VALUE",
                                                  "errors": [
                                                    {
                                                      "field": "nickname",
                                                      "code": "EMPTY_NICKNAME"
                                                    }
                                                  ]
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "MULTIPLE_ERRORS",
                                            value = """
                                                {
                                                  "code": "INVALID_PARAM_VALUE",
                                                  "errors": [
                                                    {
                                                      "field": "nickname",
                                                      "code": "SPECIAL_CHAR_NICKNAME"
                                                    },
                                                    {
                                                      "field": "nickname",
                                                      "code": "NICKNAME_LENGTH_EXCEEDED"
                                                    }
                                                  ]
                                                }
                                                """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = """
                    서버 오류로 실패한다. 다음 오류 코드가 발생할 수 있다:
                    * `SERVER_ERROR`: 서버 오류
                    """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "SERVER_ERROR", value = "{\"code\":\"SERVER_ERROR\"}"),
                            }
                    )
            )
    })
    ResponseEntity<CheckNicknameResponse> checkNickname(
            @Parameter(
                    name = "nickname",
                    description = "중복 검사할 닉네임",
                    in = ParameterIn.QUERY,
                    required = true
            )
            @RequestParam(name="nickname", required = false) String nickname
    );

    @Operation(
            operationId = "getMyPage",
            summary = "마이페이지 조회",
            description = "사용자의 마이페이지 정보를 조회한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = MyPageResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "nickname": "민돌이",
                                          "profileImageUrl": "https://...",
                                          "hoguIndex": 60,
                                          "hoguLevel": "RISKY",
                                          "hoguShortDescription": "거절보다 양보가 앞서는 타입"
                                        }
                                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = """
                            access token 관련 오류로 실패한다. 다음 오류 코드가 발생할 수 있다:
                            * `ACCESS_TOKEN_EXPIRED`: access token이 만료된 경우
                            * `EMPTY_ACCESS_TOKEN`: access token이 없는 경우
                            * `INVALID_ACCESS_TOKEN`: access token이 유효하지 않은 경우
                            """,
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
                    description = """
                        사용자를 찾을 수 없어 실패한다. 다음 오류 코드가 발생할 수 있다:
                        * `USER_NOT_FOUND`: 토큰은 존재하지만 DB에서 사용자 정보를 찾을 수 없는 경우
                        """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "USER_NOT_FOUND", value = "{\"code\":\"USER_NOT_FOUND\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = """
                    서버 오류로 실패한다. 다음 오류 코드가 발생할 수 있다:
                    * `SERVER_ERROR`: 서버 오류
                    """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "SERVER_ERROR", value = "{\"code\":\"SERVER_ERROR\"}"),
                            }
                    )
            )
    })
    ResponseEntity<MyPageResponse> getMyPage(Authentication authentication);

    @Operation(
            operationId = "getHoguReport",
            summary = "호구 리포트 조회",
            description = "사용자의 호구 리포트 정보를 조회한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = HoguReportResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "nickname": "민돌이",
                                          "profileImageUrl": "https://...",
                                          "hoguIndex": 60,
                                          "hoguLevel": "RISKY",
                                          "hoguShortDescription": "거절보다 양보가 앞서는 타입",
                                          "hoguDescription": "평소에는 괜찮지만, 상대가 강하게 나오거나 관계가 신경 쓰이는 상황에서 본인 기준이 흔들릴 수 있어요. 결정 전에 '내가 감당할 손해인가?'를 한 번 체크해보세요.",
                                          "categoryAnalysis": [
                                            {
                                              "category": "DATING",
                                              "hoguIndex": 80,
                                              "hoguLevel": "CRITICAL"
                                            },
                                            {
                                              "category": "USED_TRADE",
                                              "hoguIndex": 40,
                                              "hoguLevel": "WARNING"
                                            }
                                          ],
                                          "totalPostCount": 12,
                                          "hoguPostCount": 8,
                                          "notHoguPostCount": 4
                                        }
                                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = """
                            access token 관련 오류로 실패한다. 다음 오류 코드가 발생할 수 있다:
                            * `ACCESS_TOKEN_EXPIRED`: access token이 만료된 경우
                            * `EMPTY_ACCESS_TOKEN`: access token이 없는 경우
                            * `INVALID_ACCESS_TOKEN`: access token이 유효하지 않은 경우
                            """,
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
                    description = """
                        사용자를 찾을 수 없어 실패한다. 다음 오류 코드가 발생할 수 있다:
                        * `USER_NOT_FOUND`: 토큰은 존재하지만 DB에서 사용자 정보를 찾을 수 없는 경우
                        """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "USER_NOT_FOUND", value = "{\"code\":\"USER_NOT_FOUND\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = """
                    서버 오류로 실패한다. 다음 오류 코드가 발생할 수 있다:
                    * `SERVER_ERROR`: 서버 오류
                    """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "SERVER_ERROR", value = "{\"code\":\"SERVER_ERROR\"}"),
                            }
                    )
            )
    })
    ResponseEntity<HoguReportResponse> getHoguReport(Authentication authentication);

    @Operation(
            operationId = "getMyPosts",
            summary = "내가 쓴 게시물 목록 조회",
            description = "사용자가 작성한 게시물 목록을 무한 스크롤 방식으로 조회한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = MyPostListResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "posts": [
                                            {
                                              "postId": 123,
                                              "title": "내가 쓴 첫 번재 글",
                                              "category": "USED_TRADE",
                                              "createdAt": "2026-03-31T11:49:05",
                                              "voteSummary": "HOGU",
                                              "commentCount": 1
                                            },
                                            {
                                              "postId": 1234,
                                              "title": "내가 쓴 두 번재 글",
                                              "category": "USED_TRADE",
                                              "createdAt": "2026-03-31T11:49:05",
                                              "voteSummary": "NONE",
                                              "commentCount": 2
                                            }
                                          ],
                                          "hasNext": false,
                                          "nextCursor": "DKLJEI2JDLKJ"
                                        }
                                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = """
                        잘못된 요청으로 인해 실패한다. 다음 오류 코드가 발생할 수 있다:
                        * `INVALID_CURSOR`: 유효하지 않은 cursor 값인 경우
                        """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "INVALID_PARAM_VALUE",
                                    value = """
                                        {
                                          "code": "INVALID_PARAM_VALUE",
                                          "errors": [
                                            {
                                              "field": "cursor",
                                              "code": "INVALID_CURSOR"
                                            }
                                          ]
                                        }
                                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = """
                            access token 관련 오류로 실패한다. 다음 오류 코드가 발생할 수 있다:
                            * `ACCESS_TOKEN_EXPIRED`: access token이 만료된 경우
                            * `EMPTY_ACCESS_TOKEN`: access token이 없는 경우
                            * `INVALID_ACCESS_TOKEN`: access token이 유효하지 않은 경우
                            """,
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
                    responseCode = "500",
                    description = """
                    서버 오류로 실패한다. 다음 오류 코드가 발생할 수 있다:
                    * `SERVER_ERROR`: 서버 오류
                    """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "SERVER_ERROR", value = "{\"code\":\"SERVER_ERROR\"}"),
                            }
                    )
            )
    })
    ResponseEntity<MyPostListResponse> getMyPosts(
            Authentication authentication,
            @ParameterObject @ModelAttribute CursorRequest cursorRequest
    );

    @Operation(
            operationId = "getMyComments",
            summary = "내가 쓴 댓글(집단지성) 목록 조회",
            description = "사용자가 작성한 댓글(집단지성) 목록을 무한 스크롤 방식으로 조회한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = MyCommentListResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "comments": [
                                            {
                                              "commentId": 1234,
                                              "content": "나의 첫 번째 댓글",
                                              "createdAt": "2026-03-31T11:49:05",
                                              "post": {
                                                "postId": 12,
                                                "title": "집단지성이 포함된 게시물의 제목입니다.",
                                                "category": "USED_TRADE",
                                                "commentCount": 123,
                                                "isDeleted": false
                                              }
                                            },
                                            {
                                              "commentId": 123,
                                              "content": "나의 두 번째 댓글",
                                              "createdAt": "2026-03-31T11:49:05",
                                              "post": {
                                                "postId": 12344,
                                                "title": "집단지성이 포함된 게시물의 제목입니다.",
                                                "category": "USED_TRADE",
                                                "commentCount": 123,
                                                "isDeleted": false
                                              }
                                            }
                                          ],
                                          "hasNext": false,
                                          "nextCursor": "DLKJEIU4KJDI29"
                                        }
                                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = """
                        잘못된 요청으로 인해 실패한다. 다음 오류 코드가 발생할 수 있다:
                        * 상위 오류 코드: `INVALID_PARAM_VALUE'
                        * field: `cursor`
                        * 하위 오류 코드:
                            * `INVALID_CURSOR`: 유효하지 않은 cursor 값인 경우
                        """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "INVALID_PARAM_VALUE",
                                    value = """
                                        {
                                          "code": "INVALID_PARAM_VALUE",
                                          "errors": [
                                            {
                                              "field": "cursor",
                                              "code": "INVALID_CURSOR"
                                            }
                                          ]
                                        }
                                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = """
                            access token 관련 오류로 실패한다. 다음 오류 코드가 발생할 수 있다:
                            * `ACCESS_TOKEN_EXPIRED`: access token이 만료된 경우
                            * `EMPTY_ACCESS_TOKEN`: access token이 없는 경우
                            * `INVALID_ACCESS_TOKEN`: access token이 유효하지 않은 경우
                            """,
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
                    responseCode = "500",
                    description = """
                    서버 오류로 실패한다. 다음 오류 코드가 발생할 수 있다:
                    * `SERVER_ERROR`: 서버 오류
                    """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "SERVER_ERROR", value = "{\"code\":\"SERVER_ERROR\"}"),
                            }
                    )
            )
    })
    ResponseEntity<MyCommentListResponse> getMyComments(
            Authentication authentication,
            @ParameterObject @ModelAttribute CursorRequest cursorRequest
    );

    @Operation(
            operationId = "getMyBookmarks",
            summary = "내가 북마크한 게시물 목록 조회",
            description = "사용자가 북마크한 게시물 목록을 무한 스크롤 방식으로 조회한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = MyBookmarkListResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "posts": [
                                            {
                                              "postId": 123,
                                              "title": "내가 쓴 첫 번재 글",
                                              "category": "USED_TRADE",
                                              "createdAt": "2026-03-31T11:49:05",
                                              "voteSummary": "HOGU",
                                              "commentCount": 1,
                                              "isDeleted": false
                                            },
                                            {
                                              "postId": 1234,
                                              "title": "내가 쓴 두 번재 글",
                                              "category": "USED_TRADE",
                                              "createdAt": "2026-03-31T11:49:05",
                                              "voteSummary": "NONE",
                                              "commentCount": 2,
                                              "isDeleted": false
                                            }
                                          ],
                                          "hasNext": false,
                                          "nextCursor": "DLKJREIU2"
                                        }
                                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = """
                        잘못된 요청으로 인해 실패한다. 다음 오류 코드가 발생할 수 있다:
                        * 상위 오류 코드: `INVALID_PARAM_VALUE'
                        * field: `cursor`
                        * 하위 오류 코드:
                            * `INVALID_CURSOR`: 유효하지 않은 cursor 값인 경우
                        """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "INVALID_PARAM_VALUE",
                                    value = """
                                        {
                                          "code": "INVALID_PARAM_VALUE",
                                          "errors": [
                                            {
                                              "field": "cursor",
                                              "code": "INVALID_CURSOR"
                                            }
                                          ]
                                        }
                                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = """
                            access token 관련 오류로 실패한다. 다음 오류 코드가 발생할 수 있다:
                            * `ACCESS_TOKEN_EXPIRED`: access token이 만료된 경우
                            * `EMPTY_ACCESS_TOKEN`: access token이 없는 경우
                            * `INVALID_ACCESS_TOKEN`: access token이 유효하지 않은 경우
                            """,
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
                    responseCode = "500",
                    description = """
                    서버 오류로 실패한다. 다음 오류 코드가 발생할 수 있다:
                    * `SERVER_ERROR`: 서버 오류
                    """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "SERVER_ERROR", value = "{\"code\":\"SERVER_ERROR\"}"),
                            }
                    )
            )
    })
    ResponseEntity<MyBookmarkListResponse> getMyBookmarks(
            Authentication authentication,
            @ParameterObject @ModelAttribute CursorRequest cursorRequest
    );

    @Operation(
            operationId = "getMyVotes",
            summary = "내가 투표한 게시물 목록 조회",
            description = "사용자가 투표한 게시물 목록을 무한 스크롤 방식으로 조회한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = MyVoteListResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "votes": [
                                            {
                                              "myVote": "HOGU",
                                              "createdAt": "2026-03-31T11:49:05",
                                              "post": {
                                                "postId": 123,
                                                "title": "첫 번째로 투표한 게시물의 제목입니다.",
                                                "category": "USED_TRADE",
                                                "commentCount": 1,
                                                "isDeleted": false
                                              }
                                            },
                                            {
                                              "myVote": "HOGU",
                                              "createdAt": "2026-03-31T11:49:05",
                                              "post": {
                                                "postId": 412,
                                                "title": "두 번째로 투표한 게시물의 제목입니다.",
                                                "category": "USED_TRADE",
                                                "commentCount": 0,
                                                "isDeleted": false
                                              }
                                            }
                                          ],
                                          "hasNext": false,
                                          "nextCursor": "DLDUFKJ3"
                                        }
                                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = """
                        잘못된 요청으로 인해 실패한다. 다음 오류 코드가 발생할 수 있다:
                        * 상위 오류 코드: `INVALID_PARAM_VALUE'
                        * field: `cursor`
                        * 하위 오류 코드:
                            * `INVALID_CURSOR`: 유효하지 않은 cursor 값인 경우
                        """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "INVALID_PARAM_VALUE",
                                    value = """
                                        {
                                          "code": "INVALID_PARAM_VALUE",
                                          "errors": [
                                            {
                                              "field": "cursor",
                                              "code": "INVALID_CURSOR"
                                            }
                                          ]
                                        }
                                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = """
                            access token 관련 오류로 실패한다. 다음 오류 코드가 발생할 수 있다:
                            * `ACCESS_TOKEN_EXPIRED`: access token이 만료된 경우
                            * `EMPTY_ACCESS_TOKEN`: access token이 없는 경우
                            * `INVALID_ACCESS_TOKEN`: access token이 유효하지 않은 경우
                            """,
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
                    responseCode = "500",
                    description = """
                    서버 오류로 실패한다. 다음 오류 코드가 발생할 수 있다:
                    * `SERVER_ERROR`: 서버 오류
                    """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "SERVER_ERROR", value = "{\"code\":\"SERVER_ERROR\"}"),
                            }
                    )
            )
    })
    ResponseEntity<MyVoteListResponse> getMyVotes(
            Authentication authentication,
            @ParameterObject @ModelAttribute CursorRequest cursorRequest
    );
}
