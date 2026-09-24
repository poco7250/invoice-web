# Notion Folio PRD

> Notion을 CMS로 쓰는 개발자 포트폴리오 웹사이트

## 프로젝트 개요

- **프로젝트명**: Notion Folio (가칭)
- **목적**: Notion을 CMS로 써서, Notion에 정리한 프로젝트·경력 기록을 개인 포트폴리오 웹사이트로 자동 게시한다
- **CMS 선택 이유**: Notion API를 쓰면 코드를 고치거나 다시 배포하지 않아도 된다. Notion에서 글을 쓰고 상태를 `Published`로 바꾸기만 하면 사이트에 반영되므로, 비개발자도 콘텐츠를 관리할 수 있다
- **대상 사용자**
  - 방문자: 채용 담당자, 협업 제안자. 프로젝트와 기술스택을 빠르게 훑어보고 싶어 한다
  - 운영자(본인): Notion에서만 콘텐츠를 관리하고 싶어 한다

## 주요 기능

1. **프로젝트 목록**: 카드 그리드로 보여주고 카테고리·기술스택으로 필터링한다
2. **프로젝트 상세**: Notion 페이지 본문(블록)을 웹 페이지로 렌더링한다. 기간, 역할, 기술스택, GitHub/Demo 링크를 함께 표시한다
3. **About**: 자기소개, 경력, 기술스택, 연락처를 Notion 페이지 하나에서 불러온다
4. **자동 반영**: ISR(Incremental Static Regeneration)로 Notion의 수정 사항을 주기적으로 반영한다

## 기술 스택

| 구분 | 기술 | 비고 |
|---|---|---|
| Frontend | Next.js 15 (App Router), TypeScript | Server Component에서 Notion 호출 |
| CMS | Notion API (`@notionhq/client`) | 공식 SDK, `dataSources.query` 사용 |
| 본문 변환 | `notion-to-md` → `react-markdown` | 블록 → Markdown → React |
| Styling | Tailwind CSS, `@tailwindcss/typography`, shadcn/ui | 본문은 `prose` 클래스로 스타일링 |
| Icons | Lucide React | |
| 배포 | Vercel | ISR 지원 |

## Notion 데이터베이스 구조

### Projects (데이터베이스)

| 속성 | Notion 타입 | 필수 | 설명 |
|---|---|---|---|
| Title | `title` | O | 프로젝트명 |
| Slug | `rich_text` | O | URL 경로 (`/projects/[slug]`). 영문 소문자와 하이픈만 허용 |
| Summary | `rich_text` | O | 카드에 들어갈 한 줄 소개 |
| Thumbnail | `files` | - | 대표 이미지. 없으면 기본 이미지를 쓴다 |
| Category | `select` | O | Web / Mobile / Backend / Etc |
| TechStack | `multi_select` | O | 사용 기술 (필터용) |
| Period | `date` (range) | O | 진행 기간 (시작일~종료일) |
| Role | `rich_text` | - | 담당 역할 |
| GithubUrl | `url` | - | 저장소 링크 |
| DemoUrl | `url` | - | 배포 링크 |
| Featured | `checkbox` | - | 체크하면 홈에 노출 |
| Status | `select` | O | Draft / Published. **Published만 사이트에 노출** |
| Order | `number` | - | 정렬 순서 (작을수록 앞에 나온다) |
| (페이지 본문) | blocks | - | 상세 설명: 배경, 문제, 해결, 결과, 회고 |

### About (일반 페이지)

DB가 아니라 Notion 페이지 하나로 관리한다. 본문 블록 전체를 About 화면에 렌더링한다.

### 환경변수

```
NOTION_TOKEN=secret_xxx               # Internal Integration 토큰
NOTION_PROJECTS_DATA_SOURCE_ID=xxx    # Projects DB의 data source ID
NOTION_ABOUT_PAGE_ID=xxx              # About 페이지 ID
```

> Notion API 2025-09-03 버전부터 데이터베이스 하나가 data source를 여러 개 가질 수 있다. 조회할 때는 database ID가 아니라 **data source ID**를 써야 하고, 둘은 서로 바꿔 쓸 수 없다. data source ID는 데이터베이스를 조회한 응답의 `data_sources` 배열에서 얻는다.

