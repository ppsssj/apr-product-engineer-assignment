# 엔지니어링 노트

이 문서는 메디큐브톡 과제를 구현하며 내린 선택의 결과보다 그 선택에 도달한 과정을 기록한다. 현재 구조는 [ARCHITECTURE.md](ARCHITECTURE.md), 실행 근거와 검증 범위는 [VERIFICATION.md](VERIFICATION.md), 반복 측정과 SQL plan은 [PERFORMANCE.md](PERFORMANCE.md), 공식 Figma 최종 비교는 [DESIGN_COMPARISON.md](DESIGN_COMPARISON.md)가 각각 기준이다. 여기서는 공식 자료와 실행 결과를 어떻게 연결했고, 실제 문제가 확인된 뒤 어느 정도까지 변경했는지에 집중한다.

## 1. 과제 해석과 구현 범위

APR이 제공한 것은 Java 17/Spring Boot 3.5와 JDBC·PostgreSQL 연결, `GET /health`, CORS, Docker Compose, `seed/seed.json`, Next.js 15 App Router 골격, `/dev-user`, 아바타와 Pretendard 자산이었다. community schema, seed 적재, 도메인 API, 상태 관리와 실제 화면 동작은 구현 범위였다.

AGE-R 앱을 직접 사용하고 공식 앱 소개를 함께 확인했다. 앱에는 루틴과 전·후 사진뿐 아니라 사용자가 제품 경험과 질문을 나누는 메디큐브톡이 있고, 과제의 seed와 API도 게시글·댓글·좋아요에 집중한다. 따라서 이 과제는 사람 관계 자체를 소비하는 SNS보다 제품 사용 경험과 질문이 축적되고 다시 탐색되는 글·대화 중심 커뮤니티로 해석했다. 이 해석은 다음 범위를 정하는 기준이 됐다.

- 제목과 본문을 먼저 읽는 content-first 목록을 사용한다.
- 최신순 계약을 유지하면서 검색과 좋아요순 탐색을 보완한다.
- 데이터가 뒷받침하지 않는 팔로우 graph, 프로필, 개인화 feed는 만들지 않는다.
- 댓글 수정과 로그인은 과제 계약 밖이므로 추가하지 않는다.
- 빈 DB 재현, cursor 경계, mutation 후 cache 일관성과 모바일 긴 목록을 화면 수만큼 중요하게 본다.

## 2. 판단 방법: 공식 자료 → runtime 관찰 → 측정 → 최소 변경

처음부터 범용 production architecture를 목표로 기능을 추가하지 않았다. 먼저 가장 단순한 구현으로 계약을 만족시키고, 불확실한 지점은 공식 문서와 실제 실행으로 확인했다. 문제가 재현되면 후보를 한 번에 하나씩 바꿔 비교하고, 현재 범위에서 필요한 만큼만 수정했다.

대표적인 흐름은 다음과 같다.

```text
DB query 성공만으로 /health 200
  → clean start에서 posts total=0 관찰
  → seed transaction afterCommit까지 readiness를 닫음

rootMargin 200px
  → 제한 조건의 빠른 scroll에서 최대 약 0.3초 대기
  → 200/300/400/500px 비교
  → 게시글 목록만 500px로 변경

aria-modal dialog
  → Chromium Tab 이동이 배경으로 빠짐
  → 공용 ConfirmDialog에 작은 focus trap 추가

수동 browser acceptance
  → 핵심 흐름에 지속적인 회귀 보호가 없음
  → 위험이 큰 3개 흐름만 Playwright CI로 승격
```

모든 숫자는 보편적인 모바일 평균이나 영구 최적값이 아니라, 후보 간 차이를 드러내기 위해 고정한 조건의 결과다. 새로운 데이터 규모나 실제 기기 분포가 달라지면 다시 측정할 수 있도록 결정 조건과 한계도 함께 남겼다.

## 3. `JdbcClient`와 persistence 경계

읽기 규칙은 단순 row CRUD보다 다음 SQL 의미에 가까웠다.

- 삭제되지 않은 게시글·댓글만 읽는다.
- 댓글 수는 readable row로부터 계산한다.
- seed 좋아요 집계와 runtime requester 관계를 합산한다.
- latest와 likes 정렬에 서로 다른 cursor bound를 적용한다.
- 제목·본문 검색에서 wildcard를 literal로 처리한다.

