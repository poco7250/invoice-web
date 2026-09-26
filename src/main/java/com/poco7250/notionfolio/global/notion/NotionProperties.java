package com.poco7250.notionfolio.global.notion;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Notion API 연동 설정.
 * 비밀값(token, ID)은 환경변수로만 주입하고, 비어 있는지는 실제 호출 시점에 검증한다.
 *
 * @param token                Integration 토큰 (NOTION_TOKEN)
 * @param version              Notion-Version 헤더 값 (NOTION_VERSION)
 * @param projectsDataSourceId Projects data source ID (database ID가 아님)
 * @param aboutPageId          About 페이지 ID
 * @param baseUrl              API 기본 URL
 * @param connectTimeout       연결 타임아웃
 * @param readTimeout          읽기 타임아웃
 * @param maxAttempts          최대 시도 횟수 (첫 요청 포함)
 * @param maxBackoff           재시도 대기 상한
 * @param requestsPerSecond    초당 최대 요청 수
 */
@ConfigurationProperties(prefix = "notion")
public record NotionProperties(
        String token,
        String version,
        String projectsDataSourceId,
        String aboutPageId,
        String baseUrl,
        Duration connectTimeout,
        Duration readTimeout,
        Integer maxAttempts,
        Duration maxBackoff,
        Integer requestsPerSecond
) {

    private static final String DEFAULT_BASE_URL = "https://api.notion.com/v1";

    /** 값이 없는 항목을 기본값으로 채운다. */
    public NotionProperties {
        if (baseUrl == null) baseUrl = DEFAULT_BASE_URL;
        if (connectTimeout == null) connectTimeout = Duration.ofSeconds(5);
        if (readTimeout == null) readTimeout = Duration.ofSeconds(30);
        if (maxAttempts == null) maxAttempts = 6;
        if (maxBackoff == null) maxBackoff = Duration.ofSeconds(30);
        if (requestsPerSecond == null) requestsPerSecond = 3;
    }
}
