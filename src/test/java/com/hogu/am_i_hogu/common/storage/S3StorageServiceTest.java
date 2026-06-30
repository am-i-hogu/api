package com.hogu.am_i_hogu.common.storage;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class S3StorageServiceTest {

    @Test
    void uploadStoresFileAndReturnsCloudFrontUrl() {
        AmazonS3 amazonS3 = mock(AmazonS3.class);
        S3StorageService s3StorageService = new S3StorageService(
                amazonS3,
                "am-i-hogu-images",
                "https://d111111abcdef8.cloudfront.net"
        );
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "post-image.jpg",
                "image/jpeg",
                "image-content".getBytes()
        );

        String imageUrl = s3StorageService.upload("images/posts/1.jpg", image);

        assertThat(imageUrl).isEqualTo("https://d111111abcdef8.cloudfront.net/images/posts/1.jpg");
        verify(amazonS3).putObject(
                eq("am-i-hogu-images"),
                eq("images/posts/1.jpg"),
                any(InputStream.class),
                any(ObjectMetadata.class)
        );
    }

    @Test
    void findImageMetadataReturnsMetadataForCloudFrontUrl() {
        AmazonS3 amazonS3 = mock(AmazonS3.class);
        S3StorageService s3StorageService = new S3StorageService(
                amazonS3,
                "am-i-hogu-images",
                "https://d111111abcdef8.cloudfront.net"
        );
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("image/jpeg");
        metadata.setContentLength(123L);
        when(amazonS3.getObjectMetadata("am-i-hogu-images", "images/1.jpg"))
                .thenReturn(metadata);

        Optional<S3StorageService.ImageMetadata> result =
                s3StorageService.findImageMetadata("https://d111111abcdef8.cloudfront.net/images/1.jpg");

        assertThat(result).isPresent();
        assertThat(result.get().contentType()).isEqualTo("image/jpeg");
        assertThat(result.get().sizeBytes()).isEqualTo(123L);
    }

    @Test
    void findImageMetadataReturnsEmptyForUrlOutsideCloudFrontDomain() {
        AmazonS3 amazonS3 = mock(AmazonS3.class);
        S3StorageService s3StorageService = new S3StorageService(
                amazonS3,
                "am-i-hogu-images",
                "https://d111111abcdef8.cloudfront.net"
        );

        Optional<S3StorageService.ImageMetadata> result =
                s3StorageService.findImageMetadata("https://external.example.com/images/1.jpg");

        assertThat(result).isEmpty();
        verifyNoInteractions(amazonS3);
    }
}
