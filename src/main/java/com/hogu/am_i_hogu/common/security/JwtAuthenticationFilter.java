package com.hogu.am_i_hogu.common.security;

import com.hogu.am_i_hogu.common.exception.CommonErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.stream.Stream;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    public JwtAuthenticationFilter(
            JwtProvider jwtProvider,
            AuthenticationEntryPoint authenticationEntryPoint
    ) {
        this.jwtProvider = jwtProvider;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestURI = request.getRequestURI();

        return Stream.of("/api/users", "/api/auth/refresh")
                .anyMatch(request.getRequestURI()::equals);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String accessToken = getAccessToken(request);
        boolean isLogoutRequest = "/api/auth/logout".equals(request.getRequestURI());

        /**
         * access token 검증
         * - access token이 비어있다면 'errorCode'라는 이름의 속성에 코드 설정 후 다음 필터로 진행
         * - 문제가 있다면 request의 'errorCode'라는 이름의 속성으로 오류 코드 전달
         * - 문제가 없다면 다음 filter로 이동
         */
        JwtProvider.TokenValidationResult validationResult = jwtProvider.validateAccessToken(accessToken);
        if (validationResult == JwtProvider.TokenValidationResult.EMPTY) {                      // access token이 비어있는 경우
            request.setAttribute("errorCode", CommonErrorCode.EMPTY_ACCESS_TOKEN);
            filterChain.doFilter(request, response);                                            // commence 호출 없이 다음 filter로 이동
            return;
        } else if (validationResult == JwtProvider.TokenValidationResult.EXPIRED) {             // access token이 만료된 경우
            if (isLogoutRequest) {
                filterChain.doFilter(request, response);
                return;
            }
            handleAccessTokenError(request, response, CommonErrorCode.ACCESS_TOKEN_EXPIRED);
            return;
        } else if (validationResult == JwtProvider.TokenValidationResult.INVALID) {             // access token이 잘못된 값인 경우
            if (isLogoutRequest) {
                filterChain.doFilter(request, response);
                return;
            }
            handleAccessTokenError(request, response, CommonErrorCode.INVALID_ACCESS_TOKEN);
            return;
        }

        /**
         * access token이 유효한 경우:
         * - context에 authentication 객체로 사용자 정보 저장
         * - 다음 filter로 이동
         */
        Authentication authentication = jwtProvider.getAuthentication(accessToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    private String getAccessToken(HttpServletRequest request) {
        String requestHeader = request.getHeader("Authorization");

        if (requestHeader == null || requestHeader.isBlank()) {         // 헤더가 비어있는 경우: null 반환
            return null;
        } else if (!requestHeader.startsWith("Bearer ")) {              // 헤더가 "Bearer"로 시작하지 않는 경우: 원문 반환 (INVALID로 처리됨)
            return requestHeader;
        }

        String accessToken = requestHeader.substring(7);      // 헤더가 잘 들어온 경우: 토큰만 추출
        return accessToken.isBlank() ? null : accessToken;
    }

    private void handleAccessTokenError(
            HttpServletRequest request,
            HttpServletResponse response,
            CommonErrorCode errorCode
    ) throws IOException, ServletException {
        request.setAttribute("errorCode", errorCode);
        authenticationEntryPoint.commence(
                request,
                response,
                new BadCredentialsException(errorCode.getCode()));
    }
}
