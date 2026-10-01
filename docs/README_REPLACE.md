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

이 과제의 읽기 규칙은 단순한 row CRUD보다 “삭제되지 않은 행만 조회”, “댓글 수를 파생”, “시드 좋아요와 요청자 좋아요를 합산”, “정렬마다 다른 keyset 조건 적용”에 가깝습니다. 따라서 ORM mapping을 새로 도입할지, 스타터의 Spring JDBC 기반에서 SQL을 명시할지를 결정해야 했습니다.

도메인이 게시글·댓글·좋아요로 작고 query 의미를 리뷰에서 직접 확인하는 편이 중요하다고 판단해 `JdbcClient`와 SQL repository를 선택했습니다. Spring Framework가 설명하는 `JdbcClient`는 Spring JDBC의 named/positional parameter query와 update를 하나의 fluent API로 제공합니다.[^spring-jdbc-client] 이 capability가 현재 `PostRepository`, `CommentRepository`, `PostLikeRepository`의 작은 명시적 query 집합에 맞았습니다. 대신 JPA보다 SQL, row mapping과 DB 종속 동작을 직접 관리하는 비용을 수용했습니다.

### Schema migration — Flyway

평가자는 빈 PostgreSQL에서 시작할 수 있고, schema 변경과 과제용 `seed.json` 적재는 수명주기가 다릅니다. 애플리케이션 코드에서 테이블을 즉석 생성하거나 schema와 seed를 한 초기화 절차에 섞으면, 구조 변경 이력과 “과제 데이터가 이미 들어갔는가”를 구분하기 어렵습니다.

그래서 schema는 `V1__create_community_schema.sql`, seed는 `SeedInitializer`가 맡도록 분리했습니다. Spring Boot는 시작 시 Flyway migration을 실행하며 PostgreSQL에는 `flyway-database-postgresql` 모듈을 사용하도록 문서화하고 있고, 현재 `backend/build.gradle`도 그 구성을 따릅니다.[^spring-flyway] Flyway의 `flyway_schema_history`는 적용 migration, 성공 여부와 checksum을 추적합니다.[^flyway-history] 이 선택은 빈 DB 재현성과 변경 이력을 얻는 대신, 적용된 versioned migration을 임의 수정하지 않고 다음 migration으로 전진해야 하는 규율을 요구합니다. 과제 seed의 멱등성은 별도의 `seed_history`가 담당합니다.

### Frontend server state — TanStack Query

같은 게시글이 목록과 상세에 반복해서 나타나고, 좋아요·댓글·수정·삭제 뒤에는 여러 화면의 copy가 함께 바뀌어야 합니다. 각 component의 `useState`/`useEffect`로 page 누적, loading, 취소와 mutation 후 동기화를 따로 구현할지, API 데이터를 전담하는 cache 계층을 둘지가 핵심 질문이었습니다.

이 데이터는 브라우저가 소유하는 전역 상태가 아니라 서버가 원본인 상태이므로 TanStack Query를 선택했습니다. 공식 문서의 `useInfiniteQuery`는 `pages`, `pageParam`, `getNextPageParam`과 `fetchNextPage`로 누적 목록을 모델링하고,[^tanstack-infinite] query key는 query data를 고유하게 식별하는 cache 기준입니다.[^tanstack-query-keys] 또한 mutation 성공 후 관련 query invalidation과 query function의 `AbortSignal` 소비를 지원합니다.[^tanstack-invalidation][^tanstack-cancellation] 이는 이 과제의 무한 스크롤, sort/search별 cache, stale 검색 취소에 직접 대응합니다. 대신 production dependency와 `QueryClientProvider`가 추가되고, 어떤 cache를 직접 갱신하고 invalidate할지는 여전히 애플리케이션이 설계해야 합니다.

### Styling — CSS Modules

375px 한 화면에 맞춘 간격·타이포그래피·pressed/focus 상태를 세밀하게 표현해야 했지만, 과제 규모에서 utility framework나 UI kit를 새로 도입하면 design token보다 설정과 추상화가 더 커질 수 있었습니다. 그래서 Pretendard, reset과 공통 token만 `globals.css`에 두고 나머지는 component별 CSS Modules로 작성했습니다. Next.js는 `.module.css`의 class name을 고유하게 생성해 local scope와 충돌 방지를 제공합니다.[^next-css-modules] 추가 framework 없이 스타일의 근거가 각 feature 옆에 남는 대신 반복되는 CSS 선언을 일부 감수했습니다.

