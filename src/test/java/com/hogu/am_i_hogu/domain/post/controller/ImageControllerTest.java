package com.hogu.am_i_hogu.domain.post.controller;

import com.hogu.am_i_hogu.common.security.JwtAuthenticationEntryPoint;
import com.hogu.am_i_hogu.common.security.JwtAuthenticationFilter;
import com.hogu.am_i_hogu.common.security.JwtProvider;
import com.hogu.am_i_hogu.common.security.SecurityConfig;
import com.hogu.am_i_hogu.common.util.TsidGenerator;
import com.hogu.am_i_hogu.domain.post.service.ImageUploadService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ImageController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtAuthenticationEntryPoint.class,
        ImageUploadService.class,
        TsidGenerator.class
})
class ImageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtProvider jwtProvider;

    // 정상 케이스: jpg 이미지 파일을 업로드하면 200 OK와 임시 imageUrl을 반환한다.
    @Test
    void uploadImageReturnsTemporaryImageUrl() throws Exception {
        when(jwtProvider.validateAccessToken("valid-token"))
                .thenReturn(JwtProvider.TokenValidationResult.VALID);
        when(jwtProvider.getAuthentication("valid-token"))
                .thenReturn(new UsernamePasswordAuthenticationToken(
                        "1",
                        null,
                        Collections.emptyList()
                ));

        MockMultipartFile image = new MockMultipartFile(
                "image",
                "post-image.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "image-content".getBytes()
        );

        mockMvc.perform(multipart("/api/images")
                        .file(image)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl", startsWith("http://localhost/temporary/images/")));
    }

    // 실패 케이스: multipart 요청에 image 파일이 없으면 400 Bad Request와 EMPTY_IMAGE_FILE을 반환한다.
    @Test
    void uploadImageRejectsMissingFile() throws Exception {
        when(jwtProvider.validateAccessToken("valid-token"))
                .thenReturn(JwtProvider.TokenValidationResult.VALID);
        when(jwtProvider.getAuthentication("valid-token"))
                .thenReturn(new UsernamePasswordAuthenticationToken(
                        "1",
                        null,
                        Collections.emptyList()
                ));

        mockMvc.perform(multipart("/api/images")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EMPTY_IMAGE_FILE"));
    }

    // 실패 케이스: 허용하지 않는 확장자(gif)를 업로드하면 400 Bad Request와 UNSUPPORTED_FORMAT을 반환한다.
    @Test
    void uploadImageRejectsUnsupportedFormat() throws Exception {
        when(jwtProvider.validateAccessToken("valid-token"))
                .thenReturn(JwtProvider.TokenValidationResult.VALID);
        when(jwtProvider.getAuthentication("valid-token"))
                .thenReturn(new UsernamePasswordAuthenticationToken(
                        "1",
                        null,
                        Collections.emptyList()
                ));

        MockMultipartFile image = new MockMultipartFile(
                "image",
                "post-image.gif",
                MediaType.IMAGE_GIF_VALUE,
                "image-content".getBytes()
        );

        mockMvc.perform(multipart("/api/images")
                        .file(image)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_FORMAT"));
    }

    // 실패 케이스: 5MB를 초과한 이미지 파일을 업로드하면 413 Payload Too Large와 FILE_SIZE_EXCEEDED를 반환한다.
    @Test
    void uploadImageRejectsFileLargerThanFiveMb() throws Exception {
        when(jwtProvider.validateAccessToken("valid-token"))
                .thenReturn(JwtProvider.TokenValidationResult.VALID);
        when(jwtProvider.getAuthentication("valid-token"))
                .thenReturn(new UsernamePasswordAuthenticationToken(
                        "1",
                        null,
                        Collections.emptyList()
                ));

        MockMultipartFile image = new MockMultipartFile(
                "image",
                "large-image.png",
                MediaType.IMAGE_PNG_VALUE,
                new byte[(5 * 1024 * 1024) + 1]
        );

        mockMvc.perform(multipart("/api/images")
                        .file(image)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("FILE_SIZE_EXCEEDED"));
    }
}
