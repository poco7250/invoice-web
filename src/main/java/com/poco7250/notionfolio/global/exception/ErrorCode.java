package com.poco7250.notionfolio.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 애플리케이션 전역 오류 코드.
 * 새 오류를 추가할 때는 HTTP 상태와 기본 메시지를 함께 정의한다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    // 프로젝트
    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "프로젝트를 찾을 수 없습니다."),

    // 프로필
    PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "프로필이 아직 동기화되지 않았습니다."),

    // 이미지
    IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "이미지를 찾을 수 없습니다."),

    // Notion 연동
    NOTION_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "Notion API를 사용할 수 없습니다."),
    NOTION_RATE_LIMITED(HttpStatus.SERVICE_UNAVAILABLE, "Notion API 호출 한도를 초과했습니다. 잠시 후 다시 시도해 주세요."),
    INVALID_WEBHOOK_SIGNATURE(HttpStatus.UNAUTHORIZED, "Webhook 서명이 올바르지 않습니다."),

    // 관리자·동기화
    INVALID_ADMIN_KEY(HttpStatus.UNAUTHORIZED, "관리자 키가 없거나 올바르지 않습니다."),
    SYNC_ALREADY_RUNNING(HttpStatus.CONFLICT, "동기화가 이미 실행 중입니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
