package com.hogu.am_i_hogu.domain.oauth.controller;

import com.hogu.am_i_hogu.common.security.*;
import com.hogu.am_i_hogu.common.util.TsidGenerator;
import com.hogu.am_i_hogu.domain.oauth.domain.OAuthProvider;
import com.hogu.am_i_hogu.domain.oauth.dto.OAuthUserInfo;
import com.hogu.am_i_hogu.domain.oauth.dto.response.OAuthAuthenticationResult;
import com.hogu.am_i_hogu.domain.oauth.dto.response.TokenResponse;
import com.hogu.am_i_hogu.domain.oauth.service.OAuthCallbackHandler;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.common.security.JwtProvider;
import com.hogu.am_i_hogu.common.security.TokenEncryptor;
import com.hogu.am_i_hogu.common.security.TokenHasher;
import com.hogu.am_i_hogu.domain.oauth.exception.OAuthErrorCode;
import com.hogu.am_i_hogu.domain.oauth.service.OAuthClient;
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
import org.springframework.web.util.UriComponentsBuilder;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
public class OAuthControllerTest {

    private static final long TEST_USER_ID = 1L;
    private static final long TEST_SOCIAL_ACCOUNT_ID = 10L;
    private static final long TEST_REFRESH_TOKEN_ID = 50L;
    private static final long TEST_REGISTER_SESSION_ID = 100L;
    private static final long TEST_OAUTH_LOGIN_STATE_ID = 150L;
    private static final long TEST_SOCIAL_OAUTH_TOKEN_ID = 200L;
    private static final long TEST_EXISTING_SOCIAL_OAUTH_TOKEN_ID = 250L;
    private static final String TEST_PROVIDER_USER_ID = "test-provider-user-id";

    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("am_i_hogu_oauth_test_db")
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
    private TokenEncryptor tokenEncryptor;

    @Autowired
    private TokenHasher tokenHasher;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private OAuthClient oauthClient;

    @MockitoBean
    private TsidGenerator tsidGenerator;