## 상태 설계

상태를 한 저장소에 모두 넣으면 서버에서 다시 얻어야 하는 데이터와, 화면을 닫으면 버려도 되는 상호작용 상태의 수명주기가 섞입니다. 이를 피하기 위해 “누가 source of truth인가”로 경계를 나눴습니다.

- 서버가 원본인 게시글 목록·상세, 댓글 page와 좋아요는 TanStack Query에 둡니다. `frontend/lib/query/keys.ts`는 page size, `latest`/`likes`, 정규화된 검색어를 key에 포함해 서로 다른 결과와 cursor chain이 섞이지 않게 합니다. cursor 자체는 infinite query의 `pageParam`입니다.
- 폼 입력, 검색창 열림, 메뉴·확인 dialog와 댓글 “더보기”처럼 화면 단위로 끝나는 상태는 local state에 둡니다. 검색 input은 즉시 반응하되 확정 query만 300ms debounce해 API state와 타이핑 state를 분리합니다.

이 경계 덕분에 local UI를 위해 별도 global store를 추가하지 않았습니다. 반대로 server cache에는 mutation 간 일관성 규칙이 필요하며, 그 판단은 `frontend/lib/query/postListCache.ts`에 모았습니다.

## 구조

HTTP 처리, 소유권/transaction, SQL이 한 class에 섞이면 같은 규칙을 endpoint마다 반복하고 backend authorization을 UI 상태에 의존하기 쉽습니다. 그래서 변경 이유가 다른 경계를 기준으로 세 계층을 두었습니다.

- Controller: `PostController`, `CommentController`가 HTTP parameter/body와 응답 경계를 담당합니다.
- Service: `PostReadService`, `PostMutationService`, `CommentMutationService`가 검증, 소유권, transaction과 도메인 규칙을 담당합니다.
- Repository: `PostRepository`, `CommentRepository`, `PostLikeRepository`가 SQL과 row mapping을 담당합니다.
- Common: `RequesterResolver`, `MutationValidator`, cursor codec과 공통 error handler가 횡단 규칙을 한곳에 둡니다.

프론트엔드도 route와 기능 구현이 함께 비대해지지 않도록 App Router page는 route parameter를 feature에 연결하는 얇은 경계로 두고, `features/post-list`, `features/post-detail`, `features/post-form`으로 사용자 흐름을 나눴습니다. API client·query cache·공통 hook은 `frontend/lib`에 둡니다. 이 분리는 소규모 과제에 계층 수를 추가하지만, UI가 숨긴 버튼은 보안 경계가 아니며 service authorization이 최종 판단이라는 점을 분명하게 합니다.

## 데이터 모델

### Soft delete

과제는 삭제된 resource를 화면과 API에서는 없어진 것처럼 보이게 하되 DB row는 물리적으로 보존하라고 요구합니다. 별도 archive table로 옮기거나 `deleted_at`을 둘 수도 있었지만, 복구 시각이나 감사 시각을 쓰는 요구가 없으므로 현재 상태만 나타내는 `posts.is_deleted`와 `comments.is_deleted`를 선택했습니다.

모든 normal read와 count에 이 조건을 명시해야 한다는 비용이 생기지만, 삭제 정책이 SQL에서 확인 가능하고 seed의 deleted ID도 같은 model로 표현됩니다. 게시글 삭제 때 child row를 다시 쓰지 않고, unreadable parent의 댓글 조회·생성·삭제를 차단해 제품 API에서 함께 사라진 것처럼 만듭니다. 삭제 댓글은 `commentCount`에서도 제외합니다.

### Likes

시드는 좋아요 총수만 주고 누가 눌렀는지는 주지 않지만, API의 `isLiked`는 현재 요청자 기준이어야 합니다. 총수를 mutable counter 하나로 두면 요청자 관계를 잃고, 과거 사용자 행을 만들어 채우면 존재하지 않는 데이터를 발명하게 됩니다. 그래서 알려진 두 사실을 분리했습니다.

```text
likeCount = posts.base_like_count + COUNT(post_likes rows)
```

시드 집계는 `base_like_count`, 애플리케이션에서 생성된 관계만 `post_likes(post_id, user_id)`에 저장합니다. composite primary key가 같은 요청자의 중복 행을 막고 `isLiked`는 그 행의 존재로 계산합니다. 이 model은 입력 사실을 보존하지만, 표시 count마다 관계 행 aggregate가 필요하다는 대가가 있습니다. 제공 데이터 규모에서는 denormalized counter의 동기화 위험보다 이 명확성을 택했습니다.

