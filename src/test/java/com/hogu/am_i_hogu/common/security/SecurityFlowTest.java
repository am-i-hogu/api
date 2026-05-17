package com.hogu.am_i_hogu.common.security;

import com.hogu.am_i_hogu.domain.user.domain.User;
import com.hogu.am_i_hogu.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * SecurityConfig, JwtAuthenticationFilter,
 * JwtAuthenticationEntryPoint, JwtAccessDeniedHandler
 * 통합 테스트
 */
@WebMvcTest(controllers = SecurityFlowTest.TestController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtAuthenticationEntryPoint.class,
        JwtAccessDeniedHandler.class,
        SecurityFlowTest.TestController.class
})
public class SecurityFlowTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private UserRepository userRepository;

    @RestController
    static class TestController {
        // public endpoint 테스트를 위한 test controller
        @GetMapping("/api/posts")
        public String getPosts() {
            return "public";
        }

        // private endpoint 테스트를 위한 test controller
        @PostMapping("/api/posts")
        public String postPosts() {
            return "private";
        }
    }

    // public endpoint: access token 없이 접근 가능한지 테스트
    @Test
    void publicEndpoint200Test() throws Exception {
        when(jwtProvider.validateAccessToken(null))
                .thenReturn(JwtProvider.TokenValidationResult.EMPTY);

        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(content().string("public"));
    }

    // private endpoint: access token 없이 접근하면 401 발생하는지 테스트
    @Test
    void privateEndpoint401Test() throws Exception {
        when(jwtProvider.validateAccessToken(null))
                .thenReturn(JwtProvider.TokenValidationResult.EMPTY);

        mockMvc.perform(post("/api/posts"))
                .andExpect(status().isUnauthorized());
    }

    // private endpoint: 유효한 access token이 있으면 접근 가능한지 테스트
    @Test
    void privateEndpoint200Test() throws Exception {
        when(jwtProvider.validateAccessToken("valid-token"))
                .thenReturn(JwtProvider.TokenValidationResult.VALID);
        when(jwtProvider.getSubjectAsLong("valid-token"))
                .thenReturn(1L);
        when(userRepository.findByIdAndIsDeletedFalse(1L))
                .thenReturn(Optional.of(mock(User.class)));
        when(jwtProvider.getAuthentication("valid-token"))
                .thenReturn(new UsernamePasswordAuthenticationToken(
                        "1",
                        null,
                        Collections.emptyList()
                ));

        mockMvc.perform(post("/api/posts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(content().string("private"));
    }
}
