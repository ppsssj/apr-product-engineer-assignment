# README 교체 검토안

`README.md`는 수정하지 않았습니다. 이 파일은 최종 제출용 README 후보를 검토하기 위한 문서이며, 실제 교체가 끝나면 삭제합니다.

---

# 1. 현재 README 원본

````markdown
# 메디큐브톡 커뮤니티

> 이 파일은 **제출용 README의 템플릿**입니다.
> `## 실행` 절은 그대로 두시고 (변경한 게 있으면 고쳐주세요),
> 아래 빈 절들을 채워주세요. 과제 내용은 [`ASSIGNMENT.md`](./ASSIGNMENT.md)를 확인해주세요.

---

## 실행

```bash
docker compose up
```

| | 주소 |
| --- | --- |
| 프론트엔드 | http://localhost:3000 |
| 백엔드 | http://localhost:8080 |
| 기동 확인 | http://localhost:8080/health · `GET /health` 는 유지해주세요 |

포트가 이미 사용 중이면 환경 변수로 바꿀 수 있습니다.

```bash
FRONTEND_PORT=13000 BACKEND_PORT=18080 DB_PORT=25432 docker compose up --build
```

`BACKEND_PORT`를 바꾸면 프론트엔드를 다시 빌드해야 해서 `--build`가 필요합니다.
`NEXT_PUBLIC_API_BASE_URL`이 빌드 시점에 클라이언트 번들로 인라인되기 때문입니다.

<details>
<summary>참고 — 제공된 스타터의 구성</summary>

```
seed/seed.json        초기 데이터
docker-compose.yml    DB(PostgreSQL 16) + 백엔드 + 프론트엔드
backend/              Spring Boot 3.5 / Java 17. DB 연결 · GET /health · CORS 설정만 있습니다.
frontend/             Next.js 15 App Router. 기본 설정과 app/dev-user/ 만 있습니다.
```

- **DB 호스트 포트 기본값은 `15432`** 입니다. `5432`는 이미 쓰고 있는 경우가 많아 피했습니다.
- `seed/`는 백엔드 컨테이너의 `/seed`에 읽기 전용으로 마운트됩니다. (`APP_SEED_PATH`)
- DB 데이터는 named volume(`dbdata`)에 남습니다. `docker compose down`을 해도 유지되고,
  **초기화하려면 `docker compose down -v`** 를 쓰세요.
- 브라우저에서 호출할 때와 컨테이너 안에서 호출할 때 주소가 달라, API 주소를 두 개 줍니다.
  - `NEXT_PUBLIC_API_BASE_URL` — 브라우저에서 (클라이언트 컴포넌트)
  - `API_BASE_URL` — 컨테이너 안에서 (서버 컴포넌트 · 라우트 핸들러)
- **`/dev-user`** 에서 요청자를 바꿀 수 있습니다. 고르면 `x-user-id` 쿠키가 설정됩니다.
  쿠키가 없으면 기본 요청자는 `apr_tester`입니다.
- 백엔드 의존성에는 `spring-boot-starter-jdbc`만 들어 있습니다. ORM은 직접 고르세요.
- 테스트 환경(`spring-boot-starter-test` · JUnit · Mockito)은 준비되어 있습니다.
  테스트를 작성하면 그대로 동작합니다.
- 폰트는 `frontend/public/fonts/`의 **Pretendard**를 쓰면 됩니다. `globals.css`에 적용되어 있습니다.
- **CORS는 설정되어 있습니다** (`CorsConfig.java`). `http://localhost:*`(및 `127.0.0.1:*`) 오리진을
  자격 증명과 함께 허용하므로 포트를 바꿔도 동작합니다. 브라우저에서 직접 호출할 때는 요청에
  `credentials: "include"`를 붙여야 `x-user-id` 쿠키가 전송됩니다.

로컬 개발은 compose 없이 하셔도 됩니다. DB만 띄우고 각각 실행하는 쪽이 빠릅니다.

```bash
docker compose up db
cd backend  && ./gradlew bootRun
cd frontend && npm install && npm run dev
```

</details>

---

## 기술 선택

무엇을 골랐고 왜 그렇게 정했는지 적어주세요.
(상태 관리, 스타일링, ORM, 그 외 주요 라이브러리)

<!-- 여기에 작성 -->

---

## 상태 설계