### IDs

시드 ID는 그대로 보존해야 하지만 새 row의 ID는 자동 생성되어야 합니다. 두 경로를 한 column에서 지원하기 위해 `GENERATED BY DEFAULT AS IDENTITY`를 선택했습니다. PostgreSQL에서 이 형태는 명시값이 있으면 그 값을 우선하고, 생략하면 implicit sequence 값을 사용합니다.[^postgres-identity] 명시적 seed insert는 sequence를 자동으로 전진시키지 않으므로 `IdentitySequenceRepository.synchronizeWithSeededRows()`가 최대 ID에 맞춥니다. 별도 ID allocator를 만들지 않는 대신 seed 이후 sequence sync가 필수입니다.

## 데이터 적재와 readiness

첫 실행에는 2,118개 원본 row가 모두 들어가야 하고, 재기동은 중복되면 안 되며, 실패한 적재를 완료로 기록해서도 안 됩니다. 또한 Compose healthcheck가 seed commit보다 먼저 성공하면 평가자가 잠시 빈 API를 볼 수 있습니다. 이 네 조건을 한 transaction과 commit 이후 readiness gate로 묶었습니다.

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

`SeedInitializer.run()` 전체가 하나의 transaction이므로 posts/comments와 `assignment-v1` marker는 함께 commit되거나 rollback됩니다. sequence sync는 marker 기록 전에 실행되며, 실패 후 재시작하더라도 marker가 없으므로 적재를 다시 시도합니다. `SeedHistoryRepository`는 과제 seed의 멱등성을 판단하고, Flyway의 schema history와 역할이 다릅니다. `SeedReadiness.markReadyAfterCommit()`는 transaction commit 뒤에만 ready를 열며 그 전의 `/health`는 503입니다. 따라서 Compose가 backend를 healthy로 판단할 때 API 데이터도 준비되어 있습니다. row별 insert는 단순성과 실패 위치의 명확성을 택한 결과이며, clean start 약 0.9초의 일회성 비용을 수용했습니다.

## 페이지네이션과 무한 스크롤

목록을 한 번에 전부 가져오면 안 되고, 댓글은 한 게시글에 1,411개까지 치우쳐 있습니다. `OFFSET`은 구현은 단순하지만 앞쪽에 새 row가 생기는 동안 page 경계가 흔들릴 수 있고, PostgreSQL도 큰 `OFFSET`의 skipped row를 server가 여전히 계산한다고 설명합니다.[^postgres-limit] 그래서 최신순 게시글과 댓글은 마지막 `(created_at, id)` 뒤를 찾는 keyset pagination을 선택했습니다.

정렬은 `(created_at DESC, id DESC)`로 고정해 timestamp tie도 deterministic하게 만들고, cursor는 두 값을 URL-safe Base64로 감춰 client가 내부 형식에 의존하지 않게 했습니다. repository의 `(created_at, id) < (?, ?)`는 PostgreSQL이 문서화한 left-to-right row comparison semantics를 사용합니다.[^postgres-row-comparison] 실제 `EXPLAIN`에서 이 predicate가 composite index bound가 되는 것은 `docs/PERFORMANCE.md`로 별도 확인했습니다. `PostReadService`는 `limit + 1`행으로 다음 page를 판정합니다. 이 방식은 deep page와 concurrent insert에 적합하지만 cursor가 page number처럼 읽기 쉽거나 임의 page 이동에 적합하지는 않습니다.

브라우저에서는 별도 scroll listener 계산 대신 `IntersectionObserver` sentinel과 `useInfiniteQuery`를 연결했습니다. `useInfiniteScrollTrigger`는 진행 중 요청을 끄고 200px 여유에서 다시 arm해 한 cycle당 한 요청만 보냅니다.

현재 제품 값은 다음과 같습니다.

| 흐름 | 값 | 동작 |
| --- | ---: | --- |
| 게시글 목록 | `POST_LIST_PAGE_SIZE = 15` | 최초와 후속 페이지 모두 최대 15개 |
| 댓글 최초 | `INITIAL_COMMENT_COUNT = 2` | 과제 요구대로 최초 정확히 최대 2개 |
| 댓글 “더보기” 이후 | `COMMENT_PAGE_SIZE = 30` | cursor별 최대 30개씩 연속 로드 |
| scroll 선조회 | `rootMargin = 200px` | sentinel 도달 전에 다음 요청 시작 |
| 검색 입력 | `SEARCH_DEBOUNCE_MS = 300` | 입력이 멈춘 뒤 검색어 확정 |

