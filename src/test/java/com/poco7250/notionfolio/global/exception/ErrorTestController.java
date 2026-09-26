package com.poco7250.notionfolio.global.exception;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 프레임워크 예외 변환을 검증하기 위한 테스트 전용 컨트롤러. 운영 빌드에는 포함되지 않는다. */
@RestController
@RequestMapping("/test/errors")
class ErrorTestController {

    /** 필수 헤더 누락 검증용 */
    @GetMapping("/header")
    String requireHeader(@RequestHeader("X-Test") String value) {
        return value;
    }

    /** 파라미터 타입 불일치 검증용 */
    @GetMapping("/type")
    int requireInt(@RequestParam int value) {
        return value;
    }
}
