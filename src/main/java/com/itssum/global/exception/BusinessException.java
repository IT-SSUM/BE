package com.itssum.global.exception;

import lombok.Getter;

/**
 * 서비스 계층에서 던지는 예외. GlobalExceptionHandler가 ErrorCode의 HTTP 상태와 코드로 응답한다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
