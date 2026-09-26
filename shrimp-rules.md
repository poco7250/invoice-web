# Development Guidelines

> AI Agent 전용 규칙 문서. 일반 개발 지식은 적지 않는다. 이 저장소에서만 통하는 규칙만 적는다.
> 규칙이 `CLAUDE.md`와 충돌하면 `CLAUDE.md`가 우선한다. 충돌을 발견하면 이 파일을 고친다.

## 1. 프로젝트 개요

- 목표 제품: **Notion Folio**. Notion을 CMS로 쓰고, Spring Boot가 콘텐츠를 PostgreSQL로 동기화해서 REST API로 제공하는 포트폴리오 백엔드
- 현재 상태: Task 001 진행 중. 패키지는 `com.poco7250.notionfolio`, 샘플 `domain/user`는 제거됐고 도메인 코드는 아직 없다
- 스택: Java 25, Spring Boot 4.1.1, Hibernate 7, Flyway, PostgreSQL 18, MapStruct, springdoc, Testcontainers 2.x
- 기준 문서 (작업 전에 반드시 읽는다)
  - `docs/PRD.md` — 요구사항, API 명세, 스키마, ErrorCode 목록
  - `docs/ROADMAP.md` — Task 순서, 결정 필요 항목(D-1~D-8), 완료 기준
  - `CLAUDE.md` — 아키텍처 규칙, 스택 함정
- ⚠️ `README.md`와 `docs/PRD_PROMPT.md`는 이전 기획(Invoice Web 견적서)을 설명한다. **요구사항 근거로 쓰지 않는다**

## 2. 디렉토리 구조와 배치 규칙

```
src/main/java/com/poco7250/notionfolio/
├─ global/                  공통 인프라 (도메인 무관)
│  ├─ config/               @Configuration (JpaConfig, OpenApiConfig, WebConfig ...)
│  ├─ entity/               BaseTimeEntity
│  ├─ exception/            ErrorCode, BusinessException, GlobalExceptionHandler
│  ├─ response/             ApiResponse, ErrorDetail, (추가 예정) PageResponse
│  ├─ notion/               (추가 예정) NotionClient, dto/, 재시도/호출 제한, NotionProperties
│  ├─ markdown/             (추가 예정) BlockToMarkdownConverter
│  └─ admin/                (추가 예정) AdminProperties, AdminKeyInterceptor
└─ domain/<도메인>/
   ├─ controller/  service/  repository/  entity/  dto/  mapper/
   └─ (sync 전용) scheduler/  listener/
src/main/resources/db/migration/   V{n}__{설명}.sql
src/test/java/.../support/         AbstractIntegrationTest, (추가 예정) FakeNotionClient, NotionFixtures
```

- 새 도메인은 `controller/ service/ repository/ entity/ dto/ mapper/` 하위 패키지 구성을 따른다
- 도메인 목록은 PRD 기준 `project`, `profile`, `image`, `sync`로 고정한다. 새 도메인을 추가하려면 ROADMAP에 먼저 반영한다
- 둘 이상 도메인이 쓰는 코드만 `global/`에 둔다. 한 도메인만 쓰면 그 도메인 안에 둔다
- 예: `SyncProperties` → `domain/sync/`, `NotionProperties` → `global/notion/`

## 3. 레이어 구현 규칙

### Controller

- `ApiResponse<T>`로 감싸 반환한다. `ApiResponse.ok(data)` / `ApiResponse.ok()`만 쓴다
- try/catch를 쓰지 않는다. 예외는 `GlobalExceptionHandler`가 변환한다
- `@Tag`, `@Operation`을 붙인다 (`UserController` 형식)
- 예외: 이미지 프록시 성공 응답은 302 리다이렉트라 `ApiResponse` 본문이 없다. 에러 응답은 `ApiResponse` 형식을 유지한다
- 목록 응답은 Spring Data `Page`를 직접 반환하지 않고 `PageResponse<T>`로 변환한다 (`UserController`의 `Page` 반환은 샘플 코드라 따르지 않는다)

### Service

