package com.hogu.am_i_hogu.domain.oauth;

import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.common.exception.ErrorResponse;
import com.hogu.am_i_hogu.common.security.JwtAccessDeniedHandler;
import com.hogu.am_i_hogu.common.security.JwtAuthenticationEntryPoint;
import com.hogu.am_i_hogu.common.security.JwtProvider;
import com.hogu.am_i_hogu.common.security.SecurityConfig;
import com.hogu.am_i_hogu.domain.oauth.controller.OAuthController;
import com.hogu.am_i_hogu.domain.oauth.domain.OAuthProvider;
import com.hogu.am_i_hogu.domain.oauth.dto.response.CreateUserResponse;
import com.hogu.am_i_hogu.domain.oauth.dto.response.OAuthCallbackResult;
import com.hogu.am_i_hogu.domain.oauth.dto.response.OnboardingResult;
import com.hogu.am_i_hogu.domain.oauth.exception.OAuthErrorCode;
import com.hogu.am_i_hogu.domain.oauth.service.OAuthService;
import com.hogu.am_i_hogu.domain.oauth.service.OnboardingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OAuthController.class)
@ActiveProfiles("test")
@Import(SecurityConfig.class)
public class OAuthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockitoBean
    private JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @MockitoBean
    private OAuthService oauthService;

    @MockitoBean
    private OnboardingService onboardingService;

    /**
     * 지원하는 provider로 로그인 요청 시 OAuth provider 로그인 페이지로 redirect 되는지 테스트:
     * - access token 없이 GOOGLE 로그인 요청을 보내고,
     * - (1) 응답 status가 302 Found인지 확인
     * - (2) Location 헤더가 OAuthService가 반환한 URL과 같은지 확인
     * - (3) OAuthService가 GOOGLE provider로 호출되었는지 확인
     */
    @Test
    void redirectProviderLoginTest() throws Exception {
        when(jwtProvider.validateAccessToken(null))
                .thenReturn(JwtProvider.TokenValidationResult.EMPTY);
        when(oauthService.getAuthorizationUrl(OAuthProvider.GOOGLE))
                .thenReturn("https://accounts.google.com/o/oauth2/v2/auth?client_id=test-client-id");

        mockMvc.perform(get("/api/auth/login/GOOGLE"))
                .andExpect(status().isFound())
                .andExpect(header().string(
                        "Location",
                        "https://accounts.google.com/o/oauth2/v2/auth?client_id=test-client-id"
                ));

        verify(oauthService).getAuthorizationUrl(OAuthProvider.GOOGLE);
    }

    /**
     * 지원하지 않는 provider로 로그인 요청 시 400 Bad Request와 오류 응답을 반환하는지 테스트:
     * - access token 없이 지원하지 않는 provider로 로그인 요청을 보내고,
     * - (1) 응답 status가 400 Bad Request인지 확인
     * - (2) 응답 본문이 UNSUPPORTED_PROVIDER 오류 코드를 반환하는지 확인
     */
    @Test
    void redirectUnsupportedProviderTest() throws Exception {
        when(jwtProvider.validateAccessToken(null))
                .thenReturn(JwtProvider.TokenValidationResult.EMPTY);

        mockMvc.perform(get("/api/auth/login/INVALID_PROVIDER"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("{\"code\":\"UNSUPPORTED_PROVIDER\"}"));
    }

    /**
     * 지원하는 provider callback 요청 테스트:
     * - access token 없이 callback 요청을 보내고,
     * - (1) 응답 status가 302 Found인지 확인
     * - (2) Location 헤더가 OAuthService가 반환한 redirectUri와 같은지 확인
     * - (3) Set-Cookie 헤더가 OAuthService가 반환한 cookie 정보를 포함하는지 확인
     * - (4) OAuthService가 GOOGLE provider, code, state 값으로 호출되었는지 확인
     */
    @Test
    void callbackTest() throws Exception {
        when(jwtProvider.validateAccessToken(null))
                .thenReturn(JwtProvider.TokenValidationResult.EMPTY);
        when(oauthService.handleCallback(OAuthProvider.GOOGLE, "test-code", "test-state"))
                .thenReturn(new OAuthCallbackResult(
                        "http://localhost:8080/oauth/callback?status=LOGIN_SUCCESS",
                        "refreshToken",
                        "test-refresh-token"
                ));

        mockMvc.perform(get("/api/auth/callback/GOOGLE")
                        .param("code", "test-code")
                        .param("state", "test-state"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "http://localhost:8080/oauth/callback?status=LOGIN_SUCCESS"))
                .andExpect(header().string("Set-Cookie", "refreshToken=test-refresh-token; Path=/; Secure; HttpOnly"));

        verify(oauthService).handleCallback(OAuthProvider.GOOGLE, "test-code", "test-state");
    }

    /**
     * 지원하지 않는 provider callback 요청 테스트:
     * - access token 없이 지원하지 않는 provider로 callback 요청을 보내고,
     * - (1) 응답 status가 400 Bad Request인지 확인
     * - (2) 응답 본문이 UNSUPPORTED_PROVIDER 오류 코드를 반환하는지 확인
     */
    @Test
    void callbackUnsupportedProviderTest() throws Exception {
        when(jwtProvider.validateAccessToken(null))
                .thenReturn(JwtProvider.TokenValidationResult.EMPTY);

        mockMvc.perform(get("/api/auth/callback/INVALID_PROVIDER")
                        .param("code", "test-code")
                        .param("state", "test-state"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("{\"code\":\"UNSUPPORTED_PROVIDER\"}"));
    }

    /**
     * 온보딩 요청 성공 테스트:
     * - access token 없이 register token과 nickname을 담아 온보딩 요청을 보내고,
     * - (1) 응답 status가 200 OK인지 확인
     * - (2) Set-Cookie 헤더가 refreshToken 정보를 포함하는지 확인
     * - (3) 응답 본문에 accessToken이 포함되는지 확인
     * - (4) OnboardingService가 Authorization 헤더와 nickname 값으로 호출되었는지 확인
     */
    @Test
    void createUserTest() throws Exception {
        when(jwtProvider.getTokenType("register-token"))
                .thenReturn("register");
        when(jwtProvider.isRegisterTokenType("register"))
                .thenReturn(true);
        when(onboardingService.createUser("Bearer register-token", "nickname"))
                .thenReturn(new OnboardingResult("new-access-token", "new-refresh-token"));

        mockMvc.perform(post("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer register-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "nickname"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", "refreshToken=new-refresh-token; Path=/; Secure; HttpOnly"))
                .andExpect(content().json("""
                        {
                          "accessToken": "new-access-token"
                        }
                        """));

        verify(onboardingService).createUser("Bearer register-token", "nickname");
    }

    /**
     * 온보딩 요청 인증 실패 테스트:
     * - access token 없이 잘못된 register token으로 온보딩 요청을 보내고,
     * - (1) 응답 status가 401 Unauthorized인지 확인
     * - (2) 응답 본문이 EMPTY_REGISTER_TOKEN 오류 코드를 반환하는지 확인
     */
    @Test
    void createUserUnauthorizedTest() throws Exception {
        when(jwtProvider.getTokenType("invalid-register-token"))
                .thenReturn("register");
        when(jwtProvider.isRegisterTokenType("register"))
                .thenReturn(true);
        when(onboardingService.createUser("Bearer invalid-register-token", "nickname"))
                .thenThrow(new CustomException(OAuthErrorCode.EMPTY_REGISTER_TOKEN));

        mockMvc.perform(post("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-register-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "nickname"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("{\"code\":\"EMPTY_REGISTER_TOKEN\"}"));
    }

    /**
     * 온보딩 요청 입력값 검증 실패 테스트:
     * - access token 없이 잘못된 nickname으로 온보딩 요청을 보내고,
     * - (1) 응답 status가 400 Bad Request인지 확인
     * - (2) 응답 본문이 INVALID_INPUT_VALUE 오류 코드를 반환하는지 확인
     * - (3) 응답 본문이 nickname 필드의 상세 오류 코드를 포함하는지 확인
     */
    @Test
    void createUserBadRequestTest() throws Exception {
        when(jwtProvider.getTokenType("register-token"))
                .thenReturn("register");
        when(jwtProvider.isRegisterTokenType("register"))
                .thenReturn(true);
        when(onboardingService.createUser("Bearer register-token", "nickname!"))
                .thenThrow(new CustomException(
                        OAuthErrorCode.INVALID_INPUT_VALUE,
                        List.of(new ErrorResponse.ErrorDetail("nickname", OAuthErrorCode.SPECIAL_CHAR_NICKNAME.getCode()))
                ));

        mockMvc.perform(post("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer register-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "nickname!"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("""
                        {
                          "code": "INVALID_INPUT_VALUE",
                          "errors": [
                            {
                              "field": "nickname",
                              "code": "SPECIAL_CHAR_NICKNAME"
                            }
                          ]
                        }
                        """));
    }
}
