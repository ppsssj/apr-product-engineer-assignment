# 메디큐브톡 커뮤니티

APR Product Engineer 인턴 과제의 모바일 커뮤니티를 Spring Boot·PostgreSQL과 Next.js로 구현했습니다. 화면을 완성하는 데서 멈추지 않고, 빈 DB 재현부터 긴 목록의 요청 단위, mutation 뒤 cache 일관성, readiness·동시성·keyboard focus 같은 경계를 실제 실행으로 확인했습니다.

구현 과정은 `구현 → 문제/불확실성 관찰 → 재현·측정 → 최소 수정 → 필요한 회귀 보호` 순서를 따랐습니다. 상세한 판단 과정은 [Engineering Notes](docs/ENGINEERING_NOTES.md), 실행 근거는 [Verification](docs/VERIFICATION.md)에 있습니다.

## 실행

Docker와 Docker Compose(`docker compose`)가 필요합니다. backend와 frontend는 container 안에서 build되므로 host JDK나 Node.js는 필요하지 않습니다. 첫 실행에는 image와 Gradle·npm dependency를 내려받을 network 연결이 필요합니다.

저장소 root에서 다음 명령으로 PostgreSQL, backend와 frontend를 기동합니다.

```bash
docker compose up
```

| 대상 | 기본 주소 |
| --- | --- |
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080/api |
| Health | http://localhost:8080/health |
| 개발용 requester 전환 | http://localhost:3000/dev-user |

기본 host port `3000`, `8080`, `15432`가 겹치면 다음처럼 바꿀 수 있습니다.

```bash
FRONTEND_PORT=13000 BACKEND_PORT=18080 DB_PORT=25432 docker compose up --build
```

`BACKEND_PORT`는 frontend의 `NEXT_PUBLIC_API_BASE_URL` build argument에 반영되므로 port 변경 시 `--build`가 필요합니다. DB는 `dbdata` named volume에 유지됩니다. local data를 완전히 초기화할 때만 다음 명령을 사용합니다.

```bash
docker compose down -v --remove-orphans
```

## 기술 선택

### 과제를 해석한 기준

AGE-R 앱을 직접 사용하고 공식 앱 소개를 교차 확인했습니다. 메디큐브톡은 제품 사용 경험과 질문을 확인·공유하는 공간이고, 과제의 seed와 API도 게시글·댓글·좋아요를 중심으로 구성됩니다.[^age-r-app] 그래서 사람 관계를 소비하는 SNS 기능을 넓히기보다, 글과 대화를 먼저 읽고 다시 찾을 수 있는 community로 해석했습니다.

APR이 제공한 범위는 Java 17/Spring Boot 3.5와 JDBC·PostgreSQL 연결, `GET /health`, CORS, Compose, `seed/seed.json`, Next.js 15 App Router 골격, `/dev-user`, avatar와 Pretendard 자산입니다. 그 위에 schema, seed lifecycle, REST API, soft delete·ownership, keyset pagination, server-state cache, CRUD·댓글·좋아요, infinite scroll, 검색·정렬과 375px UI를 구현했습니다. 로그인과 댓글 수정은 과제 범위대로 추가하지 않았습니다.

### 선택과 수용한 비용

| 선택 | 해결한 문제와 선택 이유 | 수용한 비용 |
| --- | --- | --- |
| Spring `JdbcClient` | raw JDBC를 직접 조립한 것이 아니라 Spring `JdbcClient`를 사용했습니다. soft delete, requester별 `isLiked`, derived like count, keyset cursor와 locking SQL을 직접 읽고 검증할 수 있어 현재 규모에서는 query visibility를 우선했습니다.[^spring-jdbc-client] | ORM의 entity mapping·dirty checking·repository abstraction 편의를 포기하고 SQL과 row mapping을 직접 관리합니다. |
| Flyway | migration ordering과 checksum 이력으로 빈 PostgreSQL boot와 schema evolution을 재현하고, 고정 seed lifecycle과 schema lifecycle을 분리했습니다.[^spring-flyway] | 도입 복잡도는 작지만 적용된 migration을 수정하지 않고 별도 migration lifecycle을 계속 관리해야 합니다. |
| TanStack Query | 목록·상세·댓글 server state와 mutation cache를 관리하고, 서버 응답으로 증명 가능한 변경은 exact patch, membership·ordering 변화는 invalidate합니다.[^tanstack-cancellation] | query key와 invalidation policy를 application이 명시하고 모든 cache 조합을 검증해야 합니다. |
| CSS Modules | 375px 디자인을 component-local하게 정밀 조정하고 class 충돌을 막았습니다. | 별도 UI framework abstraction을 만들지 않아 일부 CSS 선언을 반복합니다. |

