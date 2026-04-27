package com.hogu.am_i_hogu.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hogu.am_i_hogu.common.exception.CommonErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class JwtAuthenticationEntryPointTest {

    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint =
            new JwtAuthenticationEntryPoint(new ObjectMapper());

    /**
     * 올바른 형태로 응답을 생성하는지 테스트:
     * 'INVALID_ACCESS_TOKEN' attribute를 저장해 request를 보내고,
     * status code, content type, content 내용을 검증
     */
    @Test
    void writeResponseTest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("errorCode", CommonErrorCode.INVALID_ACCESS_TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtAuthenticationEntryPoint.commence(
                request,
                response,
                new BadCredentialsException("INVALID_ACCESS_TOKEN")
        );

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).contains("application/json");
        assertThat(response.getContentAsString()).isEqualTo("{\"code\":\"INVALID_ACCESS_TOKEN\"}");
    }
}
