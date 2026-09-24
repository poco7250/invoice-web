package com.example.starterkit.global.response;

/**
 * 모든 API의 공통 응답 래퍼.
 * null 필드는 application.yml의 jackson.default-property-inclusion 설정으로 직렬화에서 제외된다.
 *
 * @param success 성공 여부
 * @param data    성공 시 payload
 * @param error   실패 시 오류 상세
 */
public record ApiResponse<T>(boolean success, T data, ErrorDetail error) {

    /** 데이터를 담은 성공 응답을 만든다. */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    /** 반환할 데이터가 없는 성공 응답을 만든다. */
    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null, null);
    }

    /** 오류 상세를 담은 실패 응답을 만든다. */
    public static <T> ApiResponse<T> fail(ErrorDetail error) {
        return new ApiResponse<>(false, null, error);
    }
}