선택의 후보와 변화 과정, primary source 전체는 [Engineering Notes](docs/ENGINEERING_NOTES.md)에 정리했습니다.

## 상태 설계

### Server state와 local state

게시글 목록·상세, 댓글 page와 좋아요는 서버가 원본이므로 TanStack Query cache에 둡니다. 폼 입력, 검색창·popover·dialog 열림, 댓글 “더보기” gate처럼 화면 수명에만 필요한 값은 local state입니다. 이 경계 덕분에 UI용 global store를 추가하지 않았습니다.

query key에는 page size, `latest`/`likes`, 정규화된 검색어가 들어가고 cursor는 infinite query의 `pageParam`으로 분리됩니다. mutation 뒤에는 다음 규칙을 적용했습니다.

- 서버 응답만으로 exact result를 증명할 수 있으면 loaded cache를 직접 갱신합니다.
- membership이나 ordering이 바뀔 수 있으면 invalidate해 서버에 다시 묻습니다.
- 따라서 latest 목록의 loaded 좋아요·댓글 수는 직접 반영하지만, likes-sort의 순위와 수정 뒤 검색 membership은 invalidate합니다.
- 모든 변경은 server-confirmed입니다. optimistic rollback 복잡도를 피하는 대신 한 network round-trip을 수용했습니다.

규칙은 `frontend/lib/query/postListCache.ts`에 모으고 여러 sort/search cache 조합을 Vitest로 검증했습니다.

목록 cache는 하나가 아닙니다. latest, likes, 검색어별 infinite query가 동시에 존재할 수 있고 상세 query도 같은 post를 갖습니다. 그래서 mutation helper는 현재 화면만 고치지 않고 matching query 전체를 순회합니다. 생성·수정·삭제·좋아요·댓글 변화 뒤 148개 cached-list 조합을 검증해 loaded row, total, membership과 ordering이 규칙대로 유지되는지 확인했습니다. cache를 쓴다는 사실보다 “어떤 결과를 client가 증명할 수 있는가”를 명시한 것이 핵심입니다.

### Cursor, 검색과 request boundary

게시글과 댓글은 `(createdAt, id)`의 keyset cursor를 사용합니다. ID가 같은 timestamp의 tie-breaker가 되어 순서가 deterministic하고, `limit + 1`행으로 다음 page를 판정합니다. 처음 사용한 OR predicate는 결과는 정확했지만 깊은 comment page에서 filter row와 buffer가 증가했습니다. API 형식을 유지한 채 PostgreSQL row-value comparison `(created_at, id) < (?, ?)`으로 바꾸자 조건이 `Index Cond`로 내려가 가장 깊은 측정에서도 buffer가 약 3으로 유지됐습니다.[^postgres-row-comparison]

변경 전 deepest measurement에서는 제거한 row가 1,459개, buffer가 42까지 늘었고 row-value 변경 뒤 buffer는 약 3이었습니다. 작은 local query의 절대 시간보다 page가 깊어져도 scan work가 함께 커지지 않는 구조를 선택했습니다. 대신 cursor는 page number처럼 읽기 쉽지 않고 임의 page 이동에는 적합하지 않습니다.

