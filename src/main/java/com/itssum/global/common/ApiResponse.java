package com.itssum.global.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.itssum.global.exception.ErrorCode;
import java.util.List;

import com.itssum.global.exception.SuccessCode;
import org.springframework.http.HttpStatus;

/**
 * 모든 API 응답 형식. 성공은 SuccessCode, 실패는 ErrorCode 의 code·message 를 같은 위치에 담는다.
 * status 는 JSON 에 나가지 않고 ApiResponseStatusAdvice 가 HTTP 응답 상태로 쓴다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        T data,
        List<FieldErrorBody> fieldErrors,
        @JsonIgnore HttpStatus status
) {

    public static <T> ApiResponse<T> ok(SuccessCode successCode, T data) {
        return new ApiResponse<>(true, successCode.getCode(), successCode.getMessage(), data, null,
                successCode.getStatus());
    }

    public static ApiResponse<Void> ok(SuccessCode successCode) {
        return ok(successCode, null);
    }

    public static <T> ApiResponse<T> ok(T data) {
        return ok(SuccessCode.OK, data);
    }

    public static ApiResponse<Void> ok() {
        return ok(SuccessCode.OK);
    }

    public static ApiResponse<Void> fail(ErrorCode errorCode) {
        return fail(errorCode, errorCode.getMessage());
    }

    public static ApiResponse<Void> fail(ErrorCode errorCode, String message) {
        return new ApiResponse<>(false, errorCode.getCode(), message, null, null, errorCode.getStatus());
    }

    public static ApiResponse<Void> fail(ErrorCode errorCode, List<FieldErrorBody> fieldErrors) {
        return new ApiResponse<>(false, errorCode.getCode(), errorCode.getMessage(), null, fieldErrors,
                errorCode.getStatus());
    }

    public record FieldErrorBody(
            String field,
            String reason
    ) {
    }
}
