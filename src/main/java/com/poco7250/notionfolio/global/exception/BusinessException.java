package com.example.starterkit.global.exception;

import lombok.Getter;

/**
 * 비즈니스 규칙 위반을 나타내는 예외.
 * GlobalExceptionHandler가 ErrorCode의 HTTP 상태로 변환해 응답한다.
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
