package com.itssum.global.infra.s3;

import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@ConditionalOnProperty(prefix = "storage.s3", name = "bucket")
public class S3StorageConfig {

    @Bean
    S3Client s3Client() {
        return S3Client.builder()
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .apiCallTimeout(Duration.ofSeconds(10))
                        .build())
                .build();
    }

    @Bean
    S3Presigner s3Presigner() {
        return S3Presigner.create();
    }
}
