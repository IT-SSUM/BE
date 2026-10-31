package com.itssum.global.validation;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 부분 수정 요청용: null(보내지 않음)이거나, strip() 한 뒤에도 글자가 남아야 한다.
 * {@code @NotBlank} 와 정규식 {@code \S} 는 전각 공백(U+3000)을 공백으로 보지 않아서, 서비스가 strip() 한 값과 기준이 어긋난다.
 */
@Documented
@Constraint(validatedBy = NullOrNotBlankValidator.class)
@Target({FIELD, METHOD, PARAMETER, ANNOTATION_TYPE})
@Retention(RUNTIME)
public @interface NullOrNotBlank {

    String message() default "공백만 입력할 수 없습니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
