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

    public S3StorageService(
            AmazonS3 amazonS3,
            @Value("${cloud.aws.s3.bucket}") String bucket
    ) {
        this.amazonS3 = amazonS3;
        this.bucket = bucket;
    }

    public String upload(String key, MultipartFile file) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());

        try (InputStream inputStream = file.getInputStream()) {
            amazonS3.putObject(bucket, key, inputStream, metadata);
            return amazonS3.getUrl(bucket, key).toString();
        } catch (IOException e) {
            throw new CustomException(CommonErrorCode.SERVER_ERROR);
        }
    }
}