서버에서 받아온 데이터를 어디에 두었고, 왜 그렇게 했는지 적어주세요.

<!-- 여기에 작성 -->

---

## 구조

프론트엔드와 백엔드의 계층을 어떤 기준으로 나눴는지 적어주세요.

<!-- 여기에 작성 -->

---

## 데이터 적재

`seed/seed.json`을 어떤 방식으로 적재했는지, 어떤 스키마에 담았는지 적어주세요.

<!-- 여기에 작성 -->

---

## 개선한 점 · 남은 과제 (선택)

디자인에 없어서 임의로 정한 것, 개선한 UI/UX, 시간이 더 있었다면 하고 싶었던 것이
있으면 적어주세요. **없다면 이 절은 비워두거나 지우셔도 됩니다.**

<!-- 여기에 작성 -->
````

---

# 2. 제출용 README 교체안

# 메디큐브톡 커뮤니티

APR Product Engineer 인턴 과제의 메디큐브톡 모바일 커뮤니티를 프론트엔드와 백엔드까지 구현한 저장소입니다. 제공된 Spring Boot/JDBC/PostgreSQL 및 Next.js 골격 위에 커뮤니티 도메인, 영속성, API, 서버 상태 관리와 375px 모바일 UI를 완성했습니다.

## 실행

Docker와 Docker Compose가 필요합니다. 저장소 루트에서 다음 한 줄로 PostgreSQL, 백엔드, 프론트엔드를 실행합니다.

```bash
docker compose up
```

| 대상 | 기본 주소 |
| --- | --- |
| 프론트엔드 | http://localhost:3000 |
| 백엔드 API | http://localhost:8080/api |
| 기동 확인 | http://localhost:8080/health |
| 개발용 요청자 전환 | http://localhost:3000/dev-user |

포트가 겹치면 다음처럼 변경할 수 있습니다.

```bash
FRONTEND_PORT=13000 BACKEND_PORT=18080 DB_PORT=25432 docker compose up --build
```

`BACKEND_PORT`는 프론트엔드의 `NEXT_PUBLIC_API_BASE_URL` 빌드 인자에 반영되므로, 값을 바꿀 때는 `--build`로 클라이언트 번들을 다시 만들어야 합니다. DB 데이터는 `dbdata` named volume에 유지되며, 로컬 데이터를 완전히 초기화할 때만 `docker compose down -v`를 사용합니다.

## 구현 범위

APR이 제공한 범위는 Java 17/Spring Boot 3.5 프로젝트와 Spring Web/JDBC·PostgreSQL 연결, `GET /health`, CORS, Compose, `seed/seed.json`, Next.js 15 App Router 골격, `/dev-user`, 아바타와 Pretendard 자산입니다.

그 위에 Flyway 스키마, 멱등한 시드 적재와 readiness gate, 게시글·댓글·좋아요 영속성, REST API, 소프트 삭제와 소유권 검증, keyset 페이지네이션, TanStack Query 캐시, 게시글 CRUD·댓글·좋아요, 무한 스크롤, 검색·정렬과 모바일 UI를 구현했습니다. 댓글 수정과 로그인은 과제 범위대로 구현하지 않았습니다.

## 기술 선택

### Backend persistence — Spring `JdbcClient`

스타터에 이미 포함된 Spring JDBC 위에서 `JdbcClient`와 명시적 SQL repository를 사용했습니다. 소프트 삭제 조건, 파생 `commentCount`, 요청자별 `isLiked`, 정렬별 keyset 조건처럼 조회 의미가 중요한 작은 도메인이라 SQL을 코드에서 직접 확인하기 쉽다는 점을 택했습니다. 대신 ORM보다 SQL과 row mapping을 직접 관리해야 하는 비용을 수용했습니다. 중심 구현은 `PostRepository`, `CommentRepository`, `PostLikeRepository`입니다.

### Schema migration — Flyway

`backend/src/main/resources/db/migration/V1__create_community_schema.sql`이 빈 데이터베이스에서 스키마를 재현합니다. 스키마 migration과 과제 데이터 적재를 분리해 구조 변경 이력과 시드 멱등성을 각각 관리합니다.

### Frontend server state — TanStack Query