댓글의 최초 2개는 page-size 최적화 대상이 아니라 명시적 제품 계약입니다. 그래서 detail 재방문 때 query를 제거해 다시 `limit=2`부터 시작하고, 사용자가 “더보기”를 누르기 전에는 continuation sentinel을 활성화하지 않습니다. 그 이후 page만 30개씩 받습니다.

## 측정으로 정한 제품 상수

페이지 크기와 interaction timing은 375×778 Chromium, 고정 4G(지연 200ms, 다운로드 1.6Mbps), clean seed 39 readable posts 조건에서 후보를 한 번에 하나씩 바꿔 비교했습니다.

- 게시글 `10/15/20`: 전체 순회 요청은 각각 `4/3/2`회였습니다. 15는 10보다 요청 경계를 하나 줄이면서, 최초 화면 아래 항목이 8개로 20의 13개보다 적었습니다. 20이 15보다 아낀 것은 요청 1회와 약 424B의 envelope/header overhead였으므로 초기 선조회를 더 늘리지 않았습니다.
- 댓글 continuation `10/20/30`: readable 댓글 1,411개인 seed post #2에서 각각 `141/71/47`회였습니다. 20→30은 continuation 요청을 약 34% 줄였고 p95는 248ms로 유지됐으며 30개 append에서 관찰 가능한 rendering pause가 없었습니다. 같은 최종 댓글을 받되 요청/envelope/header 반복만 줄인 결과입니다.
- `rootMargin 100/200/400px`: 100px은 loading UI가 노출됐고, 400px은 사용자 scroll 전 page 2를 요청했습니다. 두 현상이 없었던 200px을 유지했습니다.
- 검색 debounce `200/300/500ms`: 300ms는 사람 입력 중 250ms pause를 흡수하면서 500ms보다 최종 결과에 약 200ms 빨리 도달해 유지했습니다.

