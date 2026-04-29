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

    @Test
    void redirectUnsupportedProviderTest() throws Exception {
        when(jwtProvider.validateAccessToken(null))
                .thenReturn(JwtProvider.TokenValidationResult.EMPTY);

        mockMvc.perform(get("/api/auth/login/INVALID_PROVIDER"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("{\"code\":\"UNSUPPORTED_PROVIDER\"}"));
    }
}
