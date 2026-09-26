# Notion Folio 개발 로드맵

Notion을 CMS로 쓰고, Spring Boot가 콘텐츠를 PostgreSQL로 동기화해서 정제된 REST API로 제공하는 백엔드 개발자 포트폴리오.

## 개요

Notion Folio는 채용 담당자와 기술 면접관(방문자), 그리고 Notion에서만 콘텐츠를 관리하고 싶은 운영자(본인)를 위한 포트폴리오 서비스로, 다음 기능을 제공한다.

- **Notion 동기화 엔진**: 30분 주기 전체 동기화 + 수동 동기화 API. `notion_page_id` 기준 멱등 upsert, 비공개/삭제 페이지 숨김, 실행 이력 기록
- **포트폴리오 조회 API**: 프로젝트 목록(카테고리/기술스택 필터, 페이지네이션)·Featured·상세, 기술스택 필터 옵션, 프로필. 모든 응답은 `ApiResponse<T>`, Swagger 문서 제공
- **블록 → Markdown 변환기**: 동기화 시점에 Notion 본문 블록을 Markdown으로 변환해 DB에 저장
- **이미지 프록시**: `/api/images/{id}`로 제공하고, 만료된 Notion 파일 URL은 다시 받아서 302 리다이렉트
- **Next.js 프론트엔드**: 홈, 목록, 상세, About 4개 화면(ISR)
- **(2차) Webhook 증분 동기화**: 서명 검증, 이벤트 중복 제거, `@Async` 단건 동기화

참조 문서: `docs/PRD.md`, 루트 `CLAUDE.md`

## 현재 코드베이스 상태 (2026-09-24 기준)

스타터 킷을 복제한 직후 상태다. 도메인 코드는 샘플인 `domain/user`뿐이다. (Task 001 진행으로 패키지는 `com.poco7250.notionfolio`, 앱 이름은 `notion-folio`로 바뀌었고 샘플 `domain/user`는 제거됐다. 진행 상황은 `/tasks/001-setup.md` 참고)

| 구분 | 이미 있는 것 | 새로 만들어야 하는 것 |
|---|---|---|
| 공통 응답 | `global/response/ApiResponse`, `ErrorDetail` | 페이지네이션 응답 `PageResponse<T>` |
| 예외 | `global/exception/ErrorCode`(공통 3개 + user 2개), `BusinessException`, `GlobalExceptionHandler`(BusinessException, Bean Validation, Exception만 처리) | PRD ErrorCode 8개, 누락된 핸들러(404 경로, 헤더 누락, 타입 불일치, 메서드 불일치) |
| 엔티티 | `global/entity/BaseTimeEntity`(created_at/updated_at), `global/config/JpaConfig`(Auditing) | Project, ProjectTech, Profile, ImageAsset, SyncHistory, WebhookEvent |
| 설정 | `application.yml`(Virtual Thread on, OSIV off, `ddl-auto: validate`, Actuator 노출), `OpenApiConfig`(제목이 "Spring Starter Kit API") | `NotionProperties`, `SyncProperties`, `AdminProperties`(`@ConfigurationProperties`), `@EnableScheduling`/`@EnableAsync` 설정, 관리자 키 인터셉터 |
| 외부 연동 | 없음 (`RestClient` 스타터도 아직 의존성에 없음) | `global/notion/`(NotionClient, 응답 DTO, 재시도/스로틀), `global/markdown/` |
| 마이그레이션 | `V1__create_users.sql`(샘플) | PRD의 V1~V6 (아래 "마이그레이션 번호" 참고) |
| 테스트 | `support/AbstractIntegrationTest`(Testcontainers PostgreSQL 18 + MockMvc), `UserTest`, `UserApiIntegrationTest` | 도메인/변환기/매퍼 단위 테스트, `MockRestServiceServer` 클라이언트 테스트, Fake NotionClient 기반 동기화 통합 테스트 |
| 기타 | `compose.yaml`(로컬 PostgreSQL), 패키지 `com.poco7250.notionfolio`, 앱 이름 `notion-folio` (Task 001에서 변경) | Dockerfile, CI, 프론트엔드 |

작업 파일은 `/tasks/`에 있다. 새 작업 파일의 초기 형식은 `/tasks/000-sample.md`를 따른다.

## 개발 워크플로우

1. **작업 계획**
   - 기존 코드베이스와 `CLAUDE.md`를 학습하고 현재 상태를 파악한다
   - 새로운 작업을 포함하도록 이 `docs/ROADMAP.md`를 업데이트한다
   - 우선순위 작업은 마지막 완료된 작업 다음에 삽입한다

2. **작업 생성**
   - `/tasks` 디렉토리에 작업 파일을 만든다. 명명 형식은 `XXX-description.md` (예: `001-setup.md`)
   - 고수준 명세서, 관련 파일, 수락 기준, 구현 단계를 포함한다
   - API/비즈니스 로직 작업은 `## 테스트 체크리스트` 섹션(단위/통합/외부 연동 테스트 시나리오, 실행 명령)을 반드시 포함한다. 시나리오는 아래 "테스트 시나리오 범주" 8개를 모두 검토한다
   - 모든 구현 작업은 `## E2E 검증 (Playwright MCP)` 섹션(호출할 URL, 기대 응답, 에러 케이스, 결과 기록란)을 반드시 포함한다. E2E 대상이 아니면 사유를 적는다
   - 직전 완료 작업 두 개를 형식 예시로 참조한다. 완료된 작업 파일은 체크된 박스와 변경 사항 요약이 있지만, 새 작업 파일은 빈 박스로 시작하고 변경 사항 요약이 없다. 초기 상태 샘플은 `/tasks/000-sample.md`

3. **작업 구현**
   - 작업 파일의 명세서를 따른다
   - `CLAUDE.md` 아키텍처 규칙을 지킨다: 트랜잭션은 Service에만, 엔티티 setter 금지(정적 팩토리 + 도메인 메서드), 변경 감지 사용(`save()` 재호출 금지), DTO는 record, MapStruct는 엔티티 → DTO 단방향, 새 오류는 `ErrorCode` enum에 추가
   - 엔티티를 추가/변경하면 같은 Task에서 `V{n}__{설명}.sql`을 작성한다
   - 새 의존성은 먼저 Spring Boot BOM 관리 대상인지 확인한다. BOM 관리 대상이면 버전 없이 추가하고, 아니면 `gradle/libs.versions.toml`에 실제 릴리스를 확인한 버전으로 선언한다
   - 구현 후 반드시 테스트를 수행한다
     1. `./gradlew build` 통과, 테스트 리포트에서 SKIPPED 0건 확인
     2. `./gradlew bootRun` 후 Playwright MCP로 Swagger UI/화면에서 정상·에러 케이스 E2E 검증
     3. 검증 결과(URL, 기대값/실제값, 스크린샷)를 작업 파일에 기록
   - 자동 테스트와 E2E 검증이 모두 통과한 뒤에만 다음 단계로 진행한다
   - 각 단계 후 작업 파일의 진행 상황을 업데이트하고, 단계가 끝나면 멈추고 다음 지시를 기다린다

