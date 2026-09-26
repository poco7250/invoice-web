package com.poco7250.notionfolio.domain.sync;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** SyncProperties 기본값 채움 단위 테스트 */
class SyncPropertiesTest {

    @Test
    @DisplayName("설정이 없으면 스케줄러를 켜고 30분 간격을 쓴다")
    void fillDefaults() {
        SyncProperties properties = new SyncProperties(null, null);

        assertThat(properties.scheduler().enabled()).isTrue();
        assertThat(properties.fixedDelay()).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    @DisplayName("스케줄러를 끈 설정은 그대로 유지한다")
    void keepDisabledScheduler() {
        SyncProperties properties = new SyncProperties(new SyncProperties.Scheduler(false), Duration.ofMinutes(5));

        assertThat(properties.scheduler().enabled()).isFalse();
        assertThat(properties.fixedDelay()).isEqualTo(Duration.ofMinutes(5));
    }
}
