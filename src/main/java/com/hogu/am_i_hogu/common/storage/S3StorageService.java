package com.hogu.am_i_hogu.common.storage;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.hogu.am_i_hogu.common.exception.CommonErrorCode;
import com.hogu.am_i_hogu.common.exception.CustomException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

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

    private String removeTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }
}