4. **로드맵 업데이트**
   - 자동 테스트 PASSED(SKIPPED 0)와 E2E 검증 기록이 모두 확인된 작업만 ✅로 표시하고 `See: /tasks/XXX-xxx.md`를 추가한다

### 공통 완료 기준 (모든 Task에 적용)

모든 Task는 아래를 모두 만족해야 완료로 본다.

1. `./gradlew build`가 통과한다
2. 테스트 리포트(`build/reports/tests/test/index.html` 또는 콘솔의 `testLogging` 출력)에서 **Testcontainers 통합 테스트가 SKIPPED가 아니라 PASSED인지 확인한다.** `@Testcontainers(disabledWithoutDocker = true)` 때문에 Docker가 없으면 통합 테스트가 조용히 빠지고 빌드는 성공한다. SKIPPED가 하나라도 있으면 Docker를 띄우고 다시 실행한다
3. 엔티티를 건드린 Task는 Testcontainers 기반 테스트로 `ddl-auto: validate`가 실제로 통과(컨텍스트 기동 성공)했는지 확인한다
4. 새 엔드포인트는 Swagger UI(`/swagger-ui.html`)에 노출되고 응답이 `{ success, data, error }` 형태인지 확인한다 (이미지 프록시 302 응답은 예외, 아래 해석 참고)
5. **Playwright MCP E2E 검증**을 수행하고 결과를 작업 파일에 기록한다
   - `./gradlew bootRun` 후 `/actuator/health`가 `UP`인지 확인한다
   - Swagger UI에서 Task가 추가·변경한 엔드포인트를 Try it out으로 호출하고, `browser_snapshot`/`browser_network_requests`로 상태 코드와 응답 구조를 확인한다
   - 엔드포인트마다 에러 케이스를 최소 1개 호출한다 (없는 리소스, 잘못된 파라미터, 관리자 키 누락 등)
   - 프론트 Task는 화면 렌더링, 필터·페이지 이동, 404 화면, 콘솔 에러 0건, 모바일 폭(375px)을 확인한다
   - Notion 호출, 재시도, 스케줄러처럼 브라우저로 재현할 수 없는 동작은 자동 테스트가 책임진다. 이런 Task는 "E2E 대상 아님 + 사유"를 적는다
   - 실제 Notion 키가 필요한 검증은 환경변수가 있을 때만 수행한다. 없으면 "미수행 + 사유"를 기록하고 넘어가지 않는다

### 테스트 시나리오 범주 (API 연동·비즈니스 로직 Task)

아래 범주를 모두 검토한다. 해당이 없는 범주는 지우지 말고 "해당 없음 + 사유"로 남긴다.

| 범주 | 예시 |
|---|---|
| 정상 흐름 | 기대 입력에 대한 응답 필드와 값 |
| 입력 검증 실패 | 잘못된 category, size 초과, 타입 불일치, 헤더 누락 → 400 |
| 경계값 | 빈 결과, 마지막 페이지, null 필드(링크, metrics, period_end) |
| 외부 장애 | Notion 429/5xx, 타임아웃, 재시도 소진 → `NOTION_RATE_LIMITED`/`NOTION_UNAVAILABLE` |
| 멱등성·중복 | 동기화 2회 결과 동일, 중복 Webhook 이벤트 무시 |
| 동시성 | 동기화 동시 실행 → 409, 이미지 URL 동시 갱신 |
| 트랜잭션 | 한 페이지 실패 시 나머지 반영, 롤백 후 데이터 상태 |
| 에러 응답 포맷 | `{ success: false, data: null, error }`, `ErrorCode`, HTTP 상태 일치 |

## 결정 필요 항목

PRD의 오픈 이슈와 로드맵 작성 중 드러난 판단 사항이다. Task로 만들지 않고, 괄호 안의 Task를 시작하기 전에 결정한다.

| ID | 항목 | 선택지 | 기본안(결정 전 임시 방침) | 결정 시점 |
|---|---|---|---|---|
| D-1 | **배포 대상** (PRD 오픈 이슈) | Railway / Fly.io / AWS | 결정 전까지 Dockerfile과 환경변수 목록만 대상 중립적으로 준비한다 | Task 014 시작 전 |
| D-2 | **관리자 API 보호 방식 / Spring Security 도입 여부** (PRD 오픈 이슈) | (a) `X-Admin-Key` 인터셉터 유지 (b) Spring Security 도입 | MVP는 (a). `CLAUDE.md`가 Security를 의도적으로 유보했으므로 로드맵에 Security Task를 두지 않는다. 도입이 결정되면 별도 Task로 추가한다 | Task 014(운영 배포) 전 재검토 |
| D-3 | **Webhook 구독 이벤트 목록** (PRD `[확인 필요]`) | `page.content_updated`, `page.properties_updated`, `page.deleted`, `page.undeleted`, `page.created`, `data_source.*` 등 | 최소 `page.content_updated`, `page.properties_updated`, `page.deleted`로 시작한다. 구현 시점 Notion 문서로 확정한다 | Task 015 시작 전 |
| D-4 | **`NOTION_VERSION` 재확인** (PRD 리스크) | PRD 가정값 `2026-03-11` | Task 006에서 최신 Notion API 문서를 확인하고 설정값과 응답 DTO를 맞춘다. data source 모델(`2025-09-03`~) 전제가 유지되는지도 확인한다 | Task 006 시작 시 |
| D-5 | 스타터 킷 샘플 `domain/user`와 `V1__create_users.sql` 처리 | (a) 삭제하고 PRD 번호(V1~V6) 그대로 사용 (b) 유지하고 V2부터 시작 | (a). 아직 어디에도 배포된 DB가 없으므로 V1을 교체해도 안전하다. 로컬 DB는 `docker compose down -v`로 초기화한다. 공개 포트폴리오에 샘플 사용자 CRUD API가 노출되면 안 된다 | Task 001 |
| D-6 | 패키지명/앱 이름 변경 | `com.example.starterkit` 유지 / `com.<본인>.notionfolio` 등으로 변경 | 도메인 코드가 생기기 전인 Task 001에서 바꾸는 것을 권장한다(나중일수록 비용 증가) | Task 001 |
| D-7 | 토글 블록의 Markdown 표현 | (a) `<details><summary>` HTML (b) 굵은 제목 + 들여쓴 본문 | (a)는 프론트에 `rehype-raw` + sanitize가 필요하다. 프론트 담당과 합의 후 확정 | Task 007 |
| D-8 | 프론트엔드 코드 위치 / Next.js 버전 | 같은 저장소 `frontend/` / 별도 저장소. PRD는 Next.js 15 명시 | 구현 시점 최신 안정 버전과 PRD 명시 버전을 비교해 확정 | Task F01 시작 전 |

