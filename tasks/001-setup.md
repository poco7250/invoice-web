# Task 001: 스타터 킷을 Notion Folio 프로젝트로 정리하고 공통 인프라 세팅

## 명세

- 목적: 스타터 킷 흔적을 도메인 코드가 생기기 전에 정리하고, 이후 Task가 공통으로 쓰는 설정·에러 처리·의존성을 먼저 깔아둔다
- 범위
  - 포함: 패키지/앱 이름 변경, 샘플 `domain/user` 제거, ErrorCode·예외 핸들러 보강, 설정 프로퍼티 record, RestClient 의존성, 스모크/에러 포맷 테스트, 작업 파일
  - 제외: 엔티티·마이그레이션(Task 002~003), Notion 클라이언트 구현(Task 006)
- 결정 반영
  - **D-5 (a)**: `domain/user`, `UserTest`, `UserApiIntegrationTest`, `V1__create_users.sql`, `USER_NOT_FOUND`/`DUPLICATE_EMAIL` 삭제. PRD 마이그레이션 번호 V1~V6을 그대로 쓴다. 로컬 DB는 `docker compose down -v`로 초기화
  - **D-6**: 패키지 `com.poco7250.notionfolio`, 앱 이름 `notion-folio`, group `com.poco7250`

## 관련 파일

| 파일 | 변경 | 설명 |
|---|---|---|
| `src/main/java/com/poco7250/notionfolio/**` | 이동 | `com.example.starterkit`에서 이동 |
| `NotionFolioApplication.java` | 수정 | 클래스명 변경, `@ConfigurationPropertiesScan` |
| `global/exception/ErrorCode.java` | 수정 | 공통 `RESOURCE_NOT_FOUND` + PRD 8개 |
| `global/exception/GlobalExceptionHandler.java` | 수정 | 404 경로, 헤더 누락, 타입 불일치, 405 핸들러 |
| `global/notion/NotionProperties.java` | 생성 | Notion 연동 설정 |
| `global/admin/AdminProperties.java` | 생성 | 관리자 키 |
| `domain/sync/SyncProperties.java` | 생성 | 스케줄 설정 |
| `global/config/OpenApiConfig.java` | 수정 | 제목 `Notion Folio API` |
| `build.gradle.kts`, `settings.gradle.kts` | 수정 | group, 프로젝트명, restclient 스타터, `flyway-database-postgresql` |
| `src/main/resources/application.yml` | 수정 | 앱 이름, 환경변수 플레이스홀더 |
| `src/test/resources/application.yml` | 수정 | 더미값, 스케줄러 off |
| `domain/user/**`, `V1__create_users.sql` | 삭제 | D-5 |

## 수락 기준

- [x] `./gradlew build` 통과
- [x] 테스트 리포트에서 SKIPPED 0건, 통합 테스트 PASSED
- [x] 컨텍스트 기동 스모크 테스트(`AbstractIntegrationTest` 상속) PASSED
- [x] 없는 경로 요청 시 404 + `ApiResponse` 실패 포맷 테스트 PASSED

## 구현 단계

- [x] 1. 패키지 리네임과 샘플 user 도메인 제거 (Shrimp T1)
- [x] 2. ErrorCode 추가와 GlobalExceptionHandler 보강 (T2)
- [x] 3. 설정 프로퍼티 record와 restclient 의존성 추가 (T3)
- [x] 4. 컨텍스트 기동과 공통 에러 응답 통합 테스트 작성 (T4)
- [x] 5. 작업 파일과 프로젝트 문서 갱신 (T5)
- [ ] 6. 빌드·SKIPPED 0·E2E 검증과 결과 기록 (T6) — 빌드/테스트 완료, Playwright MCP 미수행(아래 참고)

### Notion 준비 (코드 외 작업)

- [ ] Notion Integration 생성, 토큰 확보 (`NOTION_TOKEN`)
- [ ] Projects 데이터베이스 생성 (PRD 속성 19개, `Status`는 Select 타입)
- [ ] About 페이지 생성
- [ ] 두 곳 모두 Integration 연결 (Connections)
- [ ] 샘플 프로젝트 3개 입력 + 필수 속성 누락 페이지 1개 (skip 시나리오용)
- [ ] data source ID 확보 (`NOTION_PROJECTS_DATA_SOURCE_ID`, database ID 아님), About 페이지 ID 확보

## 테스트 체크리스트

실행 명령: `./gradlew test`, `./gradlew test --tests '*GlobalExceptionHandlerIntegrationTest'`

