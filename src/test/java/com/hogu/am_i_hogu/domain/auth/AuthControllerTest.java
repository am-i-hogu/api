package com.hogu.am_i_hogu.domain.auth;

import com.hogu.am_i_hogu.common.security.JwtProvider;
import com.hogu.am_i_hogu.common.security.TokenHasher;
import com.hogu.am_i_hogu.common.util.TsidGenerator;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
public class AuthControllerTest {

    private static final long TEST_USER_ID = 1L;
    private static final long TEST_SOCIAL_ACCOUNT_ID = 100L;
    private static final long TEST_REFRESH_TOKEN_ID = 101L;
    private static final long TEST_REGISTER_SESSION_ID = 103L;

    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("am_i_hogu_auth_test_db")
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

    @Autowired
    private TokenHasher tokenHasher;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private TsidGenerator tsidGenerator;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM register_sessions");
        jdbcTemplate.update("DELETE FROM social_accounts");
        jdbcTemplate.update("DELETE FROM user_hogu_stats");
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
    void logoutReturns204WhenAccessTokenAndRefreshTokenAreValid() throws Exception {
        stubAuthenticatedUser();
        insertUser(TEST_USER_ID, "nickname", null);
        insertRefreshToken(TEST_REFRESH_TOKEN_ID, TEST_USER_ID, "valid-refresh-token", false, false);

        when(jwtProvider.validateRefreshToken("valid-refresh-token"))
                .thenReturn(JwtProvider.TokenValidationResult.VALID);
        when(jwtProvider.getTokenId("valid-refresh-token"))
                .thenReturn(TEST_REFRESH_TOKEN_ID);

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
                TEST_REFRESH_TOKEN_ID
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
        insertUser(1L, "nickname", null);

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
    void logoutReturns204WhenRefreshTokenDoesNotExist() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "nickname", null);

        when(jwtProvider.validateRefreshToken("valid-refresh-token"))
                .thenReturn(JwtProvider.TokenValidationResult.VALID);
        when(jwtProvider.getTokenId("valid-refresh-token"))
                .thenReturn(TEST_REFRESH_TOKEN_ID);

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
        insertUser(TEST_USER_ID, "nickname", null);
        insertRefreshToken(TEST_REFRESH_TOKEN_ID, TEST_USER_ID, "valid-refresh-token", true, false);

        when(jwtProvider.validateRefreshToken("valid-refresh-token"))
                .thenReturn(JwtProvider.TokenValidationResult.VALID);
        when(jwtProvider.getTokenId("valid-refresh-token"))
                .thenReturn(TEST_REFRESH_TOKEN_ID);

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
                TEST_REFRESH_TOKEN_ID
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
        insertUser(TEST_USER_ID, "nickname", null);
        insertRefreshToken(TEST_REFRESH_TOKEN_ID, TEST_USER_ID, "valid-refresh-token", false, false);
        when(jwtProvider.validateAccessToken(null))
                .thenReturn(JwtProvider.TokenValidationResult.EMPTY);


        when(jwtProvider.validateRefreshToken("valid-refresh-token"))
                .thenReturn(JwtProvider.TokenValidationResult.VALID);
        when(jwtProvider.getTokenId("valid-refresh-token"))
                .thenReturn(TEST_REFRESH_TOKEN_ID);

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
                TEST_REFRESH_TOKEN_ID
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
        insertUser(TEST_USER_ID, "nickname", null);
        insertRefreshToken(TEST_REFRESH_TOKEN_ID, TEST_USER_ID, "valid-refresh-token", false, false);

        when(jwtProvider.validateAccessToken("expired-access-token"))
                .thenReturn(JwtProvider.TokenValidationResult.EXPIRED);
        when(jwtProvider.validateRefreshToken("valid-refresh-token"))
                .thenReturn(JwtProvider.TokenValidationResult.VALID);
        when(jwtProvider.getTokenId("valid-refresh-token"))
                .thenReturn(TEST_REFRESH_TOKEN_ID);

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
                TEST_REFRESH_TOKEN_ID
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
        insertUser(TEST_USER_ID, "nickname", null);
        insertRefreshToken(TEST_REFRESH_TOKEN_ID, TEST_USER_ID, "valid-refresh-token", false, false);