## 개발 단계

PRD 구현 단계와 Phase의 대응 관계:

| PRD 구현 단계 | 로드맵 Phase / Task | 범위 |
|---|---|---|
| 1. 프로젝트 세팅 | Phase 1 / Task 001 | MVP |
| 3. 스키마와 도메인 | Phase 1 / Task 002~003 | MVP |
| (계약 우선 추가) API 계약 확정 | Phase 2 / Task 004~005 | MVP |
| 2. Notion 클라이언트 | Phase 3 / Task 006 | MVP |
| 4. 동기화 엔진 | Phase 3 / Task 007~009 | MVP |
| 5. 조회 API | Phase 3 / Task 010~012 | MVP |
| 6. 프론트엔드 | Phase F / Task F01~F03 | MVP |
| 7. 배포 | Phase 4 / Task 013~014 | MVP |
| 8. (2차) Webhook | Phase 5 / Task 015~016 | 2차 |

---

## MVP 범위

### Phase 1: 기반 및 도메인 골격 구축

- **Task 001: 스타터 킷을 Notion Folio 프로젝트로 정리하고 공통 인프라 세팅** - 우선순위
  - 관련: `build.gradle.kts`, `src/main/resources/application.yml`, `src/test/resources/application.yml`, `global/config/`, `global/exception/`, `domain/user/`(삭제 대상)
  - D-5, D-6 결정 반영: `domain/user`, `UserTest`, `UserApiIntegrationTest`, `V1__create_users.sql` 제거, `spring.application.name`과 `OpenApiConfig` 제목/설명을 Notion Folio로 변경
  - Notion 준비(코드 외 작업): Integration 생성, Projects 데이터베이스(PRD 속성 19개)와 About 페이지 생성, Integration 연결, 샘플 프로젝트 3개 입력(필수 속성 누락 페이지 1개를 따로 만들어 skip 시나리오 확인용으로 둔다). data source ID 확보
  - 설정 프로퍼티 record 정의: `global/notion/NotionProperties`(token, version, projectsDataSourceId, aboutPageId, baseUrl, connectTimeout, readTimeout, 최대 재시도 6, 최대 백오프 30s, 초당 요청 한도 3), `domain/sync/SyncProperties`(스케줄 cron/fixedDelay 30분, 스케줄러 enabled), `global/admin/AdminProperties`(apiKey). 비밀값은 환경변수(`NOTION_TOKEN` 등)로만 주입하고, 테스트 `application.yml`에는 더미값과 `scheduler.enabled=false`를 둔다
  - `ErrorCode`에 PRD 8개 추가: `PROJECT_NOT_FOUND(404)`, `PROFILE_NOT_FOUND(404)`, `IMAGE_NOT_FOUND(404)`, `NOTION_UNAVAILABLE(502)`, `NOTION_RATE_LIMITED(503)`, `INVALID_WEBHOOK_SIGNATURE(401)`, `INVALID_ADMIN_KEY(401)`, `SYNC_ALREADY_RUNNING(409)`, 공통 `RESOURCE_NOT_FOUND(404)` 추가
  - `GlobalExceptionHandler` 보강: `NoResourceFoundException`(현재 없는 경로가 500으로 떨어짐), `MissingRequestHeaderException`, `MethodArgumentTypeMismatchException`, `HttpRequestMethodNotSupportedException`(이미 있는 `METHOD_NOT_ALLOWED` 사용)
  - 의존성: `spring-boot-starter-restclient`, `spring-boot-starter-restclient-test`(테스트) 추가. 둘 다 Boot BOM 관리 대상(4.1.1 아티팩트 확인됨)이므로 버전을 쓰지 않는다
  - 수락 기준: `./gradlew build` 통과, 컨텍스트 기동 스모크 테스트(`AbstractIntegrationTest` 상속)가 PASSED, 없는 경로 요청 시 404 + `ApiResponse` 실패 포맷 반환 테스트 통과
  - E2E: `/actuator/health` UP, Swagger UI 제목이 Notion Folio로 표시, `/api/unknown` 접속 시 404 JSON 포맷, `/api/users`가 더 이상 노출되지 않음

- **Task 002: 프로젝트 스키마와 엔티티 설계 (Flyway V1~V2)**
  - 관련: `db/migration/V1__create_project.sql`, `V2__create_project_tech.sql`, `domain/project/entity/`, `domain/project/repository/`
  - `V1__create_project.sql`: PRD 컬럼 + `github_url`, `api_docs_url`, `architecture_image_id`(nullable, FK는 V4에서 추가), `created_at`/`updated_at`(BaseTimeEntity). `notion_page_id`·`slug` UNIQUE, 목록 조회용 인덱스 `(visible, sort_order, period_start)`, `(visible, featured)`, `(visible, category)`
  - `V2__create_project_tech.sql`: `project_id`(FK, ON DELETE CASCADE), `type`, `name`, PK `(project_id, type, name)`, 인덱스 `(type, name)`
  - 엔티티: `Project extends BaseTimeEntity`(`Project.create(...)`, `applyNotionSnapshot(...)`로 속성 일괄 반영, `hide()`, `show()`, `isChangedSince(Instant lastEditedAt)`), `ProjectTech`(`@Embeddable`, `@ElementCollection`으로 매핑하고 `replaceTechs()`로 전체 교체), `ProjectCategory`(BACKEND/INFRA/DATA/SIDE, Notion 표기 변환 메서드), `TechType`(LANGUAGE/FRAMEWORK/DATABASE/INFRA)
  - `ProjectRepository extends JpaRepository, JpaSpecificationExecutor`: `findByNotionPageId`, `findBySlugAndVisibleTrue`, `findAllByVisibleTrueAndFeaturedTrue(Sort)`, `findAllByVisibleTrueAndNotionPageIdNotIn`(숨김 대상)
  - slug 형식 규칙(`^[a-z0-9]+(-[a-z0-9]+)*$`)은 엔티티 생성 시 검증
  - 수락 기준: 엔티티 단위 테스트, Repository 통합 테스트(UNIQUE 위반, element collection 교체) PASSED
  - E2E: 대상 아님(노출 API 없음). `bootRun` 기동 성공(`validate` 통과)과 `/actuator/health` UP만 확인

