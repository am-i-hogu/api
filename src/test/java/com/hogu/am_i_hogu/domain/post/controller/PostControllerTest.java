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
        jdbcTemplate.update("DELETE FROM post_votes");
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

    // 실패 케이스: categories 배열 안에 null이 있으면 DB 조회 전에 INVALID_CATEGORIES를 반환한다.
    @Test
    void createPostRejectsNullCategoryCode() throws Exception {
        stubAuthenticatedUser();

        String requestBody = """
                {
                  "title": "제목입니다",
                  "categories": [null],
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
                .andExpect(jsonPath("$.errors[0].code").value("INVALID_CATEGORIES"));
    }

    // 실패 케이스: 썸네일이 2개 이상이면 단일 썸네일 정책에 따라 MULTIPLE_THUMBNAILS를 반환한다.
    @Test
    void createPostRejectsMultipleThumbnails() throws Exception {
        stubAuthenticatedUser();

        String requestBody = """
            {
              "title": "제목입니다",
              "categories": ["USED_TRADE"],
              "content": "본문입니다",
              "images": [
                {"imageUrl": "https://example.com/1.jpg", "order": 0, "isThumbnail": true},
                {"imageUrl": "https://example.com/2.jpg", "order": 1, "isThumbnail": true}
              ]
            }
            """;

        mockMvc.perform(post("/api/posts")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.errors[0].field").value("images"))
                .andExpect(jsonPath("$.errors[0].code").value("MULTIPLE_THUMBNAILS"));
    }

    // 실패 케이스: images 배열 안에 null이 있으면 NullPointerException 대신 필드 오류를 반환한다.
    @Test
    void createPostRejectsNullImageItem() throws Exception {
        stubAuthenticatedUser();

        String requestBody = """
                {
                  "title": "제목입니다",
                  "categories": ["USED_TRADE"],
                  "content": "본문입니다",
                  "images": [null]
                }
                """;

        mockMvc.perform(post("/api/posts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.errors[0].field").value("images"))
                .andExpect(jsonPath("$.errors[0].code").value("EMPTY_IMAGE_URL"));
    }

    // 정상 케이스: 작성자가 제목, 카테고리, 본문, 이미지를 수정하면 게시물과 이미지가 함께 갱신된다.
    @Test
    void updatePostChangesPostAndReplacesImages() throws Exception {
        stubAuthenticatedUser();

        Long postId = 1234L;
        LocalDateTime now = LocalDateTime.now();
        insertPost(postId, TEST_USER_ID, "USED_TRADE", "기존 제목", "기존 본문", false, now);
        insertImage(1001L, postId, "https://example.com/old-thumbnail.jpg", true, 0, now);
        insertImage(1002L, postId, "https://example.com/old-image.jpg", false, 1, now);

        String requestBody = """
                {
                  "title": "수정 제목",
                  "categories": ["CONTRACT"],
                  "content": "수정 본문",
                  "images": [
                    {
                      "imageUrl": "https://example.com/new-thumbnail.png",
                      "order": 0,
                      "isThumbnail": true
                    },
                    {
                      "imageUrl": "https://example.com/new-image.webp",
                      "order": 1,
                      "isThumbnail": false
                    }
                  ]
                }
                """;

        mockMvc.perform(patch("/api/posts/{postId}", postId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").value(postId));

        String updatedTitle = jdbcTemplate.queryForObject(
                "SELECT title FROM posts WHERE id = ?",
                String.class,
                postId
        );
        String updatedCategory = jdbcTemplate.queryForObject(
                "SELECT category_code FROM posts WHERE id = ?",
                String.class,
                postId
        );
        String updatedContent = jdbcTemplate.queryForObject(
                "SELECT content FROM posts WHERE id = ?",
                String.class,
                postId
        );
        Integer imageCount = countImages(postId);
        Integer oldImageCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM image_assets WHERE post_id = ? AND url LIKE ?",
                Integer.class,
                postId,
                "https://example.com/old-%"
        );
        String thumbnailUrl = jdbcTemplate.queryForObject(
                "SELECT url FROM image_assets WHERE post_id = ? AND is_thumbnail = TRUE",
                String.class,
                postId
        );

        assertThat(updatedTitle).isEqualTo("수정 제목");
        assertThat(updatedCategory).isEqualTo("CONTRACT");
        assertThat(updatedContent).isEqualTo("수정 본문");
        assertThat(imageCount).isEqualTo(2);
        assertThat(oldImageCount).isZero();
        assertThat(thumbnailUrl).isEqualTo("https://example.com/new-thumbnail.png");
    }

    // 정상 케이스: images 필드를 보내지 않으면 기존 이미지는 그대로 유지하고 요청한 게시물 필드만 수정한다.
    @Test
    void updatePostKeepsImagesWhenImagesFieldIsOmitted() throws Exception {
        stubAuthenticatedUser();

        Long postId = 1234L;
        LocalDateTime now = LocalDateTime.now();
        insertPost(postId, TEST_USER_ID, "USED_TRADE", "기존 제목", "기존 본문", false, now);
        insertImage(1001L, postId, "https://example.com/existing-thumbnail.jpg", true, 0, now);

        String requestBody = """
                {
                  "title": "제목만 수정"
                }
                """;

        mockMvc.perform(patch("/api/posts/{postId}", postId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").value(postId));

        String updatedTitle = jdbcTemplate.queryForObject(
                "SELECT title FROM posts WHERE id = ?",
                String.class,
                postId
        );
        Integer imageCount = countImages(postId);
        Integer existingImageCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM image_assets WHERE post_id = ? AND url = ?",
                Integer.class,
                postId,
                "https://example.com/existing-thumbnail.jpg"
        );

        assertThat(updatedTitle).isEqualTo("제목만 수정");
        assertThat(imageCount).isEqualTo(1);
        assertThat(existingImageCount).isEqualTo(1);
    }

    // 정상 케이스: images를 빈 배열로 보내면 해당 게시물의 기존 이미지를 모두 삭제한다.
    @Test
    void updatePostDeletesImagesWhenImagesIsEmpty() throws Exception {
        stubAuthenticatedUser();

        Long postId = 1234L;
        LocalDateTime now = LocalDateTime.now();
        insertPost(postId, TEST_USER_ID, "USED_TRADE", "기존 제목", "기존 본문", false, now);
        insertImage(1001L, postId, "https://example.com/existing-thumbnail.jpg", true, 0, now);

        String requestBody = """
                {
                  "images": []
                }
                """;

        mockMvc.perform(patch("/api/posts/{postId}", postId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").value(postId));

        assertThat(countImages(postId)).isZero();
    }

    // 실패 케이스: 작성자가 아닌 사용자가 게시물을 수정하면 403 Forbidden과 FORBIDDEN_ACCESS를 반환한다.
    @Test
    void updatePostRejectsNotWriter() throws Exception {
        stubAuthenticatedUser();

        Long otherUserId = 2L;
        Long postId = 1234L;
        LocalDateTime now = LocalDateTime.now();
        insertUser(otherUserId, "other-hogu", now);
        insertPost(postId, otherUserId, "USED_TRADE", "다른 사람 글", "본문입니다", false, now);

        String requestBody = """
                {
                  "title": "수정 시도"
                }
                """;

        mockMvc.perform(patch("/api/posts/{postId}", postId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN_ACCESS"));
    }

    // 실패 케이스: 존재하지 않는 게시물을 수정하면 404 Not Found와 POST_NOT_FOUND를 반환한다.
    @Test
    void updatePostRejectsNotFoundPost() throws Exception {
        stubAuthenticatedUser();

        Long notFoundPostId = 9999L;
        String requestBody = """
                {
                  "title": "수정 시도"
                }
                """;

        mockMvc.perform(patch("/api/posts/{postId}", notFoundPostId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
    }

    // 실패 케이스: 삭제된 게시물을 수정하면 404 Not Found와 POST_ALREADY_DELETED를 반환한다.
    @Test
    void updatePostRejectsDeletedPost() throws Exception {
        stubAuthenticatedUser();

        Long postId = 1234L;
        LocalDateTime now = LocalDateTime.now();
        insertPost(postId, TEST_USER_ID, "USED_TRADE", "삭제된 글", "본문입니다", true, now);

        String requestBody = """
                {
                  "title": "수정 시도"
                }
                """;

        mockMvc.perform(patch("/api/posts/{postId}", postId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_ALREADY_DELETED"));
    }

    // 실패 케이스: 요청 body가 없으면 400 Bad Request와 EMPTY_REQUEST_BODY를 반환한다.
    @Test
    void updatePostRejectsEmptyRequestBody() throws Exception {
        stubAuthenticatedUser();

        mockMvc.perform(patch("/api/posts/{postId}", 1234L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EMPTY_REQUEST_BODY"));
    }

    // 실패 케이스: 이미지 order가 누락되면 DB 저장 전에 필드별 오류 코드를 반환한다.
    @Test
    void updatePostRejectsImageWithoutOrder() throws Exception {
        stubAuthenticatedUser();

        Long postId = 1234L;
        LocalDateTime now = LocalDateTime.now();
        insertPost(postId, TEST_USER_ID, "USED_TRADE", "기존 제목", "기존 본문", false, now);

        String requestBody = """
                {
                  "images": [
                    {
                      "imageUrl": "https://example.com/new-thumbnail.jpg",
                      "isThumbnail": true
                    }
                  ]
                }
                """;

        mockMvc.perform(patch("/api/posts/{postId}", postId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.errors[0].field").value("images"))
                .andExpect(jsonPath("$.errors[0].code").value("EMPTY_IMAGE_ORDER"));
    }

    // 실패 케이스: 수정 요청의 썸네일이 2개 이상이면 MULTIPLE_THUMBNAILS를 반환한다.
    @Test
    void updatePostRejectsMultipleThumbnails() throws Exception {
        stubAuthenticatedUser();

        Long postId = 1234L;
        LocalDateTime now = LocalDateTime.now();
        insertPost(postId, TEST_USER_ID, "USED_TRADE", "기존 제목", "기존 본문", false, now);

        String requestBody = """
                {
                  "images": [
                    {"imageUrl": "https://example.com/new-thumbnail-1.jpg", "order": 0, "isThumbnail": true},
                    {"imageUrl": "https://example.com/new-thumbnail-2.jpg", "order": 1, "isThumbnail": true}
                  ]
                }
                """;

        mockMvc.perform(patch("/api/posts/{postId}", postId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.errors[0].field").value("images"))
                .andExpect(jsonPath("$.errors[0].code").value("MULTIPLE_THUMBNAILS"));
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
                .andExpect(jsonPath("$.viewCount").value(13))
                .andExpect(jsonPath("$.content").value("본문입니다"))
                .andExpect(jsonPath("$.images[0]").value("https://example.com/image1.jpg"))
                .andExpect(jsonPath("$.images[1]").value("https://example.com/image2.jpg"))
                .andExpect(jsonPath("$.vote.totalVotes").value(0))
                .andExpect(jsonPath("$.vote.yesVotes").value(0))
                .andExpect(jsonPath("$.vote.noVotes").value(0))
                .andExpect(jsonPath("$.vote.myVote").value("NONE"))
                .andExpect(jsonPath("$.writer.nickname").value("hogu"))
                .andExpect(jsonPath("$.writer.profileImageUrl").value("https://example.com/asdf1234-profile.jpg"));

        Integer viewCount = jdbcTemplate.queryForObject(
                "SELECT view_count FROM posts WHERE id = ?",
                Integer.class,
                postId
        );
        assertThat(viewCount).isEqualTo(13);
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

    // 정상 케이스: 로그인 사용자가 상세 조회하면 투표 집계와 내가 선택한 투표 값을 반환한다.
    @Test
    void getPostDetailAsAuthenticatedUserReturnsVoteSummary() throws Exception {
        Long postId = 1234L;
        Long noVoteUserId = 2L;
        Long noneVoteUserId = 3L;
        LocalDateTime now = LocalDateTime.now();

        insertUser(noVoteUserId, "no-voter", now);
        insertUser(noneVoteUserId, "none-voter", now);
        insertPost(postId, TEST_USER_ID, "USED_TRADE", "투표 있는 글", "본문입니다", false, now);
        insertPostVote(TEST_USER_ID, postId, "HOGU", now);
        insertPostVote(noVoteUserId, postId, "NOT_HOGU", now);
        insertPostVote(noneVoteUserId, postId, "NONE", now);
        stubAuthenticatedUser();

        mockMvc.perform(get("/api/posts/{postId}", postId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vote.totalVotes").value(2))
                .andExpect(jsonPath("$.vote.yesVotes").value(1))
                .andExpect(jsonPath("$.vote.noVotes").value(1))
                .andExpect(jsonPath("$.vote.myVote").value("HOGU"));
    }

    // 정상 케이스: 작성자가 게시물을 삭제하면 실제 row는 유지하고 삭제 상태로 변경한 뒤 204 No Content를 반환한다.
    @Test
    void deletePostSoftDeletesPostAndReturnsNoContent() throws Exception {
        stubAuthenticatedUser();

        Long postId = 1234L;
        LocalDateTime now = LocalDateTime.now();
        insertPost(postId, TEST_USER_ID, "USED_TRADE", "삭제할 글", "본문입니다", false, now);

        mockMvc.perform(delete("/api/posts/{postId}", postId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isNoContent());

        Boolean isDeleted = jdbcTemplate.queryForObject(
                "SELECT is_deleted FROM posts WHERE id = ?",
                Boolean.class,
                postId
        );
        LocalDateTime deletedAt = jdbcTemplate.queryForObject(
                "SELECT deleted_at FROM posts WHERE id = ?",
                LocalDateTime.class,
                postId
        );

        assertThat(isDeleted).isTrue();
        assertThat(deletedAt).isNotNull();
    }

    // 실패 케이스: 존재하지 않는 게시물을 삭제하면 404 Not Found와 POST_NOT_FOUND를 반환한다.
    @Test
    void deletePostRejectsNotFoundPost() throws Exception {
        stubAuthenticatedUser();

        Long notFoundPostId = 9999L;

        mockMvc.perform(delete("/api/posts/{postId}", notFoundPostId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
    }

    // 실패 케이스: 이미 삭제된 게시물을 다시 삭제하면 404 Not Found와 POST_ALREADY_DELETED를 반환한다.
    @Test
    void deletePostRejectsDeletedPost() throws Exception {
        stubAuthenticatedUser();

        Long postId = 1234L;
        LocalDateTime now = LocalDateTime.now();
        insertPost(postId, TEST_USER_ID, "USED_TRADE", "이미 삭제된 글", "본문입니다", true, now);

        mockMvc.perform(delete("/api/posts/{postId}", postId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_ALREADY_DELETED"));
    }

    // 실패 케이스: 작성자가 아닌 사용자가 게시물을 삭제하면 403 Forbidden과 FORBIDDEN_ACCESS를 반환한다.
    @Test
    void deletePostRejectsNotWriter() throws Exception {
        stubAuthenticatedUser();

        Long otherUserId = 2L;
        Long postId = 1234L;
        LocalDateTime now = LocalDateTime.now();
        insertUser(otherUserId, "other-hogu", now);
        insertPost(postId, otherUserId, "USED_TRADE", "다른 사람 글", "본문입니다", false, now);

        mockMvc.perform(delete("/api/posts/{postId}", postId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN_ACCESS"));
    }

    // 실패 케이스: postId가 숫자로 변환될 수 없는 값이면 400 Bad Request와 WRONG_POSTID_TYPE을 반환한다.
    @Test
    void deletePostRejectsWrongPostIdType() throws Exception {
        stubAuthenticatedUser();

        mockMvc.perform(delete("/api/posts/{postId}", "abc")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WRONG_POSTID_TYPE"));
    }

    private void insertUser(Long userId, String nickname, LocalDateTime now) {
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
            LocalDateTime now
    ) {
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
                isDeleted ? now : null,
                now,
                now
        );
    }

    private void insertImage(
            Long imageId,
            Long postId,
            String imageUrl,
            boolean isThumbnail,
            int sortOrder,
            LocalDateTime now
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO image_assets
                    (id, uploaded_by_user_id, post_id, url, content_type, size_bytes, is_thumbnail, sort_order, created_at)
                VALUES
                    (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                imageId,
                TEST_USER_ID,
                postId,
                imageUrl,
                "image/jpeg",
                0L,
                isThumbnail,
                sortOrder,
                now
        );
    }

    private void insertPostVote(Long userId, Long postId, String myVote, LocalDateTime now) {
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
                now,
                now
        );
    }

    private Integer countImages(Long postId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM image_assets WHERE post_id = ?",
                Integer.class,
                postId
        );
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
