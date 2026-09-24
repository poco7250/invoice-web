package com.example.starterkit.global.exception;

import com.example.starterkit.global.response.ApiResponse;
import com.example.starterkit.global.response.ErrorDetail;
import com.example.starterkit.global.response.ErrorDetail.FieldViolation;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 모든 컨트롤러의 예외를 공통 응답 포맷으로 변환한다.
 * 핸들러를 추가할 때는 구체 예외 -> 일반 예외 순서를 유지한다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 비즈니스 규칙 위반. 예상된 흐름이므로 WARN으로만 남긴다. */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("비즈니스 예외: code={}, message={}", errorCode.name(), e.getMessage());

        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.fail(ErrorDetail.of(errorCode, e.getMessage())));
    }

    /** @Valid 검증 실패. 어떤 필드가 왜 실패했는지 그대로 내려준다. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        List<FieldViolation> violations = e.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldViolation(error.getField(), error.getDefaultMessage()))
                .toList();
        log.warn("검증 실패: {}", violations);

        return ResponseEntity.status(ErrorCode.INVALID_INPUT.getHttpStatus())
                .body(ApiResponse.fail(ErrorDetail.of(ErrorCode.INVALID_INPUT, violations)));
    }

    /** 처리하지 못한 모든 예외. 원인 추적을 위해 스택트레이스를 남긴다. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
        log.error("처리되지 않은 예외", e);

        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.getHttpStatus())
                .body(ApiResponse.fail(ErrorDetail.of(ErrorCode.INTERNAL_ERROR)));
    }
}