- **Task 003: 프로필·이미지·동기화 이력 스키마와 엔티티 설계 (Flyway V3~V5)**
  - 관련: `V3__create_profile.sql`, `V4__create_image_asset.sql`, `V5__create_sync_history.sql`, `domain/profile/`, `domain/image/`, `domain/sync/` 의 entity/repository
  - `profile`: PRD 컬럼 + `notion_last_edited_at`(변경 감지용), `notion_page_id` UNIQUE. `Profile.create()`, `updateContent()`
  - `image_asset`: `notion_source_id` UNIQUE(블록 ID 또는 `페이지ID:속성명`), `source_type`(BLOCK/PAGE_PROPERTY), `cached_url`, `expires_at`(외부 URL이면 null). `project.architecture_image_id` FK를 여기서 추가. `ImageAsset.register()`, `refreshUrl(url, expiresAt)`, `isExpired(Clock, 여유시간)`
  - `sync_history`: PRD 컬럼 + `skip_details`(JSONB, 건너뛴 페이지 ID와 사유 목록). `trigger`는 SQL 예약어라 `trigger_type`으로 명명. `SyncHistory.start(trigger)`, `succeed(counts)`, `fail(message)`. JSONB는 Hibernate 7 `@JdbcTypeCode(SqlTypes.JSON)`으로 매핑
  - 동시 실행 방지 보조 장치: `sync_history`에 `status = 'RUNNING'` 부분 유니크 인덱스
  - 수락 기준: 도메인 메서드 단위 테스트(만료 판정, 상태 전이), 전체 마이그레이션 적용 후 `validate` 기동 PASSED
  - E2E: 대상 아님(노출 API 없음). `bootRun` 기동 성공과 `/actuator/health` UP만 확인

### Phase 2: API 계약 확정 (스텁 응답)

이 Phase가 끝나면 Swagger 문서만 보고 프론트엔드(Phase F)가 병렬로 개발을 시작할 수 있다.

- **Task 004: 요청/응답 DTO와 MapStruct 매퍼 정의**
  - 관련: `domain/*/dto/`, `domain/*/mapper/`, `global/response/PageResponse`
  - record DTO: `ProjectSummaryResponse`(slug, title, summary, category, period, problem, metrics, techStack, featured), `ProjectDetailResponse`(PRD 응답 예시 필드 전체: period, techStack, links, architectureImageUrl, contentMarkdown, syncedAt), `PeriodResponse`, `TechStackGroupResponse`, `LinksResponse`, `TechStackResponse`(type, name, count), `ProfileResponse`, `SyncResultResponse`, `SyncHistoryResponse`
  - 목록 조회 요청 `ProjectSearchCondition`(category, tech, page, size) + Bean Validation(size 최대 50 등)
  - `PageResponse<T>`(content, page, size, totalElements, totalPages, hasNext): Spring Data `Page`를 그대로 직렬화하지 않고 안정적인 계약을 둔다
  - MapStruct 매퍼: `ProjectMapper`(tech 목록 → 타입별 그룹, `architectureImageId` → `/api/images/{id}`), `ProfileMapper`, `SyncHistoryMapper`. `unmappedTargetPolicy=ERROR`로 누락은 컴파일 에러
  - 수락 기준: 매퍼 단위 테스트(기술스택 그룹핑, null 링크, 이미지 URL 생성) 통과
  - E2E: 대상 아님(Controller는 Task 005). 매퍼 결과는 Task 005 E2E에서 응답 구조로 함께 확인

- **Task 005: 전체 엔드포인트 Controller·Swagger 문서와 관리자 키 인터셉터 구현**
  - 관련: `domain/project/controller/ProjectController`, `TechStackController`, `domain/profile/controller/`, `domain/image/controller/ImageController`, `domain/sync/controller/AdminSyncController`, `global/admin/AdminKeyInterceptor`, `global/config/WebConfig`
  - 엔드포인트(스텁 Service가 고정 데이터 반환): `GET /api/projects`, `GET /api/projects/featured`, `GET /api/projects/{slug}`, `GET /api/tech-stacks`, `GET /api/profile`, `GET /api/images/{id}`(302), `POST /api/admin/sync`, `GET /api/admin/sync/history`. `/api/projects/featured`가 `{slug}`에 먹히지 않는지 확인
  - springdoc 어노테이션: `@Tag`, `@Operation`, 에러 응답 스키마, 관리자 API에 `X-Admin-Key` 헤더 보안 스키마 등록
  - `AdminKeyInterceptor`: `/api/admin/**`에만 적용, `MessageDigest.isEqual`로 상수 시간 비교, 실패 시 `BusinessException(INVALID_ADMIN_KEY)` (인터셉터 예외도 `GlobalExceptionHandler`로 변환됨을 테스트로 확인). D-2 결정 전까지의 MVP 방식
  - 수락 기준: `@WebMvcTest`(Boot 4 패키지 `org.springframework.boot.webmvc.test.autoconfigure`) 계약 테스트로 전 엔드포인트의 상태 코드·JSON 구조 검증, Swagger UI에서 전 엔드포인트 호출 가능
  - E2E: Swagger UI에서 8개 엔드포인트 전부 Try it out 호출, 응답이 PRD 예시 구조와 일치하는지 확인. `/api/projects/featured`가 `{slug}` 상세로 잘못 라우팅되지 않는지, `X-Admin-Key` 없이 `POST /api/admin/sync` 호출 시 401 + `INVALID_ADMIN_KEY`, `GET /api/images/{id}`가 302 + `Location` 헤더인지(`browser_network_requests`) 확인

### Phase 3: 핵심 기능 구현

