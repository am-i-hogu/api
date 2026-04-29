package com.hogu.am_i_hogu.domain.oauth;

import com.hogu.am_i_hogu.common.security.JwtAuthenticationEntryPoint;
import com.hogu.am_i_hogu.common.security.JwtProvider;
import com.hogu.am_i_hogu.common.security.SecurityConfig;
import com.hogu.am_i_hogu.domain.oauth.controller.OAuthController;
import com.hogu.am_i_hogu.domain.oauth.domain.OAuthProvider;
import com.hogu.am_i_hogu.domain.oauth.service.OAuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OAuthController.class)
@Import(SecurityConfig.class)
public class OAuthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockitoBean
    private OAuthService oauthService;

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
}
