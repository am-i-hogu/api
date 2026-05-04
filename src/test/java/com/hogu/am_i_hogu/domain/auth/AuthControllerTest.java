package com.hogu.am_i_hogu.domain.auth;

import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.common.exception.ErrorResponse;
import com.hogu.am_i_hogu.common.security.JwtAccessDeniedHandler;
import com.hogu.am_i_hogu.common.security.JwtAuthenticationEntryPoint;
import com.hogu.am_i_hogu.common.security.JwtProvider;
import com.hogu.am_i_hogu.common.security.SecurityConfig;
import com.hogu.am_i_hogu.domain.auth.exception.AuthErrorCode;
import com.hogu.am_i_hogu.domain.auth.controller.AuthController;
import com.hogu.am_i_hogu.domain.auth.dto.response.OnboardingResult;
import com.hogu.am_i_hogu.domain.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@ActiveProfiles("test")
@Import(SecurityConfig.class)
public class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockitoBean
    private JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @MockitoBean
    private AuthService authService;

    /**
     * 온보딩 요청 성공 테스트:
     * - register token 쿠키와 nickname을 담아 온보딩 요청을 보내고,
     * - (1) 응답 status가 200 OK인지 확인
     * - (2) Set-Cookie 헤더가 refreshToken 정보를 포함하는지 확인
     * - (3) 응답 본문에 accessToken이 포함되는지 확인
     * - (4) OnboardingService가 register token과 nickname 값으로 호출되었는지 확인
     */
    @Test
    void createUserTest() throws Exception {
        when(authService.createUser("register-token", "nickname"))
                .thenReturn(new OnboardingResult("new-access-token", "new-refresh-token"));

        mockMvc.perform(post("/api/users")
                        .cookie(new Cookie("registerToken", "register-token"))
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

        verify(authService).createUser("register-token", "nickname");
    }

    /**
     * 온보딩 요청 인증 실패 테스트:
     * - register token 쿠키 없이 온보딩 요청을 보내고,
     * - (1) 응답 status가 401 Unauthorized인지 확인
     * - (2) 응답 본문이 EMPTY_REGISTER_TOKEN 오류 코드를 반환하는지 확인
     */
    @Test
    void createUserWithoutRegisterTokenCookieTest() throws Exception {
        when(authService.createUser(null, "nickname"))
                .thenThrow(new CustomException(AuthErrorCode.EMPTY_REGISTER_TOKEN));

        mockMvc.perform(post("/api/users")
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
     * 온보딩 요청 인증 실패 테스트:
     * - 잘못된 register token으로 온보딩 요청을 보내고,
     * - (1) 응답 status가 401 Unauthorized인지 확인
     * - (2) 응답 본문이 INVALID_REGISTER_TOKEN 오류 코드를 반환하는지 확인
     */
    @Test
    void createUserUnauthorizedTest() throws Exception {
        when(authService.createUser("invalid-register-token", "nickname"))
                .thenThrow(new CustomException(AuthErrorCode.INVALID_REGISTER_TOKEN));

        mockMvc.perform(post("/api/users")
                        .cookie(new Cookie("registerToken", "invalid-register-token"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "nickname"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("{\"code\":\"INVALID_REGISTER_TOKEN\"}"));
    }

    /**
     * 온보딩 요청 입력값 검증 실패 테스트:
     * - register token 쿠키와 잘못된 nickname으로 온보딩 요청을 보내고,
     * - (1) 응답 status가 400 Bad Request인지 확인
     * - (2) 응답 본문이 INVALID_INPUT_VALUE 오류 코드를 반환하는지 확인
     * - (3) 응답 본문이 nickname 필드의 상세 오류 코드를 포함하는지 확인
     */
    @Test
    void createUserBadRequestTest() throws Exception {
        when(authService.createUser("register-token", "nickname!"))
                .thenThrow(new CustomException(
                        AuthErrorCode.INVALID_INPUT_VALUE,
                        List.of(new ErrorResponse.ErrorDetail("nickname", AuthErrorCode.SPECIAL_CHAR_NICKNAME.getCode()))
                ));

        mockMvc.perform(post("/api/users")
                        .cookie(new Cookie("registerToken", "register-token"))
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