- **Task 006: Notion API 클라이언트와 재시도·호출 제한 정책 구현**
  - 관련: `global/notion/NotionClient`(인터페이스), `RestNotionClient`, `global/notion/dto/`, `NotionRetryExecutor`, `NotionRateLimiter`, `NotionClientConfig`
  - D-4: 착수 시 최신 Notion API 문서로 `Notion-Version`과 엔드포인트를 재확인한다
  - 메서드: `queryDataSource(dataSourceId, filter, cursor)`(`Status = Published`, `has_more`/`next_cursor`), `retrievePage(pageId)`, `retrieveBlockChildren(blockId, cursor)`, `retrieveBlock(blockId)`
  - `RestClient` 설정: base URL, `Authorization: Bearer`, `Notion-Version` 헤더(설정값), connect/read 타임아웃
  - 응답 DTO(record): page, 속성 값(title/rich_text/select/multi_select/date/files/url/checkbox/number), block(타입별 payload), rich text annotation. 모르는 필드/타입은 무시 (Boot 4 기본 Jackson 3 사용 여부와 어노테이션 패키지를 착수 시 확인)
  - 재시도: 429/529는 `Retry-After`(초)만큼 대기, 헤더 없으면 지수 백오프(최대 30초) + jitter, 최대 6회 시도. GET은 500/502/503/504도 재시도. 소진 시 429 계열은 `NOTION_RATE_LIMITED`, 그 외는 `NOTION_UNAVAILABLE`로 변환하고 로깅. 대기는 `Sleeper`/`Clock` 추상화로 주입해 테스트에서 실제로 기다리지 않는다. 새 의존성 없이 구현한다
  - 호출 제한: 요청 간 최소 간격을 두어 초당 3회를 넘지 않게 한다
  - 수락 기준: `@RestClientTest` 또는 `MockRestServiceServer.bindTo(RestClient.Builder)`로 아래 테스트 체크리스트 전부 통과. 외부 장애 범주(429, 529, 5xx, 타임아웃, 재시도 소진, 4xx 즉시 실패)는 빠짐없이 포함
  - E2E: 대상 아님(외부 호출 내부 동작은 브라우저로 재현 불가, 자동 테스트로 검증). `NOTION_TOKEN`이 있으면 Task 009 완료 후 실제 연동으로 재확인

- **Task 007: Notion 블록 → Markdown 변환기 구현**
  - 관련: `global/markdown/BlockToMarkdownConverter`, `RichTextRenderer`, `MarkdownResult`
  - 지원 블록(MVP): paragraph, heading_1~3, bulleted/numbered_list_item(중첩 들여쓰기), code(언어 포함), image, quote, divider, toggle(D-7 결정 반영)
  - rich text annotation: bold, italic, strikethrough, inline code, link. Markdown 특수문자 이스케이프
  - 자식 블록 재귀 조회는 호출자가 트리를 만들어 넘기고, 변환기는 트리 → 문자열만 담당한다(순수 로직, Spring 컨텍스트 불필요)
  - 이미지 블록은 URL 대신 이미지 참조를 결과에 모아 반환하고, 호출자가 `image_asset` 등록 후 `/api/images/{id}`로 치환한다
  - 지원하지 않는 블록(embed, column, child_database 등)은 건너뛰고 블록 ID·타입을 WARN 로그로 남긴다. 지원 블록 목록을 README 운영 가이드에 적는다
  - 수락 기준: 블록 타입별 단위 테스트 + 실제 Notion 응답 JSON fixture 기반 스냅샷 테스트 통과
  - E2E: 대상 아님(순수 변환 로직). 렌더링 결과는 Task F03 E2E에서 상세 화면으로 확인

- **Task 008: 전체 동기화 엔진 구현 (SyncService)**
  - 관련: `domain/sync/service/SyncService`(오케스트레이션), `ProjectSyncWriter`/`ProfileSyncWriter`(페이지 단위 트랜잭션), `NotionPageParser`(Notion 페이지 → 도메인 스냅샷), `ImageAssetRegistrar`, `SyncLock`
  - 흐름: data source 전체 조회 → `last_edited_time`과 `notion_last_edited_at` 비교 → 바뀐 페이지만 블록 트리 재귀 조회 → Markdown 변환 → upsert → 결과에 없는 기존 프로젝트 `hide()` → About 페이지 동기화 → `sync_history` 기록
  - 트랜잭션 경계: Notion HTTP 호출은 트랜잭션 밖에서 하고, 페이지 1건의 DB 반영만 별도 빈(`ProjectSyncWriter`)의 `@Transactional` 메서드로 처리한다(self-invocation으로 트랜잭션이 안 걸리는 문제 방지). 한 페이지가 실패해도 나머지는 계속 진행
  - 건너뛰기 규칙: 필수 속성 누락, slug 형식 오류, 다른 페이지와 slug 중복, 알 수 없는 Category → skip하고 사유를 `skip_details`에 기록. 이미 동기화된 프로젝트가 skip되면 숨기지 않고 마지막 정상 상태를 유지한다
  - 안전장치: 목록 조회가 중간에 실패하면 숨김 처리를 하지 않고 이력을 FAILED로 남긴다(일시 장애로 전체 프로젝트가 사라지는 것 방지)
  - 동시 실행 방지: `SyncLock`(애플리케이션 내 락 + `RUNNING` 부분 유니크 인덱스). 실행 중이면 `SYNC_ALREADY_RUNNING`. 기동 시 오래된 RUNNING 이력은 FAILED로 정리
  - 수락 기준: Fake `NotionClient`(테스트 전용 빈) + Testcontainers 통합 테스트로 멱등성(2회 실행 결과 동일), 숨김, skip 사유, 부분 실패(트랜잭션 격리), 조회 실패 시 숨김 미실행, 동시 실행 차단 검증 PASSED
  - E2E: 대상 아님(동기화 API 노출은 Task 009). 동기화 결과는 Task 009 E2E에서 확인

- **Task 009: 동기화 스케줄러와 수동 동기화 API 실제 구현**
  - 관련: `domain/sync/scheduler/SyncScheduler`, `global/config/SchedulingConfig`, `domain/sync/controller/AdminSyncController`, `domain/sync/service/SyncHistoryService`
  - `@EnableScheduling` + `@Scheduled`(30분, `SyncProperties`), `@ConditionalOnProperty`로 테스트/로컬에서 끌 수 있게 한다. 스케줄 실행이 이미 실행 중과 겹치면 예외 대신 INFO 로그만 남긴다
  - `POST /api/admin/sync`: 스텁을 실제 동기화 호출로 교체, `SyncResultResponse`(upserted, hidden, skipped, 소요시간) 반환. 실행 중이면 409
  - `GET /api/admin/sync/history`: 최신순 최근 N건(기본 20)
  - 수락 기준: 관리자 키 없음/틀림 401, 정상 200, 동시 요청 시 한쪽 409 통합 테스트 PASSED
  - E2E: Swagger UI에서 `X-Admin-Key` 입력 후 `POST /api/admin/sync` → 200 + 처리 건수 확인, 키 틀림 → 401, `GET /api/admin/sync/history`에 방금 실행 이력이 최신순으로 보이는지 확인. `NOTION_TOKEN`이 있으면 실제 Notion 샘플 3건 동기화 결과(upserted 3, skipped 1)까지 확인하고, 없으면 "미수행 + 사유" 기록

