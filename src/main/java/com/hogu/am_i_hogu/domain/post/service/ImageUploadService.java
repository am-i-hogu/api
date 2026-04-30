package com.hogu.am_i_hogu.domain.post.service;

import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.common.util.TsidGenerator;
import com.hogu.am_i_hogu.domain.post.exception.ImageErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ImageUploadService {

    private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024L * 1024L;
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    private final TsidGenerator tsidGenerator;

    public String upload(MultipartFile image, String baseUrl) {
        validate(image);

        long imageId = tsidGenerator.nextId();
        String filename = sanitizeFilename(image.getOriginalFilename());

        // s3 연동 전 임시 구현 - 실제 파일 저장 없이 API 검증용 이미지 임시 URL만 발급한다.
        return "%s/temporary/images/%d/%s".formatted(baseUrl, imageId, filename);
    }

    private void validate(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new CustomException(ImageErrorCode.EMPTY_IMAGE_FILE);
        }
        if (image.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new CustomException(ImageErrorCode.FILE_SIZE_EXCEEDED);
        }
        if (!SUPPORTED_EXTENSIONS.contains(extractExtension(image.getOriginalFilename()))) {
            throw new CustomException(ImageErrorCode.UNSUPPORTED_FORMAT);
        }
    }

    private String extractExtension(String filename) {
        if (filename == null || filename.isBlank()) {
            return "";
        }

        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }

        return filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "image";
        }

        // 임시로 파일명이 깨지지 않도록 영어 대/소문자, 숫자, '.', '_', '-'는 모두 '_'로 치환한다.
        return filename.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
