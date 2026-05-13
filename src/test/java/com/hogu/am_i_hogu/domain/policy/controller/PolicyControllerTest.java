package com.hogu.am_i_hogu.domain.policy.controller;

import com.hogu.am_i_hogu.common.security.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
public class PolicyControllerTest {

    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("am_i_hogu_policy_test_db")
            .withUsername("test_user")
            .withPassword("test_password");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM policy_revisions");
    }

    /**
     * 개인정보 처리 방침 조회 성공 테스트:
     * 현재 적용 중인 개인정보 처리 방침 데이터를 저장하고 조회 요청을 보낸 뒤,
     * - (1) 응답 status가 200 OK인지 확인
     * - (2) 응답 본문에 version 값이 올바르게 포함되는지 확인
     * - (3) 응답 본문에 updatedAt 값이 문자열 형태로 올바르게 포함되는지 확인
     * - (4) 응답 본문에 html content가 그대로 포함되는지 확인
     */
    @Test
    void getPrivacyPolicyReturns200WhenCurrentPrivacyPolicyExists() throws Exception {
        LocalDateTime updatedAt = LocalDateTime.of(2026, 5, 1, 9, 0, 0);
        stubAuthenticatedUser();

        jdbcTemplate.update(
                """
                INSERT INTO policy_revisions
                    (id, policy_type, version, html_content, is_current, updated_at)
                VALUES
                    (?, ?, ?, ?, ?, ?)
                """,
                1L,
                "PRIVACY",
                "v1.0.0",
                "<h1>개인정보 처리 방침</h1><p>개인정보 처리 방침 전문입니다.</p>",
                true,
                updatedAt
        );

        mockMvc.perform(get("/api/policies/privacy")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid-access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("v1.0.0"))
                .andExpect(jsonPath("$.updatedAt").value("2026-05-01T09:00:00"))
                .andExpect(jsonPath("$.content").value("<h1>개인정보 처리 방침</h1><p>개인정보 처리 방침 전문입니다.</p>"));
    }

    /**
     * 개인정보 처리 방침 조회 인증 실패 테스트:
     * 현재 적용 중인 개인정보 처리 방침 데이터가 있더라도 access token 없이 조회 요청을 보내고,
     * 응답 status가 401 Unauthorized인지 확인
     */
    @Test
    void getPrivacyPolicyReturns401WhenAccessTokenIsMissing() throws Exception {
        LocalDateTime updatedAt = LocalDateTime.of(2026, 5, 1, 9, 0, 0);
        jdbcTemplate.update(
                """
                INSERT INTO policy_revisions
                    (id, policy_type, version, html_content, is_current, updated_at)
                VALUES
                    (?, ?, ?, ?, ?, ?)
                """,
                1L,
                "PRIVACY",
                "v1.0.0",
                "<h1>개인정보 처리 방침</h1><p>개인정보 처리 방침 전문입니다.</p>",
                true,
                updatedAt
        );

        mockMvc.perform(get("/api/policies/privacy"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * 개인정보 처리 방침 조회 실패 테스트:
     * 개인정보 처리 방침 데이터는 존재하지만 현재 적용 중인 버전이 없는 상태에서 조회 요청을 보내고,
     * 응답 status가 500 Internal Server Error인지 확인
     */
    @Test
    void getPrivacyPolicyReturns500WhenCurrentPrivacyPolicyDoesNotExist() throws Exception {
        LocalDateTime updatedAt = LocalDateTime.of(2026, 5, 1, 9, 0, 0);
        stubAuthenticatedUser();

        jdbcTemplate.update(
                """
                INSERT INTO policy_revisions
                    (id, policy_type, version, html_content, is_current, updated_at)
                VALUES
                    (?, ?, ?, ?, ?, ?)
                """,
                1L,
                "PRIVACY",
                "v1.0.0",
                "<h1>개인정보 처리 방침</h1><p>개인정보 처리 방침 전문입니다.</p>",
                false,
                updatedAt
        );

        mockMvc.perform(get("/api/policies/privacy")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-access-token"))
                .andExpect(status().isInternalServerError());
    }

    /**
     * 테스트에서 사용할 가짜 로그인 사용자를 설정
     * Authorization 헤더에 "Bearer valid-access-token"이 들어오면 userId = 1L 사용자로 인증된 상태가 됨
     */
    private void stubAuthenticatedUser() {
        when(jwtProvider.validateAccessToken("valid-access-token"))
                .thenReturn(JwtProvider.TokenValidationResult.VALID);
        when(jwtProvider.getAuthentication("valid-access-token"))
                .thenReturn(new UsernamePasswordAuthenticationToken(
                        String.valueOf(1L),
                        null,
                        Collections.emptyList()
                ));
    }
}