## 화면 구성

| 경로 | 화면 | 설명 |
|---|---|---|
| `/` | 홈 | 히어로(이름, 한 줄 소개, 연락처 아이콘) + Featured 프로젝트 카드 |
| `/projects` | 프로젝트 목록 | 카테고리 탭, 기술스택 필터 배지, 카드 그리드 (Order 오름차순 → Period 최신순) |
| `/projects/[slug]` | 프로젝트 상세 | 썸네일, 메타 정보(기간·역할·기술스택·링크), 본문 렌더링 |
| `/about` | 소개 | About 페이지 본문 렌더링 |
| `not-found` | 404 | 없는 slug이거나 Draft인 프로젝트에 접근했을 때 |

공통: 상단 네비게이션(Home / Projects / About), 다크모드 토글, 모바일 반응형.

## MVP 범위

**포함**
- Projects DB 연동 (읽기 전용, `Status = Published`만 조회)
- 홈 / 목록 / 상세 / About 4개 화면
- 카테고리·기술스택 필터 (클라이언트 사이드)
- 본문 블록 렌더링: 문단, 제목, 리스트, 코드, 이미지, 인용, 구분선, 토글
- ISR + `generateStaticParams`로 상세 페이지 정적 생성
- 반응형 + 다크모드
- 기본 SEO: 페이지별 `metadata`, `sitemap.xml`

**제외 (향후 과제)**
- 기술 블로그(Posts DB), 검색, 다국어, 댓글
- Notion Webhook을 이용한 즉시 반영 (MVP는 주기적 revalidate만)
- 방문 통계

## 구현 단계

1. **Notion 준비**
   Integration을 만들고 Projects DB와 About 페이지를 설계한 뒤 Integration을 연결한다. 샘플 프로젝트 3개를 입력하고 `.env.local`을 설정한다.
2. **프로젝트 세팅**
   Next.js 15 + TypeScript + Tailwind + shadcn/ui를 초기화하고 레이아웃과 네비게이션을 잡는다.
3. **Notion 데이터 레이어**
   `lib/notion.ts`(클라이언트, 조회 함수)와 `types/project.ts`(도메인 타입)를 만든다. Notion 응답을 도메인 타입으로 바꾸는 매퍼도 만들고, 페이지네이션(`has_more`/`next_cursor`)을 처리한다.
4. **목록·상세 페이지**
   카드 컴포넌트, 필터, 상세 메타 영역을 만든다. 본문은 `notion-to-md` → `react-markdown`으로 렌더링한다.
5. **홈·About·404**
   Featured 섹션, About 렌더링, not-found 페이지를 만든다.
6. **마무리·배포**
   ISR 주기를 설정하고 이미지 만료에 대응한다. SEO metadata와 sitemap을 넣고 Vercel에 배포한다.

## 기술 리스크

| 리스크 | 영향 | 대응 |
|---|---|---|
| **Notion 호스팅 파일 URL은 1시간 후 만료** | 캐시된 페이지의 썸네일과 본문 이미지가 깨진다 | revalidate를 1시간보다 짧게(예: 30분) 잡는다. 장기적으로는 빌드할 때 이미지를 외부 스토리지(Vercel Blob 등)에 다시 올리는 방식을 검토한다 |
| Notion API rate limit (평균 초당 약 3회) | 빌드할 때 상세 페이지를 대량 생성하면 429 응답을 받는다 | 목록 조회 결과를 재사용하고 요청을 순차 처리한다. 429가 오면 `Retry-After`만큼 기다렸다가 재시도한다 |
| data source 모델 전환 (API 2025-09-03~) | 옛날 자료에 나오는 `databases.query`로는 동작하지 않는다 | `dataSources.query`와 data source ID를 쓴다. SDK와 `Notion-Version`은 최신 버전으로 고정한다 |
| `notion-to-md`가 지원하지 않는 블록 | 임베드나 컬럼 같은 블록이 누락된다 | MVP 지원 블록 목록을 운영 가이드에 적어둔다. 필요하면 custom transformer를 추가한다 |
| Slug 중복·누락 | 라우팅 충돌이나 404가 난다 | 빌드할 때 slug를 검증하고, 중복되거나 비어 있으면 빌드를 실패시킨다 |