게시글 목록·상세, 댓글, 좋아요는 API가 원본인 server state이므로 `@tanstack/react-query`의 query/mutation과 infinite query로 관리합니다. 의존성과 `QueryClientProvider` 경계가 하나 늘지만, 페이지 누적·취소·캐시 동기화·재조회 규칙을 별도의 effect 기반 캐시로 다시 만들지 않아도 됩니다. 브라우저 세션당 하나의 `QueryClient`는 `frontend/app/providers.tsx`에서 생성합니다.

### Styling — CSS Modules

Pretendard, reset과 공통 색상 token은 `frontend/app/globals.css`에 두고 화면별 스타일은 CSS Modules로 격리했습니다. 375px 과제 화면의 간격·타이포그래피·상태 스타일을 직접 표현하기에 충분해 별도 UI framework는 추가하지 않았습니다.

## 상태 설계

TanStack Query가 게시글 목록·상세, 댓글 페이지와 좋아요 상태를 보관합니다. `frontend/lib/query/keys.ts`의 query key에는 page size, `latest`/`likes` 정렬, 정규화된 검색어가 포함되므로 서로 다른 목록은 별도 캐시와 커서 체인을 가집니다. 커서는 infinite query의 `pageParam`으로만 전달합니다.

폼 입력, 검색창 열림 여부, 메뉴·확인 dialog, 댓글의 “더보기” 여부처럼 서버의 원본이 아닌 짧은 상호작용 상태는 각 feature component의 local state에 둡니다. 검색 입력은 즉시 local state에 반영하고, 300ms 뒤 확정된 검색어만 query key와 API 요청에 반영합니다.

## 구조

백엔드는 다음 책임으로 나눴습니다.

- Controller: `PostController`, `CommentController`가 HTTP parameter/body와 응답 경계를 담당합니다.
- Service: `PostReadService`, `PostMutationService`, `CommentMutationService`가 검증, 소유권, transaction과 도메인 규칙을 담당합니다.
- Repository: `PostRepository`, `CommentRepository`, `PostLikeRepository`가 SQL과 row mapping을 담당합니다.
- Common: `RequesterResolver`, `MutationValidator`, cursor codec과 공통 error handler가 횡단 규칙을 한곳에 둡니다.

프론트엔드는 App Router의 얇은 route page 아래 `features/post-list`, `features/post-detail`, `features/post-form`으로 사용자 흐름을 나누고, API client·query key/cache·공통 hook은 `frontend/lib`에 둡니다. UI가 타인의 수정·삭제 버튼을 숨기더라도 보안 경계는 백엔드입니다. 실제 mutation은 service가 다시 소유권을 확인하고 다른 사용자의 요청을 거절합니다.

## 데이터 모델

### Soft delete

`posts.is_deleted`와 `comments.is_deleted`로 삭제 상태를 남기며 정상 조회는 삭제된 행을 제외합니다. 게시글을 삭제해도 그 자식 댓글 행이나 좋아요 행을 강제로 바꾸지 않습니다. 대신 부모 게시글이 unreadable이면 댓글 조회·생성·삭제 경로에서도 접근할 수 없게 해 제품 API에서는 함께 사라진 것처럼 동작합니다. 삭제된 댓글은 `commentCount`에도 포함하지 않습니다.

### Likes

표시되는 좋아요 수는 다음처럼 계산합니다.

```text
likeCount = posts.base_like_count + COUNT(post_likes rows)
```

시드는 집계 `likeCount`만 제공하고 과거 사용자별 기록은 제공하지 않습니다. 따라서 시드 값은 `base_like_count`로 보존하고, 애플리케이션에서 누른 좋아요만 `post_likes(post_id, user_id)`에 저장합니다. 이 두 컬럼의 composite primary key가 같은 요청자의 중복 좋아요 행을 막으며, `isLiked`는 요청자 행의 존재 여부로 계산합니다.

### IDs

`posts.id`와 `comments.id`는 명시적 시드 ID도 받을 수 있는 PostgreSQL identity column입니다. 시드 ID를 원본 그대로 넣은 뒤 `IdentitySequenceRepository.synchronizeWithSeededRows()`가 sequence를 최대 ID 다음으로 맞춰 새 게시글·댓글의 ID 충돌을 방지합니다.

## 데이터 적재와 readiness

초기화 흐름은 다음과 같습니다.

