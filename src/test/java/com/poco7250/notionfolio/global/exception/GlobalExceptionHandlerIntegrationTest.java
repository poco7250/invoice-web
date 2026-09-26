package com.poco7250.notionfolio.global.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.poco7250.notionfolio.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** 공통 에러 응답 포맷 통합 테스트. 모든 실패 응답은 success=false와 error.code를 가진다. */
class GlobalExceptionHandlerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("없는 경로를 요청하면 404와 RESOURCE_NOT_FOUND를 반환한다")
    void unknownPath() throws Exception {
        expectFailure(mockMvc.perform(get("/api/unknown")), "RESOURCE_NOT_FOUND")
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("제거한 샘플 사용자 API는 더 이상 노출되지 않는다")
    void removedUserApi() throws Exception {
        expectFailure(mockMvc.perform(get("/api/users")), "RESOURCE_NOT_FOUND")
                .andExpect(status().isNotFound());
        expectFailure(mockMvc.perform(get("/api/v1/users/1")), "RESOURCE_NOT_FOUND")
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("허용되지 않은 메서드로 요청하면 405와 METHOD_NOT_ALLOWED를 반환한다")
    void methodNotAllowed() throws Exception {
        expectFailure(mockMvc.perform(delete("/test/errors/type")), "METHOD_NOT_ALLOWED")
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("필수 헤더가 없으면 400과 헤더명이 담긴 메시지를 반환한다")
    void missingHeader() throws Exception {
        expectFailure(mockMvc.perform(get("/test/errors/header")), "INVALID_INPUT")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("필수 헤더가 없습니다: header=X-Test"));
    }

    @Test
    @DisplayName("파라미터 타입이 틀리면 400을 반환하고 입력값은 응답에 반사하지 않는다")
    void typeMismatch() throws Exception {
        expectFailure(mockMvc.perform(get("/test/errors/type").param("value", "<script>")), "INVALID_INPUT")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("파라미터 타입이 올바르지 않습니다: param=value"));
    }

    /** 실패 응답 공통 포맷을 검증한다. non_null 설정 때문에 data 키는 직렬화되지 않는다. */
    private ResultActions expectFailure(ResultActions actions, String errorCode) throws Exception {
        return actions
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.error.code").value(errorCode));
    }
}
