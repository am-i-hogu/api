package com.hogu.am_i_hogu.domain.auth.controller;

import com.hogu.am_i_hogu.common.exception.ErrorResponse;
import com.hogu.am_i_hogu.domain.auth.dto.request.OnboardingRequest;
import com.hogu.am_i_hogu.domain.auth.dto.response.OnboardingResponse;
import com.hogu.am_i_hogu.domain.auth.dto.response.ReissueResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

@Tag(name = "Auth", description = "서비스 자체 인증(JWT) API")
public interface AuthApiDoc {

    @Operation(
            operationId = "createUser",
            summary = "온보딩",
            description = "registerToken 쿠키를 사용해 회원 가입을 완료하고 access token을 반환한다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "성공",
                    headers = @Header(
                            name = HttpHeaders.SET_COOKIE,
                            description = "refreshToken 쿠키와 registerToken 삭제 쿠키가 함께 내려간다."
                    ),
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OnboardingResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "accessToken": "..."
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
                            * field: `nickname`
                            * 하위 오류 코드:
                                * 필드가 `nickname`:
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
                                                      "code": "INVALID_INPUT_VALUE",
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
                            register token 관련 오류로 실패한다. 다음 오류 코드가 발생할 수 있다:
                            * `REGISTER_TOKEN_EXPIRED`: register token이 만료된 경우
                            * `EMPTY_REGISTER_TOKEN`: register token이 없는 경우
                            * `INVALID_REGISTER_TOKEN`: register token이 유효하지 않은 경우
                            """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "EMPTY_REGISTER_TOKEN", value = "{\"code\":\"EMPTY_REGISTER_TOKEN\"}"),
                                    @ExampleObject(name = "REGISTER_TOKEN_EXPIRED", value = "{\"code\":\"REGISTER_TOKEN_EXPIRED\"}"),
                                    @ExampleObject(name = "INVALID_REGISTER_TOKEN", value = "{\"code\":\"INVALID_REGISTER_TOKEN\"}")
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = """
                        충돌로 인해 실패한다. 다음 오류 코드가 발생할 수 있다:
                        * `DUPLICATE_NICKNAME`: 이미 존재하는 닉네임인 경우
                        """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\":\"DUPLICATE_NICKNAME\"}")
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
    ResponseEntity<OnboardingResponse> createUser(
            @Parameter(
                    name = "registerToken",
                    in = ParameterIn.COOKIE,
                    required = true,
                    description = "회원 가입을 위한 임시 토큰"
            )
            String registerToken,
            OnboardingRequest requestBody
    );


    @Operation(
            operationId = "refreshAccessToken",
            summary = "access token 재발급",
            description = "유효한 refreshToken 쿠키를 검증하여 새로운 access token과 갱신된 refresh token(RTR)을 발급한다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "성공 (새로운 토큰 발급 완료)",
                    headers = @Header(
                            name = HttpHeaders.SET_COOKIE,
                            description = "새롭게 갱신된 refreshToken 쿠키 (HttpOnly, Secure, RTR 적용)"
                    ),
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ReissueResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "accessToken": "..."
                                        }
                                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = """
                            refresh token 관련 오류로 실패한다. 다음 오류 코드가 발생할 수 있다:
                            * `EMPTY_REFRESH_TOKEN`: refresh token이 없는 경우
                            * `REFRESH_TOKEN_EXPIRED`: refresh token이 만료된 경우
                            * `INVALID_REFRESH_TOKEN`: refresh token이 유효하지 않은 경우
                            * `REFRESH_TOKEN_REUSED`: refresh token이 재사용된 경우
                            """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "EMPTY_REFRESH_TOKEN", value = "{\"code\":\"EMPTY_REFRESH_TOKEN\"}"),
                                    @ExampleObject(name = "REFRESH_TOKEN_EXPIRED", value = "{\"code\":\"REFRESH_TOKEN_EXPIRED\"}"),
                                    @ExampleObject(name = "INVALID_REFRESH_TOKEN", value = "{\"code\":\"INVALID_REFRESH_TOKEN\"}"),
                                    @ExampleObject(name = "REFRESH_TOKEN_REUSED", value = "{\"code\":\"REFRESH_TOKEN_REUSED\"}")
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
    ResponseEntity<ReissueResponse> reissueToken(
            @Parameter(
                    name = "refreshToken",
                    in = ParameterIn.COOKIE,
                    required = true,
                    description = "access token 재발급을 위한 refresh token"
            )
            String refreshToken
    );


    @Operation(
            operationId = "logout",
            summary = "로그아웃",
            description = "서버 내 Refresh Token을 무효화하고 클라이언트의 쿠키를 삭제하여 로그아웃 처리한다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "성공 (로그아웃 완료 및 쿠키 삭제)",
                    headers = @Header(
                            name = HttpHeaders.SET_COOKIE,
                            description = "refreshToken 삭제를 위한 쿠키 (Max-Age=0 옵션 적용)"
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
    ResponseEntity<Void> logout(
            @Parameter(
                    name = "refreshToken",
                    in = ParameterIn.COOKIE,
                    required = true,
                    description = "로그아웃할 유저의 refresh token"
            )
            String refreshToken
    );
}