- 클래스에 `@Transactional(readOnly = true)`, 쓰기 메서드에만 `@Transactional`
- 엔티티 수정 후 `save()`를 부르지 않는다. 신규 생성할 때만 `save()`
- 조회 실패는 `private X getXOrThrow(...)` 헬퍼로 `BusinessException(ErrorCode.XXX)`를 던진다
- **Notion HTTP 호출은 트랜잭션 밖에서 한다.** 페이지 1건의 DB 반영만 별도 빈(`ProjectSyncWriter` 등)의 `@Transactional` 메서드로 분리한다. 같은 클래스 내부 호출로 트랜잭션을 기대하지 않는다
- 지연 로딩은 Service 안에서 끝내고 DTO만 반환한다 (OSIV off)

### Repository

- `JpaRepository` 상속. 필터 조회는 `JpaSpecificationExecutor` + `Specification`
- `@Transactional`을 두지 않는다
- QueryDSL을 쓰지 않는다

### Entity

- `@Getter`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)`만 Lombok으로 쓴다
- 생성은 정적 팩토리(`Project.create(...)`), 변경은 의도가 드러나는 메서드(`hide()`, `refreshUrl(...)`)
- setter, `@Setter`, `@Data`, `@Builder` 금지
- 생성/수정 시각이 필요하면 `BaseTimeEntity`를 상속한다
- 테이블명과 SQL 예약어가 겹치면 컬럼명을 바꾼다 (예: `trigger` → `trigger_type`)

### DTO / Mapper

- DTO는 `record`. 요청 DTO에 Bean Validation 어노테이션과 한국어 `message`를 붙인다
- MapStruct는 엔티티 → 응답 DTO 단방향만. 요청 DTO → 엔티티는 도메인 팩토리를 쓴다
- `@Mapper`에 `componentModel`을 적지 않는다 (컴파일 옵션으로 지정됨)
- 매핑 누락은 컴파일 에러(`unmappedTargetPolicy=ERROR`)다. `@Mapping(target = ..., ignore = true)`로 덮지 말고 필드를 실제로 매핑한다

### 예외

- 새 오류는 `ErrorCode` enum에 `이름(HttpStatus, "한국어 메시지")`로 추가하고 도메인별 주석 구역에 둔다
- 새 예외 클래스, 새 `@ExceptionHandler`를 도메인용으로 만들지 않는다
- `GlobalExceptionHandler`에 프레임워크 예외 핸들러를 추가할 때는 `Exception` 핸들러보다 위에 둔다
- 로그 수준: 비즈니스 예외 WARN, 처리되지 않은 예외 ERROR + 스택트레이스

## 4. 코드 스타일

- 주석, Javadoc, 로그 메시지, 예외 메시지, 문서: **한국어**
- public 클래스와 public 메서드에 한 줄 이상 Javadoc (`/** ... */`)
- 변수·메서드 camelCase. 로깅은 `@Slf4j`의 `log`만. `System.out` 금지
- 메서드는 30줄 이하. 넘으면 private 메서드나 별도 빈으로 분리한다
- 로그는 `log.info("동기화 완료: id={}", id)`처럼 `키=값` 플레이스홀더 형식

## 5. 의존성 규칙

| 대상 | 방법 |
|---|---|
| Boot BOM 관리 대상 (Hibernate, Flyway, Testcontainers, PostgreSQL, JUnit, AssertJ, Lombok, Logback, Micrometer, restclient 스타터) | `build.gradle.kts`에 **버전 없이** 추가 |
| BOM 비관리 대상 (springdoc, MapStruct 등) | `gradle/libs.versions.toml`에 버전 선언 → `libs.xxx`로 참조 |

- 버전은 `https://repo1.maven.org/maven2/<group>/<artifact>/maven-metadata.xml`로 확인한다. `search.maven.org` 검색 API를 쓰지 않는다. 마일스톤/RC는 쓰지 않는다
- Notion 연동은 Spring `RestClient`로 직접 구현한다. 서드파티 Notion SDK, 재시도 라이브러리(Resilience4j, Spring Retry)를 추가하지 않는다
- 추가 금지(확인 전): Spring Security, QueryDSL, Spotless, Checkstyle

### Boot 4 / Testcontainers 2 아티팩트 표기

