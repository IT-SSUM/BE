package com.itssum.global.infra.s3;

import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/** 도메인에 관계없는 S3 서명 URL과 객체 조회. 접근 권한과 key 규칙은 각 도메인에서 판단한다. */
@Component
@ConditionalOnProperty(prefix = "storage.s3", name = "bucket")
@RequiredArgsConstructor
public class S3ObjectStorage {

    private static final Duration URL_LIFETIME = Duration.ofMinutes(10);

    private final S3Client s3Client;
    private final S3Presigner presigner;

    @Value("${storage.s3.bucket}")
    private String bucket;

    public String presignUpload(String key, String contentType) {
        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(bucket).key(key).contentType(contentType).ifNoneMatch("*").build();
        return presigner.presignPutObject(PutObjectPresignRequest.builder()
                .signatureDuration(URL_LIFETIME).putObjectRequest(put).build()).url().toString();
    }

    public String presignDownload(String key) {
        GetObjectRequest get = GetObjectRequest.builder().bucket(bucket).key(key).build();
        return presigner.presignGetObject(GetObjectPresignRequest.builder()
                .signatureDuration(URL_LIFETIME).getObjectRequest(get).build()).url().toString();
    }

    public Optional<StoredObjectMetadata> find(String key) {
        try {
            HeadObjectResponse head = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucket).key(key).build());
            return Optional.of(new StoredObjectMetadata(head.contentLength(), head.contentType()));
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return Optional.empty();
            }
            throw e;
        }
    }

    public void delete(String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }

    public record StoredObjectMetadata(Long size, String contentType) {
    }
}
