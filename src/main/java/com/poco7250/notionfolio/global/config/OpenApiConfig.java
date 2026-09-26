package com.poco7250.notionfolio.global.config;

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
    public OpenAPI notionFolioOpenApi() {
        Info info = new Info()
                .title("Notion Folio API")
                .version("v1")
                .description("Notion을 CMS로 쓰는 백엔드 개발자 포트폴리오 API");

        return new OpenAPI().info(info);
    }
}
