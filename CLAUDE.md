# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Java 25 + Spring Boot 4.1.1 기반 레이어드 아키텍처 스타터 킷. 새 프로젝트의 베이스로 복제해서 쓰는 것이 목적이다.

## 명령어

로컬에 JDK 25가 없어도 된다. Gradle 툴체인(foojay resolver)이 자동으로 받아온다. 로컬 `gradle` 설치도 불필요하며 항상 `./gradlew`를 쓴다.

```bash
./gradlew build                          # 컴파일 + 테스트 + bootJar
./gradlew bootRun                        # 실행 (compose.yaml의 PostgreSQL 자동 기동)
./gradlew test                           # 전체 테스트
./gradlew test --tests '*UserTest'       # 단일 테스트 클래스
./gradlew test --tests '*UserTest.changeName'   # 단일 테스트 메서드
```

- http://localhost:8080/swagger-ui.html — API 문서
- http://localhost:8080/actuator/health — 헬스체크

**Docker가 없으면 통합 테스트는 조용히 SKIPPED로 빠진다.** `@Testcontainers(disabledWithoutDocker = true)` 때문이다. 테스트 결과를 보고할 때 SKIPPED를 PASSED로 착각하면 안 된다. 통합 테스트까지 실제로 검증하려면 Docker가 떠 있어야 한다.

린트 도구는 아직 설정되어 있지 않다.

## 버전 관리 정책

**Spring Boot BOM이 관리하는 의존성에는 버전을 쓰지 않는다.** Hibernate, Flyway, Testcontainers, PostgreSQL 드라이버, JUnit, AssertJ, Lombok, Logback, Micrometer가 여기 해당한다. 개별 최신 버전을 찾아 박으면 BOM이 검증한 조합이 깨진다.

BOM이 관리하지 않는 것(springdoc, MapStruct 등)만 `gradle/libs.versions.toml`에 선언한다.

버전을 올리거나 새로 고를 때는 기억이나 추측에 의존하지 말고 실제 릴리스를 확인한다. `search.maven.org`의 검색 API는 인덱스가 오래돼 신뢰할 수 없다(Spring Boot 최신을 3.5.3으로 응답함). 대신:

```bash
curl -s "https://repo1.maven.org/maven2/<group/path>/<artifact>/maven-metadata.xml"   # 마일스톤/RC 제외 후 사용
curl -s "https://api.adoptium.net/v3/info/available_releases"                          # Java LTS
```

## 아키텍처 규칙

`global/`(공통 인프라)과 `domain/<도메인>/`(도메인별 레이어 묶음)으로 나뉜다. 새 도메인은 `domain/user`를 복사해 이름만 바꾸는 것을 전제로 설계됐다.

**트랜잭션 경계는 Service에만 둔다.** 클래스에 `@Transactional(readOnly = true)`를 걸고 쓰기 메서드에만 `@Transactional`을 덮어쓴다. Controller나 Repository에는 절대 두지 않는다.

**모든 API 응답은 `ApiResponse<T>`로 감싼다.** 성공/실패 모두 `{ success, data, error }` 형태를 유지한다. 예외 응답은 `GlobalExceptionHandler`가 자동으로 이 포맷으로 변환하므로 Controller에서 try/catch 하지 않는다.

**새 오류는 `ErrorCode` enum에 추가한다.** HTTP 상태와 메시지를 enum에 같이 정의하므로, 새 예외 핸들러를 만들 필요 없이 `throw new BusinessException(ErrorCode.XXX)`로 끝난다.

**엔티티에 setter를 만들지 않는다.** 생성은 정적 팩토리(`User.create()`), 변경은 의도가 드러나는 도메인 메서드(`user.changeName()`, `user.deactivate()`)로만 한다. Service에서 엔티티를 수정할 때는 변경 감지에 맡기고 `save()`를 호출하지 않는다.

**스키마 변경은 Flyway 전용이다.** `ddl-auto: validate`라서 엔티티와 스키마가 어긋나면 기동 시점에 실패한다. 엔티티 필드를 추가/변경하면 반드시 `src/main/resources/db/migration/V{n}__{설명}.sql`도 같이 만든다.

**OSIV가 꺼져 있다.** 지연 로딩은 Service 트랜잭션 안에서 끝내고 Controller로는 DTO만 넘긴다.

**DTO는 record로 쓴다.** Lombok은 엔티티의 `@Getter`/`@NoArgsConstructor`와 `@Slf4j`로만 제한한다.

**MapStruct는 엔티티 → 응답 DTO 단방향만 담당한다.** 역방향(요청 DTO → 엔티티)은 도메인 팩토리를 쓴다. `unmappedTargetPolicy=ERROR`가 켜져 있어 매핑 누락은 컴파일 에러가 된다.

## 이 스택 특유의 함정

버전이 최신이라 기존 자료/기억과 어긋나는 지점이 있다. 아래는 모두 실제로 확인한 것들이다.

**Spring Boot 4**
- `spring-boot-starter-web`은 deprecated → `spring-boot-starter-webmvc`
- Flyway가 전용 스타터로 분리 → `spring-boot-starter-flyway`
- `@AutoConfigureMockMvc` 패키지 이동 → `org.springframework.boot.webmvc.test.autoconfigure`
- MVC 테스트 스타터 신설 → `spring-boot-starter-webmvc-test`

**Testcontainers 2.x**
- 모듈 아티팩트에 접두사 추가 → `org.testcontainers:testcontainers-postgresql` (접두사 없는 `org.testcontainers:postgresql`은 1.21.4에서 멈춰 있어 2.x를 받지 못한다)
- 컨테이너 클래스 패키지 이동 → `org.testcontainers.postgresql.PostgreSQLContainer`
- 제네릭이 없어졌다 → `new PostgreSQLContainer("postgres:18-alpine")`

**Lombok + MapStruct**
- `org.projectlombok:lombok-mapstruct-binding`을 `annotationProcessor`로 같이 걸어야 한다. 빠지면 MapStruct가 Lombok 생성 getter를 보지 못해 `Unmapped target properties` 컴파일 에러가 난다. 애노테이션 프로세서 실행 순서 문제라 소스만 봐서는 원인이 드러나지 않는다.

## 의도적으로 빠져 있는 것

없어서 빠진 게 아니라 판단이 필요해서 유보한 것들이다. 임의로 추가하지 말고 먼저 확인한다.

- **Spring Security** — 추가하는 순간 모든 엔드포인트가 잠기므로 인증 정책을 정한 뒤 붙인다
- **QueryDSL** — openfeign 포크 7.6이 최신이나 Hibernate 7.4 조합 검증이 안 됐다. 동적 쿼리는 Spring Data `Specification`으로 시작한다
- **Spotless / Checkstyle** — 팀 컨벤션 확정 후
