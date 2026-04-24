package com.hogu.am_i_hogu.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(SecurityConfigTest.TestController.class)
@ActiveProfiles("test")
public class SecurityConfigTest {
    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JwtProvider jwtProvider;

    @RestController
    static class TestController {
        @GetMapping("/api/posts")
        public String getPosts() {
            return "public";
        }

        @GetMapping("/api/users/me")
        public String getMyInfo() {
            return "private";
        }
    }

    // 공개 endpoint: access token 없이 접근 가능한지 테스트
    @Test
    void publicEndpoint200Test() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/api/posts", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("public");
    }

    // 보호 endpoint: access token 없이 접근하면 401 발생하는지 테스트
    @Test
    void protectedEndpoint401Test() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/api/users/me", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).contains("EMPTY_ACCESS_TOKEN");
    }

    // 보호 endpoint: 유효한 access token이 있으면 접근 가능한지 테스트
    @Test
    void protectedEndpoint200Test() {
        String accessToken = jwtProvider.createAccessToken(1L, 1000L * 60 * 30);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/users/me",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("private");
    }

}
