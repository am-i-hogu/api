package com.hogu.am_i_hogu.domain.post.controller;

import com.hogu.am_i_hogu.common.exception.ErrorResponse;
import com.hogu.am_i_hogu.domain.post.dto.response.ImageUploadResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Image", description = "이미지 업로드 API")
public interface ImageApiDoc {

    @Operation(
            operationId = "uploadPostImage",
            summary = "게시물/프로필 이미지 업로드",
            description = "게시물 및 프로필에 사용될 이미지를 업로드하고 저장된 URL을 반환한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ImageUploadResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "imageUrl": "https://..."
                                        }
                                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = """
                            잘못된 요청으로 인해 실패한다. 다음 오류 코드가 발생할 수 있다:
                            * `UNSUPPORTED_FORMAT`: 지원하지 않는 확장자
                            * `EMPTY_IMAGE_FILE`: 이미지 파일 없음
                            """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "UNSUPPORTED_FORMAT", value = "{\"code\":\"UNSUPPORTED_FORMAT\"}"),
                                    @ExampleObject(name = "EMPTY_IMAGE_FILE", value = "{\"code\":\"EMPTY_IMAGE_FILE\"}")
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
                            * `USER_NOT_FOUND`: 존재하지 않거나 삭제된 사용자인 경우
                            """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "USER_NOT_FOUND", value = "{\"code\":\"USER_NOT_FOUND\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "413",
                    description = """
                            파일 크기 초과로 실패한다. 다음 오류 코드가 발생할 수 있다:
                            * `FILE_SIZE_EXCEEDED`: 파일 크기가 5MB 초과한 경우
                            """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "FILE_SIZE_EXCEEDED", value = "{\"code\":\"FILE_SIZE_EXCEEDED\"}")
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
    ResponseEntity<ImageUploadResponse> uploadImage(
            @Parameter(hidden = true)
            Authentication authentication,
            @Parameter(
                    description = "업로드 할 이미지 파일",
                    content = @Content(
                            mediaType = "multipart/form-data",
                            schema = @Schema(type = "string", format = "binary")
                    )
            )
            @RequestBody(required = true)
            MultipartFile image
    );
}
