package com.hogu.am_i_hogu.domain.oauth;

import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.common.security.JwtProvider;
import com.hogu.am_i_hogu.common.security.TokenEncryptor;
import com.hogu.am_i_hogu.common.security.TokenHasher;
import com.hogu.am_i_hogu.domain.oauth.dto.response.TokenResponse;
import com.hogu.am_i_hogu.domain.oauth.exception.OAuthErrorCode;
import com.hogu.am_i_hogu.domain.oauth.service.OAuthClient;
import org.hamcrest.Matchers;
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
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
public class OAuthControllerTest {

    private static final long TEST_USER_ID = 1L;
    private static final long TEST_SOCIAL_ACCOUNT_ID = 100L;
    private static final long TEST_SOCIAL_OAUTH_TOKEN_ID = 101L;
    private static final long TEST_REFRESH_TOKEN_ID = 102L;
    private static final long TEST_REGISTER_SESSIONS_ID = 103L;

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
        insertRegisterSession(TEST_REGISTER_SESSIONS_ID, TEST_SOCIAL_ACCOUNT_ID, "register-token", now);

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
        insertRegisterSession(TEST_REGISTER_SESSIONS_ID, TEST_SOCIAL_ACCOUNT_ID, "register-token", now);

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
     * - (1) revoke API 호출에 실패하면 응답 status가 502 Bad Gateway인지 확인
     * - (2) users 테이블의 soft delete 정보가 변경되지 않았는지 확인
     * - (3) social_accounts, social_oauth_tokens, refresh_tokens,
     *       register_sessions, user_hogu_stats 테이블의 데이터가 그대로 유지되는지 확인
     * - (4) refresh token을 통해 revoke API가 호출되었는지 확인
     */
    @Test
    void deleteUserDoesNotChangeDatabaseWhenGoogleUnlinkFails() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        insertUser(TEST_USER_ID, "nickname", false, now);
        insertUserHoguStat(TEST_USER_ID, now);
        insertSocialAccount(TEST_SOCIAL_ACCOUNT_ID, TEST_USER_ID, "GOOGLE", "google-provider-id", now);
        insertSocialOAuthToken(TEST_SOCIAL_OAUTH_TOKEN_ID, TEST_SOCIAL_ACCOUNT_ID, "google-access-token", "google-refresh-token", now);
        insertRefreshToken(TEST_REFRESH_TOKEN_ID, TEST_USER_ID, "local-refresh-token", now);
        insertRegisterSession(TEST_REGISTER_SESSIONS_ID, TEST_SOCIAL_ACCOUNT_ID, "register-token", now);

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
    }

    /**
     * 카카오 사용자 탈퇴 재시도 성공 테스트:
     * 카카오로 가입한 유저의 탈퇴 요청을 보내고,
     * - (1) 첫 unlink API 호출이 실패하면 access token 재발급을 시도하는지 확인
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
        insertRegisterSession(TEST_REGISTER_SESSIONS_ID, TEST_SOCIAL_ACCOUNT_ID, "register-token", now);

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
        insertRegisterSession(TEST_REGISTER_SESSIONS_ID, TEST_SOCIAL_ACCOUNT_ID, "register-token", now);

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
                now.plusHours(1),
                now.plusDays(30),
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

    private void stubAuthenticatedUser() {
        when(jwtProvider.validateAccessToken("valid-access-token"))
                .thenReturn(JwtProvider.TokenValidationResult.VALID);
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
