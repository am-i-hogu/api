package com.hogu.am_i_hogu.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hogu.am_i_hogu.common.exception.CommonErrorCode;
import com.hogu.am_i_hogu.common.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper;

    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        CommonErrorCode code = (CommonErrorCode) request    // request의 attribute로 들어온 code를 읽음
                .getAttribute("errorCode");
        if (code == null) {                                 // spring security에서 발생시킨 오류 처리
            code = CommonErrorCode.SECURITY_UNAUTHORIZED;
            log.warn("Authentication failed without errorCode. message={}", authException.getMessage());
        }

        // JSON 응답 작성
        response.setStatus(code.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorResponse errorResponse = ErrorResponse.of(code);
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}