| 범주 | 시나리오 | 테스트 수준 | 결과 |
|---|---|---|---|
| 정상 흐름 | 컨텍스트 기동(마이그레이션 0개), 프로퍼티가 테스트 더미값과 기본값으로 바인딩 | 통합 (`ApplicationContextSmokeTest`) | [x] |
| 입력 검증 실패 | 필수 헤더 누락 → 400 `INVALID_INPUT`(헤더명 포함), 파라미터 타입 불일치 → 400(입력값 미반사), 허용되지 않은 메서드 → 405 | 통합 (`GlobalExceptionHandlerIntegrationTest`) | [x] |
| 경계값 | 프로퍼티 값이 없을 때 기본값 채움, 값이 있을 때 유지 | 단위 (`NotionPropertiesTest`, `SyncPropertiesTest`) | [x] |
| 외부 장애 | 해당 없음: Notion 호출 코드가 아직 없다 (Task 006) | - | - |
| 멱등성·중복 | 해당 없음: 쓰기 동작이 없다 | - | - |
| 동시성 | 해당 없음: 공유 상태가 없다 | - | - |
| 트랜잭션 | 해당 없음: DB 쓰기가 없다 | - | - |
| 에러 응답 포맷 | 없는 경로 → 404 `RESOURCE_NOT_FOUND`, 제거된 `/api/users`·`/api/v1/users/1` → 404, 모든 실패가 `success=false` + `error.code` | 통합 | [x] |

> **결정 필요 메모**: `jackson.default-property-inclusion: non_null` 때문에 실패 응답에 `data` 키가 아예 없다. ROADMAP·PRD 표기(`data: null`)와 다르다. 이번 Task에서는 정책을 바꾸지 않고 테스트에서 `$.data` 부재를 검증한다. 계약을 `data: null`로 고정할지는 Task 005(API 계약) 전에 정한다.

## E2E 검증 (Playwright MCP)

- [x] 사전: `./gradlew bootRun`, `/actuator/health` UP (curl)
- [ ] Swagger UI(`/swagger-ui.html`) 제목이 `Notion Folio API`로 표시 — `/v3/api-docs`로 확인, 화면 확인은 미수행
- [x] 에러: `/api/unknown` → 404, `success=false`, `error.code=RESOURCE_NOT_FOUND` (curl)
- [x] 에러: `/api/users` → 404 (샘플 API 미노출) (curl)
- [x] Swagger UI에 user 엔드포인트가 없다 (`/v3/api-docs`의 paths가 빈 배열)

> **Playwright MCP 미수행 (2026-09-26)**: MCP 서버가 `chrome` 채널을 쓰는데 `/Applications/Google Chrome.app`이 없어 기동에 실패했다(`npx playwright install chrome` 필요). 같은 케이스를 curl로 대신 확인했다. Chrome 설치 후 Playwright로 Swagger UI 화면을 다시 확인해야 ROADMAP에 ✅를 붙인다.

### 결과 기록
| 케이스 | URL | 기대 | 실제 | 스크린샷 |
|---|---|---|---|---|
| 헬스체크 | `GET /actuator/health` | 200, `status=UP` | 200, `{"status":"UP"}` | 미수행 (curl) |
| Swagger 제목 | `GET /v3/api-docs` | `info.title=Notion Folio API` | `Notion Folio API` | 미수행 (curl) |
| Swagger UI 진입 | `GET /swagger-ui.html` | 302 → `/swagger-ui/index.html` | 302 → `/swagger-ui/index.html` | 미수행 (curl) |
| 없는 경로 | `GET /api/unknown` | 404, `success=false`, `RESOURCE_NOT_FOUND` | 404, 동일 | 미수행 (curl) |
| 샘플 API 제거 | `GET /api/users`, `GET /api/v1/users/1` | 404 `RESOURCE_NOT_FOUND` | 404, 동일 | 미수행 (curl) |
| user 엔드포인트 미노출 | `GET /v3/api-docs` | paths에 user 없음 | paths `[]` | 미수행 (curl) |

### 자동 테스트 결과 (2026-09-26)
- `./gradlew test --rerun`: 11개 PASSED, SKIPPED 0, FAILED 0 (Docker Desktop 29.8.0, Testcontainers PostgreSQL 18)

## 변경 사항 요약

- 패키지 `com.poco7250.notionfolio`, 앱 이름 `notion-folio`, 샘플 user 도메인 제거
- `ErrorCode` 9개 추가, `GlobalExceptionHandler`에 404 경로/헤더 누락/타입 불일치/405 핸들러와 `toResponse` 헬퍼
- `NotionProperties`/`AdminProperties`/`SyncProperties` record + 기본값, `@ConfigurationPropertiesScan`
- 의존성: `spring-boot-starter-restclient`(+test), `flyway-database-postgresql`(기존 결함: 없으면 PostgreSQL 18 기동 실패)
- 테스트: 스모크, 공통 에러 포맷 통합 테스트, 프로퍼티 단위 테스트
