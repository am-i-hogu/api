package com.hogu.am_i_hogu.domain.user.controller;

import com.hogu.am_i_hogu.common.security.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
import org.hamcrest.Matchers;
import com.jayway.jsonpath.JsonPath;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
public class UserControllerTest {

    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("am_i_hogu_user_test_db")
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
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        jdbcTemplate.update("DELETE FROM user_hogu_stats");
        jdbcTemplate.update("DELETE FROM post_bookmarks");
        jdbcTemplate.update("DELETE FROM post_votes");
        jdbcTemplate.update("DELETE FROM comments");
        jdbcTemplate.update("DELETE FROM posts");
        jdbcTemplate.update("DELETE FROM users");
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
    }

    /**
     * 닉네임 업데이트 성공 테스트:
     * profileImageUrl 없이 nickname 필드만 포함해 요청을 보내고,
     * - (1) 응답 status가 200 OK인지 확인
     * - (2) 업데이트 된 프로필 정보를 반환하는지 확인
     * - (3) users 테이블에 nickname 정보가 업데이트 되었는지 확인
     * - (4) users 테이블에 profileImageUrl이 변경되지 않았는지 확인
     */
    @Test
    void updateProfileReturns200WhenNicknameIsUpdated() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "oldNickname", null);

        String requestBody = """
                {
                    "nickname" : "newNickname"
                }
                """;
        mockMvc.perform(patch("/api/users/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nickname").value("newNickname"))
                .andExpect(jsonPath("$.profileImageUrl").value(Matchers.nullValue()));

        String savedNickname = jdbcTemplate.queryForObject(
                "SELECT nickname FROM users WHERE id = ?",
                String.class,
                1L
        );
        assertThat(savedNickname).isEqualTo("newNickname");

        String savedProfileImageUrl = jdbcTemplate.queryForObject(
                "SELECT profile_image_url FROM users WHERE id = ?",
                String.class,
                1L
        );
        assertThat(savedProfileImageUrl).isNull();
    }

    /**
     * 프로필 사진 업데이트 성공 테스트:
     * nickname 없이 profileImageUrl 필드만 포함해 요청을 보내고,
     * - (1) 응답 status가 200 OK인지 확인
     * - (2) 업데이트 된 프로필 정보를 반환하는지 확인
     * - (3) users 테이블에 profileImageUrl 정보가 업데이트 되었는지 확인
     * - (4) users 테이블에 nickname이 변경되지 않았는지 확인
     */
    @Test
    void updateProfileReturns200WhenProfileImageIsUpdated() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "nickname", null);

        String requestBody = """
                {
                    "profileImageUrl" : "http://localhost:8080/temporary/images/1/profile-image.jpg"
                }
                """;
        mockMvc.perform(patch("/api/users/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nickname").value("nickname"))
                .andExpect(jsonPath("$.profileImageUrl").value("http://localhost:8080/temporary/images/1/profile-image.jpg"));

        String savedNickname = jdbcTemplate.queryForObject(
                "SELECT nickname FROM users WHERE id = ?",
                String.class,
                1L
        );
        assertThat(savedNickname).isEqualTo("nickname");

        String savedProfileImageUrl = jdbcTemplate.queryForObject(
                "SELECT profile_image_url FROM users WHERE id = ?",
                String.class,
                1L
        );
        assertThat(savedProfileImageUrl).isEqualTo("http://localhost:8080/temporary/images/1/profile-image.jpg");
    }

    /**
     * 프로필 업데이트 성공 테스트:
     * nickname, profileImageUrl 필드를 포함해 요청을 보내고,
     * - (1) 응답 status가 200 OK인지 확인
     * - (2) 업데이트 된 프로필 정보를 반환하는지 확인
     * - (3) users 테이블에 nickname, profileImageUrl 정보가 업데이트 되었는지 확인
     */
    @Test
    void updateProfileReturns200WhenNicknameAndProfileImageAreUpdated() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "oldNickname", null);

        String requestBody = """
                {
                    "nickname" : "newNickname",
                    "profileImageUrl" : "http://localhost:8080/temporary/images/1/profile-image.jpg"
                }
                """;

        mockMvc.perform(patch("/api/users/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nickname").value("newNickname"))
                .andExpect(jsonPath("$.profileImageUrl").value("http://localhost:8080/temporary/images/1/profile-image.jpg"));

        String savedNickname = jdbcTemplate.queryForObject(
                "SELECT nickname FROM users WHERE id = ?",
                String.class,
                1L
        );
        assertThat(savedNickname).isEqualTo("newNickname");

        String savedProfileImageUrl = jdbcTemplate.queryForObject(
                "SELECT profile_image_url FROM users WHERE id = ?",
                String.class,
                1L
        );
        assertThat(savedProfileImageUrl).isEqualTo("http://localhost:8080/temporary/images/1/profile-image.jpg");
    }

    /**
     * 프로필 사진 삭제 성공 테스트:
     * nickname 필드 없이 profileImageUrl 필드만 포함해 요청을 보내고,
     * - (1) 응답 status가 200 OK인지 확인
     * - (2) 업데이트 된 프로필 정보를 반환하는지 확인
     * - (3) users 테이블에 nickname, profileImageUrl 정보가 업데이트 되었는지 확인
     */
    @Test
    void updateProfileReturns200WhenProfileImageIsDeleted() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "nickname", "http://localhost:8080/temporary/images/1/profile-image.jpg");

        String requestBody = """
                {
                    "profileImageUrl" : null
                }
                """;

        mockMvc.perform(patch("/api/users/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nickname").value("nickname"))
                .andExpect(jsonPath("$.profileImageUrl").value(Matchers.nullValue()));

        String savedNickname = jdbcTemplate.queryForObject(
                "SELECT nickname FROM users WHERE id = ?",
                String.class,
                1L
        );
        assertThat(savedNickname).isEqualTo("nickname");

        String savedProfileImageUrl = jdbcTemplate.queryForObject(
                "SELECT profile_image_url FROM users WHERE id = ?",
                String.class,
                1L
        );
        assertThat(savedProfileImageUrl).isNull();
    }

    /**
     * 프로필 업데이트 실패 테스트:
     * 요청 body 없이 프로필 수정 요청을 보내고,
     * - (1) 응답 status가 400 Bad Request인지 확인
     * - (2) EMPTY_REQUEST_BODY 오류 코드를 반환하는지 확인
     */
    @Test
    void updateProfileReturns400WhenRequestBodyIsEmpty() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "nickname", null);

        mockMvc.perform(patch("/api/users/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EMPTY_REQUEST_BODY"));
    }

    /**
     * 프로필 업데이트 실패 테스트:
     * 빈 객체를 body로 프로필 수정 요청을 보내고,
     * - (1) 응답 status가 400 Bad Request인지 확인
     * - (2) EMPTY_REQUEST_BODY 오류 코드를 반환하는지 확인
     */
    @Test
    void updateProfileReturns400WhenRequestBodyIsEmptyJsonObject() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "nickname", null);

        mockMvc.perform(patch("/api/users/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                            }
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EMPTY_REQUEST_BODY"));
    }

    /**
     * 프로필 업데이트 실패 테스트:
     * 이미 사용 중인 닉네임으로 프로필 수정 요청을 보내고,
     * - (1) 응답 status가 409 Conflict인지 확인
     * - (2) DUPLICATE_NICKNAME 오류 코드를 반환하는지 확인
     */
    @Test
    void updateProfileReturns409WhenNicknameIsDuplicated() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "oldNickname", null);
        insertUser(2L, "duplicatedNickname", null);

        String requestBody = """
                {
                    "nickname" : "duplicatedNickname"
                }
                """;

        mockMvc.perform(patch("/api/users/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_NICKNAME"));
    }

    /**
     * 프로필 업데이트 실패 테스트:
     * 인증된 사용자와 일치하는 user row가 DB에 없을 때 프로필 수정 요청을 보내고,
     * - (1) 응답 status가 401 Unauthorized인지 확인
     * - (2) INVALID_ACCESS_TOKEN 오류 코드를 반환하는지 확인
     */
    @Test
    void updateProfileReturns404WhenUserIsNotFound() throws Exception {
        stubAuthenticatedUser();

        String requestBody = """
                {
                    "nickname" : "newNickname"
                }
                """;

        mockMvc.perform(patch("/api/users/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_ACCESS_TOKEN"));
    }

    /**
     * 닉네임 중복 검사 성공 테스트:
     * 이용 가능한 nickname으로 요청을 보내고,
     * - (1) 응답 status가 200 OK인지 확인
     * - (2) 사용 가능 여부가 true로 반환되는지 확인
     */
    @Test
    void checkNicknameReturns200WhenNicknameIsAvailable() throws Exception {
        when(jwtProvider.validateAccessToken(null))
                .thenReturn(JwtProvider.TokenValidationResult.EMPTY);

        mockMvc.perform(get("/api/users/check-nickname")
                    .param("nickname", "availableNickname"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isAvailable").value(true));
    }

    /**
     * 닉네임 중복 검사 성공 테스트:
     * 이용 불가능한 nickname으로 요청을 보내고,
     * - (1) 응답 status가 200 OK인지 확인
     * - (2) 사용 가능 여부가 false로 반환되는지 확인
     */
    @Test
    void checkNicknameReturns200WhenNicknameIsDuplicated() throws Exception {
        when(jwtProvider.validateAccessToken(null))
                .thenReturn(JwtProvider.TokenValidationResult.EMPTY);

        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
                """
                INSERT INTO users
                    (id, nickname, profile_image_url, is_deleted, deleted_at, created_at, updated_at)
                VALUES
                    (?, ?, ?, ?, ?, ?, ?)
                """,
                1L,
                "duplicatedNickname",
                null,
                false,
                null,
                now,
                now
        );

        mockMvc.perform(get("/api/users/check-nickname")
                    .param("nickname", "duplicatedNickname"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isAvailable").value(false));
    }

    /**
     * 닉네임 중복 검사 실패 테스트:
     * 비어있는 요청으로 보내고,
     * - (1) 응답 status가 400 Bad Request인지 확인
     * - (2) <필드 정보: nickname, 오류 코드: EMPTY_NICKNAME> 반환 확인
     */
    @Test
    void checkNicknameReturns400WhenNicknameParamIsMissing() throws Exception {
        when(jwtProvider.validateAccessToken(null))
                .thenReturn(JwtProvider.TokenValidationResult.EMPTY);

        mockMvc.perform(get("/api/users/check-nickname"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAM_VALUE"))
                .andExpect(jsonPath("$.errors[0].field").value("nickname"))
                .andExpect(jsonPath("$.errors[0].code").value("EMPTY_NICKNAME"));
    }

    /**
     * 닉네임 중복 검사 실패 테스트:
     * 비어있는 nickname을 요청으로 보내고,
     * - (1) 응답 status가 400 Bad Request인지 확인
     * - (2) <필드 정보: nickname, 오류 코드: EMPTY_NICKNAME> 반환 확인
     */
    @Test
    void checkNicknameReturns400WhenNicknameIsEmpty() throws Exception {
        when(jwtProvider.validateAccessToken(null))
                .thenReturn(JwtProvider.TokenValidationResult.EMPTY);

        mockMvc.perform(get("/api/users/check-nickname")
                    .param("nickname", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAM_VALUE"))
                .andExpect(jsonPath("$.errors[0].field").value("nickname"))
                .andExpect(jsonPath("$.errors[0].code").value("EMPTY_NICKNAME"));
    }

    /**
     * 닉네임 중복 검사 실패 테스트:
     * 특수문자를 포함한 nickname을 요청으로 보내고,
     * - (1) 응답 status가 400 Bad Request인지 확인
     * - (2) <필드 정보: nickname, 오류 코드: SPECIAL_CHAR_NICKNAME> 반환 확인
     */
    @Test
    void checkNicknameReturns400WhenNicknameContainsSpecialCharacter() throws Exception {
        when(jwtProvider.validateAccessToken(null))
                .thenReturn(JwtProvider.TokenValidationResult.EMPTY);

        mockMvc.perform(get("/api/users/check-nickname")
                    .param("nickname", "special char nickname"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAM_VALUE"))
                .andExpect(jsonPath("$.errors[0].field").value("nickname"))
                .andExpect(jsonPath("$.errors[0].code").value("SPECIAL_CHAR_NICKNAME"));
    }

    /**
     * 닉네임 중복 검사 실패 테스트:
     * 길이가 20을 초과하는 nickname을 요청으로 보내고,
     * - (1) 응답 status가 400 Bad Request인지 확인
     * - (2) <필드 정보: nickname, 오류 코드: NICKNAME_LENGTH_EXCEEDED> 반환 확인
     */
    @Test
    void checkNicknameReturns400WhenNicknameLengthExceedsLimit() throws Exception {
        when(jwtProvider.validateAccessToken(null))
                .thenReturn(JwtProvider.TokenValidationResult.EMPTY);

        String nickname = "thisisverylongnickname";
        mockMvc.perform(get("/api/users/check-nickname")
                    .param("nickname", nickname))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAM_VALUE"))
                .andExpect(jsonPath("$.errors[0].field").value("nickname"))
                .andExpect(jsonPath("$.errors[0].code").value("NICKNAME_LENGTH_EXCEEDED"));
    }

    /**
     * 닉네임 중복 검사 실패 테스트:
     * 특수문자를 포함하며 길이가 20을 초과하는 nickname을 요청으로 보내고,
     * - (1) 응답 status가 400 Bad Request인지 확인
     * - (2) <필드 정보: nickname, 오류 코드: SPECIAL_CHAR_NICKNAME>,
     *      <필드 정보: nickname, 오류 코드: NICKNAME_LENGTH_EXCEEDED> 반환 확인
     */
    @Test
    void checkNicknameReturns400WhenNicknameContainsSpecialCharacterAndLengthExceedsLimit() throws Exception {
        when(jwtProvider.validateAccessToken(null))
                .thenReturn(JwtProvider.TokenValidationResult.EMPTY);

        String nickname = "this is very long nickname";
        mockMvc.perform(get("/api/users/check-nickname")
                .param("nickname", nickname))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAM_VALUE"))
                .andExpect(jsonPath("$.errors[0].field").value("nickname"))
                .andExpect(jsonPath("$.errors[0].code").value("SPECIAL_CHAR_NICKNAME"))
                .andExpect(jsonPath("$.errors[1].field").value("nickname"))
                .andExpect(jsonPath("$.errors[1].code").value("NICKNAME_LENGTH_EXCEEDED"));
    }

    /**
     * 마이페이지 조회 성공 테스트:
     * 투표 참여된 게시물 수가 5개가 넘는 유저가 조회 요청을 보내고,
     * - (1) 응답 status가 200 OK인지 확인
     * - (2) 유저 프로필 정보, 레벨 정보가 적절히 반환되는지 확인
     */
    @Test
    void getMyPageReturns200WhenVotedPostExceeds5() throws Exception {
        LocalDateTime now = LocalDateTime.now();

        stubAuthenticatedUser();
        insertUser(1L, "nickname", null);
        insertUserHoguStat(1L, 72, 10, now);

        mockMvc.perform(get("/api/users/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("nickname"))
                .andExpect(jsonPath("$.profileImageUrl").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.hoguIndex").value(72))
                .andExpect(jsonPath("$.hoguLevel").value("RISKY"))
                .andExpect(jsonPath("$.hoguShortDescription").value("거절보다 양보가 앞서는 타입"));
    }

    /**
     * 마이페이지 조회 성공 테스트:
     * 투표 참여된 게시물 수가 5개 미만인 유저가 조회 요청을 보내고,
     * - (1) 응답 status가 200 OK인지 확인
     * - (2) 유저 프로필 정보가 적절히 반환되는지 확인
     * - (3) 유저 호구 레벨 및 레벨 설명이 적절히 반환되는지 확인
     */
    @Test
    void getMyPageReturns200WhenVotedPostIsLessThan5() throws Exception {
        LocalDateTime now = LocalDateTime.now();

        stubAuthenticatedUser();
        insertUser(1L, "nickname", null);
        insertUserHoguStat(1L, 72, 3, now);

        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("nickname"))
                .andExpect(jsonPath("$.profileImageUrl").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.hoguIndex").value(72))
                .andExpect(jsonPath("$.hoguLevel").value("NONE"))
                .andExpect(jsonPath("$.hoguShortDescription").value("레벨을 집계할 수 없습니다."));
    }

    /**
     * 마이페이지 조회 성공 테스트:
     * hoguIndex가 경계값(59)인 유저가 조회 요청을 보내고,
     * - (1) 응답 status가 200 OK인지 확인
     * - (2) 유저 프로필 정보, 레벨 정보가 적절히 반환되는지 확인
     */
    @Test
    void getMyPageReturns200WhenHoguIndexIsBoundaryValue() throws Exception {
        LocalDateTime now = LocalDateTime.now();

        stubAuthenticatedUser();
        insertUser(1L, "nickname", null);
        insertUserHoguStat(1L, 59, 12, now);

        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("nickname"))
                .andExpect(jsonPath("$.profileImageUrl").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.hoguIndex").value(59))
                .andExpect(jsonPath("$.hoguLevel").value("WARNING"))
                .andExpect(jsonPath("$.hoguShortDescription").value("가끔 손해를 감수하는 타입"));
    }

    /**
     * 마이페이지 조회 실패 테스트:
     * access token 없이 요청을 보내고,
     * 응답 status가 401 Unauthorized인지 확인
     */
    @Test
    void getMyPageReturns401WhenAccessTokenIsMissing() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * 호구 보고서 조회 성공 테스트:
     * 투표 참여된 게시물 수가 5개 미만인 유저가 조회 요청을 보내고,
     * - (1) 응답 status가 200 OK인지 확인
     * - (2) 유저 프로필 정보가 적절히 반환되는지 확인
     * - (3) 유저 호구 레벨 및 레벨 설명이 적절히 반환되는지 확인
     * - (4) 카테고리별 분석이 적절히 반환되는지 확인
     */
    @Test
    void getHoguReportReturns200WhenVotedPostIsLessThan5() throws Exception {
        LocalDateTime now = LocalDateTime.now();

        stubAuthenticatedUser();
        insertUser(1L, "nickname 1", null);
        insertUser(2L, "nickname 2", null);
        insertUser(3L, "nickname 3", null);
        insertUserHoguStat(1L, 72, 2, now);

        insertPost(100L, 1L, "DATING", "title 1", "content 1", false, now);
        insertPost(101L, 1L, "USED_TRADE", "title 2", "content 2", false, now);

        insertPostVote(2L, 100L, "HOGU", now);
        insertPostVote(3L, 100L, "NOT_HOGU", now);

        insertPostVote(2L, 101L, "HOGU", now);

        mockMvc.perform(get("/api/users/me/report")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("nickname 1"))
                .andExpect(jsonPath("$.profileImageUrl").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.hoguIndex").value(72))
                .andExpect(jsonPath("$.hoguLevel").value("NONE"))
                .andExpect(jsonPath("$.hoguShortDescription").value("레벨을 집계할 수 없습니다."))
                .andExpect(jsonPath("$.hoguDescription").value("레벨을 집계할 수 없습니다."))
                .andExpect(jsonPath("$.categoryAnalysis.length()").value(0))
                .andExpect(jsonPath("$.totalPostCount").value(2))
                .andExpect(jsonPath("$.hoguPostCount").value(1))
                .andExpect(jsonPath("$.notHoguPostCount").value(0));
    }

    /**
     * 호구 보고서 조회 성공 테스트:
     * 투표 참여된 게시물 수가 5개 이상이며 작성된 게시물의 카테고리가 2가지 이상인 유저가 조회 요청을 보내고,
     * - (1) 응답 status가 200 OK인지 확인
     * - (2) 유저 프로필 정보가 적절히 반환되는지 확인
     * - (3) 유저 호구 레벨 및 레벨 설명이 적절히 반환되는지 확인
     * - (4) 카테고리별 분석이 적절히 반환되는지 확인
     */
    @Test
    void getHoguReportReturns200WhenVotedPostIsEqualTo5AndHasTwoCategories() throws Exception {
        LocalDateTime now = LocalDateTime.now();

        stubAuthenticatedUser();
        insertUser(1L, "nickname 1", null);
        insertUser(2L, "nickname 2", null);
        insertUser(3L, "nickname 3", null);
        insertUserHoguStat(1L, 72, 5, now);

        insertPost(100L, 1L, "DATING", "title 1", "content 1", false, now);
        insertPost(101L, 1L, "USED_TRADE", "title 2", "content 2", false, now);
        insertPost(102L, 1L, "USED_TRADE", "title 3", "content 3", false, now);
        insertPost(103L, 1L, "USED_TRADE", "title 4", "content 4", false, now);
        insertPost(104L, 1L, "USED_TRADE", "title 5", "content 5", false, now);
        insertPost(105L, 1L, "DATING", "title 6", "content 6", false, now);

        insertPostVote(2L, 100L, "HOGU", now);
        insertPostVote(3L, 100L, "NOT_HOGU", now);

        insertPostVote(2L, 101L, "HOGU", now);
        insertPostVote(3L, 101L, "NOT_HOGU", now);

        insertPostVote(3L, 102L, "NOT_HOGU", now);

        insertPostVote(3L, 103L, "NOT_HOGU", now);

        insertPostVote(2L, 104L, "HOGU", now);
        insertPostVote(3L, 104L, "NOT_HOGU", now);

        mockMvc.perform(get("/api/users/me/report")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("nickname 1"))
                .andExpect(jsonPath("$.profileImageUrl").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.hoguIndex").value(72))
                .andExpect(jsonPath("$.hoguLevel").value("RISKY"))
                .andExpect(jsonPath("$.hoguShortDescription").value("거절보다 양보가 앞서는 타입"))
                .andExpect(jsonPath("$.hoguDescription", startsWith("상대를 배려하다가")))
                .andExpect(jsonPath("$.categoryAnalysis.length()").value(2))
                .andExpect(jsonPath("$.categoryAnalysis[0].category").value("DATING"))
                .andExpect(jsonPath("$.categoryAnalysis[0].hoguIndex").value(50))
                .andExpect(jsonPath("$.categoryAnalysis[0].hoguLevel").value("WARNING"))
                .andExpect(jsonPath("$.categoryAnalysis[1].category").value("USED_TRADE"))
                .andExpect(jsonPath("$.categoryAnalysis[1].hoguIndex").value(33))
                .andExpect(jsonPath("$.categoryAnalysis[1].hoguLevel").value("CAUTIOUS"))
                .andExpect(jsonPath("$.totalPostCount").value(6))
                .andExpect(jsonPath("$.hoguPostCount").value(0))
                .andExpect(jsonPath("$.notHoguPostCount").value(2));
    }

    /**
     * 호구 보고서 조회 실패 테스트:
     * access token 없이 요청을 보내고,
     * 응답 status가 401 Unauthorized인지 확인
     */
    @Test
    void getHoguReportReturns401WhenAccessTokenIsMissing() throws Exception {
        mockMvc.perform(get("/api/users/me/report"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * 작성한 게시물 리스트 조회 성공 테스트:
     * 인증된 사용자 정보로 작성한 게시물 리스트 조회를 요청하고,
     * - (1) 응답 status가 200 OK인지 확인
     * - (2) 최신순으로 게시물이 반환되는지 확인
     * - (3) voteSummary가 올바르게 계산되는지 확인
     * - (4) 댓글이 없는 게시물의 commentCount가 0으로 반환되는지 확인
     * - (5) hasNext가 false이고 nextCursor가 null인지 확인
     */
    @Test
    void getMyPostsReturns200WhenFirstPageRequestIsValid() throws Exception {
        stubAuthenticatedUser();

        insertUser(1L, "nickname1", null);
        insertUser(2L, "nickname2", null);

        LocalDateTime olderCreatedAt = LocalDateTime.of(2026, 5, 1, 9, 0, 0);
        LocalDateTime newerCreatedAt = LocalDateTime.of(2026, 5, 1, 12, 0, 0);
        insertPost(100L, 1L, "USED_TRADE", "title 1", "content 1", false, olderCreatedAt);
        insertPost(101L, 1L, "USED_TRADE", "title 2", "content 2", false, newerCreatedAt);
        insertPost(102L, 2L, "USED_TRADE", "title 3", "content 3", false, newerCreatedAt);
        insertPost(103L, 1L, "USED_TRADE", "deleted title", "deleted content", true, newerCreatedAt);

        insertPostVote(1L, 100L, "NOT_HOGU", olderCreatedAt);
        insertPostVote(2L, 100L, "HOGU", olderCreatedAt.plusMinutes(1));

        insertComment(1000L, 100L, 1L, null, 0, "comment 1", olderCreatedAt, false);
        insertComment(1001L, 100L, 2L, 1000L, 1, "comment 1-1", olderCreatedAt.plusMinutes(1), false);

        mockMvc.perform(get("/api/users/me/posts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts.length()").value(2))
                .andExpect(jsonPath("$.posts[0].postId").value(101L))
                .andExpect(jsonPath("$.posts[0].title").value("title 2"))
                .andExpect(jsonPath("$.posts[0].category").value("USED_TRADE"))
                .andExpect(jsonPath("$.posts[0].voteSummary").value("NONE"))
                .andExpect(jsonPath("$.posts[0].commentCount").value(0))
                .andExpect(jsonPath("$.posts[1].postId").value(100))
                .andExpect(jsonPath("$.posts[1].title").value("title 1"))
                .andExpect(jsonPath("$.posts[1].category").value("USED_TRADE"))
                .andExpect(jsonPath("$.posts[1].voteSummary").value("TIE"))
                .andExpect(jsonPath("$.posts[1].commentCount").value(2))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.nextCursor").value(Matchers.nullValue()));
    }

    /**
     * 작성한 게시물 리스트 조회 성공 테스트:
     * 인증된 사용자가 작성한 게시물이 없을 때 요청을 보내고,
     * - (1) 응답 status가 200 OK인지 확인
     * - (2) posts가 빈 배열인지 확인
     * - (3) hasNext가 false인지 확인
     * - (4) nextCursor가 null인지 확인
     */
    @Test
    void getMyPostsReturns200WhenUserHasNoPosts() throws Exception {
        stubAuthenticatedUser();

        insertUser(1L, "nickname", null);

        mockMvc.perform(get("/api/users/me/posts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts.length()").value(0))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.nextCursor").value(Matchers.nullValue()));
    }

    /**
     * 작성한 게시물 리스트 조회 성공 테스트:
     * 인증된 사용자가 pageSize보다 많은 게시물을 가지고 있을 때 요청을 보내고,
     * - (1) 응답 status가 200 OK인지 확인
     * - (2) pageSize만큼 게시물이 반환되는지 확인
     * - (3) hasNext가 true인지 확인
     * - (4) nextCursor가 null이 아닌지 확인
     */
    @Test
    void getMyPostsReturns200WhenHasNextIsTrue() throws Exception {
        stubAuthenticatedUser();

        insertUser(1L, "nickname", null);

        insertPost(100L, 1L, "USED_TRADE", "title 1", "content 1", false, LocalDateTime.of(2026, 5, 1, 9, 0, 0));
        insertPost(101L, 1L, "USED_TRADE", "title 2", "content 2", false, LocalDateTime.of(2026, 5, 1, 10, 0, 0));
        insertPost(102L, 1L, "USED_TRADE", "title 3", "content 3", false, LocalDateTime.of(2026, 5, 1, 11, 0, 0));
        insertPost(103L, 1L, "USED_TRADE", "title 4", "content 4", false, LocalDateTime.of(2026, 5, 1, 12, 0, 0));
        insertPost(104L, 1L, "USED_TRADE", "title 5", "content 5", false, LocalDateTime.of(2026, 5, 1, 13, 0, 0));
        insertPost(105L, 1L, "USED_TRADE", "title 6", "content 6", false, LocalDateTime.of(2026, 5, 1, 14, 0, 0));

        mockMvc.perform(get("/api/users/me/posts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts.length()").value(5))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.nextCursor").isNotEmpty());
    }

    /**
     * 작성한 게시물 리스트 조회 성공 테스트:
     * 인증된 사용자가 첫 페이지에서 받은 nextCursor로 다음 페이지를 요청하고,
     * - (1) 응답 status가 200 OK인지 확인
     * - (2) 다음 페이지의 게시물만 반환되는지 확인
     * - (3) hasNext가 false인지 확인
     * - (4) nextCursor가 null인지 확인
     */
    @Test
    void getMyPostsReturns200WhenCursorIsValid() throws Exception {
        stubAuthenticatedUser();

        insertUser(1L, "nickname", null);

        insertPost(100L, 1L, "USED_TRADE", "title 1", "content 1", false, LocalDateTime.of(2026, 5, 1, 9, 0, 0));
        insertPost(101L, 1L, "USED_TRADE", "title 2", "content 2", false, LocalDateTime.of(2026, 5, 1, 10, 0, 0));
        insertPost(102L, 1L, "USED_TRADE", "title 3", "content 3", false, LocalDateTime.of(2026, 5, 1, 11, 0, 0));
        insertPost(103L, 1L, "USED_TRADE", "title 4", "content 4", false, LocalDateTime.of(2026, 5, 1, 12, 0, 0));
        insertPost(104L, 1L, "USED_TRADE", "title 5", "content 5", false, LocalDateTime.of(2026, 5, 1, 13, 0, 0));
        insertPost(105L, 1L, "USED_TRADE", "title 6", "content 6", false, LocalDateTime.of(2026, 5, 1, 14, 0, 0));

        String responseBody = mockMvc.perform(get("/api/users/me/posts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String nextCursor = JsonPath.read(responseBody, "$.nextCursor");

        mockMvc.perform(get("/api/users/me/posts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                        .param("cursor", nextCursor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts.length()").value(1))
                .andExpect(jsonPath("$.posts[0].postId").value(100L))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.nextCursor").value(Matchers.nullValue()));
    }

    /**
     * 작성한 게시물 리스트 조회 실패 테스트:
     * access token 없이 요청을 보내고,
     * 응답 status가 401 Unauthorized인지 확인
     */
    @Test
    void getMyPostsReturns401WhenAccessTokenIsMissing() throws Exception {
        mockMvc.perform(get("/api/users/me/posts"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * 작성한 댓글 리스트 조회 성공 테스트:
     * 인증된 사용자가 자신이 작성한 댓글 리스트 조회를 요청하고,
     * - (1) 응답 status가 200 OK인지 확인
     * - (2) 최신순으로 댓글이 반환되는지 확인
     * - (3) 삭제된 게시물의 댓글도 포함되는지 확인
     * - (4) 관련 게시물 정보(postId, title, isDeleted)가 올바르게 반환되는지 확인
     * - (5) hasNext가 false이고 nextCursor가 null인지 확인
     */
    @Test
    void getMyCommentsReturns200WhenFirstPageRequestIsValid() throws Exception {
        stubAuthenticatedUser();

        insertUser(1L, "nickname1", null);
        insertUser(2L, "nickname2", null);

        LocalDateTime olderCreatedAt = LocalDateTime.of(2026, 5, 1, 9, 0, 0);
        LocalDateTime newerCreatedAt = LocalDateTime.of(2026, 5, 1, 12, 0, 0);

        insertPost(100L, 1L, "USED_TRADE", "title 1", "content 1", false, olderCreatedAt);
        insertPost(101L, 2L, "USED_TRADE", "deleted title", "deleted content", true, newerCreatedAt);
        insertPost(102L, 2L, "USED_TRADE", "title 2", "content 2", false, newerCreatedAt);

        insertComment(1000L, 100L, 1L, null, 0, "comment 1", olderCreatedAt, false);
        insertComment(1001L, 101L, 1L, null, 0, "comment 2", newerCreatedAt, false);
        insertComment(1002L, 102L, 2L, null, 0, "comment 3", newerCreatedAt, false);

        mockMvc.perform(get("/api/users/me/comments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments.length()").value(2))
                .andExpect(jsonPath("$.comments[0].commentId").value(1001L))
                .andExpect(jsonPath("$.comments[0].content").value("comment 2"))
                .andExpect(jsonPath("$.comments[0].post.postId").value(101L))
                .andExpect(jsonPath("$.comments[0].post.title").value("deleted title"))
                .andExpect(jsonPath("$.comments[0].post.category").value("USED_TRADE"))
                .andExpect(jsonPath("$.comments[0].post.commentCount").value(1))
                .andExpect(jsonPath("$.comments[0].post.isDeleted").value(true))
                .andExpect(jsonPath("$.comments[1].commentId").value(1000L))
                .andExpect(jsonPath("$.comments[1].content").value("comment 1"))
                .andExpect(jsonPath("$.comments[1].post.postId").value(100L))
                .andExpect(jsonPath("$.comments[1].post.title").value("title 1"))
                .andExpect(jsonPath("$.comments[1].post.category").value("USED_TRADE"))
                .andExpect(jsonPath("$.comments[1].post.commentCount").value(1))
                .andExpect(jsonPath("$.comments[1].post.isDeleted").value(false))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.nextCursor").value(Matchers.nullValue()));
    }

    /**
     * 작성한 댓글 리스트 조회 성공 테스트:
     * 인증된 사용자가 pageSize보다 많은 댓글을 작성한 상태에서 요청을 보내고,
     * - (1) 응답 status가 200 OK인지 확인
     * - (2) pageSize만큼 댓글이 반환되는지 확인
     * - (3) hasNext가 true인지 확인
     * - (4) nextCursor가 null이 아닌지 확인
     */
    @Test
    void getMyCommentsReturns200WhenUserHasMoreCommentsThanPageSize() throws Exception {
        stubAuthenticatedUser();

        insertUser(1L, "nickname", null);
        insertPost(100L, 1L, "USED_TRADE", "title 100", "content 100", false, LocalDateTime.of(2026, 5, 1, 9, 0, 0));

        insertComment(1000L, 100L, 1L, null, 0, "comment 1", LocalDateTime.of(2026, 5, 1, 9, 0, 0), false);
        insertComment(1001L, 100L, 1L, null, 0, "comment 2", LocalDateTime.of(2026, 5, 1, 10, 0, 0), false);
        insertComment(1002L, 100L, 1L, null, 0, "comment 3", LocalDateTime.of(2026, 5, 1, 11, 0, 0), false);
        insertComment(1003L, 100L, 1L, null, 0, "comment 4", LocalDateTime.of(2026, 5, 1, 12, 0, 0), false);
        insertComment(1004L, 100L, 1L, null, 0, "comment 5", LocalDateTime.of(2026, 5, 1, 13, 0, 0), false);
        insertComment(1005L, 100L, 1L, null, 0, "comment 6", LocalDateTime.of(2026, 5, 1, 14, 0, 0), false);

        mockMvc.perform(get("/api/users/me/comments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments.length()").value(5))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.nextCursor").isNotEmpty());
    }

    /**
     * 작성한 댓글 리스트 조회 성공 테스트:
     * 인증된 사용자가 작성한 댓글이 없을 때 요청을 보내고,
     * - (1) 응답 status가 200 OK인지 확인
     * - (2) comments가 빈 배열인지 확인
     * - (3) hasNext가 false인지 확인
     * - (4) nextCursor가 null인지 확인
     */
    @Test
    void getMyCommentsReturns200WhenUserHasNoComments() throws Exception {
        stubAuthenticatedUser();

        insertUser(1L, "nickname", null);

        mockMvc.perform(get("/api/users/me/comments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments.length()").value(0))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.nextCursor").value(Matchers.nullValue()));
    }

    /**
     * 작성한 댓글 리스트 조회 성공 테스트:
     * 인증된 사용자가 첫 페이지에서 받은 nextCursor로 다음 페이지를 요청하고,
     * - (1) 응답 status가 200 OK인지 확인
     * - (2) 다음 페이지의 댓글만 반환되는지 확인
     * - (3) hasNext가 false인지 확인
     * - (4) nextCursor가 null인지 확인
     */
    @Test
    void getMyCommentsReturns200WhenCursorIsValidForNextPageRequest() throws Exception {
        stubAuthenticatedUser();

        insertUser(1L, "nickname", null);
        insertPost(100L, 1L, "USED_TRADE", "title", "content", false, LocalDateTime.of(2026, 5, 1, 9, 0, 0));

        insertComment(1000L, 100L, 1L, null, 0, "comment 1", LocalDateTime.of(2026, 5, 1, 9, 0, 0), false);
        insertComment(1001L, 100L, 1L, null, 0, "comment 2", LocalDateTime.of(2026, 5, 1, 10, 0, 0), false);
        insertComment(1002L, 100L, 1L, null, 0, "comment 3", LocalDateTime.of(2026, 5, 1, 11, 0, 0), false);
        insertComment(1003L, 100L, 1L, null, 0, "comment 4", LocalDateTime.of(2026, 5, 1, 12, 0, 0), false);
        insertComment(1004L, 100L, 1L, null, 0, "comment 5", LocalDateTime.of(2026, 5, 1, 13, 0, 0), false);
        insertComment(1005L, 100L, 1L, null, 0, "comment 6", LocalDateTime.of(2026, 5, 1, 14, 0, 0), false);

        String responseBody = mockMvc.perform(get("/api/users/me/comments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String nextCursor = JsonPath.read(responseBody, "$.nextCursor");

        mockMvc.perform(get("/api/users/me/comments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                        .param("cursor", nextCursor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments.length()").value(1))
                .andExpect(jsonPath("$.comments[0].commentId").value(1000L))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.nextCursor").value(Matchers.nullValue()));
    }

    /**
     * 작성한 댓글 리스트 조회 실패 테스트:
     * access token 없이 요청을 보내고,
     * - (1) 응답 status가 401 Unauthorized인지 확인
     */
    @Test
    void getMyCommentsReturns401WhenAccessTokenIsMissing() throws Exception {
        mockMvc.perform(get("/api/users/me/comments"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * 북마크한 게시물 리스트 조회 성공 테스트:
     * 인증된 사용자가 자신이 북마크한 게시물 리스트 조회를 요청하고,
     * - (1) 응답 status가 200 OK인지 확인
     * - (2) 북마크 생성일 최신순으로 게시물이 반환되는지 확인
     * - (3) 삭제된 게시물도 포함되는지 확인
     * - (4) voteSummary와 commentCount가 올바르게 반환되는지 확인
     * - (5) hasNext가 false이고 nextCursor가 null인지 확인
     */
    @Test
    void getMyBookmarksReturns200WhenFirstPageRequestIsValid() throws Exception {
        stubAuthenticatedUser();

        insertUser(1L, "nickname1", null);
        insertUser(2L, "nickname2", null);

        LocalDateTime olderCreatedAt = LocalDateTime.of(2026, 5, 1, 9, 0, 0);
        LocalDateTime newerCreatedAt = LocalDateTime.of(2026, 5, 1, 12, 0, 0);
        LocalDateTime olderBookmarkCreatedAt = LocalDateTime.of(2026, 5, 2, 9, 0, 0);
        LocalDateTime newerBookmarkCreatedAt = LocalDateTime.of(2026, 5, 2, 12, 0, 0);

        insertPost(100L, 2L, "USED_TRADE", "title 1", "content 1", false, olderCreatedAt);
        insertPost(101L, 2L, "USED_TRADE", "deleted title", "deleted content", true, newerCreatedAt);
        insertPost(102L, 2L, "USED_TRADE", "title 2", "content 2", false, newerCreatedAt);

        insertPostBookmark(1L, 100L, olderBookmarkCreatedAt);
        insertPostBookmark(1L, 101L, newerBookmarkCreatedAt);
        insertPostBookmark(2L, 102L, newerBookmarkCreatedAt);

        insertPostVote(1L, 100L, "NOT_HOGU", olderCreatedAt);
        insertPostVote(2L, 100L, "HOGU", olderCreatedAt.plusMinutes(1));

        insertComment(1000L, 100L, 1L, null, 0, "comment 1", olderCreatedAt, false);
        insertComment(1001L, 100L, 2L, 1000L, 1, "comment 1-1", olderCreatedAt.plusMinutes(1), false);

        mockMvc.perform(get("/api/users/me/bookmarks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts.length()").value(2))
                .andExpect(jsonPath("$.posts[0].postId").value(101L))
                .andExpect(jsonPath("$.posts[0].title").value("deleted title"))
                .andExpect(jsonPath("$.posts[0].category").value("USED_TRADE"))
                .andExpect(jsonPath("$.posts[0].voteSummary").value("NONE"))
                .andExpect(jsonPath("$.posts[0].commentCount").value(0))
                .andExpect(jsonPath("$.posts[0].isDeleted").value(true))
                .andExpect(jsonPath("$.posts[1].postId").value(100L))
                .andExpect(jsonPath("$.posts[1].title").value("title 1"))
                .andExpect(jsonPath("$.posts[1].category").value("USED_TRADE"))
                .andExpect(jsonPath("$.posts[1].voteSummary").value("TIE"))
                .andExpect(jsonPath("$.posts[1].commentCount").value(2))
                .andExpect(jsonPath("$.posts[1].isDeleted").value(false))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.nextCursor").value(Matchers.nullValue()));
    }

    /**
     * 북마크한 게시물 리스트 조회 성공 테스트:
     * 인증된 사용자가 pageSize보다 많은 게시물을 북마크한 상태에서 요청을 보내고,
     * - (1) 응답 status가 200 OK인지 확인
     * - (2) pageSize만큼 게시물이 반환되는지 확인
     * - (3) hasNext가 true인지 확인
     * - (4) nextCursor가 null이 아닌지 확인
     */
    @Test
    void getMyBookmarksReturns200WhenUserHasMoreBookmarksThanPageSize() throws Exception {
        stubAuthenticatedUser();

        insertUser(1L, "nickname", null);

        insertPost(100L, 1L, "USED_TRADE", "title 1", "content 1", false, LocalDateTime.of(2026, 5, 1, 9, 0, 0));
        insertPost(101L, 1L, "USED_TRADE", "title 2", "content 2", false, LocalDateTime.of(2026, 5, 1, 10, 0, 0));
        insertPost(102L, 1L, "USED_TRADE", "title 3", "content 3", false, LocalDateTime.of(2026, 5, 1, 11, 0, 0));
        insertPost(103L, 1L, "USED_TRADE", "title 4", "content 4", false, LocalDateTime.of(2026, 5, 1, 12, 0, 0));
        insertPost(104L, 1L, "USED_TRADE", "title 5", "content 5", false, LocalDateTime.of(2026, 5, 1, 13, 0, 0));
        insertPost(105L, 1L, "USED_TRADE", "title 6", "content 6", false, LocalDateTime.of(2026, 5, 1, 14, 0, 0));

        insertPostBookmark(1L, 100L, LocalDateTime.of(2026, 5, 2, 9, 0, 0));
        insertPostBookmark(1L, 101L, LocalDateTime.of(2026, 5, 2, 10, 0, 0));
        insertPostBookmark(1L, 102L, LocalDateTime.of(2026, 5, 2, 11, 0, 0));
        insertPostBookmark(1L, 103L, LocalDateTime.of(2026, 5, 2, 12, 0, 0));
        insertPostBookmark(1L, 104L, LocalDateTime.of(2026, 5, 2, 13, 0, 0));
        insertPostBookmark(1L, 105L, LocalDateTime.of(2026, 5, 2, 14, 0, 0));

        mockMvc.perform(get("/api/users/me/bookmarks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts.length()").value(5))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.nextCursor").isNotEmpty());
    }

    /**
     * 북마크한 게시물 리스트 조회 성공 테스트:
     * 인증된 사용자가 북마크한 게시물이 없을 때 요청을 보내고,
     * - (1) 응답 status가 200 OK인지 확인
     * - (2) posts가 빈 배열인지 확인
     * - (3) hasNext가 false인지 확인
     * - (4) nextCursor가 null인지 확인
     */
    @Test
    void getMyBookmarksReturns200WhenUserHasNoBookmarks() throws Exception {
        stubAuthenticatedUser();

        insertUser(1L, "nickname", null);

        mockMvc.perform(get("/api/users/me/bookmarks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts.length()").value(0))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.nextCursor").value(Matchers.nullValue()));
    }

    /**
     * 북마크한 게시물 리스트 조회 성공 테스트:
     * 인증된 사용자가 삭제된 게시물을 북마크한 상태에서 요청을 보내고,
     * - (1) 응답 status가 200 OK인지 확인
     * - (2) 삭제된 게시물도 포함되는지 확인
     */
    @Test
    void getMyBookmarksReturns200WhenBookmarkedPostsAreDeleted() throws Exception {
        stubAuthenticatedUser();

        insertUser(1L, "nickname", null);

        LocalDateTime createdAt = LocalDateTime.of(2026, 5, 1, 9, 0, 0);
        insertPost(100L, 1L, "USED_TRADE", "deleted title", "deleted content", true, createdAt);
        insertPostBookmark(1L, 100L, LocalDateTime.of(2026, 5, 2, 9, 0, 0));

        mockMvc.perform(get("/api/users/me/bookmarks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts.length()").value(1))
                .andExpect(jsonPath("$.posts[0].postId").value(100L))
                .andExpect(jsonPath("$.posts[0].title").value("deleted title"))
                .andExpect(jsonPath("$.posts[0].category").value("USED_TRADE"))
                .andExpect(jsonPath("$.posts[0].commentCount").value(0))
                .andExpect(jsonPath("$.posts[0].isDeleted").value(true));
    }

    /**
     * 북마크한 게시물 리스트 조회 성공 테스트:
     * 인증된 사용자가 첫 페이지에서 받은 nextCursor로 다음 페이지를 요청하고,
     * - (1) 응답 status가 200 OK인지 확인
     * - (2) 다음 페이지의 게시물만 반환되는지 확인
     * - (3) hasNext가 false인지 확인
     * - (4) nextCursor가 null인지 확인
     */
    @Test
    void getMyBookmarksReturns200WhenCursorIsValidForNextPageRequest() throws Exception {
        stubAuthenticatedUser();

        insertUser(1L, "nickname", null);

        insertPost(100L, 1L, "USED_TRADE", "title 1", "content 1", false, LocalDateTime.of(2026, 5, 1, 9, 0, 0));
        insertPost(101L, 1L, "USED_TRADE", "title 2", "content 2", false, LocalDateTime.of(2026, 5, 1, 10, 0, 0));
        insertPost(102L, 1L, "USED_TRADE", "title 3", "content 3", false, LocalDateTime.of(2026, 5, 1, 11, 0, 0));
        insertPost(103L, 1L, "USED_TRADE", "title 4", "content 4", false, LocalDateTime.of(2026, 5, 1, 12, 0, 0));
        insertPost(104L, 1L, "USED_TRADE", "title 5", "content 5", false, LocalDateTime.of(2026, 5, 1, 13, 0, 0));
        insertPost(105L, 1L, "USED_TRADE", "title 6", "content 6", false, LocalDateTime.of(2026, 5, 1, 14, 0, 0));

        insertPostBookmark(1L, 100L, LocalDateTime.of(2026, 5, 2, 9, 0, 0));
        insertPostBookmark(1L, 101L, LocalDateTime.of(2026, 5, 2, 10, 0, 0));
        insertPostBookmark(1L, 102L, LocalDateTime.of(2026, 5, 2, 11, 0, 0));
        insertPostBookmark(1L, 103L, LocalDateTime.of(2026, 5, 2, 12, 0, 0));
        insertPostBookmark(1L, 104L, LocalDateTime.of(2026, 5, 2, 13, 0, 0));
        insertPostBookmark(1L, 105L, LocalDateTime.of(2026, 5, 2, 14, 0, 0));

        String responseBody = mockMvc.perform(get("/api/users/me/bookmarks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String nextCursor = JsonPath.read(responseBody, "$.nextCursor");

        mockMvc.perform(get("/api/users/me/bookmarks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                        .param("cursor", nextCursor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts.length()").value(1))
                .andExpect(jsonPath("$.posts[0].postId").value(100L))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.nextCursor").value(Matchers.nullValue()));
    }

    /**
     * 북마크한 게시물 리스트 조회 실패 테스트:
     * access token 없이 요청을 보내고,
     * - (1) 응답 status가 401 Unauthorized인지 확인
     */
    @Test
    void getMyBookmarksReturns401WhenAccessTokenIsMissing() throws Exception {
        mockMvc.perform(get("/api/users/me/bookmarks"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * 참여한 투표 리스트 조회 성공 테스트:
     * 인증된 사용자가 자신이 참여한 투표 리스트 조회를 요청하고,
     * - (1) 응답 status가 200 OK인지 확인
     * - (2) 투표 생성일 최신순으로 반환되는지 확인
     * - (3) 삭제된 게시물의 투표도 포함되는지 확인
     * - (4) 관련 게시물 정보(postId, title, isDeleted)가 올바르게 반환되는지 확인
     * - (5) hasNext가 false이고 nextCursor가 null인지 확인
     */
    @Test
    void getMyVotesReturns200WhenFirstPageRequestIsValid() throws Exception {
        stubAuthenticatedUser();

        insertUser(1L, "nickname1", null);
        insertUser(2L, "nickname2", null);

        LocalDateTime olderCreatedAt = LocalDateTime.of(2026, 5, 1, 9, 0, 0);
        LocalDateTime newerCreatedAt = LocalDateTime.of(2026, 5, 1, 12, 0, 0);

        insertPost(100L, 2L, "USED_TRADE", "title 1", "content 1", false, olderCreatedAt);
        insertPost(101L, 2L, "USED_TRADE", "deleted title", "deleted content", true, newerCreatedAt);
        insertPost(102L, 2L, "USED_TRADE", "title 2", "content 2", false, newerCreatedAt);

        insertPostVote(1L, 100L, "NOT_HOGU", olderCreatedAt);
        insertPostVote(1L, 101L, "HOGU", newerCreatedAt);
        insertPostVote(2L, 102L, "HOGU", newerCreatedAt);

        mockMvc.perform(get("/api/users/me/votes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.votes.length()").value(2))
                .andExpect(jsonPath("$.votes[0].myVote").value("HOGU"))
                .andExpect(jsonPath("$.votes[0].post.postId").value(101L))
                .andExpect(jsonPath("$.votes[0].post.title").value("deleted title"))
                .andExpect(jsonPath("$.votes[0].post.category").value("USED_TRADE"))
                .andExpect(jsonPath("$.votes[0].post.commentCount").value(0))
                .andExpect(jsonPath("$.votes[0].post.isDeleted").value(true))
                .andExpect(jsonPath("$.votes[1].myVote").value("NOT_HOGU"))
                .andExpect(jsonPath("$.votes[1].post.postId").value(100L))
                .andExpect(jsonPath("$.votes[1].post.title").value("title 1"))
                .andExpect(jsonPath("$.votes[1].post.category").value("USED_TRADE"))
                .andExpect(jsonPath("$.votes[1].post.commentCount").value(0))
                .andExpect(jsonPath("$.votes[1].post.isDeleted").value(false))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.nextCursor").value(Matchers.nullValue()));
    }

    /**
     * 참여한 투표 리스트 조회 성공 테스트:
     * 인증된 사용자가 pageSize보다 많은 투표를 한 상태에서 요청을 보내고,
     * - (1) 응답 status가 200 OK인지 확인
     * - (2) pageSize만큼 투표가 반환되는지 확인
     * - (3) hasNext가 true인지 확인
     * - (4) nextCursor가 null이 아닌지 확인
     */
    @Test
    void getMyVotesReturns200WhenUserHasMoreVotesThanPageSize() throws Exception {
        stubAuthenticatedUser();

        insertUser(1L, "nickname", null);

        insertPost(100L, 1L, "USED_TRADE", "title 1", "content 1", false, LocalDateTime.of(2026, 5, 1, 9, 0, 0));
        insertPost(101L, 1L, "USED_TRADE", "title 2", "content 2", false, LocalDateTime.of(2026, 5, 1, 10, 0, 0));
        insertPost(102L, 1L, "USED_TRADE", "title 3", "content 3", false, LocalDateTime.of(2026, 5, 1, 11, 0, 0));
        insertPost(103L, 1L, "USED_TRADE", "title 4", "content 4", false, LocalDateTime.of(2026, 5, 1, 12, 0, 0));
        insertPost(104L, 1L, "USED_TRADE", "title 5", "content 5", false, LocalDateTime.of(2026, 5, 1, 13, 0, 0));
        insertPost(105L, 1L, "USED_TRADE", "title 6", "content 6", false, LocalDateTime.of(2026, 5, 1, 14, 0, 0));

        insertPostVote(1L, 100L, "HOGU", LocalDateTime.of(2026, 5, 2, 9, 0, 0));
        insertPostVote(1L, 101L, "HOGU", LocalDateTime.of(2026, 5, 2, 10, 0, 0));
        insertPostVote(1L, 102L, "HOGU", LocalDateTime.of(2026, 5, 2, 11, 0, 0));
        insertPostVote(1L, 103L, "HOGU", LocalDateTime.of(2026, 5, 2, 12, 0, 0));
        insertPostVote(1L, 104L, "HOGU", LocalDateTime.of(2026, 5, 2, 13, 0, 0));
        insertPostVote(1L, 105L, "HOGU", LocalDateTime.of(2026, 5, 2, 14, 0, 0));

        mockMvc.perform(get("/api/users/me/votes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.votes.length()").value(5))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.nextCursor").isNotEmpty());
    }

    /**
     * 참여한 투표 리스트 조회 성공 테스트:
     * 인증된 사용자가 참여한 투표가 없을 때 요청을 보내고,
     * - (1) 응답 status가 200 OK인지 확인
     * - (2) votes가 빈 배열인지 확인
     * - (3) hasNext가 false인지 확인
     * - (4) nextCursor가 null인지 확인
     */
    @Test
    void getMyVotesReturns200WhenUserHasNoVotes() throws Exception {
        stubAuthenticatedUser();

        insertUser(1L, "nickname", null);

        mockMvc.perform(get("/api/users/me/votes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.votes.length()").value(0))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.nextCursor").value(Matchers.nullValue()));
    }

    /**
     * 참여한 투표 리스트 조회 성공 테스트:
     * 인증된 사용자가 삭제된 게시물에 투표한 상태에서 요청을 보내고,
     * - (1) 응답 status가 200 OK인지 확인
     * - (2) 삭제된 게시물의 투표도 포함되는지 확인
     */
    @Test
    void getMyVotesReturns200WhenVotedPostsAreDeleted() throws Exception {
        stubAuthenticatedUser();

        insertUser(1L, "nickname", null);

        LocalDateTime createdAt = LocalDateTime.of(2026, 5, 1, 9, 0, 0);
        insertPost(100L, 1L, "USED_TRADE", "deleted title", "deleted content", true, createdAt);
        insertPostVote(1L, 100L, "HOGU", LocalDateTime.of(2026, 5, 2, 9, 0, 0));

        mockMvc.perform(get("/api/users/me/votes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.votes.length()").value(1))
                .andExpect(jsonPath("$.votes[0].myVote").value("HOGU"))
                .andExpect(jsonPath("$.votes[0].post.postId").value(100L))
                .andExpect(jsonPath("$.votes[0].post.title").value("deleted title"))
                .andExpect(jsonPath("$.votes[0].post.category").value("USED_TRADE"))
                .andExpect(jsonPath("$.votes[0].post.commentCount").value(0))
                .andExpect(jsonPath("$.votes[0].post.isDeleted").value(true));
    }

    /**
     * 참여한 투표 리스트 조회 성공 테스트:
     * 인증된 사용자가 첫 페이지에서 받은 nextCursor로 다음 페이지를 요청하고,
     * - (1) 응답 status가 200 OK인지 확인
     * - (2) 다음 페이지의 투표만 반환되는지 확인
     * - (3) hasNext가 false인지 확인
     * - (4) nextCursor가 null인지 확인
     */
    @Test
    void getMyVotesReturns200WhenCursorIsValidForNextPageRequest() throws Exception {
        stubAuthenticatedUser();

        insertUser(1L, "nickname", null);

        insertPost(100L, 1L, "USED_TRADE", "title 1", "content 1", false, LocalDateTime.of(2026, 5, 1, 9, 0, 0));
        insertPost(101L, 1L, "USED_TRADE", "title 2", "content 2", false, LocalDateTime.of(2026, 5, 1, 10, 0, 0));
        insertPost(102L, 1L, "USED_TRADE", "title 3", "content 3", false, LocalDateTime.of(2026, 5, 1, 11, 0, 0));
        insertPost(103L, 1L, "USED_TRADE", "title 4", "content 4", false, LocalDateTime.of(2026, 5, 1, 12, 0, 0));
        insertPost(104L, 1L, "USED_TRADE", "title 5", "content 5", false, LocalDateTime.of(2026, 5, 1, 13, 0, 0));
        insertPost(105L, 1L, "USED_TRADE", "title 6", "content 6", false, LocalDateTime.of(2026, 5, 1, 14, 0, 0));

        insertPostVote(1L, 100L, "HOGU", LocalDateTime.of(2026, 5, 2, 9, 0, 0));
        insertPostVote(1L, 101L, "HOGU", LocalDateTime.of(2026, 5, 2, 10, 0, 0));
        insertPostVote(1L, 102L, "HOGU", LocalDateTime.of(2026, 5, 2, 11, 0, 0));
        insertPostVote(1L, 103L, "HOGU", LocalDateTime.of(2026, 5, 2, 12, 0, 0));
        insertPostVote(1L, 104L, "HOGU", LocalDateTime.of(2026, 5, 2, 13, 0, 0));
        insertPostVote(1L, 105L, "HOGU", LocalDateTime.of(2026, 5, 2, 14, 0, 0));

        String responseBody = mockMvc.perform(get("/api/users/me/votes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String nextCursor = JsonPath.read(responseBody, "$.nextCursor");

        mockMvc.perform(get("/api/users/me/votes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                        .param("cursor", nextCursor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.votes.length()").value(1))
                .andExpect(jsonPath("$.votes[0].post.postId").value(100L))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.nextCursor").value(Matchers.nullValue()));
    }

    /**
     * 참여한 투표 리스트 조회 실패 테스트:
     * access token 없이 요청을 보내고,
     * - (1) 응답 status가 401 Unauthorized인지 확인
     */
    @Test
    void getMyVotesReturns401WhenAccessTokenIsMissing() throws Exception {
        mockMvc.perform(get("/api/users/me/votes"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * 테스트에서 사용할 가짜 로그인 사용자를 설정
     * Authorization 헤더에 "Bearer valid-token"이 들어오면 userId = 1L 사용자로 인증된 상태가 됨
     */
    private void stubAuthenticatedUser() {
        when(jwtProvider.validateAccessToken("valid-token"))
                .thenReturn(JwtProvider.TokenValidationResult.VALID);
        when(jwtProvider.getSubjectAsLong("valid-token"))
                .thenReturn(1L);
        when(jwtProvider.getAuthentication("valid-token"))
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

    private void insertPost(
            Long postId,
            Long writerUserId,
            String categoryCode,
            String title,
            String content,
            boolean isDeleted,
            LocalDateTime createdAt
    ) {
        LocalDateTime deletedAt = isDeleted ? createdAt.plusMinutes(10) : null;

        jdbcTemplate.update(
                """
                INSERT INTO posts
                    (id, writer_user_id, category_code, title, content, view_count, is_deleted, deleted_at, created_at, updated_at)
                VALUES
                    (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                postId,
                writerUserId,
                categoryCode,
                title,
                content,
                0,
                isDeleted,
                deletedAt,
                createdAt,
                createdAt
        );
    }

    private void insertPostVote(Long userId, Long postId, String myVote, LocalDateTime createdAt) {
        jdbcTemplate.update(
                """
                INSERT INTO post_votes
                    (user_id, post_id, my_vote, created_at, updated_at)
                VALUES
                    (?, ?, ?, ?, ?)
                """,
                userId,
                postId,
                myVote,
                createdAt,
                createdAt
        );
    }

    private void insertComment(
            Long id,
            Long postId,
            Long writerUserId,
            Long parentCommentId,
            Integer depth,
            String content,
            LocalDateTime createdAt,
            boolean isDeleted
    ) {
        LocalDateTime deletedAt = isDeleted ? createdAt.plusMinutes(10) : null;

        jdbcTemplate.update(
                """
                INSERT INTO comments
                    (id, post_id, writer_user_id, parent_comment_id, depth, content,
                     is_deleted, deleted_at, created_at, updated_at)
                VALUES 
                    (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                id,
                postId,
                writerUserId,
                parentCommentId,
                depth,
                content,
                isDeleted,
                deletedAt,
                createdAt,
                createdAt
        );
    }

    private void insertPostBookmark(Long userId, Long postId, LocalDateTime createdAt) {
        jdbcTemplate.update(
                """
                INSERT INTO post_bookmarks
                    (user_id, post_id, created_at)
                VALUES
                    (?, ?, ?)
                """,
                userId,
                postId,
                createdAt
        );
    }

    private void insertUserHoguStat(
            Long userId,
            int hoguIndex,
            int votedPostCount,
            LocalDateTime now
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO user_hogu_stats
                    (user_id, hogu_vote_count, total_vote_count, hogu_index, updated_at, voted_post_count)
                VALUES
                    (?, ?, ?, ?, ?, ?)
                """,
                userId,
                0,
                0,
                hoguIndex,
                now,
                votedPostCount
        );
    }
}
