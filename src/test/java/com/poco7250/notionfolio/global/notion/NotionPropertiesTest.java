package com.poco7250.notionfolio.global.notion;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** NotionProperties 기본값 채움 단위 테스트 */
class NotionPropertiesTest {

    @Test
    @DisplayName("값이 없는 항목은 기본값으로 채운다")
    void fillDefaults() {
        NotionProperties properties = new NotionProperties(
                "token", "2026-03-11", "ds", "page", null, null, null, null, null, null);

        assertThat(properties.baseUrl()).isEqualTo("https://api.notion.com/v1");
        assertThat(properties.connectTimeout()).isEqualTo(Duration.ofSeconds(5));
        assertThat(properties.readTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(properties.maxAttempts()).isEqualTo(6);
        assertThat(properties.maxBackoff()).isEqualTo(Duration.ofSeconds(30));
        assertThat(properties.requestsPerSecond()).isEqualTo(3);
    }

    @Test
    @DisplayName("값이 있는 항목은 기본값으로 덮어쓰지 않는다")
    void keepGivenValues() {
        NotionProperties properties = new NotionProperties(
                "token", "v", "ds", "page", "http://localhost", Duration.ofSeconds(1),
                Duration.ofSeconds(2), 3, Duration.ofSeconds(4), 5);

        assertThat(properties.baseUrl()).isEqualTo("http://localhost");
        assertThat(properties.maxAttempts()).isEqualTo(3);
        assertThat(properties.requestsPerSecond()).isEqualTo(5);
    }
}
