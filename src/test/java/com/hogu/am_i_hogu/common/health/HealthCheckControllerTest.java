package com.hogu.am_i_hogu.common.health;

import com.hogu.am_i_hogu.common.security.JwtAccessDeniedHandler;
import com.hogu.am_i_hogu.common.security.JwtAuthenticationEntryPoint;
import com.hogu.am_i_hogu.common.security.JwtProvider;
import com.hogu.am_i_hogu.common.security.SecurityConfig;
import com.hogu.am_i_hogu.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthCheckController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "server.env=blue",
        "server.port=8080",
        "server.serverAddress=43.201.237.252",
        "serverName=blue_server"
})
class HealthCheckControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockitoBean
    private JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void healthCheckReturnsUpWithoutAccessToken() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.env").value("blue"))
                .andExpect(jsonPath("$.serverPort").value(8080))
                .andExpect(jsonPath("$.serverAddress").value("43.201.237.252"))
                .andExpect(jsonPath("$.serverName").value("blue_server"));

        verifyNoInteractions(jwtProvider);
    }
}
