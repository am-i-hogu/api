package com.hogu.am_i_hogu.domain.post.controller;

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

import java.time.LocalDateTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class PostControllerTest {

    private static final long TEST_USER_ID = 1L;

    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("am_i_hogu_post_test_db")
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
        jdbcTemplate.update("DELETE FROM image_assets");
        jdbcTemplate.update("DELETE FROM posts");
        jdbcTemplate.update("DELETE FROM user_hogu_stats");
        jdbcTemplate.update("DELETE FROM users");

        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
                """
                INSERT INTO users
                    (id, nickname, profile_image_url, is_deleted, deleted_at, created_at, updated_at)
                VALUES
                    (?, ?, ?, ?, ?, ?, ?)
                """,
                TEST_USER_ID,
                "hogu",
                null,
                false,
                null,
                now,
                now
        );
    }

    // 정상 케이스: 인증된 사용자가 필수 값과 이미지 정보를 보내면 게시물이 생성되고 postId를 반환한다.
    @Test
    void createPostReturnsPostIdAndPersistsPost() throws Exception {
        stubAuthenticatedUser();

        String requestBody = """
                {
                  "title": "제목입니다",
                  "categories": ["USED_TRADE"],
                  "content": "본문입니다",
                  "images": [
                    {
                      "imageUrl": "http://localhost/temporary/images/1/post-image.jpg",
                      "order": 0,
                      "isThumbnail": true
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/posts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.postId").isNumber());

        Long postId = jdbcTemplate.queryForObject(
                "SELECT id FROM posts WHERE title = ?",
                Long.class,
                "제목입니다"
        );
        assertThat(postId).isNotNull();

        Integer imageCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM image_assets WHERE post_id = ? AND is_thumbnail = TRUE",
                Integer.class,
                postId
        );
        assertThat(imageCount).isEqualTo(1);
    }

    // 정상 케이스: images가 빈 배열이면 이미지 없이 게시물을 생성하고 postId를 반환한다.
    @Test
    void createPostAllowsEmptyImages() throws Exception {
        stubAuthenticatedUser();

        String requestBody = """
                {
                  "title": "이미지 없는 글",
                  "categories": ["USED_TRADE"],
                  "content": "본문입니다",
                  "images": []
                }
                """;

        mockMvc.perform(post("/api/posts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.postId").isNumber());

        Integer imageCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM image_assets ia
                JOIN posts p ON ia.post_id = p.id
                WHERE p.title = ?
                """,
                Integer.class,
                "이미지 없는 글"
        );
        assertThat(imageCount).isZero();
    }

    // 실패 케이스: 요청 body가 없으면 400 Bad Request와 EMPTY_REQUEST_BODY를 반환한다.
    @Test
    void createPostRejectsEmptyRequestBody() throws Exception {
        stubAuthenticatedUser();

        mockMvc.perform(post("/api/posts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EMPTY_REQUEST_BODY"));
    }

    // 실패 케이스: 필수 필드가 비어 있으면 400 Bad Request와 필드별 오류 코드를 반환한다.
    @Test
    void createPostRejectsEmptyRequiredFields() throws Exception {
        stubAuthenticatedUser();

        String requestBody = """
                {
                  "title": "   ",
                  "categories": [],
                  "content": "",
                  "images": [
                    {
                      "imageUrl": "",
                      "order": 0,
                      "isThumbnail": false
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/posts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.errors[0].field").value("title"))
                .andExpect(jsonPath("$.errors[0].code").value("EMPTY_TITLE"))
                .andExpect(jsonPath("$.errors[1].field").value("categories"))
                .andExpect(jsonPath("$.errors[1].code").value("EMPTY_CATEGORIES"))
                .andExpect(jsonPath("$.errors[2].field").value("content"))
                .andExpect(jsonPath("$.errors[2].code").value("EMPTY_CONTENT"))
                .andExpect(jsonPath("$.errors[3].field").value("images"))
                .andExpect(jsonPath("$.errors[3].code").value("EMPTY_IMAGE_URL"))
                .andExpect(jsonPath("$.errors[4].field").value("images"))
                .andExpect(jsonPath("$.errors[4].code").value("EMPTY_THUMBNAIL"));
    }

    // 실패 케이스: 제목 길이, 카테고리, 이미지 개수, 이미지 URL이 유효하지 않으면 필드별 오류 코드를 반환한다.
    @Test
    void createPostRejectsInvalidFieldValues() throws Exception {
        stubAuthenticatedUser();

        String requestBody = """
                {
                  "title": "123456789012345678901234567890123456789012345678901",
                  "categories": ["UNKNOWN"],
                  "content": "본문입니다",
                  "images": [
                    {"imageUrl": "not-url", "order": 0, "isThumbnail": true},
                    {"imageUrl": "http://localhost/temporary/images/2/image.jpg", "order": 1, "isThumbnail": false},
                    {"imageUrl": "http://localhost/temporary/images/3/image.jpg", "order": 2, "isThumbnail": false},
                    {"imageUrl": "http://localhost/temporary/images/4/image.jpg", "order": 3, "isThumbnail": false},
                    {"imageUrl": "http://localhost/temporary/images/5/image.jpg", "order": 4, "isThumbnail": false},
                    {"imageUrl": "http://localhost/temporary/images/6/image.jpg", "order": 5, "isThumbnail": false}
                  ]
                }
                """;

        mockMvc.perform(post("/api/posts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.errors[0].field").value("title"))
                .andExpect(jsonPath("$.errors[0].code").value("TITLE_LENGTH_EXCEEDED"))
                .andExpect(jsonPath("$.errors[1].field").value("categories"))
                .andExpect(jsonPath("$.errors[1].code").value("INVALID_CATEGORIES"))
                .andExpect(jsonPath("$.errors[2].field").value("images"))
                .andExpect(jsonPath("$.errors[2].code").value("IMAGE_COUNT_EXCEEDED"))
                .andExpect(jsonPath("$.errors[3].field").value("images"))
                .andExpect(jsonPath("$.errors[3].code").value("INVALID_IMAGE_URL"));
    }

    // 실패 케이스: 카테고리가 2개 이상이면 단일 카테고리 정책에 따라 MULTIPLE_CATEGORIES를 반환한다.
    @Test
    void createPostRejectsMultipleCategories() throws Exception {
        stubAuthenticatedUser();

        String requestBody = """
                {
                  "title": "제목입니다",
                  "categories": ["USED_TRADE", "CONTRACT"],
                  "content": "본문입니다",
                  "images": []
                }
                """;

        mockMvc.perform(post("/api/posts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.errors[0].field").value("categories"))
                .andExpect(jsonPath("$.errors[0].code").value("MULTIPLE_CATEGORIES"));
    }

    // 정상 케이스: 비회원도 게시글 상세 정보를 조회할 수 있다.
    @Test
    void getPostDetailAsGuestReturnsPostDetail() throws Exception {
        Long postId = 1234L;
        LocalDateTime now = LocalDateTime.now();

        jdbcTemplate.update(
                """
                UPDATE users
                SET profile_image_url = ?
                WHERE id = ?
                """,
                "https://example.com/asdf1234-profile.jpg",
                TEST_USER_ID
        );

        jdbcTemplate.update(
                """
                INSERT INTO posts
                    (id, writer_user_id, category_code, title, content, view_count, is_deleted, deleted_at, created_at, updated_at)
                VALUES
                    (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                postId,
                TEST_USER_ID,
                "USED_TRADE",
                "안녕하세요",
                "본문입니다",
                12,
                false,
                null,
                now,
                now
        );

        jdbcTemplate.update(
                """
                INSERT INTO image_assets
                    (id, uploaded_by_user_id, post_id, url, content_type, size_bytes, is_thumbnail, sort_order, created_at)
                VALUES
                    (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                1001L,
                TEST_USER_ID,
                postId,
                "https://example.com/image1.jpg",
                "image/jpg",
                0L,
                true,
                0,
                now
        );

        jdbcTemplate.update(
                """
                INSERT INTO image_assets
                    (id, uploaded_by_user_id, post_id, url, content_type, size_bytes, is_thumbnail, sort_order, created_at)
                VALUES
                    (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                1002L,
                TEST_USER_ID,
                postId,
                "https://example.com/image2.jpg",
                "image/jpg",
                0L,
                false,
                1,
                now
        );

        mockMvc.perform(get("/api/posts/{postId}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").value(postId))
                .andExpect(jsonPath("$.isMine").value(false))
                .andExpect(jsonPath("$.categories[0]").value("USED_TRADE"))
                .andExpect(jsonPath("$.title").value("안녕하세요"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andExpect(jsonPath("$.viewCount").value(12))
                .andExpect(jsonPath("$.content").value("본문입니다"))
                .andExpect(jsonPath("$.images[0]").value("https://example.com/image1.jpg"))
                .andExpect(jsonPath("$.images[1]").value("https://example.com/image2.jpg"))
                .andExpect(jsonPath("$.vote.totalVotes").value(0))
                .andExpect(jsonPath("$.vote.yesVotes").value(0))
                .andExpect(jsonPath("$.vote.noVotes").value(0))
                .andExpect(jsonPath("$.vote.myVote").value("NONE"))
                .andExpect(jsonPath("$.writer.nickname").value("hogu"))
                .andExpect(jsonPath("$.writer.profileImageUrl").value("https://example.com/asdf1234-profile.jpg"));
    }

    // 실패 케이스: 존재하지 않는 게시글을 조회하면 404 Not Found와 POST_NOT_FOUND를 반환한다.
    @Test
    void getPostDetailRejectNotFoundPost() throws Exception {
        Long notFoundPostId = 9999L;

        mockMvc.perform(get("/api/posts/{postId}", notFoundPostId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
    }

    // 실패 케이스: 삭제된 게시글을 조회하면 404 Not Found와 POST_ALREADY_DELETED를 반환한다.
    @Test
    void getPostDetailRejectsDeletedPost() throws Exception {
        Long postId = 1234L;
        LocalDateTime now = LocalDateTime.now();

        jdbcTemplate.update(
                """
                INSERT INTO posts
                    (id, writer_user_id, category_code, title, content, view_count, is_deleted, deleted_at, created_at, updated_at)
                VALUES
                    (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                postId,
                TEST_USER_ID,
                "USED_TRADE",
                "삭제된 글입니다",
                "본문입니다",
                0,
                true,
                now,
                now,
                now
        );

        mockMvc.perform(get("/api/posts/{postId}", postId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_ALREADY_DELETED"));
    }

    // 정상 케이스: 작성자가 본인이 작성한 게시글 조회시, `isMine`을 `true`로 반환한다.
    @Test
    void getPostDetailAsWriterReturnsIsMineTrue() throws Exception {
        Long postId = 1234L;
        LocalDateTime now = LocalDateTime.now();

        jdbcTemplate.update(
                """
                INSERT INTO posts
                    (id, writer_user_id, category_code, title, content, view_count, is_deleted, deleted_at, created_at, updated_at)
                VALUES
                    (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                postId,
                TEST_USER_ID,
                "USED_TRADE",
                "내가 쓴 글",
                "본문입니다",
                12,
                false,
                null,
                now,
                now
        );

        stubAuthenticatedUser();

        mockMvc.perform(get("/api/posts/{postId}", postId)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").value(postId))
                .andExpect(jsonPath("$.isMine").value(true));
    }

    /**
     * 테스트에서 사용할 가짜 로그인 사용자를 설정한다.
     * Authorization 헤더에 "Bearer valid-token"이 들어오면 TEST_USER_ID 사용자로 인증된 상태가 된다.
     */
    private void stubAuthenticatedUser() {
        when(jwtProvider.validateAccessToken("valid-token"))
                .thenReturn(JwtProvider.TokenValidationResult.VALID);
        when(jwtProvider.getAuthentication("valid-token"))
                .thenReturn(new UsernamePasswordAuthenticationToken(
                        String.valueOf(TEST_USER_ID),
                        null,
                        Collections.emptyList()
                ));
    }
}
