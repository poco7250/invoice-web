package com.poco7250.notionfolio.domain.sync;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Notion 전체 동기화 스케줄 설정.
 *
 * @param scheduler  스케줄러 설정
 * @param fixedDelay 이전 동기화가 끝난 뒤 다음 동기화까지의 간격
 */
@ConfigurationProperties(prefix = "sync")
public record SyncProperties(Scheduler scheduler, Duration fixedDelay) {

    /** 값이 없는 항목을 기본값으로 채운다. */
    public SyncProperties {
        if (scheduler == null) scheduler = new Scheduler(null);
        if (fixedDelay == null) fixedDelay = Duration.ofMinutes(30);
    }

    /**
     * 스케줄러 설정.
     *
     * @param enabled 스케줄 동기화 사용 여부 (테스트/로컬에서 끌 수 있다)
     */
    public record Scheduler(Boolean enabled) {

        /** 값이 없으면 스케줄러를 켠다. */
        public Scheduler {
            if (enabled == null) enabled = true;
        }
    }
}
