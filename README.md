# 메디큐브톡 커뮤니티

APR Product Engineer 인턴 과제의 메디큐브톡 모바일 커뮤니티를 프론트엔드와 백엔드까지 구현한 저장소입니다. 제공된 Spring Boot/JDBC/PostgreSQL 및 Next.js 골격 위에 커뮤니티 도메인, 영속성, API, 서버 상태 관리와 375px 모바일 UI를 완성했습니다.

## 실행

Docker와 Docker Compose(`docker compose` 명령)가 필요합니다. 백엔드와 프론트엔드는 컨테이너 안에서 빌드되므로 JDK나 Node.js를 따로 설치할 필요는 없습니다. 다만 첫 실행에는 base image와 Gradle·npm 의존성을 내려받을 네트워크 연결이 필요하고, 기본값으로는 host의 `3000`, `8080`, `15432` 포트를 사용합니다. 저장소 루트에서 다음 한 줄로 PostgreSQL, 백엔드, 프론트엔드를 실행합니다.

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

## 기술 선택

상태 관리·persistence·스타일링 등 주요 기술을 무엇으로 골랐고 왜 골랐는지, 그 대가로 무엇을 받아들였는지를 정리합니다. 먼저 이 선택들의 기준이 된 과제 해석을 적습니다.

### 과제를 어떻게 해석했는가

이 과제의 출발점은 완성된 커뮤니티를 다듬는 일이 아니라, 실행 골격과 데이터·디자인을 받아 제품의 빈 경계를 스스로 정의하는 일이었습니다. 따라서 화면 개수만 채우기보다 빈 DB가 같은 상태로 재현되는지, 읽기 규칙이 API와 SQL에서 일관적인지, 긴 목록을 모바일 네트워크에서 끝까지 탐색할 수 있는지, mutation 뒤 여러 화면이 같은 사실을 보여 주는지를 먼저 설계 기준으로 삼았습니다.

공식 AGE-R 앱 소개는 나만의 루틴과 전·후 사진 기록뿐 아니라, 메디큐브 톡에서 AGE-R 유저들과 다양한 정보를 확인하고 공유하는 기능을 주요 기능으로 제시합니다.[^age-r-app] 여기에 과제에서 제공된 `seed/seed.json`이 게시글과 댓글을 중심으로 구성되어 있고, API 역시 게시글·댓글·좋아요 흐름에 집중되어 있다는 점을 함께 봤습니다. 이 두 맥락을 종합해 이번 메디큐브톡은 사람 관계 자체를 소비하는 SNS보다 제품 사용 경험과 질문이 공유·축적되고 다시 탐색되는 커뮤니티에 가깝다고 해석했습니다. 과제 계약에 없는 사용자 관계나 프로필 영역을 새로 확장하기보다, 제목과 본문을 먼저 읽게 하는 목록, 최신순을 보존한 검색·좋아요순 탐색, 댓글 흐름에 집중했습니다. 이 해석은 뒤의 UI hierarchy와 검색·정렬 범위를 정하는 기준이 됐습니다.

#### 제공된 출발점과 구현한 범위

APR이 제공한 범위는 Java 17/Spring Boot 3.5 프로젝트와 Spring Web/JDBC·PostgreSQL 연결, `GET /health`, CORS, Compose, `seed/seed.json`, Next.js 15 App Router 골격, `/dev-user`, 아바타와 Pretendard 자산입니다. 즉 실행 환경과 입력 데이터는 있었지만 community schema, 데이터 적재, 도메인 API와 실제 화면 동작은 구현 대상이었습니다.

그 위에 Flyway 스키마, 멱등한 시드 적재와 readiness gate, 게시글·댓글·좋아요 영속성, REST API, 소프트 삭제와 소유권 검증, keyset 페이지네이션, TanStack Query 캐시, 게시글 CRUD·댓글·좋아요, 무한 스크롤, 검색·정렬과 모바일 UI를 구현했습니다. 댓글 수정과 로그인은 과제 범위대로 구현하지 않았습니다. 이 경계를 명시한 이유는 starter가 보장한 것과 이 구현에서 판단하고 검증한 것을 섞지 않기 위해서입니다.

### Backend persistence — Spring `JdbcClient`

이 과제의 읽기 규칙은 단순한 row CRUD보다 “삭제되지 않은 행만 조회”, “댓글 수를 파생”, “시드 좋아요와 요청자 좋아요를 합산”, “정렬마다 다른 keyset 조건 적용”에 가깝습니다. 따라서 ORM mapping을 새로 도입할지, 스타터의 Spring JDBC 기반에서 SQL을 명시할지를 결정해야 했습니다.

도메인이 게시글·댓글·좋아요로 작고 query 의미를 리뷰에서 직접 확인하는 편이 중요하다고 판단해 `JdbcClient`와 SQL repository를 선택했습니다. Spring Framework가 설명하는 `JdbcClient`는 Spring JDBC의 named/positional parameter query와 update를 하나의 fluent API로 제공합니다.[^spring-jdbc-client] 이 capability가 현재 `PostRepository`, `CommentRepository`, `PostLikeRepository`의 작은 명시적 query 집합에 맞았습니다. 대신 JPA보다 SQL, row mapping과 DB 종속 동작을 직접 관리하는 비용을 수용했습니다.

이 선택은 구현 중 soft-delete predicate, requester별 `isLiked`, derived count, 검색 조건, 정렬별 cursor bound가 추가될 때 의미가 더 분명해졌습니다. SQL 자체를 설계 대상으로 두었기 때문에 논리 조건을 그대로 검토하고 `EXPLAIN`으로 index 사용과 cursor scan을 추적한 뒤 predicate만 교체할 수 있었습니다. 이는 JPA로 구현할 수 없다는 뜻이 아니라, query가 실제로 어떻게 실행되는지를 확인하며 발전시킨 이번 작업 방식에 명시적 SQL이 잘 맞았다는 뜻입니다.

### Schema migration — Flyway

평가자는 빈 PostgreSQL에서 시작할 수 있고, schema 변경과 과제용 `seed.json` 적재는 수명주기가 다릅니다. 애플리케이션 코드에서 테이블을 즉석 생성하거나 schema와 seed를 한 초기화 절차에 섞으면, 구조 변경 이력과 “과제 데이터가 이미 들어갔는가”를 구분하기 어렵습니다.

