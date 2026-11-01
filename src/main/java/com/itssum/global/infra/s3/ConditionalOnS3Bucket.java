package com.itssum.global.infra.s3;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;

/** storage.s3.bucket 이 없거나 비어 있으면 빈을 만들지 않는다. 버킷은 운영 설정에만 있어 로컬·테스트에서는 S3를 쓰지 않는다. */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ConditionalOnExpression("!'${storage.s3.bucket:}'.isBlank()")
public @interface ConditionalOnS3Bucket {
}
