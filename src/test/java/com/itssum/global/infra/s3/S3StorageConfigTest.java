package com.itssum.global.infra.s3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

class S3StorageConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(S3StorageConfig.class, S3ObjectStorage.class);

    @Test
    void noBucketCreatesNoS3Beans() {
        runner.run(context -> assertS3Beans(context, 0));
    }

    @Test
    void blankBucketCreatesNoS3Beans() {
        runner.withPropertyValues("storage.s3.bucket=")
                .run(context -> assertS3Beans(context, 0));
    }

    @Test
    void bucketCreatesS3BeansInConfiguredRegion() {
        runner.withPropertyValues("storage.s3.bucket=itssum-images-6318", "storage.s3.region=ap-northeast-2")
                .run(context -> {
                    assertS3Beans(context, 1);
                    assertEquals(Region.AP_NORTHEAST_2,
                            context.getBean(S3Client.class).serviceClientConfiguration().region());
                });
    }

    private void assertS3Beans(AssertableApplicationContext context, int expected) {
        assertNull(context.getStartupFailure());
        assertEquals(expected, context.getBeanNamesForType(S3Client.class).length);
        assertEquals(expected, context.getBeanNamesForType(S3Presigner.class).length);
        assertEquals(expected, context.getBeanNamesForType(S3ObjectStorage.class).length);
    }
}
