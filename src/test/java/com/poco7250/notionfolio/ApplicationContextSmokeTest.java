package com.poco7250.notionfolio;

import static org.assertj.core.api.Assertions.assertThat;

import com.poco7250.notionfolio.domain.sync.SyncProperties;
import com.poco7250.notionfolio.global.admin.AdminProperties;
import com.poco7250.notionfolio.global.notion.NotionProperties;
import com.poco7250.notionfolio.support.AbstractIntegrationTest;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** 컨텍스트 기동과 설정 프로퍼티 바인딩 스모크 테스트. 마이그레이션 0개 상태에서도 기동해야 한다. */
class ApplicationContextSmokeTest extends AbstractIntegrationTest {

    @Autowired
    private NotionProperties notionProperties;

    @Autowired
    private AdminProperties adminProperties;

    @Autowired
    private SyncProperties syncProperties;

    @Test
    @DisplayName("Notion 설정은 테스트 더미값과 기본값으로 바인딩된다")
    void bindNotionProperties() {
        assertThat(notionProperties.token()).isEqualTo("test-notion-token");
        assertThat(notionProperties.version()).isEqualTo("2026-03-11");
        assertThat(notionProperties.projectsDataSourceId()).isEqualTo("test-data-source-id");
        assertThat(notionProperties.baseUrl()).isEqualTo("https://api.notion.com/v1");
        assertThat(notionProperties.maxAttempts()).isEqualTo(6);
    }

    @Test
    @DisplayName("관리자 키와 동기화 설정이 바인딩되고 테스트에서는 스케줄러가 꺼져 있다")
    void bindAdminAndSyncProperties() {
        assertThat(adminProperties.apiKey()).isEqualTo("test-admin-key");
        assertThat(syncProperties.scheduler().enabled()).isFalse();
        assertThat(syncProperties.fixedDelay()).isEqualTo(Duration.ofMinutes(30));
    }
}