도메인이 게시글·댓글·좋아요로 작고 query 의미와 실행 계획을 직접 리뷰하는 편이 중요해 Spring `JdbcClient`와 명시적 SQL repository를 유지했다. raw JDBC를 직접 조립한 것이 아니라 Spring JDBC 위의 `JdbcClient`를 사용했다. `JdbcClient`는 named/positional parameter query와 update를 fluent API로 제공해 starter의 Spring JDBC 경계 안에서 필요한 query를 읽기 쉽게 표현했다.

JPA로 같은 기능을 구현할 수 없어서 선택한 것은 아니다. 이 작업에서는 soft-delete predicate, requester별 `isLiked`, derived count, 검색과 정렬별 cursor가 추가될 때 SQL을 설계 대상으로 두는 편이 맞았다. 그 결과 cursor predicate가 filter로 실행되는 사실을 `EXPLAIN`으로 확인하고 API 계약을 바꾸지 않은 채 repository SQL만 교체할 수 있었다. 대신 row mapping, query 조합과 PostgreSQL 종속 동작을 직접 관리하는 비용을 수용했다.

HTTP parsing은 controller, transaction·validation·ownership은 service, SQL과 row mapping은 repository가 담당한다. UI에서 버튼을 숨기는 것은 authorization이 아니므로 최종 소유권 판단은 항상 backend service에 남겼다.

## 4. Flyway, schema, seed lifecycle

평가자는 빈 PostgreSQL에서 시작할 수 있지만 schema 변경 이력과 고정 과제 데이터의 적재 여부는 서로 다른 문제다. 둘을 애플리케이션 초기화 한 단계에 섞으면 구조가 적용됐는지와 `seed.json`이 이미 반영됐는지를 구분하기 어렵다.

- Flyway의 `V1__create_community_schema.sql`은 구조와 checksum 이력을 관리한다.
- `SeedInitializer`는 고정 `seed/seed.json`을 transaction 안에서 적재한다.
- `seed_history`의 `assignment-v1` marker는 이 과제 seed가 완료됐는지를 기록한다.
- marker가 있으면 재기동 시 runtime에서 생성·수정된 데이터를 seed로 덮어쓰지 않는다.

```text
Flyway schema migration
  → SeedInitializer(ApplicationRunner)
  → seed JSON 파싱
  → 원본 ID로 posts/comments 삽입
    (deleted ID에 속한 row는 삽입 시 is_deleted = TRUE)
  → identity sequence 동기화
  → seed_history assignment-v1 기록
  → transaction commit
  → SeedReadiness afterCommit에서 ready
```

posts/comments와 marker는 함께 commit되거나 rollback된다. 실패를 완료로 기록하지 않고, marker가 없으면 다음 시작에서 전체 적재와 sequence sync를 다시 시도한다. versioned production content migration까지 일반화하지 않고, 고정 과제 입력에는 하나의 marker가 충분하다고 판단했다.

seed를 Flyway migration에 넣지 않은 이유는 재실행 때문이 아니다. versioned migration은 이미 적용된 version을 다시 실행하지 않는다. 핵심은 역할 분리다. Flyway는 schema lifecycle을, `SeedInitializer`는 과제가 제공한 JSON의 적재 lifecycle을 맡는다. 이렇게 해야 `seed/seed.json`이 유일한 과제 seed 원본으로 남고, 같은 데이터를 SQL로 한 벌 더 관리하지 않는다.

## 5. soft delete, base likes, runtime likes

### Soft delete

과제는 삭제된 resource가 화면과 API에서는 사라지되 DB row는 물리적으로 남도록 요구한다. seed에는 삭제 ID만 있고 삭제 시각은 없으므로 존재하지 않는 시간을 발명하는 `deleted_at` 대신 `is_deleted` boolean을 사용했다.

모든 normal read와 count에 readable predicate를 명시해야 하는 비용이 있지만, seed 삭제 상태와 runtime 삭제를 같은 model로 표현한다. 게시글을 삭제할 때 child 댓글을 함께 변경하지 않는다. 댓글 자체가 삭제된 상태와 parent가 unreadable해서 접근할 수 없는 상태를 구분하되 제품 API에서는 둘 다 노출하지 않는다. 따라서 Playwright의 disposable post cleanup도 production `DELETE` endpoint를 호출하지만 실제 repository 동작은 physical delete가 아니라 `is_deleted = TRUE` update다.

### Likes

seed는 좋아요 총수만 제공하고 누가 눌렀는지는 제공하지 않는다. 존재하지 않는 사용자 관계를 만들어 채우지 않고 알려진 사실을 분리했다.

