package com.itssum.global.infra.s3;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

class S3ObjectStorageTest {

    @Test
    void uploadUrlSignsConditionalWriteHeader() {
        try (S3Presigner presigner = S3Presigner.builder()
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("access", "secret")))
                .region(Region.AP_NORTHEAST_2)
                .build()) {
            S3ObjectStorage storage = new S3ObjectStorage(mock(S3Client.class), presigner);
            ReflectionTestUtils.setField(storage, "bucket", "example-bucket");

            String url = storage.presignUpload("chat/42/image.jpg", "image/jpeg");
            String query = URLDecoder.decode(URI.create(url).getRawQuery(), StandardCharsets.UTF_8);

            assertTrue(query.contains("if-none-match"));
        }
    }
}
