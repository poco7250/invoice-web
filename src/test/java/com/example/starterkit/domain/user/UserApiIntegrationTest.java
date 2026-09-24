package com.example.starterkit.domain.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.starterkit.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** 사용자 API 통합 테스트. 실제 PostgreSQL + Flyway 마이그레이션 위에서 검증한다. */
class UserApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("사용자를 생성하면 201과 생성된 사용자 정보를 반환한다")
    void createUser() throws Exception {
        String body = """
                {"email": "newbie@example.com", "name": "신입"}
                """;

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("newbie@example.com"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("이메일 형식이 틀리면 400과 필드별 오류를 반환한다")
    void createUserWithInvalidEmail() throws Exception {
        String body = """
                {"email": "not-an-email", "name": "신입"}
                """;

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.error.violations[0].field").value("email"));
    }

    @Test
    @DisplayName("없는 사용자를 조회하면 404와 USER_NOT_FOUND를 반환한다")
    void findMissingUser() throws Exception {
        mockMvc.perform(get("/api/v1/users/{id}", 999_999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("USER_NOT_FOUND"));
    }
}
