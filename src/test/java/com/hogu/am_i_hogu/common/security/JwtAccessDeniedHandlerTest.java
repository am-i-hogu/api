package com.hogu.am_i_hogu.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class JwtAccessDeniedHandlerTest {
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler =
            new JwtAccessDeniedHandler(new ObjectMapper());

    @Test
    void writeResponseTest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtAccessDeniedHandler.handle(
                request,
                response,
                new AccessDeniedException("FORBIDDEN_ACCESS")
        );

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).contains("application/json");
        assertThat(response.getContentAsString()).isEqualTo("{\"code\":\"FORBIDDEN_ACCESS\"}");
    }
}