```text
Flyway schema migration
  → SeedInitializer(ApplicationRunner)
  → seed/seed.json 읽기
  → 원본 ID로 posts/comments 삽입
  → deletedPostIds/deletedCommentIds 반영
  → identity sequence 동기화
  → seed_history에 assignment-v1 기록
  → transaction commit
```

`SeedInitializer.run()` 전체가 하나의 transaction이므로 중간 실패가 부분 데이터나 완료 marker로 commit되지 않습니다. 재기동 때 `SeedHistoryRepository.hasImported("assignment-v1")`가 이미 완료된 시드를 건너뜁니다. `SeedReadiness.markReadyAfterCommit()`는 commit 이후에만 ready 상태를 열고, `HealthController`는 그 전까지 503을 반환합니다. 따라서 Compose가 백엔드를 healthy로 판단하는 시점에는 시드 API 데이터도 준비되어 있습니다.

## 페이지네이션과 무한 스크롤

최신순 게시글과 댓글은 `(created_at DESC, id DESC)`로 정렬하고 마지막 행의 두 값을 담은 URL-safe Base64 cursor로 다음 범위를 찾습니다. 같은 시각의 행도 `id`로 순서가 결정되며, 프론트엔드는 `nextCursor` 내부를 해석하지 않고 다음 요청에 그대로 돌려줍니다. offset과 달리 앞쪽에 새 행이 추가되어도 이미 받은 위치를 숫자 offset으로 다시 세지 않습니다.

`PostReadService`는 `limit + 1`행으로 다음 페이지 존재 여부를 판정하고, repository는 composite row-value 조건을 index bound로 사용합니다. `IntersectionObserver` 기반 `useInfiniteScrollTrigger`가 200px 아래 여유에서 다음 페이지를 요청합니다.

현재 제품 값은 다음과 같습니다.

| 흐름 | 값 | 동작 |
| --- | ---: | --- |
| 게시글 목록 | `POST_LIST_PAGE_SIZE = 15` | 최초와 후속 페이지 모두 최대 15개 |
| 댓글 최초 | `INITIAL_COMMENT_COUNT = 2` | 과제 요구대로 최초 정확히 최대 2개 |
| 댓글 “더보기” 이후 | `COMMENT_PAGE_SIZE = 30` | cursor별 최대 30개씩 연속 로드 |
| scroll 선조회 | `rootMargin = 200px` | sentinel 도달 전에 다음 요청 시작 |
| 검색 입력 | `SEARCH_DEBOUNCE_MS = 300` | 입력이 멈춘 뒤 검색어 확정 |

댓글 query는 화면을 떠날 때 제거되므로 같은 세션에 상세로 다시 들어와도 먼저 `limit=2`만 요청하며, “더보기” 전에는 continuation sentinel을 활성화하지 않습니다.

## 측정으로 정한 제품 상수

페이지 크기와 interaction timing은 375×778 Chromium, 고정 4G(지연 200ms, 다운로드 1.6Mbps), clean seed 39 readable posts 조건에서 후보를 한 번에 하나씩 바꿔 비교했습니다.

- 게시글 `10/15/20`: 전체 순회 요청은 각각 `4/3/2`회였습니다. 15는 10보다 요청 경계를 하나 줄이면서, 최초 화면 아래 항목이 8개로 20의 13개보다 적었습니다. 20이 15보다 아낀 것은 요청 1회와 약 424B의 envelope/header overhead였으므로 초기 선조회를 더 늘리지 않았습니다.
- 댓글 continuation `10/20/30`: readable 댓글 1,411개인 seed post #2에서 각각 `141/71/47`회였습니다. 20→30은 continuation 요청을 약 34% 줄였고 p95는 248ms로 유지됐으며 30개 append에서 관찰 가능한 rendering pause가 없었습니다. 같은 최종 댓글을 받되 요청/envelope/header 반복만 줄인 결과입니다.
- `rootMargin 100/200/400px`: 100px은 loading UI가 노출됐고, 400px은 사용자 scroll 전 page 2를 요청했습니다. 두 현상이 없었던 200px을 유지했습니다.
- 검색 debounce `200/300/500ms`: 300ms는 사람 입력 중 250ms pause를 흡수하면서 500ms보다 최종 결과에 약 200ms 빨리 도달해 유지했습니다.

