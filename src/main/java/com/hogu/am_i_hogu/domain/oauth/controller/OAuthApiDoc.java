package com.hogu.am_i_hogu.domain.oauth.controller;

import com.hogu.am_i_hogu.common.exception.ErrorResponse;
import com.hogu.am_i_hogu.domain.oauth.domain.OAuthProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

@Tag(name = "OAuth", description = "OAuth API")
public interface OAuthApiDoc {

    @Operation(
            operationId = "login",
            summary = "OAuth 로그인 시작",
            description = "OAuth 제공자 로그인 페이지로 리다이렉트한다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "302",
                    description = "성공. OAuth 제공자 로그인 페이지로 리다이렉트한다.",
                    headers = @Header(
                            name = "Location",
                            description = "Authorization server의 로그인 창 URL"
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = """
                        잘못된 요청으로 인해 실패한다. 다음 오류 코드가 발생할 수 있다:
                        * `UNSUPPORTED_PROVIDER`: 지원하지 않는 provider가 입력된 경우
                        """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "UNSUPPORTED_PROVIDER", value = "{\"code\":\"UNSUPPORTED_PROVIDER\"}")
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
    ResponseEntity<Void> redirectToProviderLogin(
            @Parameter(
                    name = "provider",
                    description = "소셜 로그인 제공자 (GOOGLE, KAKAO)",
                    in = ParameterIn.PATH,
                    required = true
            )
            String provider
    );

    @Operation(
            operationId = "handleOAuthCallback",
            summary = "OAuth 로그인 콜백 처리",
            description = "OAuth 인가 코드를 받아 로그인을 처리하고 결과에 따라 프론트엔드로 리다이렉트한다. 실패 시 오류 코드와 함께 리다이렉트 처리된다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "302",
                    description = "로그인 처리를 완료하고 프론트엔드로 리다이렉트한다.",
                    headers = {
                            @Header(
                                    name = "Location",
                                    description = "기존 회원 로그인 성공 시, 신규 회원 온보딩 페이지, 또는 실패 시 오류 코드가 포함된 URL"
                            ),
                            @Header(
                                    name = "Set-Cookie",
                                    description = "기존 회원은 refreshToken, 신규 회원은 registerToken이 포함되며 실패 시에는 포함되지 않는다."
                            )
                    }
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = """
                        잘못된 요청으로 인해 실패한다. 다음 오류 코드가 발생할 수 있다:
                        * `UNSUPPORTED_PROVIDER`: 지원하지 않는 provider가 입력된 경우
                        * `PROVIDER_MISMATCH`: DB에 저장된 state-provider의 조합과 일치하지 않는 경우
                        """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "UNSUPPORTED_PROVIDER", value = "{\"code\":\"UNSUPPORTED_PROVIDER\"}"),
                                    @ExampleObject(name = "PROVIDER_MISMATCH", value = "{\"code\":\"PROVIDER_MISMATCH\"}")
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = """
                        인가 검증 실패로 인해 실패한다. 다음 오류 코드가 발생할 수 있다:
                        * `INVALID_STATE`: state 값이 서버에 없거나 일치하지 않는 경우
                        * `STATE_REUSED`: state 값이 이미 사용된 값인 경우
                        * `STATE_EXPIRED`: state 값이 만료된 경우
                        * `INVALID_AUTH_CODE`: code가 유효하지 않거나 만료된 경우
                        * `INVALID_ID_TOKEN`: 발급받은 id_token이 유효하지 않은 경우
                        """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "INVALID_STATE", value = "{\"code\":\"INVALID_STATE\"}"),
                                    @ExampleObject(name = "STATE_REUSED", value = "{\"code\":\"STATE_REUSED\"}"),
                                    @ExampleObject(name = "STATE_EXPIRED", value = "{\"code\":\"STATE_EXPIRED\"}"),
                                    @ExampleObject(name = "INVALID_AUTH_CODE", value = "{\"code\":\"INVALID_AUTH_CODE\"}"),
                                    @ExampleObject(name = "INVALID_ID_TOKEN", value = "{\"code\":\"INVALID_ID_TOKEN\"}")
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
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = """
                        소셜 서버 연동 오류로 인해 실패한다. 다음 오류 코드가 발생할 수 있다:
                        * `SOCIAL_SERVER_ERROR`: 소셜 서버로부터 정상적인 응답을 받지 못한 경우
                        """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "SOCIAL_SERVER_ERROR", value = "{\"code\":\"SOCIAL_SERVER_ERROR\"}")
                    )
            )
    })
    ResponseEntity<Void> handleCallback(
            @Parameter(
                    name = "provider",
                    description = "소셜 로그인 제공자 (GOOGLE, KAKAO)",
                    in = ParameterIn.PATH,
                    required = true
            )
            String provider,

            @Parameter(
                    name = "code",
                    description = "Authorization server로부터 발급 받은 일회용 인가 코드",
                    in = ParameterIn.QUERY,
                    required = true
            )
            String code,

            @Parameter(
                    name = "state",
                    description = "CSRF 공격 방어를 위한 무작위 난수",
                    in = ParameterIn.QUERY,
                    required = true
            )
            String state
    );

    @Operation(
            operationId = "deleteUser",
            summary = "회원 탈퇴",
            description = "회원 탈퇴를 처리하고 리프레시 토큰 쿠키를 삭제한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "성공",
                    headers = @Header(
                            name = "Set-Cookie",
                            description = "refreshToken 삭제를 위해 Max-Age=0 옵션을 적용한 쿠키"
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
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = """
                        소셜 서버 연동 오류로 인해 실패한다. 다음 오류 코드가 발생할 수 있다:
                        * `SOCIAL_SERVER_ERROR`: 소셜 서버로부터 정상적인 응답을 받지 못한 경우
                        """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "SOCIAL_SERVER_ERROR", value = "{\"code\":\"SOCIAL_SERVER_ERROR\"}")
                    )
            )
    })
    ResponseEntity<Void> deleteUser(Authentication authentication);
}
