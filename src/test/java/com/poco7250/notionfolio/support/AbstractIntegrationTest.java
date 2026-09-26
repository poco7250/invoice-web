package com.example.starterkit.support;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * 통합 테스트 공통 베이스.
 * 실제 PostgreSQL 컨테이너를 띄워 운영 환경과 같은 DB로 검증한다. (H2 대체 금지)
 * Docker가 없는 환경에서는 disabledWithoutDocker 옵션으로 자동 skip된다.
 */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
public abstract class AbstractIntegrationTest {

    /**
     * @ServiceConnection이 컨테이너의 JDBC URL/계정을 스프링 컨텍스트에 자동 주입한다.
     * static이므로 컨테이너는 테스트 클래스 전체에서 한 번만 뜬다.
     */
    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");
}
