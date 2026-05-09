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

import java.time.LocalDateTime;
import java.util.Collections;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
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
        jdbcTemplate.update("DELETE FROM user_hogu_stats");
        jdbcTemplate.update("DELETE FROM users");
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
    void nicknameUpdateReturnsUserInfoAndUpdateNickname() throws Exception {
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
    void profileImageUpdateReturnUserInfoAndUpdateProfileImage() throws Exception {
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
    void profileUpdateReturnUserInfoAndUpdateProfile() throws Exception {
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
    void deleteProfileImageReturnsUserInfoAndDeleteProfileImage() throws Exception {
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
    void profileUpdateRejectsEmptyRequestBody() throws Exception {
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
    void profileUpdateRejectsEmptyJsonObject() throws Exception {
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
    void profileUpdateRejectsDuplicateNickname() throws Exception {
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
     * - (1) 응답 status가 404 Not Found인지 확인
     * - (2) USER_NOT_FOUND 오류 코드를 반환하는지 확인
     */
    @Test
    void profileUpdateRejectsWhenUserNotFound() throws Exception {
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
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    /**
     * 닉네임 중복 검사 성공 테스트:
     * 이용 가능한 nickname으로 요청을 보내고,
     * - (1) 응답 status가 200 OK인지 확인
     * - (2) 사용 가능 여부가 true로 반환되는지 확인
     */
    @Test
    void nicknameCheckReturnsTrue() throws Exception {
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
    void nicknameCheckReturnsFalse() throws Exception {
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
     * 비어있는 nickname을 요청으로 보내고,
     * - (1) 응답 status가 400 Bad Request인지 확인
     * - (2) <필드 정보: nickname, 오류 코드: EMPTY_NICKNAME> 반환 확인
     */
    @Test
    void nicknameCheckRejectsEmptyNickname() throws Exception {
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
    void nicknameCheckRejectsSpecialChar() throws Exception {
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
    void nicknameCheckRejectsLongNickname() throws Exception {
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
    void nicknameCheckRejectsSpecialCharAndLongNickname() throws Exception {
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
     * 테스트에서 사용할 가짜 로그인 사용자를 설정
     * Authorization 헤더에 "Bearer valid-token"이 들어오면 userId = 1L 사용자로 인증된 상태가 됨
     */
    private void stubAuthenticatedUser() {
        when(jwtProvider.validateAccessToken("valid-token"))
                .thenReturn(JwtProvider.TokenValidationResult.VALID);
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
}
