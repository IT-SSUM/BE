package com.itssum.global.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itssum.global.exception.ErrorCode;
import java.util.List;

/**
 * 공통 응답 포맷.
 * <pre>
 * 성공: { "success": true,  "data": { ... } }
 * 실패: { "success": false, "error": { "code": "COMMON_400", "message": "...", "fieldErrors": [ ... ] } }
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        T data,
        ErrorBody error
) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null, null);
    }

    public static ApiResponse<Void> fail(ErrorCode errorCode) {
        return fail(errorCode, errorCode.getMessage());
    }

    public static ApiResponse<Void> fail(ErrorCode errorCode, String message) {
        return new ApiResponse<>(false, null, new ErrorBody(errorCode.getCode(), message, null));
    }

    public static ApiResponse<Void> fail(ErrorCode errorCode, List<FieldErrorBody> fieldErrors) {
        return new ApiResponse<>(false, null,
                new ErrorBody(errorCode.getCode(), errorCode.getMessage(), fieldErrors));
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ErrorBody(
            String code,
            String message,
            List<FieldErrorBody> fieldErrors
    ) {
    }

    public record FieldErrorBody(
            String field,
            String reason
    ) {
    }
}
