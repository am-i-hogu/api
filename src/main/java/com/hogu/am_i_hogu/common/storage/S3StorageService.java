package com.hogu.am_i_hogu.common.storage;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.hogu.am_i_hogu.common.exception.CommonErrorCode;
import com.hogu.am_i_hogu.common.exception.CustomException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

@Service
public class S3StorageService {

    private final AmazonS3 amazonS3;
    private final String bucket;
    private final String cloudFrontDomain;

    public S3StorageService(
            AmazonS3 amazonS3,
            @Value("${cloud.aws.s3.bucket}") String bucket,
            @Value("${cloud.aws.cloudfront.domain}") String cloudFrontDomain
    ) {
        this.amazonS3 = amazonS3;
        this.bucket = bucket;
        this.cloudFrontDomain = removeTrailingSlash(cloudFrontDomain);
    }

    public String upload(String key, MultipartFile file) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());

        try (InputStream inputStream = file.getInputStream()) {
            amazonS3.putObject(bucket, key, inputStream, metadata);
            return "%s/%s".formatted(cloudFrontDomain, key);
        } catch (IOException e) {
            throw new CustomException(CommonErrorCode.SERVER_ERROR);
        }
    }

    public Optional<ImageMetadata> findImageMetadata(String imageUrl) {
        return extractKey(imageUrl).flatMap(key -> {
            try {
                ObjectMetadata metadata = amazonS3.getObjectMetadata(bucket, key);
                return Optional.of(new ImageMetadata(metadata.getContentType(), metadata.getContentLength()));
            } catch (AmazonS3Exception e) {
                if (e.getStatusCode() == 404) {
                    return Optional.empty();
                }
                throw new CustomException(CommonErrorCode.SERVER_ERROR);
            } catch (SdkClientException e) {
                throw new CustomException(CommonErrorCode.SERVER_ERROR);
            }
        });
    }

    private Optional<String> extractKey(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return Optional.empty();
        }

        String prefix = cloudFrontDomain + "/";
        if (!imageUrl.startsWith(prefix)) {
            return Optional.empty();
        }

        String key = imageUrl.substring(prefix.length());
        if (key.isBlank()) {
            return Optional.empty();
        }

        return Optional.of(key);
    }

    private String removeTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    public record ImageMetadata(
            String contentType,
            Long sizeBytes
    ) {
    }
}
