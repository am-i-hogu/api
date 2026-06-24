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
import com.hogu.am_i_hogu.domain.policy.controller.PolicyController;
import com.hogu.am_i_hogu.domain.policy.service.PrivacyService;
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

import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
        UserController.class,
        PolicyController.class
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

    @MockitoBean
    private PostCreateService postCreateService;
    @MockitoBean private PostDetailService postDetailService;
    @MockitoBean private PostUpdateService postUpdateService;
    @MockitoBean private PostDeleteService postDeleteService;
    @MockitoBean private PostBookmarkService postBookmarkService;
    @MockitoBean private PostVoteService postVoteService;
    @MockitoBean private HomePostQueryService homePostQueryService;
    @MockitoBean private CommentCreateService commentCreateService;
    @MockitoBean private CommentReadService commentReadService;
    @MockitoBean private CommentUpdateService commentUpdateService;
    @MockitoBean private CommentDeleteService commentDeleteService;
    @MockitoBean private CommentHelpfulService commentHelpfulService;
    @MockitoBean private OnboardingService onboardingService;
    @MockitoBean private ReissueService reissueService;
    @MockitoBean private LogoutService logoutService;
    @MockitoBean private OAuthService oauthService;
    @MockitoBean private UserDeletionService userDeletionService;
    @MockitoBean private JwtProvider jwtProvider;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private AuthenticationEntryPoint authenticationEntryPoint;
    @MockitoBean private AccessDeniedHandler accessDeniedHandler;
    @MockitoBean private NicknameCheckService nicknameCheckService;
    @MockitoBean private ProfileUpdateService profileUpdateService;
    @MockitoBean private MyPostQueryService myPostQueryService;
    @MockitoBean private MyCommentQueryService myCommentQueryService;
    @MockitoBean private MyBookmarkQueryService myBookmarkQueryService;
    @MockitoBean private MyVoteQueryService myVoteQueryService;
    @MockitoBean private MyPageService myPageService;
    @MockitoBean private HoguReportService hoguReportService;
    @MockitoBean private PrivacyService privacyService;

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
        assertThat(root.get("paths").get("/api/auth/callback/{provider}").get("get").get("operationId").asText()).isEqualTo("handleOAuthCallback");
        assertThat(root.get("paths").get("/api/users/me").get("delete").get("operationId").asText()).isEqualTo("deleteUser");
        assertThat(root.get("paths").get("/api/users/me").get("patch").get("operationId").asText()).isEqualTo("updateProfile");
        assertThat(root.get("paths").get("/api/users/check-nickname").get("get").get("operationId").asText()).isEqualTo("checkNickname");
        assertThat(root.get("paths").get("/api/users/me").get("get").get("operationId").asText()).isEqualTo("getMyPage");
        assertThat(root.get("paths").get("/api/users/me/report").get("get").get("operationId").asText()).isEqualTo("getHoguReport");
        assertThat(root.get("paths").get("/api/users/me/posts").get("get").get("operationId").asText()).isEqualTo("getMyPosts");
        assertThat(root.get("paths").get("/api/users/me/comments").get("get").get("operationId").asText()).isEqualTo("getMyComments");
        assertThat(root.get("paths").get("/api/users/me/bookmarks").get("get").get("operationId").asText()).isEqualTo("getMyBookmarks");
        assertThat(root.get("paths").get("/api/users/me/votes").get("get").get("operationId").asText()).isEqualTo("getMyVotes");
        assertThat(root.get("paths").get("/api/policies/privacy").get("get").get("operationId").asText()).isEqualTo("getPrivacyPolicy");
        assertThat(root.at("/components/securitySchemes/bearerAuth/type").asText()).isEqualTo("http");
        assertThat(root.at("/components/schemas/PostDetailResponse/properties/images/items/$ref").asText())
                .isEqualTo("#/components/schemas/PostImageResponse");

        Path specFile = tempDir.resolve("api-docs.json");
        Files.writeString(specFile, apiDocs);

        Path outputDir = tempDir.resolve("typescript-fetch");
        Files.createDirectories(outputDir);

        Process process = new ProcessBuilder(List.of(
                "sh", "-c",
                String.format(
                        "docker run --rm -u $(id -u):$(id -g) -v %s:/local openapitools/openapi-generator-cli:v7.13.0 generate -i /local/api-docs.json -g typescript-fetch -o /local/typescript-fetch",
                        tempDir.toAbsolutePath()
                )
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
        Path userApi = outputDir.resolve("apis/UserApi.ts");
        Path policyApi = outputDir.resolve("apis/PolicyApi.ts");
        Path postVoteRequest = outputDir.resolve("models/PostVoteRequest.ts");
        Path postImageResponse = outputDir.resolve("models/PostImageResponse.ts");
        Path homePostListResponse = outputDir.resolve("models/HomePostListResponse.ts");
        Path policyResponse = outputDir.resolve("models/PolicyResponse.ts");
        Path updateProfileResponse = outputDir.resolve("models/UpdateProfileResponse.ts");

        assertThat(authApi).exists();
        assertThat(oauthApi).exists();
        assertThat(postApi).exists();
        assertThat(commentApi).exists();
        assertThat(userApi).exists();
        assertThat(policyApi).exists();
        assertThat(postVoteRequest).exists();
        assertThat(postImageResponse).exists();
        assertThat(homePostListResponse).exists();
        assertThat(policyResponse).exists();
        assertThat(updateProfileResponse).exists();

        String authApiContent = Files.readString(authApi);
        String oauthApiContent = Files.readString(oauthApi);
        String postApiContent = Files.readString(postApi);
        String commentApiContent = Files.readString(commentApi);
        String userApiContent = Files.readString(userApi);
        String policyApiContent = Files.readString(policyApi);
        String postVoteRequestContent = Files.readString(postVoteRequest);
        String homePostListResponseContent = Files.readString(homePostListResponse);
        String policyResponseContent = Files.readString(policyResponse);
        String updateProfileResponseContent = Files.readString(updateProfileResponse);

        assertThat(authApiContent).contains("async createUser(");
        assertThat(authApiContent).contains("async refreshAccessToken(");
        assertThat(authApiContent).contains("async logout(");
        assertThat(oauthApiContent).contains("async login(");
        assertThat(oauthApiContent).contains("async handleOAuthCallback(");
        assertThat(oauthApiContent).contains("async deleteUser(");

        assertThat(postApiContent).contains("async getHomePosts(");
        assertThat(postApiContent).contains("async getPostDetail(");
        assertThat(postApiContent).contains("async createPost(");
        assertThat(postApiContent).contains("async updatePost(");
        assertThat(postApiContent).contains("async deletePost(");

        assertThat(commentApiContent).contains("async createComment(");
        assertThat(commentApiContent).contains("async getComments(");
        assertThat(commentApiContent).contains("async updateComment(");
        assertThat(commentApiContent).contains("async deleteComment(");
        assertThat(commentApiContent).contains("async createCommentHelpful(");
        assertThat(commentApiContent).contains("async deleteCommentHelpful(");

        assertThat(userApiContent).contains("async updateProfile(");
        assertThat(userApiContent).contains("async checkNickname(");
        assertThat(userApiContent).contains("async getMyPage(");
        assertThat(userApiContent).contains("async getHoguReport(");
        assertThat(userApiContent).contains("async getMyPosts(");
        assertThat(userApiContent).contains("async getMyComments(");
        assertThat(userApiContent).contains("async getMyBookmarks(");
        assertThat(userApiContent).contains("async getMyVotes(");

        assertThat(policyApiContent).contains("async getPrivacyPolicy(");

        assertThat(postVoteRequestContent).contains("Hogu: 'HOGU'");
        assertThat(postVoteRequestContent).contains("NotHogu: 'NOT_HOGU'");
        assertThat(homePostListResponseContent).contains("HomePostListResponse");
        assertThat(policyResponseContent).contains("PolicyResponse");
        assertThat(updateProfileResponseContent).contains("UpdateProfileResponse");
    }
}