그래서 schema는 `V1__create_community_schema.sql`, seed는 `SeedInitializer`가 맡도록 분리했습니다. Spring Boot는 시작 시 Flyway migration을 실행하며 PostgreSQL에는 `flyway-database-postgresql` 모듈을 사용하도록 문서화하고 있고, 현재 `backend/build.gradle`도 그 구성을 따릅니다.[^spring-flyway] Flyway의 `flyway_schema_history`는 적용 migration, 성공 여부와 checksum을 추적합니다.[^flyway-history] 이 선택은 빈 DB 재현성과 변경 이력을 얻는 대신, 적용된 versioned migration을 임의 수정하지 않고 다음 migration으로 전진해야 하는 규율을 요구합니다. 과제 seed의 멱등성은 별도의 `seed_history`가 담당합니다.

### Frontend server state — TanStack Query

같은 게시글이 목록과 상세에 반복해서 나타나고, 좋아요·댓글·수정·삭제 뒤에는 여러 화면의 copy가 함께 바뀌어야 합니다. 각 component의 `useState`/`useEffect`로 page 누적, loading, 취소와 mutation 후 동기화를 따로 구현할지, API 데이터를 전담하는 cache 계층을 둘지가 핵심 질문이었습니다.

이 데이터는 브라우저가 소유하는 전역 상태가 아니라 서버가 원본인 상태이므로 TanStack Query를 선택했습니다. 공식 문서의 `useInfiniteQuery`는 `pages`, `pageParam`, `getNextPageParam`과 `fetchNextPage`로 누적 목록을 모델링하고,[^tanstack-infinite] query key는 query data를 고유하게 식별하는 cache 기준입니다.[^tanstack-query-keys] 또한 mutation 성공 후 관련 query invalidation과 query function의 `AbortSignal` 소비를 지원합니다.[^tanstack-invalidation][^tanstack-cancellation] 이는 이 과제의 무한 스크롤, sort/search별 cache, 더 이상 필요 없는 읽기 요청의 취소에 직접 대응합니다. 대신 production dependency와 `QueryClientProvider`가 추가되고, 어떤 cache를 직접 갱신하고 invalidate할지는 여전히 애플리케이션이 설계해야 합니다.

### Styling — CSS Modules

375px 한 화면에 맞춘 간격·타이포그래피·pressed/focus 상태를 세밀하게 표현해야 했지만, 과제 규모에서 utility framework나 UI kit를 새로 도입하면 design token보다 설정과 추상화가 더 커질 수 있었습니다. 그래서 Pretendard, reset과 공통 token만 `globals.css`에 두고 나머지는 component별 CSS Modules로 작성했습니다. Next.js는 `.module.css`의 class name을 고유하게 생성해 local scope와 충돌 방지를 제공합니다.[^next-css-modules] 추가 framework 없이 스타일의 근거가 각 feature 옆에 남는 대신 반복되는 CSS 선언을 일부 감수했습니다.

## 상태 설계

서버에서 받아온 데이터를 어디에 두고 언제 갱신하는지, 그 데이터를 어떤 단위로 나눠 요청하고 언제 취소하는지, 그리고 서버가 최종 판단하는 검증과 소유권을 차례로 설명합니다.

### 서버 상태와 cache 전략

같은 게시글은 최신 목록, 검색 결과, 좋아요순 목록과 상세 cache에 동시에 존재합니다. 여기에 수정·삭제·좋아요·댓글 수 변경이 생기므로 화면마다 `useEffect`로 fetch와 동기화를 따로 관리하면 어느 copy가 오래된 상태인지 추적하기 어려웠습니다. 이 문제가 API 데이터를 local/global UI state와 분리하고 TanStack Query를 server-state 계층으로 둔 직접적인 이유였습니다.

서버가 원본인 게시글 목록·상세, 댓글 page와 좋아요는 query cache에 둡니다. `frontend/lib/query/keys.ts`는 page size, `latest`/`likes`, 정규화된 검색어를 key에 포함해 서로 다른 결과와 cursor chain이 섞이지 않게 합니다. cursor 자체는 infinite query의 `pageParam`입니다. 반면 폼 입력, 검색창 열림, 메뉴·확인 dialog와 댓글 “더보기”처럼 화면을 닫으면 끝나는 상태는 local state에 둡니다. 이 경계 덕분에 UI만을 위한 별도 global store는 추가하지 않았습니다.

cache를 도입한 뒤에도 mutation 이후의 선택은 남았습니다. 모든 목록을 무조건 invalidate하면 정확하지만 network 낭비와 화면 깜빡임이 생기고, 반대로 모든 결과를 client에서 직접 고치면 검색 membership이나 likes ordering을 잘못 추론할 수 있습니다. 구현을 진행하며 “서버 응답만으로 결과를 정확히 증명할 수 있는가”를 기준으로 두 극단 사이의 규칙을 정했습니다. TanStack Query의 mutation 후 invalidation은 관련 query를 stale 처리하고 재조회하는 공식 mechanism입니다.[^tanstack-invalidation]

- 최신순 목록의 loaded post 좋아요 수와 댓글 수는 직접 반영합니다.
- 좋아요 toggle 뒤 likes-sort 목록은 순서가 바뀔 수 있어 invalidate합니다.
- 게시글 수정 뒤 검색 목록은 검색 포함 여부가 바뀔 수 있어 invalidate합니다.
- 생성 글의 검색 포함 여부나 likes 순위도 client가 단정하지 않고 재조회합니다.

이 규칙은 `frontend/lib/query/postListCache.ts`에 모았습니다. 모든 mutation은 server-confirmed 방식으로, 성공 전 cache를 바꾸지 않아 rollback 복잡성과 false success를 피합니다. 그 대가로 optimistic UI보다 좋아요 반응이 한 network round-trip 늦습니다.

### 페이지네이션과 조회 상태

목록을 한 번에 전부 가져오면 안 되고, 댓글은 한 게시글에 1,411개까지 치우쳐 있습니다. 가장 단순한 후보였던 `OFFSET`은 앞쪽에 새 row가 생기는 동안 page 경계가 흔들릴 수 있고, PostgreSQL도 큰 `OFFSET`의 skipped row를 server가 여전히 계산한다고 설명합니다.[^postgres-limit] 모바일 무한 스크롤은 임의 page 이동보다 앞에서 본 항목 다음을 안정적으로 이어 받는 일이 중요하므로, 최신순 게시글과 댓글은 마지막 `(created_at, id)` 뒤를 찾는 keyset pagination으로 시작했습니다.

