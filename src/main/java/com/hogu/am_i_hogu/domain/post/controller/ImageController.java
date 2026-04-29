package com.hogu.am_i_hogu.domain.post.controller;

import com.hogu.am_i_hogu.domain.post.dto.response.ImageUploadResponse;
import com.hogu.am_i_hogu.domain.post.service.ImageUploadService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    private final ImageUploadService imageUploadService;

    public ImageController(ImageUploadService imageUploadService) {
        this.imageUploadService = imageUploadService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageUploadResponse> uploadImage(
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        String baseUrl = ServletUriComponentsBuilder
                .fromCurrentContextPath() // 개발시에는 http://localhost:8080
                .build()
                .toUriString();
        String imageUrl = imageUploadService.upload(image, baseUrl);

        return ResponseEntity.ok(new ImageUploadResponse(imageUrl));
    }
}
