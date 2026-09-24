# Invoice Web

운영자가 **Notion 데이터베이스**에 입력한 견적서를 클라이언트가 **공유 링크로 웹에서 확인**하고 **PDF로 다운로드**할 수 있는 웹 서비스.

## 프로젝트 개요

| 항목 | 내용 |
|---|---|
| 운영자 | Notion에서 견적서와 견적 항목을 작성한다. 별도 관리 화면 없이 Notion이 관리 UI 역할을 한다 |
| 클라이언트 | 로그인 없이 추측할 수 없는 토큰 URL로 견적서를 조회하고 PDF로 받는다 |
| 데이터 소스 | Notion API (읽기 전용) |

### MVP 범위

- Notion 견적서 DB + 견적 항목 DB 연동
- 토큰 URL 기반 견적서 웹 조회
- 견적서 PDF 다운로드 (웹 화면과 같은 내용)
- 공급가액 / 부가세 / 합계 계산
- Notion API 응답 캐싱과 rate limit 대응

### 문서

- [`docs/PRD_PROMPT.md`](docs/PRD_PROMPT.md): 견적서 뷰어 PRD 생성 프롬프트
- [`docs/PRD.md`](docs/PRD.md): PRD 문서

---

아래는 이 프로젝트의 베이스인 **Spring Starter Kit**의 설명이다. 레이어드 아키텍처(Controller → Service → Repository) 기반이다.

## 기술 스택

| 항목 | 버전 | 비고 |
|---|---|---|
| Java | 25 | 최신 LTS |
| Spring Boot | 4.1.1 | 최신 GA |
| Spring Framework | 7.0.9 | Boot BOM 관리 |
| Gradle | 9.7.1 | Kotlin DSL + 버전 카탈로그 |
| PostgreSQL | 18 | 로컬은 Docker Compose |
| Hibernate | 7.4.5 | Boot BOM 관리 |
| Flyway | 12.4.0 | Boot BOM 관리 |
| Testcontainers | 2.0.5 | Boot BOM 관리 |
| springdoc-openapi | 3.1.1 | Boot 4 대응 라인 |
| MapStruct | 1.6.3 | |

> BOM이 관리하는 의존성은 `build.gradle.kts`에 버전을 쓰지 않는다. 검증된 조합이 깨진다.

## 실행

로컬에 JDK 25가 없어도 된다. Gradle 툴체인이 자동으로 받아온다.

```bash
# DB 포함 실행 (Docker 필요, compose.yaml의 PostgreSQL이 자동으로 뜸)
./gradlew bootRun

# 빌드
./gradlew build

# 단위 테스트만 (Docker 불필요)
./gradlew test --tests '*UserTest'

# 전체 테스트 (Docker 없으면 통합 테스트는 자동 skip)
./gradlew test
```

| 주소 | 용도 |
|---|---|
| http://localhost:8080/swagger-ui.html | API 문서 |
| http://localhost:8080/actuator/health | 헬스체크 |

## 패키지 구조

```
com.example.starterkit
├─ global/                     공통 인프라
│  ├─ config/                  JpaConfig(Auditing), OpenApiConfig
│  ├─ entity/BaseTimeEntity    createdAt / updatedAt 자동 기록
│  ├─ exception/               ErrorCode, BusinessException, GlobalExceptionHandler
│  └─ response/                ApiResponse<T>, ErrorDetail
└─ domain/
   └─ user/                    도메인별로 레이어를 묶는다
      ├─ controller/           요청·응답 변환만
      ├─ service/              비즈니스 로직 + 트랜잭션 경계
      ├─ repository/           데이터 접근
      ├─ entity/               도메인 모델 (setter 금지, 도메인 메서드로만 변경)
      ├─ dto/                  record 기반 요청/응답
      └─ mapper/               MapStruct 매핑
```

새 도메인을 추가할 때는 `domain/user`를 통째로 복사해서 이름만 바꾸면 된다.

## 설계 규칙

**트랜잭션은 Service 레이어에만.** 클래스에 `@Transactional(readOnly = true)`를 걸고 쓰기 메서드에만 `@Transactional`을 덮어쓴다. Controller나 Repository에 흩어지면 경계가 무너진다.

**API 응답은 항상 `ApiResponse<T>`로 감싼다.** 성공이든 실패든 `{ success, data, error }` 형태가 유지되어 클라이언트가 분기하기 쉽다.

**예외는 `BusinessException` + `ErrorCode`로.** `ErrorCode` enum에 HTTP 상태와 메시지를 같이 정의하면 핸들러를 새로 만들 필요가 없다.

**엔티티에 setter를 두지 않는다.** `User.create()`, `user.changeName()`처럼 의도가 드러나는 도메인 메서드만 노출한다.

**스키마 변경은 Flyway가 전담한다.** `ddl-auto: validate`로 두었기 때문에 엔티티와 스키마가 어긋나면 기동 시점에 바로 실패한다. 변경은 `src/main/resources/db/migration/V{n}__{설명}.sql` 추가로 한다.

**OSIV는 꺼져 있다.** (`open-in-view: false`) 지연 로딩은 Service 트랜잭션 안에서 끝내고, Controller로는 DTO만 넘긴다.

## 알아둘 것

**Spring Boot 4에서 바뀐 것들**

- `spring-boot-starter-web`은 deprecated → `spring-boot-starter-webmvc` 사용
- Flyway는 전용 스타터 `spring-boot-starter-flyway`로 분리됨
- `@AutoConfigureMockMvc` 패키지 이동: `org.springframework.boot.webmvc.test.autoconfigure`
- MVC 테스트용 스타터 `spring-boot-starter-webmvc-test` 신설

**Lombok + MapStruct 조합**

`org.projectlombok:lombok-mapstruct-binding`을 `annotationProcessor`로 같이 걸어야 한다. 빠지면 MapStruct가 Lombok이 생성한 getter를 못 보고 `Unmapped target properties` 컴파일 에러가 난다. 애노테이션 프로세서 실행 순서 문제라 코드만 봐서는 원인을 찾기 어렵다.

**Testcontainers 2.x에서 바뀐 것들**

- 모듈 아티팩트에 접두사가 붙음: `org.testcontainers:postgresql` → `org.testcontainers:testcontainers-postgresql`
- 컨테이너 클래스 패키지 이동: `org.testcontainers.containers.PostgreSQLContainer` → `org.testcontainers.postgresql.PostgreSQLContainer`

## 아직 안 들어간 것

필요해지면 붙이면 된다.

- **Spring Security + JWT** — 넣는 순간 모든 엔드포인트가 잠기므로 인증 정책을 정하고 추가하는 게 낫다
- **QueryDSL** — openfeign 포크 7.6이 최신이나 Hibernate 7.4 조합 검증이 필요하다. 동적 쿼리는 우선 Spring Data `Specification`으로 시작하고, 부족해지면 QueryDSL이나 jOOQ를 붙이는 순서를 권장
- **Redis 캐시** — `spring-boot-starter-data-redis` + `@Cacheable`
- **Spotless / Checkstyle** — 팀 컨벤션 확정 후 추가