    @MockitoBean
    private OAuthCallbackHandler oauthCallbackHandler;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM register_sessions");
        jdbcTemplate.update("DELETE FROM social_oauth_tokens");
        jdbcTemplate.update("DELETE FROM refresh_tokens");
        jdbcTemplate.update("DELETE FROM social_accounts");
        jdbcTemplate.update("DELETE FROM user_hogu_stats");
        jdbcTemplate.update("DELETE FROM users");
    }

    /**
     * 로그인 요청 redirect 성공 테스트:
     * Google 로그인 요청을 보내고,
     * - (1) 응답 status가 302 Found인지 확인
     * - (2) Location 헤더가 Google 로그인 페이지 Url로 설정되었는지 확인
     * - (3) redirect query param이 적절하게 설정되었는지 확인
     * - (4) oauth login state 테이블 데이터가 적절하게 생성되었는지 확인
     */
    @Test
    void redirectToProviderLoginReturns302WhenProviderIsValid() throws Exception {
        when(tsidGenerator.nextId())
                .thenReturn(TEST_OAUTH_LOGIN_STATE_ID);

        String location = mockMvc.perform(get("/api/auth/login/GOOGLE"))
                .andExpect(status().isFound())
                .andExpect(header().string(
                        HttpHeaders.LOCATION,
                        Matchers.startsWith("https://accounts.google.com/o/oauth2/v2/auth")
                ))
                .andReturn()
                .getResponse()
                .getHeader(HttpHeaders.LOCATION);

        Map<String, List<String>> params = UriComponentsBuilder.fromUriString(location)
                        .build()
                        .getQueryParams();
        String savedState = jdbcTemplate.queryForObject(
                "SELECT state FROM oauth_login_states WHERE id = ?",
                String.class,
                TEST_OAUTH_LOGIN_STATE_ID
        );
        String savedNonce = jdbcTemplate.queryForObject(
                "SELECT nonce FROM oauth_login_states WHERE id = ?",
                String.class,
                TEST_OAUTH_LOGIN_STATE_ID
        );
        String provider = jdbcTemplate.queryForObject(
                "SELECT provider FROM oauth_login_states WHERE id = ?",
                String.class,
                TEST_OAUTH_LOGIN_STATE_ID
        );
        LocalDateTime createdAt = jdbcTemplate.queryForObject(
                "SELECT created_at FROM oauth_login_states WHERE id = ?",
                LocalDateTime.class,
                TEST_OAUTH_LOGIN_STATE_ID
        );
        LocalDateTime expiresAt = jdbcTemplate.queryForObject(
                "SELECT expires_at FROM oauth_login_states WHERE id = ?",
                LocalDateTime.class,
                TEST_OAUTH_LOGIN_STATE_ID
        );
        LocalDateTime consumedAt = jdbcTemplate.queryForObject(
                "SELECT consumed_at FROM oauth_login_states WHERE id = ?",
                LocalDateTime.class,
                TEST_OAUTH_LOGIN_STATE_ID
        );


        assertThat(params.get("client_id").get(0)).isEqualTo("test-client-id");
        assertThat(params.get("redirect_uri").get(0)).isEqualTo("http://localhost:8080/api/auth/callback/GOOGLE");
        assertThat(params.get("state").get(0)).isEqualTo(savedState);
        assertThat(params.get("nonce").get(0)).isEqualTo(savedNonce);

        assertThat(provider).isEqualTo("GOOGLE");
        assertThat(createdAt).isNotNull();
        assertThat(consumedAt).isNull();
        assertThat(expiresAt).isEqualTo(createdAt.plusMinutes(5));
    }

    /**
     * 로그인 요청 redirect 실패 테스트:
     * 지원하지 않는 provider로 로그인 요청을 보내고,
     * - (1) 응답 status가 400 Bad Request인지 확인
     * - (2) 응답 본문이 UNSUPPORTED_PROVIDER 오류 코드를 반환하는지 확인
     */
    @Test
    void redirectToProviderLoginReturns400WhenProviderIsInvalid() throws Exception {
        mockMvc.perform(get("/api/auth/login/INVALID_PROVIDER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_PROVIDER"));
    }

    /**
     * 기존 유저 callback 처리 성공 테스트:
     * Google에 callback 요청을 보내고,
     * - (1) 응답 status가 302 Found인지 확인
     * - (2) Location 헤더가 로그인 성공 시 이동할 url로 설정되었는지 확인
     * - (3) Set-Cookie 헤더가 refresh token 정보를 포함하는지 확인
     * - (4) oauth login state row가 사용 처리되었는지 확인
     * - (5) social account table에 변화가 없는지 확인
     * - (6) register session row가 생성되지 않았는지 확인
     * - (7) social oauth token과 refresh token이 생성되었는지 확인
     */
    @Test
    void handleCallbackReturns302WhenProviderIsValidForExistingUser() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        insertOAuthLoginState(
                TEST_OAUTH_LOGIN_STATE_ID,
                OAuthProvider.GOOGLE,
                "test-state",
                "test-nonce",
                now.plusMinutes(5),
                null
        );
        insertUser(TEST_USER_ID, "nickname", false, now);
        insertSocialAccount(
                TEST_SOCIAL_ACCOUNT_ID,
                TEST_USER_ID,
                OAuthProvider.GOOGLE,
                TEST_PROVIDER_USER_ID,
                null,
                now
        );

        TokenResponse tokenResponse = createTokenResponse(
                "google-access-token",
                3600,
                "google-refresh-token",
                7200
        );

        when(oauthCallbackHandler.handle(eq("test-code"), any(), eq(OAuthProvider.GOOGLE)))
                .thenReturn(createAuthResult(OAuthProvider.GOOGLE, TEST_PROVIDER_USER_ID, tokenResponse));
        when(tokenEncryptor.encrypt("google-access-token"))
                .thenReturn("encrypted-google-access-token");
        when(tokenEncryptor.encrypt("google-refresh-token"))
                .thenReturn("encrypted-google-refresh-token");
        when(tsidGenerator.nextId())
                .thenReturn(TEST_SOCIAL_OAUTH_TOKEN_ID, TEST_REFRESH_TOKEN_ID);
        when(jwtProvider.createRefreshToken(TEST_USER_ID, TEST_REFRESH_TOKEN_ID))
                .thenReturn("valid-refresh-token");

        mockMvc.perform(get("/api/auth/callback/GOOGLE")
                        .param("code", "test-code")
                        .param("state", "test-state"))
                .andExpect(status().isFound())
                .andExpect(header().string(
                        HttpHeaders.LOCATION,
                        "http://localhost:8080/oauth/callback?status=LOGIN_SUCCESS"
                ))
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        "refreshToken=valid-refresh-token; Path=/; Secure; HttpOnly"
                ));

        LocalDateTime consumedAt = jdbcTemplate.queryForObject(
                "SELECT consumed_at FROM oauth_login_states WHERE id = ?",
                LocalDateTime.class,
                TEST_OAUTH_LOGIN_STATE_ID
        );
        Long socialAccountUserId = jdbcTemplate.queryForObject(
                "SELECT user_id FROM social_accounts WHERE id = ?",
                Long.class,
                TEST_SOCIAL_ACCOUNT_ID
        );
        String socialAccountProvider = jdbcTemplate.queryForObject(
                "SELECT provider FROM social_accounts WHERE id = ?",
                String.class,
                TEST_SOCIAL_ACCOUNT_ID
        );
        String socialAccountProviderUserId = jdbcTemplate.queryForObject(
                "SELECT provider_user_id FROM social_accounts WHERE id = ? AND user_id = ?",
                String.class,
                TEST_SOCIAL_ACCOUNT_ID,
                TEST_USER_ID
        );
        Integer socialOAuthTokenCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM social_oauth_tokens WHERE social_account_id = ?",
                Integer.class,
                TEST_SOCIAL_ACCOUNT_ID
        );
        Integer registerSessionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM register_sessions WHERE social_account_id = ?",
                Integer.class,
                TEST_SOCIAL_ACCOUNT_ID
        );
        Integer refreshTokenCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refresh_tokens WHERE user_id = ?",
                Integer.class,
                TEST_USER_ID
        );

        assertThat(consumedAt).isNotNull();
        assertThat(socialAccountUserId).isEqualTo(TEST_USER_ID);
        assertThat(socialAccountProvider).isEqualTo("GOOGLE");
        assertThat(socialAccountProviderUserId).isEqualTo(TEST_PROVIDER_USER_ID);
        assertThat(socialOAuthTokenCount).isEqualTo(1);
        assertThat(registerSessionCount).isEqualTo(0);
        assertThat(refreshTokenCount).isEqualTo(1);
    }

    /**
     * 신규 유저 callback 처리 성공 테스트:
     * Google에 callback 요청을 보내고,
     * - (1) 응답 status가 302 Found인지 확인
     * - (2) Location 헤더가 온보딩 url로 설정되었는지 확인
     * - (3) Set-Cookie 헤더가 register token 정보를 포함하는지 확인
     * - (4) oauth login state row가 사용 처리되었는지 확인
     * - (5) social account table row가 생성되었는지 확인
     * - (6) register session row가 생성되었는지 확인
     * - (7) social oauth token row가 생성되었는지 확인
     * - (8) refresh token row가 생성되지 않았는지 확인
     */
    @Test
    void handleCallbackReturns302WhenProviderIsValidForNewUser() throws Exception {
        insertOAuthLoginState(
                TEST_OAUTH_LOGIN_STATE_ID,
                OAuthProvider.GOOGLE,
                "test-state",
                "test-nonce",
                LocalDateTime.now().plusMinutes(5),
                null
        );

        TokenResponse tokenResponse = createTokenResponse(
                "google-access-token",
                3600,
                "google-refresh-token",
                7200
        );

        when(oauthCallbackHandler.handle(eq("test-code"), any(), eq(OAuthProvider.GOOGLE)))
                .thenReturn(createAuthResult(OAuthProvider.GOOGLE, TEST_PROVIDER_USER_ID, tokenResponse));
        when(tokenEncryptor.encrypt("google-access-token"))
                .thenReturn("encrypted-google-access-token");
        when(tokenEncryptor.encrypt("google-refresh-token"))
                .thenReturn("encrypted-google-refresh-token");
        when(tsidGenerator.nextId())
                .thenReturn(
                        TEST_SOCIAL_ACCOUNT_ID,
                        TEST_SOCIAL_OAUTH_TOKEN_ID,
                        TEST_REGISTER_SESSION_ID
                );
        when(jwtProvider.createRegisterToken(TEST_SOCIAL_ACCOUNT_ID))
                .thenReturn("valid-register-token");

        mockMvc.perform(get("/api/auth/callback/GOOGLE")
                        .param("code", "test-code")
                        .param("state", "test-state"))
                .andExpect(status().isFound())
                .andExpect(header().string(
                        HttpHeaders.LOCATION,
                        "http://localhost:8080/onboarding"
                ))
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        "registerToken=valid-register-token; Path=/; Secure; HttpOnly"
                ));

        LocalDateTime consumedAt = jdbcTemplate.queryForObject(
                "SELECT consumed_at FROM oauth_login_states WHERE id = ?",
                LocalDateTime.class,
                TEST_OAUTH_LOGIN_STATE_ID
        );
        Long socialAccountUserId = jdbcTemplate.queryForObject(
                "SELECT user_id FROM social_accounts WHERE id = ?",
                Long.class,
                TEST_SOCIAL_ACCOUNT_ID
        );
        String socialAccountProvider = jdbcTemplate.queryForObject(
                "SELECT provider FROM social_accounts WHERE id = ?",
                String.class,
                TEST_SOCIAL_ACCOUNT_ID
        );
        String socialAccountProviderUserId = jdbcTemplate.queryForObject(
                "SELECT provider_user_id FROM social_accounts WHERE id = ?",
                String.class,
                TEST_SOCIAL_ACCOUNT_ID
        );
        Long registerSessionSocialAccountId = jdbcTemplate.queryForObject(
                "SELECT social_account_id FROM register_sessions WHERE id = ?",
                Long.class,
                TEST_REGISTER_SESSION_ID
        );
        Integer socialOAuthTokenCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM social_oauth_tokens WHERE social_account_id = ?",
                Integer.class,
                TEST_SOCIAL_ACCOUNT_ID
        );
        Integer refreshTokenCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refresh_tokens WHERE user_id = ?",
                Integer.class,
                TEST_USER_ID
        );

        assertThat(consumedAt).isNotNull();
        assertThat(socialAccountUserId).isNull();
        assertThat(socialAccountProvider).isEqualTo("GOOGLE");
        assertThat(socialAccountProviderUserId).isEqualTo(TEST_PROVIDER_USER_ID);
        assertThat(registerSessionSocialAccountId).isEqualTo(TEST_SOCIAL_ACCOUNT_ID);
        assertThat(socialOAuthTokenCount).isEqualTo(1);
        assertThat(refreshTokenCount).isEqualTo(0);
    }

    /**
     * callback 처리 실패 테스트:
     * DB에 저장되지 않은 state로 callback 요청을 보내고,
     * - (1) 응답 status가 302 Found인지 확인
     * - (2) Location 헤더가 프론트 실패 redirect URI와 같은지 확인
     */
    @Test
    void handleCallbackReturns302WhenStateIsInvalid() throws Exception {
        mockMvc.perform(get("/api/auth/callback/GOOGLE")
                        .param("code", "test-code")
                        .param("state", "test-state"))
                .andExpect(status().isFound())
                .andExpect(header().string(
                        HttpHeaders.LOCATION,
                        "http://localhost:3000/oauth/callback?status=LOGIN_FAILED&code=INVALID_STATE"
                ));
    }

    /**
     * callback 처리 실패 테스트:
     * state row에 저장된 provider와 다른 provider로 callback 요청을 보내고,
     * - (1) 응답 status가 302 Found인지 확인
     * - (2) Location 헤더가 PROVIDER_MISMATCH 실패 redirect URI와 같은지 확인
     * - (3) oauth login state row가 사용 처리되지 않았는지 확인
     */
    @Test
    void handleCallbackReturns302WhenProviderDoesNotMatchStateProvider() throws Exception {
        insertOAuthLoginState(
                TEST_OAUTH_LOGIN_STATE_ID,
                OAuthProvider.GOOGLE,
                "test-state",
                "test-nonce",
                LocalDateTime.now().plusMinutes(5),
                null
        );

        mockMvc.perform(get("/api/auth/callback/KAKAO")
                        .param("code", "test-code")
                        .param("state", "test-state"))
                .andExpect(status().isFound())
                .andExpect(header().string(
                        HttpHeaders.LOCATION,
                        "http://localhost:3000/oauth/callback?status=LOGIN_FAILED&code=PROVIDER_MISMATCH"
                ));

        LocalDateTime consumedAt = jdbcTemplate.queryForObject(
                "SELECT consumed_at FROM oauth_login_states WHERE id = ?",
                LocalDateTime.class,
                TEST_OAUTH_LOGIN_STATE_ID
        );
        assertThat(consumedAt).isNull();
    }

    /**
     * callback 처리 실패 테스트:
     * 이미 사용 처리된 state로 callback 요청을 보내고,
     * - (1) 응답 status가 302 Found인지 확인
     * - (2) Location 헤더가 STATE_REUSED 실패 redirect URI와 같은지 확인
     * - (3) oauth login state row가 여전히 사용 처리 상태인지 확인
     */
    @Test
    void handleCallbackReturns302WhenStateIsReused() throws Exception {
        LocalDateTime consumedAt = LocalDateTime.now();
        insertOAuthLoginState(
                TEST_OAUTH_LOGIN_STATE_ID,
                OAuthProvider.GOOGLE,
                "test-state",
                "test-nonce",
                LocalDateTime.now().plusMinutes(5),
                consumedAt
        );

        mockMvc.perform(get("/api/auth/callback/GOOGLE")
                        .param("code", "test-code")
                        .param("state", "test-state"))
                .andExpect(status().isFound())
                .andExpect(header().string(
                        HttpHeaders.LOCATION,
                        "http://localhost:3000/oauth/callback?status=LOGIN_FAILED&code=STATE_REUSED"
                ));

        LocalDateTime savedConsumedAt = jdbcTemplate.queryForObject(
                "SELECT consumed_at FROM oauth_login_states WHERE id = ?",
                LocalDateTime.class,
                TEST_OAUTH_LOGIN_STATE_ID
        );
        assertThat(savedConsumedAt).isNotNull();
    }

    /**
     * callback 처리 실패 테스트:
     * 만료된 state로 callback 요청을 보내고,
     * - (1) 응답 status가 302 Found인지 확인
     * - (2) Location 헤더가 STATE_EXPIRED 실패 redirect URI와 같은지 확인
     * - (3) oauth login state row가 사용 처리되지 않았는지 확인
     */
    @Test
    void handleCallbackReturns302WhenStateIsExpired() throws Exception {
        insertOAuthLoginState(
                TEST_OAUTH_LOGIN_STATE_ID,
                OAuthProvider.GOOGLE,
                "test-state",
                "test-nonce",
                LocalDateTime.now().minusMinutes(1),
                null
        );

        mockMvc.perform(get("/api/auth/callback/GOOGLE")
                        .param("code", "test-code")
                        .param("state", "test-state"))
                .andExpect(status().isFound())
                .andExpect(header().string(
                        HttpHeaders.LOCATION,
                        "http://localhost:3000/oauth/callback?status=LOGIN_FAILED&code=STATE_EXPIRED"
                ));

        LocalDateTime consumedAt = jdbcTemplate.queryForObject(
                "SELECT consumed_at FROM oauth_login_states WHERE id = ?",
                LocalDateTime.class,
                TEST_OAUTH_LOGIN_STATE_ID
        );
        assertThat(consumedAt).isNull();
    }

    /**
     * callback 처리 실패 테스트:
     * callback handler가 ID token 검증에 실패한 상태로 callback 요청을 보내고,
     * - (1) 응답 status가 302 Found인지 확인
     * - (2) Location 헤더가 INVALID_ID_TOKEN 실패 redirect URI와 같은지 확인
     * - (3) oauth login state row가 사용 처리되지 않았는지 확인
     */
    @Test
    void handleCallbackReturns302WhenIdTokenIsInvalid() throws Exception {
        insertOAuthLoginState(
                TEST_OAUTH_LOGIN_STATE_ID,
                OAuthProvider.GOOGLE,
                "test-state",
                "test-nonce",
                LocalDateTime.now().plusMinutes(5),
                null
        );

        when(oauthCallbackHandler.handle(eq("test-code"), any(), eq(OAuthProvider.GOOGLE)))
                .thenThrow(new CustomException(OAuthErrorCode.INVALID_ID_TOKEN));

        mockMvc.perform(get("/api/auth/callback/GOOGLE")
                        .param("code", "test-code")
                        .param("state", "test-state"))
                .andExpect(status().isFound())
                .andExpect(header().string(
                        HttpHeaders.LOCATION,
                        "http://localhost:3000/oauth/callback?status=LOGIN_FAILED&code=INVALID_ID_TOKEN"
                ));

        LocalDateTime consumedAt = jdbcTemplate.queryForObject(
                "SELECT consumed_at FROM oauth_login_states WHERE id = ?",
                LocalDateTime.class,
                TEST_OAUTH_LOGIN_STATE_ID
        );
        assertThat(consumedAt).isNull();
    }

    /**
     * callback 처리 성공 테스트:
     * 기존 social oauth token이 있고 provider가 refresh token 없이 callback 응답을 준 상태에서 요청을 보내고,
     * - (1) 응답 status가 302 Found인지 확인
     * - (2) Location 헤더가 로그인 성공 시 이동할 url로 설정되었는지 확인
     * - (3) 기존 social oauth token row의 access token만 갱신되는지 확인
     * - (4) refresh token 정보와 만료 시각은 유지되는지 확인
     * - (5) refresh token row가 새로 생성되는지 확인
     */
    @Test
    void handleCallbackReturns302WhenProviderDoesNotReturnRefreshTokenForExistingUser() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime existingRefreshTokenExpiresAt = now.plusDays(30).withNano(0);
        insertOAuthLoginState(
                TEST_OAUTH_LOGIN_STATE_ID,
                OAuthProvider.GOOGLE,
                "test-state",
                "test-nonce",
                now.plusMinutes(5),
                null
        );
        insertUser(TEST_USER_ID, "nickname", false, now);
        insertSocialAccount(
                TEST_SOCIAL_ACCOUNT_ID,
                TEST_USER_ID,
                OAuthProvider.GOOGLE,
                TEST_PROVIDER_USER_ID,
                now,
                now
        );
        insertSocialOAuthToken(
                TEST_EXISTING_SOCIAL_OAUTH_TOKEN_ID,
                TEST_SOCIAL_ACCOUNT_ID,
                "old-encrypted-access-token",
                "old-encrypted-refresh-token",
                now.plusHours(1),
                existingRefreshTokenExpiresAt,
                now.minusDays(1)
        );

        TokenResponse tokenResponse = createTokenResponse(
                "google-access-token",
                3600,
                null,
                null
        );

        when(oauthCallbackHandler.handle(eq("test-code"), any(), eq(OAuthProvider.GOOGLE)))
                .thenReturn(createAuthResult(OAuthProvider.GOOGLE, TEST_PROVIDER_USER_ID, tokenResponse));
        when(tokenEncryptor.encrypt("google-access-token"))
                .thenReturn("new-encrypted-access-token");
        when(tsidGenerator.nextId())
                .thenReturn(TEST_REFRESH_TOKEN_ID);
        when(jwtProvider.createRefreshToken(TEST_USER_ID, TEST_REFRESH_TOKEN_ID))
                .thenReturn("valid-refresh-token");

        mockMvc.perform(get("/api/auth/callback/GOOGLE")
                        .param("code", "test-code")
                        .param("state", "test-state"))
                .andExpect(status().isFound())
                .andExpect(header().string(
                        HttpHeaders.LOCATION,
                        "http://localhost:8080/oauth/callback?status=LOGIN_SUCCESS"
                ))
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        "refreshToken=valid-refresh-token; Path=/; Secure; HttpOnly"
                ));

        String accessTokenEncrypted = jdbcTemplate.queryForObject(
                "SELECT access_token_encrypted FROM social_oauth_tokens WHERE id = ?",
                String.class,
                TEST_EXISTING_SOCIAL_OAUTH_TOKEN_ID
        );
        String refreshTokenEncrypted = jdbcTemplate.queryForObject(
                "SELECT refresh_token_encrypted FROM social_oauth_tokens WHERE id = ?",
                String.class,
                TEST_EXISTING_SOCIAL_OAUTH_TOKEN_ID
        );
        LocalDateTime refreshTokenExpiresAt = jdbcTemplate.queryForObject(
                "SELECT refresh_token_expires_at FROM social_oauth_tokens WHERE id = ?",
                LocalDateTime.class,
                TEST_EXISTING_SOCIAL_OAUTH_TOKEN_ID
        );
        Integer socialOAuthTokenCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM social_oauth_tokens WHERE social_account_id = ?",
                Integer.class,
                TEST_SOCIAL_ACCOUNT_ID
        );
        Integer refreshTokenCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refresh_tokens WHERE user_id = ?",
                Integer.class,
                TEST_USER_ID
        );

        assertThat(accessTokenEncrypted).isEqualTo("new-encrypted-access-token");
        assertThat(refreshTokenEncrypted).isEqualTo("old-encrypted-refresh-token");
        assertThat(refreshTokenExpiresAt).isEqualTo(existingRefreshTokenExpiresAt);
        assertThat(socialOAuthTokenCount).isEqualTo(1);
        assertThat(refreshTokenCount).isEqualTo(1);
    }

    /**
     * callback 처리 성공 테스트:
     * provider가 refresh token 만료 시각 없이 callback 응답을 준 상태에서 요청을 보내고,
     * - (1) 응답 status가 302 Found인지 확인
     * - (2) Location 헤더가 로그인 성공 시 이동할 url로 설정되었는지 확인
     * - (3) social oauth token row가 생성되는지 확인
     * - (4) refresh token 만료 시각이 null로 저장되는지 확인
     * - (5) refresh token row가 새로 생성되는지 확인
     */
    @Test
    void handleCallbackReturns302WhenRefreshTokenExpiryIsMissing() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        insertOAuthLoginState(
                TEST_OAUTH_LOGIN_STATE_ID,
                OAuthProvider.GOOGLE,
                "test-state",
                "test-nonce",
                now.plusMinutes(5),
                null
        );
        insertUser(TEST_USER_ID, "nickname", false, now);
        insertSocialAccount(
                TEST_SOCIAL_ACCOUNT_ID,
                TEST_USER_ID,
                OAuthProvider.GOOGLE,
                TEST_PROVIDER_USER_ID,
                now,
                now
        );

        TokenResponse tokenResponse = createTokenResponse(
                "google-access-token",
                3600,
                "google-refresh-token",
                null
        );

        when(oauthCallbackHandler.handle(eq("test-code"), any(), eq(OAuthProvider.GOOGLE)))
                .thenReturn(createAuthResult(OAuthProvider.GOOGLE, TEST_PROVIDER_USER_ID, tokenResponse));
        when(tokenEncryptor.encrypt("google-access-token"))
                .thenReturn("encrypted-google-access-token");
        when(tokenEncryptor.encrypt("google-refresh-token"))
                .thenReturn("encrypted-google-refresh-token");
        when(tsidGenerator.nextId())
                .thenReturn(TEST_SOCIAL_OAUTH_TOKEN_ID, TEST_REFRESH_TOKEN_ID);
        when(jwtProvider.createRefreshToken(TEST_USER_ID, TEST_REFRESH_TOKEN_ID))
                .thenReturn("valid-refresh-token");

        mockMvc.perform(get("/api/auth/callback/GOOGLE")
                        .param("code", "test-code")
                        .param("state", "test-state"))
                .andExpect(status().isFound())
                .andExpect(header().string(
                        HttpHeaders.LOCATION,
                        "http://localhost:8080/oauth/callback?status=LOGIN_SUCCESS"
                ))
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        "refreshToken=valid-refresh-token; Path=/; Secure; HttpOnly"
                ));

        LocalDateTime savedRefreshTokenExpiresAt = jdbcTemplate.queryForObject(
                "SELECT refresh_token_expires_at FROM social_oauth_tokens WHERE id = ?",
                LocalDateTime.class,
                TEST_SOCIAL_OAUTH_TOKEN_ID
        );
        Integer socialOAuthTokenCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM social_oauth_tokens WHERE social_account_id = ?",
                Integer.class,
                TEST_SOCIAL_ACCOUNT_ID
        );
        Integer refreshTokenCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refresh_tokens WHERE user_id = ?",
                Integer.class,
                TEST_USER_ID
        );

        assertThat(savedRefreshTokenExpiresAt).isNull();
        assertThat(socialOAuthTokenCount).isEqualTo(1);
        assertThat(refreshTokenCount).isEqualTo(1);
    }

    /**
     * callback 처리 실패 테스트:
     * 지원하지 않는 provider로 callback 요청을 보내고,
     * - (1) 응답 status가 302 Found인지 확인
     * - (2) Location 헤더가 프론트 실패 redirect URI와 같은지 확인
     */
    @Test
    void handleCallbackReturns302WhenProviderIsInvalid() throws Exception {
        mockMvc.perform(get("/api/auth/callback/INVALID_PROVIDER")
                        .param("code", "test-code")
                        .param("state", "test-state"))
                .andExpect(status().isFound())
                .andExpect(header().string(
                        HttpHeaders.LOCATION,
                        "http://localhost:3000/oauth/callback?status=LOGIN_FAILED&code=UNSUPPORTED_PROVIDER"
                ));
    }

    /**
     * 구글 사용자 탈퇴 성공 테스트:
     * 구글로 가입한 유저의 탈퇴 요청을 보내고,
     * - (1) 응답 status가 204 No Content인지 확인
     * - (2) refresh token cookie가 삭제되는지 확인
     * - (3) 유저 soft delete 처리가 제대로 되었는지 확인 (is_deleted = true, nickname에 "d_" prefix 추가)
     * - (4) social_accounts, social_oauth_tokens, refresh_tokens,
     *       register_sessions, user_hogu_stats 테이블에서 유저 정보가 모두 hard delete 되었는지 확인
     * - (5) refresh token을 통해 revoke API가 호출되었는지 확인
     */
    @Test
    void deleteUserReturns204AndDeletesGoogleUserData() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        insertUser(TEST_USER_ID, "nickname", false, now);
        insertUserHoguStat(TEST_USER_ID, now);
        insertSocialAccount(TEST_SOCIAL_ACCOUNT_ID, TEST_USER_ID, "GOOGLE", "google-provider-id", now);
        insertSocialOAuthToken(TEST_SOCIAL_OAUTH_TOKEN_ID, TEST_SOCIAL_ACCOUNT_ID, "google-access-token", "google-refresh-token", now);
        insertRefreshToken(TEST_REFRESH_TOKEN_ID, TEST_USER_ID, "local-refresh-token", now);
        insertRegisterSession(TEST_REGISTER_SESSION_ID, TEST_SOCIAL_ACCOUNT_ID, "register-token", now);

        stubAuthenticatedUser();

        mockMvc.perform(delete("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-access-token"))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, Matchers.containsString("refreshToken=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, Matchers.containsString("Max-Age=0")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, Matchers.containsString("Path=/")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, Matchers.containsString("Secure")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, Matchers.containsString("HttpOnly")));

        Boolean isDeleted = jdbcTemplate.queryForObject(
                "SELECT is_deleted FROM users WHERE id = ?",
                Boolean.class,
                TEST_USER_ID
        );
        String deletedNickname = jdbcTemplate.queryForObject(
                "SELECT nickname FROM users WHERE id = ?",
                String.class,
                TEST_USER_ID
        );
        Integer socialAccountCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM social_accounts WHERE id = ?",
                Integer.class,
                TEST_SOCIAL_ACCOUNT_ID
        );
        Integer socialTokenCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM social_oauth_tokens WHERE social_account_id = ?",
                Integer.class,
                TEST_SOCIAL_ACCOUNT_ID
        );
        Integer refreshTokenCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refresh_tokens WHERE user_id = ?",
                Integer.class,
                TEST_USER_ID
        );
        Integer registerSessionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM register_sessions WHERE social_account_id = ?",
                Integer.class,
                TEST_SOCIAL_ACCOUNT_ID
        );
        Integer userHoguStatCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_hogu_stats WHERE user_id = ?",
                Integer.class,
                TEST_USER_ID
        );

        assertThat(isDeleted).isTrue();
        assertThat(deletedNickname).startsWith("d_");
        assertThat(socialAccountCount).isZero();
        assertThat(socialTokenCount).isZero();
        assertThat(refreshTokenCount).isZero();
        assertThat(registerSessionCount).isZero();
        assertThat(userHoguStatCount).isZero();

        verify(oauthClient).revokeGoogleToken("google-refresh-token");
    }

    /**
     * 카카오 사용자 탈퇴 성공 테스트:
     * 카카오로 가입한 유저의 탈퇴 요청을 보내고,
     * - (1) 응답 status가 204 No Content인지 확인
     * - (2) refresh token cookie가 삭제되는지 확인
     * - (3) 유저 soft delete 처리가 제대로 되었는지 확인 (is_deleted = true, nickname에 "d_" prefix 추가)
     * - (4) social_accounts, social_oauth_tokens, refresh_tokens,
     *       register_sessions, user_hogu_stats 테이블에서 유저 정보가 모두 hard delete 되었는지 확인
     * - (5) access token으로 unlink API가 호출되었는지 확인
     * - (6) access token 재발급 API가 호출되지 않았는지 확인
     */
    @Test
    void deleteUserReturns204AndDeletesKakaoUserData() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        insertUser(TEST_USER_ID, "nickname", false, now);
        insertUserHoguStat(TEST_USER_ID, now);
        insertSocialAccount(TEST_SOCIAL_ACCOUNT_ID, TEST_USER_ID, "KAKAO", "kakao-provider-id", now);
        insertSocialOAuthToken(TEST_SOCIAL_OAUTH_TOKEN_ID, TEST_SOCIAL_ACCOUNT_ID, "kakao-access-token", "kakao-refresh-token", now);
        insertRefreshToken(TEST_REFRESH_TOKEN_ID, TEST_USER_ID, "local-refresh-token", now);
        insertRegisterSession(TEST_REGISTER_SESSION_ID, TEST_SOCIAL_ACCOUNT_ID, "register-token", now);

        stubAuthenticatedUser();
        mockMvc.perform(delete("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-access-token"))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, Matchers.containsString("refreshToken=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, Matchers.containsString("Max-Age=0")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, Matchers.containsString("Path=/")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, Matchers.containsString("Secure")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, Matchers.containsString("HttpOnly")));
        Boolean isDeleted = jdbcTemplate.queryForObject(
                "SELECT is_deleted FROM users WHERE id = ?",
                Boolean.class,
                TEST_USER_ID
        );
        String deletedNickname = jdbcTemplate.queryForObject(
                "SELECT nickname FROM users WHERE id = ?",
                String.class,
                TEST_USER_ID
        );
        Integer socialAccountCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM social_accounts WHERE id = ?",
                Integer.class,
                TEST_SOCIAL_ACCOUNT_ID
        );
        Integer socialTokenCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM social_oauth_tokens WHERE social_account_id = ?",
                Integer.class,
                TEST_SOCIAL_ACCOUNT_ID
        );
        Integer refreshTokenCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refresh_tokens WHERE user_id = ?",
                Integer.class,
                TEST_USER_ID
        );
        Integer registerSessionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM register_sessions WHERE social_account_id = ?",
                Integer.class,
                TEST_SOCIAL_ACCOUNT_ID
        );
        Integer userHoguStatCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_hogu_stats WHERE user_id = ?",
                Integer.class,
                TEST_USER_ID
        );

        assertThat(isDeleted).isTrue();
        assertThat(deletedNickname).startsWith("d_");
        assertThat(socialAccountCount).isZero();
        assertThat(socialTokenCount).isZero();
        assertThat(refreshTokenCount).isZero();
        assertThat(registerSessionCount).isZero();
        assertThat(userHoguStatCount).isZero();

        verify(oauthClient).unlinkKakao("kakao-access-token");
        verify(oauthClient, never()).reissueKakaoToken(anyString());
    }

    /**
     * 구글 사용자 탈퇴 실패 테스트:
     * 구글로 가입한 유저의 탈퇴 요청을 보내고,
     * - (1) refresh token revoke 실패 후 access token revoke도 실패하면 응답 status가 502 Bad Gateway인지 확인
     * - (2) users 테이블의 soft delete 정보가 변경되지 않았는지 확인
     * - (3) social_accounts, social_oauth_tokens, refresh_tokens,
     *       register_sessions, user_hogu_stats 테이블의 데이터가 그대로 유지되는지 확인
     * - (4) refresh token과 access token을 통해 revoke API가 각각 호출되었는지 확인
     */
    @Test
    void deleteUserDoesNotChangeDatabaseWhenGoogleUnlinkFails() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        insertUser(TEST_USER_ID, "nickname", false, now);
        insertUserHoguStat(TEST_USER_ID, now);
        insertSocialAccount(TEST_SOCIAL_ACCOUNT_ID, TEST_USER_ID, "GOOGLE", "google-provider-id", now);
        insertSocialOAuthToken(TEST_SOCIAL_OAUTH_TOKEN_ID, TEST_SOCIAL_ACCOUNT_ID, "google-access-token", "google-refresh-token", now);
        insertRefreshToken(TEST_REFRESH_TOKEN_ID, TEST_USER_ID, "local-refresh-token", now);
        insertRegisterSession(TEST_REGISTER_SESSION_ID, TEST_SOCIAL_ACCOUNT_ID, "register-token", now);

        stubAuthenticatedUser();
        stubGoogleUnlinkFailure();

        mockMvc.perform(delete("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-access-token"))
                .andExpect(status().isBadGateway());

        Boolean isDeleted = jdbcTemplate.queryForObject(
                "SELECT is_deleted FROM users WHERE id = ?",
                Boolean.class,
                TEST_USER_ID
        );
        String nickname = jdbcTemplate.queryForObject(
                "SELECT nickname FROM users WHERE id = ?",
                String.class,
                TEST_USER_ID
        );
        Integer socialAccountCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM social_accounts WHERE id = ?",
                Integer.class,
                TEST_SOCIAL_ACCOUNT_ID
        );
        Integer socialTokenCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM social_oauth_tokens WHERE social_account_id = ?",
                Integer.class,
                TEST_SOCIAL_ACCOUNT_ID
        );
        Integer refreshTokenCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refresh_tokens WHERE user_id = ?",
                Integer.class,
                TEST_USER_ID
        );
        Integer registerSessionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM register_sessions WHERE social_account_id = ?",
                Integer.class,
                TEST_SOCIAL_ACCOUNT_ID
        );
        Integer userHoguStatCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_hogu_stats WHERE user_id = ?",
                Integer.class,
                TEST_USER_ID
        );

        assertThat(isDeleted).isEqualTo(false);
        assertThat(nickname).isEqualTo("nickname");
        assertThat(socialAccountCount).isEqualTo(1);
        assertThat(socialTokenCount).isEqualTo(1);
        assertThat(refreshTokenCount).isEqualTo(1);
        assertThat(registerSessionCount).isEqualTo(1);
        assertThat(userHoguStatCount).isEqualTo(1);

        verify(oauthClient).revokeGoogleToken("google-refresh-token");
        verify(oauthClient).revokeGoogleToken("google-access-token");
    }

    /**
     * 구글 사용자 탈퇴 fallback 성공 테스트:
     * 구글로 가입한 유저의 탈퇴 요청을 보내고,
     * - (1) refresh token revoke가 실패하면 access token revoke를 재시도하는지 확인
     * - (2) access token revoke가 성공하면 응답 status가 204 No Content인지 확인
     * - (3) 유저 soft delete 처리가 제대로 되었는지 확인 (is_deleted = true, nickname에 "d_" prefix 추가)
     * - (4) social_accounts, social_oauth_tokens, refresh_tokens,
     *       register_sessions, user_hogu_stats 테이블에서 유저 정보가 모두 hard delete 되었는지 확인
     * - (5) refresh token과 access token을 통해 revoke API가 각각 호출되었는지 확인
     */
    @Test
    void deleteUserRevokesGoogleAccessTokenWhenGoogleRefreshTokenRevokeFails() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        insertUser(TEST_USER_ID, "nickname", false, now);
        insertUserHoguStat(TEST_USER_ID, now);
        insertSocialAccount(TEST_SOCIAL_ACCOUNT_ID, TEST_USER_ID, "GOOGLE", "google-provider-id", now);
        insertSocialOAuthToken(TEST_SOCIAL_OAUTH_TOKEN_ID, TEST_SOCIAL_ACCOUNT_ID, "google-access-token", "google-refresh-token", now);
        insertRefreshToken(TEST_REFRESH_TOKEN_ID, TEST_USER_ID, "local-refresh-token", now);
        insertRegisterSession(TEST_REGISTER_SESSION_ID, TEST_SOCIAL_ACCOUNT_ID, "register-token", now);

        stubAuthenticatedUser();
        doThrow(new CustomException(OAuthErrorCode.SOCIAL_SERVER_ERROR))
                .doNothing()
                .when(oauthClient)
                .revokeGoogleToken("google-refresh-token");

        mockMvc.perform(delete("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-access-token"))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, Matchers.containsString("refreshToken=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, Matchers.containsString("Max-Age=0")));

        Boolean isDeleted = jdbcTemplate.queryForObject(
                "SELECT is_deleted FROM users WHERE id = ?",
                Boolean.class,
                TEST_USER_ID
        );
        String deletedNickname = jdbcTemplate.queryForObject(
                "SELECT nickname FROM users WHERE id = ?",
                String.class,
                TEST_USER_ID
        );
        Integer socialAccountCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM social_accounts WHERE id = ?",
                Integer.class,
                TEST_SOCIAL_ACCOUNT_ID
        );
        Integer socialTokenCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM social_oauth_tokens WHERE social_account_id = ?",
                Integer.class,
                TEST_SOCIAL_ACCOUNT_ID
        );
        Integer refreshTokenCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refresh_tokens WHERE user_id = ?",
                Integer.class,
                TEST_USER_ID
        );
        Integer registerSessionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM register_sessions WHERE social_account_id = ?",
                Integer.class,
                TEST_SOCIAL_ACCOUNT_ID
        );
        Integer userHoguStatCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_hogu_stats WHERE user_id = ?",
                Integer.class,
                TEST_USER_ID
        );

        assertThat(isDeleted).isTrue();
        assertThat(deletedNickname).startsWith("d_");
        assertThat(socialAccountCount).isZero();
        assertThat(socialTokenCount).isZero();
        assertThat(refreshTokenCount).isZero();
        assertThat(registerSessionCount).isZero();
        assertThat(userHoguStatCount).isZero();

        verify(oauthClient).revokeGoogleToken("google-refresh-token");
        verify(oauthClient).revokeGoogleToken("google-access-token");
    }

    /**
     * 구글 사용자 탈퇴 실패 테스트:
     * 구글로 가입한 유저의 탈퇴 요청을 보내고,
     * - (1) refresh token revoke에 실패하고 access token도 이미 만료된 경우 응답 status가 502 Bad Gateway인지 확인
     * - (2) users 테이블의 soft delete 정보가 변경되지 않았는지 확인
     * - (3) social_accounts, social_oauth_tokens, refresh_tokens,
     *       register_sessions, user_hogu_stats 테이블의 데이터가 그대로 유지되는지 확인
     * - (4) access token revoke API가 호출되지 않았는지 확인
     */
    @Test
    void deleteUserDoesNotChangeDatabaseWhenGoogleRefreshTokenRevokeFailsAndAccessTokenIsExpired() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        insertUser(TEST_USER_ID, "nickname", false, now);
        insertUserHoguStat(TEST_USER_ID, now);
        insertSocialAccount(TEST_SOCIAL_ACCOUNT_ID, TEST_USER_ID, "GOOGLE", "google-provider-id", now);
        insertSocialOAuthToken(
                TEST_SOCIAL_OAUTH_TOKEN_ID,
                TEST_SOCIAL_ACCOUNT_ID,
                "google-access-token",
                "google-refresh-token",
                now.minusHours(2),
                now.plusDays(30),
                now
        );
        insertRefreshToken(TEST_REFRESH_TOKEN_ID, TEST_USER_ID, "local-refresh-token", now);
        insertRegisterSession(TEST_REGISTER_SESSION_ID, TEST_SOCIAL_ACCOUNT_ID, "register-token", now);

        stubAuthenticatedUser();
        doThrow(new CustomException(OAuthErrorCode.SOCIAL_SERVER_ERROR))
                .doNothing()
                .when(oauthClient)
                .revokeGoogleToken("google-refresh-token");

        mockMvc.perform(delete("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-access-token"))
                .andExpect(status().isBadGateway());

        Boolean isDeleted = jdbcTemplate.queryForObject(
                "SELECT is_deleted FROM users WHERE id = ?",
                Boolean.class,
                TEST_USER_ID
        );
        String nickname = jdbcTemplate.queryForObject(
                "SELECT nickname FROM users WHERE id = ?",
                String.class,
                TEST_USER_ID
        );
        Integer socialAccountCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM social_accounts WHERE id = ?",
                Integer.class,
                TEST_SOCIAL_ACCOUNT_ID
        );
        Integer socialTokenCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM social_oauth_tokens WHERE social_account_id = ?",
                Integer.class,
                TEST_SOCIAL_ACCOUNT_ID
        );
        Integer refreshTokenCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refresh_tokens WHERE user_id = ?",
                Integer.class,
                TEST_USER_ID
        );
        Integer registerSessionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM register_sessions WHERE social_account_id = ?",
                Integer.class,
                TEST_SOCIAL_ACCOUNT_ID
        );
        Integer userHoguStatCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_hogu_stats WHERE user_id = ?",
                Integer.class,
                TEST_USER_ID
        );

        assertThat(isDeleted).isEqualTo(false);
        assertThat(nickname).isEqualTo("nickname");
        assertThat(socialAccountCount).isEqualTo(1);
        assertThat(socialTokenCount).isEqualTo(1);
        assertThat(refreshTokenCount).isEqualTo(1);
        assertThat(registerSessionCount).isEqualTo(1);
        assertThat(userHoguStatCount).isEqualTo(1);

        verify(oauthClient, never()).revokeGoogleToken("google-access-token");
    }

    /**
     * 카카오 사용자 탈퇴 재시도 성공 테스트:
     * 카카오로 가입한 유저의 탈퇴 요청을 보내고,
     * - (1) 저장된 access token이 아직 만료되지 않았더라도 실제 unlink API 호출 시점에는 만료되었을 수 있으므로,
     *       첫 unlink API 호출이 실패하면 access token 재발급을 시도하는지 확인
     * - (2) 재발급된 access token으로 unlink API를 다시 호출하는지 확인
     * - (3) 응답 status가 204 No Content인지 확인
     * - (4) refresh token cookie가 삭제되는지 확인
     * - (5) 유저 soft delete 처리가 제대로 되었는지 확인 (is_deleted = true, nickname에 "d_" prefix 추가)
     * - (6) social_accounts, social_oauth_tokens, refresh_tokens,
     *       register_sessions, user_hogu_stats 테이블에서 유저 정보가 모두 hard delete 되었는지 확인
     */
    @Test
    void deleteUserRetriesKakaoUnlinkAfterTokenReissue() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        insertUser(TEST_USER_ID, "nickname", false, now);
        insertUserHoguStat(TEST_USER_ID, now);
        insertSocialAccount(TEST_SOCIAL_ACCOUNT_ID, TEST_USER_ID, "KAKAO", "kakao-provider-id", now);
        insertSocialOAuthToken(TEST_SOCIAL_OAUTH_TOKEN_ID, TEST_SOCIAL_ACCOUNT_ID, "kakao-access-token", "kakao-refresh-token", now);
        insertRefreshToken(TEST_REFRESH_TOKEN_ID, TEST_USER_ID, "local-refresh-token", now);
        insertRegisterSession(TEST_REGISTER_SESSION_ID, TEST_SOCIAL_ACCOUNT_ID, "register-token", now);

        stubAuthenticatedUser();
        stubKakaoUnlinkRequiresReissue();
        stubKakaoTokenReissueSuccess("reissued-kakao-access-token");

        mockMvc.perform(delete("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-access-token"))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, Matchers.containsString("refreshToken=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, Matchers.containsString("Max-Age=0")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, Matchers.containsString("Path=/")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, Matchers.containsString("Secure")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, Matchers.containsString("HttpOnly")));

        Boolean isDeleted = jdbcTemplate.queryForObject(
                "SELECT is_deleted FROM users WHERE id = ?",
                Boolean.class,
                TEST_USER_ID
        );
        String nickname = jdbcTemplate.queryForObject(
                "SELECT nickname FROM users WHERE id = ?",
                String.class,
                TEST_USER_ID
        );
        Integer socialAccountCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM social_accounts WHERE id = ?",
                Integer.class,
                TEST_SOCIAL_ACCOUNT_ID
        );
        Integer socialTokenCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM social_oauth_tokens WHERE social_account_id = ?",
                Integer.class,
                TEST_SOCIAL_ACCOUNT_ID
        );
        Integer refreshTokenCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refresh_tokens WHERE user_id = ?",
                Integer.class,
                TEST_USER_ID
        );
        Integer registerSessionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM register_sessions WHERE social_account_id = ?",
                Integer.class,
                TEST_SOCIAL_ACCOUNT_ID
        );
        Integer userHoguStatCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_hogu_stats WHERE user_id = ?",
                Integer.class,
                TEST_USER_ID
        );

        assertThat(isDeleted).isEqualTo(true);
        assertThat(nickname).startsWith("d_");
        assertThat(socialAccountCount).isZero();
        assertThat(socialTokenCount).isZero();
        assertThat(refreshTokenCount).isZero();
        assertThat(registerSessionCount).isZero();
        assertThat(userHoguStatCount).isZero();

        verify(oauthClient, times(2)).unlinkKakao(anyString());
        verify(oauthClient).reissueKakaoToken("kakao-refresh-token");
    }

    /**
     * 카카오 사용자 탈퇴 선분기 성공 테스트:
     * 카카오로 가입한 유저의 탈퇴 요청을 보내고,
     * - (1) 저장된 access token이 이미 만료된 경우 첫 unlink 없이 access token 재발급을 시도하는지 확인
     * - (2) 재발급된 access token으로 unlink API를 호출하는지 확인
     * - (3) 응답 status가 204 No Content인지 확인
     * - (4) 유저 soft delete 처리가 제대로 되었는지 확인 (is_deleted = true, nickname에 "d_" prefix 추가)
     * - (5) social_accounts, social_oauth_tokens, refresh_tokens,
     *       register_sessions, user_hogu_stats 테이블에서 유저 정보가 모두 hard delete 되었는지 확인
     */
    @Test
    void deleteUserReissuesKakaoTokenWhenStoredAccessTokenIsExpired() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        insertUser(TEST_USER_ID, "nickname", false, now);
        insertUserHoguStat(TEST_USER_ID, now);
        insertSocialAccount(TEST_SOCIAL_ACCOUNT_ID, TEST_USER_ID, "KAKAO", "kakao-provider-id", now);
        insertSocialOAuthToken(
                TEST_SOCIAL_OAUTH_TOKEN_ID,
                TEST_SOCIAL_ACCOUNT_ID,
                "expired-kakao-access-token",
                "kakao-refresh-token",
                now.minusHours(2),
                now.plusDays(30),
                now
        );
        insertRefreshToken(TEST_REFRESH_TOKEN_ID, TEST_USER_ID, "local-refresh-token", now);
        insertRegisterSession(TEST_REGISTER_SESSION_ID, TEST_SOCIAL_ACCOUNT_ID, "register-token", now);

        stubAuthenticatedUser();
        stubKakaoTokenReissueSuccess("reissued-kakao-access-token");

        mockMvc.perform(delete("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-access-token"))
                .andExpect(status().isNoContent());

        Boolean isDeleted = jdbcTemplate.queryForObject(
                "SELECT is_deleted FROM users WHERE id = ?",
                Boolean.class,
                TEST_USER_ID
        );
        String deletedNickname = jdbcTemplate.queryForObject(
                "SELECT nickname FROM users WHERE id = ?",
                String.class,
                TEST_USER_ID
        );
        Integer socialAccountCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM social_accounts WHERE id = ?",
                Integer.class,
                TEST_SOCIAL_ACCOUNT_ID
        );
        Integer socialTokenCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM social_oauth_tokens WHERE social_account_id = ?",
                Integer.class,
                TEST_SOCIAL_ACCOUNT_ID
        );
        Integer refreshTokenCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refresh_tokens WHERE user_id = ?",
                Integer.class,
                TEST_USER_ID
        );
        Integer registerSessionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM register_sessions WHERE social_account_id = ?",
                Integer.class,
                TEST_SOCIAL_ACCOUNT_ID
        );
        Integer userHoguStatCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_hogu_stats WHERE user_id = ?",
                Integer.class,
                TEST_USER_ID
        );

        assertThat(isDeleted).isTrue();
        assertThat(deletedNickname).startsWith("d_");
        assertThat(socialAccountCount).isZero();
        assertThat(socialTokenCount).isZero();
        assertThat(refreshTokenCount).isZero();
        assertThat(registerSessionCount).isZero();
        assertThat(userHoguStatCount).isZero();

        verify(oauthClient, never()).unlinkKakao("expired-kakao-access-token");
        verify(oauthClient).reissueKakaoToken("kakao-refresh-token");
        verify(oauthClient).unlinkKakao("reissued-kakao-access-token");
    }

    /**
     * 카카오 사용자 탈퇴 재발급 실패 테스트:
     * 카카오로 가입한 유저의 탈퇴 요청을 보내고,
     * - (1) 첫 unlink API 호출이 실패하면 access token 재발급을 시도하는지 확인
     * - (2) access token 재발급에도 실패하면 응답 status가 502 Bad Gateway인지 확인
     * - (3) users 테이블의 soft delete 정보가 변경되지 않았는지 확인
     * - (4) social_accounts, social_oauth_tokens, refresh_tokens,
     *       register_sessions, user_hogu_stats 테이블의 데이터가 그대로 유지되는지 확인
     * - (5) unlink API와 access token 재발급 API가 각각 호출되었는지 확인
     */
    @Test
    void deleteUserDoesNotChangeDatabaseWhenKakaoTokenReissueFails() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        insertUser(TEST_USER_ID, "nickname", false, now);
        insertUserHoguStat(TEST_USER_ID, now);
        insertSocialAccount(TEST_SOCIAL_ACCOUNT_ID, TEST_USER_ID, "KAKAO", "kakao-provider-id", now);
        insertSocialOAuthToken(TEST_SOCIAL_OAUTH_TOKEN_ID, TEST_SOCIAL_ACCOUNT_ID, "kakao-access-token", "kakao-refresh-token", now);
        insertRefreshToken(TEST_REFRESH_TOKEN_ID, TEST_USER_ID, "local-refresh-token", now);
        insertRegisterSession(TEST_REGISTER_SESSION_ID, TEST_SOCIAL_ACCOUNT_ID, "register-token", now);

        stubAuthenticatedUser();
        doThrow(new CustomException(OAuthErrorCode.SOCIAL_SERVER_ERROR))
                .when(oauthClient)
                .unlinkKakao(anyString());
        stubKakaoTokenReissueFailure();

        mockMvc.perform(delete("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-access-token"))
                .andExpect(status().isBadGateway());

        Boolean isDeleted = jdbcTemplate.queryForObject(
                "SELECT is_deleted FROM users WHERE id = ?",
                Boolean.class,
                TEST_USER_ID
        );
        String nickname = jdbcTemplate.queryForObject(
                "SELECT nickname FROM users WHERE id = ?",
                String.class,
                TEST_USER_ID
        );
        Integer socialAccountCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM social_accounts WHERE id = ?",
                Integer.class,
                TEST_SOCIAL_ACCOUNT_ID
        );
        Integer socialTokenCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM social_oauth_tokens WHERE social_account_id = ?",
                Integer.class,
                TEST_SOCIAL_ACCOUNT_ID
        );
        Integer refreshTokenCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refresh_tokens WHERE user_id = ?",
                Integer.class,
                TEST_USER_ID
        );
        Integer registerSessionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM register_sessions WHERE social_account_id = ?",
                Integer.class,
                TEST_SOCIAL_ACCOUNT_ID
        );
        Integer userHoguStatCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_hogu_stats WHERE user_id = ?",
                Integer.class,
                TEST_USER_ID
        );

        assertThat(isDeleted).isEqualTo(false);
        assertThat(nickname).isEqualTo("nickname");
        assertThat(socialAccountCount).isEqualTo(1);
        assertThat(socialTokenCount).isEqualTo(1);
        assertThat(refreshTokenCount).isEqualTo(1);
        assertThat(registerSessionCount).isEqualTo(1);
        assertThat(userHoguStatCount).isEqualTo(1);

        verify(oauthClient).unlinkKakao("kakao-access-token");
        verify(oauthClient).reissueKakaoToken("kakao-refresh-token");
    }

    private void insertUser(Long userId, String nickname, boolean isDeleted, LocalDateTime now) {
        jdbcTemplate.update(
                """
                INSERT INTO users
                    (id, nickname, profile_image_url, is_deleted, deleted_at, created_at, updated_at)
                VALUES
                    (?, ?, ?, ?, ?, ?, ?)
                """,
                userId,
                nickname,
                null,
                isDeleted,
                null,
                now,
                now
        );
    }

    private void insertUserHoguStat(Long userId, LocalDateTime now) {
        jdbcTemplate.update(
                """
                INSERT INTO user_hogu_stats
                    (user_id, hogu_vote_count, total_vote_count, hogu_index, updated_at)
                VALUES
                    (?, 0, 0, 0, ?)
                """,
                userId,
                now
        );
    }

    private void insertSocialAccount(
            Long socialAccountId,
            Long userId,
            String provider,
            String providerUserId,
            LocalDateTime now
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
                now,
                now
        );
    }

    private void insertSocialOAuthToken(
            Long socialOAuthTokenId,
            Long socialAccountId,
            String accessToken,
            String refreshToken,
            LocalDateTime now
    ) {
        insertSocialOAuthToken(
                socialOAuthTokenId,
                socialAccountId,
                accessToken,
                refreshToken,
                now.plusHours(1),
                now.plusDays(30),
                now
        );
    }

    private void insertSocialOAuthToken(
            Long socialOAuthTokenId,
            Long socialAccountId,
            String accessToken,
            String refreshToken,
            LocalDateTime accessTokenExpiresAt,
            LocalDateTime refreshTokenExpiresAt,
            LocalDateTime now
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO social_oauth_tokens
                    (id, social_account_id, access_token_encrypted, refresh_token_encrypted,
                     access_token_expires_at, refresh_token_expires_at, created_at, updated_at)
                VALUES
                    (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                socialOAuthTokenId,
                socialAccountId,
                tokenEncryptor.encrypt(accessToken),
                tokenEncryptor.encrypt(refreshToken),
                accessTokenExpiresAt,
                refreshTokenExpiresAt,
                now,
                now
        );
    }

    private void insertRefreshToken(
            Long refreshTokenId,
            Long userId,
            String rawRefreshToken,
            LocalDateTime now
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO refresh_tokens
                    (id, user_id, token_hash, is_revoked, is_rotated, expires_at, revoked_at, created_at)
                VALUES
                    (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                refreshTokenId,
                userId,
                tokenHasher.hash(rawRefreshToken),
                false,
                false,
                now.plusDays(7),
                null,
                now
        );
    }

    private void insertRegisterSession(
            Long registerSessionId,
            Long socialAccountId,
            String registerToken,
            LocalDateTime now
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
                now.plusMinutes(10),
                null,
                now
        );
    }

    private void insertOAuthLoginState(
            Long id,
            OAuthProvider provider,
            String state,
            String nonce,
            LocalDateTime expiresAt,
            LocalDateTime consumedAt
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO oauth_login_states
                    (id, provider, state, nonce, expires_at, consumed_at, created_at)
                VALUES
                    (?, ?, ?, ?, ?, ?, ?)
                """,
                id,
                provider.name(),
                state,
                nonce,
                expiresAt,
                consumedAt,
                LocalDateTime.now()
        );
    }

    private void insertSocialAccount(
            Long socialAccountId,
            Long userId,
            OAuthProvider provider,
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
                provider.name(),
                providerUserId,
                linkedAt,
                createdAt
        );
    }

    private TokenResponse createTokenResponse(
            String accessToken,
            Integer expiresIn,
            String refreshToken,
            Integer refreshTokenExpiresIn
    ) {
        TokenResponse tokenResponse = mock(TokenResponse.class);
        when(tokenResponse.getAccessToken())
                .thenReturn(accessToken);
        when(tokenResponse.getExpiresIn())
                .thenReturn(expiresIn);
        when(tokenResponse.getRefreshToken())
                .thenReturn(refreshToken);
        when(tokenResponse.getRefreshTokenExpiresIn())
                .thenReturn(refreshTokenExpiresIn);

        return tokenResponse;
    }

    private OAuthAuthenticationResult createAuthResult(
            OAuthProvider provider,
            String providerUserId,
            TokenResponse tokenResponse
    ) {
        return new OAuthAuthenticationResult(
                new OAuthUserInfo(provider, providerUserId),
                tokenResponse
        );
    }

    private void stubAuthenticatedUser() {
        when(jwtProvider.validateAccessToken("valid-access-token"))
                .thenReturn(JwtProvider.TokenValidationResult.VALID);
        when(jwtProvider.getSubjectAsLong("valid-access-token"))
                .thenReturn(TEST_USER_ID);
        when(jwtProvider.getAuthentication("valid-access-token"))
                .thenReturn(new UsernamePasswordAuthenticationToken(
                        String.valueOf(TEST_USER_ID),
                        null,
                        Collections.emptyList()
                ));
    }

    private void stubGoogleUnlinkFailure() {
        doThrow(new CustomException(OAuthErrorCode.SOCIAL_SERVER_ERROR))
                .when(oauthClient)
                .revokeGoogleToken(anyString());
    }

    private void stubKakaoUnlinkRequiresReissue() {
        doThrow(new CustomException(OAuthErrorCode.SOCIAL_SERVER_ERROR))
                .doNothing()
                .when(oauthClient)
                .unlinkKakao(anyString());
    }

    private void stubKakaoTokenReissueSuccess(String reissuedAccessToken) {
        TokenResponse tokenResponse = org.mockito.Mockito.mock(TokenResponse.class);
        when(tokenResponse.getAccessToken()).thenReturn(reissuedAccessToken);
        when(oauthClient.reissueKakaoToken(anyString())).thenReturn(tokenResponse);
    }

    private void stubKakaoTokenReissueFailure() {
        when(oauthClient.reissueKakaoToken(anyString()))
                .thenThrow(new CustomException(OAuthErrorCode.SOCIAL_SERVER_ERROR));
    }
}
