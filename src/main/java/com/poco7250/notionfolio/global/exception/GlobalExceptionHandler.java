package com.poco7250.notionfolio.global.exception;

import com.poco7250.notionfolio.global.response.ApiResponse;
import com.poco7250.notionfolio.global.response.ErrorDetail;
import com.poco7250.notionfolio.global.response.ErrorDetail.FieldViolation;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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

        return toResponse(errorCode, ErrorDetail.of(errorCode, e.getMessage()));
    }

    /** @Valid 검증 실패. 어떤 필드가 왜 실패했는지 그대로 내려준다. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        List<FieldViolation> violations = e.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldViolation(error.getField(), error.getDefaultMessage()))
                .toList();
        log.warn("검증 실패: {}", violations);

        return toResponse(ErrorCode.INVALID_INPUT, ErrorDetail.of(ErrorCode.INVALID_INPUT, violations));
    }

    /** 매핑되지 않은 경로. 스캐너 요청이 많아 DEBUG로만 남긴다. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResource(NoResourceFoundException e) {
        log.debug("없는 경로 요청: path={}", e.getResourcePath());

        return toResponse(ErrorCode.RESOURCE_NOT_FOUND, ErrorDetail.of(ErrorCode.RESOURCE_NOT_FOUND));
    }

    /** 필수 헤더 누락. 어떤 헤더가 빠졌는지 메시지에 담는다. */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingHeader(MissingRequestHeaderException e) {
        String message = "필수 헤더가 없습니다: header=" + e.getHeaderName();
        log.warn("헤더 누락: header={}", e.getHeaderName());

        return toResponse(ErrorCode.INVALID_INPUT, ErrorDetail.of(ErrorCode.INVALID_INPUT, message));
    }

    /** 파라미터 타입 불일치. 입력값은 응답에 반사하지 않고 파라미터명만 알려준다. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String message = "파라미터 타입이 올바르지 않습니다: param=" + e.getName();
        log.warn("타입 불일치: param={}", e.getName());

        return toResponse(ErrorCode.INVALID_INPUT, ErrorDetail.of(ErrorCode.INVALID_INPUT, message));
    }

    /** 지원하지 않는 HTTP 메서드. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("지원하지 않는 메서드: method={}", e.getMethod());

        return toResponse(ErrorCode.METHOD_NOT_ALLOWED, ErrorDetail.of(ErrorCode.METHOD_NOT_ALLOWED));
    }

    /** 처리하지 못한 모든 예외. 원인 추적을 위해 스택트레이스를 남긴다. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
        log.error("처리되지 않은 예외", e);

        return toResponse(ErrorCode.INTERNAL_ERROR, ErrorDetail.of(ErrorCode.INTERNAL_ERROR));
    }

    /** ErrorCode의 HTTP 상태와 실패 응답 본문을 묶어 응답을 만든다. */
    private ResponseEntity<ApiResponse<Void>> toResponse(ErrorCode errorCode, ErrorDetail detail) {
        return ResponseEntity.status(errorCode.getHttpStatus()).body(ApiResponse.fail(detail));
    }
}
