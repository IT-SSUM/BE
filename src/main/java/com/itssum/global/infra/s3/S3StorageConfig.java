package com.itssum.global.infra.s3;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@ConditionalOnS3Bucket
public class S3StorageConfig {

    // EC2 메타데이터에서 리전을 찾지 않고 설정값으로 고정한다
    @Value("${storage.s3.region:ap-northeast-2}")
    private String region;

    @Bean
    S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .apiCallTimeout(Duration.ofSeconds(10))
                        .build())
                .build();
    }

    @Bean
    S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .region(Region.of(region))
                .build();
    }
}
