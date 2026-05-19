package com.hogu.am_i_hogu.domain.comment.controller;

import com.hogu.am_i_hogu.common.security.JwtProvider;
import com.hogu.am_i_hogu.domain.comment.domain.Comment;
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
import com.jayway.jsonpath.JsonPath;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
public class CommentControllerTest {

    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("am_i_hogu_comment_test_db")
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
        jdbcTemplate.update("DELETE FROM comments");
        jdbcTemplate.update("DELETE FROM posts");
        jdbcTemplate.update("DELETE FROM users");
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
    }

    /**
     * 집단지성 생성 성공 테스트:
     * 게시물 작성자가 아닌 유저가 게시물에 첫 집단지성 생성 요청을 보내고,
     * - (1) 응답 status가 200 OK인지 확인
     * - (2) 작성된 집단지성 정보가 적절히 반환되는지 확인
     * - (3) 게시물과 댓글이 연결되었는지 확인
     */
    @Test
    void createReturns200WhenDepthEquals0AndIsNotPostWriter() throws Exception{
        stubAuthenticatedUser();
        insertUser(1L, "comment writer", null);
        insertUser(2L, "post writer", null);

        LocalDateTime now = LocalDateTime.now();
        insertPost(100L, 2L, "USED_TRADE", "title", "content", false, now);

        String requestBody = """
                {
                    "parentId" : null,
                    "content" : "content"
                }
                """;
        mockMvc.perform(post("/api/posts/{postId}/comments", 100L)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentId").isNumber())
                .andExpect(jsonPath("$.content").value("content"))
                .andExpect(jsonPath("$.isMine").value(true))
                .andExpect(jsonPath("$.writer.nickname").value("comment writer"))
                .andExpect(jsonPath("$.writer.profileImageUrl").value(nullValue()))
                .andExpect(jsonPath("$.writer.isPostWriter").value(false))
                .andExpect(jsonPath("$.createdAt").value(notNullValue()))
                .andExpect(jsonPath("$.updatedAt").value(notNullValue()))
                .andExpect(jsonPath("$.isHelpful").value(false))
                .andExpect(jsonPath("$.totalHelpfulCount").isNumber())
                .andExpect(jsonPath("$.parentId").value(nullValue()))
                .andExpect(jsonPath("$.depth").value(0));

        Long commentCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM comments WHERE post_id = ?",
                Long.class,
                100L
        );
        assertThat(commentCount).isEqualTo(1);
    }

    /**
     * 집단지성 생성 성공 테스트:
     * 게시물 작성자가 아닌 유저가 게시물에 depth 1 집단지성 생성 요청을 보내고,
     * - (1) 응답 status가 200 OK인지 확인
     * - (2) 작성된 집단지성 정보가 적절히 반환되는지 확인
     * - (3) 게시물과 댓글이 연결되었는지 확인
     */
    @Test
    void createReturns200WhenDepthEquals1AndIsNotPostWriter() throws Exception{
        stubAuthenticatedUser();
        insertUser(1L, "comment writer", null);
        insertUser(2L, "post writer", null);

        LocalDateTime now = LocalDateTime.now();
        insertPost(100L, 2L, "USED_TRADE", "title", "content", false, now);
        insertComment(1000L, 100L, 1L, null, 0, "parent content", now, false);

        String requestBody = """
                {
                    "parentId" : 1000,
                    "content" : "child content"
                }
                """;
        mockMvc.perform(post("/api/posts/{postId}/comments", 100L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentId").isNumber())
                .andExpect(jsonPath("$.content").value("child content"))
                .andExpect(jsonPath("$.isMine").value(true))
                .andExpect(jsonPath("$.writer.nickname").value("comment writer"))
                .andExpect(jsonPath("$.writer.profileImageUrl").value(nullValue()))
                .andExpect(jsonPath("$.writer.isPostWriter").value(false))
                .andExpect(jsonPath("$.createdAt").value(notNullValue()))
                .andExpect(jsonPath("$.updatedAt").value(notNullValue()))
                .andExpect(jsonPath("$.isHelpful").value(false))
                .andExpect(jsonPath("$.totalHelpfulCount").isNumber())
                .andExpect(jsonPath("$.parentId").value(1000))
                .andExpect(jsonPath("$.depth").value(1));

        Long commentCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM comments WHERE post_id = ?",
                Long.class,
                100L
        );
        assertThat(commentCount).isEqualTo(2);
    }

    /**
     * 집단지성 생성 성공 테스트:
     * 게시물 작성자인 유저가 게시물에 첫 집단지성 생성 요청을 보내고,
     * - (1) 응답 status가 200 OK인지 확인
     * - (2) 작성된 집단지성 정보가 적절히 반환되는지 확인
     * - (3) 게시물과 댓글이 연결되었는지 확인
     */
    @Test
    void createReturns200WhenIsPostWriter() throws Exception{
        stubAuthenticatedUser();
        insertUser(1L, "writer", null);

        LocalDateTime now = LocalDateTime.now();
        insertPost(100L, 1L, "USED_TRADE", "title", "content", false, now);

        String requestBody = """
                {
                    "parentId" : null,
                    "content" : "content"
                }
                """;
        mockMvc.perform(post("/api/posts/{postId}/comments", 100L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentId").isNumber())
                .andExpect(jsonPath("$.content").value("content"))
                .andExpect(jsonPath("$.isMine").value(true))
                .andExpect(jsonPath("$.writer.nickname").value("writer"))
                .andExpect(jsonPath("$.writer.profileImageUrl").value(nullValue()))
                .andExpect(jsonPath("$.writer.isPostWriter").value(true))
                .andExpect(jsonPath("$.createdAt").value(notNullValue()))
                .andExpect(jsonPath("$.updatedAt").value(notNullValue()))
                .andExpect(jsonPath("$.isHelpful").value(false))
                .andExpect(jsonPath("$.totalHelpfulCount").isNumber())
                .andExpect(jsonPath("$.parentId").value(nullValue()))
                .andExpect(jsonPath("$.depth").value(0));

        Long commentCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM comments WHERE post_id = ?",
                Long.class,
                100L
        );
        assertThat(commentCount).isEqualTo(1);
    }

    /**
     * 집단지성 생성 실패 테스트:
     * 삭제된 게시물에 대해 집단지성 생성 요청을 보내고,
     * - (1) 응답 status가 404 Not Found인지 확인
     * - (2) DB에 집단지성 row가 생성되지 않았는지 확인
     */
    @Test
    void createReturns404WhenPostAlreadyDeleted() throws Exception{
        stubAuthenticatedUser();
        insertUser(1L, "comment writer", null);

        LocalDateTime now = LocalDateTime.now();
        insertPost(100L, 1L, "USED_TRADE", "title", "content", true, now);

        String requestBody = """
                {
                    "parentId" : null,
                    "content" : "content"
                }
                """;
        mockMvc.perform(post("/api/posts/{postId}/comments", 100L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_ALREADY_DELETED"));

        Long commentCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM comments",
                Long.class
        );
        assertThat(commentCount).isEqualTo(0);
    }

    /**
     * 집단지성 생성 실패 테스트:
     * 존재하지 않는 게시물에 대해 집단지성 생성 요청을 보내고,
     * - (1) 응답 status가 404 Not Found인지 확인
     * - (2) DB에 집단지성 row가 생성되지 않았는지 확인
     */
    @Test
    void createReturns404WhenPostDoesNotExists() throws Exception{
        stubAuthenticatedUser();
        insertUser(1L, "comment writer", null);

        String requestBody = """
                {
                    "parentId" : null,
                    "content" : "content"
                }
                """;
        mockMvc.perform(post("/api/posts/{postId}/comments", 100L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));

        Long commentCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM comments",
                Long.class
        );
        assertThat(commentCount).isEqualTo(0);
    }

    /**
     * 집단지성 생성 실패 테스트:
     * 존재하지 않는 집단지성 하위에 집단지성 생성 요청을 보내고,
     * - (1) 응답 status가 404 Not Found인지 확인
     * - (2) DB에 집단지성 row가 생성되지 않았는지 확인
     */
    @Test
    void createReturns404WhenParentDoesNotExists() throws Exception{
        stubAuthenticatedUser();
        insertUser(1L, "comment writer", null);

        LocalDateTime now = LocalDateTime.now();
        insertPost(100L, 1L, "USED_TRADE", "title", "content", false, now);

        String requestBody = """
                {
                    "parentId" : 1000,
                    "content" : "content"
                }
                """;
        mockMvc.perform(post("/api/posts/{postId}/comments", 100L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PARENT_NOT_FOUND"));

        Long commentCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM comments",
                Long.class
        );
        assertThat(commentCount).isEqualTo(0);
    }

    /**
     * 집단지성 생성 실패 테스트:
     * 삭제된 부모 집단지성 하위에 집단지성 생성 요청을 보내고,
     * - (1) 응답 status가 404 Not Found인지 확인
     * - (2) DB에 집단지성 row가 생성되지 않았는지 확인
     */
    @Test
    void createReturns404WhenParentAlreadyDeleted() throws Exception{
        stubAuthenticatedUser();
        insertUser(1L, "comment writer", null);

        LocalDateTime now = LocalDateTime.now();
        insertPost(100L, 1L, "USED_TRADE", "title", "content", false, now);
        insertComment(1000L, 100L, 1L, null, 0, "parent content", now, true);

        String requestBody = """
                {
                    "parentId" : 1000,
                    "content" : "content"
                }
                """;
        mockMvc.perform(post("/api/posts/{postId}/comments", 100L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PARENT_ALREADY_DELETED"));

        Long commentCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM comments WHERE is_deleted = false",
                Long.class
        );
        assertThat(commentCount).isEqualTo(0);
    }

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

    /**
     * 집단지성 생성 실패 테스트:
     * depth 1인 집단지성에 대해 본문 길이가 300을 초과하는 집단지성 생성 요청을 보내고,
     * - (1) 응답 status가 400 Bad Request인지 확인
     * - (2) DB에 집단지성 row가 생성되지 않았는지 확인
     */
    @Test
    void createReturns400WhenContentLengthExceeds300AndDepthExceeds1() throws Exception{
        stubAuthenticatedUser();
        insertUser(1L, "writer", null);

        LocalDateTime now = LocalDateTime.now();
        insertPost(100L, 1L, "USED_TRADE", "title", "content", false, now);
        insertComment(1000L, 100L, 1L, null, 0, "parent content", now, false);
        insertComment(1001L, 100L, 1L, 1000L, 1, "child content", now, false);

        String longContent = "a".repeat(301);
        String requestBody = """
                {
                    "parentId" : 1001,
                    "content" : "%s"
                }
                """.formatted(longContent);
        mockMvc.perform(post("/api/posts/{postId}/comments", 100L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.errors[0].field").value("content"))
                .andExpect(jsonPath("$.errors[0].code").value("CONTENT_LENGTH_EXCEEDED"))
                .andExpect(jsonPath("$.errors[1].field").value("depth"))
                .andExpect(jsonPath("$.errors[1].code").value("DEPTH_EXCEEDED"));

        Long commentCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM comments WHERE post_id = ?",
                Long.class,
                100L
        );
        assertThat(commentCount).isEqualTo(2);
    }

    /**
     * 집단지성 생성 실패 테스트:
     * depth 1인 집단지성에 대해 본문 길이가 300을 초과하는 집단지성 생성 요청을 보내고,
     * - (1) 응답 status가 400 Bad Request인지 확인
     * - (2) DB에 집단지성 row가 생성되지 않았는지 확인
     */
    @Test
    void createReturns400WhenContentIsEmptyAndDepthExceeds1() throws Exception{
        stubAuthenticatedUser();
        insertUser(1L, "writer", null);

        LocalDateTime now = LocalDateTime.now();
        insertPost(100L, 1L, "USED_TRADE", "title", "content", false, now);
        insertComment(1000L, 100L, 1L, null, 0, "parent content", now, false);
        insertComment(1001L, 100L, 1L, 1000L, 1, "child content", now, false);

        String requestBody = """
                {
                    "parentId" : 1001,
                    "content" : "     "
                }
                """;
        mockMvc.perform(post("/api/posts/{postId}/comments", 100L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.errors[0].field").value("content"))
                .andExpect(jsonPath("$.errors[0].code").value("EMPTY_CONTENT"))
                .andExpect(jsonPath("$.errors[1].field").value("depth"))
                .andExpect(jsonPath("$.errors[1].code").value("DEPTH_EXCEEDED"));

        Long commentCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM comments WHERE post_id = ?",
                Long.class,
                100L
        );
        assertThat(commentCount).isEqualTo(2);
    }

    /**
     * 집단지성 생성 실패 테스트:
     * access token 없이 요청을 보내고,
     * 응답 status가 401 Unauthorized인지 확인
     */
    @Test
    void getMyPostsReturns401WhenAccessTokenIsMissing() throws Exception {
        mockMvc.perform(post("/api/post/{postId}/comments", 100L))
                .andExpect(status().isUnauthorized());
    }

    /**
     * 집단지성 수정 성공 테스트:
     * 작성자가 본인 집단지성 수정 요청을 보내고,
     * - (1) 응답 status가 200 OK인지 확인
     * - (2) 수정된 집단지성 정보가 적절히 반환되는지 확인
     * - (3) DB에 집단지성 내용이 수정되었는지 확인
     */
    @Test
    void updateReturns200WhenCommentIsUpdated() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "comment writer", null);
        insertUser(2L, "post writer", null);

        LocalDateTime now = LocalDateTime.now();
        insertPost(100L, 2L, "USED_TRADE", "title", "content", false, now);
        insertComment(1000L, 100L, 1L, null, 0, "old content", now, false);

        String requestBody = """
                {
                    "content" : "new content"
                }
                """;
        mockMvc.perform(patch("/api/posts/{postId}/comments/{commentId}", 100L, 1000L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentId").value(1000L))
                .andExpect(jsonPath("$.content").value("new content"))
                .andExpect(jsonPath("$.isMine").value(true))
                .andExpect(jsonPath("$.writer.nickname").value("comment writer"))
                .andExpect(jsonPath("$.writer.profileImageUrl").value(nullValue()))
                .andExpect(jsonPath("$.writer.isPostWriter").value(false))
                .andExpect(jsonPath("$.createdAt").value(notNullValue()))
                .andExpect(jsonPath("$.updatedAt").value(notNullValue()))
                .andExpect(jsonPath("$.isHelpful").value(false))
                .andExpect(jsonPath("$.totalHelpfulCount").value(0))
                .andExpect(jsonPath("$.parentId").value(nullValue()))
                .andExpect(jsonPath("$.depth").value(0));

        String savedContent = jdbcTemplate.queryForObject(
                "SELECT content FROM comments WHERE id = ?",
                String.class,
                1000L
        );
        assertThat(savedContent).isEqualTo("new content");
    }

    /**
     * 집단지성 수정 실패 테스트:
     * 작성자가 아닌 유저가 집단지성 수정 요청을 보내고,
     * - (1) 응답 status가 403 Forbidden인지 확인
     * - (2) DB에 집단지성 내용이 수정되지 않았는지 확인
     */
    @Test
    void updateReturns403WhenUserIsNotWriter() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "request user", null);
        insertUser(2L, "comment writer", null);

        LocalDateTime now = LocalDateTime.now();
        insertPost(100L, 2L, "USED_TRADE", "title", "content", false, now);
        insertComment(1000L, 100L, 2L, null, 0, "old content", now, false);

        String requestBody = """
                {
                    "content" : "new content"
                }
                """;
        mockMvc.perform(patch("/api/posts/{postId}/comments/{commentId}", 100L, 1000L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN_ACCESS"));

        String savedContent = jdbcTemplate.queryForObject(
                "SELECT content FROM comments WHERE id = ?",
                String.class,
                1000L
        );
        assertThat(savedContent).isEqualTo("old content");
    }

    /**
     * 집단지성 수정 실패 테스트:
     * 삭제된 게시물의 집단지성 수정 요청을 보내고,
     * - (1) 응답 status가 404 Not Found인지 확인
     * - (2) POST_ALREADY_DELETED 오류 코드를 반환하는지 확인
     */
    @Test
    void updateReturns404WhenPostAlreadyDeleted() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "comment writer", null);

        LocalDateTime now = LocalDateTime.now();
        insertPost(100L, 1L, "USED_TRADE", "title", "content", true, now);
        insertComment(1000L, 100L, 1L, null, 0, "old content", now, false);

        String requestBody = """
                {
                    "content" : "new content"
                }
                """;
        mockMvc.perform(patch("/api/posts/{postId}/comments/{commentId}", 100L, 1000L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_ALREADY_DELETED"));
    }

    /**
     * 집단지성 수정 실패 테스트:
     * 존재하지 않는 게시물의 집단지성 수정 요청을 보내고,
     * - (1) 응답 status가 404 Not Found인지 확인
     * - (2) POST_NOT_FOUND 오류 코드를 반환하는지 확인
     */
    @Test
    void updateReturns404WhenPostDoesNotExist() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "comment writer", null);

        String requestBody = """
                {
                    "content" : "new content"
                }
                """;
        mockMvc.perform(patch("/api/posts/{postId}/comments/{commentId}", 100L, 1000L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
    }

    /**
     * 집단지성 수정 실패 테스트:
     * 삭제된 집단지성 수정 요청을 보내고,
     * - (1) 응답 status가 404 Not Found인지 확인
     * - (2) COMMENT_ALREADY_DELETED 오류 코드를 반환하는지 확인
     */
    @Test
    void updateReturns404WhenCommentAlreadyDeleted() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "comment writer", null);

        LocalDateTime now = LocalDateTime.now();
        insertPost(100L, 1L, "USED_TRADE", "title", "content", false, now);
        insertComment(1000L, 100L, 1L, null, 0, "old content", now, true);

        String requestBody = """
                {
                    "content" : "new content"
                }
                """;
        mockMvc.perform(patch("/api/posts/{postId}/comments/{commentId}", 100L, 1000L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMENT_ALREADY_DELETED"));
    }

    /**
     * 집단지성 수정 실패 테스트:
     * 존재하지 않는 집단지성 수정 요청을 보내고,
     * - (1) 응답 status가 404 Not Found인지 확인
     * - (2) COMMENT_NOT_FOUND 오류 코드를 반환하는지 확인
     */
    @Test
    void updateReturns404WhenCommentDoesNotExist() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "comment writer", null);

        LocalDateTime now = LocalDateTime.now();
        insertPost(100L, 1L, "USED_TRADE", "title", "content", false, now);

        String requestBody = """
                {
                    "content" : "new content"
                }
                """;
        mockMvc.perform(patch("/api/posts/{postId}/comments/{commentId}", 100L, 1000L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"));
    }

    /**
     * 집단지성 수정 실패 테스트:
     * 요청 body 없이 수정 요청을 보내고,
     * - (1) 응답 status가 400 Bad Request인지 확인
     * - (2) EMPTY_REQUEST_BODY 오류 코드를 반환하는지 확인
     */
    @Test
    void updateReturns400WhenRequestBodyIsEmpty() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "comment writer", null);

        LocalDateTime now = LocalDateTime.now();
        insertPost(100L, 1L, "USED_TRADE", "title", "content", false, now);
        insertComment(1000L, 100L, 1L, null, 0, "old content", now, false);

        mockMvc.perform(patch("/api/posts/{postId}/comments/{commentId}", 100L, 1000L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EMPTY_REQUEST_BODY"));
    }

    /**
     * 집단지성 수정 실패 테스트:
     * 비어있는 본문으로 수정 요청을 보내고,
     * - (1) 응답 status가 400 Bad Request인지 확인
     * - (2) EMPTY_CONTENT 오류 코드를 반환하는지 확인
     */
    @Test
    void updateReturns400WhenContentIsEmpty() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "comment writer", null);

        LocalDateTime now = LocalDateTime.now();
        insertPost(100L, 1L, "USED_TRADE", "title", "content", false, now);
        insertComment(1000L, 100L, 1L, null, 0, "old content", now, false);

        String requestBody = """
                {
                    "content" : "     "
                }
                """;
        mockMvc.perform(patch("/api/posts/{postId}/comments/{commentId}", 100L, 1000L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EMPTY_CONTENT"));
    }

    /**
     * 집단지성 수정 실패 테스트:
     * 본문 길이가 300자를 초과하는 수정 요청을 보내고,
     * - (1) 응답 status가 400 Bad Request인지 확인
     * - (2) CONTENT_LENGTH_EXCEEDED 오류 코드를 반환하는지 확인
     */
    @Test
    void updateReturns400WhenContentLengthExceeds300() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "comment writer", null);

        LocalDateTime now = LocalDateTime.now();
        insertPost(100L, 1L, "USED_TRADE", "title", "content", false, now);
        insertComment(1000L, 100L, 1L, null, 0, "old content", now, false);

        String requestBody = """
                {
                    "content" : "%s"
                }
                """.formatted("a".repeat(301));
        mockMvc.perform(patch("/api/posts/{postId}/comments/{commentId}", 100L, 1000L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CONTENT_LENGTH_EXCEEDED"));
    }

    /**
     * 집단지성 수정 실패 테스트:
     * access token 없이 요청을 보내고,
     * 응답 status가 401 Unauthorized인지 확인
     */
    @Test
    void updateReturns401WhenAccessTokenIsMissing() throws Exception {
        mockMvc.perform(patch("/api/posts/{postId}/comments/{commentId}", 100L, 1000L))
                .andExpect(status().isUnauthorized());
    }

    /**
     * 집단지성 조회 성공 테스트:
     * 집단지성이 하나도 없는 게시물에 대해 비회원이 조회 요청을 보내고,
     * - (1) 응답 status가 200 OK인지 확인
     * - (2) comments가 빈 배열인지 확인
     * - (3) hasNext가 false이고 nextCursor가 null인지 확인
     */
    @Test
    void readReturns200WhenCommentsAreEmpty() throws Exception {
        stubUnauthenticatedUser();
        insertUser(1L, "post writer", null);

        LocalDateTime now = LocalDateTime.now();
        insertPost(100L, 1L, "USED_TRADE", "title", "content", false, now);

        mockMvc.perform(get("/api/posts/{postId}/comments", 100L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments.length()").value(0))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.nextCursor").value(nullValue()));
    }

    /**
     * 집단지성 조회 성공 테스트:
     * 집단지성이 5개 이하인 게시물에 대해 비회원이 조회 요청을 보내고,
     * - (1) 응답 status가 200 OK인지 확인
     * - (2) 오래된 순으로 집단지성이 반환되는지 확인
     * - (3) hasNext가 false이고 nextCursor가 null인지 확인
     */
    @Test
    void readReturns200WhenCommentCountIsLessThanOrEqualTo5() throws Exception {
        stubUnauthenticatedUser();
        insertUser(1L, "post writer", null);
        insertUser(2L, "comment writer", null);

        LocalDateTime now = LocalDateTime.now();
        insertPost(100L, 1L, "USED_TRADE", "title", "content", false, now);
        insertComment(1000L, 100L, 2L, null, 0, "content 1", now.minusMinutes(3), false);
        insertComment(1001L, 100L, 2L, null, 0, "content 2", now.minusMinutes(2), false);
        insertComment(1002L, 100L, 2L, null, 0, "content 3", now.minusMinutes(1), false);

        mockMvc.perform(get("/api/posts/{postId}/comments", 100L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments.length()").value(3))
                .andExpect(jsonPath("$.comments[0].commentId").value(1000L))
                .andExpect(jsonPath("$.comments[0].isMine").value(false))
                .andExpect(jsonPath("$.comments[0].writer.isPostWriter").value(false))
                .andExpect(jsonPath("$.comments[0].isHelpful").value(false))
                .andExpect(jsonPath("$.comments[0].totalHelpfulCount").value(0))
                .andExpect(jsonPath("$.comments[2].commentId").value(1002L))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.nextCursor").value(nullValue()));
    }

    /**
     * 집단지성 조회 성공 테스트:
     * 집단지성이 pageSize보다 많은 게시물에 대해 비회원이 조회 요청을 보내고,
     * - (1) 응답 status가 200 OK인지 확인
     * - (2) pageSize만큼만 집단지성이 반환되는지 확인
     * - (3) hasNext가 true이고 nextCursor가 null이 아닌지 확인
     */
    @Test
    void readReturns200WhenCommentCountExceedsPageSize() throws Exception {
        stubUnauthenticatedUser();
        insertUser(1L, "post writer", null);
        insertUser(2L, "comment writer", null);

        LocalDateTime now = LocalDateTime.now();
        insertPost(100L, 1L, "USED_TRADE", "title", "content", false, now);
        insertComment(1000L, 100L, 2L, null, 0, "content 1", now.minusMinutes(6), false);
        insertComment(1001L, 100L, 2L, null, 0, "content 2", now.minusMinutes(5), false);
        insertComment(1002L, 100L, 2L, null, 0, "content 3", now.minusMinutes(4), false);
        insertComment(1003L, 100L, 2L, null, 0, "content 4", now.minusMinutes(3), false);
        insertComment(1004L, 100L, 2L, null, 0, "content 5", now.minusMinutes(2), false);
        insertComment(1005L, 100L, 2L, null, 0, "content 6", now.minusMinutes(1), false);

        mockMvc.perform(get("/api/posts/{postId}/comments", 100L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments.length()").value(5))
                .andExpect(jsonPath("$.comments[0].commentId").value(1000L))
                .andExpect(jsonPath("$.comments[4].commentId").value(1004L))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.nextCursor").isNotEmpty());
    }

    /**
     * 집단지성 조회 성공 테스트:
     * 비회원이 첫 페이지에서 받은 nextCursor로 두 번째 조회 요청을 보내고,
     * - (1) 응답 status가 200 OK인지 확인
     * - (2) 다음 페이지의 집단지성만 반환되는지 확인
     * - (3) hasNext가 false이고 nextCursor가 null인지 확인
     */
    @Test
    void readReturns200WhenCursorIsValidForNextPageRequest() throws Exception {
        stubUnauthenticatedUser();
        insertUser(1L, "post writer", null);
        insertUser(2L, "comment writer", null);

        LocalDateTime now = LocalDateTime.now();
        insertPost(100L, 1L, "USED_TRADE", "title", "content", false, now);
        insertComment(1000L, 100L, 2L, null, 0, "content 1", now.minusMinutes(6), false);
        insertComment(1001L, 100L, 2L, null, 0, "content 2", now.minusMinutes(5), false);
        insertComment(1002L, 100L, 2L, null, 0, "content 3", now.minusMinutes(4), false);
        insertComment(1003L, 100L, 2L, null, 0, "content 4", now.minusMinutes(3), false);
        insertComment(1004L, 100L, 2L, null, 0, "content 5", now.minusMinutes(2), false);
        insertComment(1005L, 100L, 2L, null, 0, "content 6", now.minusMinutes(1), false);

        String responseBody = mockMvc.perform(get("/api/posts/{postId}/comments", 100L))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String nextCursor = JsonPath.read(responseBody, "$.nextCursor");

        mockMvc.perform(get("/api/posts/{postId}/comments", 100L)
                        .param("cursor", nextCursor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments.length()").value(1))
                .andExpect(jsonPath("$.comments[0].commentId").value(1005L))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.nextCursor").value(nullValue()));
    }

    /**
     * 집단지성 조회 실패 테스트:
     * 삭제된 게시물에 대해 비회원이 조회 요청을 보내고,
     * - (1) 응답 status가 404 Not Found인지 확인
     * - (2) POST_ALREADY_DELETED 오류 코드를 반환하는지 확인
     */
    @Test
    void readReturns404WhenPostAlreadyDeleted() throws Exception {
        stubUnauthenticatedUser();
        insertUser(1L, "post writer", null);

        LocalDateTime now = LocalDateTime.now();
        insertPost(100L, 1L, "USED_TRADE", "title", "content", true, now);

        mockMvc.perform(get("/api/posts/{postId}/comments", 100L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_ALREADY_DELETED"));
    }

    /**
     * 집단지성 조회 실패 테스트:
     * 존재하지 않는 게시물에 대해 비회원이 조회 요청을 보내고,
     * - (1) 응답 status가 404 Not Found인지 확인
     * - (2) POST_NOT_FOUND 오류 코드를 반환하는지 확인
     */
    @Test
    void readReturns404WhenPostDoesNotExist() throws Exception {
        stubUnauthenticatedUser();

        mockMvc.perform(get("/api/posts/{postId}/comments", 100L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
    }

    /**
     * 집단지성 조회 실패 테스트:
     * 유효하지 않은 정렬 기준과 cursor로 비회원이 조회 요청을 보내고,
     * - (1) 응답 status가 400 Bad Request인지 확인
     * - (2) INVALID_INPUT_VALUE 오류 코드를 반환하는지 확인
     * - (3) sortBy, cursor 오류가 함께 반환되는지 확인
     */
    @Test
    void readReturns400WhenSortByAndCursorAreInvalid() throws Exception {
        stubUnauthenticatedUser();
        insertUser(1L, "post writer", null);

        LocalDateTime now = LocalDateTime.now();
        insertPost(100L, 1L, "USED_TRADE", "title", "content", false, now);

        mockMvc.perform(get("/api/posts/{postId}/comments", 100L)
                        .param("sortBy", "INVALID")
                        .param("cursor", "invalid-cursor"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.errors[0].field").value("sortBy"))
                .andExpect(jsonPath("$.errors[0].code").value("INVALID_SORTING"))
                .andExpect(jsonPath("$.errors[1].field").value("cursor"))
                .andExpect(jsonPath("$.errors[1].code").value("INVALID_CURSOR"));
    }

    /**
     * 집단지성 조회 실패 테스트:
     * 다중 정렬 기준과 유효하지 않은 cursor로 비회원이 조회 요청을 보내고,
     * - (1) 응답 status가 400 Bad Request인지 확인
     * - (2) INVALID_INPUT_VALUE 오류 코드를 반환하는지 확인
     * - (3) sortBy, cursor 오류가 함께 반환되는지 확인
     */
    @Test
    void readReturns400WhenSortByIsMultipleAndCursorIsInvalid() throws Exception {
        stubUnauthenticatedUser();
        insertUser(1L, "post writer", null);

        LocalDateTime now = LocalDateTime.now();
        insertPost(100L, 1L, "USED_TRADE", "title", "content", false, now);

        mockMvc.perform(get("/api/posts/{postId}/comments", 100L)
                        .param("sortBy", "OLDEST,LATEST")
                        .param("cursor", "invalid-cursor"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.errors[0].field").value("sortBy"))
                .andExpect(jsonPath("$.errors[0].code").value("MULTIPLE_SORTING"))
                .andExpect(jsonPath("$.errors[1].field").value("cursor"))
                .andExpect(jsonPath("$.errors[1].code").value("INVALID_CURSOR"));
    }

    /**
     * 집단지성 조회 성공 테스트:
     * access token 없이 요청을 보내고,
     * 응답 status가 200 OK인지 확인
     */
    @Test
    void readReturns200WhenAccessTokenIsMissing() throws Exception {
        stubUnauthenticatedUser();
        insertUser(1L, "post writer", null);

        LocalDateTime now = LocalDateTime.now();
        insertPost(100L, 1L, "USED_TRADE", "title", "content", false, now);

        mockMvc.perform(get("/api/posts/{postId}/comments", 100L))
                .andExpect(status().isOk());
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

    private void stubUnauthenticatedUser() {
        when(jwtProvider.validateAccessToken(null))
                .thenReturn(JwtProvider.TokenValidationResult.EMPTY);
    }
}