검색은 이미 받은 client page가 아니라 backend에서 수행합니다. `\`, `%`, `_`를 literal로 escape한 bound parameter를 사용하고, latest와 `(likeCount, createdAt, id)` likes cursor는 codec을 분리해 다른 정렬의 cursor를 거절합니다.

검색 입력에는 300ms debounce를 두되, 이것만으로 이미 시작한 요청이 멈추지는 않으므로 TanStack Query의 `AbortSignal`을 공통 `request()`에서 실제 `fetch`까지 전달합니다. read는 network·5xx에 한 번만 retry하고 4xx와 mutation은 retry하지 않습니다. frontend validation은 빠른 feedback이고, Unicode 길이·blank·소유권의 최종 판단은 backend service가 담당합니다.

실제 browser에서 다섯 글자를 연속 입력했을 때 확정 검색 요청은 한 번만 발생했고, 의도적으로 느리게 만든 이전 검색은 abort된 뒤 최종 결과를 덮지 않았습니다. 상세·최초 댓글 요청도 navigation 시 취소되며 cancellation은 `NETWORK_ERROR`나 error UI로 바뀌지 않습니다. debounce와 cancellation을 같은 최적화로 취급하지 않고, 각각 “요청을 만들지 않음”과 “이미 시작했지만 불필요해진 요청을 중지함”으로 나눴습니다.

### 측정으로 정한 요청 단위

초기 상수를 관례로 확정하지 않고, 후보 차이를 드러내기 위한 constrained condition에서 하나씩 비교했습니다. 조건은 **375×778 Chromium, CPU 4× slowdown, 다운로드 약 1.47Mbps**였습니다. 이는 평균 모바일 환경을 주장하는 값이 아니라 동일 후보를 비교하기 위한 고정 조건입니다.

| 항목 | 후보 | 선택 | 결정적 관찰 |
| --- | --- | ---: | --- |
| Post batch | `10 / 15 / 20` | `15` | 10은 후속 요청·loading 노출이 늘고 20은 SSR/continuation에서 일관된 이득이 없었습니다. |
| Post `rootMargin` | `200 / 300 / 400 / 500px` | `500px` | 200px은 빠른 scroll에서 최대 약 0.3초 기다렸고, 500px은 해당 조건에서 wait·blank·scroll 전 요청이 없었습니다. |
| Comment continuation | `10 / 20 / 30 / 50` | `30` | 10은 요청이 많고 50은 첫 반영과 over-fetch가 컸습니다. 30은 120개까지 5회 요청, 빠른 소비 중 loading 노출 1회였습니다. |
| Search debounce | `200 / 300 / 500ms` | `300ms` | 짧은 입력 pause의 요청은 흡수하면서 500ms보다 최종 결과를 약 200ms 빨리 보였습니다. |

최초 댓글 `limit=2`는 실험 대상이 아니라 과제 계약입니다. 재방문도 다시 2개에서 시작하고 사용자가 “더보기”를 누르기 전에는 continuation을 요청하지 않습니다. 반복 run, 160-post dataset과 SQL plan은 [Performance](docs/PERFORMANCE.md)에 있습니다.

현재 runtime contract는 게시글 최초·후속 page 모두 15개, 게시글 선조회 500px, 댓글 최초 2개·후속 30개, 댓글 선조회 200px입니다. 적용 뒤 clean seed의 39 readable posts는 `15 / 15 / 9`로 끝났고, 1,411-comment post도 최초 2개 이후 30개 cursor page로 중복·누락 없이 종료했습니다. 전체 데이터 크기에 맞춰 고정한 값이 아니라 한 session에서 사용자가 소비하는 깊이와 요청 시점을 기준으로 정한 값입니다.

## 구조

변경 이유가 다른 책임을 분리했습니다. UI affordance는 보안 경계가 아니며 service authorization이 최종 판단입니다.

```text
Backend
HTTP Controller
  → Read / Mutation Service      검증·소유권·transaction
    → Repository                 SQL·row mapping
      → PostgreSQL