        when(jwtProvider.validateAccessToken("invalid-access-token"))
                .thenReturn(JwtProvider.TokenValidationResult.INVALID);
        when(jwtProvider.validateRefreshToken("valid-refresh-token"))
                .thenReturn(JwtProvider.TokenValidationResult.VALID);
        when(jwtProvider.getTokenId("valid-refresh-token"))
                .thenReturn(TEST_REFRESH_TOKEN_ID);

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
                TEST_REFRESH_TOKEN_ID
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
        insertUser(TEST_USER_ID, "nickname", null);
        insertRefreshTokenWithHash(TEST_REFRESH_TOKEN_ID, TEST_USER_ID, "strange-hash-value", false, false);

        when(jwtProvider.validateRefreshToken("valid-refresh-token"))
                .thenReturn(JwtProvider.TokenValidationResult.VALID);
        when(jwtProvider.getTokenId("valid-refresh-token"))
                .thenReturn(TEST_REFRESH_TOKEN_ID);

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
                TEST_REFRESH_TOKEN_ID
        );
        assertThat(isRevoked).isFalse();
    }

    /**
     * 온보딩 성공 테스트:
     * 유효한 register token과 닉네임으로 요청을 보내고,
     * - (1) 응답 status가 200 OK인지 확인
     * - (2) registerToken 쿠키를 삭제하고, 새로운 refresh token 쿠키를 반환하는지 확인
     * - (3) 새로운 access token을 반환하는지 확인
     * - (4) 닉네임, hogu stat, refresh token 테이블이 적절히 초기화되는지 확인
     * - (5) 소셜 계정-유저 연결과 등록 세션 사용 처리가 되는지 확인
     */
    @Test
    void createUserReturns200WhenRegisterTokenAndNicknameAreValid() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        insertSocialAccount(TEST_SOCIAL_ACCOUNT_ID, null, "GOOGLE", "google-provider-id", null, now);
        insertRegisterSession(TEST_REGISTER_SESSION_ID, TEST_SOCIAL_ACCOUNT_ID, "valid-register-token", now, null);

        when(jwtProvider.validateRegisterToken("valid-register-token"))
                .thenReturn(JwtProvider.TokenValidationResult.VALID);
        when(jwtProvider.getSubjectAsLong("valid-register-token"))
                .thenReturn(TEST_SOCIAL_ACCOUNT_ID);
        when(tsidGenerator.nextId())
                .thenReturn(TEST_USER_ID)
                .thenReturn(TEST_REFRESH_TOKEN_ID);
        when(jwtProvider.createRefreshToken(TEST_USER_ID, TEST_REFRESH_TOKEN_ID))
                .thenReturn("new-refresh-token");
        when(jwtProvider.createAccessToken(TEST_USER_ID))
                .thenReturn("new-access-token");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(new Cookie("registerToken", "valid-register-token"))
                        .content("""
                                {
                                  "nickname" : "nickname"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().stringValues(
                        HttpHeaders.SET_COOKIE,
                        "refreshToken=new-refresh-token; Path=/; Secure; HttpOnly",
                        "registerToken=; Path=/; Max-Age=0; Expires=Thu, 1 Jan 1970 00:00:00 GMT; Secure; HttpOnly"
                ))
                .andExpect(content().json("""
                        {
                          "accessToken" : "new-access-token"
                        }
                        """));

        String savedNickname = jdbcTemplate.queryForObject(
                "SELECT nickname FROM users WHERE id = ?",
                String.class,
                TEST_USER_ID
        );
        Integer userHoguStatCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_hogu_stats WHERE user_id = ?",
                Integer.class,
                TEST_USER_ID
        );
        Long linkedUserId = jdbcTemplate.queryForObject(
                "SELECT user_id FROM social_accounts WHERE id = ?",
                Long.class,
                TEST_SOCIAL_ACCOUNT_ID
        );
        LocalDateTime consumedAt = jdbcTemplate.queryForObject(
                "SELECT consumed_at FROM register_sessions WHERE id = ?",
                LocalDateTime.class,
                TEST_REGISTER_SESSION_ID
        );
        Integer refreshTokenCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refresh_tokens WHERE user_id = ?",
                Integer.class,
                TEST_USER_ID
        );

        assertThat(savedNickname).isEqualTo("nickname");
        assertThat(userHoguStatCount).isEqualTo(1);
        assertThat(linkedUserId).isEqualTo(TEST_USER_ID);
        assertThat(consumedAt).isNotNull();
        assertThat(refreshTokenCount).isEqualTo(1);
    }

    /**
     * 온보딩 실패 테스트:
     * DB에 해당 social account의 register session이 없는 상태에서 요청을 보내고,
     * 응답이 401 Unauthorized + INVALID_REGISTER_TOKEN인지 확인
     */
    @Test
    void createUserReturns401WhenRegisterSessionDoesNotExist() throws Exception {
        when(jwtProvider.validateRegisterToken("valid-register-token"))
                .thenReturn(JwtProvider.TokenValidationResult.VALID);
        when(jwtProvider.getSubjectAsLong("valid-register-token"))
                .thenReturn(TEST_SOCIAL_ACCOUNT_ID);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(new Cookie("registerToken", "valid-register-token"))
                        .content("""
                                {
                                  "nickname" : "nickname"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REGISTER_TOKEN"));
    }

    /**
     * 온보딩 실패 테스트:
     * DB에 저장되어 있는 것과 hash 값이 다른 register token으로 요청을 보내고,
     * 응답이 401 Unauthorized + INVALID_REGISTER_TOKEN인지 확인
     */
    @Test
    void createUserReturns401WhenRegisterTokenHashDoesNotMatch() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        insertSocialAccount(TEST_SOCIAL_ACCOUNT_ID, null, "GOOGLE", "google-provider-id", null, now);
        insertRegisterSession(TEST_REGISTER_SESSION_ID, TEST_SOCIAL_ACCOUNT_ID, "different-register-token", now, null);

        when(jwtProvider.validateRegisterToken("valid-register-token"))
                .thenReturn(JwtProvider.TokenValidationResult.VALID);
        when(jwtProvider.getSubjectAsLong("valid-register-token"))
                .thenReturn(TEST_SOCIAL_ACCOUNT_ID);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(new Cookie("registerToken", "valid-register-token"))
                        .content("""
                                {
                                  "nickname" : "nickname"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REGISTER_TOKEN"));
    }

    /**
     * consumed register session
     * 이미 사용된 register token으로 요청을 보내고,
     * 응답이 401 Unauthorized + INVALID_REGISTER_TOKEN인지 확인
     */
    @Test
    void createUserReturns401WhenRegisterSessionIsConsumed() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        insertSocialAccount(TEST_SOCIAL_ACCOUNT_ID, null, "GOOGLE", "google-provider-id", null, now);
        insertRegisterSession(TEST_REGISTER_SESSION_ID, TEST_SOCIAL_ACCOUNT_ID, "valid-register-token", now, now.plusMinutes(1));

        when(jwtProvider.validateRegisterToken("valid-register-token"))
                .thenReturn(JwtProvider.TokenValidationResult.VALID);
        when(jwtProvider.getSubjectAsLong("valid-register-token"))
                .thenReturn(TEST_SOCIAL_ACCOUNT_ID);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(new Cookie("registerToken", "valid-register-token"))
                        .content("""
                                {
                                  "nickname" : "nickname"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REGISTER_TOKEN"));
    }

    /**
     * 온보딩 실패 테스트:
     * 사용 중인 닉네임으로 요청을 보내고,
     * 응답이 409 Conflict + DUPLICATE_NICKNAME인지 확인
     */
    @Test
    void createUserReturns409WhenNicknameIsDuplicated() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        insertUser(TEST_USER_ID, "nickname", null);
        insertSocialAccount(TEST_SOCIAL_ACCOUNT_ID, null, "GOOGLE", "google-provider-id", null, now);
        insertRegisterSession(TEST_REGISTER_SESSION_ID, TEST_SOCIAL_ACCOUNT_ID, "valid-register-token", now, null);

        when(jwtProvider.validateRegisterToken("valid-register-token"))
                .thenReturn(JwtProvider.TokenValidationResult.VALID);
        when(jwtProvider.getSubjectAsLong("valid-register-token"))
                .thenReturn(TEST_SOCIAL_ACCOUNT_ID);
        when(tsidGenerator.nextId())
                .thenReturn(TEST_USER_ID + 1);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(new Cookie("registerToken", "valid-register-token"))
                        .content("""
                                {
                                  "nickname" : "nickname"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_NICKNAME"));
    }

    /**
     * 토큰 재발급 성공 테스트:
     * 유효한 refresh token으로 요청을 보내고,
     * - (1) 응답 status가 200 OK인지 확인
     * - (2) refreshToken 쿠키가 새로운 refresh token으로 설정되는지 확인
     * - (3) 새로운 access token을 반환하는지 확인
     * - (4) refresh token 테이블에 기존 refresh token row가 rotated 처리되는지 확인
     * - (5) refresh token 테이블에 새로운 refresh token 정보가 저장되었는지 확인
     */
    @Test
    void reissueTokenReturns200WhenRefreshTokenIsValid() throws Exception {
        insertUser(TEST_USER_ID, "nickname", null);
        insertRefreshToken(TEST_REFRESH_TOKEN_ID, TEST_USER_ID, "valid-refresh-token", false, false);

        when(jwtProvider.validateRefreshToken("valid-refresh-token"))
                .thenReturn(JwtProvider.TokenValidationResult.VALID);
        when(jwtProvider.getTokenId("valid-refresh-token"))
                .thenReturn(TEST_REFRESH_TOKEN_ID);
        when(tsidGenerator.nextId())
                .thenReturn(TEST_REFRESH_TOKEN_ID + 1);
        when(jwtProvider.createRefreshToken(TEST_USER_ID, TEST_REFRESH_TOKEN_ID + 1))
                .thenReturn("new-refresh-token");
        when(jwtProvider.createAccessToken(TEST_USER_ID))
                .thenReturn("new-access-token");

        mockMvc.perform(post("/api/auth/refresh")
                    .cookie(new Cookie("refreshToken", "valid-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        "refreshToken=new-refresh-token; Path=/; Secure; HttpOnly"
                ))
                .andExpect(content().json(
                        """
                        {
                          "accessToken" : "new-access-token"
                        }
                        """
                ));

        Boolean isRevoked = jdbcTemplate.queryForObject(
                "SELECT is_revoked FROM refresh_tokens WHERE id = ?",
                Boolean.class,
                TEST_REFRESH_TOKEN_ID
        );
        Boolean isRotated = jdbcTemplate.queryForObject(
                "SELECT is_rotated FROM refresh_tokens WHERE id = ?",
                Boolean.class,
                TEST_REFRESH_TOKEN_ID
        );
        LocalDateTime revokedAt = jdbcTemplate.queryForObject(
                "SELECT revoked_at FROM refresh_tokens WHERE id = ?",
                LocalDateTime.class,
                TEST_REFRESH_TOKEN_ID
        );
        Integer refreshTokenCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refresh_tokens WHERE user_id = ?",
                Integer.class,
                TEST_USER_ID
        );
        Boolean newRefreshTokenExists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) = 1 FROM refresh_tokens WHERE id = ? AND user_id = ?",
                Boolean.class,
                TEST_REFRESH_TOKEN_ID + 1,
                TEST_USER_ID
        );

        assertThat(isRevoked).isTrue();
        assertThat(isRotated).isTrue();
        assertThat(revokedAt).isNotNull();
        assertThat(refreshTokenCount).isEqualTo(2);
        assertThat(newRefreshTokenExists).isTrue();
    }

    /**
     * 토큰 재발급 실패 테스트:
     * DB에 저장되어 있지 않은 refresh token으로 요청을 보내고,
     * - (1) 응답이 401 Unauthorized + INVALID_REFRESH_TOKEN인지 확인
     * - (2) 새로운 refresh token 정보가 생성되지 않았는지 확인
     */
    @Test
    void reissueTokenReturns401WhenRefreshTokenDoesNotExist() throws Exception {
        when(jwtProvider.validateRefreshToken("valid-refresh-token"))
                .thenReturn(JwtProvider.TokenValidationResult.VALID);
        when(jwtProvider.getTokenId("valid-refresh-token"))
                .thenReturn(TEST_REFRESH_TOKEN_ID);

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refreshToken", "valid-refresh-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));

        Integer refreshTokenCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refresh_tokens WHERE user_id = ?",
                Integer.class,
                TEST_USER_ID
        );
        assertThat(refreshTokenCount).isEqualTo(0);
    }


    /**
     * 토큰 재발급 실패 테스트:
     * 이미 사용되어 폐기처리된 refresh token으로 요청을 보내고,
     * - (1) 응답이 401 Unauthorized + REFRESH_TOKEN_REUSED인지 확인
     * - (2) 새로운 refresh token 정보가 생성되지 않았는지 확인
     */
    @Test
    void reissueTokenReturns401WhenRefreshTokenIsRotated() throws Exception {
        insertUser(TEST_USER_ID, "nickname", null);
        insertRefreshToken(TEST_REFRESH_TOKEN_ID, TEST_USER_ID, "valid-refresh-token", true, true);

        when(jwtProvider.validateRefreshToken("valid-refresh-token"))
                .thenReturn(JwtProvider.TokenValidationResult.VALID);
        when(jwtProvider.getTokenId("valid-refresh-token"))
                .thenReturn(TEST_REFRESH_TOKEN_ID);

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refreshToken", "valid-refresh-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_REUSED"));

        Boolean isRevoked = jdbcTemplate.queryForObject(
                "SELECT is_revoked FROM refresh_tokens WHERE id = ?",
                Boolean.class,
                TEST_REFRESH_TOKEN_ID
        );
        Boolean isRotated = jdbcTemplate.queryForObject(
                "SELECT is_rotated FROM refresh_tokens WHERE id = ?",
                Boolean.class,
                TEST_REFRESH_TOKEN_ID
        );
        Integer refreshTokenCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refresh_tokens WHERE user_id = ?",
                Integer.class,
                TEST_USER_ID
        );
        assertThat(isRevoked).isTrue();
        assertThat(isRotated).isTrue();
        assertThat(refreshTokenCount).isEqualTo(1);
    }

    /**
     * 토큰 재발급 실패 테스트:
     * DB에 저장된 refresh token과 hash가 일치하지 않는 refresh token으로 요청을 보내고,
     * - (1) 응답이 401 Unauthorized + INVALID_REFRESH_TOKEN인지 확인
     * - (2) 새로운 refresh token 정보가 생성되지 않았는지 확인
     */
    @Test
    void reissueTokenReturns401WhenRefreshTokenHashDoesNotMatch() throws Exception {
        insertUser(TEST_USER_ID, "nickname", null);
        insertRefreshTokenWithHash(TEST_REFRESH_TOKEN_ID, TEST_USER_ID, "different-hash", false, false);

        when(jwtProvider.validateRefreshToken("valid-refresh-token"))
                .thenReturn(JwtProvider.TokenValidationResult.VALID);
        when(jwtProvider.getTokenId("valid-refresh-token"))
                .thenReturn(TEST_REFRESH_TOKEN_ID);

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refreshToken", "valid-refresh-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));

        Boolean isRevoked = jdbcTemplate.queryForObject(
                "SELECT is_revoked FROM refresh_tokens WHERE id = ?",
                Boolean.class,
                TEST_REFRESH_TOKEN_ID
        );
        Boolean isRotated = jdbcTemplate.queryForObject(
                "SELECT is_rotated FROM refresh_tokens WHERE id = ?",
                Boolean.class,
                TEST_REFRESH_TOKEN_ID
        );
        Integer refreshTokenCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refresh_tokens WHERE user_id = ?",
                Integer.class,
                TEST_USER_ID
        );

        assertThat(isRevoked).isFalse();
        assertThat(isRotated).isFalse();
        assertThat(refreshTokenCount).isEqualTo(1);
    }

    /**
     * 테스트에서 사용할 가짜 로그인 사용자를 설정
     * Authorization 헤더에 "Bearer valid-access-token"이 들어오면 userId = 1L 사용자로 인증된 상태가 됨
     */
    private void stubAuthenticatedUser() {
        when(jwtProvider.validateAccessToken("valid-access-token"))
                .thenReturn(JwtProvider.TokenValidationResult.VALID);
        when(jwtProvider.getSubjectAsLong("valid-access-token"))
                .thenReturn(1L);
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
            boolean isRevoked,
            boolean isRotated
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
                isRotated,
                now.plusDays(7),
                isRevoked ? now : null,
                now
        );
    }

    private void insertRefreshTokenWithHash(
            Long refreshTokenId,
            Long userId,
            String tokenHash,
            boolean isRevoked,
            boolean isRotated
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
                isRotated,
                now.plusDays(7),
                isRevoked ? now : null,
                now
        );
    }

    private void insertSocialAccount(
            Long socialAccountId,
            Long userId,
            String provider,
            String providerUserId,
            LocalDateTime linkedAt,
            LocalDateTime createdAt
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO social_accounts
                    (id, user_id, provider, provider_user_id, linked_at, created_at)
                VALUES
                    (?, ?, ?, ?, ?, ?)
                """,
                socialAccountId,
                userId,
                provider,
                providerUserId,
                linkedAt,
                createdAt
        );
    }

    private void insertRegisterSession(
            Long registerSessionId,
            Long socialAccountId,
            String registerToken,
            LocalDateTime createdAt,
            LocalDateTime consumedAt
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO register_sessions
                    (id, social_account_id, register_token_hash, expires_at, consumed_at, created_at)
                VALUES
                    (?, ?, ?, ?, ?, ?)
                """,
                registerSessionId,
                socialAccountId,
                tokenHasher.hash(registerToken),
                createdAt.plusMinutes(10),
                consumedAt,
                createdAt
        );
    }
}