첫 predicate는 의미가 가장 직접적인 `created_at < cursorCreatedAt OR (created_at = cursorCreatedAt AND id < cursorId)`였습니다. 전체 순회와 동일 timestamp fixture에서는 중복·누락 없이 정확했지만, 큰 댓글 데이터로 `EXPLAIN (ANALYZE, BUFFERS)`를 확인하자 이 조건이 composite index의 경계가 아니라 filter로 평가됐습니다. 깊어질수록 제거한 row와 buffer가 `3 → 728 → 1,459`, `6 → 23 → 42`로 늘어 API가 정확하다는 사실과 DB가 keyset의 구조적 이점을 얻는다는 사실이 같지 않음을 확인했습니다.

API 계약과 cursor 형식은 그대로 두고 repository predicate만 `(created_at, id) < (?, ?)` row-value comparison으로 바꿨습니다. PostgreSQL이 문서화한 left-to-right 비교 semantics와 `(created_at DESC, id DESC)` index ordering이 맞물려 조건이 `Index Cond`로 내려갔고, 가장 깊은 측정의 buffer는 약 3으로 유지됐습니다.[^postgres-row-comparison] 측정 SQL은 약 `0.285ms → 0.112ms`였지만 작은 로컬 API의 체감 향상 자체보다, page가 깊어져도 불필요한 scan work가 늘지 않는 구조를 얻은 것이 핵심입니다. 변경 뒤에도 전체 순회와 timestamp tie fixture로 같은 결과를 다시 검증했습니다.

정렬은 `(created_at DESC, id DESC)`로 고정해 timestamp tie도 deterministic하게 만들고, cursor는 두 값을 URL-safe Base64로 감춰 client가 내부 형식에 의존하지 않게 했습니다. `PostReadService`는 `limit + 1`행으로 다음 page를 판정합니다. 이 방식은 deep page와 concurrent insert에 적합하지만 cursor가 page number처럼 읽기 쉽거나 임의 page 이동에 적합하지는 않습니다.

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

### 측정으로 정한 요청 단위

첫 구현은 동작을 완성하기 위한 보수적 초기값으로 게시글 10개, 댓글 continuation 20개, `rootMargin` 200px, 검색 debounce 300ms를 사용했습니다. 이 값들은 일반적인 관례를 근거로 확정한 것이 아니므로 기능이 안정된 뒤 375×778 Chromium, 고정 4G(지연 200ms, 다운로드 1.6Mbps), clean seed 39 readable posts 조건에서 후보를 한 번에 하나씩 바꿔 비교했습니다. 최초 댓글 `limit=2`는 과제 계약이므로 실험 대상에서 제외했습니다.