- **Task 010: 포트폴리오 조회 API 실제 구현**
  - 관련: `domain/project/service/ProjectQueryService`, `domain/project/repository/ProjectSpecifications`, `domain/profile/service/ProfileService`
  - Service에 `@Transactional(readOnly = true)`, 지연 로딩은 Service 안에서 끝내고 DTO만 반환(OSIV off)
  - `GET /api/projects`: `Specification`으로 `visible = true` + category + tech 필터. tech 필터는 조인 대신 `EXISTS` 서브쿼리로 해서 중복 행과 페이지네이션 왜곡을 막는다. 정렬은 `sort_order ASC NULLS LAST` → `period_start DESC`
  - 목록의 techStack 로딩 N+1 방지: 페이지 조회 후 ID 목록으로 tech를 한 번에 로딩(`@BatchSize` 또는 별도 쿼리)
  - `GET /api/projects/featured`, `GET /api/projects/{slug}`(비공개/없음 → `PROJECT_NOT_FOUND`), `GET /api/tech-stacks`(공개 프로젝트 기준 타입·이름별 사용 횟수 집계 JPQL), `GET /api/profile`(없으면 `PROFILE_NOT_FOUND`)
  - 수락 기준: 각 API 통합 테스트 PASSED, 목록 조회 시 실행 쿼리 수가 페이지 크기와 무관하게 고정인지 확인
  - E2E: `/api/projects?category=BACKEND` 결과가 모두 BACKEND, `tech=Java` 필터, 마지막 페이지와 빈 결과, `size=100` → 400, `/api/projects/unknown-slug` → 404 `PROJECT_NOT_FOUND`, `/api/tech-stacks` 사용 횟수, `/api/profile` 응답 확인

- **Task 011: 이미지 프록시 구현**
  - 관련: `domain/image/service/ImageProxyService`, `domain/image/controller/ImageController`
  - `GET /api/images/{id}`: 만료 전(여유시간 5분)이면 `cached_url`로 302 + `Cache-Control: max-age=(남은 유효시간 - 여유)`. 만료됐으면 source 타입에 따라 `retrieveBlock` 또는 `retrievePage`로 새 URL을 받아 `refreshUrl()` 후 302
  - Notion 호출은 트랜잭션 밖에서, URL 갱신만 짧은 쓰기 트랜잭션으로 처리한다. 동시 요청이 몰려도 결과가 같도록 마지막 쓰기 승리 허용
  - 없는 ID → `IMAGE_NOT_FOUND`(404), Notion 장애 → `NOTION_UNAVAILABLE`(502). 에러는 JSON `ApiResponse` 포맷
  - 수락 기준: 유효/만료/외부 URL(만료 없음)/없는 ID/Notion 장애 통합 테스트 PASSED
  - E2E: `/api/images/{id}` 접속 시 302 + `Location`, `Cache-Control` 헤더 확인(`browser_network_requests`), 최종 이미지가 브라우저에 표시되는지 확인, 없는 ID → 404 JSON 포맷

- **Task 012: 핵심 기능 통합 테스트**
  - 관련: `src/test/java/.../scenario/`, `support/FakeNotionClient`, `support/NotionFixtures`
  - 전체 흐름: Fake Notion 데이터 → `POST /api/admin/sync` → 목록/상세/기술스택/프로필/이미지 API 응답 검증
  - Notion 장애 시나리오: 동기화 실패 후에도 조회 API가 마지막 동기화 데이터로 정상 응답(PRD "Notion 장애가 곧 사이트 장애" 해결 검증)
  - 에러 응답 포맷 `{ success: false, data: null, error }` 일관성: 404(없는 slug, 없는 경로), 400(잘못된 category, size 초과, 타입 불일치), 401, 409
  - 엣지 케이스: 빈 결과 페이지, Published → Draft 전환 후 숨김, 다시 Published 시 복구, 본문 없는 페이지
  - 수락 기준: 시나리오 테스트 전부 PASSED(SKIPPED 0건), 테스트 실행 시간 기록. 시나리오 범주 8개가 모두 커버됐는지 체크리스트로 확인
  - E2E: Swagger UI에서 수동 동기화 → 목록 → 상세 → 기술스택 → 프로필 → 이미지 순서로 전체 흐름을 한 번에 호출해 확인하고, 에러 포맷(404/400/401/409) 케이스를 각각 1회 호출

### Phase F: 프론트엔드 (Phase 2 완료 후 병렬 진행 가능)

백엔드 Phase 2의 Swagger 계약(스텁 응답)만으로 시작할 수 있다. 실데이터 연동 확인은 Task 010~011 완료 후 한다.

- **Task F01: Next.js 프로젝트 세팅과 API 클라이언트 구현**
  - 의존 백엔드: Task 005 (API 계약)
  - D-8 결정 반영(코드 위치, Next.js 버전). App Router, TypeScript, Tailwind CSS, `@tailwindcss/typography`, shadcn/ui, Lucide React
  - `ApiResponse<T>`, `PageResponse<T>`와 DTO 타입 정의(Swagger 스펙 기반 생성 또는 수기), 공통 fetch 래퍼(에러 포맷 처리, `revalidate: 300`)
  - API base URL은 환경변수로 관리
  - 수락 기준: 스텁 API 대상 빌드·린트 통과
  - E2E: 개발 서버 접속 시 기본 레이아웃 렌더링, 백엔드 API 호출 성공(`browser_network_requests`), 콘솔 에러 0건

- **Task F02: 홈과 프로젝트 목록 화면 구현**
  - 의존 백엔드: `GET /api/projects/featured`, `GET /api/tech-stacks`, `GET /api/projects`
  - `/`: 히어로 + Featured 프로젝트 + 주요 기술스택
  - `/projects`: 카테고리 탭, 기술스택 필터(쿼리스트링 동기화), 카드(Problem과 Metrics 강조), 페이지네이션, 반응형
  - 수락 기준: 모바일/데스크톱 레이아웃 확인, 빈 결과 화면 처리
  - E2E: 홈 Featured 카드 표시, 카테고리 탭·기술스택 필터 클릭 시 결과와 쿼리스트링 변경, 페이지 이동, 빈 결과 화면, 375px/1280px 스크린샷, 콘솔 에러 0건

