package com.itssum.global.infra.s3;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

class S3ObjectStorageTest {

    @Test
    void deleteUsesCheckedETagAndPreservesChangedObject() {
        S3Client client = mock(S3Client.class);
        S3ObjectStorage storage = new S3ObjectStorage(client, mock(S3Presigner.class));
        ReflectionTestUtils.setField(storage, "bucket", "example-bucket");
        when(client.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(412).build());

        assertDoesNotThrow(() -> storage.delete("chat/42/image.jpg", "etag-1"));

        ArgumentCaptor<DeleteObjectRequest> request = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(client).deleteObject(request.capture());
        assertEquals("etag-1", request.getValue().ifMatch());
    }

    @Test
    void readHeadRequestsOnlyLeadingBytesOfCheckedObject() {
        S3Client client = mock(S3Client.class);
        S3ObjectStorage storage = new S3ObjectStorage(client, mock(S3Presigner.class));
        ReflectionTestUtils.setField(storage, "bucket", "example-bucket");
        byte[] head = {1, 2, 3};
        when(client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenReturn(ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), head));

        assertArrayEquals(head, storage.readHead("chat/42/image.jpg", "etag-1", 12));

        ArgumentCaptor<GetObjectRequest> request = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(client).getObjectAsBytes(request.capture());
        assertEquals("bytes=0-11", request.getValue().range());
        assertEquals("etag-1", request.getValue().ifMatch());
    }

    @Test
    void readHeadOfMissingOrChangedObjectIsEmpty() {
        S3Client client = mock(S3Client.class);
        S3ObjectStorage storage = new S3ObjectStorage(client, mock(S3Presigner.class));
        ReflectionTestUtils.setField(storage, "bucket", "example-bucket");
        when(client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(404).build())
                .thenThrow(S3Exception.builder().statusCode(412).build());

        assertEquals(0, storage.readHead("chat/42/image.jpg", "etag-1", 12).length);
        assertEquals(0, storage.readHead("chat/42/image.jpg", "etag-1", 12).length);
    }

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

    @Test
    void readUrlLastsOneHourAndUploadUrlTenMinutes() {
        try (S3Presigner presigner = S3Presigner.builder()
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("access", "secret")))
                .region(Region.AP_NORTHEAST_2)
                .build()) {
            S3ObjectStorage storage = new S3ObjectStorage(mock(S3Client.class), presigner);
            ReflectionTestUtils.setField(storage, "bucket", "example-bucket");

            String readQuery = URI.create(storage.presignDownload("chat/42/image.jpg")).getRawQuery();
            String uploadQuery = URI.create(storage.presignUpload("chat/42/image.jpg", "image/jpeg")).getRawQuery();

            assertTrue(readQuery.contains("X-Amz-Expires=3600"));
            assertTrue(uploadQuery.contains("X-Amz-Expires=600"));
        }
    }
}