Frontend
App Router page                  route 경계
  → features/*                   목록·상세·form 사용자 흐름
    → lib/api · query · hooks    통신·cache·공통 동작
```

backend의 `RequesterResolver`, `MutationValidator`, cursor codec과 error handler는 횡단 규칙을 한곳에 둡니다. frontend page는 route parameter를 feature에 연결하는 얇은 경계이고, query key와 cache 갱신은 `frontend/lib/query`가 담당합니다. 실제 class/file 책임과 request 흐름은 [Architecture](docs/ARCHITECTURE.md)에 있습니다.

대표 read와 mutation은 다음 경계를 통과합니다.

```text
GET /posts
→ PostReadService
→ latest / likes keyset cursor
→ JdbcClient
→ PostgreSQL

post / comment / like mutation
→ validation + readability / ownership check
→ 필요한 post·aggregate row를 SELECT ... FOR UPDATE
→ mutation commit
→ TanStack Query exact patch 또는 invalidate
```

- Controller는 HTTP parameter/body와 response boundary만 다룹니다.
- Read service는 cursor와 조회 계약을, mutation service는 validation·ownership·transaction을 담당합니다.
- Repository는 HTTP status를 결정하지 않고 SQL primitive와 row mapping만 제공합니다.
- App Router page는 server route shell을 유지하고 cookie와 interaction이 필요한 화면만 client feature로 격리합니다.

이 정도의 경계는 layer 수를 늘리기 위한 것이 아니라 direct API call에도 같은 validation과 authorization을 적용하고, SQL 변경이 HTTP·UI 규칙과 섞이지 않게 하기 위한 최소 구분입니다.

## 데이터 적재

### 입력 사실을 보존하는 model

seed에는 **42 posts / 2,076 comments**가 있고 soft-deleted row를 제외하면 **39 posts**를 읽을 수 있습니다. seed가 가진 정보와 API가 요구하는 정보를 임의 데이터로 채우지 않고 schema에 분리했습니다.

| 입력과 요구 | model | 이유 |
| --- | --- | --- |
| 삭제 ID는 있지만 삭제 시각은 없음 | `posts/comments.is_deleted` | 존재하지 않는 `deleted_at`을 만들지 않고 physical row를 보존합니다. |
| seed는 like 총수만 제공 | `base_like_count + post_likes` | 알 수 없는 사용자 관계를 만들지 않고 runtime requester 관계만 별도 저장합니다. |
| seed ID 보존 + runtime 자동 ID | `GENERATED BY DEFAULT AS IDENTITY` | explicit/implicit ID를 함께 지원하고 seed 뒤 sequence를 최대 ID에 맞춥니다. |

post/comment `DELETE`는 physical delete가 아니라 `is_deleted = TRUE` update입니다. unreadable parent의 child 접근도 막지만 child row를 함께 삭제 상태로 다시 쓰지는 않습니다.

원본 기준으로 posts 3개와 comments 81개가 deleted 상태입니다. readable count와 comment count는 항상 `is_deleted = FALSE`를 기준으로 계산합니다. seed like에는 requester 정보가 없으므로 초기 `isLiked`는 누구에게나 false이고, 이후 toggle에서 생긴 `post_likes(post_id, user_id)`만 requester별 상태를 만듭니다.

### Schema와 seed lifecycle

Flyway는 schema version을, `SeedInitializer`와 `seed_history`는 고정 입력의 적재 여부를 관리합니다.

```text
Flyway migration
  → SeedInitializer transaction
  → seed/seed.json을 원본 ID로 삽입
  → deleted ID 반영
  → identity sequence 동기화
  → seed_history: assignment-v1
  → commit
  → ready
```

posts/comments와 marker는 함께 commit 또는 rollback됩니다. marker가 있으면 재기동 때 seed를 다시 써 runtime data를 되돌리지 않습니다. 실패하면 marker가 없으므로 다음 시작에서 적재와 sequence sync를 다시 시도합니다.

`BY DEFAULT` identity는 explicit seed ID를 보존하면서 runtime insert에는 sequence 값을 제공합니다. explicit insert가 sequence를 자동으로 최대 ID까지 옮기지는 않으므로 marker 기록 전에 posts/comments sequence를 각각 최대 ID와 동기화합니다. clean PostgreSQL에서 첫 runtime post 43, comment 2,077이 충돌 없이 생성되는 것을 integration과 clean-clone acceptance에서 확인했습니다.

### 실제 race에서 추가한 readiness

초기 `/health`는 “DB에 query 가능”만 확인했습니다. clean start에서 **health는 200인데 post API `total`은 0**인 짧은 구간을 관찰했습니다. process가 살아 있다는 사실과 seed까지 commit되어 요청을 받을 준비가 됐다는 사실을 같게 본 것이 원인이었습니다.

그래서 새 적재와 재기동 경로 모두 seed transaction의 `afterCommit`에서만 `SeedReadiness`를 열었습니다. 그 전에는 DB가 연결되어도 `/health`가 503 `starting`을 반환합니다. 처음부터 복잡한 readiness 체계를 만든 것이 아니라, 재현된 race를 기준으로 작은 gate를 추가한 사례입니다.

## 개선한 점 · 남은 과제 (선택)

### 정적 디자인을 runtime 제품으로 연결

공식 Figma의 typography·색상·핵심 화면 의미를 유지하면서 정의되지 않은 상태는 글·대화 중심 community 해석과 375×778 실행 결과로 보완했습니다. 목록은 제목 → 본문 preview → 작성자·반응·시간의 content-first hierarchy를 사용하고, 검색과 latest/likes sort는 한 줄 discovery toolbar에 배치했습니다. 수정·삭제 두 command에는 full-width bottom sheet보다 trigger와 관계가 분명한 non-modal popover를 선택하되 destructive delete는 별도 confirmation dialog로 유지했습니다.

최초·후속 loading/error/retry와 mutation pending도 정적 디자인에 없다는 이유로 생략하지 않았습니다. 다만 이를 공식 Figma와 pixel 단위로 일치한다고 주장하지 않습니다. design/product decision 구분은 [UI Spec](docs/UI_SPEC.md)에 있으며 최종 공식 비교 V-081은 `PASS`입니다. 근거와 전체 coverage는 [Official Figma Comparison](docs/DESIGN_COMPARISON.md)에 기록했습니다.

아래 Official 이미지는 V-081에서 현재 file/node mapping을 다시 확인한 공식 Figma frame의 repository-preserved export이고, 오른쪽은 production Compose의 Chromium 375×778 구현입니다.

<table>
  <tr><th colspan="2">목록</th></tr>
  <tr><th>Official Figma</th><th>Current implementation</th></tr>
  <tr>
    <td><img src="design/design-export/755-8831-post-list.png" width="165" alt="Official Figma post-list frame export"></td>
    <td><img src="docs/assets/design-comparison/post-list-runtime.png" width="165" alt="Current post list at 375 by 778"></td>
  </tr>
  <tr><td colspan="2">공식 typography·색상·FAB basis를 유지하고, discovery toolbar와 content-first hierarchy, 108px row rhythm을 적용했습니다.</td></tr>
  <tr><th colspan="2">상세</th></tr>
  <tr><th>Official Figma</th><th>Current implementation</th></tr>
  <tr>
    <td><img src="design/design-export/755-12645-detail-other-many-comments.png" width="165" alt="Official Figma post-detail frame export"></td>
    <td><img src="docs/assets/design-comparison/post-detail-runtime.png" width="165" alt="Current post detail at 375 by 778"></td>
  </tr>
  <tr><td colspan="2">title·body·like·comments·More·composer 구조를 유지하면서 byline과 metadata hierarchy를 compact하게 다듬었습니다.</td></tr>
  <tr><th colspan="2">Post menu</th></tr>
  <tr><th>Official Figma</th><th>Current implementation</th></tr>
  <tr>
    <td><img src="design/design-export/830-139-detail-own-action-sheet.png" width="165" alt="Official Figma own-post action sheet frame export"></td>
    <td><img src="docs/assets/design-comparison/post-menu-runtime.png" width="165" alt="Current own-post anchored popover at 375 by 778"></td>
  </tr>
  <tr><td colspan="2">Figma의 scrim + bottom sheet를 두 command와 trigger의 공간 관계가 분명한 anchored popover로 바꿨습니다. destructive delete는 별도 confirmation dialog를 유지합니다.</td></tr>
</table>

**12 official frames + shared edit state를 비교했고, V-081은 `PASS`, accidental mismatch는 `0`입니다.** 차이의 기존 rationale와 전체 matrix는 [Official Figma Comparison](docs/DESIGN_COMPARISON.md)에 있습니다.

### `aria-modal` 이후에도 확인한 keyboard focus

첫 dialog는 `role="alertdialog"`, `aria-modal`, label과 open focus를 제공했습니다. 실제 Chromium에서 Tab/Shift+Tab을 따라가자 focus가 dialog 뒤의 navigation·좋아요·댓글 입력으로 빠졌고, pending 중 button이 disabled되면 실패 후에도 focus가 `<body>`에 남았습니다.

버튼 두 개짜리 dialog에 새 modal library를 추가하지 않고 공용 `ConfirmDialog`에서만 focus를 순환시키고, pending 중 background 이동을 막고, 실패 시 cancel과 close 시 trigger로 focus를 복구했습니다. 이 흐름은 현재 Playwright가 실제 keyboard로 회귀 검증합니다.

### Framework default도 runtime에서 재확인

- 댓글 cache를 기본 수명으로 두자 “더보기” 후 SPA 재방문에서 cached pages가 최초 2개 계약을 우회했습니다. 댓글 query에만 `gcTime: 0`을 적용해 방문마다 `limit=2`와 닫힌 gate에서 시작합니다. 긴 댓글을 다시 요청하는 비용을 수용했습니다.
- 목록 전체를 순회하자 dynamic detail prefetch가 40회 발생했지만 route shell은 약 40–150B였고 실제 click도 빨라지지 않았습니다. 반복되는 detail link만 `prefetch={false}`로 바꾸고 작성처럼 정적이며 재사용 가치가 있는 link는 기본 동작을 유지했습니다.

### Correctness와 verification의 evolution

boundary를 DB 없이 확인할 수 있는 규칙, 실제 PostgreSQL이 필요한 규칙, browser가 필요한 동작으로 나눴습니다.

| 영역 | 대표 evidence | 현재 경계·결과 |
| --- | --- | --- |
| Backend | controller/service/cursor/seed | 18 suites / 91 tests |
| PostgreSQL integration | 실제 Flyway·SQL·constraint·locking/concurrency | Testcontainers 6 suites / 31 tests |
| Frontend | ID parsing, query key, cache helper, retry boundary | Vitest 4 files / 21 tests |
| Browser | Chromium 375×778 | Playwright 3 core flows |
| Production runtime | Docker Compose build/start, health·API·SSR·seed marker·browser API base | `full-stack-smoke` PASS |
| Design | 12 official frames + shared edit state | V-081 `PASS`, accidental mismatch 0 |
| Reproducibility | 빈 clone·새 volume·재기동 persistence | clean-clone gate VERIFIED |

동시 mutation은 service의 실제 transaction과 `SELECT ... FOR UPDATE`를 Testcontainers PostgreSQL에서 여러 worker로 실행해 검증했습니다. mock으로 “lock method가 호출됐다”만 확인하지 않고, worker가 DB lock을 기다리는 상태와 lock 제거 시 실패하는 sensitivity도 확인했습니다.

`full-stack-smoke`는 production Compose를 build/start하고 health, API, SSR, seed marker와 browser API base를 확인한 뒤 Chromium **375×778 Playwright 3 core flows**를 실행합니다.

1. seed 기반 검색·좋아요순·mobile invariant와 horizontal overflow
2. 최초 댓글 2개 → More(30개) → SPA 재방문 시 다시 2개
3. delete dialog keyboard focus, Escape/trigger 복귀와 confirm 전 DELETE 부재

Playwright는 workers 1, retries 0이며 실패 screenshot과 trace를 `playwright-failure-artifacts`로 보존합니다. 이는 수동 acceptance 전체를 대체하지 않습니다. 전체 cursor traversal, 모든 failure injection과 CRUD/ownership 조합, Firefox/WebKit, visual regression, Lighthouse CI는 여전히 [Verification](docs/VERIFICATION.md)에 범위와 한계를 구분해 기록했습니다.

Playwright 밖의 runtime acceptance에는 다음이 남아 있습니다.

- 39 readable posts와 1,411 comments의 cursor 완전 순회, timestamp tie와 종료 뒤 추가 요청 부재
- header/cookie requester 우선순위, 다른 사용자 ownership 거절과 requester별 like 격리
- 삭제 row의 물리 보존, unreadable parent의 child 접근 차단과 mutation race
- 검색·정렬 조합, 느린 request cancellation과 각 loading/error/retry state
- clean volume·alternate ports·재기동 persistence와 seed/sequence 멱등성

자동화하지 않았다는 사실과 검증하지 않았다는 사실을 구분했습니다. 반복 비용과 회귀 위험이 큰 규칙은 unit/integration/Playwright로 올리고, 넓지만 변경 빈도가 낮은 acceptance matrix는 실행 조건과 결과를 문서에 남겼습니다.

대표 mobile Lighthouse 재측정의 median은 Performance **93**, LCP 약 **2.275s**, Accessibility·Best Practices·SEO **100**이었습니다. 특정 local/CI-like 조건의 회귀 evidence이며 모든 실제 기기 성능을 일반화하지 않습니다.

### 현재 tradeoff와 확장 조건

- **긴 댓글 virtualization:** 1,411개를 모두 적재하면 약 10,000 DOM elements, 약 1,000개 이후 append는 50ms 이상·최대 약 80ms였지만 측정 환경에서 scroll 반응 저하는 없었습니다. 더 긴 실제 대화나 저사양 기기에서 체감 문제가 확인되면 visible window만 mount하는 방식을 검토합니다.
- **변하는 좋아요 순위:** rank가 page 요청 사이에 움직이면 duplicate 또는 missing row가 생길 수 있습니다. duplicate는 client가 ID로 제거하지만 이미 cursor 위로 이동한 missing row는 현재 traversal에서 복구하지 못합니다. assignment 규모에서는 snapshot/version 비용보다 이 한계를 수용했습니다.
- **Requester는 인증이 아님:** `x-user-id` header/cookie는 과제의 ownership contract이며 production authentication·session 보안을 제공하지 않습니다.
- **검색 규모:** readable 39 posts에는 escaped substring search가 충분합니다. latency나 품질 문제가 실제 query에서 나타나면 PostgreSQL full-text search와 index를 측정합니다.
- **검증 환경:** browser automation과 visual 확인은 Chromium 375×778 중심입니다. Firefox/WebKit visual regression, production traffic/load·observability·CD는 검증 범위가 아닙니다.

장기적으로 대화가 충분히 쌓여 좋은 사용법이 여러 글에 흩어지는 문제가 생기면, 반복 참조되는 경험을 `제품 → 순서 → 부위 → 사용량 → 주의사항` 형태의 따라 할 수 있는 guide로 연결할 수 있습니다. 현재 저장소에는 이 domain이 없으며 실제 사용 근거가 생긴 뒤 검토할 제품 방향입니다.

문서별 기준은 다음과 같습니다: [Engineering Notes](docs/ENGINEERING_NOTES.md)는 결정 과정, [Architecture](docs/ARCHITECTURE.md)는 현재 구조, [Requirements](docs/REQUIREMENTS.md)는 traceability, [Verification](docs/VERIFICATION.md)은 실행 evidence, [Performance](docs/PERFORMANCE.md)는 raw benchmark를 담당합니다.

[^age-r-app]: [Google Play — 에이지알 메디큐브 디지털클리닉 AGE-R](https://play.google.com/store/apps/details?hl=ko&id=com.apr.digitalclinic)
[^spring-jdbc-client]: [Spring Framework — Unified JDBC Query/Update Operations: `JdbcClient`](https://docs.spring.io/spring-framework/reference/data-access/jdbc/core.html#jdbc-JdbcClient)
[^spring-flyway]: [Spring Boot — Execute Flyway Database Migrations on Startup](https://docs.spring.io/spring-boot/how-to/data-initialization.html#howto.data-initialization.migration-tool.flyway)
[^tanstack-cancellation]: [TanStack Query — Query Cancellation](https://tanstack.com/query/latest/docs/framework/react/guides/query-cancellation)
[^postgres-row-comparison]: [PostgreSQL — Row Constructor Comparison](https://www.postgresql.org/docs/current/functions-comparisons.html#ROW-WISE-COMPARISON)
