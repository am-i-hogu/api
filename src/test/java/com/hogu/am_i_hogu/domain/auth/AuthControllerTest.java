package com.hogu.am_i_hogu.domain.auth;

import com.hogu.am_i_hogu.common.security.JwtProvider;
import com.hogu.am_i_hogu.common.security.TokenHasher;
import jakarta.servlet.http.Cookie;
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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
public class AuthControllerTest {

    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("am_i_hogu_auth_test_db")
            .withUsername("test_user")
            .withPassword("test_password");
    @Autowired
    private TokenHasher tokenHasher;

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
        jdbcTemplate.update("DELETE FROM refresh_tokens");
        jdbcTemplate.update("DELETE FROM users");
    }

    /**
     * 로그아웃 성공 테스트:
     * 유효한 access token과 refresh token으로 요청을 보내고,
     * - (1) 응답 status가 204 No Content인지 확인
     * - (2) refreshToken 쿠키를 삭제하는지 확인
     * - (3) DB에 is_revoked=true로 설정되는지 확인
     */
    @Test
    void logoutRevokesRefreshTokenWhenAccessTokenAndRefreshTokenAreValid() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "nickname", null);
        insertRefreshToken(100L, 1L, "valid-refresh-token", false);

        when(jwtProvider.validateRefreshToken("valid-refresh-token"))
                .thenReturn(JwtProvider.TokenValidationResult.VALID);
        when(jwtProvider.getSubjectAsLong("valid-refresh-token"))
                .thenReturn(1L);
        when(jwtProvider.getTokenId("valid-refresh-token"))
                .thenReturn(100L);

        mockMvc.perform(post("/api/auth/logout")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer valid-access-token")
                    .cookie(new Cookie("refreshToken", "valid-refresh-token")))
                .andExpect(status().isNoContent())
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        "refreshToken=; Path=/; Max-Age=0; Expires=Thu, 1 Jan 1970 00:00:00 GMT; Secure; HttpOnly"
                ));

        Boolean isRevoked = jdbcTemplate.queryForObject(
                "SELECT is_revoked FROM refresh_tokens WHERE id = ?",
                Boolean.class,
                100L
        );
        assertThat(isRevoked).isTrue();
    }

    /**
     * 로그아웃 성공 테스트:
     * access token만 포함해 요청을 보내고,
     * - (1) 응답 status가 204 No Content인지 확인
     * - (2) refreshToken 쿠키를 삭제하는지 확인
     */
    @Test
    void logoutReturns204WhenRefreshTokenCookieIsMissing() throws Exception {
        stubAuthenticatedUser();

        mockMvc.perform(post("/api/auth/logout")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer valid-access-token"))
                .andExpect(status().isNoContent())
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        "refreshToken=; Path=/; Max-Age=0; Expires=Thu, 1 Jan 1970 00:00:00 GMT; Secure; HttpOnly"
                ));
    }

    /**
     * 로그아웃 성공 테스트:
     * 유효한 access token과 DB에 저장되어 있지 않지만 유효한 refresh token으로 요청을 보내고,
     * - (1) 응답 status가 204 No Content인지 확인
     * - (2) refreshToken 쿠키를 삭제하는지 확인
     */
    @Test
    void logoutReturns204WhenRefreshTokenDoesNotExistInDatabase() throws Exception {
        stubAuthenticatedUser();

        when(jwtProvider.validateRefreshToken("valid-refresh-token"))
                .thenReturn(JwtProvider.TokenValidationResult.VALID);
        when(jwtProvider.getSubjectAsLong("valid-refresh-token"))
                .thenReturn(1L);
        when(jwtProvider.getTokenId("valid-refresh-token"))
                .thenReturn(100L);

        mockMvc.perform(post("/api/auth/logout")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer valid-access-token")
                    .cookie(new Cookie("refreshToken", "valid-refresh-token")))
                .andExpect(status().isNoContent())
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        "refreshToken=; Path=/; Max-Age=0; Expires=Thu, 1 Jan 1970 00:00:00 GMT; Secure; HttpOnly"
                ));
    }

    /**
     * 로그아웃 성공 테스트:
     * 유효한 access token과 폐기된 refresh token으로 요청을 보내고,
     * - (1) 응답 status가 204 No Content인지 확인
     * - (2) refreshToken 쿠키를 삭제하는지 확인
     * - (3) DB의 refresh token이 여전히 is_revoked=true인지 확인
     * - (4) DB의 refresh token이 여전히 1개인지 확인
     */
    @Test
    void logoutReturns204WhenRefreshTokenIsRevoked() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "nickname", null);
        insertRefreshToken(100L, 1L, "valid-refresh-token", true);

        when(jwtProvider.validateRefreshToken("valid-refresh-token"))
                .thenReturn(JwtProvider.TokenValidationResult.VALID);
        when(jwtProvider.getSubjectAsLong("valid-refresh-token"))
                .thenReturn(1L);
        when(jwtProvider.getTokenId("valid-refresh-token"))
                .thenReturn(100L);

        mockMvc.perform(post("/api/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-access-token")
                        .cookie(new Cookie("refreshToken", "valid-refresh-token")))
                .andExpect(status().isNoContent())
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        "refreshToken=; Path=/; Max-Age=0; Expires=Thu, 1 Jan 1970 00:00:00 GMT; Secure; HttpOnly"
                ));

        Boolean isRevoked = jdbcTemplate.queryForObject(
                "SELECT is_revoked FROM refresh_tokens WHERE id = ?",
                Boolean.class,
                100L
        );
        assertThat(isRevoked).isTrue();

        Integer refreshTokenCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refresh_tokens",
                Integer.class
        );
        assertThat(refreshTokenCount).isEqualTo(1);
    }

    /**
     * 로그아웃 성공 테스트:
     * refresh token만 포함해 요청을 보내고,
     * - (1) 응답 status가 204 No Content인지 확인
     * - (2) refreshToken 쿠키를 삭제하는지 확인
     * - (3) DB의 refresh token이 is_revoked=true인지 확인
     */
    @Test
    void logoutReturns204WhenAccessTokenIsMissing() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "nickname", null);
        insertRefreshToken(100L, 1L, "valid-refresh-token", false);

        when(jwtProvider.validateRefreshToken("valid-refresh-token"))
                .thenReturn(JwtProvider.TokenValidationResult.VALID);
        when(jwtProvider.getSubjectAsLong("valid-refresh-token"))
                .thenReturn(1L);
        when(jwtProvider.getTokenId("valid-refresh-token"))
                .thenReturn(100L);

        mockMvc.perform(post("/api/auth/logout")
                        .cookie(new Cookie("refreshToken", "valid-refresh-token")))
                .andExpect(status().isNoContent())
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        "refreshToken=; Path=/; Max-Age=0; Expires=Thu, 1 Jan 1970 00:00:00 GMT; Secure; HttpOnly"
                ));

        Boolean isRevoked = jdbcTemplate.queryForObject(
                "SELECT is_revoked FROM refresh_tokens WHERE id = ?",
                Boolean.class,
                100L
        );
        assertThat(isRevoked).isTrue();
    }

    /**
     * 로그아웃 성공 테스트:
     * 만료된 access token과 유효한 refresh token으로 요청을 보내고,
     * - (1) 응답 status가 204 No Content인지 확인
     * - (2) refreshToken 쿠키를 삭제하는지 확인
     * - (3) DB의 refresh token이 is_revoked=true인지 확인
     */
    @Test
    void logoutReturns204WhenAccessTokenIsExpired() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "nickname", null);
        insertRefreshToken(100L, 1L, "valid-refresh-token", false);

        when(jwtProvider.validateAccessToken("expired-access-token"))
                .thenReturn(JwtProvider.TokenValidationResult.EXPIRED);
        when(jwtProvider.validateRefreshToken("valid-refresh-token"))
                .thenReturn(JwtProvider.TokenValidationResult.VALID);
        when(jwtProvider.getSubjectAsLong("valid-refresh-token"))
                .thenReturn(1L);
        when(jwtProvider.getTokenId("valid-refresh-token"))
                .thenReturn(100L);

        mockMvc.perform(post("/api/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer expired-access-token")
                        .cookie(new Cookie("refreshToken", "valid-refresh-token")))
                .andExpect(status().isNoContent())
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        "refreshToken=; Path=/; Max-Age=0; Expires=Thu, 1 Jan 1970 00:00:00 GMT; Secure; HttpOnly"
                ));

        Boolean isRevoked = jdbcTemplate.queryForObject(
                "SELECT is_revoked FROM refresh_tokens WHERE id = ?",
                Boolean.class,
                100L
        );
        assertThat(isRevoked).isTrue();
    }

    /**
     * 로그아웃 성공 테스트:
     * 잘못된 access token과 유효한 refresh token으로 요청을 보내고,
     * - (1) 응답 status가 204 No Content인지 확인
     * - (2) refreshToken 쿠키를 삭제하는지 확인
     * - (3) DB의 refresh token이 is_revoked=true인지 확인
     */
    @Test
    void logoutReturns204WhenAccessTokenIsInvalid() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "nickname", null);
        insertRefreshToken(100L, 1L, "valid-refresh-token", false);

        when(jwtProvider.validateAccessToken("invalid-access-token"))
                .thenReturn(JwtProvider.TokenValidationResult.INVALID);
        when(jwtProvider.validateRefreshToken("valid-refresh-token"))
                .thenReturn(JwtProvider.TokenValidationResult.VALID);
        when(jwtProvider.getSubjectAsLong("valid-refresh-token"))
                .thenReturn(1L);
        when(jwtProvider.getTokenId("valid-refresh-token"))
                .thenReturn(100L);

        mockMvc.perform(post("/api/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-access-token")
                        .cookie(new Cookie("refreshToken", "valid-refresh-token")))
                .andExpect(status().isNoContent())
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        "refreshToken=; Path=/; Max-Age=0; Expires=Thu, 1 Jan 1970 00:00:00 GMT; Secure; HttpOnly"
                ));

        Boolean isRevoked = jdbcTemplate.queryForObject(
                "SELECT is_revoked FROM refresh_tokens WHERE id = ?",
                Boolean.class,
                100L
        );
        assertThat(isRevoked).isTrue();
    }

    /**
     * 로그아웃 성공 테스트:
     * userId가 다르게 설정된 access token과 refresh token으로 요청을 보내고,
     * - (1) 응답 status가 204 No Content인지 확인
     * - (2) refreshToken 쿠키를 삭제하는지 확인
     * - (3) DB의 refresh token이 is_revoked=true인지 확인
     */
    @Test
    void logoutReturns204WhenUserIdDoesNotMatch() throws Exception {
        stubAuthenticatedUser();
        insertUser(2L, "nickname", null);
        insertRefreshToken(100L, 2L, "valid-refresh-token", false);

        when(jwtProvider.validateRefreshToken("valid-refresh-token"))
                .thenReturn(JwtProvider.TokenValidationResult.VALID);
        when(jwtProvider.getSubjectAsLong("valid-refresh-token"))
                .thenReturn(2L);
        when(jwtProvider.getTokenId("valid-refresh-token"))
                .thenReturn(100L);

        mockMvc.perform(post("/api/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-access-token")
                        .cookie(new Cookie("refreshToken", "valid-refresh-token")))
                .andExpect(status().isNoContent())
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        "refreshToken=; Path=/; Max-Age=0; Expires=Thu, 1 Jan 1970 00:00:00 GMT; Secure; HttpOnly"
                ));

        Boolean isRevoked = jdbcTemplate.queryForObject(
                "SELECT is_revoked FROM refresh_tokens WHERE id = ?",
                Boolean.class,
                100L
        );
        assertThat(isRevoked).isTrue();
    }

    /**
     * 로그아웃 성공 테스트:
     * access token과 DB에 저장된 것과 hash 값이 다른 refresh token으로 요청을 보내고,
     * - (1) 응답 status가 204 No Content인지 확인
     * - (2) refreshToken 쿠키를 삭제하는지 확인
     * - (3) DB의 refresh token이 is_revoked=false인지 확인
     */
    @Test
    void logoutReturns204WhenRefreshTokenHashDoesNotMatch() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "nickname", null);
        insertRefreshTokenWithHash(100L, 1L, "strange-hash-value", false);

        when(jwtProvider.validateRefreshToken("valid-refresh-token"))
                .thenReturn(JwtProvider.TokenValidationResult.VALID);
        when(jwtProvider.getSubjectAsLong("valid-refresh-token"))
                .thenReturn(1L);
        when(jwtProvider.getTokenId("valid-refresh-token"))
                .thenReturn(100L);

        mockMvc.perform(post("/api/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-access-token")
                        .cookie(new Cookie("refreshToken", "valid-refresh-token")))
                .andExpect(status().isNoContent())
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        "refreshToken=; Path=/; Max-Age=0; Expires=Thu, 1 Jan 1970 00:00:00 GMT; Secure; HttpOnly"
                ));

        Boolean isRevoked = jdbcTemplate.queryForObject(
                "SELECT is_revoked FROM refresh_tokens WHERE id = ?",
                Boolean.class,
                100L
        );
        assertThat(isRevoked).isFalse();
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

    private void insertUser(Long id, String nickname, String profileImageUrl) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
                """
                INSERT INTO users
                    (id, nickname, profile_image_url, is_deleted, deleted_at, created_at, updated_at)
                VALUES
                    (?, ?, ?, ?, ?, ?, ?)
                """,
                id,
                nickname,
                profileImageUrl,
                false,
                null,
                now,
                now
        );
    }

    private void insertRefreshToken(
            Long refreshTokenId,
            Long userId,
            String rawRefreshToken,
            boolean isRevoked
    ) {
        LocalDateTime now = LocalDateTime.now();
        String tokenHash = tokenHasher.hash(rawRefreshToken);

        jdbcTemplate.update(
                """
                INSERT INTO refresh_tokens
                    (id, user_id, token_hash, is_revoked, is_rotated, expires_at, revoked_at, created_at)
                VALUES
                    (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                refreshTokenId,
                userId,
                tokenHash,
                isRevoked,
                false,
                now.plusDays(7),
                isRevoked ? now : null,
                now
        );
    }

    private void insertRefreshTokenWithHash(
            Long refreshTokenId,
            Long userId,
            String tokenHash,
            boolean isRevoked
    ) {
        LocalDateTime now = LocalDateTime.now();

        jdbcTemplate.update(
                """
                INSERT INTO refresh_tokens
                    (id, user_id, token_hash, is_revoked, is_rotated, expires_at, revoked_at, created_at)
                VALUES
                    (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                refreshTokenId,
                userId,
                tokenHash,
                isRevoked,
                false,
                now.plusDays(7),
                isRevoked ? now : null,
                now
        );
    }
}