선택값을 적용한 [PR #23](https://github.com/ppsssj/apr-product-engineer-assignment/pull/23)에서는 게시글 `15/15/9` 3회로 39개, 댓글은 최초 2개 뒤 30개 단위로 1,411개를 끝까지 순회해 중복·누락과 종료 후 추가 요청이 없음을 다시 확인했습니다.

## 검색·정렬 확장 (`PRODUCT_DECISION`)

과제 계약의 기본값은 계속 최신순입니다. 그 위에 탐색 편의를 위해 `likes` 정렬과 제목·본문 검색을 선택 기능으로 추가했습니다. 필터링과 정렬은 이미 받은 client page만 대상으로 하지 않고 `PostController` → `PostReadService` → `PostRepository`에서 server-side로 수행합니다.

검색은 사용자 입력의 `%`, `_`, `\\`를 escape한 뒤 제목 또는 본문에 case-insensitive substring match를 적용합니다. 최신순 cursor와 좋아요순 `(likeCount, createdAt, id)` cursor는 형식을 분리해 다른 정렬의 cursor를 섞으면 거절합니다. 프론트 query key에도 sort와 검색어가 들어가므로 각 조합은 첫 페이지부터 독립적으로 시작합니다.

## 캐시 동기화 원칙

`frontend/lib/query/postListCache.ts`는 mutation 결과를 정확히 알 수 있을 때 cached copy를 직접 갱신하고, membership이나 ordering이 달라질 수 있으면 해당 목록을 invalidate해 다시 조회합니다.

- 최신순 목록의 loaded post 좋아요 수와 댓글 수는 직접 반영합니다.
- 좋아요 toggle 뒤 likes-sort 목록은 순서가 바뀔 수 있어 invalidate합니다.
- 게시글 수정 뒤 검색 목록은 검색 포함 여부가 바뀔 수 있어 invalidate합니다.
- 생성 글의 검색 포함 여부나 likes 순위도 client가 단정하지 않고 재조회합니다.

모든 mutation은 server-confirmed 방식입니다. 서버 성공 전 화면을 낙관적으로 바꾸지 않아 실패 때 가짜 성공 상태를 되돌리는 복잡성을 피했습니다.

## 검증과 소유권

프론트 validation은 잘못된 제출을 미리 막는 UX이고, 백엔드 `MutationValidator`가 제목 필수·20 Unicode code point 이하·본문/댓글 nonblank 규칙을 다시 검사해 데이터 보호를 책임집니다.

`RequesterResolver`의 우선순위는 `x-user-id` header → 같은 이름의 cookie → `apr_tester`입니다. 별도 user table이나 login 없이 `author == requester`를 소유권으로 판단합니다. `PostMutationService`와 `CommentMutationService`는 대상 row를 lock하고 readable 여부와 소유권을 확인하므로 UI affordance를 우회한 다른 사용자의 수정·삭제도 거절됩니다.

## UI/UX 선택과 Figma 차이

기본 viewport, typography, 색상, 작성·상세·삭제 확인 흐름은 제공 디자인을 따르되 다음 항목은 의도적인 `PRODUCT_DECISION`입니다.

- 목록은 작성자 우선 배열 대신 제목 → 본문 preview → 작성자/반응/시간의 content-first hierarchy로 바꾸고, 98px 시안보다 숨이 트이도록 108px row rhythm을 사용했습니다.
- 공식 화면에 없던 최신순·좋아요순·검색 toolbar를 44px 한 줄로 추가했습니다. app title은 scroll되고 discovery toolbar만 sticky로 남겨 화면 상단 점유를 줄였습니다.
- 검색 상태는 별도 pill이나 modal 없이 같은 toolbar가 `← · 검색 icon · input`의 flat mode로 전환됩니다. Escape/뒤로 닫기, focus 반환과 16px input을 포함했습니다.
- 본인 글의 수정·삭제 두 명령은 전체 폭 bottom sheet 대신 `⋮` 아래 144px non-modal popover에 배치했습니다. action locality와 content context를 유지하는 대신 공식 sheet와의 시각적 동일성은 포기했습니다. 삭제 자체는 중앙 confirmation dialog와 scrim을 거쳐 destructive action을 분리합니다.
- 디자인에 없는 loading/error/retry, keyboard focus, reduced-motion 상태를 추가했습니다.

세부 근거와 공식 디자인/제품 결정의 경계는 `docs/UI_SPEC.md` 및 `design/design-retouch/readme.md`에 기록했습니다. 공식 Figma 원본과의 최종 fresh pixel-level 비교 항목(V-081)은 실행되지 않았으므로 pixel-perfect 일치를 주장하지 않습니다.

## 테스트와 검증

백엔드 JUnit/Mockito tests는 requester 우선순위, Unicode validation, cursor 정밀도와 malformed input, seed parsing/readiness, 게시글·댓글 mutation 소유권, soft-delete/not-found, 좋아요와 검색·정렬 계약을 다룹니다. 관련 test는 `backend/src/test/java/com/apr/community` 아래에 있습니다.

`docs/VERIFICATION.md`에는 실제 PostgreSQL/Compose 및 Chromium으로 다음을 검증한 근거가 있습니다.

- fresh volume 전체 기동, `/health` readiness와 alternate ports
- 원본 ID·삭제 상태·sequence·`seed_history` 멱등성
- 39개 readable post와 최다 1,411개 댓글의 cursor 완전 순회, tie 순서, 중복·누락·종료 조건
- requester header/cookie 전환, 소유권, 좋아요 격리, post CRUD와 comment create/delete
- soft-deleted row의 물리적 보존과 API/cache에서의 제거
- latest/likes/search 조합, stale 검색 요청 취소와 mutation 후 membership/order 갱신
- 375×778의 필수 화면, fixed control, horizontal overflow와 loading/error 흐름

문서만 보고 통과로 간주하지 않고 실행 증거가 있는 항목만 `PASS`/`VERIFIED`로 기록했습니다. 성능 데이터셋과 SQL plan 측정은 `docs/PERFORMANCE.md`에 분리했습니다.

## 트레이드오프와 남은 제한

- `JdbcClient`는 query를 명시적으로 통제하는 대신 ORM보다 SQL과 mapping 코드가 많습니다.
- keyset cursor는 concurrent insert에 강하고 deep page를 index seek로 찾지만 page number처럼 사람이 읽고 임의 페이지로 이동하기는 어렵습니다.
- likes-sort는 mutable `likeCount`를 순위에 사용합니다. 요청 중 다른 사용자의 좋아요로 행이 이동하면 페이지 사이 중복 가능성이 있어 UI에서 같은 post ID의 첫 항목만 렌더링합니다. assignment scope에서 snapshot/versioned ranking은 추가하지 않았습니다.
- 네트워크 pagination은 한 번에 1,411개 댓글을 받지 않게 하지만 사용자가 끝까지 순회하면 DOM node도 결국 누적됩니다. 이 데이터 규모와 모바일 과제 범위에서는 virtualization을 추가하지 않았습니다.
- 좋아요는 server-confirmed라 실패 일관성은 단순하지만 optimistic UI보다 응답 체감이 늦을 수 있습니다.
- clean start의 row-by-row seed insert는 측정상 약 0.9초의 일회성 비용이었습니다. batching의 복잡도를 추가할 만큼 사용자 병목은 아니어서 유지했습니다.

더 자세한 결정 배경은 `docs/ARCHITECTURE.md`, 요구사항 traceability는 `docs/REQUIREMENTS.md`, 검증 절차와 결과는 `docs/VERIFICATION.md`를 참고할 수 있습니다.

---

# 3. 교체 전 검토 체크리스트

- [x] 과제 README 요구사항(기술 선택·상태 설계·구조·데이터 적재·테스트)을 모두 설명했다.
- [x] 설명을 실제 file/class/function에 연결했다.
- [x] APR 제공 starter와 추가 구현을 구분했다.
- [x] 의도적인 UI/UX 개선 및 공식 디자인과의 차이를 기록했다.
- [x] 선택하지 않은 대안과 현재 제한을 trade-off로 기록했다.
- [x] 기본 `docker compose up`과 기본 endpoint를 상단에 안내했다.
- [x] alternate ports와 `BACKEND_PORT` 변경 시 `--build`가 필요한 이유를 설명했다.
- [x] Flyway가 없는 것처럼 보이는 기존 starter 문구 등 현재와 맞지 않는 설명을 교체안에서 제거했다.
- [x] 성능 수치는 선행 실측과 repository evidence로 확인된 범위만 사용하고, page size가 원본 data 자체를 줄인다고 표현하지 않았다.
- [x] 제출용 교체안에 빈 절, TODO, template comment를 남기지 않았다.
