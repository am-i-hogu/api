package com.hogu.am_i_hogu.domain.post.service;

import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.common.storage.S3StorageService;
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
    private static final Set<String> SUPPORTED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    );

    private final TsidGenerator tsidGenerator;
    private final S3StorageService s3StorageService;

    /**
     * 인증 사용자의 이미지를 S3에 업로드하고 반환된 CloudFront URL만 반환한다.
     *
     * @param image 업로드할 multipart 이미지 파일
     * @return 업로드된 이미지에 접근할 CloudFront URL
     * @throws CustomException 파일 검증에 실패한 경우
     */
    public String upload(MultipartFile image) {
        validate(image);

        long imageId = tsidGenerator.nextId();
        String extension = extractExtension(image.getOriginalFilename());
        String key = "images/%d.%s".formatted(imageId, extension);

        return s3StorageService.upload(key, image);
    }

    /**
     * 업로드 파일의 존재 여부, 크기, MIME 타입, 확장자를 이미지 업로드 정책에 따라 검증한다.
     *
     * @param image 검증할 multipart 이미지 파일
     * @throws CustomException 이미지 파일이 비었거나 크기 또는 형식 제한을 위반한 경우
     */
    private void validate(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new CustomException(ImageErrorCode.EMPTY_IMAGE_FILE);
        }
        if (image.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new CustomException(ImageErrorCode.FILE_SIZE_EXCEEDED);
        }
        String contentType = image.getContentType();
        if (contentType == null || !SUPPORTED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new CustomException(ImageErrorCode.UNSUPPORTED_FORMAT);
        }
        if (!SUPPORTED_EXTENSIONS.contains(extractExtension(image.getOriginalFilename()))) {
            throw new CustomException(ImageErrorCode.UNSUPPORTED_FORMAT);
        }
    }

    /**
     * 파일명에서 소문자로 정규화된 확장자를 추출한다.
     *
     * @param filename 확장자를 확인할 원본 파일명
     * @return 파일명에 유효한 확장자가 있으면 소문자 확장자, 아니면 빈 문자열
     */
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

}
