package com.example.starterkit.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI 문서 메타 정보.
 * 실행 후 http://localhost:8080/swagger-ui.html 에서 확인한다.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI starterKitOpenApi() {
        Info info = new Info()
                .title("Spring Starter Kit API")
                .version("v1")
                .description("레이어드 아키텍처 기반 Spring Boot 스타터 킷");

        return new OpenAPI().info(info);
    }
}
