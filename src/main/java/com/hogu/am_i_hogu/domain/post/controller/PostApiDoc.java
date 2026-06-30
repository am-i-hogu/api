package com.hogu.am_i_hogu.domain.post.controller;

import com.hogu.am_i_hogu.common.exception.ErrorResponse;
import com.hogu.am_i_hogu.domain.post.dto.request.HomePostSearchRequest;
import com.hogu.am_i_hogu.domain.post.dto.request.PostCreateRequest;
import com.hogu.am_i_hogu.domain.post.dto.request.PostUpdateRequest;
import com.hogu.am_i_hogu.domain.post.dto.request.PostVoteRequest;
import com.hogu.am_i_hogu.domain.post.dto.response.HomePostListResponse;
import com.hogu.am_i_hogu.domain.post.dto.response.PostBookmarkResponse;
import com.hogu.am_i_hogu.domain.post.dto.response.PostCreateResponse;
import com.hogu.am_i_hogu.domain.post.dto.response.PostDetailResponse;
import com.hogu.am_i_hogu.domain.post.dto.response.PostUpdateResponse;
import com.hogu.am_i_hogu.domain.post.dto.response.PostVoteResponse;
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

@Tag(name = "Post", description = "게시물 API")
public interface PostApiDoc {

    @Operation(
            operationId = "getHomePosts",
            summary = "홈 게시물 목록 조회",
            description = "홈 화면 및 검색 결과에서 게시물 목록을 무한 스크롤 방식으로 조회한다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = HomePostListResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "totalPostCount": 123,
                                          "posts": [
                                            {
                                              "postId": 1234,
                                              "isBookmarked": true,
                                              "categories": ["USED_TRADE"],
                                              "title": "제목입니다",
                                              "createdAt": "2026-03-31T11:49:05",
                                              "viewCount": 12,
                                              "contentPreview": "본문프리뷰입니다",
                                              "thumbnailUrl": "https://...",
                                              "totalVoteCount": 100,
                                              "commentCount": 3,
                                              "writer": {
                                                "nickname": "hogu",
                                                "profileImageUrl": "https://..."
                                              }
                                            },
                                            {
                                              "postId": 1235,
                                              "isBookmarked": false,
                                              "categories": ["USED_TRADE"],
                                              "title": "제목입니다",
                                              "createdAt": "2026-03-31T11:49:05",
                                              "viewCount": 12,
                                              "contentPreview": "본문프리뷰입니다",
                                              "thumbnailUrl": "https://...",
                                              "totalVoteCount": 100,
                                              "commentCount": 3,
                                              "writer": {
                                                "nickname": "nothogu",
                                                "profileImageUrl": "https://..."
                                              }
                                            }
                                          ],
                                          "hasNext": true,
                                          "nextCursor": "1E2N3C4O5D6E7D8V9A0LUE"
                                        }
                                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = """
                            잘못된 요청으로 인해 실패한다. 다음 오류 코드가 발생할 수 있다:
                            * 상위 오류 코드: `INVALID_PARAM_VALUE`
                            * field: `keyword`, `categories`, `sortBy`, `cursor`
                            * 하위 오류 코드:
                                * 필드가 `keyword`:
                                    * `EMPTY_KEYWORD`: 키워드가 공백으로만 이루어져 있거나 비어있는 경우
                                * 필드가 `categories`:
                                    * `INVALID_CATEGORIES`: 유효하지 않은 카테고리인 경우
                                * 필드가 `sortBy`:
                                    * `INVALID_SORTING`: 유효하지 않은 정렬 기준인 경우
                                    * `MULTIPLE_SORTING`: 정렬 기준이 다중 선택된 경우
                                * 필드가 `cursor`:
                                    * `INVALID_CURSOR`: 유효하지 않은 cursor인 경우
                            """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "INVALID_PARAM_VALUE",
                                            value = """
                                                {
                                                  "code": "INVALID_PARAM_VALUE",
                                                  "errors": [
                                                    {
                                                      "field": "keyword",
                                                      "code": "EMPTY_KEYWORD"
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
    ResponseEntity<HomePostListResponse> getHomePosts(
            Authentication authentication,
            @ParameterObject @ModelAttribute HomePostSearchRequest request
    );

    @Operation(
            operationId = "getPostDetail",
            summary = "게시물 상세 조회",
            description = "특정 게시물의 상세 정보를 조회한다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PostDetailResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "postId": 1234,
                                          "isMine": false,
                                          "isBookmarked": true,
                                          "categories": ["USED_TRADE"],
                                          "title": "안녕하세요",
                                          "createdAt": "2026-03-31T11:49:05",
                                          "updatedAt": "2026-03-31T11:49:05",
                                          "viewCount": 12,
                                          "content": "본문입니다",
                                          "images": [
                                            {
                                              "imageUrl": "https://example.com/image1.jpg",
                                              "order": 0,
                                              "isThumbnail": true
                                            },
                                            {
                                              "imageUrl": "https://example.com/image2.jpg",
                                              "order": 1,
                                              "isThumbnail": false
                                            }
                                          ],
                                          "vote": {
                                            "totalVotes": 100,
                                            "yesVotes": 70,
                                            "noVotes": 30,
                                            "myVote": "HOGU"
                                          },
                                          "writer": {
                                            "nickname": "hogu",
                                            "profileImageUrl": "https://..."
                                          }
                                        }
                                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = """
                        잘못된 요청으로 인해 실패한다. 다음 오류 코드가 발생할 수 있다:
                        * `WRONG_POSTID_TYPE`: 잘못된 postId 타입
                        """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "WRONG_POSTID_TYPE", value = "{\"code\":\"WRONG_POSTID_TYPE\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = """
                            게시물을 찾을 수 없어 실패한다. 다음 오류 코드가 발생할 수 있다:
                            * `POST_NOT_FOUND`: 게시물이 존재하지 않음
                            * `POST_ALREADY_DELETED`: 이미 삭제된 게시물
                            """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "POST_NOT_FOUND", value = "{\"code\":\"POST_NOT_FOUND\"}"),
                                    @ExampleObject(name = "POST_ALREADY_DELETED", value = "{\"code\":\"POST_ALREADY_DELETED\"}")
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
    ResponseEntity<PostDetailResponse> getPostById(
            @Parameter(
                    name = "postId",
                    description = "게시물 고유 식별자",
                    in = ParameterIn.PATH,
                    required = true
            )
            Long postId,
            Authentication authentication
    );

    @Operation(
            operationId = "createPost",
            summary = "게시물 생성",
            description = "새로운 게시물을 생성한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "생성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PostCreateResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "postId": 1234
                                        }
                                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = """
                            잘못된 요청으로 인해 실패한다. 다음 오류 코드가 발생할 수 있다:
                            * 상위 오류 코드: `INVALID_INPUT_VALUE`
                            * field: `title`, `categories`, `content`, `images`
                            * 하위 오류 코드:
                                * 필드가 `title`:
                                    * `EMPTY_TITLE`: 제목이 공백으로만 이루어져 있거나 비어있는 경우
                                    * `TITLE_LENGTH_EXCEEDED`: 제목 길이를 초과한 경우
                                * 필드가 `categories`:
                                    * `EMPTY_CATEGORIES`: 카테고리가 아무것도 선택되지 않은 경우
                                    * `MULTIPLE_CATEGORIES`: 카테고리가 다중 선택된 경우
                                    * `INVALID_CATEGORIES`: 존재하지 않는 카테고리인 경우
                                * 필드가 `content`:
                                    * `EMPTY_CONTENT`: 본문이 공백으로만 이루어져 있거나 비어있는 경우
                                * 필드가 `images`:
                                    * `IMAGE_COUNT_EXCEEDED`: 이미지 개수를 초과한 경우
                                    * `INVALID_IMAGE_URL`: 이미지 URL이 유효하지 않은 경우
                                    * `EMPTY_IMAGE_URL`: 이미지 URL이 빈 문자열인 경우
                                    * `DUPLICATE_IMAGE_URL`: 중복된 이미지 URL이 존재하는 경우
                                    * `EMPTY_IMAGE_ORDER`: order 필드가 없는 경우
                                    * `EMPTY_THUMBNAIL`: 썸네일이 지정되지 않은 경우
                                    * `MULTIPLE_THUMBNAIL`: 썸네일이 2개 이상 지정된 경우
                            """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "EMPTY_REQUEST_BODY", value = "{\"code\":\"EMPTY_REQUEST_BODY\"}"),
                                    @ExampleObject(
                                            name = "INVALID_INPUT_VALUE",
                                            value = """
                                                {
                                                  "code": "INVALID_INPUT_VALUE",
                                                  "errors": [
                                                    {
                                                      "field": "title",
                                                      "code": "EMPTY_TITLE"
                                                    },
                                                    {
                                                      "field": "images",
                                                      "code": "INVALID_IMAGE_URL"
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
    ResponseEntity<PostCreateResponse> createPost(
            Authentication authentication,
            @RequestBody PostCreateRequest request
    );

    @Operation(
            operationId = "updatePost",
            summary = "게시물 수정",
            description = "특정 게시물을 수정한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PostUpdateResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "postId": 1234
                                        }
                                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = """
                            잘못된 요청으로 인해 실패한다. 다음 오류 코드가 발생할 수 있다:
                            * 상위 오류 코드: `INVALID_INPUT_VALUE`
                            * field: `title`, `categories`, `content`, `images`
                            * 하위 오류 코드:
                                * 필드가 `title`:
                                    * `EMPTY_TITLE`: 제목이 공백으로만 이루어져 있거나 비어있는 경우
                                    * `TITLE_LENGTH_EXCEEDED`: 제목 길이를 초과한 경우
                                * 필드가 `categories`:
                                    * `EMPTY_CATEGORIES`: 카테고리가 아무것도 선택되지 않은 경우
                                    * `INVALID_CATEGORIES`: 존재하지 않는 카테고리인 경우
                                    * `MULTIPLE_CATEGORIES`: 카테고리가 다중 선택된 경우
                                * 필드가 `content`:
                                    * `EMPTY_CONTENT`: 본문이 공백으로만 이루어져 있거나 비어있는 경우
                                * 필드가 `images`:
                                    * `IMAGE_COUNT_EXCEEDED`: 이미지 개수를 초과한 경우
                                    * `INVALID_IMAGE_URL`: 이미지 URL이 유효하지 않은 경우
                                    * `EMPTY_IMAGE_URL`: 이미지 URL이 빈 문자열인 경우
                                    * `DUPLICATE_IMAGE_URL`: 중복된 이미지 URL이 존재하는 경우
                                    * `EMPTY_IMAGE_ORDER`: order 필드가 없는 경우
                                    * `EMPTY_THUMBNAIL`: 썸네일이 지정되지 않은 경우
                                    * `MULTIPLE_THUMBNAIL`: 썸네일이 2개 이상 지정된 경우
                            """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "WRONG_POSTID_TYPE", value = "{\"code\":\"WRONG_POSTID_TYPE\"}"),
                                    @ExampleObject(name = "EMPTY_REQUEST_BODY", value = "{\"code\":\"EMPTY_REQUEST_BODY\"}"),
                                    @ExampleObject(
                                            name = "INVALID_INPUT_VALUE_TITLE",
                                            value = """
                                                {
                                                  "code": "INVALID_INPUT_VALUE",
                                                  "errors": [
                                                    {
                                                      "field": "title",
                                                      "code": "EMPTY_TITLE"
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
                                            name = "INVALID_INPUT_VALUE_IMAGES",
                                            value = """
                                                {
                                                  "code": "INVALID_INPUT_VALUE",
                                                  "errors": [
                                                    {
                                                      "field": "images",
                                                      "code": "INVALID_IMAGE_URL"
                                                    },
                                                    {
                                                      "field": "images",
                                                      "code": "IMAGE_COUNT_EXCEEDED"
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
                    responseCode = "403",
                    description = """
                            권한이 없어 실패한다. 다음 오류 코드가 발생할 수 있다:
                            * `FORBIDDEN_ACCESS`: 게시물 작성자가 아닌 경우
                            """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "FORBIDDEN_ACCESS", value = "{\"code\":\"FORBIDDEN_ACCESS\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = """
                            게시물을 찾을 수 없어 실패한다. 다음 오류 코드가 발생할 수 있다:
                            * `POST_ALREADY_DELETED`: 이미 삭제된 게시물인 경우
                            * `POST_NOT_FOUND`: 게시물을 찾을 수 없는 경우
                            """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "POST_ALREADY_DELETED", value = "{\"code\":\"POST_ALREADY_DELETED\"}"),
                                    @ExampleObject(name = "POST_NOT_FOUND", value = "{\"code\":\"POST_NOT_FOUND\"}")
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
    ResponseEntity<PostUpdateResponse> updatePost(
            @Parameter(
                    name = "postId",
                    description = "게시물 고유 식별자",
                    in = ParameterIn.PATH,
                    required = true
            )
            Long postId,

            Authentication authentication,

            PostUpdateRequest request
    );

    @Operation(
            operationId = "deletePost",
            summary = "게시물 삭제",
            description = "특정 게시물을 삭제한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "성공"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = """
                            잘못된 요청으로 인해 실패한다. 다음 오류 코드가 발생할 수 있다:
                            * `WRONG_POSTID_TYPE`: postId 타입이 잘못된 경우
                            """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "WRONG_POSTID_TYPE", value = "{\"code\":\"WRONG_POSTID_TYPE\"}")
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
                    responseCode = "403",
                    description = """
                            권한이 없어 실패한다. 다음 오류 코드가 발생할 수 있다:
                            * `FORBIDDEN_ACCESS`: 게시물 작성자가 아닌 경우
                            """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "FORBIDDEN_ACCESS", value = "{\"code\":\"FORBIDDEN_ACCESS\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = """
                            게시물을 찾을 수 없어 실패한다. 다음 오류 코드가 발생할 수 있다:
                            * `POST_ALREADY_DELETED`: 이미 삭제된 게시물인 경우
                            * `POST_NOT_FOUND`: 게시물을 찾을 수 없는 경우
                            """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "POST_ALREADY_DELETED", value = "{\"code\":\"POST_ALREADY_DELETED\"}"),
                                    @ExampleObject(name = "POST_NOT_FOUND", value = "{\"code\":\"POST_NOT_FOUND\"}")
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
    ResponseEntity<Void> deletePost(
            @Parameter(
                    name = "postId",
                    description = "게시물 고유 식별자",
                    in = ParameterIn.PATH,
                    required = true
            )
            Long postId,

            Authentication authentication
    );

    @Operation(
            operationId = "createBookmark",
            summary = "게시물 북마크 추가",
            description = "특정 게시물을 북마크에 추가한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PostBookmarkResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "isBookmarked": true
                                        }
                                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = """
                            잘못된 요청으로 인해 실패한다. 다음 오류 코드가 발생할 수 있다:
                            * `WRONG_POSTID_TYPE`: postId 타입이 잘못된 경우
                            """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "WRONG_POSTID_TYPE", value = "{\"code\":\"WRONG_POSTID_TYPE\"}")
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
                            게시물을 찾을 수 없어 실패한다. 다음 오류 코드가 발생할 수 있다:
                            * `POST_ALREADY_DELETED`: 이미 삭제된 게시물인 경우
                            * `POST_NOT_FOUND`: 게시물을 찾을 수 없는 경우
                            """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "POST_ALREADY_DELETED", value = "{\"code\":\"POST_ALREADY_DELETED\"}"),
                                    @ExampleObject(name = "POST_NOT_FOUND", value = "{\"code\":\"POST_NOT_FOUND\"}")
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = """
                        충돌로 인해 실패한다. 다음 오류 코드가 발생할 수 있다:
                        * `DUPLICATE_REQUEST`: 이미 북마크가 등록되어 있는 경우
                        """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "DUPLICATE_REQUEST", value = "{\"code\":\"DUPLICATE_REQUEST\"}")
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
    ResponseEntity<PostBookmarkResponse> createBookmark(
            @Parameter(
                    name = "postId",
                    description = "게시물 고유 식별자",
                    in = ParameterIn.PATH,
                    required = true
            )
            Long postId,

            Authentication authentication
    );

    @Operation(
            operationId = "deleteBookmark",
            summary = "게시물 북마크 취소",
            description = "특정 게시물에 등록한 북마크를 취소한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PostBookmarkResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "isBookmarked": false
                                        }
                                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = """
                            잘못된 요청으로 인해 실패한다. 다음 오류 코드가 발생할 수 있다:
                            * `WRONG_POSTID_TYPE`: postId 타입이 잘못된 경우
                            """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "WRONG_POSTID_TYPE", value = "{\"code\":\"WRONG_POSTID_TYPE\"}")
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
                            게시물을 찾을 수 없어 실패한다. 다음 오류 코드가 발생할 수 있다:
                            * `POST_ALREADY_DELETED`: 이미 삭제된 게시물인 경우
                            * `POST_NOT_FOUND`: 게시물을 찾을 수 없는 경우
                            """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "POST_ALREADY_DELETED", value = "{\"code\":\"POST_ALREADY_DELETED\"}"),
                                    @ExampleObject(name = "POST_NOT_FOUND", value = "{\"code\":\"POST_NOT_FOUND\"}")
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
    ResponseEntity<PostBookmarkResponse> deleteBookmark(
            @Parameter(
                    name = "postId",
                    description = "게시물 고유 식별자",
                    in = ParameterIn.PATH,
                    required = true
            )
            Long postId,

            Authentication authentication
    );

    @Operation(
            operationId = "updatePostVote",
            summary = "게시물 투표 등록 및 수정",
            description = "특정 게시물에 호구 찬성 또는 반대 투표를 등록하거나 수정한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PostVoteResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "totalVotes": 100,
                                          "yesVotes": 30,
                                          "noVotes": 70,
                                          "myVote": "HOGU"
                                        }
                                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = """
                            잘못된 요청으로 인해 실패한다. 다음 오류 코드가 발생할 수 있다:
                            * `WRONG_POSTID_TYPE`: postId 타입이 잘못된 경우
                            * `EMPTY_REQUEST_BODY`: 요청 본문이 비어있는 경우
                            * `INVALID_MYVOTE`: 유효하지 않은 myVote 값
                            """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "WRONG_POSTID_TYPE", value = "{\"code\":\"WRONG_POSTID_TYPE\"}")
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
                    responseCode = "403",
                    description = """
                            권한이 없어 실패한다. 다음 오류 코드가 발생할 수 있다:
                            * `FORBIDDEN_ACCESS`: 게시물 작성자인 경우(본인 게시물에는 투표할 수 없음)
                            """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "FORBIDDEN_ACCESS", value = "{\"code\":\"FORBIDDEN_ACCESS\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = """
                            게시물을 찾을 수 없어 실패한다. 다음 오류 코드가 발생할 수 있다:
                            * `POST_ALREADY_DELETED`: 이미 삭제된 게시물인 경우
                            * `POST_NOT_FOUND`: 게시물을 찾을 수 없는 경우
                            """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "POST_ALREADY_DELETED", value = "{\"code\":\"POST_ALREADY_DELETED\"}"),
                                    @ExampleObject(name = "POST_NOT_FOUND", value = "{\"code\":\"POST_NOT_FOUND\"}")
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
    ResponseEntity<PostVoteResponse> vote(
            @Parameter(
                    name = "postId",
                    description = "게시물 고유 식별자",
                    in = ParameterIn.PATH,
                    required = true
            )
            Long postId,

            Authentication authentication,

            @RequestBody PostVoteRequest request
    );

    @Operation(
            operationId = "cancelPostVote",
            summary = "게시물 투표 취소",
            description = "특정 게시물에 등록한 투표를 취소한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PostVoteResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "totalVotes": 100,
                                          "yesVotes": 30,
                                          "noVotes": 70,
                                          "myVote": "NONE"
                                        }
                                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = """
                            잘못된 요청으로 인해 실패한다. 다음 오류 코드가 발생할 수 있다:
                            * `WRONG_POSTID_TYPE`: postId 타입이 잘못된 경우
                            """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "WRONG_POSTID_TYPE", value = "{\"code\":\"WRONG_POSTID_TYPE\"}")
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
                            게시물을 찾을 수 없어 실패한다. 다음 오류 코드가 발생할 수 있다:
                            * `POST_ALREADY_DELETED`: 이미 삭제된 게시물인 경우
                            * `POST_NOT_FOUND`: 게시물을 찾을 수 없는 경우
                            """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "POST_ALREADY_DELETED", value = "{\"code\":\"POST_ALREADY_DELETED\"}"),
                                    @ExampleObject(name = "POST_NOT_FOUND", value = "{\"code\":\"POST_NOT_FOUND\"}")
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
    ResponseEntity<PostVoteResponse> cancelVote(
            @Parameter(
                    name = "postId",
                    description = "게시물 고유 식별자",
                    in = ParameterIn.PATH,
                    required = true
            )
            Long postId,

            Authentication authentication
    );
}
