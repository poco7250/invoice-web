package com.poco7250.notionfolio.global.admin;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 관리자 API 보호 설정.
 *
 * @param apiKey X-Admin-Key 헤더와 비교할 키 (ADMIN_API_KEY)
 */
@ConfigurationProperties(prefix = "admin")
public record AdminProperties(String apiKey) {
}