- **Task F03: 프로젝트 상세·About·404 화면과 ISR 구현**
  - 의존 백엔드: `GET /api/projects/{slug}`, `GET /api/profile`, `GET /api/images/{id}`
  - `/projects/[slug]`: 문제 → 해결 → 성과 요약 카드, 아키텍처 이미지, `react-markdown` 본문 렌더링(D-7 토글 처리 포함), `generateStaticParams`
  - `/about`, `not-found`(백엔드 404 응답 시 `notFound()`)
  - ISR 5분 revalidate
  - 수락 기준: 없는 slug 접근 시 404 화면, 이미지 프록시 URL 정상 표시
  - E2E: 목록 카드 클릭 → 상세 이동, 문제/해결/성과 카드와 Markdown 본문(제목, 코드, 리스트, 토글) 렌더링, 아키텍처 이미지 로딩, `/projects/unknown-slug` → 404 화면, `/about` 표시, 콘솔 에러 0건

### Phase 4: 운영 및 배포

- **Task 013: 운영 가시성과 성능 점검**
  - 관련: `domain/sync/`(메트릭), `global/config/`, `application.yml`
  - Micrometer 커스텀 메트릭: 동기화 실행 횟수/결과별 카운터, 소요시간 타이머, upserted/hidden/skipped, Notion 재시도 횟수와 429 발생 수
  - Actuator: 헬스체크 확인, 마지막 성공 동기화 시각을 헬스 details(또는 info)로 노출. Notion 장애가 헬스 DOWN으로 이어져 배포/재시작이 반복되지 않도록 DOWN 판정에는 넣지 않는다 (Boot 4에서 `HealthIndicator` 패키지 위치를 착수 시 확인)
  - 운영 프로파일 로깅 정리: `org.hibernate.orm.jdbc.bind: TRACE` 제거, 동기화 로그에 sync_history id 포함
  - 성능 점검: 조회 API 쿼리 플랜과 인덱스 사용 확인, N+1 재확인, 이미지 프록시 `Cache-Control` 확인
  - 수락 기준: `/actuator/metrics`에서 커스텀 메트릭 조회, 조회 API 로컬 p95 200ms 이하 측정 기록
  - E2E: 수동 동기화 1회 후 `/actuator/metrics/{커스텀 메트릭명}` 값 증가 확인, `/actuator/health` details에 마지막 동기화 시각 표시

- **Task 014: Docker 이미지, CI, 운영 배포**
  - D-1(배포 대상), D-2(관리자 API 보호 재검토) 결정 필요
  - 백엔드 Dockerfile(멀티 스테이지, JDK 25 런타임, non-root) 또는 `bootBuildImage` 중 선택
  - CI(GitHub Actions 등): Docker가 있는 러너에서 `./gradlew build` 실행, 테스트 리포트에서 SKIPPED 0건 검증 단계 포함
  - 백엔드와 PostgreSQL 배포, 환경변수(`NOTION_TOKEN`, `NOTION_VERSION`, `NOTION_PROJECTS_DATA_SOURCE_ID`, `NOTION_ABOUT_PAGE_ID`, `ADMIN_API_KEY`, DB 접속 정보) 설정. 비밀값은 플랫폼 시크릿으로만 관리
  - Vercel에 프론트 배포, API base URL 설정, 필요하면 백엔드 CORS 설정
  - 수락 기준: 운영 `/actuator/health` UP, 운영에서 수동 동기화 1회 성공 후 프론트 4개 화면 정상 표시
  - E2E: 운영 URL 대상으로 Playwright MCP 스모크 테스트(헬스체크, 4개 화면, 상세 이미지, 404 화면, 콘솔 에러 0건)

---

## 2차 범위

### Phase 5: Webhook 증분 동기화 (2차 목표 1순위)

- **Task 015: Notion Webhook 수신·서명 검증·중복 제거 구현 (Flyway V6)**
  - D-3(구독 이벤트 목록) 결정 필요
  - 관련: `V6__create_webhook_event.sql`, `domain/sync/entity/WebhookEvent`, `domain/sync/controller/NotionWebhookController`, `domain/sync/service/WebhookService`, `global/notion/WebhookSignatureVerifier`
  - `POST /api/webhooks/notion`: `@RequestBody String`으로 raw body를 받아 `X-Notion-Signature`(HMAC-SHA256, 키 `NOTION_WEBHOOK_VERIFICATION_TOKEN`)를 검증한 뒤에 파싱한다. 실패 시 `INVALID_WEBHOOK_SIGNATURE`(401). 상수 시간 비교
  - 구독 생성 시 Notion이 보내는 최초 검증 요청(verification token 전달)을 처리하고 로그로 확인 가능하게 한다(동작 방식은 착수 시 문서 확인)
  - 중복 제거: `webhook_event`(event_id PK)에 insert, 이미 있으면 무시하고 200. insert 충돌을 동시성 안전하게 처리
  - 응답은 곧바로 200 + `ApiResponse.ok()`. 실제 처리는 Task 016의 비동기 핸들러로 넘긴다
  - 수락 기준: 올바른 서명/틀린 서명/서명 없음/같은 이벤트 2회 통합 테스트 PASSED
  - E2E: 대상 아님(서명이 필요한 POST라 브라우저로 재현하기 어렵다. 자동 테스트로 검증). 실제 Notion 구독이 가능하면 Notion에서 페이지를 수정한 뒤 로그와 `webhook_event` 기록 확인

- **Task 016: 비동기 단건 증분 동기화 구현**
  - 관련: `global/config/AsyncConfig`, `domain/sync/service/IncrementalSyncService`, `domain/sync/listener/`
  - `@EnableAsync`, `spring.threads.virtual.enabled=true`로 자동 구성되는 Virtual Thread 실행기 사용. 비동기 예외는 `AsyncUncaughtExceptionHandler`로 로깅
  - `page.content_updated`/`page.properties_updated` → 해당 페이지만 조회해 `ProjectSyncWriter` 재사용(About 페이지 ID면 프로필 갱신), `page.deleted` → `hide()`
  - 전체 동기화와 겹칠 때의 정책: 같은 `SyncLock`을 공유하거나 페이지 단위 `last_edited_time` 비교로 오래된 데이터 덮어쓰기를 막는다
  - `sync_history`에 `WEBHOOK` 트리거로 기록
  - 수락 기준: Webhook 수신 → 비동기 처리 완료 후 조회 API 반영을 `Awaitility` 또는 폴링으로 검증(Awaitility는 Boot BOM 관리 대상 여부 확인 후 추가), 삭제 이벤트 숨김 검증 PASSED
  - E2E: 실제 Notion 구독이 있으면 Notion에서 제목 수정 → `/api/projects/{slug}`와 프론트 상세 화면에 반영되는지 확인, 없으면 "미수행 + 사유" 기록

