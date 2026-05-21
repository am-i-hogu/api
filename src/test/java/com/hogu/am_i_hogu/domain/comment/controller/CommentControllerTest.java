package com.hogu.am_i_hogu.domain.comment.controller;

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
import com.jayway.jsonpath.JsonPath;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
        jdbcTemplate.update("DELETE FROM comment_helpful_marks");
        jdbcTemplate.update("DELETE FROM comments");
        jdbcTemplate.update("DELETE FROM posts");
        jdbcTemplate.update("DELETE FROM users");
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
    }

    /**
     * 집단지성 생성 성공 테스트:
     * 게시물 작성자가 아닌 유저가 게시물에 첫 집단지성 생성 요청을 보내고,
     * - (1) 응답 status가 201 Created인지 확인
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
                .andExpect(status().isCreated())
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
     * - (1) 응답 status가 201 Created인지 확인
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
                .andExpect(status().isCreated())
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
     * - (1) 응답 status가 201 Created인지 확인
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
                .andExpect(status().isCreated())
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
     * 다른 게시물에 속한 부모 집단지성 하위에 집단지성 생성 요청을 보내고,
     * - (1) 응답 status가 404 Not Found인지 확인
     * - (2) PARENT_NOT_FOUND 오류 코드를 반환하는지 확인
     * - (3) DB에 집단지성 row가 생성되지 않았는지 확인
     */
    @Test
    void createReturns404WhenParentDoesNotBelongToPost() throws Exception{
        stubAuthenticatedUser();
        insertUser(1L, "comment writer", null);

        LocalDateTime now = LocalDateTime.now();
        insertPost(100L, 1L, "USED_TRADE", "title 1", "content 1", false, now);
        insertPost(101L, 1L, "USED_TRADE", "title 2", "content 2", false, now);
        insertComment(1000L, 101L, 1L, null, 0, "parent content", now, false);

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
                "SELECT COUNT(*) FROM comments WHERE post_id = ?",
                Long.class,
                100L
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
     * 다른 게시물에 속한 집단지성 수정 요청을 보내고,
     * - (1) 응답 status가 404 Not Found인지 확인
     * - (2) COMMENT_NOT_FOUND 오류 코드를 반환하는지 확인
     * - (3) DB에 집단지성 내용이 수정되지 않았는지 확인
     */
    @Test
    void updateReturns404WhenCommentDoesNotBelongToPost() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "comment writer", null);

        LocalDateTime now = LocalDateTime.now();
        insertPost(100L, 1L, "USED_TRADE", "title 1", "content 1", false, now);
        insertPost(101L, 1L, "USED_TRADE", "title 2", "content 2", false, now);
        insertComment(1000L, 101L, 1L, null, 0, "old content", now, false);

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

        String savedContent = jdbcTemplate.queryForObject(
                "SELECT content FROM comments WHERE id = ?",
                String.class,
                1000L
        );
        assertThat(savedContent).isEqualTo("old content");
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
     * 집단지성 삭제 성공 테스트:
     * 작성자가 본인 집단지성 삭제 요청을 보내고,
     * - (1) 응답 status가 204 No Content인지 확인
     * - (2) DB에 집단지성이 soft delete 처리되었는지 확인
     */
    @Test
    void deleteReturns204WhenCommentIsDeleted() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "comment writer", null);

        LocalDateTime now = LocalDateTime.now();
        insertPost(100L, 1L, "USED_TRADE", "title", "content", false, now);
        insertComment(1000L, 100L, 1L, null, 0, "content", now, false);

        mockMvc.perform(delete("/api/posts/{postId}/comments/{commentId}", 100L, 1000L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isNoContent());

        Boolean isDeleted = jdbcTemplate.queryForObject(
                "SELECT is_deleted FROM comments WHERE id = ?",
                Boolean.class,
                1000L
        );
        assertThat(isDeleted).isEqualTo(true);
    }

    /**
     * 집단지성 삭제 실패 테스트:
     * 작성자가 아닌 유저가 집단지성 삭제 요청을 보내고,
     * - (1) 응답 status가 403 Forbidden인지 확인
     * - (2) DB에 집단지성이 삭제되지 않았는지 확인
     */
    @Test
    void deleteReturns403WhenUserIsNotWriter() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "request user", null);
        insertUser(2L, "comment writer", null);

        LocalDateTime now = LocalDateTime.now();
        insertPost(100L, 2L, "USED_TRADE", "title", "content", false, now);
        insertComment(1000L, 100L, 2L, null, 0, "content", now, false);

        mockMvc.perform(delete("/api/posts/{postId}/comments/{commentId}", 100L, 1000L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN_ACCESS"));

        Boolean isDeleted = jdbcTemplate.queryForObject(
                "SELECT is_deleted FROM comments WHERE id = ?",
                Boolean.class,
                1000L
        );
        assertThat(isDeleted).isEqualTo(false);
    }

    /**
     * 집단지성 삭제 실패 테스트:
     * 삭제된 게시물의 집단지성 삭제 요청을 보내고,
     * - (1) 응답 status가 404 Not Found인지 확인
     * - (2) POST_ALREADY_DELETED 오류 코드를 반환하는지 확인
     */
    @Test
    void deleteReturns404WhenPostAlreadyDeleted() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "comment writer", null);

        LocalDateTime now = LocalDateTime.now();
        insertPost(100L, 1L, "USED_TRADE", "title", "content", true, now);
        insertComment(1000L, 100L, 1L, null, 0, "content", now, false);

        mockMvc.perform(delete("/api/posts/{postId}/comments/{commentId}", 100L, 1000L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_ALREADY_DELETED"));
    }

    /**
     * 집단지성 삭제 실패 테스트:
     * 존재하지 않는 게시물의 집단지성 삭제 요청을 보내고,
     * - (1) 응답 status가 404 Not Found인지 확인
     * - (2) POST_NOT_FOUND 오류 코드를 반환하는지 확인
     */
    @Test
    void deleteReturns404WhenPostDoesNotExist() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "comment writer", null);

        mockMvc.perform(delete("/api/posts/{postId}/comments/{commentId}", 100L, 1000L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
    }

    /**
     * 집단지성 삭제 실패 테스트:
     * 삭제된 집단지성 삭제 요청을 보내고,
     * - (1) 응답 status가 404 Not Found인지 확인
     * - (2) COMMENT_ALREADY_DELETED 오류 코드를 반환하는지 확인
     */
    @Test
    void deleteReturns404WhenCommentAlreadyDeleted() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "comment writer", null);

        LocalDateTime now = LocalDateTime.now();
        insertPost(100L, 1L, "USED_TRADE", "title", "content", false, now);
        insertComment(1000L, 100L, 1L, null, 0, "content", now, true);

        mockMvc.perform(delete("/api/posts/{postId}/comments/{commentId}", 100L, 1000L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMENT_ALREADY_DELETED"));
    }

    /**
     * 집단지성 삭제 실패 테스트:
     * 존재하지 않는 집단지성 삭제 요청을 보내고,
     * - (1) 응답 status가 404 Not Found인지 확인
     * - (2) COMMENT_NOT_FOUND 오류 코드를 반환하는지 확인
     */
    @Test
    void deleteReturns404WhenCommentDoesNotExist() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "comment writer", null);

        LocalDateTime now = LocalDateTime.now();
        insertPost(100L, 1L, "USED_TRADE", "title", "content", false, now);

        mockMvc.perform(delete("/api/posts/{postId}/comments/{commentId}", 100L, 1000L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"));
    }

    /**
     * 집단지성 삭제 실패 테스트:
     * 다른 게시물에 속한 집단지성 삭제 요청을 보내고,
     * - (1) 응답 status가 404 Not Found인지 확인
     * - (2) COMMENT_NOT_FOUND 오류 코드를 반환하는지 확인
     * - (3) DB에 집단지성이 삭제되지 않았는지 확인
     */
    @Test
    void deleteReturns404WhenCommentDoesNotBelongToPost() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "comment writer", null);

        LocalDateTime now = LocalDateTime.now();
        insertPost(100L, 1L, "USED_TRADE", "title 1", "content 1", false, now);
        insertPost(101L, 1L, "USED_TRADE", "title 2", "content 2", false, now);
        insertComment(1000L, 101L, 1L, null, 0, "content", now, false);

        mockMvc.perform(delete("/api/posts/{postId}/comments/{commentId}", 100L, 1000L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"));

        Boolean isDeleted = jdbcTemplate.queryForObject(
                "SELECT is_deleted FROM comments WHERE id = ?",
                Boolean.class,
                1000L
        );
        assertThat(isDeleted).isEqualTo(false);
    }

    /**
     * 집단지성 삭제 실패 테스트:
     * access token 없이 요청을 보내고,
     * 응답 status가 401 Unauthorized인지 확인
     */
    @Test
    void deleteReturns401WhenAccessTokenIsMissing() throws Exception {
        mockMvc.perform(delete("/api/posts/{postId}/comments/{commentId}", 100L, 1000L))
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
                .andExpect(jsonPath("$.comments[0].commentId").value(1002L))
                .andExpect(jsonPath("$.comments[0].isMine").value(false))
                .andExpect(jsonPath("$.comments[0].writer.isPostWriter").value(false))
                .andExpect(jsonPath("$.comments[0].isHelpful").value(false))
                .andExpect(jsonPath("$.comments[0].totalHelpfulCount").value(0))
                .andExpect(jsonPath("$.comments[2].commentId").value(1000L))
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
                .andExpect(jsonPath("$.comments[0].commentId").value(1005L))
                .andExpect(jsonPath("$.comments[4].commentId").value(1001L))
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
                .andExpect(jsonPath("$.comments[0].commentId").value(1000L))
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

    /**
     * 집단지성 조회 성공 테스트:
     * 정렬 기준을 생략하고 비회원이 조회 요청을 보내고,
     * - (1) 응답 status가 200 OK인지 확인
     * - (2) 부모 집단지성이 최신순으로 반환되는지 확인
     * - (3) 자식 집단지성은 부모 아래에서 오래된 순으로 반환되는지 확인
     */
    @Test
    void readReturns200WhenSortByIsOmitted() throws Exception {
        stubUnauthenticatedUser();
        insertUser(1L, "post writer", null);
        insertUser(2L, "comment writer", null);

        LocalDateTime now = LocalDateTime.now();
        insertPost(100L, 1L, "USED_TRADE", "title", "content", false, now);
        insertComment(1000L, 100L, 2L, null, 0, "parent 1", now.minusMinutes(3), false);
        insertComment(1001L, 100L, 2L, null, 0, "parent 2", now.minusMinutes(1), false);
        insertComment(1002L, 100L, 2L, 1001L, 1, "child 1", now.minusSeconds(50), false);
        insertComment(1003L, 100L, 2L, 1001L, 1, "child 2", now.minusSeconds(40), false);

        mockMvc.perform(get("/api/posts/{postId}/comments", 100L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments.length()").value(4))
                .andExpect(jsonPath("$.comments[0].commentId").value(1001L))
                .andExpect(jsonPath("$.comments[1].commentId").value(1002L))
                .andExpect(jsonPath("$.comments[2].commentId").value(1003L))
                .andExpect(jsonPath("$.comments[3].commentId").value(1000L));
    }

    /**
     * 집단지성 조회 성공 테스트:
     * 같은 생성 시각의 부모 집단지성들에 대해 최신순 조회 요청을 보내고,
     * - (1) 응답 status가 200 OK인지 확인
     * - (2) commentId가 큰 순으로 반환되는지 확인
     */
    @Test
    void readReturns200WhenSortByIsLatestAndCreatedAtIsSame() throws Exception {
        stubUnauthenticatedUser();
        insertUser(1L, "post writer", null);
        insertUser(2L, "comment writer", null);

        LocalDateTime now = LocalDateTime.now();
        insertPost(100L, 1L, "USED_TRADE", "title", "content", false, now);
        insertComment(1000L, 100L, 2L, null, 0, "parent 1", now, false);
        insertComment(1001L, 100L, 2L, null, 0, "parent 2", now, false);

        mockMvc.perform(get("/api/posts/{postId}/comments", 100L)
                        .param("sortBy", "LATEST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments.length()").value(2))
                .andExpect(jsonPath("$.comments[0].commentId").value(1001L))
                .andExpect(jsonPath("$.comments[1].commentId").value(1000L));
    }

    /**
     * 집단지성 조회 성공 테스트:
     * 부모 집단지성들의 유익해요 수가 다를 때 비회원이 조회 요청을 보내고,
     * - (1) 응답 status가 200 OK인지 확인
     * - (2) 부모 집단지성이 유익해요 많은 순으로 반환되는지 확인
     * - (3) 자식 집단지성은 부모 아래에서 오래된 순으로 반환되는지 확인
     */
    @Test
    void readReturns200WhenSortByIsHelpful() throws Exception {
        stubUnauthenticatedUser();
        insertUser(1L, "post writer", null);
        insertUser(2L, "comment writer", null);
        insertUser(3L, "helpful user 1", null);
        insertUser(4L, "helpful user 2", null);
        insertUser(5L, "helpful user 3", null);

        LocalDateTime now = LocalDateTime.now();
        insertPost(100L, 1L, "USED_TRADE", "title", "content", false, now);
        insertComment(1000L, 100L, 2L, null, 0, "parent 1", now.minusMinutes(3), false);
        insertComment(1001L, 100L, 2L, null, 0, "parent 2", now.minusMinutes(2), false);
        insertComment(1002L, 100L, 2L, 1001L, 1, "child 1", now.minusMinutes(1), false);
        insertComment(1003L, 100L, 2L, 1001L, 1, "child 2", now.minusSeconds(30), false);

        insertCommentHelpfulMark(3L, 1000L, now);
        insertCommentHelpfulMark(3L, 1001L, now);
        insertCommentHelpfulMark(4L, 1001L, now);
        insertCommentHelpfulMark(5L, 1001L, now);

        mockMvc.perform(get("/api/posts/{postId}/comments", 100L)
                        .param("sortBy", "HELPFUL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments.length()").value(4))
                .andExpect(jsonPath("$.comments[0].commentId").value(1001L))
                .andExpect(jsonPath("$.comments[0].totalHelpfulCount").value(3))
                .andExpect(jsonPath("$.comments[1].commentId").value(1002L))
                .andExpect(jsonPath("$.comments[2].commentId").value(1003L))
                .andExpect(jsonPath("$.comments[3].commentId").value(1000L))
                .andExpect(jsonPath("$.comments[3].totalHelpfulCount").value(1));
    }

    /**
     * 집단지성 조회 성공 테스트:
     * 부모 집단지성들의 유익해요 수가 같을 때 비회원이 조회 요청을 보내고,
     * - (1) 응답 status가 200 OK인지 확인
     * - (2) commentId가 큰 순으로 반환되는지 확인
     */
    @Test
    void readReturns200WhenSortByIsHelpfulAndHelpfulCountIsSame() throws Exception {
        stubUnauthenticatedUser();
        insertUser(1L, "post writer", null);
        insertUser(2L, "comment writer", null);
        insertUser(3L, "helpful user", null);

        LocalDateTime now = LocalDateTime.now();
        insertPost(100L, 1L, "USED_TRADE", "title", "content", false, now);
        insertComment(1000L, 100L, 2L, null, 0, "parent 1", now.minusMinutes(2), false);
        insertComment(1001L, 100L, 2L, null, 0, "parent 2", now.minusMinutes(1), false);

        insertCommentHelpfulMark(3L, 1000L, now);
        insertCommentHelpfulMark(3L, 1001L, now);

        mockMvc.perform(get("/api/posts/{postId}/comments", 100L)
                        .param("sortBy", "HELPFUL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments.length()").value(2))
                .andExpect(jsonPath("$.comments[0].commentId").value(1001L))
                .andExpect(jsonPath("$.comments[1].commentId").value(1000L));
    }

    /**
     * 집단지성 조회 성공 테스트:
     * 삭제된 부모 집단지성이 유익해요를 받은 상태에서 비회원이 조회 요청을 보내고,
     * - (1) 응답 status가 200 OK인지 확인
     * - (2) 삭제된 집단지성도 실제 유익해요 수 기준으로 정렬되는지 확인
     * - (3) 삭제된 집단지성의 totalHelpfulCount가 노출되는지 확인
     */
    @Test
    void readReturns200WhenDeletedCommentExists() throws Exception {
        stubUnauthenticatedUser();
        insertUser(1L, "post writer", null);
        insertUser(2L, "comment writer", null);
        insertUser(3L, "helpful user 1", null);
        insertUser(4L, "helpful user 2", null);
        insertUser(5L, "helpful user 3", null);

        LocalDateTime now = LocalDateTime.now();
        insertPost(100L, 1L, "USED_TRADE", "title", "content", false, now);
        insertComment(1000L, 100L, 2L, null, 0, "normal parent", now.minusMinutes(2), false);
        insertComment(1001L, 100L, 2L, null, 0, "deleted parent", now.minusMinutes(1), true);

        insertCommentHelpfulMark(3L, 1000L, now);
        insertCommentHelpfulMark(3L, 1001L, now);
        insertCommentHelpfulMark(4L, 1001L, now);
        insertCommentHelpfulMark(5L, 1001L, now);

        mockMvc.perform(get("/api/posts/{postId}/comments", 100L)
                        .param("sortBy", "HELPFUL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments.length()").value(2))
                .andExpect(jsonPath("$.comments[0].commentId").value(1001L))
                .andExpect(jsonPath("$.comments[0].isDeleted").value(true))
                .andExpect(jsonPath("$.comments[0].content").value(nullValue()))
                .andExpect(jsonPath("$.comments[0].totalHelpfulCount").value(3))
                .andExpect(jsonPath("$.comments[1].commentId").value(1000L))
                .andExpect(jsonPath("$.comments[1].totalHelpfulCount").value(1));
    }

    /**
     * 집단지성 조회 성공 테스트:
     * 비회원이 유익해요 순으로 첫 페이지에서 받은 nextCursor로 두 번째 조회 요청을 보내고,
     * - (1) 응답 status가 200 OK인지 확인
     * - (2) 다음 페이지의 부모 집단지성만 반환되는지 확인
     * - (3) hasNext가 false이고 nextCursor가 null인지 확인
     */
    @Test
    void readReturns200WhenHelpfulCursorIsValidForNextPageRequest() throws Exception {
        stubUnauthenticatedUser();
        insertUser(1L, "post writer", null);
        insertUser(2L, "comment writer", null);
        insertUser(3L, "helpful user 1", null);
        insertUser(4L, "helpful user 2", null);
        insertUser(5L, "helpful user 3", null);
        insertUser(6L, "helpful user 4", null);
        insertUser(7L, "helpful user 5", null);
        insertUser(8L, "helpful user 6", null);

        LocalDateTime now = LocalDateTime.now();
        insertPost(100L, 1L, "USED_TRADE", "title", "content", false, now);
        insertComment(1000L, 100L, 2L, null, 0, "parent 1", now.minusMinutes(6), false);
        insertComment(1001L, 100L, 2L, null, 0, "parent 2", now.minusMinutes(5), false);
        insertComment(1002L, 100L, 2L, null, 0, "parent 3", now.minusMinutes(4), false);
        insertComment(1003L, 100L, 2L, null, 0, "parent 4", now.minusMinutes(3), false);
        insertComment(1004L, 100L, 2L, null, 0, "parent 5", now.minusMinutes(2), false);
        insertComment(1005L, 100L, 2L, null, 0, "parent 6", now.minusMinutes(1), false);

        insertCommentHelpfulMark(3L, 1000L, now);
        insertCommentHelpfulMark(3L, 1001L, now);
        insertCommentHelpfulMark(4L, 1001L, now);
        insertCommentHelpfulMark(3L, 1002L, now);
        insertCommentHelpfulMark(4L, 1002L, now);
        insertCommentHelpfulMark(5L, 1002L, now);
        insertCommentHelpfulMark(3L, 1003L, now);
        insertCommentHelpfulMark(4L, 1003L, now);
        insertCommentHelpfulMark(5L, 1003L, now);
        insertCommentHelpfulMark(6L, 1003L, now);
        insertCommentHelpfulMark(3L, 1004L, now);
        insertCommentHelpfulMark(4L, 1004L, now);
        insertCommentHelpfulMark(5L, 1004L, now);
        insertCommentHelpfulMark(6L, 1004L, now);
        insertCommentHelpfulMark(7L, 1004L, now);
        insertCommentHelpfulMark(3L, 1005L, now);
        insertCommentHelpfulMark(4L, 1005L, now);
        insertCommentHelpfulMark(5L, 1005L, now);
        insertCommentHelpfulMark(6L, 1005L, now);
        insertCommentHelpfulMark(7L, 1005L, now);
        insertCommentHelpfulMark(8L, 1005L, now);

        String responseBody = mockMvc.perform(get("/api/posts/{postId}/comments", 100L)
                        .param("sortBy", "HELPFUL"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String nextCursor = JsonPath.read(responseBody, "$.nextCursor");

        mockMvc.perform(get("/api/posts/{postId}/comments", 100L)
                        .param("sortBy", "HELPFUL")
                        .param("cursor", nextCursor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments.length()").value(1))
                .andExpect(jsonPath("$.comments[0].commentId").value(1000L))
                .andExpect(jsonPath("$.comments[0].totalHelpfulCount").value(1))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.nextCursor").value(nullValue()));
    }

    /**
     * 집단지성 조회 성공 테스트:
     * 회원이 조회 요청을 보내고,
     * - (1) 응답 status가 200 OK인지 확인
     * - (2) 본인이 작성한 집단지성의 isMine이 true인지 확인
     * - (3) 유익해요를 누른 집단지성의 isHelpful이 true인지 확인
     */
    @Test
    void readReturns200WhenAuthenticatedUserRequestsComments() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "comment writer", null);
        insertUser(2L, "post writer", null);
        insertUser(3L, "another writer", null);

        LocalDateTime now = LocalDateTime.now();
        insertPost(100L, 2L, "USED_TRADE", "title", "content", false, now);
        insertComment(1000L, 100L, 1L, null, 0, "my comment", now.minusMinutes(2), false);
        insertComment(1001L, 100L, 3L, null, 0, "other comment", now.minusMinutes(1), false);
        insertCommentHelpfulMark(1L, 1001L, now);

        mockMvc.perform(get("/api/posts/{postId}/comments", 100L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments.length()").value(2))
                .andExpect(jsonPath("$.comments[0].commentId").value(1001L))
                .andExpect(jsonPath("$.comments[0].isMine").value(false))
                .andExpect(jsonPath("$.comments[0].isHelpful").value(true))
                .andExpect(jsonPath("$.comments[1].commentId").value(1000L))
                .andExpect(jsonPath("$.comments[1].isMine").value(true))
                .andExpect(jsonPath("$.comments[1].isHelpful").value(false));
    }

    /**
     * 집단지성 생성 실패 테스트:
     * postId 타입이 잘못된 요청을 보내고,
     * - (1) 응답 status가 400 Bad Request인지 확인
     * - (2) WRONG_POSTID_TYPE 오류 코드를 반환하는지 확인
     */
    @Test
    void createReturns400WhenPostIdTypeIsWrong() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "comment writer", null);

        String requestBody = """
                {
                    "parentId" : null,
                    "content" : "content"
                }
                """;
        mockMvc.perform(post("/api/posts/{postId}/comments", "abc")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WRONG_POSTID_TYPE"));
    }

    /**
     * 집단지성 생성 실패 테스트:
     * parentId 타입이 잘못된 요청을 보내고,
     * - (1) 응답 status가 400 Bad Request인지 확인
     * - (2) WRONG_PARENTID_TYPE 오류 코드를 반환하는지 확인
     */
    @Test
    void createReturns400WhenParentIdTypeIsWrong() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "comment writer", null);

        LocalDateTime now = LocalDateTime.now();
        insertPost(100L, 1L, "USED_TRADE", "title", "content", false, now);

        String requestBody = """
                {
                    "parentId" : "abc",
                    "content" : "content"
                }
                """;
        mockMvc.perform(post("/api/posts/{postId}/comments", 100L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WRONG_PARENTID_TYPE"));
    }

    /**
     * 집단지성 수정 실패 테스트:
     * postId 타입이 잘못된 요청을 보내고,
     * - (1) 응답 status가 400 Bad Request인지 확인
     * - (2) WRONG_POSTID_TYPE 오류 코드를 반환하는지 확인
     */
    @Test
    void updateReturns400WhenPostIdTypeIsWrong() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "comment writer", null);

        String requestBody = """
                {
                    "content" : "new content"
                }
                """;
        mockMvc.perform(patch("/api/posts/{postId}/comments/{commentId}", "abc", 1000L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WRONG_POSTID_TYPE"));
    }

    /**
     * 집단지성 수정 실패 테스트:
     * commentId 타입이 잘못된 요청을 보내고,
     * - (1) 응답 status가 400 Bad Request인지 확인
     * - (2) WRONG_COMMENTID_TYPE 오류 코드를 반환하는지 확인
     */
    @Test
    void updateReturns400WhenCommentIdTypeIsWrong() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "comment writer", null);

        String requestBody = """
                {
                    "content" : "new content"
                }
                """;
        mockMvc.perform(patch("/api/posts/{postId}/comments/{commentId}", 100L, "abc")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WRONG_COMMENTID_TYPE"));
    }

    /**
     * 집단지성 삭제 실패 테스트:
     * postId 타입이 잘못된 요청을 보내고,
     * - (1) 응답 status가 400 Bad Request인지 확인
     * - (2) WRONG_POSTID_TYPE 오류 코드를 반환하는지 확인
     */
    @Test
    void deleteReturns400WhenPostIdTypeIsWrong() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "comment writer", null);

        mockMvc.perform(delete("/api/posts/{postId}/comments/{commentId}", "abc", 1000L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WRONG_POSTID_TYPE"));
    }

    /**
     * 집단지성 삭제 실패 테스트:
     * commentId 타입이 잘못된 요청을 보내고,
     * - (1) 응답 status가 400 Bad Request인지 확인
     * - (2) WRONG_COMMENTID_TYPE 오류 코드를 반환하는지 확인
     */
    @Test
    void deleteReturns400WhenCommentIdTypeIsWrong() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "comment writer", null);

        mockMvc.perform(delete("/api/posts/{postId}/comments/{commentId}", 100L, "abc")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WRONG_COMMENTID_TYPE"));
    }

    /**
     * 집단지성 조회 실패 테스트:
     * postId 타입이 잘못된 요청을 보내고,
     * - (1) 응답 status가 400 Bad Request인지 확인
     * - (2) WRONG_POSTID_TYPE 오류 코드를 반환하는지 확인
     */
    @Test
    void readReturns400WhenPostIdTypeIsWrong() throws Exception {
        stubUnauthenticatedUser();

        mockMvc.perform(get("/api/posts/{postId}/comments", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WRONG_POSTID_TYPE"));
    }

    /**
     * 유익해요 등록 성공 테스트:
     * 작성자가 아닌 유저가 집단지성에 유익해요 등록 요청을 보내고,
     * - (1) 응답 status가 200 OK인지 확인
     * - (2) 총 유익해요 수와 선택 여부가 적절히 반환되는지 확인
     * - (3) DB에 유익해요 row가 생성되었는지 확인
     */
    @Test
    void createHelpfulReturns200WhenHelpfulIsCreated() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "request user", null);
        insertUser(2L, "post writer", null);
        insertUser(3L, "comment writer", null);

        LocalDateTime now = LocalDateTime.now();
        insertPost(100L, 2L, "USED_TRADE", "title", "content", false, now);
        insertComment(1000L, 100L, 3L, null, 0, "content", now, false);

        mockMvc.perform(post("/api/posts/{postId}/comments/{commentId}/helpful", 100L, 1000L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalHelpfulCount").value(1))
                .andExpect(jsonPath("$.isHelpful").value(true));

        Long helpfulMarkCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM comment_helpful_marks WHERE user_id = ? AND comment_id = ?",
                Long.class,
                1L,
                1000L
        );
        assertThat(helpfulMarkCount).isEqualTo(1);
    }

    /**
     * 유익해요 등록 성공 테스트:
     * 기존 유익해요가 있는 집단지성에 다른 유저가 등록 요청을 보내고,
     * - (1) 응답 status가 200 OK인지 확인
     * - (2) 총 유익해요 수가 누적되어 반환되는지 확인
     * - (3) DB에 유익해요 row가 추가 생성되었는지 확인
     */
    @Test
    void createHelpfulReturns200WhenHelpfulCountAccumulates() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "request user", null);
        insertUser(2L, "post writer", null);
        insertUser(3L, "comment writer", null);
        insertUser(4L, "helpful user 1", null);
        insertUser(5L, "helpful user 2", null);

        LocalDateTime now = LocalDateTime.now();
        insertPost(100L, 2L, "USED_TRADE", "title", "content", false, now);
        insertComment(1000L, 100L, 3L, null, 0, "content", now, false);
        insertCommentHelpfulMark(4L, 1000L, now);
        insertCommentHelpfulMark(5L, 1000L, now);

        mockMvc.perform(post("/api/posts/{postId}/comments/{commentId}/helpful", 100L, 1000L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalHelpfulCount").value(3))
                .andExpect(jsonPath("$.isHelpful").value(true));

        Long helpfulMarkCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM comment_helpful_marks WHERE comment_id = ?",
                Long.class,
                1000L
        );
        assertThat(helpfulMarkCount).isEqualTo(3);
    }

    /**
     * 유익해요 등록 실패 테스트:
     * postId 타입이 잘못된 요청을 보내고,
     * - (1) 응답 status가 400 Bad Request인지 확인
     * - (2) WRONG_POSTID_TYPE 오류 코드를 반환하는지 확인
     */
    @Test
    void createHelpfulReturns400WhenPostIdTypeIsWrong() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "request user", null);

        mockMvc.perform(post("/api/posts/{postId}/comments/{commentId}/helpful", "abc", 1000L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WRONG_POSTID_TYPE"));
    }

    /**
     * 유익해요 등록 실패 테스트:
     * commentId 타입이 잘못된 요청을 보내고,
     * - (1) 응답 status가 400 Bad Request인지 확인
     * - (2) WRONG_COMMENTID_TYPE 오류 코드를 반환하는지 확인
     */
    @Test
    void createHelpfulReturns400WhenCommentIdTypeIsWrong() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "request user", null);

        mockMvc.perform(post("/api/posts/{postId}/comments/{commentId}/helpful", 100L, "abc")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WRONG_COMMENTID_TYPE"));
    }

    /**
     * 유익해요 등록 실패 테스트:
     * 본인 집단지성에 유익해요 등록 요청을 보내고,
     * - (1) 응답 status가 403 Forbidden인지 확인
     * - (2) FORBIDDEN_ACCESS 오류 코드를 반환하는지 확인
     */
    @Test
    void createHelpfulReturns403WhenUserIsCommentWriter() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "comment writer", null);
        insertUser(2L, "post writer", null);

        LocalDateTime now = LocalDateTime.now();
        insertPost(100L, 2L, "USED_TRADE", "title", "content", false, now);
        insertComment(1000L, 100L, 1L, null, 0, "content", now, false);

        mockMvc.perform(post("/api/posts/{postId}/comments/{commentId}/helpful", 100L, 1000L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN_ACCESS"));
    }

    /**
     * 유익해요 등록 실패 테스트:
     * 삭제된 게시물의 집단지성에 유익해요 등록 요청을 보내고,
     * - (1) 응답 status가 404 Not Found인지 확인
     * - (2) POST_ALREADY_DELETED 오류 코드를 반환하는지 확인
     */
    @Test
    void createHelpfulReturns404WhenPostAlreadyDeleted() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "request user", null);
        insertUser(2L, "post writer", null);
        insertUser(3L, "comment writer", null);

        LocalDateTime now = LocalDateTime.now();
        insertPost(100L, 2L, "USED_TRADE", "title", "content", true, now);
        insertComment(1000L, 100L, 3L, null, 0, "content", now, false);

        mockMvc.perform(post("/api/posts/{postId}/comments/{commentId}/helpful", 100L, 1000L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_ALREADY_DELETED"));
    }

    /**
     * 유익해요 등록 실패 테스트:
     * 존재하지 않는 게시물의 집단지성에 유익해요 등록 요청을 보내고,
     * - (1) 응답 status가 404 Not Found인지 확인
     * - (2) POST_NOT_FOUND 오류 코드를 반환하는지 확인
     */
    @Test
    void createHelpfulReturns404WhenPostDoesNotExist() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "request user", null);

        mockMvc.perform(post("/api/posts/{postId}/comments/{commentId}/helpful", 100L, 1000L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
    }

    /**
     * 유익해요 등록 실패 테스트:
     * 삭제된 집단지성에 유익해요 등록 요청을 보내고,
     * - (1) 응답 status가 404 Not Found인지 확인
     * - (2) COMMENT_ALREADY_DELETED 오류 코드를 반환하는지 확인
     */
    @Test
    void createHelpfulReturns404WhenCommentAlreadyDeleted() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "request user", null);
        insertUser(2L, "post writer", null);
        insertUser(3L, "comment writer", null);

        LocalDateTime now = LocalDateTime.now();
        insertPost(100L, 2L, "USED_TRADE", "title", "content", false, now);
        insertComment(1000L, 100L, 3L, null, 0, "content", now, true);

        mockMvc.perform(post("/api/posts/{postId}/comments/{commentId}/helpful", 100L, 1000L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMENT_ALREADY_DELETED"));
    }

    /**
     * 유익해요 등록 실패 테스트:
     * 존재하지 않는 집단지성에 유익해요 등록 요청을 보내고,
     * - (1) 응답 status가 404 Not Found인지 확인
     * - (2) COMMENT_NOT_FOUND 오류 코드를 반환하는지 확인
     */
    @Test
    void createHelpfulReturns404WhenCommentDoesNotExist() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "request user", null);
        insertUser(2L, "post writer", null);

        LocalDateTime now = LocalDateTime.now();
        insertPost(100L, 2L, "USED_TRADE", "title", "content", false, now);

        mockMvc.perform(post("/api/posts/{postId}/comments/{commentId}/helpful", 100L, 1000L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"));
    }

    /**
     * 유익해요 등록 실패 테스트:
     * 다른 게시물에 속한 집단지성에 유익해요 등록 요청을 보내고,
     * - (1) 응답 status가 404 Not Found인지 확인
     * - (2) COMMENT_NOT_FOUND 오류 코드를 반환하는지 확인
     */
    @Test
    void createHelpfulReturns404WhenCommentDoesNotBelongToPost() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "request user", null);
        insertUser(2L, "post writer", null);
        insertUser(3L, "comment writer", null);

        LocalDateTime now = LocalDateTime.now();
        insertPost(100L, 2L, "USED_TRADE", "title 1", "content 1", false, now);
        insertPost(101L, 2L, "USED_TRADE", "title 2", "content 2", false, now);
        insertComment(1000L, 101L, 3L, null, 0, "content", now, false);

        mockMvc.perform(post("/api/posts/{postId}/comments/{commentId}/helpful", 100L, 1000L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"));
    }

    /**
     * 유익해요 등록 실패 테스트:
     * 이미 유익해요가 등록된 집단지성에 다시 요청을 보내고,
     * - (1) 응답 status가 409 Conflict인지 확인
     * - (2) DUPLICATE_REQUEST 오류 코드를 반환하는지 확인
     */
    @Test
    void createHelpfulReturns409WhenHelpfulAlreadyExists() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "request user", null);
        insertUser(2L, "post writer", null);
        insertUser(3L, "comment writer", null);

        LocalDateTime now = LocalDateTime.now();
        insertPost(100L, 2L, "USED_TRADE", "title", "content", false, now);
        insertComment(1000L, 100L, 3L, null, 0, "content", now, false);
        insertCommentHelpfulMark(1L, 1000L, now);

        mockMvc.perform(post("/api/posts/{postId}/comments/{commentId}/helpful", 100L, 1000L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_REQUEST"));
    }

    /**
     * 유익해요 등록 실패 테스트:
     * access token 없이 요청을 보내고,
     * 응답 status가 401 Unauthorized인지 확인
     */
    @Test
    void createHelpfulReturns401WhenAccessTokenIsMissing() throws Exception {
        mockMvc.perform(post("/api/posts/{postId}/comments/{commentId}/helpful", 100L, 1000L))
                .andExpect(status().isUnauthorized());
    }

    /**
     * 유익해요 취소 성공 테스트:
     * 유익해요가 등록된 집단지성에 취소 요청을 보내고,
     * - (1) 응답 status가 200 OK인지 확인
     * - (2) 총 유익해요 수와 선택 여부가 적절히 반환되는지 확인
     * - (3) DB에서 유익해요 row가 삭제되었는지 확인
     */
    @Test
    void deleteHelpfulReturns200WhenHelpfulIsDeleted() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "request user", null);
        insertUser(2L, "post writer", null);
        insertUser(3L, "comment writer", null);

        LocalDateTime now = LocalDateTime.now();
        insertPost(100L, 2L, "USED_TRADE", "title", "content", false, now);
        insertComment(1000L, 100L, 3L, null, 0, "content", now, false);
        insertCommentHelpfulMark(1L, 1000L, now);

        mockMvc.perform(delete("/api/posts/{postId}/comments/{commentId}/helpful", 100L, 1000L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalHelpfulCount").value(0))
                .andExpect(jsonPath("$.isHelpful").value(false));

        Long helpfulMarkCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM comment_helpful_marks WHERE user_id = ? AND comment_id = ?",
                Long.class,
                1L,
                1000L
        );
        assertThat(helpfulMarkCount).isEqualTo(0);
    }

    /**
     * 유익해요 취소 성공 테스트:
     * 유익해요가 없는 집단지성에 취소 요청을 보내고,
     * - (1) 응답 status가 200 OK인지 확인
     * - (2) 총 유익해요 수와 선택 여부가 적절히 반환되는지 확인
     */
    @Test
    void deleteHelpfulReturns200WhenHelpfulDoesNotExist() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "request user", null);
        insertUser(2L, "post writer", null);
        insertUser(3L, "comment writer", null);

        LocalDateTime now = LocalDateTime.now();
        insertPost(100L, 2L, "USED_TRADE", "title", "content", false, now);
        insertComment(1000L, 100L, 3L, null, 0, "content", now, false);

        mockMvc.perform(delete("/api/posts/{postId}/comments/{commentId}/helpful", 100L, 1000L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalHelpfulCount").value(0))
                .andExpect(jsonPath("$.isHelpful").value(false));
    }

    /**
     * 유익해요 취소 성공 테스트:
     * 여러 명이 유익해요를 등록한 집단지성에 취소 요청을 보내고,
     * - (1) 응답 status가 200 OK인지 확인
     * - (2) 총 유익해요 수가 1 감소해 반환되는지 확인
     * - (3) DB에서 해당 유저의 유익해요 row만 삭제되었는지 확인
     */
    @Test
    void deleteHelpfulReturns200WhenHelpfulCountDecreases() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "request user", null);
        insertUser(2L, "post writer", null);
        insertUser(3L, "comment writer", null);
        insertUser(4L, "helpful user 1", null);
        insertUser(5L, "helpful user 2", null);

        LocalDateTime now = LocalDateTime.now();
        insertPost(100L, 2L, "USED_TRADE", "title", "content", false, now);
        insertComment(1000L, 100L, 3L, null, 0, "content", now, false);
        insertCommentHelpfulMark(1L, 1000L, now);
        insertCommentHelpfulMark(4L, 1000L, now);
        insertCommentHelpfulMark(5L, 1000L, now);

        mockMvc.perform(delete("/api/posts/{postId}/comments/{commentId}/helpful", 100L, 1000L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalHelpfulCount").value(2))
                .andExpect(jsonPath("$.isHelpful").value(false));

        Long helpfulMarkCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM comment_helpful_marks WHERE comment_id = ?",
                Long.class,
                1000L
        );
        assertThat(helpfulMarkCount).isEqualTo(2);
    }

    /**
     * 유익해요 취소 실패 테스트:
     * postId 타입이 잘못된 요청을 보내고,
     * - (1) 응답 status가 400 Bad Request인지 확인
     * - (2) WRONG_POSTID_TYPE 오류 코드를 반환하는지 확인
     */
    @Test
    void deleteHelpfulReturns400WhenPostIdTypeIsWrong() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "request user", null);

        mockMvc.perform(delete("/api/posts/{postId}/comments/{commentId}/helpful", "abc", 1000L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WRONG_POSTID_TYPE"));
    }

    /**
     * 유익해요 취소 실패 테스트:
     * commentId 타입이 잘못된 요청을 보내고,
     * - (1) 응답 status가 400 Bad Request인지 확인
     * - (2) WRONG_COMMENTID_TYPE 오류 코드를 반환하는지 확인
     */
    @Test
    void deleteHelpfulReturns400WhenCommentIdTypeIsWrong() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "request user", null);

        mockMvc.perform(delete("/api/posts/{postId}/comments/{commentId}/helpful", 100L, "abc")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WRONG_COMMENTID_TYPE"));
    }

    /**
     * 유익해요 취소 성공 테스트:
     * 본인 집단지성에 유익해요 취소 요청을 보내고,
     * - (1) 응답 status가 200 OK인지 확인
     * - (2) 총 유익해요 수와 선택 여부가 적절히 반환되는지 확인
     */
    @Test
    void deleteHelpfulReturns200WhenUserIsCommentWriter() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "comment writer", null);
        insertUser(2L, "post writer", null);

        LocalDateTime now = LocalDateTime.now();
        insertPost(100L, 2L, "USED_TRADE", "title", "content", false, now);
        insertComment(1000L, 100L, 1L, null, 0, "content", now, false);

        mockMvc.perform(delete("/api/posts/{postId}/comments/{commentId}/helpful", 100L, 1000L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalHelpfulCount").value(0))
                .andExpect(jsonPath("$.isHelpful").value(false));
    }

    /**
     * 유익해요 취소 실패 테스트:
     * 삭제된 게시물의 집단지성에 취소 요청을 보내고,
     * - (1) 응답 status가 404 Not Found인지 확인
     * - (2) POST_ALREADY_DELETED 오류 코드를 반환하는지 확인
     */
    @Test
    void deleteHelpfulReturns404WhenHelpfulPostAlreadyDeleted() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "request user", null);
        insertUser(2L, "post writer", null);
        insertUser(3L, "comment writer", null);

        LocalDateTime now = LocalDateTime.now();
        insertPost(100L, 2L, "USED_TRADE", "title", "content", true, now);
        insertComment(1000L, 100L, 3L, null, 0, "content", now, false);

        mockMvc.perform(delete("/api/posts/{postId}/comments/{commentId}/helpful", 100L, 1000L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_ALREADY_DELETED"));
    }

    /**
     * 유익해요 취소 실패 테스트:
     * 존재하지 않는 게시물의 집단지성에 취소 요청을 보내고,
     * - (1) 응답 status가 404 Not Found인지 확인
     * - (2) POST_NOT_FOUND 오류 코드를 반환하는지 확인
     */
    @Test
    void deleteHelpfulReturns404WhenHelpfulPostDoesNotExist() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "request user", null);

        mockMvc.perform(delete("/api/posts/{postId}/comments/{commentId}/helpful", 100L, 1000L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
    }

    /**
     * 유익해요 취소 실패 테스트:
     * 삭제된 집단지성에 취소 요청을 보내고,
     * - (1) 응답 status가 404 Not Found인지 확인
     * - (2) COMMENT_ALREADY_DELETED 오류 코드를 반환하는지 확인
     */
    @Test
    void deleteHelpfulReturns404WhenHelpfulCommentAlreadyDeleted() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "request user", null);
        insertUser(2L, "post writer", null);
        insertUser(3L, "comment writer", null);

        LocalDateTime now = LocalDateTime.now();
        insertPost(100L, 2L, "USED_TRADE", "title", "content", false, now);
        insertComment(1000L, 100L, 3L, null, 0, "content", now, true);

        mockMvc.perform(delete("/api/posts/{postId}/comments/{commentId}/helpful", 100L, 1000L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMENT_ALREADY_DELETED"));
    }

    /**
     * 유익해요 취소 실패 테스트:
     * 존재하지 않는 집단지성에 취소 요청을 보내고,
     * - (1) 응답 status가 404 Not Found인지 확인
     * - (2) COMMENT_NOT_FOUND 오류 코드를 반환하는지 확인
     */
    @Test
    void deleteHelpfulReturns404WhenHelpfulCommentDoesNotExist() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "request user", null);
        insertUser(2L, "post writer", null);

        LocalDateTime now = LocalDateTime.now();
        insertPost(100L, 2L, "USED_TRADE", "title", "content", false, now);

        mockMvc.perform(delete("/api/posts/{postId}/comments/{commentId}/helpful", 100L, 1000L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"));
    }

    /**
     * 유익해요 취소 실패 테스트:
     * 다른 게시물에 속한 집단지성에 취소 요청을 보내고,
     * - (1) 응답 status가 404 Not Found인지 확인
     * - (2) COMMENT_NOT_FOUND 오류 코드를 반환하는지 확인
     */
    @Test
    void deleteHelpfulReturns404WhenHelpfulCommentDoesNotBelongToPost() throws Exception {
        stubAuthenticatedUser();
        insertUser(1L, "request user", null);
        insertUser(2L, "post writer", null);
        insertUser(3L, "comment writer", null);

        LocalDateTime now = LocalDateTime.now();
        insertPost(100L, 2L, "USED_TRADE", "title 1", "content 1", false, now);
        insertPost(101L, 2L, "USED_TRADE", "title 2", "content 2", false, now);
        insertComment(1000L, 101L, 3L, null, 0, "content", now, false);

        mockMvc.perform(delete("/api/posts/{postId}/comments/{commentId}/helpful", 100L, 1000L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"));
    }

    /**
     * 유익해요 취소 실패 테스트:
     * access token 없이 요청을 보내고,
     * 응답 status가 401 Unauthorized인지 확인
     */
    @Test
    void deleteHelpfulReturns401WhenAccessTokenIsMissing() throws Exception {
        mockMvc.perform(delete("/api/posts/{postId}/comments/{commentId}/helpful", 100L, 1000L))
                .andExpect(status().isUnauthorized());
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

    private void insertCommentHelpfulMark(Long userId, Long commentId, LocalDateTime createdAt) {
        jdbcTemplate.update(
                """
                INSERT INTO comment_helpful_marks
                    (user_id, comment_id, created_at)
                VALUES
                    (?, ?, ?)
                """,
                userId,
                commentId,
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