```text
likeCount = posts.base_like_count + COUNT(post_likes rows)
```

seed 집계는 `base_like_count`, 애플리케이션에서 새로 생긴 requester 관계는 `post_likes(post_id, user_id)`에 저장한다. composite primary key가 중복 좋아요 관계를 막고 `isLiked`는 현재 requester row의 존재로 계산한다. 표시할 때 aggregate가 필요하지만 제공 데이터 규모에서는 mutable counter와 관계의 동기화 위험보다 의미가 명확하다.

## 6. identity와 sequence 동기화

seed ID는 URL과 관계의 기준이어서 보존해야 하고, runtime row는 자동으로 ID를 받아야 한다. `GENERATED BY DEFAULT AS IDENTITY`는 explicit seed ID와 implicit runtime ID를 같은 column에서 지원한다.

PostgreSQL sequence는 explicit insert만으로 최대 ID까지 전진하지 않으므로 `IdentitySequenceRepository.synchronizeWithSeededRows()`가 seed insert 뒤 sequence를 각 table의 최대 ID에 맞춘다. 별도 ID allocator를 만들지 않는 대신 이 sync가 seed transaction의 필수 단계가 됐다. integration test는 seed 뒤 새 post/comment ID가 충돌 없이 이어지는지도 실제 PostgreSQL에서 확인한다.

## 7. cursor pagination과 SQL plan의 변화

댓글은 한 게시글에 최대 1,411개까지 몰려 있어 전체 조회를 피해야 했다. `OFFSET`은 앞쪽 insert 동안 page 경계가 흔들리고 깊은 page에서도 skipped row를 계산한다. 임의 page 이동보다 이미 읽은 항목 다음을 안정적으로 이어 받는 것이 중요한 모바일 infinite list라서 keyset pagination을 사용했다.

정렬은 `(created_at DESC, id DESC)`로 고정한다. 같은 timestamp에서도 ID가 tie-breaker가 되어 순서가 deterministic하고, cursor는 두 값을 URL-safe Base64로 encode한다. repository는 `limit + 1`행으로 다음 page 존재 여부를 판정한다.

첫 predicate는 의미가 직접적인 OR 형태였다.

```sql
created_at < :cursorCreatedAt
OR (created_at = :cursorCreatedAt AND id < :cursorId)
```

전체 cursor traversal과 timestamp tie fixture는 정확했다. 하지만 이번 query·index·seed의 `EXPLAIN (ANALYZE, BUFFERS)`에서는 cursor 조건이 composite index의 경계가 아니라 `Index Cond: post_id = …` 뒤의 `Filter`로 평가되는 것을 관찰했다. post #2에서 page가 깊어질수록 `Rows Removed by Filter`가 `3 → 728 → 1,459`, buffer가 `6 → 23 → 42`로 늘었다. 계약상으로는 keyset이었지만 실행은 OFFSET처럼 동작한 것이다. API correctness와, keyset의 구조적 이점을 실제 DB 실행이 얻는지는 별개의 문제였다.

API와 cursor 형식은 유지하고 predicate만 PostgreSQL row-value comparison으로 바꿨다.

```sql
(created_at, id) < (:cursorCreatedAt, :cursorId)
```