### 향후 과제 (Task 미생성, PRD MVP 제외 항목)

- 동기화 완료 후 Next.js on-demand revalidate 호출
- 이미지를 S3에 영구 저장해 프록시 대체
- 기술 블로그(Posts DB), 전문 검색(PostgreSQL FTS), 방문 통계
- Redis 캐시, Prometheus + Grafana 모니터링 대시보드

---

## 작업 파일 테스트 체크리스트 가이드

각 작업 파일의 `## 테스트 체크리스트`는 아래 수준을 구분해 작성한다. Task별 핵심 시나리오는 다음과 같다.

| Task | 단위 테스트 | 통합 테스트 (`AbstractIntegrationTest` 상속) | 외부 연동 테스트 (`MockRestServiceServer`) |
|---|---|---|---|
| 001 | - | 컨텍스트 기동, 없는 경로 404 포맷 | - |
| 002 | `Project` 생성/변경/숨김, slug 검증, 변경 여부 판정 | UNIQUE 제약, tech 교체 | - |
| 003 | 이미지 만료 판정, 이력 상태 전이 | 마이그레이션 validate, JSONB 저장/조회 | - |
| 004 | 매퍼 변환(그룹핑, null, 이미지 URL) | - | - |
| 005 | - | `@WebMvcTest` 계약 테스트, 관리자 키 401 | - |
| 006 | 백오프 계산, `Retry-After` 파싱 | - | 200 정상, 429 후 성공, `Retry-After` 준수, 6회 소진 → `NOTION_RATE_LIMITED`, GET 5xx 재시도, 4xx 즉시 실패, 페이지네이션 커서, 타임아웃 |
| 007 | 블록 타입별 변환, 중첩 리스트, annotation, 미지원 블록 skip | - | - |
| 008 | 페이지 파서(필수 속성 누락, 잘못된 Category) | 멱등성, 숨김, skip 사유, 부분 실패, 조회 실패 시 숨김 없음, 동시 실행 409 | (Fake NotionClient 사용) |
| 009 | - | 스케줄러 비활성 확인, 수동 동기화 200/401/409, 이력 조회 | - |
| 010 | Specification 조합 | 필터/정렬/페이지네이션, 비공개 404, 기술스택 집계, 쿼리 수 | - |
| 011 | 만료 여유시간 판정 | 302 Location/Cache-Control, 만료 갱신, 404, 502 | Notion 블록/페이지 재조회 |
| 012 | - | 전체 시나리오, Notion 장애 시 조회 유지, 에러 포맷 | - |
| 015 | 서명 계산/비교 | 서명 성공/실패, 중복 이벤트 | - |
| 016 | 이벤트 타입 라우팅 | 비동기 반영, 삭제 숨김 | - |

- 실행 명령 예: `./gradlew test --tests '*NotionClientTest'`, `./gradlew test --tests '*SyncServiceIntegrationTest'`
- 시나리오는 "테스트 시나리오 범주" 8개 기준으로 작성하고, 해당 없는 범주는 사유를 남긴다
- **주의**: Docker 없이 실행하면 통합 테스트가 SKIPPED된다. 완료 판정 전에 SKIPPED 0건을 반드시 확인한다

작업 파일의 `## E2E 검증 (Playwright MCP)` 섹션은 아래 형식으로 작성한다. Task별 확인 포인트는 각 Task의 "E2E" 항목을 옮겨 적는다.

```markdown
## E2E 검증 (Playwright MCP)

- [ ] 사전: `./gradlew bootRun`, `/actuator/health` UP
- [ ] 정상: `GET /api/projects?category=BACKEND` → 200, `success=true`, 모든 항목 category=BACKEND
- [ ] 에러: `GET /api/projects/unknown-slug` → 404, `error.code=PROJECT_NOT_FOUND`
- [ ] Swagger UI에서 변경 엔드포인트 Try it out 호출 성공

### 결과 기록
| 케이스 | URL | 기대 | 실제 | 스크린샷 |
|---|---|---|---|---|
```

## PRD 대비 조정 사항

- **순서 조정**: PRD는 "2. Notion 클라이언트 → 3. 스키마와 도메인" 순이지만, 계약 우선 원칙에 따라 스키마/엔티티(Phase 1)와 API 계약 스텁(Phase 2)을 먼저 두고 Notion 클라이언트는 Phase 3 첫 Task로 옮겼다. 프론트엔드가 Phase 2 이후 병렬로 진행할 수 있다
- **스키마 보강**: API 응답에 필요한데 PRD 스키마 표에 없는 `project.github_url`, `api_docs_url`, `architecture_image_id`, 변경 감지용 `profile.notion_last_edited_at`, skip 사유 저장용 `sync_history.skip_details`(JSONB), 이미지 재조회 분기용 `image_asset.source_type`를 추가했다. 엔티티 공통 `created_at`/`updated_at`은 기존 `BaseTimeEntity`를 따른다
- **컬럼명 변경**: `sync_history.trigger`는 SQL 예약어라 `trigger_type`으로 바꿨다
- **Flyway V6(webhook_event)**: PRD 구현 단계대로 2차(Task 015)에서 만든다. MVP 마이그레이션은 V1~V5
- **페이지 응답**: PRD의 `ApiResponse<Page<...>>`는 Spring Data `Page`를 직접 직렬화하지 않고 `PageResponse<T>` record로 계약을 고정한다
- **응답 포맷 예외**: 이미지 프록시 성공 응답은 302 리다이렉트라 `ApiResponse` 본문이 없다. 이미지 프록시의 에러 응답과 Webhook 200 응답은 `ApiResponse` 포맷을 유지한다
- **동기화 안전장치 추가**: 목록 조회가 중간에 실패하면 숨김 처리를 하지 않고, 이미 동기화된 프로젝트가 필수 속성 누락으로 skip되면 마지막 정상 상태를 유지한다(PRD의 "Notion이 멈춰도 사이트는 동작" 목표에 맞춤)
- **테스트 격리 방식**: HTTP 수준 재시도/실패는 `MockRestServiceServer`(Task 006)로, 동기화·조회 통합 테스트는 `NotionClient` 인터페이스의 Fake 구현(Task 008, 012)으로 검증한다
