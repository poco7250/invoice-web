package com.example.starterkit.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing 활성화.
 * @WebMvcTest 등 슬라이스 테스트에서 Auditing 빈이 불필요하게 로딩되지 않도록
 * 메인 애플리케이션 클래스가 아닌 별도 설정으로 분리했다.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
