package com.hogu.am_i_hogu.domain.user.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM user_hogu_stats");
        jdbcTemplate.update("DELETE FROM users");
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
     * - (1) 응답 status가 404인지 확인
     * - (2) <필드 정보: nickname, 오류 코드: EMPTY_NICKNAME> 반환 확인
     */
    @Test
    void nicknameCheckRejectsEmptyNickname() throws Exception {
        mockMvc.perform(get("/api/users/check-nickname")
                    .param("nickname", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.errors[0].field").value("nickname"))
                .andExpect(jsonPath("$.errors[0].code").value("EMPTY_NICKNAME"));
    }

    /**
     * 닉네임 중복 검사 실패 테스트:
     * 특수문자를 포함한 nickname을 요청으로 보내고,
     * - (1) 응답 status가 404인지 확인
     * - (2) <필드 정보: nickname, 오류 코드: SPECIAL_CHAR_NICKNAME> 반환 확인
     */
    @Test
    void nicknameCheckRejectsSpecialChar() throws Exception {
        mockMvc.perform(get("/api/users/check-nickname")
                    .param("nickname", "special char nickname"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.errors[0].field").value("nickname"))
                .andExpect(jsonPath("$.errors[0].code").value("SPECIAL_CHAR_NICKNAME"));
    }

    /**
     * 닉네임 중복 검사 실패 테스트:
     * 길이가 20을 초과하는 nickname을 요청으로 보내고,
     * - (1) 응답 status가 404인지 확인
     * - (2) <필드 정보: nickname, 오류 코드: NICKNAME_LENGTH_EXCEEDED> 반환 확인
     */
    @Test
    void nicknameCheckRejectsLongNickname() throws Exception {
        String nickname = "thisisverylongnickname";
        mockMvc.perform(get("/api/users/check-nickname")
                    .param("nickname", nickname))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.errors[0].field").value("nickname"))
                .andExpect(jsonPath("$.errors[0].code").value("NICKNAME_LENGTH_EXCEEDED"));
    }

    /**
     * 닉네임 중복 검사 실패 테스트:
     * 특수문자를 포함하며 길이가 20을 초과하는 nickname을 요청으로 보내고,
     * - (1) 응답 status가 404인지 확인
     * - (2) <필드 정보: nickname, 오류 코드: SPECIAL_CHAR_NICKNAME>,
     *      <필드 정보: nickname, 오류 코드: NICKNAME_LENGTH_EXCEEDED 반환 확인
     */
    @Test
    void nicknameCheckRejectsSpecialCharAndLongNickname() throws Exception {
        String nickname = "this is very long nickname";
        mockMvc.perform(get("/api/users/check-nickname")
                .param("nickname", nickname))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.errors[0].field").value("nickname"))
                .andExpect(jsonPath("$.errors[0].code").value("SPECIAL_CHAR_NICKNAME"))
                .andExpect(jsonPath("$.errors[1].field").value("nickname"))
                .andExpect(jsonPath("$.errors[1].code").value("NICKNAME_LENGTH_EXCEEDED"));
    }
}