조건이 `Index Cond`로 내려가 첫 continuation부터 가장 깊은 page까지 buffer가 3–4로 유지됐다(가장 깊은 page 3). 가장 깊은 page의 측정 SQL은 약 `0.285ms → 0.112ms`였지만, 작은 로컬 데이터의 절대 시간보다 page 깊이에 따라 scan work가 자라지 않는 구조를 얻은 것이 핵심이다. API와 cursor 계약은 바뀌지 않았고, 변경 뒤 전체 traversal과 tie fixture를 다시 실행해 결과가 같음을 확인했다. raw plan은 [PERFORMANCE.md §12](PERFORMANCE.md#12-selected-optimization--row-value-keyset)에 남아 있다.

likes 정렬은 `(likeCount, createdAt, id)` cursor를 별도로 사용한다. `likeCount`가 mutation으로 움직일 수 있어 긴 동시 순회의 snapshot까지 보장하지 않으며, frontend는 같은 post ID가 중복으로 도착하면 첫 항목만 렌더링한다.

## 8. search escaping과 sort cursor

이미 받은 client page만 검색하면 “결과 없음”이 전체 데이터에 대한 결론처럼 보일 수 있어 제목·본문 검색을 server-side로 구현했다. 입력의 `\`, `%`, `_`를 먼저 escape하고 bound parameter와 `ILIKE ... ESCAPE '\'`를 사용한다. 검색어를 SQL 문자열로 조립하지 않는다.

latest와 likes cursor codec은 분리되어 다른 ordering의 cursor를 재사용하면 400으로 거절한다. frontend query key도 정규화된 검색어와 sort를 포함해 각 조합이 독립된 첫 page와 cursor chain을 갖는다. 기본 latest 계약은 유지하고 검색과 likes는 선택 가능한 탐색 경로로만 추가했다.

## 9. frontend server state와 cache 일관성

목록·상세·댓글은 서버가 원본인 state이고, 폼 입력·검색창 열림·popover·dialog·댓글 “더보기” gate는 화면 수명이 끝나면 사라지는 local state다. API data를 component별 `useEffect`에 복제하지 않고 TanStack Query가 page 누적, loading/error, refetch와 cancellation을 담당한다. 별도 global UI store는 추가하지 않았다.

query key에는 page size, `latest`/`likes`, 정규화된 검색어가 포함된다. cursor는 infinite query의 `pageParam`이어서 결과 집합의 identity와 page 위치를 섞지 않는다.

mutation 뒤 cache 처리는 “서버 응답만으로 결과를 정확히 증명할 수 있는가”를 기준으로 정했다.

- loaded post의 exact field, latest 목록의 좋아요·댓글 수처럼 결과가 명확하면 직접 갱신한다.
- 좋아요순은 count 변화로 순서가 바뀔 수 있어 invalidate한다.
- 게시글 수정 뒤 검색 결과는 membership이 바뀔 수 있어 invalidate한다.
- 생성 글의 검색 membership이나 likes rank도 client가 추론하지 않는다.
- 삭제는 서버 성공 뒤 loaded row를 모든 목록에서 제거하고, filter 없는 목록과 row가 로드된 목록의 total을 줄인다. 검색 목록은 로드되지 않은 범위의 total을 알 수 없어 invalidate한다.

모든 mutation은 server-confirmed 방식이다. optimistic UI보다 한 round-trip 늦지만 rollback과 false success 복잡도를 피했다. 이 규칙은 `frontend/lib/query/postListCache.ts`에 모으고 Vitest로 여러 cached query 조합을 검증한다.

## 10. 요청 단위 실험

첫 구현의 게시글 10개, 댓글 continuation 20개, rootMargin 200px, debounce 300ms는 기능을 완성하기 위한 초기값이었다. 기능 안정화 뒤 후보 차이를 보려고 375×778 Chromium, CPU 4× slowdown, 다운로드 약 1.47Mbps의 제한 network profile을 고정했다. 이는 평균 모바일 환경을 주장하는 값이 아니라 비교용 constrained condition이다.

### 게시글 batch 크기

clean seed의 readable post가 39개뿐이면 목록 끝 효과가 결과를 왜곡할 수 있어 원본 분포를 유지한 일회용 160-post dataset을 만들었다. SSR size와 continuation size를 각각 `10 / 15 / 20`으로 비교하고, 전체를 읽지 않고 30·45개 고정 깊이에서 측정했다.

- SSR LCP median은 약 `2,278 / 2,260 / 2,372ms`로 분포가 겹쳤다.
- continuation 10은 요청이 늘고 빠른 소비에서 loading 문구가 보였다.
- 20은 15보다 일관된 이득이 없었다.
- 분기 복잡도를 만들 성능 근거가 없어 SSR/continuation 모두 15를 유지했다.

### sentinel `rootMargin`

15/15 조건에서 `200 / 300 / 400 / 500px`를 비교했다. 다음 page가 요청부터 화면 반영까지 약 0.66초 걸리고 빠른 scroll은 그동안 약 400px 이동했다.

- 200px은 page 경계마다 최대 약 0.3초 기다렸다.
- 300px은 절반의 경계에서 기다렸다.
- 400px은 대기는 없었지만 짧은 빈 영역이 보였다.
- 500px은 해당 조건에서 wait, blank, scroll 전 요청이 관찰되지 않았다.

게시글 목록만 500px로 변경했다. 댓글은 같은 실험을 하지 않았으므로 200px을 유지했다. 500px도 모든 기기의 최적값이 아니라 이 조건에서 비교한 후보 중의 선택이다.

### 댓글 continuation 크기

최초 `limit=2`는 과제 계약이므로 고정하고, 이후 page만 `10 / 20 / 30 / 50`으로 비교했다. 1,411-comment post에서 30·60·120개 고정 깊이와 약 25개에서 이탈하는 경우를 측정했다.

| 후보 | 첫 더보기 반영 | 120개까지 요청 | 빠른 소비 loading 노출 | 25개 이탈 시 미열람 수신 |
| --- | ---: | ---: | ---: | ---: |
| 10 | 639ms | 13회 | 8회 | 6개 |
| 20 | 660ms | 7회 | 2회 | 16개 |
| 30 | 670ms | 5회 | 1회 | 6개 |
| 50 | 725ms | 3회 | 2회 | 26개 |

10은 요청과 loading 노출이 많고 50은 첫 반영과 over-fetch 비용이 컸다. 20보다 요청을 줄이면서 50의 비용을 피한 30을 유지했다. 전체 1,411개를 적재하면 약 10,000 DOM elements가 생겼고, 약 1,000개 이후 append에 50ms 이상, 최대 약 80ms가 걸렸지만 측정한 환경에서는 scroll 반응 저하가 보이지 않았다. 따라서 virtualization은 현재 문제에 대한 수정이 아니라 future trigger로 남겼다. 상세 raw matrix는 [PERFORMANCE.md §13–§15](PERFORMANCE.md#13-frontend-list-batch-size-benchmark--2026-10-02)에 있다.

## 11. runtime에서 발견한 framework 동작

### AbortSignal과 retry

300ms debounce는 짧은 입력 연속을 묶지만 이미 시작한 요청은 중지하지 않는다. TanStack Query가 query function에 제공하는 `AbortSignal`을 `getPosts`·`getPost`·`getComments`와 공통 `request()`를 거쳐 실제 `fetch`까지 전달했다. 느린 이전 검색과 상세/최초 댓글 요청을 의도적으로 만들고 화면을 떠났을 때 request가 abort되고 error UI로 바뀌지 않는 것을 확인했다.

read retry는 network, 5xx와 분류되지 않은 일시 오류에 한 번만 적용하고 4xx는 retry하지 않는다. mutation은 자동 retry하지 않는다. 소유권 실패나 validation 오류를 다시 보내지 않고, side effect가 있는 요청의 중복 위험도 피한다.

### comment `gcTime: 0`

댓글 query도 처음에는 shared `QueryClient` 수명을 사용했다. “더보기”로 continuation을 받은 뒤 같은 SPA session에서 상세에 재방문하면 cached pages가 즉시 복구되어, 사용자가 누르기 전에 최초 댓글 2개 계약을 우회했다.

continuation 재사용보다 방문마다 명시적 gate를 지키는 것이 중요해 댓글 query에만 `gcTime: 0`을 적용했다. 재방문은 다시 `limit=2`에서 시작하고 더보기 전에는 sentinel이 열리지 않는다. 대가로 방금 읽은 긴 댓글도 재방문하면 다시 요청한다. Playwright의 두 번째 core flow가 이 회귀를 CI에서 보호한다.

### Next.js detail prefetch

목록 전체 39개를 순회하자 dynamic detail route prefetch가 40회 발생했고 상당수가 취소됐다. 응답 route shell은 약 40–150B였지만 클릭 시 route/chunk 요청이 다시 필요했고, 제목 표시까지의 측정도 prefetch가 더 빠르지 않았다.

모든 prefetch를 끄지 않고 반복되는 dynamic post detail link만 `prefetch={false}`로 바꿨다. 작성처럼 정적이고 재사용 가치가 있는 link는 기본 동작을 유지한다. framework default를 일괄 부정한 것이 아니라 route별 hit 가능성과 실제 효과를 확인한 결과다.

## 12. readiness race와 afterCommit

초기 `/health`는 DB query가 가능한지만 확인했다. Tomcat과 DB가 먼저 살아나고 `ApplicationRunner`의 seed transaction이 아직 commit되지 않은 짧은 구간에도 200을 반환할 수 있었다. clean start에서 health는 성공했지만 post API `total`이 0인 순간을 실제로 관찰했다.

`SeedReadiness`를 추가하고 새 적재와 이미 적재된 재기동 경로 모두 transaction `afterCommit`에서만 ready를 열도록 변경했다. 그 전에는 DB 연결이 살아 있어도 `/health`가 503 `starting`을 반환한다. Compose가 backend를 healthy로 판단하는 시점에는 product data도 요청 가능한 상태다.

이는 초기부터 복잡한 readiness framework를 넣은 것이 아니라 “process alive”와 “service ready”를 같게 둔 실제 race를 확인한 뒤 작은 gate를 추가한 사례다. row별 seed insert의 단순성과 실패 위치 명확성을 유지하며 clean start 약 0.9초의 일회성 비용을 수용했다.

## 13. ownership, locking, concurrency

과제에는 login/user table이 없고 requester header/cookie 계약이 있다. `RequesterResolver`의 우선순위는 `x-user-id` header → 같은 이름의 cookie → `apr_tester`다. author 문자열과 requester가 같을 때만 수정·삭제할 수 있다. 실제 인증 모델로 일반화하지 않고 과제 계약 안에서 backend를 authoritative boundary로 유지했다.

frontend validation은 빠른 feedback만 제공하고 `MutationValidator`가 제목 필수·20 Unicode code points 이하·본문/댓글 nonblank를 다시 검사한다. transaction과 row lock은 목적이 다르다. 6개 mutation method는 모두 service의 `@Transactional` 안에서 실행되어 여러 단계 변경이 함께 commit되거나 rollback된다. row lock(`SELECT ... FOR UPDATE`)은 동시에 들어온 mutation을 직렬화하기 위한 것이며, method마다 필요한 대상에만 건다.

```text
post create           → 잠글 기존 row 없음
post update / delete  → readable post lock 후 소유권 확인
like toggle           → readable post lock; requester 중복은 (post_id, user_id) PK가 막음
comment create        → readable parent post lock
comment delete        → readable comment + readable parent (FOR UPDATE OF p, c) 후 소유권 확인
```

대상이 없거나 soft delete됐으면 lock 단계에서 404가 된다. parent 삭제와 child 작업이 경합하면 같은 post row에서 직렬화되므로, child 작업이 먼저 commit되거나 삭제 뒤 실행되어 404를 받는 두 결과만 남는다.

Testcontainers concurrency test 8개는 mock이 아니라 실제 transaction을 여러 worker에서 실행한다. 별도 JDBC transaction이 row lock을 잡고 `pg_stat_activity`에서 worker wait를 확인한 뒤 순서를 제어했다. `FOR UPDATE`를 제거하는 sensitivity change에서는 8개 중 4개가 실패했다(좋아요 toggle 경합의 실제 `DuplicateKeyException` 포함). 현재 데이터와 mutation 경계에서 필요한 직렬화만 사용했으며 분산 lock이나 별도 queue는 추가하지 않았다.

## 14. Figma에서 runtime product decision으로

공식 Figma는 핵심 화면과 visual hierarchy를 제공하지만 loading, error, 긴 목록, keyboard와 모든 product rule을 정의하지는 않는다. 디자인에 정의된 typography, 색상, 주요 화면의 의미는 유지하고 빈 runtime behavior는 글·대화 중심 community 해석과 실제 375×778 결과를 기준으로 채웠다.

- 목록은 제목 → 본문 preview → 작성자·반응·시간의 content-first hierarchy를 사용한다.
- 98px row는 실제 viewport에서 과밀해 읽기와 touch 분리가 나은 108px rhythm으로 조정했다.
- search와 latest/likes sort는 content를 밀어내는 새 화면 대신 44px discovery toolbar에 둔다.
- app title은 scroll되고 discovery toolbar만 sticky로 남겨 작은 화면의 세로 공간을 보존한다.
- 수정/삭제 두 command에는 full-width bottom sheet보다 trigger와 관계가 분명한 144px `PostActionMenu` popover를 사용한다.
- destructive delete에는 별도의 confirmation dialog를 유지한다.
- static design에 없는 loading/error/retry와 pending 중복 실행 방지를 제품 상태로 구현한다.

이 변경은 공식 디자인과 같다고 주장하지 않는다. 구체적인 `DESIGN_DEFINED`, `PRODUCT_DECISION`, `UX_CONSISTENCY` 구분은 [UI_SPEC.md](UI_SPEC.md)와 [design retouch 기록](../design/design-retouch/readme.md)에 있다.

구현 도중에는 local PNG/SVG와 runtime 비교만 있었고 현재 공식 file/node mapping을 확인하지 않았으므로, 당시 최종 비교 V-081은 `NOT_RUN`이었다. 이후 V-081에서 공식 Figma file과 Page 1 node mapping을 다시 확인하고, repository에 보존된 공식 frame export 12개와 공유 수정 form 상태를 375×778 production 구현과 비교했다. 위 deviation은 모두 V-081 전에 이미 기록된 `PRODUCT_DECISION`으로 분류됐고, 우발적 mismatch(`ACCIDENTAL_MISMATCH`)는 `NONE`, 결과는 `PASS`였다. production 변경은 필요 없었다. 이 비교는 frame metadata, rendered geometry, visual hierarchy를 기준으로 했고 pixel-difference threshold는 쓰지 않았다. loading/error/retry, focus, sticky 동작 같은 runtime-only 상태는 visual 비교에서 제외했다. 근거는 [DESIGN_COMPARISON.md](DESIGN_COMPARISON.md)와 [VERIFICATION.md](VERIFICATION.md) "Final V-081 Audit"에 있다.

## 15. dialog 접근성과 focus 동작

첫 `ConfirmDialog`는 `role="alertdialog"`, `aria-modal="true"`, label/description 연결과 open 시 cancel focus를 제공했다. 하지만 375×778 Chromium에서 Tab을 직접 따라가자 `예` 다음과 `취소` 이전의 focus가 dialog 뒤의 navigation, like button과 comment input으로 이동했다. `aria-modal`은 의미를 전달하지만 keyboard 이동 자체를 막지 않았다.

삭제 요청 중 두 버튼이 disabled 되면 Chromium이 focus를 `<body>`로 옮겼고, 실패한 뒤에도 dialog 밖에 남는 문제도 재현했다. 버튼 두 개짜리 dialog에 modal library나 portal을 추가하지 않고 공용 component에서만 보완했다.

- Tab/Shift+Tab을 활성화된 dialog button 사이에서 순환한다.
- focus가 dialog 밖이면 방향에 따라 첫/마지막 button으로 돌린다.
- 요청 중 모든 button이 disabled여도 focus가 배경으로 빠지지 않는다.
- 실패 후 cancel button으로 focus를 복구한다.
- close 뒤 trigger로 focus를 돌린다.
- pending 동안 Escape와 scrim close를 막는다.

이 keyboard behavior는 Playwright의 delete dialog flow가 실제 browser에서 반복 검증한다.

## 16. 검증 체계의 변화

### 수동 acceptance

초기에는 실제 PostgreSQL·Compose와 375×778 Chromium에서 요구사항별 acceptance를 수행했다. cursor 전체 순회, 동일 timestamp tie, requester header/cookie 전환, soft-deleted row의 물리 보존, cancellation, loading/error state와 focus behavior처럼 unit test만으로 충분하지 않은 경계를 기록했다. 당시 사용한 일부 browser script는 disposable/temporary였고 repository에는 남기지 않았다. 그 기록은 [VERIFICATION.md](VERIFICATION.md)의 historical evidence다.

### Vitest와 Testcontainers

반복 가치가 높은 순수 규칙은 committed tests로 옮겼다.

- backend JUnit/Mockito: controller, service, cursor codec, validator, requester와 seed/readiness 규칙
- PostgreSQL Testcontainers: 실제 Flyway·seed·repository SQL·schema constraints·cursor·search·mutation persistence와 concurrency
- frontend Vitest: ID parsing, query key normalization, cached list update와 retry policy

현재 snapshot은 backend 18 suites / 91 tests이며 그중 Testcontainers가 6 suites / 31 tests다. frontend Vitest는 4 files / 21 tests다.

### GitHub Actions

`backend`와 `frontend` job이 독립 실행되고 둘이 성공하면 `full-stack-smoke`가 production Compose image를 build/start한다. smoke script는 service health, `/health`, latest API, SSR HTML, `seed_history`, baked browser API base를 확인한다. 실패 시 Compose status와 logs를 남기고 마지막에는 항상 disposable volume까지 제거한다.

### 핵심 Playwright 회귀 검증

manual browser evidence 전체를 자동화하지 않고 실제 회귀 위험과 재현 가치가 큰 세 흐름만 committed Playwright로 승격했다. Chromium 375×778, workers 1, retries 0으로 production Compose stack을 대상으로 실행한다.

1. seed 기반 검색, 좋아요순, mobile control과 horizontal overflow invariant
2. 최초 댓글 2개, More의 30개 continuation, SPA 재방문 시 다시 2개와 닫힌 gate
3. disposable post의 delete dialog focus trap, Escape/trigger focus 복귀와 confirm 전 browser DELETE 부재

실패 시 `frontend/test-results`의 screenshot과 trace를 `playwright-failure-artifacts`로 7일 보존한다. artifact가 없거나 upload가 실패해도 원래 failure와 Compose diagnostics를 가리지 않으며 cleanup은 항상 실행된다.

이 세 테스트는 전체 runtime acceptance를 대체하지 않는다. 전체 cursor traversal, 모든 failure injection과 CRUD/ownership 조합, Firefox/WebKit, visual regression, Lighthouse CI는 자동 Playwright 범위가 아니다. 공식 Figma 최종 비교 V-081은 자동화하지 않고 수동 audit로 수행했다(§14).

## 17. 현재 trade-off와 확장 조건

### 긴 댓글 virtualization

cursor pagination은 network 단위를 제한하지만 사용자가 1,411개를 모두 읽으면 DOM은 누적된다. 약 10,000 elements와 50–80ms append가 측정됐어도 현재 환경에서 사용자에게 보이는 scroll 저하는 없었다. 더 긴 실제 대화나 저사양 기기에서 체감 문제가 확인되면 visible window와 buffer만 mount하는 virtualization을 검토한다.

### 변하는 좋아요 순위

likes cursor의 count는 concurrent mutation으로 움직일 수 있다. 현재는 duplicate ID를 client에서 제거하고 과제 데이터 규모에는 ranking snapshot을 추가하지 않았다. 동시 반응과 page 깊이가 커져 page 이동이 제품 문제로 나타나면 snapshot/version 전략을 데이터 비용과 함께 비교한다.

### 더 큰 규모의 검색

39 readable posts에는 escaped substring search가 단순하고 충분하다. latency나 result quality 문제가 실제 query에서 확인되면 PostgreSQL full-text search, index, relevance와 한국어 처리 후보를 측정한다.

### visual regression과 browser matrix

핵심 Chromium behavior만 CI에 있다. 공식 Figma 최종 비교 V-081은 수동 audit로 `PASS`했지만, 이는 한 시점의 비교이지 자동 visual regression이 아니다. screenshot baseline을 CI에서 비교하는 visual regression과 Firefox/WebKit matrix는 아직 없으므로 자동 pixel fidelity는 주장하지 않는다. UI 변경 빈도와 회귀 비용이 커지면 stable screenshot baseline과 Firefox/WebKit을 추가할 수 있다.

### 제품 경험

대화가 충분히 축적되어 좋은 사용법이 여러 글에 흩어지는 문제가 생기면, 반복 참조되는 경험을 `제품 → 순서 → 부위 → 사용량 → 주의사항`처럼 따라 할 수 있는 guide로 연결할 수 있다. 현재 구현에는 guide domain이 없으며, 실제 반복 참조와 필요한 field를 관찰한 뒤 결정할 제품 방향이다.

## 18. 주요 출처

제품 해석과 기술 판단에는 블로그보다 공식 자료를 우선했다.

- [Google Play — 에이지알 메디큐브 디지털클리닉 AGE-R](https://play.google.com/store/apps/details?hl=ko&id=com.apr.digitalclinic)
- [Spring Framework — `JdbcClient`](https://docs.spring.io/spring-framework/reference/data-access/jdbc/core.html#jdbc-JdbcClient)
- [Spring Boot — Flyway database initialization](https://docs.spring.io/spring-boot/how-to/data-initialization.html#howto.data-initialization.migration-tool.flyway)
- [Redgate Flyway — Schema History Table](https://documentation.red-gate.com/flyway/flyway-concepts/migrations/flyway-schema-history-table)
- [TanStack Query — Infinite Queries](https://tanstack.com/query/latest/docs/framework/react/guides/infinite-queries)
- [TanStack Query — Query Keys](https://tanstack.com/query/latest/docs/framework/react/guides/query-keys)
- [TanStack Query — Invalidations from Mutations](https://tanstack.com/query/latest/docs/framework/react/guides/invalidations-from-mutations)
- [TanStack Query — Query Cancellation](https://tanstack.com/query/latest/docs/framework/react/guides/query-cancellation)
- [Next.js — CSS Modules](https://nextjs.org/docs/app/getting-started/css#css-modules)
- [PostgreSQL — Identity Columns](https://www.postgresql.org/docs/current/ddl-identity-columns.html)
- [PostgreSQL — LIMIT and OFFSET](https://www.postgresql.org/docs/current/queries-limit.html)
- [PostgreSQL — Row Constructor Comparison](https://www.postgresql.org/docs/current/functions-comparisons.html#ROW-WISE-COMPARISON)
