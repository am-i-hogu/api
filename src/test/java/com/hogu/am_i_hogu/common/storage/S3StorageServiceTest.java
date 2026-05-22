package com.hogu.am_i_hogu.common.storage;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class S3StorageServiceTest {

    @Test
    void uploadStoresFileAndReturnsS3Url() throws Exception {
        AmazonS3 amazonS3 = mock(AmazonS3.class);
        S3StorageService s3StorageService = new S3StorageService(
                amazonS3,
                "am-i-hogu-images"
        );
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "post-image.jpg",
                "image/jpeg",
                "image-content".getBytes()
        );
        when(amazonS3.getUrl("am-i-hogu-images", "images/posts/1.jpg"))
                .thenReturn(new URL("https://am-i-hogu-images.s3.ap-northeast-2.amazonaws.com/images/posts/1.jpg"));

        String imageUrl = s3StorageService.upload("images/posts/1.jpg", image);

        assertThat(imageUrl).isEqualTo("https://am-i-hogu-images.s3.ap-northeast-2.amazonaws.com/images/posts/1.jpg");
        verify(amazonS3).putObject(
                eq("am-i-hogu-images"),
                eq("images/posts/1.jpg"),
                any(InputStream.class),
                any(ObjectMetadata.class)
        );
    }
}