- 게시글 `10/15/20`: 처음에는 39개 전체 순회의 요청 수로 15를 골랐습니다. 이 비교에는 seed 끝에 도달하는 효과가 섞여 있어, 첫 page SSR 적용 뒤 [게시글 batch size 재측정](#게시글-batch-size-재측정)으로 다시 확인했습니다.
- 댓글 continuation `10/20/30`: readable 댓글 1,411개인 seed 게시글(`id=2`)에서 각각 `141/71/47`회였습니다. 20→30은 continuation 요청을 약 34% 줄였고 p95는 248ms로 유지됐으며 30개 append에서 관찰 가능한 rendering pause가 없었습니다. 같은 최종 댓글을 받되 요청/envelope/header 반복만 줄인 결과입니다.
- `rootMargin 100/200/400px`: 100px은 loading UI가 노출됐고, 400px은 사용자 scroll 전 page 2를 요청했습니다. 두 현상이 없었던 200px을 유지했습니다. 이후 게시글 batch size 재측정에서는 빠른 scroll의 대기가 page size보다 다음 page 요청 시점의 영향을 받는 정황이 보여, `rootMargin`은 별도 재검증 대상으로 남겼습니다.
- 검색 debounce `200/300/500ms`: 같은 검색어를 사람처럼 연속 입력했을 때 200ms는 짧은 250ms 멈춤에도 요청을 시작했고, 500ms는 최종 입력 뒤 결과가 그만큼 늦게 보였습니다. 300ms는 중간 pause를 흡수하면서 500ms보다 최종 결과에 약 200ms 빨리 도달했습니다.

당시 측정으로 게시글 page size를 `10 → 15`, 댓글 continuation을 `20 → 30`으로 조정했고, `rootMargin=200px`와 debounce 300ms는 유지했습니다. 적용 뒤 게시글은 `15/15/9`의 3회 응답으로 39개, 댓글은 최초 2개 뒤 30개 단위로 1,411개를 끝까지 순회해 중복·누락과 종료 후 추가 요청이 없음을 다시 확인했습니다. 값 하나가 모든 환경의 정답이라는 뜻은 아니며, 당시 seed·viewport·network profile에서 사용자에게 loading gap을 보이지 않으면서 선조회와 요청 반복을 줄인 균형점이었습니다. 게시글 batch size는 이후 첫 page SSR을 도입한 뒤 아래 재측정에서 첫 SSR과 후속 page를 따로 검증해 `15/15` 유지 근거를 다시 확인했습니다.

#### 게시글 batch size 재측정

readable post가 39개뿐인 seed에서는 어떤 후보든 곧 목록 끝에 닿고, 15가 "39개를 세 번에 나누는 값"처럼 보일 수도 있습니다. 또 첫 page SSR 이후에는 HTML에 넣는 개수(`SSR_SIZE`)와 scroll 중 cursor 요청 크기(`CONTINUATION_SIZE`)가 서로 다른 구간에 영향을 주는 별개의 결정입니다. 그래서 원본의 제목·본문 길이, 작성자, 좋아요·댓글 수 분포와 시간 간격을 유지한 일회용 DB(visible 게시글 160개)를 만들고, 375×778·CPU 4x·Lighthouse devtools mobile throttling 조건에서 두 값을 10/15/20으로 따로 측정했습니다. 160개를 끝까지 읽지 않고, 30개와 45개를 소비하는 고정 깊이로 비교했습니다.

| 판단 | 후보 | 결과 |
| --- | --- | --- |
| 첫 SSR | 10 / 15 / 20 | LCP median 2,278 / 2,260 / 2,372ms로 범위가 겹치는 noise 수준 → 15 유지 |
| 후속 page | 10 / 15 / 20 | 10은 요청이 늘고 읽는 속도에서도 loading 문구가 보임, 20은 일관된 이득 없음 → 15 유지 |

결론은 현재 값 15/15 유지입니다. 두 값은 기술적으로 따로 정할 수 있지만 다르게 둘 성능상 이유가 측정되지 않았고, 15/20처럼 섞으면 첫 page와 후속 page 크기 분기가 query·cache·refetch 경로에 생깁니다. page size는 전체 게시글 수에 맞춰 정하는 값도 아닙니다. cursor pagination은 사용자가 읽은 만큼만 요청하므로, 게시글이 1,000개나 100,000개로 늘어도 한 session의 소비 깊이가 기준입니다.

15는 비교한 10/15/20 중 바꿀 근거가 없었던 값이지 모든 기기·network의 최적값이 아닙니다. 실제 기기와 scroll 패턴, HTTP/2 환경은 측정하지 않았습니다. 상세 조건과 수치는 [docs/PERFORMANCE.md §13](docs/PERFORMANCE.md#13-frontend-list-batch-size-benchmark--2026-10-02)에 있습니다.

### 검색·정렬과 요청 제어

39개 목록에서도 특정 대화를 다시 찾거나 반응이 큰 글을 보는 탐색 경로가 없었습니다. 사람·프로필 중심 기능을 새로 만들기보다 대화 재발견을 돕는 것이 앞서 정한 커뮤니티 해석에 맞았습니다. 다만 과제 계약은 최신순이므로 이를 기본값으로 보존하고, 좋아요순과 제목·본문 검색을 선택 기능으로 추가했습니다. 이미 받은 client page만 필터링하면 “검색 결과 없음”이 전체 데이터의 결론처럼 보이는 오류가 생기므로 `PostController` → `PostReadService` → `PostRepository`에서 server-side로 처리합니다.

검색은 `%`, `_`, `\\`를 literal로 escape한 뒤 제목 또는 본문에 case-insensitive substring match를 적용합니다. 최신순 cursor와 좋아요순 `(likeCount, createdAt, id)` cursor는 형식을 분리해 다른 ordering에 재사용하면 거절합니다. query key에도 sort와 검색어를 포함해 각 조합이 첫 page부터 독립적으로 시작합니다. likes ordering은 탐색 가치를 얻는 대신 count가 concurrent mutation으로 움직일 수 있어 snapshot ranking까지는 보장하지 않습니다.

검색 input과 서버 query를 같은 state로 즉시 연결하면 글자마다 요청이 생겼습니다. debounce는 짧은 입력 연속을 한 요청으로 묶어 요청 수를 줄이지만, 이미 시작한 이전 요청을 멈추지는 못합니다. 그래서 TanStack Query가 query function에 넘겨 주는 `AbortSignal`을 실제 `fetch`까지 전달하도록 했습니다.[^tanstack-cancellation] query key 분리로 서로 다른 검색 결과 cache가 섞이지 않게 하고, signal 소비로 더 이상 필요하지 않은 이전 요청도 취소합니다. 실제 검증에서는 다섯 글자를 연속 입력했을 때 확정 요청 한 번만 발생했고, 일부러 느리게 만든 이전 검색은 abort된 뒤 최종 검색 결과가 유지됐습니다. 즉 300ms debounce는 불필요한 요청이 생기지 않게 막고, cancellation은 이미 시작됐지만 더 이상 필요 없는 요청을 멈추는, 서로 다른 문제를 해결합니다.

같은 규칙은 목록에만 두지 않고 TanStack Query로 읽는 모든 화면에 적용했습니다. `usePostList`, `usePostDetail`, `useComments`의 query function은 각각 `getPosts`, `getPost`, `getComments`(`frontend/lib/api/community.ts`)에 signal을 넘기고, 공통 `request()`(`frontend/lib/api/client.ts`)가 이를 `fetch`에 연결합니다. `request()`는 signal이 이미 abort된 경우 원래의 abort 오류를 그대로 던져 의도된 취소가 `NETWORK_ERROR`로 바뀌지 않게 합니다. 브라우저에서 상세와 최초 댓글 요청을 일부러 지연시킨 뒤 화면을 떠나면 두 요청 모두 abort되었고, query는 error 상태가 아니었으며 오류 UI도 나타나지 않았습니다. “더보기” 이후 continuation page를 받는 도중 화면을 떠나는 경우도 같은 query function을 거치지만, 그 경로는 브라우저에서 따로 검증하지 않았습니다.

### 검증과 소유권

버튼을 숨기거나 submit을 disable하는 것만으로는 직접 API 호출을 막을 수 없습니다. 따라서 frontend validation은 빠른 UX feedback만 담당하고, backend `MutationValidator`가 제목 필수·20 Unicode code point 이하·본문/댓글 nonblank 규칙을 다시 검사합니다.

과제는 login이나 user table 없이 requester header/cookie를 계약으로 줍니다. 그래서 `RequesterResolver`의 우선순위를 `x-user-id` header → 같은 이름의 cookie → `apr_tester`로 한곳에 고정하고 `author == requester`를 소유권으로 사용했습니다. `PostMutationService`와 `CommentMutationService`가 readable target을 lock한 뒤 소유권을 확인하므로 UI affordance를 우회한 mutation도 거절됩니다. 실제 인증 체계로 확장할 수 있는 model은 아니지만, 과제 계약 밖의 user domain을 만들지 않으면서 backend를 authoritative boundary로 유지합니다.

## 구조

앞의 결정을 코드에서 유지하기 위해 HTTP 처리, 소유권·transaction, SQL을 변경 이유가 다른 경계로 나눴습니다. 한 class에 섞으면 같은 규칙을 endpoint마다 반복하고 backend authorization을 UI 상태에 의존하기 쉽기 때문입니다.

### Backend 구조

- Controller: `PostController`, `CommentController`가 HTTP parameter/body와 응답 경계를 담당합니다.
- Service: `PostReadService`, `PostMutationService`, `CommentMutationService`가 검증, 소유권, transaction과 도메인 규칙을 담당합니다.
- Repository: `PostRepository`, `CommentRepository`, `PostLikeRepository`가 SQL과 row mapping을 담당합니다.
- Common: `RequesterResolver`, `MutationValidator`, cursor codec과 공통 error handler가 횡단 규칙을 한곳에 둡니다.
- Seed: `SeedInitializer`와 `SeedReadiness`가 seed 적재와 readiness를 담당합니다(데이터 적재 참고).

### Frontend 구조

프론트엔드의 App Router page는 route parameter를 feature에 연결하는 얇은 경계로 두고, `features/post-list`, `features/post-detail`, `features/post-form`으로 사용자 흐름을 나눴습니다. API client·query cache·공통 hook은 `frontend/lib`에 둡니다. 이 분리는 소규모 과제에 계층 수를 추가하지만, UI가 숨긴 버튼은 보안 경계가 아니며 service authorization이 최종 판단이라는 점을 분명하게 합니다.

`frontend/lib`에는 `api/`(공통 `request()`와 endpoint 함수), `query/`(query key와 목록 cache 규칙), `hooks/`(`useInfiniteScrollTrigger`, `useSingleFlight`), `time/`·`validation/`(상대 시간과 게시글 입력 검증)이 있습니다.

## 데이터 적재

### 데이터 모델과 seed 해석

처음에는 화면에 필요한 `posts`와 `comments`만 옮기면 될 것처럼 보였지만, seed가 표현하는 사실과 API가 요구하는 사실의 모양이 달랐습니다. 삭제 목록은 별도 ID 집합이고 좋아요는 사용자별 관계가 아닌 집계이며, 원본 ID는 보존해야 하지만 새 데이터에는 자동 ID가 필요했습니다. 이 차이를 초기화 코드에서 임시로 보정하지 않고 schema에 드러내는 쪽을 택했습니다.

#### Soft delete

과제는 삭제된 resource를 화면과 API에서는 없어진 것처럼 보이게 하되 DB row는 물리적으로 보존하라고 요구합니다. 별도 archive table로 옮기거나 `deleted_at`을 둘 수도 있었지만, seed는 삭제된 ID만 제공하고 삭제 시각은 제공하지 않습니다. 존재하지 않는 시각을 만들어 넣기보다 현재 상태만 나타내는 `posts.is_deleted`와 `comments.is_deleted`를 선택했습니다.

모든 normal read와 count에 이 조건을 명시해야 한다는 비용이 생기지만, 삭제 정책이 SQL에서 확인 가능하고 seed의 deleted ID도 같은 model로 표현됩니다. 게시글 삭제 때 child row까지 삭제 상태로 다시 쓰지는 않습니다. 댓글 자체가 명시적으로 삭제된 상태와, 댓글은 남아 있지만 parent 게시글이 unreadable해 접근할 수 없는 상태를 구분하면서도 제품 API에서는 둘 다 노출하지 않습니다. unreadable parent의 댓글 조회·생성·삭제를 차단하고, 명시적으로 삭제된 댓글은 `commentCount`에서도 제외합니다.

#### Likes

시드는 좋아요 총수만 주고 누가 눌렀는지는 주지 않으며, 아직 애플리케이션에서 만들어진 requester 관계가 없으므로 초기 `isLiked`는 모든 요청자에게 false여야 합니다. 반면 이후 API의 `isLiked`는 현재 요청자 기준이어야 합니다. 총수를 mutable counter 하나로 두면 요청자 관계를 잃고, 과거 사용자 행을 만들어 채우면 존재하지 않는 사용자와 관계를 발명하게 됩니다. 그래서 알려진 두 사실을 분리했습니다.

```text
likeCount = posts.base_like_count + COUNT(post_likes rows)
```

시드 집계는 `base_like_count`, 애플리케이션에서 생성된 관계만 `post_likes(post_id, user_id)`에 저장합니다. composite primary key가 같은 요청자의 중복 행을 막고 `isLiked`는 그 행의 존재로 계산합니다. 이 model은 입력 사실을 보존하지만, 표시 count마다 관계 행 aggregate가 필요하다는 대가가 있습니다. 제공 데이터 규모에서는 denormalized counter의 동기화 위험보다 이 명확성을 택했습니다.

#### IDs

시드 ID는 URL과 관계의 기준이므로 그대로 보존해야 하지만 새 row의 ID는 자동 생성되어야 합니다. 단순 auto-increment만 믿으면 명시적 seed ID와 이후 생성 ID가 충돌할 수 있고, 반대로 모든 ID를 애플리케이션에서 만들면 DB가 제공하는 동시성 안전한 할당을 버리게 됩니다. 두 경로를 한 column에서 지원하기 위해 `GENERATED BY DEFAULT AS IDENTITY`를 선택했습니다. PostgreSQL에서 이 형태는 명시값이 있으면 그 값을 우선하고, 생략하면 implicit sequence 값을 사용합니다.[^postgres-identity] 명시적 seed insert는 sequence를 자동으로 전진시키지 않으므로 `IdentitySequenceRepository.synchronizeWithSeededRows()`가 최대 ID에 맞춥니다. 별도 ID allocator를 만들지 않는 대신 seed 이후 sequence sync가 필수입니다.

### schema와 seed 분리

첫 구현부터 schema migration과 과제 데이터 적재는 분리했습니다. Flyway는 구조의 버전과 checksum을 관리하고, `SeedInitializer`와 `seed_history`는 이 저장소의 고정 입력이 한 번 반영됐는지를 판단합니다. 첫 실행에는 2,118개 원본 row가 모두 들어가야 하고, 재기동은 중복되면 안 되며, 실패한 적재를 완료로 기록해서도 안 되기 때문입니다. 고정된 과제 seed에는 단일 marker가 단순하고 충분하지만, 운영 데이터가 버전별로 바뀌는 제품이라면 content version별 migration 전략이 추가로 필요합니다.

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

`SeedInitializer.run()` 전체가 하나의 transaction이므로 posts/comments와 `assignment-v1` marker는 함께 commit되거나 rollback됩니다. marker가 이미 있으면 재기동 때 seed를 다시 쓰지 않아 중복을 만들거나 사용자가 만든·수정한 runtime data를 원본으로 되돌리지 않습니다. sequence sync는 marker 기록 전에 실행되며, 실패 후 재시작하더라도 marker가 없으므로 적재를 다시 시도합니다. PostgreSQL sequence 자체가 transaction rollback의 대상이라고 가정하지 않고, marker가 없으면 전체 적재와 sync를 다시 수행하는 복구 경로를 기준으로 삼았습니다. `SeedHistoryRepository`는 과제 seed의 멱등성을 판단하고, Flyway의 schema history와 역할이 다릅니다.

### Readiness — “서버가 켜짐”과 “서비스 준비 완료”의 분리

초기 `/health`는 DB에 query가 되는지만 확인했습니다. 이 기준에서는 Tomcat과 DB가 먼저 살아난 뒤 `ApplicationRunner`의 seed transaction이 아직 commit되지 않은 짧은 구간에도 200을 반환했고, clean start 측정에서 health는 성공했지만 게시글 API의 `total`은 아직 0인 상태를 실제로 관찰했습니다. 프로세스가 살아 있다는 의미와 제품 데이터까지 요청을 받을 준비가 됐다는 의미를 같게 본 것이 원인이었습니다.

이를 확인한 뒤 `SeedReadiness`를 추가하고, 이미 적재된 재기동 경로와 새 적재 경로 모두 transaction의 `afterCommit`에서만 ready를 열도록 바꿨습니다. 그 전의 `/health`는 DB 연결이 살아 있어도 503과 `starting`을 반환합니다. 따라서 Compose가 backend를 healthy로 판단할 때 API 데이터도 준비되어 있습니다. readiness를 얻는 대신 최초 기동은 seed가 끝날 때까지 healthy가 되지 않으며, row별 insert의 단순성과 실패 위치 명확성을 위해 clean start 약 0.9초의 일회성 비용을 수용했습니다.

## 개선한 점 · 남은 과제 (선택)

### Figma를 제품 동작으로 옮긴 과정

공식 Figma는 핵심 화면과 visual hierarchy를 제공하지만 모든 runtime 상태를 정의한 명세는 아닙니다. 그래서 보이는 모양을 그대로 복제하는 것과, 디자인에 없는 제품 규칙을 임의로 만드는 일을 구분했습니다. 기본 viewport, typography, 색상, 작성·상세·삭제 확인의 의미는 유지하고, 빠진 동작은 앞서 정한 “글과 대화 중심 커뮤니티” 해석과 실제 375×778 실행 결과를 기준으로 채웠습니다.

목록의 첫 판단 대상은 작성자보다 글 내용이라고 보고 제목 → 본문 preview → 작성자·반응·시간 순서의 content-first hierarchy를 택했습니다. 처음 retouch한 98px 행은 한 화면에 더 많이 보였지만 정보가 과밀했고, 실제 viewport에서 읽기와 touch 분리가 나아지는 108px rhythm으로 조정했습니다. 이 과정에서도 팔로우·팔로워, 상세 프로필, 인플루언서 표시나 개인화 feed처럼 API와 데이터가 뒷받침하지 않는 SNS 기능은 추가하지 않았습니다.

초기 화면은 피드 소비에는 충분했지만 특정 대화를 다시 찾는 진입점이 없었습니다. 검색과 최신순·좋아요순을 추가하되 별도 화면으로 content를 밀어내지 않고 44px discovery toolbar 한 줄에 모았습니다. app title까지 고정하면 작은 화면의 세로 공간을 계속 점유하므로 title은 scroll되고 discovery toolbar만 sticky로 남겼습니다. 검색을 열면 새 modal이나 pill을 겹치지 않고 같은 toolbar가 `← · 검색 icon · input`의 flat mode로 바뀌며, Escape와 뒤로 닫기, focus 복귀, iOS 확대를 피하는 16px input을 포함합니다.

본인 글의 action은 처음에는 모바일 관례를 따라 bottom sheet로 해석할 수 있었습니다. 하지만 실제 command가 수정과 삭제 두 개뿐이라 화면 전체 폭, scrim과 별도 cancel affordance가 action의 무게보다 컸습니다. 최종 구현은 `⋮` 바로 아래 144px non-modal popover로 바꿔 trigger와 action의 공간적 관계를 보존했습니다. 바깥 클릭과 Escape로 닫고 trigger로 focus를 돌려보냅니다. 반면 삭제는 되돌리기 어려운 별도 단계이므로 중앙 confirmation dialog와 scrim을 유지하고, 확인 전에는 실제 mutation을 보내지 않습니다.

loading과 error UI는 빠진 상태가 아니라 코드에 구현돼 있습니다. 목록은 최초 loading·최초 error·다음 page loading·다음 page error와 retry를, 상세는 loading·not found·error를, 댓글은 최초와 continuation의 loading/error/retry를 각각 구분합니다. 작성·수정·삭제·좋아요 같은 mutation도 pending 동안 중복 실행을 막고 실패 메시지를 표시합니다. 정적 Figma가 이 상태를 그리지 않았기 때문에 runtime에서 보완한 것이며, 디자인 미구현을 그대로 방치한 것이 아닙니다.

세부 근거와 공식 디자인/제품 결정의 경계는 `docs/UI_SPEC.md` 및 `design/design-retouch/readme.md`에 기록했습니다. 다만 공식 Figma 원본과 새로 캡처한 모든 화면을 pixel 단위로 대조한 결과까지 주장하지는 않습니다.

### 접근성과 삭제 확인 dialog

keyboard focus, Escape 종료, popover·dialog를 연 trigger로의 focus 복귀, destructive confirmation, `prefers-reduced-motion` 대응도 정적 Figma가 정의하지 않은 runtime 동작이라 직접 추가했습니다. 애니메이션은 상태 변화를 설명하는 범위로 제한하고 reduced-motion 환경에서는 제거합니다.

게시글과 댓글 삭제는 공용 `ConfirmDialog`(`frontend/features/post-detail/ConfirmDialog.tsx`)를 사용합니다. 처음에는 `role="alertdialog"`와 `aria-modal="true"`, 제목·본문 연결, 열릴 때 취소 버튼 focus만으로 modal 동작이 충분하다고 보았습니다. 그러나 375×778 Chromium에서 Tab 순서를 실제로 따라가 보니 `예` 다음 Tab과 `취소` 이전 Shift+Tab이 dialog 뒤의 뒤로 가기, 좋아요, 댓글 입력까지 이동했습니다. `aria-modal`은 보조기술에 modal임을 알릴 뿐 브라우저의 keyboard 이동을 막지 않는다는 점을 확인한 것입니다. 또 삭제 요청 중 두 버튼이 disabled 되면 Chromium이 focus를 `<body>`로 옮겼고, 요청이 실패한 뒤에도 focus가 dialog 밖에 남았습니다.

그래서 공용 component 안에서만 고쳤습니다. Tab과 Shift+Tab은 활성화된 dialog 버튼 사이에서 순환하고, focus가 dialog 밖에 있으면 방향에 따라 첫 버튼이나 마지막 버튼으로 돌아옵니다. 요청 중 두 버튼이 모두 disabled일 때는 Tab이 배경으로 이동하지 않고, 실패해 요청이 끝나면 focus를 `취소`로 되돌립니다. 닫히면 dialog를 연 요소로 focus를 돌려보내고, 요청 중에는 Escape와 scrim 클릭으로 닫히지 않습니다. 버튼이 두 개뿐인 dialog라 별도 modal library나 portal, `inert` 없이 해결했습니다. dialog 뒤 페이지 스크롤은 잠그지 않았습니다. dialog가 화면에 고정되어 있어 조작에는 영향이 없다고 판단했습니다.

### 제품 동작을 끝까지 다듬으며 바뀐 선택

댓글 query는 처음에 다른 server state처럼 shared `QueryClient`의 cache 수명을 그대로 사용했습니다. 한 번 “더보기”로 continuation page를 받은 뒤 같은 SPA session에서 상세를 다시 방문하자, cached page가 먼저 복구되어 사용자가 누르기 전부터 최초 2개 계약을 우회하는 문제가 드러났습니다. 댓글 continuation의 재사용 가치보다 방문마다 명시적 gate를 지키는 일이 중요하다고 판단해 이 query에 `gcTime: 0`을 적용했습니다. 이제 상세 재방문도 다시 `limit=2`에서 시작하고 “더보기” 전에는 continuation sentinel이 열리지 않습니다. 대가로 방금 본 긴 댓글 목록도 재방문하면 다시 요청합니다.

목록의 각 제목 link에는 Next.js의 route prefetch가 기본 적용돼 있었습니다. 전체 39개를 순회해 보니 detail route 선조회가 40회 발생했고 상당수가 중간 취소됐지만, 동적 route shell은 약 40~150B에 그쳤고 실제 클릭에는 route/chunk 요청이 다시 필요했습니다. 클릭 제목이 보이기까지의 측정도 prefetch를 켠 경우가 더 빠르지 않았습니다. 그래서 반복되는 게시글 detail link만 `prefetch={false}`로 바꾸고, 작성처럼 정적이며 재사용 가치가 있는 link는 기본 동작을 유지했습니다. prefetch 전체를 끈 것이 아니라 실제 hit 가능성과 비용을 route 성격별로 다시 판단한 결과입니다.

### 테스트와 검증

이 구현의 위험은 happy path보다 boundary에 있습니다. 같은 timestamp의 cursor, 삭제 parent의 child 접근, header/cookie 우선순위, 재기동 seed와 mutation 뒤 여러 cache가 대표적입니다. 그래서 검증을 세 층으로 나눴습니다. 저장소에 커밋된 자동 test, 모든 pull request에서 같은 조건으로 반복되는 CI, 그리고 실제 PostgreSQL·Compose·브라우저에서 확인한 runtime acceptance입니다.

#### 1. 백엔드 자동 test

백엔드 JUnit/Mockito tests는 requester 우선순위, Unicode validation, cursor 정밀도와 malformed input, seed parsing/readiness, mutation 소유권, soft-delete/not-found, 좋아요와 검색·정렬 계약을 다룹니다. 관련 test는 `backend/src/test/java/com/apr/community` 아래에 있습니다. 이 test들은 controller·service·cursor codec·seed reader 같은 단위로 작성되어 실제 DB 없이 실행되며, DB가 필요한 경계는 아래 runtime 검증에서 확인했습니다.

#### 2. GitHub Actions CI

로컬에서 실행하던 기본 검사를 리뷰어도 같은 조건으로 확인할 수 있도록 `.github/workflows/ci.yml`을 두었습니다. `main`을 대상으로 한 pull request와 `main` push마다 두 job이 독립적으로 실행됩니다.

| job | 환경 | 실행 |
| --- | --- | --- |
| backend | Ubuntu, Java 17 | 저장소의 Gradle wrapper로 `./gradlew test` |
| frontend | Ubuntu, Node.js 22 | `npm ci` → `npm run typecheck` → `npm run build` |

현재 커밋된 백엔드 test는 실제 DB를 요구하지 않으므로 CI는 PostgreSQL이나 Docker Compose를 띄우지 않습니다. 필요 없는 서비스를 추가하지 않아 실패 원인이 test나 build 자체로 좁혀지는 대신, CI가 통과했다는 사실이 실제 DB와 브라우저 동작을 보장하지는 않습니다. CI는 반복 가능한 unit/typecheck/build 관문이고, 실제 DB·Compose·브라우저 동작은 다음 층에서 확인합니다.

#### 3. Runtime acceptance

`docs/VERIFICATION.md`에는 실제 PostgreSQL/Compose와 375×778 Chromium으로 다음을 검증한 근거가 있습니다.

- fresh volume 전체 기동, `/health` readiness와 alternate ports
- 원본 ID·삭제 상태·sequence·`seed_history` 멱등성
- 39개 readable post와 최다 1,411개 댓글의 cursor 완전 순회, tie 순서, 중복·누락·종료 조건
- requester header/cookie 전환, 소유권, 좋아요 격리, post CRUD와 comment create/delete
- soft-deleted row의 물리적 보존과 API/cache에서의 제거
- latest/likes/search 조합과 mutation 후 membership/order 갱신
- 검색·상세·최초 댓글 요청의 취소와, 취소가 오류 상태로 바뀌지 않음
- 삭제 확인 dialog의 Tab/Shift+Tab 순환, 실패한 삭제 뒤 focus 복구, Escape·scrim·focus 복귀
- 375×778의 필수 화면, fixed control, horizontal overflow와 loading/error 흐름

브라우저 검증에는 Playwright 등 임시 자동화 script를 사용했지만 저장소에 commit하지 않았으므로 CI에서 반복 실행되지 않습니다. 문서만 보고 통과로 간주하지 않고 실행 증거가 있는 항목만 통과로 기록했습니다. 성능 데이터셋과 SQL plan 측정은 `docs/PERFORMANCE.md`에 분리했습니다.

### 트레이드오프와 시간이 더 있었다면

현재 구현은 과제의 데이터 규모와 핵심 사용자 흐름에서 필요한 기능을 완결하면서, 아직 문제가 되지 않은 복잡도는 의도적으로 추가하지 않았습니다. 다음 단계도 기능을 무작정 늘리기보다 현재 선택의 한계가 실제 사용량·데이터 규모에서 제품 문제로 바뀌는 시점에 진행하고 싶습니다.

#### 가까운 기술 개선

**긴 댓글의 rendering virtualization**

현재 cursor pagination은 댓글 1,411개를 한 번에 받지 않게 해 network와 응답 크기를 제한합니다. 다만 사용자가 끝까지 읽으면 이미 받은 댓글의 DOM node는 계속 누적됩니다. 현재 데이터와 모바일 실행에서는 문제가 관찰되지 않았지만, 더 긴 대화가 일반화되거나 저사양 기기의 rendering 비용이 커진다면 보이는 범위와 작은 buffer만 mount하는 list virtualization을 검토하겠습니다. pagination은 data를 언제 얼마나 받을지, virtualization은 받은 data 중 무엇을 DOM에 남길지를 제어하므로 두 계층을 독립적으로 유지하는 방향입니다.

**변하는 좋아요 순위의 안정적인 pagination**

좋아요순은 mutable `likeCount`를 cursor의 일부로 사용하므로 순회 중 다른 mutation이 일어나면 항목이 page 경계 사이로 이동할 수 있습니다. 현재는 같은 post ID를 첫 항목만 렌더링해 중복 노출을 막고 있으며, 39개 과제 데이터에는 snapshot 체계를 추가하지 않았습니다. 동시 반응과 page 수가 커져 이 보정만으로 충분하지 않게 되면 ranking snapshot이나 version을 두는 방식 등, 변하는 순위를 안정적으로 순회할 수 있는 전략을 요구사항과 저장 비용까지 포함해 비교하겠습니다. 아직 구현 방식을 확정한 것은 아닙니다.

**데이터 규모에 맞춘 검색 품질**

현재 제목·본문 substring 검색은 readable post 39개의 대화를 다시 찾는 데 충분하고 동작도 단순합니다. 글이 크게 늘어나 검색 latency나 결과 품질이 실제 문제가 되면 PostgreSQL full-text search, 적절한 index와 relevance ranking을 후보로 측정하겠습니다. 한국어 형태소 처리도 미리 도입하기보다 실제 query와 검색 실패 사례가 그 복잡도를 정당화할 때 평가할 대상입니다.

**주요 상태의 visual regression**

필수 화면과 loading·error·검색·dialog interaction은 375×778 runtime에서 검증했지만, UI 변경마다 모든 상태의 pixel 차이를 자동 비교하는 체계까지 두지는 않았습니다. 화면 변경 빈도와 회귀 비용이 커지면 안정된 screenshot baseline을 만들고 happy path뿐 아니라 최초·후속 loading, error/retry, 검색 mode와 destructive dialog까지 visual regression 대상으로 포함하겠습니다.

#### 제품 경험 확장 — 커뮤니티 대화를 따라 할 수 있는 지식으로

현재 메디큐브톡은 사용 경험과 질문을 게시글·댓글이라는 자유 형식으로 공유하는 대화 중심 커뮤니티입니다. 이 범위에서는 글을 읽고 반응하며 후속 질문을 남기는 흐름에 집중했고, 팔로우 graph나 개인화 feed를 추가하지 않았습니다. 그러나 대화가 충분히 쌓이면 좋은 사용법이 여러 글과 댓글에 흩어져, 실제로 따라 하려는 사용자가 제품, 사용 순서, 적용 부위, 사용량과 주의사항을 다시 해석해야 하는 문제가 생길 수 있습니다.

그 시점의 다음 제품 단계로는 반응이 좋고 반복해서 참고되는 경험을 구조화된 단계별 사용 가이드로 표현하는 방향을 탐색하고 싶습니다. 원문 대화를 없애거나 모든 글을 정형화하는 것이 아니라, 재사용 가치가 확인된 지식을 `제품 → 순서·단계 → 대상 부위 → 사용량 → 주의사항·팁`처럼 따라 할 수 있는 형태로 연결하는 방식입니다.

```text
자유 형식 커뮤니티 대화
  → 반복해서 참고되는 사용 지식
  → 따라 할 수 있는 구조화된 사용 가이드
```

이 방향은 사람을 추천하는 SNS로 확장하는 대신 커뮤니티에 축적된 대화의 실용성을 높입니다. 먼저 어떤 글이 실제로 반복 참조되는지와 어떤 필드가 사용자에게 필요한지를 관찰한 뒤 구조를 정해야 하며, 현재 저장소에는 이러한 guide domain이나 변환 기능이 구현돼 있지 않습니다.

더 장기적으로는 제품 특성상 위치나 순서를 말로만 설명하기 어려운 경우, 구조화된 가이드를 카메라 기반 위치 안내나 단계별 코칭 경험으로 연결할 가능성도 검토할 수 있습니다. 이는 현재 계획이나 기술 선택이 아니라, 사용자가 설명을 읽는 데서 그치지 않고 실제 routine을 따라가는 과정에서 공간 안내가 필요하다는 근거가 확인될 때 탐색할 제품 방향입니다.

더 자세한 결정 배경은 `docs/ARCHITECTURE.md`, 요구사항 traceability는 `docs/REQUIREMENTS.md`, 검증 절차와 결과는 `docs/VERIFICATION.md`를 참고할 수 있습니다.

[^spring-jdbc-client]: [Spring Framework — Unified JDBC Query/Update Operations: `JdbcClient`](https://docs.spring.io/spring-framework/reference/data-access/jdbc/core.html#jdbc-JdbcClient)
[^age-r-app]: [Google Play 앱 페이지 — 에이지알 - 메디큐브 디지털클리닉 AGE-R](https://play.google.com/store/apps/details?hl=ko&id=com.apr.digitalclinic)
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
