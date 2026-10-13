package com.itssum.global.exception;

import com.itssum.global.common.ApiResponse;
import com.itssum.global.common.ApiResponse.FieldErrorBody;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        log.warn("BusinessException: code={}, message={}", e.getErrorCode().getCode(), e.getMessage());
        return toResponse(e.getErrorCode(), ApiResponse.fail(e.getErrorCode(), e.getMessage()));
    }

    /** @Valid @RequestBody 검증 실패 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        List<FieldErrorBody> fieldErrors = e.getBindingResult().getAllErrors().stream()
                .map(error -> new FieldErrorBody(
                        error instanceof FieldError fieldError ? fieldError.getField() : error.getObjectName(),
                        error.getDefaultMessage()))
                .toList();
        return toResponse(ErrorCode.INVALID_INPUT, ApiResponse.fail(ErrorCode.INVALID_INPUT, fieldErrors));
    }

    /** @RequestParam·@PathVariable 등에 직접 붙인 제약 조건 검증 실패 (메서드 검증) */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleHandlerMethodValidation(HandlerMethodValidationException e) {
        List<FieldErrorBody> fieldErrors = e.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new FieldErrorBody(
                                error instanceof FieldError fieldError
                                        ? fieldError.getField()
                                        : result.getMethodParameter().getParameterName(),
                                error.getDefaultMessage())))
                .toList();
        return toResponse(ErrorCode.INVALID_INPUT, ApiResponse.fail(ErrorCode.INVALID_INPUT, fieldErrors));
    }

    /** @Validated 클래스(서비스 등)의 메서드 검증 실패 */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException e) {
        List<FieldErrorBody> fieldErrors = e.getConstraintViolations().stream()
                .map(violation -> new FieldErrorBody(violation.getPropertyPath().toString(), violation.getMessage()))
                .toList();
        return toResponse(ErrorCode.INVALID_INPUT, ApiResponse.fail(ErrorCode.INVALID_INPUT, fieldErrors));
    }

    /** 본문 JSON 파싱 실패, 필수 파라미터 누락, 파라미터 타입 불일치 */
    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception e) {
        log.debug("Bad request: {}", e.getMessage());
        return toResponse(ErrorCode.INVALID_INPUT, ApiResponse.fail(ErrorCode.INVALID_INPUT));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return toResponse(ErrorCode.METHOD_NOT_ALLOWED, ApiResponse.fail(ErrorCode.METHOD_NOT_ALLOWED));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(NoResourceFoundException e) {
        return toResponse(ErrorCode.RESOURCE_NOT_FOUND, ApiResponse.fail(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return toResponse(ErrorCode.INTERNAL_SERVER_ERROR, ApiResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR));
    }

    private ResponseEntity<ApiResponse<Void>> toResponse(ErrorCode errorCode, ApiResponse<Void> body) {
        return ResponseEntity.status(errorCode.getStatus()).body(body);
    }
}