선택값을 적용한 [PR #23](https://github.com/ppsssj/apr-product-engineer-assignment/pull/23)에서는 게시글 `15/15/9` 3회로 39개, 댓글은 최초 2개 뒤 30개 단위로 1,411개를 끝까지 순회해 중복·누락과 종료 후 추가 요청이 없음을 다시 확인했습니다.

## 검색·정렬 확장 (`PRODUCT_DECISION`)

39개 목록에서도 특정 대화를 다시 찾거나 반응이 큰 글을 보는 탐색 경로가 없었습니다. 다만 과제 계약은 최신순이므로 이를 바꾸지 않고, `likes`와 제목·본문 검색을 선택 기능으로 추가했습니다. 이미 받은 client page만 필터링하면 “검색 결과 없음”이 전체 데이터의 결론처럼 보이는 오류가 생기므로 `PostController` → `PostReadService` → `PostRepository`에서 server-side로 처리합니다.

검색은 `%`, `_`, `\\`를 literal로 escape한 뒤 제목 또는 본문에 case-insensitive substring match를 적용합니다. 최신순 cursor와 좋아요순 `(likeCount, createdAt, id)` cursor는 형식을 분리해 다른 ordering에 재사용하면 거절합니다. query key에도 sort와 검색어를 포함해 각 조합이 첫 page부터 독립적으로 시작합니다. likes ordering은 탐색 가치를 얻는 대신 count가 concurrent mutation으로 움직일 수 있어 snapshot ranking까지는 보장하지 않습니다.

## 캐시 동기화 원칙

mutation마다 모든 목록을 무조건 refetch하면 정확하지만 network 낭비와 화면 깜빡임이 생기고, 반대로 모든 결과를 client에서 직접 고치면 검색 membership이나 likes ordering을 잘못 추론할 수 있습니다. 그래서 “서버 응답만으로 결과를 정확히 증명할 수 있는가”를 기준으로 둘을 나눴습니다. TanStack Query의 mutation 후 invalidation은 관련 query를 stale 처리하고 재조회하는 공식 mechanism입니다.[^tanstack-invalidation]

- 최신순 목록의 loaded post 좋아요 수와 댓글 수는 직접 반영합니다.
- 좋아요 toggle 뒤 likes-sort 목록은 순서가 바뀔 수 있어 invalidate합니다.
- 게시글 수정 뒤 검색 목록은 검색 포함 여부가 바뀔 수 있어 invalidate합니다.
- 생성 글의 검색 포함 여부나 likes 순위도 client가 단정하지 않고 재조회합니다.

이 규칙은 `frontend/lib/query/postListCache.ts`에 모았습니다. 모든 mutation은 server-confirmed 방식으로, 성공 전 cache를 바꾸지 않아 rollback 복잡성과 false success를 피합니다. 그 대가로 optimistic UI보다 좋아요 반응이 한 network round-trip 늦습니다.

## 검증과 소유권

버튼을 숨기거나 submit을 disable하는 것만으로는 직접 API 호출을 막을 수 없습니다. 따라서 frontend validation은 빠른 UX feedback만 담당하고, backend `MutationValidator`가 제목 필수·20 Unicode code point 이하·본문/댓글 nonblank 규칙을 다시 검사합니다.

과제는 login이나 user table 없이 requester header/cookie를 계약으로 줍니다. 그래서 `RequesterResolver`의 우선순위를 `x-user-id` header → 같은 이름의 cookie → `apr_tester`로 한곳에 고정하고 `author == requester`를 소유권으로 사용했습니다. `PostMutationService`와 `CommentMutationService`가 readable target을 lock한 뒤 소유권을 확인하므로 UI affordance를 우회한 mutation도 거절됩니다. 실제 인증 체계로 확장할 수 있는 model은 아니지만, 과제 계약 밖의 user domain을 만들지 않으면서 backend를 authoritative boundary로 유지합니다.

## UI/UX 선택과 Figma 차이

공식 Figma는 핵심 화면과 visual hierarchy를 제공하지만 검색·정렬, loading/error, keyboard와 motion-reduction 상태까지 정의하지는 않습니다. 따라서 기본 viewport, typography, 색상, 작성·상세·삭제 확인 흐름은 유지하고, 비어 있는 제품 결정을 다음 기준으로 채웠습니다.

- 목록의 첫 판단 대상은 작성자보다 글 내용이라고 보고 제목 → 본문 preview → 작성자/반응/시간의 content-first hierarchy를 선택했습니다. 98px retouch 시안은 실제 375×778에서 과밀해 108px rhythm으로 조정했습니다.
- 탐색 기능이 content를 가리지 않도록 최신순·좋아요순·검색을 44px 한 줄에 넣고, app title은 scroll되되 discovery toolbar만 sticky로 남겼습니다. 두 band를 모두 고정하지 않아 세로 공간을 보존했습니다.
- 검색은 새 modal이나 pill을 더하지 않고 같은 toolbar가 `← · 검색 icon · input`의 flat mode로 바뀝니다. Escape/뒤로 닫기, focus 반환과 iOS zoom을 피하는 16px input을 포함합니다.
- 본인 글에는 두 command만 있으므로 전체 폭 bottom sheet 대신 `⋮` 바로 아래 144px non-modal popover를 선택했습니다. action locality와 context를 얻는 대신 공식 sheet와의 시각적 동일성은 포기했습니다. 실제 삭제는 중앙 confirmation dialog와 scrim으로 destructive step을 다시 분리합니다.
- 정적 design에 없는 loading/error/retry, keyboard focus와 reduced-motion은 동작 완결성과 접근성을 위해 추가했습니다.

세부 근거와 공식 디자인/제품 결정의 경계는 `docs/UI_SPEC.md` 및 `design/design-retouch/readme.md`에 기록했습니다. 공식 Figma 원본과의 최종 fresh pixel-level 비교 항목(V-081)은 실행되지 않았으므로 pixel-perfect 일치를 주장하지 않습니다.

## 테스트와 검증

이 구현의 위험은 happy path보다 boundary에 있습니다. 같은 timestamp의 cursor, 삭제 parent의 child 접근, header/cookie 우선순위, 재기동 seed와 mutation 뒤 여러 cache가 대표적입니다. 그래서 단위 test만으로 끝내지 않고 실제 PostgreSQL·Compose·375×778 Chromium을 층별로 사용했습니다.

백엔드 JUnit/Mockito tests는 requester 우선순위, Unicode validation, cursor 정밀도와 malformed input, seed parsing/readiness, mutation 소유권, soft-delete/not-found, 좋아요와 검색·정렬 계약을 다룹니다. 관련 test는 `backend/src/test/java/com/apr/community` 아래에 있습니다.

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

- likes-sort는 mutable `likeCount`를 순위에 사용합니다. 요청 중 다른 사용자의 좋아요로 행이 이동하면 페이지 사이 중복 가능성이 있어 UI에서 같은 post ID의 첫 항목만 렌더링합니다. 과제 범위에서 snapshot/versioned ranking은 추가하지 않았습니다.
- 네트워크 pagination은 한 번에 1,411개 댓글을 받지 않게 하지만 사용자가 끝까지 순회하면 DOM node도 결국 누적됩니다. 이 데이터 규모와 모바일 과제 범위에서는 virtualization을 추가하지 않았습니다.
- 검색은 title/content substring에 한정했습니다. 전문 검색 index, 형태소 분석과 relevance ranking은 39개 seed와 과제 범위에 비해 과도해 추가하지 않았습니다.
- 공식 Figma와의 fresh pixel-level 비교(V-081)는 실행되지 않았습니다. 기록된 design evidence와 375×778 runtime을 검증했지만 pixel-perfect 일치를 주장하지 않습니다.

더 자세한 결정 배경은 `docs/ARCHITECTURE.md`, 요구사항 traceability는 `docs/REQUIREMENTS.md`, 검증 절차와 결과는 `docs/VERIFICATION.md`를 참고할 수 있습니다.

[^spring-jdbc-client]: [Spring Framework — Unified JDBC Query/Update Operations: `JdbcClient`](https://docs.spring.io/spring-framework/reference/data-access/jdbc/core.html#jdbc-JdbcClient)
[^spring-flyway]: [Spring Boot — Database Initialization: Execute Flyway Database Migrations on Startup](https://docs.spring.io/spring-boot/how-to/data-initialization.html#howto.data-initialization.migration-tool.flyway)
[^flyway-history]: [Redgate Flyway — Schema History Table](https://documentation.red-gate.com/flyway/flyway-concepts/migrations/flyway-schema-history-table)
[^tanstack-infinite]: [TanStack Query — Infinite Queries](https://tanstack.com/query/latest/docs/framework/react/guides/infinite-queries)
[^tanstack-query-keys]: [TanStack Query — Query Keys](https://tanstack.com/query/latest/docs/framework/react/guides/query-keys)
[^tanstack-invalidation]: [TanStack Query — Invalidations from Mutations](https://tanstack.com/query/latest/docs/framework/react/guides/invalidations-from-mutations)
[^tanstack-cancellation]: [TanStack Query — Query Cancellation](https://tanstack.com/query/latest/docs/framework/react/guides/query-cancellation)
[^next-css-modules]: [Next.js — CSS Modules](https://nextjs.org/docs/app/getting-started/css#css-modules)
[^postgres-identity]: [PostgreSQL — Identity Columns](https://www.postgresql.org/docs/current/ddl-identity-columns.html)
[^postgres-limit]: [PostgreSQL — `LIMIT` and `OFFSET`](https://www.postgresql.org/docs/current/queries-limit.html)
[^postgres-row-comparison]: [PostgreSQL — Row Constructor Comparison](https://www.postgresql.org/docs/current/functions-comparisons.html#ROW-WISE-COMPARISON)

---

# 3. 교체 전 검토 체크리스트

- [x] 과제 README 요구사항(기술 선택·상태 설계·구조·데이터 적재·테스트)을 모두 설명했다.
- [x] 설명을 실제 file/class/function에 연결했다.
- [x] APR 제공 starter와 추가 구현을 구분했다.
- [x] 주요 기술 선택을 문제·대안·선택·적합성·공식 문서 근거·trade-off 순서로 설명하고, 공식/primary source만 인용했다.
- [x] 의도적인 UI/UX 개선 및 공식 디자인과의 차이를 기록했다.
- [x] 선택하지 않은 대안과 현재 제한을 trade-off로 기록했다.
- [x] 기본 `docker compose up`과 기본 endpoint를 상단에 안내했다.
- [x] alternate ports와 `BACKEND_PORT` 변경 시 `--build`가 필요한 이유를 설명했다.
- [x] Flyway가 없는 것처럼 보이는 기존 starter 문구 등 현재와 맞지 않는 설명을 교체안에서 제거했다.
- [x] 성능 수치는 선행 실측과 repository evidence로 확인된 범위만 사용하고, page size가 원본 data 자체를 줄인다고 표현하지 않았다.
- [x] 제출용 교체안에 빈 절, TODO, template comment를 남기지 않았다.