| 쓰기 | 쓰지 않기 |
|---|---|
| `spring-boot-starter-webmvc` | `spring-boot-starter-web` |
| `spring-boot-starter-flyway` | `flyway-core` 단독 |
| `spring-boot-starter-webmvc-test` | `spring-boot-starter-test`만으로 MVC 테스트 |
| `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc` | `org.springframework.boot.test.autoconfigure.web.servlet.*` |
| `org.testcontainers:testcontainers-postgresql` | `org.testcontainers:postgresql` |
| `new PostgreSQLContainer("postgres:18-alpine")` (`org.testcontainers.postgresql`) | `new PostgreSQLContainer<>(...)` |

- `lombok-mapstruct-binding` annotationProcessor를 제거하지 않는다

## 6. 동시 수정 파일 규칙 (Key File Interaction)

| 이 파일을 바꾸면 | 반드시 같이 바꾼다 |
|---|---|
| `domain/*/entity/*.java` 필드/테이블 | `db/migration/V{다음번호}__{설명}.sql` 신규 작성 (기존 마이그레이션 수정 금지, 단 D-5의 V1 교체는 예외) |
| `ErrorCode` 추가/변경 | 해당 에러를 검증하는 통합 테스트, PRD ErrorCode 표와 불일치하면 PRD 기준으로 맞춘다 |
| `@ConfigurationProperties` 추가 | `src/main/resources/application.yml`(환경변수 플레이스홀더) + `src/test/resources/application.yml`(더미값) |
| 새 환경변수 | PRD `### 환경변수` 블록, ROADMAP Task 014 환경변수 목록 |
| 새 의존성 (BOM 비관리) | `gradle/libs.versions.toml` + `build.gradle.kts` |
| 패키지명 변경 (D-6) | `src/main`, `src/test` 전체 `package`/`import`, `build.gradle.kts`의 `group`, `application.yml`의 `logging.level.com.example.starterkit` |
| 앱 이름 변경 | `settings.gradle.kts` `rootProject.name`, `application.yml` `spring.application.name`, `OpenApiConfig` 제목/설명, `.claude/settings.json` 알림 제목 |
| 새 엔드포인트 | `@Tag`/`@Operation`, 통합/계약 테스트, PRD API 명세 표와 경로·응답 타입 일치 확인 |
| `/tasks/XXX-*.md` 완료 | `docs/ROADMAP.md`에 ✅ + `See: /tasks/XXX-xxx.md` (자동 테스트 PASSED·SKIPPED 0, E2E 기록이 있을 때만) |
| 스택 함정 새로 발견 | `CLAUDE.md` "이 스택 특유의 함정" 섹션 |
| `domain/user` 삭제 (D-5) | `UserTest`, `UserApiIntegrationTest`, `V1__create_users.sql`, `ErrorCode`의 USER_NOT_FOUND/DUPLICATE_EMAIL 같이 삭제 |

## 7. 테스트 규칙

- 통합 테스트는 `support/AbstractIntegrationTest`를 상속한다. H2를 쓰지 않는다
- 단위 테스트(도메인, 변환기, 매퍼)는 스프링 컨텍스트 없이 작성한다 (`NotionPropertiesTest` 형식)
- `@DisplayName`은 한국어 문장으로 기대 동작을 적는다
- Notion HTTP 수준(재시도, 429, 5xx, 타임아웃)은 `MockRestServiceServer`로 테스트한다
- 동기화/조회 통합 테스트는 `NotionClient` 인터페이스의 Fake 구현(`support/FakeNotionClient`)을 쓴다. 실제 Notion을 호출하지 않는다
- 재시도 대기는 `Sleeper`/`Clock`을 주입해서 테스트에서 실제로 기다리지 않는다
- 테스트 `application.yml`에서 스케줄러는 꺼둔다 (`scheduler.enabled=false`)
- ⚠️ **SKIPPED는 통과가 아니다.** `./gradlew build` 후 출력/리포트에서 SKIPPED 0건을 확인한다. SKIPPED가 있으면 Docker를 띄우고 다시 돌린다. Docker를 띄울 수 없으면 "미검증"으로 보고한다

## 8. 작업 워크플로우

