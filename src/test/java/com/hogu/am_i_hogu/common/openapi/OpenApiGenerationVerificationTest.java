package com.hogu.am_i_hogu.common.openapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hogu.am_i_hogu.common.config.OpenApiConfig;
import com.hogu.am_i_hogu.common.security.JwtProvider;
import com.hogu.am_i_hogu.domain.auth.controller.AuthController;
import com.hogu.am_i_hogu.domain.auth.service.LogoutService;
import com.hogu.am_i_hogu.domain.auth.service.OnboardingService;
import com.hogu.am_i_hogu.domain.auth.service.ReissueService;
import com.hogu.am_i_hogu.domain.comment.controller.CommentController;
import com.hogu.am_i_hogu.domain.comment.service.CommentCreateService;
import com.hogu.am_i_hogu.domain.comment.service.CommentDeleteService;
import com.hogu.am_i_hogu.domain.comment.service.CommentHelpfulService;
import com.hogu.am_i_hogu.domain.comment.service.CommentReadService;
import com.hogu.am_i_hogu.domain.comment.service.CommentUpdateService;
import com.hogu.am_i_hogu.domain.oauth.controller.OAuthController;
import com.hogu.am_i_hogu.domain.oauth.service.OAuthService;
import com.hogu.am_i_hogu.domain.oauth.service.UserDeletionService;
import com.hogu.am_i_hogu.domain.post.controller.PostController;
import com.hogu.am_i_hogu.domain.post.service.HomePostQueryService;
import com.hogu.am_i_hogu.domain.post.service.PostBookmarkService;
import com.hogu.am_i_hogu.domain.post.service.PostCreateService;
import com.hogu.am_i_hogu.domain.post.service.PostDeleteService;
import com.hogu.am_i_hogu.domain.post.service.PostDetailService;
import com.hogu.am_i_hogu.domain.post.service.PostUpdateService;
import com.hogu.am_i_hogu.domain.post.service.PostVoteService;
import com.hogu.am_i_hogu.domain.user.controller.UserController;
import com.hogu.am_i_hogu.domain.user.repository.UserRepository;
import com.hogu.am_i_hogu.domain.user.service.HoguReportService;
import com.hogu.am_i_hogu.domain.user.service.MyBookmarkQueryService;
import com.hogu.am_i_hogu.domain.user.service.MyCommentQueryService;
import com.hogu.am_i_hogu.domain.user.service.MyPageService;
import com.hogu.am_i_hogu.domain.user.service.MyPostQueryService;
import com.hogu.am_i_hogu.domain.user.service.MyVoteQueryService;
import com.hogu.am_i_hogu.domain.user.service.NicknameCheckService;
import com.hogu.am_i_hogu.domain.user.service.ProfileUpdateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.configuration.SpringDocSecurityConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        PostController.class,
        CommentController.class,
        AuthController.class,
        OAuthController.class,
        UserController.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import({
        OpenApiConfig.class,
        SpringDocConfiguration.class,
        SpringDocWebMvcConfiguration.class,
        SpringDocSecurityConfiguration.class
})
@EnableConfigurationProperties({
        OpenApiConfig.OpenApiProperties.class,
        SpringDocConfigProperties.class,
        SwaggerUiConfigProperties.class
})
@ActiveProfiles("test")
class OpenApiGenerationVerificationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean private PostCreateService postCreateService;
    @MockBean private PostDetailService postDetailService;
    @MockBean private PostUpdateService postUpdateService;
    @MockBean private PostDeleteService postDeleteService;
    @MockBean private PostBookmarkService postBookmarkService;
    @MockBean private PostVoteService postVoteService;
    @MockBean private HomePostQueryService homePostQueryService;
    @MockBean private CommentCreateService commentCreateService;
    @MockBean private CommentReadService commentReadService;
    @MockBean private CommentUpdateService commentUpdateService;
    @MockBean private CommentDeleteService commentDeleteService;
    @MockBean private CommentHelpfulService commentHelpfulService;
    @MockBean private OnboardingService onboardingService;
    @MockBean private ReissueService reissueService;
    @MockBean private LogoutService logoutService;
    @MockBean private OAuthService oauthService;
    @MockBean private UserDeletionService userDeletionService;
    @MockBean private JwtProvider jwtProvider;
    @MockBean private UserRepository userRepository;
    @MockBean private AuthenticationEntryPoint authenticationEntryPoint;
    @MockBean private AccessDeniedHandler accessDeniedHandler;
    @MockBean private NicknameCheckService nicknameCheckService;
    @MockBean private ProfileUpdateService profileUpdateService;
    @MockBean private MyPostQueryService myPostQueryService;
    @MockBean private MyCommentQueryService myCommentQueryService;
    @MockBean private MyBookmarkQueryService myBookmarkQueryService;
    @MockBean private MyVoteQueryService myVoteQueryService;
    @MockBean private MyPageService myPageService;
    @MockBean private HoguReportService hoguReportService;

    @Test
    void apiDocs_and_typescriptFetch_generation_are_valid(@TempDir Path tempDir) throws Exception {
        String apiDocs = mockMvc.perform(get("/v3/api-docs").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(apiDocs);
        assertThat(root.get("openapi").asText()).startsWith("3.0");
        assertThat(root.get("paths").get("/api/posts").get("get").get("operationId").asText()).isEqualTo("getHomePosts");
        assertThat(root.get("paths").get("/api/posts/{postId}").get("get").get("operationId").asText()).isEqualTo("getPostDetail");
        assertThat(root.get("paths").get("/api/posts/{postId}/comments").get("post").get("operationId").asText()).isEqualTo("createComment");
        assertThat(root.get("paths").get("/api/auth/login/{provider}").get("get").get("operationId").asText()).isEqualTo("login");
        assertThat(root.at("/components/securitySchemes/bearerAuth/type").asText()).isEqualTo("http");

        Path specFile = tempDir.resolve("api-docs.json");
        Files.writeString(specFile, apiDocs);

        Path outputDir = tempDir.resolve("typescript-fetch");
        Files.createDirectories(outputDir);

        Process process = new ProcessBuilder(List.of(
                "docker", "run", "--rm",
                "-v", tempDir.toAbsolutePath() + ":/local",
                "openapitools/openapi-generator-cli:v7.13.0",
                "generate",
                "-i", "/local/api-docs.json",
                "-g", "typescript-fetch",
                "-o", "/local/typescript-fetch"
        )).redirectErrorStream(true).start();

        String generatorOutput = new String(process.getInputStream().readAllBytes());
        int exitCode = process.waitFor();
        assertThat(exitCode)
                .withFailMessage("openapi-generator failed with output:%n%s", generatorOutput)
                .isZero();

        Path authApi = outputDir.resolve("apis/AuthApi.ts");
        Path oauthApi = outputDir.resolve("apis/OAuthApi.ts");
        Path postApi = outputDir.resolve("apis/PostApi.ts");
        Path commentApi = outputDir.resolve("apis/CommentApi.ts");
        Path postVoteRequest = outputDir.resolve("models/PostVoteRequest.ts");
        Path homePostListResponse = outputDir.resolve("models/HomePostListResponse.ts");

        assertThat(authApi).exists();
        assertThat(oauthApi).exists();
        assertThat(postApi).exists();
        assertThat(commentApi).exists();
        assertThat(postVoteRequest).exists();
        assertThat(homePostListResponse).exists();

        String authApiContent = Files.readString(authApi);
        String oauthApiContent = Files.readString(oauthApi);
        String postApiContent = Files.readString(postApi);
        String commentApiContent = Files.readString(commentApi);
        String postVoteRequestContent = Files.readString(postVoteRequest);
        String homePostListResponseContent = Files.readString(homePostListResponse);

        assertThat(authApiContent).contains("async createUser(");
        assertThat(authApiContent).contains("async refreshAccessToken(");
        assertThat(authApiContent).contains("async logout(");
        assertThat(oauthApiContent).contains("async login(");

        assertThat(postApiContent).contains("async getHomePosts(");
        assertThat(postApiContent).contains("async getPostDetail(");
        assertThat(postApiContent).contains("async createPost(");
        assertThat(postApiContent).contains("async updatePost(");
        assertThat(postApiContent).contains("async deletePost(");

        assertThat(commentApiContent).contains("async createComment(");
        assertThat(commentApiContent).contains("async getComments(");

        assertThat(postVoteRequestContent).contains("Hogu: 'HOGU'");
        assertThat(postVoteRequestContent).contains("NotHogu: 'NOT_HOGU'");
        assertThat(homePostListResponseContent).contains("HomePostListResponse");
    }
}
