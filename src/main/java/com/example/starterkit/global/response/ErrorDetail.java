package com.example.starterkit.global.response;

import com.example.starterkit.global.exception.ErrorCode;
import java.util.List;

/**
 * 오류 응답 본문.
 *
 * @param code       클라이언트가 분기에 쓰는 오류 코드 (예: USER_NOT_FOUND)
 * @param message    사람이 읽는 오류 메시지
 * @param violations 필드 단위 검증 실패 목록 (없으면 null)
 */
public record ErrorDetail(String code, String message, List<FieldViolation> violations) {

    public static ErrorDetail of(ErrorCode errorCode) {
        return new ErrorDetail(errorCode.name(), errorCode.getMessage(), null);
    }

    public static ErrorDetail of(ErrorCode errorCode, String message) {
        return new ErrorDetail(errorCode.name(), message, null);
    }

    public static ErrorDetail of(ErrorCode errorCode, List<FieldViolation> violations) {
        return new ErrorDetail(errorCode.name(), errorCode.getMessage(), violations);
    }

    /** 검증 실패한 개별 필드 정보. */
    public record FieldViolation(String field, String reason) {
    }
}