1. `docs/ROADMAP.md`에서 다음 Task와 관련 결정 항목(D-n)을 확인한다
2. `/tasks/XXX-description.md`를 만든다. 없으면 `/tasks/000-sample.md` 템플릿도 같이 만든다
   - 필수 섹션: 명세, 관련 파일, 수락 기준, 구현 단계, `## 테스트 체크리스트`(범주 8개 검토), `## E2E 검증 (Playwright MCP)`
3. 구현 → `./gradlew build` → SKIPPED 0 확인 → `./gradlew bootRun` → Playwright MCP로 Swagger UI 정상/에러 케이스 확인
4. 결과를 작업 파일에 기록하고 ROADMAP을 갱신한다
5. **한 Task(단계)가 끝나면 멈추고 다음 지시를 기다린다.** 다음 Task를 임의로 시작하지 않는다
- 브랜치: `feature/기능명`. 커밋 메시지: 한국어. 커밋은 사용자가 요청할 때만 한다

## 9. AI 판단 기준

### 결정이 안 된 항목을 만났을 때

```
ROADMAP "결정 필요 항목"에 있는가?
├─ 예 → 사용자 결정이 기록돼 있는가?
│       ├─ 예 → 그대로 따른다
│       └─ 아니오 → "기본안" 열대로 진행하고, 기본안을 썼다고 보고한다
│                  단, D-1(배포 대상)·D-2(Security)는 기본안 외 작업을 하지 않는다
└─ 아니오 → PRD에 근거가 있는가?
        ├─ 예 → PRD를 따른다 (ROADMAP의 "PRD 대비 조정 사항"이 있으면 그것이 우선)
        └─ 아니오 → 되돌리기 어려운 결정(스키마, 공개 API 계약, 의존성)이면 사용자에게 묻는다
                   되돌리기 쉬운 결정(내부 private 구조)이면 기존 코드 관례를 따르고 보고한다
```

### 우선순위

1. `CLAUDE.md` 아키텍처 규칙
2. `docs/ROADMAP.md` (PRD 대비 조정 사항 포함)
3. `docs/PRD.md`
4. 기존 코드 관례 (`global/`)
5. 이 문서의 나머지 규칙

### 라이브러리 API가 불확실할 때

- Spring Boot 4, Hibernate 7, Testcontainers 2, Jackson 3, Notion API는 기억에 의존하지 않는다. Context7 MCP 또는 공식 문서로 확인한다
- Notion API 버전(`Notion-Version`)과 data source 엔드포인트는 Task 006 착수 시 최신 문서로 재확인한다 (D-4)

## 10. 금지 사항

- ❌ Controller/Repository에 `@Transactional`
- ❌ Controller에서 try/catch, 엔티티 직접 반환, `Page` 직접 반환
- ❌ 엔티티 setter, `@Setter`, `@Data`, `@Builder`, DTO에 Lombok
- ❌ 수정한 엔티티에 `save()` 재호출
- ❌ 엔티티만 바꾸고 Flyway 마이그레이션 누락, `ddl-auto`를 `validate` 외 값으로 변경
- ❌ 이미 적용된 마이그레이션 파일 수정 (D-5 V1 교체만 예외)
- ❌ BOM 관리 의존성에 버전 명시
- ❌ Spring Security, QueryDSL, Spotless, Checkstyle 임의 추가
- ❌ 트랜잭션 안에서 Notion HTTP 호출
- ❌ 방문자 조회 API에서 Notion 직접 호출 (조회는 DB에서만)
- ❌ 비밀값(`NOTION_TOKEN`, `ADMIN_API_KEY` 등)을 `application.yml`·코드·테스트에 실값으로 기록
- ❌ 관리자 키를 `equals`로 비교 (`MessageDigest.isEqual` 사용)
- ❌ Webhook 서명을 파싱 후 재직렬화한 JSON으로 검증 (raw body 문자열로 검증)
- ❌ SKIPPED 테스트를 PASSED로 보고, E2E 미수행을 수행으로 보고
- ❌ `README.md`/`docs/PRD_PROMPT.md`의 견적서(Invoice) 요구사항 구현
- ❌ 사용자 요청 없이 커밋, 다음 Task 자동 착수
