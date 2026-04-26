package com.hogu.am_i_hogu.common.security;

import com.hogu.am_i_hogu.common.exception.CommonErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JwtAuthenticationFilterTest {
    private final JwtProvider jwtProvider = mock(JwtProvider.class);
    private final AuthenticationEntryPoint authenticationEntryPoint = mock(AuthenticationEntryPoint.class);
    private final JwtAuthenticationFilter jwtAuthenticationFilter =
            new JwtAuthenticationFilter(jwtProvider, authenticationEntryPoint);

    /**
     * access token이 비어있는 경우 올바른 오류 코드를 attribute에 저장하는지 테스트:
     * - Authorization header 없이 요청을 보내고,
     * - request의 errorCode가 EMPTY_ACCESS_TOKEN인지 확인
     */
    @Test
    void filterEmptyAccessTokenTest() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/api/posts/1");

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);
        when(jwtProvider.validateAccessToken(null))
                .thenReturn(JwtProvider.TokenValidationResult.EMPTY);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(request.getAttribute("errorCode")).isEqualTo(CommonErrorCode.EMPTY_ACCESS_TOKEN);
    }

    /**
     * access token이 만료된 경우 올바른 오류 코드를 attribute에 저장하는지 테스트:
     * - 만료된 access token 값을 Authorization header에 담아 요청을 보내고,
     * - request의 errorCode가 ACCESS_TOKEN_EXPIRED인지 확인
     */
    @Test
    void filterExpiredAccessTokenTest() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/api/posts/1");
        request.addHeader("Authorization", "Bearer expired-access-token");

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);
        when(jwtProvider.validateAccessToken("expired-access-token"))
                .thenReturn(JwtProvider.TokenValidationResult.EXPIRED);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(request.getAttribute("errorCode")).isEqualTo(CommonErrorCode.ACCESS_TOKEN_EXPIRED);
    }

    /**
     * access token이 잘못된 값인 경우 올바른 오류 코드를 attribute에 저장하는지 테스트:
     * - 잘못된 access token 값을 Authorization header에 담아 요청을 보내고,
     * - request의 errorCode가 INVALID_ACCESS_TOKEN인지 확인
     */
    @Test
    void filterInvalidAccessTokenTest() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/api/posts/1");
        request.addHeader("Authorization", "invalid-access-token");

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);
        when(jwtProvider.validateAccessToken("invalid-access-token"))
                .thenReturn(JwtProvider.TokenValidationResult.INVALID);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(request.getAttribute("errorCode")).isEqualTo(CommonErrorCode.INVALID_ACCESS_TOKEN);
    }

    /**
     * 유효한 access token일 경우 authentication 객체를 context에 저장하는지 테스트:
     * - 유효한 access token 값을 Authorization header에 담아 요청을 보내고,
     * - SecurityContext의 authentication이 mock 객체와 같은지 확인
     */
    @Test
    void filterValidAccessTokenTest() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/api/posts/1");
        request.addHeader("Authorization", "Bearer valid-access-token");

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);
        Authentication authentication = mock(Authentication.class);
        when(jwtProvider.validateAccessToken("valid-access-token"))
                .thenReturn(JwtProvider.TokenValidationResult.VALID);
        when(jwtProvider.getAuthentication("valid-access-token"))
                .thenReturn(authentication);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(authentication);
    }
}
