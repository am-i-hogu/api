package com.hogu.am_i_hogu.domain.post.controller;

import com.hogu.am_i_hogu.domain.post.dto.response.ImageUploadResponse;
import com.hogu.am_i_hogu.domain.post.service.ImageUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/images")
public class ImageController implements ImageApiDoc {

    private final ImageUploadService imageUploadService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Override
    public ResponseEntity<ImageUploadResponse> uploadImage(
            Authentication authentication,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        Long userId = Long.valueOf(authentication.getName());
        String imageUrl = imageUploadService.upload(userId, image);

        return ResponseEntity.ok(new ImageUploadResponse(imageUrl));
    }
}
