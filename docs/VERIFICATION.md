# 인수 검증 기록

이 문서는 APR 과제의 실행 가능한 acceptance 시나리오를 정의하고, 실제로 실행한 검증 근거를 기록합니다.

code가 맞아 보인다는 이유만으로 요구사항을 `VERIFIED`로 표시하지 않습니다. 해당 시나리오를 실행하고 근거를 기록합니다.

## 결과 값

- `NOT_RUN`
- `PASS`
- `FAIL`
- `BLOCKED`

## 이 문서를 읽는 방법 — Historical vs Current

이 문서는 상세한 master 검증 기록입니다. 나중에 바뀐 값까지 포함해, 각 phase의 근거를 그 날짜와 함께 보존합니다.

- 날짜가 붙은 section과 heading(예: "P4 … — 2026-09-29", "P8-B 당시 후보 근거 — 2026-09-30")은 **그 날짜의 code**를 설명합니다. 그 사실은 그 날짜 기준으로 맞으며, 나중의 근거가 앞선 근거를 틀렸다고 만들지 않습니다. heading의 "당시 main", "당시 후보"는 그 phase의 main/후보를 뜻하며 오늘의 `main`이 아닙니다.
- post page size `10`, 댓글 continuation `20`, post 목록 `rootMargin` `200px`, SSR seed freshness 30 s, SSR timeout 1.5 s, TanStack Query 기본 retry(시도 4회), backend test 수 `26/49/51/60/83` 같은 값은 그 날짜에 맞는 값이며 historical evidence로 남깁니다. 나중 결정이 이를 대체했으면 해당 section이 그렇게 적습니다.
- 문서 안의 시점 구분:
  - **날짜가 붙은 근거:** 그 날짜의 사실.
  - **최종 Configuration Snapshot**(아래): 특정 code baseline(`3c902dc`)에서 읽은 configuration.
  - **최종 Engineering 검증**(2026-10-02–04, 끝부분): 이후의 engineering audit.
  - **최종 Sign-Off**(맨 끝): 현재 repository에 대한 최종 해석.

## 최종 Configuration Snapshot — base `main` @ `3c902dc`

현재 값을 참고하기 위한 표이며, 아래 근거를 대체하지 않습니다. 각 값은 `3c902dc9126b2eb2ec0713844b3d7cbec237e80f`의 code에서 읽었습니다.

*현재 적용 여부:* `3c902dc` 이후 `main` `f3f79fbc61a9215014419f58e91a596ede252a1e`까지 `git diff --name-only`로 확인한 결과, `docs/`와 `README.md` 밖의 변경(`frontend/`, `backend/`, `docker-compose.yml`, package/lockfile, Gradle, workflow, Playwright 설정·test)은 없습니다. 따라서 이 snapshot의 값은 현재 `main`에서도 그대로입니다. 아래 "Mutation locking" 행만 method별 실제 구현에 맞게 더 정확히 적었습니다.

**Frontend**

| 항목 | 현재 값 | Source |
| --- | --- | --- |
| Next.js | 15.5.27 | `frontend/package.json` |
| React / React DOM | 19.1.9 / 19.1.9 | `frontend/package.json` |
| Node engine | `>=22.12.0` | `frontend/package.json` `engines` |
| Post 목록 page size (SSR 첫 page와 continuation) | 15 / 15 | `features/post-list/postListConfig.ts` `POST_LIST_PAGE_SIZE` |
| 댓글: 처음 / continuation | 2 / 30 | `features/post-detail/useComments.ts` |
| 댓글 query cache | `gcTime: 0` (방문 단위) | `useComments.ts` |
| Post 목록 prefetch 거리 | `rootMargin` `0px 0px 500px 0px` | `postListConfig.ts` `POST_LIST_PREFETCH_ROOT_MARGIN` |
| 공용 infinite-scroll 기본값 (댓글이 사용) | `rootMargin` `0px 0px 200px 0px` | `lib/hooks/useInfiniteScrollTrigger.ts` |
| 검색 debounce | 300 ms | `PostListScreen.tsx` `SEARCH_DEBOUNCE_MS` |
| 첫 page SSR | 기본 view만 (`latest`, 검색 없음) | `app/page.tsx`, `lib/api/server.ts` |
| SSR seed freshness | 5 s (`dataUpdateCount === 0`일 때만 `staleTime`, 이후 0) | `usePostList.ts` `SEED_FRESH_MS = 5_000` |
| SSR backend timeout | 2,000 ms, 이후 `null` → client fetch | `lib/api/server.ts` `SERVER_FETCH_TIMEOUT_MS = 2000` |
| Read query retry | `ApiError` 4xx: retry 없음; network, 5xx, 알 수 없음: retry 1회 (기본 지연 1 s) | `lib/query/retryPolicy.ts`, `app/providers.tsx` |
| Mutation retry | 0 (TanStack Query 기본값; `retry`를 지정한 mutation 없음) | — |
| Font | Pretendard 공식 variable font (`PretendardVariable.woff2`, 2,057,688 B) 유지 | `public/fonts`, `app/globals.css` |
| Frontend test | Vitest, 4 files / 21 tests | `lib/**/*.test.ts` |
| Browser 회귀 | Playwright, Chromium 375×778, core flow 3개, workers 1, retries 0 | `playwright.config.ts`, `tests/e2e/core-flows.spec.ts` |

**Backend**

| 항목 | 현재 값 | Source |
| --- | --- | --- |
| Java | 17 (Gradle toolchain) | `backend/build.gradle` |
| Spring Boot | 3.5.16 | `backend/build.gradle` |
| Database | PostgreSQL 16 (`postgres:16-alpine`) | `docker-compose.yml` |
| Schema | Flyway `V1__create_community_schema.sql` | `backend/src/main/resources/db/migration` |
| Data access | Spring `JdbcClient` repository | `persistence/*Repository.java` |
| Test database | Testcontainers PostgreSQL, version override `1.21.4` | `build.gradle` `ext['testcontainers.version']` |
| 삭제 | soft delete (`is_deleted`) | `V1` |
| Pagination | keyset `(created_at, id)` row-value; likes `(effective likes, created_at, id)` | repository |
| Mutation locking | 6개 mutation method 모두 service `@Transactional`(원자성/rollback 경계). row lock(`SELECT … FOR UPDATE`, 경합 mutation 직렬화)은 method별: post 생성은 잠글 기존 row 없음; post 수정/삭제/좋아요는 읽을 수 있는 post를 잠근 뒤 ownership 확인; 댓글 생성은 읽을 수 있는 parent post; 댓글 삭제는 읽을 수 있는 댓글과 parent를 함께 잠금(`FOR UPDATE OF p, c`) 후 ownership 확인 | `PostMutationService`, `CommentMutationService`, `findReadableMutationTargetForUpdate` |
| Backend test | 18 suites / 91 tests (PostgreSQL integration 6 suites / 31 tests) | `gradlew clean test` |

**Security / CI**

| 항목 | 현재 값 |
| --- | --- |
| PostCSS | root `overrides` `8.5.28` (`next@15.5.27`이 요청하는 `8.4.31`을 대체) |
| `npm audit` / `npm audit --omit=dev` | 0 / 0 |
| CI job | `backend`, `frontend`, `full-stack-smoke`; `permissions: contents: read` |
| Playwright 실패 근거 | `frontend/test-results` → `playwright-failure-artifacts`, 7일 보관; upload가 원래 실패를 가리지 않음 |
| `main`의 branch protection | 사용 안 함 (GitHub API `protected: false`) |

---

# Release Gate

아래 gate는 APR 과제 문구가 아니라 이 repository의 내부 release gate 기준입니다. V-001 같은 ID도 이 repository가 붙인 것입니다. 아래 gate가 모두 통과하지 않으면 이 기준에서 release-ready로 보지 않습니다.

## V-001 — 전체 Stack 기동

**요구사항:** `ENV-006`, `ENV-007`, `ENV-008`

### 단계

1. 이전 과제 container를 멈춥니다.
2. 다음으로 시작합니다.

```bash
docker compose up --build
```

3. backend health를 확인합니다.

```bash
curl -i http://localhost:8080/health
```

4. `http://localhost:3000`에서 frontend를 엽니다.

### 기대 결과

- DB가 시작됩니다.
- Backend가 시작됩니다.
- Frontend가 시작됩니다.
- `/health`가 HTTP 200으로 응답합니다.
- Frontend에 접속할 수 있습니다.

### 실행 근거 — 2026-09-29

- 빈 volume에서 `docker compose up --build`가 성공적으로 끝났습니다.
- `db`, `backend`, `frontend`가 `healthy`에 도달했습니다.
- `GET /health`가 HTTP 200을 반환했습니다.
- Flyway migration `V1`이 성공적으로 끝났습니다.

### P8-A 당시 main 근거 — 2026-09-30

- 최종 기본 port `docker compose up --build -d` build가 끝났고, `backend`보다 `db`를 먼저 시작했으며 frontend는 backend health를 기다렸습니다. 세 service가 모두 healthy가 되었고, `/health`는 `{"status":"ok","db":"up"}`과 함께 HTTP 200, `/api/posts?limit=1`은 total 39, frontend root는 HTTP 200을 반환했습니다.
- readiness 수정 전 새 volume 관찰 세 번에서는 seed 기반 데이터가 확실히 준비되기 전에 health가 200이었습니다. 첫 번째 관찰에서는 health 200 뒤에 `/api/posts` total 0을 직접 봤습니다. 아래 전용 조사 section을 참고하세요.
- 수정 후 새 volume 기동 세 번에서는 seed readiness가 commit될 때까지 `/health`가 503을 유지했습니다. health 200 이후 관찰한 첫 API 응답은 모두 total 39였습니다. 따라서 Docker는 읽을 수 있는 seed가 없는 동안 backend를 healthy로 보고할 수 없었습니다.

### P8-B 당시 후보 메모 — 2026-09-30

- P8-B는 product code를 바꾸지 않았습니다. 다시 build한 기본 port stack은 `/health` 200, total 39로 healthy를 유지했습니다. P8-A의 빈 volume 근거가 그대로 적용되며, 파괴적으로 다시 실행하지 않았습니다.

**결과:** `PASS`

---

## V-002 — 대체 Port 기동

**요구사항:** `ENV-009`, `ENV-010`, `ENV-011`

### 단계

기본값이 아닌 port로 시작합니다. 예:

```bash
FRONTEND_PORT=13000 BACKEND_PORT=18080 DB_PORT=25432 docker compose up --build
```

확인:

```bash
curl -i http://localhost:18080/health
```

`http://localhost:13000`에서 frontend를 엽니다.

### 기대 결과

- 모든 service가 지정한 port에서 정상 시작됩니다.
- Frontend가 backend와 정상 통신합니다.

### P8-A 당시 main 근거 — 2026-09-30

- PowerShell 범위의 `FRONTEND_PORT=13000`, `BACKEND_PORT=18080`, `DB_PORT=25432`로 `docker compose up --build -d`를 실행하자, 정확히 그 host port에 묶인 healthy service 세 개가 생겼습니다. `GET http://localhost:18080/health`는 200, 대체 API는 total 39를 반환했습니다.
- Chromium은 `http://localhost:13000/`을 목록 row 10개로 load했습니다. 실제 browser resource는 `http://localhost:18080/api/posts?limit=10`이었고, `localhost:8080`을 쓴 요청은 없었습니다. 임시 PowerShell port 변수는 실행 후 제거했고, 기본 port stack을 다시 성공적으로 build했습니다.

### P8-B 당시 후보 메모 — 2026-09-30

- P8-A 이후 Compose, port, frontend API base 변경이 없으므로 대체 port 근거가 그대로 적용됩니다. 다시 실행하지 않았습니다.

**결과:** `PASS`

---

## V-003 — Seed Idempotency

**이 시나리오로 완전히 검증한 요구사항:** `SEED-001`, `SEED-002`, `SEED-005`, `SEED-006`, `SEED-007`, `SEED-011`, `SEED-012`

**이 시나리오에서는 영속성 근거만:** `SEED-003`, `SEED-004`, `SEED-008`, `SEED-009`, `SEED-010` (이후 API 근거는 아래 P2A 시나리오에 기록)

### 단계

1. 빈 DB volume에서 시작합니다.
2. application을 시작하고 post/comment row 수를 기록합니다.
3. DB volume을 지우지 않고 application을 멈춥니다.
4. application을 다시 시작합니다.
5. row 수를 다시 기록합니다.
6. 좋아요 toggle 전에 `post_likes`가 비어 있어 어떤 요청자도 처음 좋아요 관계가 없음을 확인합니다.
7. Node.js로 `seed/seed.json`을 UTF-8로 parse하고, seed의 모든 post `likeCount`를 `posts.base_like_count`와 비교합니다.
8. parse한 seed와 database 사이에서 전체 삭제 post/comment ID 집합과 post별 삭제되지 않은 댓글 수를 비교합니다.

### 기대 결과

- 첫 기동 후 seed row가 있습니다.
- ID가 seed ID와 같습니다.
- 이미 삭제된 seed ID는 삭제 상태로 저장되어 있습니다.
- 두 번째 기동이 seed row를 중복시키지 않습니다.
- 삭제되지 않은 댓글의 database 수가 그대로입니다.
- 처음 `post_likes` 영속 상태는 비어 있습니다.
- 모든 seed post는 제공된 `likeCount`에서 시작합니다.
- Database의 삭제 ID 집합이 `deletedPostIds`, `deletedCommentIds`와 정확히 같습니다.
- post별 읽을 수 있는 댓글 수는 `deletedCommentIds`의 모든 ID를 제외합니다.

### 실행 근거 — 2026-09-29

- 빈 volume 초기 상태: post 42, comment 2,076, 삭제 post 3, 삭제 comment 81, `post_likes` 0, `assignment-v1` `seed_history` row 1개.
- 보존된 범위: post ID `1..42`; comment ID `1..2076`.
- 생성 identity 확인에서 post ID `43`과 comment ID `2077`이 나왔고, 보존된 seed ID와 충돌하지 않았습니다.
- 영속 volume으로 backend를 재시작해도 post 42, comment 2,076, `post_likes` 0, `assignment-v1` seed-history row 1개가 유지되었습니다.
- 수정한 기존 seed row는 backend 재시작 후에도 수정된 값을 유지해, 기동이 기존 데이터를 덮어쓰지 않음을 증명했습니다. 값은 검증 후 복원했습니다.
- Node.js가 실제 `seed/seed.json`을 UTF-8로 parse했습니다(`JSON.parse`: post 42, comment 2,076). sample이 아니라 정렬된 PostgreSQL 전체 결과와 비교했습니다.
  - `seed.deletedPostIds`는 `SELECT id FROM posts WHERE is_deleted = TRUE ORDER BY id`와 정확히 같았습니다(ID 3/3).
  - `seed.deletedCommentIds`는 `SELECT id FROM comments WHERE is_deleted = TRUE ORDER BY id`와 정확히 같았습니다(ID 81/81).
  - 42개 `seed.posts[].likeCount` 값이 모두 해당 `posts.base_like_count`와 같았습니다(row 42/42).
  - 42개 post 모두에서 `deletedCommentIds`를 제외한 seed 댓글 수가 `comments.is_deleted = FALSE`인 database 수와 같았습니다(post 42/42).
- `backend/gradlew.bat test --no-daemon`이 `BUILD SUCCESSFUL`로 끝났습니다.

### P8-A 당시 main 근거 — 2026-09-30

- 새 P8-A volume에서 Node.js가 실제 UTF-8 `seed/seed.json`을 parse해 database 전체 결과와 비교했습니다. post 42, comment 2,076, 삭제 post 3, 삭제 comment 81, 좋아요 0, `assignment-v1` history row 정확히 1개. 모든 post ID와 comment ID, 두 삭제 ID 집합 전체, 42개 base like count, 42개 post별 읽을 수 있는 댓글 수가 원본과 같았고, 읽을 수 있는 post API total은 39였습니다.
- P8-B 메모: P8-B 시험 전 database는 여전히 `42|3|2076|81|0|1`이었고 seed와 seed runner는 바뀌지 않았습니다. P8-B는 runtime에 생성하고 soft delete한 acceptance row만 추가했습니다("P8-B Full Behavioral Acceptance" 참고).
- volume을 지우지 않고 `docker compose stop` 후 `docker compose up -d`를 실행해도 상태는 `42|2076|3|81|0|1`, 읽을 수 있는 total은 39로 유지되었습니다. seed row, 삭제 marker, like row, seed-history marker 모두 중복되지 않았습니다.

### Acceptance 범위

- 이 `PASS`는 완료된 P1 영속성/seed 검증을 기록한 것이며, 모든 복합 `SEED-*` 요구사항의 최종 API acceptance가 아닙니다.
- 이후 P2A가 V-021로 `SEED-003`을, V-065로 `SEED-004`, `SEED-008`, `SEED-009`를 검증했습니다.
- P2A가 `SEED-010`의 첫 read 부분을 검증했고, 이후 P2B가 V-050에서 요청자별 toggle 근거를 완성했습니다.

**결과:** `PASS`

---

## V-004 — 수정 불가 Starter 계약

**요구사항:** `START-001`, `ENV-007`, `ENV-008`

### 단계

- `frontend/app/dev-user/`를 starter version과 비교합니다.
- `/health`를 호출합니다.
- `/dev-user`로 user를 바꾸고 `x-user-id` cookie 동작이 계속 되는지 확인합니다.

### 기대 결과

- `frontend/app/dev-user/`가 바뀌지 않았습니다.
- `/health`가 계속 200을 반환합니다.
- User 전환 동작이 계속 됩니다.

### P8-A 당시 main 근거 — 2026-09-30

- `git rev-list --max-parents=0 HEAD`로 starter commit `38a975520f5086fe6a6be98058e7d659bfcca8c9`를 찾았습니다. 당시 HEAD와의 `frontend/app/dev-user/page.tsx` diff는 비어 있고, 그 file 이력에는 그 starter commit만 있습니다.
- `/health`는 같은 path를 유지하며 readiness 이후에만 HTTP 200을 반환했습니다. Chromium에서 `/dev-user`로 `mintcream`을 고르자 `x-user-id=mintcream`이 설정되었고, 실제 frontend API 요청이 그 cookie를 실었습니다. post 1을 `mintcream`으로 좋아요하고, `apr_tester`로 좋아요 안 함 상태를 읽고, 다시 `mintcream`으로 좋아요 함 상태를 읽었습니다. 관계는 toggle로 해제하고 cookie를 `apr_tester`로 되돌려 `post_likes`를 0으로 남겼습니다.

### P8-B 당시 후보 메모 — 2026-09-30

- `frontend/app/dev-user/`(와 `CorsConfig.java`)에 대한 `git diff 38a9755 HEAD`는 여전히 비어 있습니다. `/dev-user`로 `mintcream`, `Roidl_08`, `apr_tester`를 오가며 browser에서 요청자별 좋아요, 댓글, post ownership 동작을 확인했습니다.

**결과:** `PASS`

---

## P8-A First-Boot Readiness Investigation — 2026-09-30

*당시 발견과 이후 해결:* 아래는 2026-09-30의 문제 발견 기록입니다. 이 조사에서 추가한 `SeedReadiness`가 현재 구현이며, 기동 중 seed commit 전에는 `/health`가 503 `starting`, ready 후에는 200입니다.

`T1`은 backend port 8080의 첫 TCP 수락, `T2`는 첫 `/health` HTTP 200, `T3`는 첫 성공 목록 API 응답, `T4`는 처음 관찰한 목록 total 39, `T5`는 그에 맞는 DB 확인 `42|2076|3|81|0|1`입니다. 시간은 각 polling window 시작부터의 초이며, 첫 image build 시간은 의도적으로 제외했습니다.

- 수정 전 빈 volume 세 번은 T4보다 T2가 먼저였습니다. 1회차 `65.380/71.748/72.016/72.860/72.860`(T3에서 실제 API total 0 포함); 2회차 `.032/.235/.371/.371/.371`; 3회차 `.033/.229/.359/.359/.359`. `HealthController`는 DB connection만 확인했고 transaction 안의 `SeedInitializer`는 `ApplicationRunner`로 실행되었으므로, 1회차는 timing noise가 아니라 release blocker임을 증명했습니다.
- 수정은 `SeedReadiness`를 추가합니다. `SeedInitializer`가 현재 transaction의 `afterCommit`에 readiness를 등록하고, 그 flag가 true가 될 때까지 `/health`는 503을 반환합니다. path, 결국의 200 응답, seed 데이터, Docker healthcheck 계약은 바꾸지 않습니다.
- 수정 후 빈 volume 세 번에서는 health 200과 빈 seed가 함께 나타나지 않았습니다. 1회차 `10.099/17.071/16.112/17.104/17.104`(내부 API는 health 전에 total 0으로 접근 가능했지만, health 이후 첫 API는 total 39); 2회차 `.033/.233/.366/.366/.366`; 3회차 `.031/.231/.377/.377/.377`. Docker health는 seed가 준비된 뒤에만 healthy가 될 수 있었습니다.

**분류:** `BENIGN_INTERNAL_GAP`

### P8-A Static Check 메모

- `backend/gradlew.bat clean test --no-daemon`, `npm run typecheck`, `npm run build`, `git diff --check`가 통과했습니다. local `frontend/.next` 대상은 확인했지만 명시적 재귀 삭제는 이 실행 환경의 안전 정책으로 막혔으므로, local npm build가 깨끗했다고 주장하지 않습니다. frontend Docker build context는 `frontend/.dockerignore`로 `.next`를 제외하며, runtime image에 쓴 production build를 성공적으로 수행했습니다.

---

## Post Discovery (search + sort) Evidence — 2026-09-30

- **범위:** P8-C branch 위에 쌓은 branch `feat/post-list-search-sort`. 과제 범위를 넘는 `PRODUCT_DECISION`이며, 설계는 `docs/ARCHITECTURE.md` "Post Discovery 반영 (검색 + 정렬)"과 `docs/UI_SPEC.md` §13에 있습니다. 임시 verifier는 session scratchpad에 두었습니다.
- **Backend test:** `gradlew clean test`가 60 tests, 0 failures로 통과했습니다(이전 51). 새 test가 다루는 것:
  - 기본 sort와 query 정규화;
  - `INVALID_SORT`와 `INVALID_QUERY`;
  - 다른 sort의 cursor 거부;
  - `PostLikeCursorCodec` 왕복과 잘못된 payload;
  - LIKE escaping.
- **PostgreSQL 대상 API, 31/31.** marker dataset(좋아요 동점, `%`/`_` 미끼, soft delete된 일치 항목, 삭제된 댓글)을 넣었습니다. 결과:
  - 기본값은 `sort=latest`, PostgreSQL 최신순과 같습니다.
  - likes 전체 순회가 `(base + likes) DESC, created_at DESC, id DESC`와 같습니다.
  - `limit=2` 검색은 두 sort 모두 PostgreSQL과 같았고 중복·누락이 없었으며, 동점은 created_at, id 순이었습니다.
  - filter된 `total`은 읽을 수 있는 일치 항목 수와 같습니다.
  - `100%`와 `a_b`는 문자 그대로 일치하고(미끼 `1000`, `axb` 제외), `\`도 안전합니다. 일치는 대소문자를 구분하지 않습니다.
  - 본문만 일치하는 항목도 찾고, soft delete된 post는 제외합니다.
  - 모든 cursor 오용은 400 `INVALID_CURSOR`를 반환합니다. `INVALID_LIMIT`, `INVALID_SORT`, `INVALID_QUERY`(100 code point 초과)가 적용됩니다.
  - 요청자 `isLiked`와 `commentCount`가 정확합니다.
  - Query plan: likes page는 `post_likes` 집계 하나를 가진 statement 하나이고, 11-row page에서 comment subplan은 `loops=11`이었습니다(N+1 없음).
- **실제 375×778 browser, 29/29:**
  - 첫 load는 정확히 목록 요청 1개입니다.
  - toolbar는 44px row 하나이며, scroll하면 top 0에 sticky이고 title은 scroll되어 사라집니다. 두 mode 모두 overflow가 없습니다.
  - sort를 바꾸면 새 첫 page에서 시작합니다. likes infinite scroll은 API와 같고, latest/likes cursor가 다른 목록으로 넘어가지 않았습니다.
  - 깊게 scroll한 뒤 sort를 바꾸면 viewport가 toolbar 아래로 돌아갑니다.
  - 5글자를 입력하면 debounce된 요청 1개를 보냈습니다. 느린 이전 검색은 취소되었고(`net::ERR_ABORTED`) 더 새 결과를 덮어쓰지 않았습니다.
  - 제목, 본문, 결과 없음 검색이 동작하고, 지우기·Escape·← 닫기가 동작하며, 닫은 뒤에도 sort가 유지됩니다.
  - likes와 함께 쓴 검색이 올바르게 정렬되고, 좋아요 후 돌아오면 post가 위로 올라갔습니다.
  - 작성, 검색 대상에서 빠지는 수정, 삭제가 돌아온 뒤 검색 membership을 갱신했습니다.
  - page error와 실패 응답이 없었습니다.
- **회귀:** 목록, 좋아요/요청자 전환, 댓글, 댓글 mutation, post CRUD, E2E, post-action popover, 삭제 dialog와 댓글 identity에서 160/160.
- **Static:**
  - `npm run typecheck`, clean `npm run build`, `git diff --check`가 통과했습니다.
  - dependency, schema, Compose, seed, README 변경은 없습니다.
- **데이터:** 임시 좋아요는 모두 되돌렸고, 임시 post와 댓글은 soft delete했습니다.
- **이 단계의 V-081 상태(당시):** `NOT_RUN`; toolbar는 이후 최종 비교를 위해 의도적 차이로 기록했습니다.

---

## CI, Read Cancellation, and Dialog Focus Evidence — 2026-10-01

Browser 확인은 375×778 headless Chromium에서, session scratchpad에 둔 임시 Playwright script로 수행했습니다. 각 branch build는 Compose backend를 대상으로 `next start`로 실행했습니다.

- **CI (`.github/workflows/ci.yml`, PR #25):**
  - `main`으로의 pull request와 `main` push에서 read-only `contents` 권한으로 실행됩니다.
  - `backend` job: Ubuntu, Temurin 17, repository wrapper로 `./gradlew test`. `backend/gradlew`가 `100644`로 추적되므로 먼저 `chmod +x`를 실행합니다.
  - `frontend` job: Ubuntu, Node.js 22, `npm ci` → `npm run typecheck` → `npm run build`.
  - PostgreSQL service나 Compose 없음: 당시 backend test class 12개는 Spring context를 띄우지 않고 database가 필요 없었습니다(`HealthControllerTest`는 `DataSource`를 mock).
  - *(Historical — 이후 대체됨. `7da54e8` 기준: `frontend` job은 `npm test`도 실행하고(PR #43); `backend` job은 runner의 Docker daemon이 필요한 PostgreSQL Testcontainers integration test 31개를 포함한 91 tests를 실행하며(PR #42, #44); 세 번째 `full-stack-smoke` job이 production Compose stack을 build하고 확인합니다(PR #46). "Final Engineering Verification" 참고.)*
  - push 전: `gradlew.bat test --rerun-tasks`가 60 tests, 0 failures로 실행되었고, `npm ci`, typecheck, build가 exit 0이었습니다.
  - Actions: 첫 PR run(36852207676)이 두 job 모두 통과했습니다. PR #25, #26, #27 merge 후의 `main` push run도 성공했습니다(36852647774, 36855352902, 36856028261).
- **Read 취소 (PR #26):** `getPost`와 `getComments`가 선택적 `AbortSignal`을 받고, `usePostList`가 이미 그랬듯 `usePostDetail`/`useComments`가 query function의 signal을 전달합니다.
  - 방법: post 2에 대한 대상 GET을 route interception으로 4 s 붙잡았습니다. test는 목록에서 `/posts/2`로 이동하고 0.7 s 뒤 뒤로 갔습니다. page marker로 document reload가 아니라 client 측 이동임을 확인했습니다.
  - 상세 `GET /api/posts/2`: fetch가 signal을 실었고, signal이 발생했으며, fetch가 `AbortError`로 reject되었고, Chromium은 `net::ERR_ABORTED`를 보고했습니다. 상세 query는 error 없이 `pending`/`idle`로 남았습니다. alert, console error, page error가 없었습니다.
  - 첫 댓글 `GET /api/posts/2/comments?limit=2`: 같은 결과; 상세 query는 `success`를 유지했습니다.
  - 대조군: 변경 전 build에서는 같은 단계가 signal 없이 두 요청을 보냈고 둘 다 200으로 끝났습니다.
  - 회귀: 목록은 `limit=15&sort=latest`를 요청했습니다. 댓글은 `limit=2`, 더보기 후 cursor와 함께 `limit=30`을 요청했습니다. 좋아요 toggle은 signal 없이 전송되어 200을 반환했습니다(최종 변화 0). 없는 post는 요청 1개를 보내 404를 받았고 retry하지 않았습니다.
  - Browser로 test하지 않음: continuation page loading 중 떠나기.
- **ConfirmDialog focus 가두기 (`754abc4`에서 audit, PR #27에서 수정):**
  - 수정 전:
    - post와 댓글 dialog 모두에서 `예` 다음 Tab과 `취소` 이전 Shift+Tab이 뒤의 control(뒤로 가기, post 메뉴, 좋아요, 댓글 삭제, 더보기, 댓글 입력)에 닿았습니다.
    - 삭제가 pending인 동안 두 button이 비활성이 되어 Chromium이 focus를 `<body>`로 옮겼습니다. DELETE 실패 후 첫 Tab은 뒤로 가기 button에 닿았습니다.
  - 수정 후, 44/44 확인:
    - dialog를 keyboard로 열든 mouse로 열든 focus는 `취소`에서 시작합니다.
    - Tab ×6과 Shift+Tab ×6은 `예`와 `취소` 사이만 오갑니다.
    - Escape, `취소`, scrim이 dialog를 닫고 focus를 `게시글 메뉴` 또는 해당 댓글의 `삭제`로 돌립니다.
    - pending 중: 두 button이 비활성이고, Escape와 scrim은 무시되며, Tab 4번과 Shift+Tab 4번이 background에 닿지 않습니다.
    - 붙잡은 DELETE에 500을 응답한 뒤: 오류 문구가 나타나고, button이 다시 활성화되며, focus는 `취소`에 있고 다음 Tab은 `예`로 갑니다.
  - 데이터: 모든 DELETE는 script가 가로채 응답했습니다. post 5와 comment 1527은 이후에도 존재했습니다.
  - background scroll은 잠기지 않습니다(scrim 위 wheel에서 `scrollY` 0 → 800). polish로 분류해 범위 밖으로 두었습니다.
- **Static:** 각 변경에서 `npm run typecheck`, `npm run build`, `git diff --check`가 통과했고, 각 PR의 CI가 통과했습니다.

---

## P8-B Full Behavioral Acceptance — 2026-09-30

- **후보(당시):** `79ebd67`(P8-A merge)의 `feat/p8b-full-acceptance`. P8-B 동안 product code는 바뀌지 않았습니다. 이 commit에서 Compose production stack을 다시 build했고, `db`, `backend`, `frontend`가 healthy였으며 `/health`는 `{"status":"ok","db":"up"}`을 반환했습니다.
- **시험 전 baseline:** posts 42(삭제 3), comments 2,076(삭제 81), `post_likes` 0, `assignment-v1` seed-history row 1개; 읽을 수 있는 post total 39.
- **방법:** 임시 Node.js API verifier(108 checks, 상태 변경마다 `psql`로 교차 확인)와 실제 `375 × 778` viewport(`innerWidth`와 document `scrollWidth` 375)의 Playwright Chromium verifier를, 독립 section들과 연속 end-to-end 경로 하나로 실행했습니다. script는 session scratchpad에 두었고 repository artifact가 아닙니다.

### 요청자 (V-010, V-011, V-012)

- post 13에 `p8b_req_a`가 남긴 임시 좋아요(`post_likes`의 `user_id = p8b_req_a`)는 header만 있으면 `isLiked: true`, cookie만 있으면 true, 둘 다 없으면 false로 읽혔습니다. 둘 다 있으면 양방향 모두 header가 이겼습니다(header 다른 user + cookie A → false; header A + cookie 다른 user → true). 빈 header는 cookie로 fallback했습니다.
- 기본값: header나 cookie 없이 toggle하면 `user_id = apr_tester`로 저장되었고, header `apr_tester`에게는 보이고 다른 요청자에게는 보이지 않았습니다. identity 없이 만든 post의 author는 `apr_tester`였고 cookie `apr_tester`로 삭제할 수 있었습니다.
- Browser: `/dev-user` → `mintcream`으로 product 요청이 `x-user-id=mintcream`을 실었고, 좋아요 상태, 댓글 작성자, 삭제 affordance가 그 요청자를 따랐습니다.

### 목록 / pagination (V-020, V-021)

- limit 7, 10, 50의 API 순회(6/4/1 pages, 크기 `7/7/7/7/7/4`, `10/10/10/9`, `39`)가 읽을 수 있는 row에 대한 PostgreSQL `ORDER BY created_at DESC, id DESC`와 정확히 같았습니다. 모든 page에서 `total`은 39였고 중복·누락이 없었으며 마지막 `nextCursor`는 `null`이었습니다. seed에서 삭제된 post 9, 30, 31은 없었습니다. 기본 limit은 20 rows를 반환했습니다.
- 잘못된 입력: `limit` 0, 51, -1 → 400 `INVALID_LIMIT`; `limit=abc` → 400 `INVALID_PARAMETER`; 잘못되거나 빈 `cursor` → posts와 comments 모두 400 `INVALID_CURSOR`.
- Browser(당시 page size 10): 첫 `limit=10` 요청 정확히 1개; scroll하자 `nextCursor`로 이어진 요청 4개로 `10/10/10/9` row가 추가되었습니다. 39 row는 고유하고 API 순서였으며, 마지막 page 이후 scroll은 요청을 보내지 않았습니다.

### 응답 형태 (V-070, V-071)

- 목록: `{ items, total, nextCursor }`. 각 item은 정확히 `id`, `title`, `content`, `likeCount`, `commentCount`, `isLiked`, `author`, `createdAt`을 가지며, `createdAt`은 ISO-8601 UTC입니다(sample `2026-08-30T13:55:54Z`).
- 상세: envelope 없는 같은 단일 object이며, 목록 item과 byte 단위로 같습니다. PATCH는 `author`와 `createdAt`이 바뀌지 않은 완전한 수정 object를 반환했습니다.
- 댓글: `{ items, total, nextCursor }`, item은 정확히 `id`, `postId`, `content`, `author`, `createdAt`.
- 좋아요 toggle: 정확히 `{ isLiked, likeCount }`.

### 좋아요 (V-050)

- post 13의 API: A와 B 모두 like row 없이 false로 시작했습니다. count는 `856 → 857 → 858 → 857 → 856`(A on, B on, A off, B off)으로 움직였습니다. A가 on인 동안 B는 false였고, A가 off된 뒤 B는 true를 유지했으며, 목록은 A의 `isLiked`와 count를 반영했고, 실행은 like row 0개로 끝났습니다.
- Browser: 상세 하트(`aria-pressed`)와 count가 정확히 toggle 요청 1개로 갱신되었습니다. 목록으로 돌아오자 row의 하트가 좋아요 accent와 동기화된 count를 보였습니다. `mintcream`으로 바꾸자 post는 +1에서 좋아요 안 함으로 읽혔고, mintcream의 toggle로 +2가 되었으며 목록도 반영했습니다. 두 요청자 모두 되돌렸고, reload와 database가 원래 856, row 0과 같았습니다.

### 댓글 읽기 (V-060, V-061, V-065)

- API: posts 10 (0), 24 (1), 1 (2), 3 (3), 4 (27)을 `limit=2` 후 `limit=20`으로 순회했고, PostgreSQL의 읽을 수 있는 ID와 순서가 정확히 같았으며 `total`과 상세 `commentCount`도 같았습니다. post 2 (1,411)는 두 번 전체 순회했고(`2` 후 `50`: 30 pages; `2` 후 `20`: 72 pages) 중복·누락 없이 마지막 cursor가 null이었습니다.
- Browser(당시 continuation 20): 각 post는 cursor 없는 `limit=2` 요청 1개를 보내고 API 순서로 최대 2개 댓글을 보였습니다. 더보기는 3과 27에서만 나타났습니다. 더보기 전 scroll은 아무것도 보내지 않았습니다. 더보기 후 `nextCursor`로 이어진 `limit=20` 요청이 댓글 3개와 27개를 모두 render했고, 더보기가 사라졌으며, null cursor 이후 요청은 없었습니다.
- 재방문: 같은 session에서 post 4로 돌아오자(reload 없음, window marker로 확인) 새 `limit=2` 요청 1개, continuation 0개, 더보기와 함께 댓글 2개가 보였습니다.
- Browser의 post 2: 더보기와 함께 2개 표시, 이후 범위가 정해진 `limit=20` page(요청 7개 후 122 row).

### 댓글 mutation (V-062, V-063, V-064)

- post 1(댓글 2개)에서 `apr_tester`로 browser 실행: POST 1번으로 새 댓글이 author `apr_tester`로 맨 앞에 왔고, heading은 3이 되었으며, 더보기가 나타났고, 목록 count는 +1이 되었습니다.
- `삭제`는 요청자 본인 댓글에만 나타났습니다. 취소는 DELETE를 보내지 않았고, 확인은 정확히 1번 보냈습니다. 댓글이 사라졌고, heading과 목록 count는 2로 돌아왔으며, row는 `is_deleted = true`로 남았고, 반복 DELETE는 404를 반환했습니다.
- `mintcream`이 UI로 댓글을 만들었습니다(author `mintcream`, 그 댓글에만 `삭제`). `apr_tester`는 `삭제` 없이 그 댓글을 봤고, 직접 `apr_tester` DELETE는 row 변경 없이 403을 반환했습니다. 이후 mintcream이 UI로 삭제했습니다.
- API: post 소유자가 다른 요청자의 댓글을 DELETE하면 403이었습니다. 삭제된 댓글은 read와 count에서 제외되었고, 소유자든 비소유자든 반복하면 404 `COMMENT_NOT_FOUND`였습니다.

### Post validation과 CRUD (V-030, V-031, V-032, V-033, V-040, V-041)

- **Backend:** 빈 제목, 없는 제목, 공백만 있는 제목, U+3000만 있는 제목; 빈 내용, 없는 내용, 공백만 있는 내용; ASCII 21개, emoji 21개, 한국어 21 code point에 대해 400 JSON error. row는 만들어지지 않았습니다. ASCII 20개, emoji 20개(UTF-16 unit 40개), 한국어 20 code point는 201을 반환했습니다. PATCH는 emoji 21개 제목을 거부했습니다.
- **Frontend:** 같은 잘못된 경우에 `작성 완료`는 비활성으로 남았고, 강제 `requestSubmit()`도 요청 0개를 보냈습니다. 20 code point 제목 세 개는 활성화했고, 길이 초과 안내가 나타났습니다.
- **작성(browser):** FAB가 `/posts/new`를 열었습니다. 20 code point 제목(UTF-16 unit 21개)과 여러 줄 내용으로 POST 1번(201). app은 `/`로 돌아갔고 post가 맨 앞, 목록 total +1이었습니다. `게시글이 등록되었습니다.` toast가 한 번 나타나고 사라졌습니다. 서버 author는 `apr_tester`, 좋아요 0, 댓글 0, `isLiked: false`였고 상세도 같았습니다.
- **수정(browser):** 본인 글 메뉴가 sheet를 열었고(당시 bottom sheet), `수정하기`가 정확한 제목과 내용이 채워진 form을 열었습니다. PATCH 1번으로 상세가 갱신되었고, 목록 cache가 수정된 제목을 보였으며, reload와 API로 `createdAt`이 바뀌지 않은 채 저장되었음을 확인했습니다.
- **삭제(browser):** `삭제하기`가 sheet를 dialog로 바꿨고, 확인하자 DELETE 1번 후 해당 row 없이 `/`로 돌아갔습니다. total은 39로 돌아왔고, 직접 방문하면 `게시글을 찾을 수 없습니다.`가 보였으며, row는 `is_deleted = true`로 남았고, 반복 DELETE는 404를 반환했습니다.
- **Ownership:** post 2(Roidl_08)는 `apr_tester`에게 메뉴를 보이지 않았고, `/posts/2/edit`은 form 없이 `이 게시글을 수정할 권한이 없습니다.`를 보였습니다. `Roidl_08`로 바꾸자 메뉴가 보였습니다. 비소유자 PATCH와 DELETE는 post 변경 없이 읽을 수 있는 상태로 403을 반환했습니다.

### Soft delete (V-040, V-063, V-065)

- `p8b_owner`의 post에는 소유자와 비소유자의 댓글, 좋아요 두 개가 있었습니다. 읽을 수 있는 동안 비소유자는 PATCH와 DELETE에서 403을 받았습니다.
- 소유자의 DELETE(204) 후:
  - post row는 `is_deleted = true`로 남았고, 두 하위 댓글은 `is_deleted = false`를 유지했으며, like row 두 개도 남았습니다.
  - 목록은 그 post를 제외했고 total은 1 줄었습니다.
  - 모든 product route가 소유자와 비소유자 모두에게 404 `POST_NOT_FOUND` 또는 `COMMENT_NOT_FOUND`를 반환했습니다: 상세, 댓글 읽기, PATCH, 반복 DELETE, 좋아요 toggle, 댓글 작성, 각 하위 댓글을 작성자 본인이 삭제.
  - 그 404들은 database를 바꾸지 않았습니다.
- 이는 문서화된 model과 같습니다: 읽을 수 있는 post = 삭제되지 않음; 읽을 수 있는 댓글 = 삭제되지 않았고 parent가 읽을 수 있음; 유효한 좋아요 = parent가 읽을 수 있고 관계가 있음. 삭제된 resource는 ownership 확인 전에 404를 반환합니다.

### 오류 경로

- post 작성, PATCH, 댓글 작성의 잘못된 JSON → 400 `INVALID_REQUEST`; 빈 댓글 → 400 `INVALID_CONTENT`.
- 없거나 seed에서 삭제된 post(상세, 댓글, 좋아요) → 404 `POST_NOT_FOUND`; 없는 댓글 → 404 `COMMENT_NOT_FOUND`; 숫자가 아닌 post ID → 400 `INVALID_PARAMETER`.
- seed post 2의 비소유자 PATCH 또는 DELETE → 403 `FORBIDDEN`; seed 댓글의 비소유자 삭제 → row 변경 없이 403.
- 모든 오류는 HTML이나 stack trace 없는 JSON `{ code, message }` body였고, `/health`는 200을 유지했습니다.

### 연속 375px end-to-end (V-080 회귀)

- 경로는 다른 cookie에서 시작해 Chromium session 하나로 실행했습니다: `/dev-user` → `apr_tester` → 목록 → infinite scroll → post 13 상세 → 좋아요 on → 댓글 작성 → 댓글 삭제 → 좋아요 off → 뒤로 → FAB → 작성 → toast → 만든 글 열기 → sheet → 수정(채워진 값) → 저장 → sheet → 삭제 → 목록.
- 모든 화면과 overlay의 `scrollWidth`는 375였습니다. FAB, 댓글 composer, form action bar는 viewport 안에 고정되어 있었고, 수동 reload 없이 상태가 갱신되었습니다.
- page error, console error, 4xx/5xx 응답이 없었습니다. post 13의 좋아요와 댓글 상태는 복원했습니다.

### 발견 사항 (product 변경 없음)

- browser의 자동 `GET /favicon.ico`는 starter 이후 favicon이 없어 404를 반환합니다. app 요청이 아니며 기능 영향이 없어, 추측성 변경 금지 규칙에 따라 그대로 두었습니다. *(이후 별도 PR에서 app icon이 추가되었습니다: `frontend/app/icon.svg`.)*
- Chromium은 UI DELETE 요청의 `204` 응답이 도착한 뒤 `net::ERR_ABORTED`를 log합니다. client는 의도적으로 204 body를 읽지 않으므로(P3) 읽지 않은 stream이 취소됩니다. `requestfinished` trace와 database가 확인하듯 요청은 성공합니다. 이동으로 취소된 Next.js RSC prefetch도 `ERR_ABORTED`를 보고합니다. 둘 다 실패한 product 요청이 아닙니다.
- verifier 실수 다섯 개를 product 변경 없이 고쳤습니다.
  - Tomcat이 버린 잘못된 percent-encoded cursor;
  - boolean을 text로 비교한 정리 단계;
  - 21 code point짜리 "20" fixture(UI가 올바르게 거부);
  - 고정 action bar가 아니라 button에 대한 고정 위치 확인;
  - `Cookie` header를 빠뜨린 Playwright `headers()`, `allHeaders()`로 수정.

### 자동 / static 확인

- `backend/gradlew.bat clean test --no-daemon`: `BUILD SUCCESSFUL`, 51 tests, 0 failures/errors/skips.
- `npm run typecheck`: exit 0.
- Production build:
  - Local: 생성된 `frontend/.next`를 지운 뒤(git-ignored; 이번에는 삭제가 허용됨) `npm run build`가 `/`, `/_not-found`, `/dev-user`, `/posts/[postId]`, `/posts/[postId]/edit`, `/posts/new`로 성공적으로 compile되었습니다. 깨끗한 local build입니다.
  - Docker: `docker compose build --no-cache frontend`도 성공했습니다. context는 `frontend/.dockerignore`로 `.next`와 `node_modules`를 제외합니다. image를 healthy로 다시 배포했고, 목록 browser section이 그에 대해 7/7로 통과했습니다.
- `git diff --check`: clean.
- 확인 후 runtime: `db`, `backend`, `frontend` healthy; `/health` `{"status":"ok","db":"up"}`; `/api/posts?limit=1` total 39; frontend root 200.
- Starter 계약: `frontend/app/dev-user/`와 `CorsConfig.java`에 대한 `git diff 38a9755 HEAD`가 비어 있습니다. `:3000`에서 `:8080`으로 `x-user-id` cookie를 실은 cross-origin browser 요청이, preflight가 필요한 PATCH와 DELETE를 포함해 내내 성공했습니다.

### 데이터 복원

- 읽을 수 있는 product 상태가 baseline과 같습니다: 읽을 수 있는 total 39, post 13은 856으로 돌아왔고 읽을 수 있는 post에 like row가 없으며, 읽을 수 있는 post의 임시 댓글은 모두 soft delete했고, seed row는 그대로입니다.
- soft delete된 P8-B artifact는 의도적으로 남겼습니다(row는 지우지 않음).
  - posts 43–50은 모두 `is_deleted = true`입니다.
  - comments 2079–2084는 `is_deleted = true`입니다.
  - comments 2077과 2078, `post_likes` row 두 개는 삭제된 post 47 아래에 있습니다. parent 삭제 근거이며, 물리적으로 남아 있지만 접근할 수 없고 count되지 않습니다.
- 최종 물리 상태: posts 50(삭제 11), comments 2,084(삭제 87), `post_likes` 2(읽을 수 있는 post에는 0), seed history 1. seed에서 삭제된 posts `9,30,31`과 seed에서 삭제된 comment ID 81개는 그대로입니다.
- 따라서 수정 불가 seed baseline `42|2076`에는 이제 runtime에 생성된 이 acceptance row들이 포함됩니다. 새 volume은 여전히 정확히 다시 seed합니다(V-003).

---

## P2A Read API Runtime Acceptance — 2026-09-29

- `backend/gradlew.bat test --no-daemon`이 `BUILD SUCCESSFUL`로 끝났고, cursor codec 정밀도/잘못된 입력, 요청자 우선순위/fallback, read web 계약/오류를 다루는 26 tests가 통과했습니다.
- `docker compose up -d --build`가 기존 P1 volume으로 application을 다시 build하고 시작했습니다. `db`, `backend`, `frontend`가 모두 `healthy`였고 backend `/health`와 frontend root가 각각 HTTP 200을 반환했습니다.
- `GET /api/posts?limit=7`이 읽을 수 있는 post 39개를 6 page로 순회했습니다. API ID는 PostgreSQL `(created_at DESC, id DESC)` 결과와 중복·누락 없이 정확히 같았고, `total`은 39를 유지했으며, 모든 page가 최대 7 row였고, 마지막 `nextCursor`는 null이었습니다.
- 삭제된 post ID `9`, `30`, `31`은 전체 순회에 없었고, 각각의 상세와 댓글 read는 내용 노출 없이 404를 반환했습니다.
- 읽을 수 있는 post 응답 39개 모두가 PostgreSQL에서 계산한 `base_like_count + post_likes`와 삭제되지 않은 댓글 수와 같았습니다. 요청자 mutation 근거 전에는 `post_likes`가 0 row였고 읽을 수 있는 모든 post가 `isLiked: false`를 반환했습니다.
- 읽을 수 있는 post 39개의 댓글을 limit 50으로 순회했고(총 69 pages) PostgreSQL의 읽을 수 있는 댓글 ID와 순서가 정확히 같았습니다. seed에서 삭제된 comment ID 81개는 하나도 나오지 않았습니다. post 2는 limit 2로 추가 순회했습니다: 읽을 수 있는 댓글 1,411개를 706 page에 걸쳐, 중복·누락 없이 마지막 cursor null.
- 고유한 임시 `post_likes` row로 header와 cookie 요청자 일치, 기본값 `apr_tester`, header 우선 원칙을 증명했습니다. row는 정리 단계에서 제거했고 `post_likes`는 0 row로 돌아갔습니다.
- 잘못된 post/comment cursor는 HTTP 400 `INVALID_CURSOR`, limit 0과 51은 HTTP 400 `INVALID_LIMIT`을 반환했습니다.
- runtime verifier는 Node.js로 JSON을 parse했고 실행 후 제거했습니다. repository artifact가 아닙니다.

---

## P2B Mutation API Runtime Acceptance — 2026-09-29

- project 범위의 빈 volume `docker compose up -d --build`로 `db`, `backend`, `frontend`가 healthy가 되었고, `/health`는 database 상태 `up`과 함께 HTTP 200을 반환했습니다.
- baseline: 물리 42/읽을 수 있는 39 posts, 물리 2,076/삭제되지 않은 1,995 comment rows, `post_likes` 0, `assignment-v1` seed-history row 1개. API에서 읽을 수 있는 댓글은 추가로 읽을 수 있는 parent post가 필요합니다.
- post ID `43`을 `p2b_user_a`로 만들었습니다. 파생 count는 0, 요청자 좋아요는 false였고, 목록 맨 앞에 나왔으며, 요청자 B의 PATCH/DELETE를 403으로 거부하고 소유자 PATCH/DELETE를 받아들였고, soft delete 후에도 물리적으로 저장되어 있었습니다.
- Validation은 없거나 빈 제목·내용, 21 code point 제목, 빈 댓글, 잘못된 JSON에 JSON 400 error를 반환했습니다. emoji code point 정확히 20개인 제목은 201을 반환했습니다.
- 두 요청자가 한 post를 독립적으로 toggle해 count가 `0 → 1 → 2 → 1 → 0`으로 움직였습니다. 각 요청자는 자신의 `isLiked`만 봤고, 최종 database like row 수는 0이었습니다.
- comment ID `2077`을 요청자 B가 만들었습니다. post 소유자 A는 삭제할 수 없었고(403), 요청자 B는 삭제할 수 있었으며(204), row는 `is_deleted = true`로 남았고, 파생 count는 1에서 0으로 돌아왔고, 반복 삭제는 404를 반환했습니다.
- post 삭제 후 목록 제외와 상세, 댓글 읽기/작성, PATCH, 좋아요 toggle, 반복 DELETE의 404 동작을 확인했습니다. 원래 post 42, comment 2,076, seed-history marker, seed 삭제 read 제외는 그대로였습니다.
- 정리는 이 APR Compose project의 `down -v` 후 `up -d`만 썼습니다. 복원된 baseline은 posts 42/39, 물리 2,076/삭제되지 않은 1,995 comment rows, 좋아요 0, seed-history row 1개, `/health` HTTP 200입니다. Node.js verifier는 제거했습니다.

---

## P3 Frontend Foundation Evidence — 2026-09-29

- `npm ci`가 갱신된 lockfile에서 설치했습니다. `@tanstack/react-query`는 5.104.0, `@tanstack/query-core` 5.104.0으로 해석되었고 다른 새 package는 없었습니다.
- `npm run typecheck`(`tsc --noEmit`, strict)가 exit 0이었습니다. 당시 `npm run build`(Next.js 15.4.6)가 `/`, `/_not-found`, `/dev-user` route로 성공했습니다.
- `docker compose up -d --build`가 기존 volume으로 backend와 frontend를 다시 build했고, `db`, `backend`, `frontend`가 `healthy`였습니다. `GET /health`, frontend `/`, frontend `/dev-user`가 각각 HTTP 200을 반환했습니다.
- P2 read smoke: `GET /api/posts?limit=2`가 `items`(2), `total`(39), 문자열 `nextCursor`와 함께 HTTP 200을 반환했고, item field가 `frontend/lib/api/types.ts` `Post`와 정확히 같았습니다.
- 임시 Node.js script로 `frontend/lib/api/client.ts` `request()`를 직접 실행했습니다: GET/POST/DELETE의 `credentials: "include"`, DELETE의 body·`Content-Type` 없음, POST의 JSON body와 `Content-Type`, fallback base URL, path 연결, `URLSearchParams` cursor encoding, parse 없이 `undefined`를 반환하는 204, `ApiError`로 바뀌는 non-JSON·network 실패, 그리고 live backend 대상: `nextCursor`로 page 이어 읽기, 삭제된 post 9 → 404 `POST_NOT_FOUND`, 잘못된 cursor → 400 `INVALID_CURSOR`. 14개 확인이 모두 통과했고 script는 제거했습니다.
- 임시 고정 clock script로 `formatRelativeTime`을 확인했습니다: `0초 전`, `30초 전`, `3분 전`, `3시간 전`, `2일 전`, `1개월 전`, `1년 전`, 미래 `5분 후`, 잘못된 timestamp는 `null`. script는 제거했습니다.
- 범위: 당시 어떤 product 화면도 browser API 요청을 보내지 않았으므로, credential을 실은 browser cookie 흐름(`V-012`)은 P3에서 실행하지 않았습니다.

---

## P4 Post List Evidence — 2026-09-29

- `npm run typecheck`가 exit 0이었습니다. 오래된 local `.next`를 지운 뒤 clean `npm run build`가 성공했고, `/`는 이제 client 목록 화면이었습니다.
- `docker compose up -d --build frontend`가 실행 중인 healthy `db`/`backend`에 맞춰 frontend를 다시 build했고, frontend `/`는 HTTP 200을 반환했습니다.
- 임시 Playwright(Chromium) script가 `375 × 778` viewport에서 그 runtime을 대상으로 48/48 확인을 통과했습니다. script와 screenshot은 session scratchpad에 두었고 commit하지 않았습니다.
- Pagination(당시 page size 10): scroll 전 정확히 요청 1개 `GET /api/posts?limit=10`(cursor 없음). scroll하자 정확히 요청 4개가 생겼고, 모두 `limit=10`이며 이후 cursor는 이전 `nextCursor`와 같았습니다. page 크기 `10/10/10/9`, 모든 page에서 `total: 39`, 마지막 `nextCursor: null`, 추가 scroll 후 요청 없음.
- Rendering: 39 rows, 중복 post ID 없음, DOM 순서가 API 최신순 순회와 같음; 모든 row가 `29일 전` / `1개월 전` 같은 상대 시간을 보입니다.
- Avatar: 모든 row가 제공된 asset 세 개 중 하나를 쓰고, 한 작성자는 row에 관계없이 한 avatar에 대응하며, reload 후에도 mapping이 같습니다.
- 이동: row를 누르면 `/posts/1`이 열렸고, 작성 FAB는 `/posts/new`를 열었습니다. 당시 두 page 모두 P5/P6 전이라 404였고, 관찰한 404 60개는 모두 `/posts/{id}`와 `/posts/new`로의 production `Link` prefetch/이동이었습니다.
- State(Playwright route interception으로 강제): 첫 loading(`게시글을 불러오는 중입니다.`), empty(`아직 작성된 게시글이 없습니다.`), TanStack 기본 retry(시도 4회) 후 동작하는 `다시 시도`가 있는 첫 error, 다음 page loading(`게시글을 더 불러오는 중입니다.`), 로드된 10 row를 유지하고 추가 scroll에 자동 retry하지 않으며 자체 `다시 시도`로 복구되는 다음 page error. *(Historical: 시도 4회 기본값과 page size 10은 2026-09-29에 적용된 값입니다. PR #40 이후 실패한 read는 2번 시도하며(retry 1회; 4xx는 없음) page size는 15입니다. "F-06" 참고.)*
- 이 실행 중 발견해 고친 회귀: TanStack Query v5는 `fetchNextPage`만 실패해도 `status: "error"`를 설정하므로, 첫 build는 첫 error block도 render했습니다(`다시 시도` button 두 개). 첫 error block은 이제 `data === undefined`를 요구하며, 재실행에서 retry control이 하나임을 확인했습니다.

---

## P5A Post Detail / Like / Comment Read Evidence — 2026-09-29

- 구현 commit `2af23aab5cf1009c6e39a359be5abb01a7c34566`. `npm run typecheck`가 exit 0; clean `npm run build`가 `ƒ /posts/[postId]`를 만들었고; `git diff --check`가 깨끗했습니다. `docker compose up -d --build frontend`가 healthy `db`/`backend`에 맞춰 production frontend를 다시 build했습니다.
- `375 × 778`의 임시 Playwright(Chromium) script가 63/63 확인을 통과했습니다. script와 screenshot은 session scratchpad에 두었고 commit하지 않았습니다.
- 상세: 목록 row가 같은 제목으로 `/posts/1`을 열었고, 뒤로 가기 button은 `/`로 돌아갔습니다. 직접 `/posts/4`는 제목, 작성자, 제공 avatar, `YY.MM.DD` 날짜, 내용, `좋아요 1,109`, `댓글 (27)`을 render했습니다. nav 375×48, y=56부터 content, 가로 scroll 없음, 제목 Bold 18/26, 작성자 Bold 15/22, 본문 Regular 14/18, heading Bold 16/24 (`#292828`).
- 없음/삭제: `/posts/9`(seed 삭제)와 `/posts/99999`는 `목록으로` link와 함께 `게시글을 찾을 수 없습니다.`를 보였습니다. 404는 retry하지 않았고(상세 요청 1개) 댓글 요청은 보내지 않았습니다. `/posts/abc`는 API 요청 없이 같은 state를 보였습니다. 상세 loading과 (기본 retry 후) 강제 500에서 동작하는 `다시 시도`도 확인했습니다.
- 좋아요(post 13, `apr_tester`): 좋아요 안 함 → 함, `856 → 857`, 채운 하트 `rgb(255, 141, 161)` vs 좋아요 안 함 `rgb(151, 158, 169)`, `aria-pressed` 갱신; reload가 backend와 같았고; 함 → 안 함으로 `856` 복원. 같은 tick의 두 click과 붙잡은 요청 중의 강제 click은 정확히 toggle 요청 1개를 만들었고, pending 중 button은 비활성이었습니다. 강제 500은 상태를 `856` 좋아요 안 함으로 남기고 `좋아요를 반영하지 못했습니다. 다시 시도해 주세요.`를 보였으며 button을 다시 활성화했습니다.
- 요청자 분리: API로 `apr_tester`가 post 13에 좋아요한 상태에서, 수정하지 않은 `/dev-user`로 `mintcream`을 고르자 post가 `857`에 좋아요 안 함으로 보였고, `mintcream`으로 toggle하자 `858`이 되었으며 backend에서 두 user 모두 좋아요 상태였습니다. 임시 좋아요는 모두 되돌렸습니다(post 13은 `856`, 두 user 모두 좋아요 안 함).
- 목록 일관성: route interception으로 목록 refetch를 막은 채 상세에서 최신 post에 좋아요하고 `/`로 돌아오자, 갱신된 목록 cache에서 늘어난 count를 보였습니다. 좋아요는 되돌렸습니다.
- 댓글 수: post 10 (0)은 목록, 더보기, 빈 메시지 없이 `댓글 (0)`만 보였고; post 24 (1)와 post 1 (2)은 더보기 없이 모든 댓글을 최신순으로 보였습니다. 각각 정확히 댓글 요청 1개, cursor 없는 `limit=2`를 보냈습니다.
- 더보기 + infinite scroll(당시 continuation 20): posts 3(댓글 3개)과 4(27개)는 먼저 `limit=2` 요청 1개를 보내고 최신 2개와 `댓글 더보기`를 보였습니다. 더보기 전 바닥까지 scroll해도 추가 요청이 없었습니다. 더보기 후 요청은 `limit=20`이었고 각 cursor는 이전 `nextCursor`와 같았으며, 모든 댓글이 중복 없이 최신순으로 render되었고, 더보기가 사라졌으며, 마지막 `nextCursor`는 `null`이고 그 뒤 요청은 없었습니다(post 3: 요청 2개, post 4: 요청 3개). post 2 (1,411)는 범위가 정해진 `limit=20` page로 점진적으로 로드되었습니다.
- 댓글 state: 첫 loading, 첫 error(post 본문은 계속 보임)와 retry, continuation loading, 로드된 댓글 22개를 유지하고 첫 error block 없이 `다시 시도` 하나를 보이며 scroll에 자동 retry하지 않고 27로 복구되는 continuation error.
- 이 실행 중 발견해 고친 회귀: TanStack의 `isPending`이 `mutate` 한 scheduler tick 뒤에 render되므로 같은 tick의 double click이 toggle 두 번을 보냈습니다. `useTogglePostLike`는 이제 in-flight ref로 막습니다. 실패한 실행에서 남은 test 좋아요는 통과 실행 전에 되돌렸습니다.
- P4 회귀: P4 script가 48/48(39 posts를 10/10/10/9로, 중복 없음, FAB → `/posts/new`)로 404 응답 0개로 통과했습니다. 목록의 상세 link prefetch는 이제 성공하는 background 요청을 보내고, `/posts/new` prefetch는 발생하지 않습니다.

### P5A Review Fix — Same-Session Revisit — 2026-09-29

- 결함(PR #9 review): 상세 화면을 떠난 뒤에도 댓글 page가 공유 `QueryClient`에 남았습니다. `375 × 778`의 임시 Playwright 재방문 script가 수정 전 production build(`14ab5a0`)에서 재현했습니다: 전체 reload 없이 post 4에 다시 들어가자 mount refetch가 더보기 전에 cache된 `limit=20` continuation 요청 두 개를 보냈고, 더보기가 50 ms 안에 댓글 27개를 모두 보였습니다(12개 중 6개 확인 실패).
- 수정 commit `ddc1a9da6e4c91eb0d8dfe3b1a4c9c886dbf3c1c`(`useComments` `gcTime: 0`). `npm run typecheck` exit 0, clean `npm run build` 성공, `git diff --check` clean, production frontend container 재build.
- 수정된 build에서 재방문 script 12/12: 방문 1은 `limit=2` 요청 1개, 이후 더보기와 scroll로 continuation page 2개를 로드했습니다(27개 표시). 앱 내 뒤로 가기 button으로 돌아가 같은 row를 누르자(전체 reload 없음, window marker로 확인), scroll 후에도 더보기 전에는 정확히 새 `limit=2` 요청 1개와 continuation 요청 0개였고, 보이는 댓글은 최대 2개였습니다. 더보기 50 ms 후에도 2개만 보였습니다. 첫 continuation은 재방문의 첫 page `nextCursor`로 `limit=20`이었고 22개를 render했습니다. scroll로 cursor chain이 API 최신순으로 중복 없이 27까지 이어졌고, 마지막 `nextCursor`는 `null`이며 그 뒤 요청은 없었습니다.
- 같은 build에서 재실행: P5A script 63/63, P4 script 48/48(404 응답 0). post 13 좋아요 상태 복원을 확인했습니다(`856`, `apr_tester`와 `mintcream` 모두 좋아요 안 함).

---

## P5B Comment Mutation Evidence — 2026-09-29

- 구현 commit `07c93724df195b2158d459d98eddd399797faa9b`(frontend만; backend, schema, dependency 변경 없음). `npm run typecheck` exit 0; clean `npm run build` 성공; `backend/gradlew.bat clean test` 49/49 변화 없이 통과; `git diff --check` clean. 이 code로 production frontend container를 다시 build했습니다.
- `375 × 778`의 임시 Playwright(Chromium) script가 Compose production stack에 대해 **52/52** 확인을 통과했습니다. script와 screenshot은 session scratchpad에 두었습니다.
- Composer: 댓글 0개를 포함해 모든 post에서 y=692의 고정 375×86 영역. 공백만 있는 입력은 `등록`을 비활성화하고 Enter도 요청을 보내지 않습니다. 입력은 작성 성공 후에만 비워집니다. page 바닥의 content는 composer 위에 남고, 끊김 없는 긴 댓글은 가로 overflow 없이 줄바꿈됩니다.
- `apr_tester`로 post 23(skin_beginner의 글, 댓글 0개)에서 작성 경계:
  - 0→1: `댓글 (1)`, 서버 작성자 `apr_tester`, 더보기 없음.
  - 1→2: 둘 다 최신순, 더보기 없음.
  - 2→3: 접힌 상태는 여전히 최신 2개를 보이고 `댓글 더보기`가 나타나며, 더보기는 추가 요청 없이 세 번째를 보입니다.
  - Reload가 서버와 같았습니다(3).
  - post 4에서 3+→+1(27→28): 새 댓글이 맨 앞, 더보기 유지, 펼친 목록이 서버와 같음.
- 같은 post에서 삭제 경계:
  - 3→2(첫 page 항목, 접힌 상태): 더보기가 사라지고 남은 2개가 보입니다.
  - 2→1, 이어서 1→0: `댓글 (0)`만, 새 빈 메시지 없음, composer 유지.
  - Reload가 서버와 같았습니다(0).
  - Roidl_08의 post 26에 있는 seed comment 1834(`apr_tester`): 그 댓글에만 `삭제` 표시. 취소는 DELETE를 보내지 않았습니다. 확인하자 3→2, 더보기가 사라졌고 삭제가 유지되었습니다.
- post 8(58)의 펼친 여러 page 상태:
  - continuation page 2개를 로드한 뒤 본인 댓글 1613(continuation page 안)을 삭제하자 그것만 제거되었습니다(`댓글 (57)`).
  - 펼친 상태에서 작성하자 새 댓글이 중복 없이 맨 앞에 왔습니다(`댓글 (58)`).
  - 끝까지 scroll하자 서버와 정확히 같았습니다(58).
- Cursor 경계: `Jelin`(cookie)으로 마지막 로드 row 1608(42번째, page의 cursor row)을 삭제했습니다. 다음 continuation 요청은 삭제된 row의 `(createdAt, id)` cursor(`…|1608`)를 썼고, 전체 목록은 누락·중복 없이 서버(57)와 같았습니다.
- Count 동기화(post 1, 목록 row 1, route interception으로 목록 refetch 막음):
  - 작성 후 돌아가자 목록 count가 +1이었습니다.
  - 상세에 다시 들어가자 `댓글 (3)`이었습니다.
  - 삭제 후 목록 count가 돌아왔습니다.
  - 목록 reload가 서버와 같았습니다.
- 실패:
  - POST 500, 400, network abort는 각각 입력 text를 유지하고, 댓글을 추가하지 않았으며, count를 바꾸지 않았고, `댓글을 등록하지 못했습니다. 다시 시도해 주세요.`를 보였습니다. retry는 정확히 댓글 1개를 만들었습니다.
  - DELETE 500, 403, 404는 각각 `댓글을 삭제하지 못했습니다. 다시 시도해 주세요.`와 함께 dialog를 열어 두고, `예`를 다시 활성화했으며, 댓글과 count를 바꾸지 않았습니다. retry는 성공했습니다.
- 중복 방지: 같은 tick의 `requestSubmit()` 두 번은 정확히 POST 1개와 댓글 1개를 만들었습니다. 같은 tick의 `예` click 두 번은 정확히 DELETE 1개를 만들었습니다.
- Dialog: `aria-modal="true"`와 `aria-labelledby`가 있는 `alertdialog`, (24, 257)의 327×264, 제목 `삭제하기`, 본문 `삭제한 댓글은 복구할 수 없습니다.` / `정말 삭제 하시겠습니까?`, action `취소` / `예`.
- 요청자 전환:
  - `/dev-user` → `mintcream` 후 Jelin의 post 41에 작성: 응답과 UI 작성자 `mintcream`, mintcream의 댓글에만 `삭제`.
  - 새 `apr_tester` load에서는 그 댓글에 `삭제`가 없었습니다.
  - Backend: 그 댓글에 대한 `apr_tester` DELETE는 403; mintcream 본인 삭제는 204, 반복은 404.
- 회귀:
  - mutation 후 P5A 재방문 script가 12/12로 통과했습니다(post 4 댓글 28개: 새 `limit=2`, 더보기 전 continuation 요청 0).
  - 다시 seed한 뒤: P4 48/48(예상치 않은 404 0), P5A 63/63, keyset 순회/tie script(post 39개; post 2의 댓글 1,411개를 72 pages; tie fixture) 모두 통과.
- Test script 유지보수(product 변경 아님): 이전 P5A/재방문 script는 `li p:last-child`로 댓글 text를 읽었는데, 본인 댓글이 이제 `삭제` button으로 끝나므로 `p:nth-of-type(2)`를 읽게 했습니다. P5A의 댓글 0개 "추가 빈 메시지 없음" 확인은 이제 section text에서 composer form을 제외합니다.
- DB baseline: soft delete된 test 댓글이 물리 row로 남으므로, schema를 drop하고 Flyway와 seed runner가 다시 만들게 해 baseline을 복원했습니다. 최종 상태: posts 42(삭제 3), comments 2,076(삭제 81), 좋아요 0, `seed_history = assignment-v1`.

---

## P6 Post CRUD Evidence — 2026-09-29

- 구현 commit `1f8a21f95fbbc132266e9f3d76f6154504127fd1`(frontend만). 확인: `npm run typecheck` 0 errors, clean `npm run build`(route `○ /posts/new`, `ƒ /posts/[postId]/edit`), backend `gradlew clean test` 49/49, `git diff --check` clean. 이 code로 production frontend container를 다시 build했습니다.
- `375 × 778`의 임시 Playwright(Chromium) script가 다시 seed한 뒤 **74/74** 확인을 통과했습니다. script와 screenshot은 session scratchpad에 두었습니다.
- **Validation(작성과 수정):** 빈 제목이나 내용, 공백만, U+3000만, ASCII 21자 제목, emoji 21개 제목(UTF-16 unit 42개)은 모두 button을 비활성으로 두었고, 강제 `requestSubmit()`도 요청 0개를 보냈습니다. ASCII 20자 제목, emoji 20개 제목(UTF-16 unit 40개), 한국어 제목은 받아들여졌습니다. frontend는 Java `codePointCount`처럼 code point를 세고 Java `isBlank`처럼 공백을 다룹니다. 값은 trim하지 않고 보냅니다. 제목에서 Enter는 제출하지 않습니다.
- **작성:**
  - FAB가 `/posts/new`를 엽니다(`LIST-006`).
  - POST 500과 network abort 모두 form 값과 URL을 유지하고, `게시글을 등록하지 못했습니다. 다시 시도해 주세요.`를 보였으며, toast를 보이지 않았고, 아무것도 만들지 않았습니다.
  - 같은 tick의 double submit은 정확히 POST 1개를 보냈습니다.
  - 서버 post는 author `apr_tester`, `likeCount` 0, `commentCount` 0, `isLiked` false, 정확한 20 code point 제목과 여러 줄 내용이었습니다.
  - 목록 첫 page refetch를 막은 상태에서 목록은 cache 갱신으로 새 post를 맨 앞에 보였습니다.
  - 이어서 전체 순회가 서버와 같았습니다(total 40, 고유, 새 글 맨 앞).
- **Toast:** `게시글이 등록되었습니다.`가 181×40, x=97.5, y=657에 `pointer-events: none`으로 한 번 나타났습니다. 약 2.5 s 후 사라졌고 목록 재방문이나 reload에서 다시 나타나지 않았습니다.
- **수정(본인 글):**
  - trigger가 보였고, sheet(P6 당시 bottom sheet `PostActionSheet`)는 y=554의 375×224였습니다. Escape, scrim click, `취소`가 각각 닫았습니다.
  - `수정하기`가 같은 form(`글수정` / `수정 완료`)과 정확한 채움으로 `/posts/{id}/edit`을 열었습니다. 빈 제목이나 21 code point 제목은 button을 비활성화했습니다.
  - PATCH 400, 403, 404, 500, network 실패는 각각 값과 URL을 유지하고, 오류를 보이며, retry를 허용했고, 서버에서 아무것도 바꾸지 않았습니다.
  - 같은 tick의 double save는 정확히 PATCH 1개를 보냈습니다.
  - 성공 시 상세가 응답을 바로 보였고, 뒤로 가기는 목록으로 돌아가 cache에서 수정된 제목을 보였습니다(첫 page refetch 막음). reload 후 서버 제목과 내용이 같았고 `createdAt`은 바뀌지 않았습니다.
- **다른 사용자 글:**
  - post 2(Roidl_08)에는 trigger가 없었고, `/posts/2/edit`은 form 없이 `이 게시글을 수정할 권한이 없습니다.`를 보였습니다.
  - `/posts/99999/edit`과 `/posts/abc/edit`은 not-found를 보였습니다.
  - `apr_tester`의 PATCH와 DELETE는 403을 반환했고 post는 바뀌지 않았습니다.
- **삭제(본인 글):**
  - `삭제하기`가 sheet를 닫고 (24, 257)의 327×264 dialog를 `삭제한 글은 복구할 수 없습니다.` / `정말 삭제 하시겠습니까?`로 열었으므로 overlay는 하나만 열렸습니다. `취소`는 DELETE 0개를 보냈습니다.
  - DELETE 403, 404, 500, network 실패는 각각 `게시글을 삭제하지 못했습니다. 다시 시도해 주세요.`와 다시 활성화된 `예`로 dialog를 유지하고, 상세에 머물렀으며, post를 읽을 수 있게 남겼습니다.
  - 같은 tick의 double confirm은 정확히 DELETE 1개를 보내고 URL을 `/`로 replace했습니다.
  - 목록 refetch를 막은 상태에서 post는 cache된 목록에 없었습니다. 전체 순회가 서버와 같았습니다(total 39).
  - 직접 재방문하면 not-found였고, DB row는 `is_deleted = true`로 남았습니다.
- **Cache된 목록 삭제:**
  - 중간 row(post 5, row 5)를 삭제하자 cache된 page에서 그 row만 제거되었습니다.
  - 두 번째 cache page 삭제는 `booster_0`으로 post 11을 대상으로 했고, 역시 그 row만 제거했습니다.
  - 두 경우 모두 scroll하면 cache된 cursor에서 이어졌고, 전체 목록이 서버와 같았으며, 누락·중복이 없었습니다.
  - 정정(Deletion Policy Hardening): 같은 실행에서 post 5가 이미 삭제되었으므로, 그 순간 post 11은 page의 cursor row가 아니라 row 9였습니다. 실제 cursor row 삭제는 별도로 검증했고("Deletion Consistency Evidence"의 시나리오 C), P6 script는 이제 실행 시점에 row 10을 고릅니다(row 10/10의 post 12를 삭제하고 통과).
- **`/dev-user` → `mintcream`:**
  - 만든 post의 author는 `mintcream`이었고, mintcream은 trigger를 봤지만 새 `apr_tester` load는 보지 못했습니다.
  - `apr_tester` PATCH/DELETE는 403을 반환했습니다.
  - mintcream이 UI로 삭제했습니다(이후 404).
- **375×778 (V-080):** 목록, 상세, action sheet, 게시글 삭제 dialog, 작성(내용 80줄을 끝까지 scroll), 수정의 `scrollWidth`가 모두 375였습니다. 고정 control은 보였고 content를 가리지 않았습니다.
- **commit 전 발견해 고친 회귀:** 작성이나 수정 성공 후 `router.replace`로 돌아가면 중복 history entry가 쌓여, 수정한 상세에서 뒤로 가기를 하면 같은 상세가 다시 보였습니다. form은 이제 자신을 연 앱 내 화면을 기억하고(`lib/formOrigin.ts`) 그 entry로 돌아갑니다. 직접 URL load는 여전히 replace합니다.
- **다시 seed한 뒤 회귀:**
  - P4 48/48과 P5A 63/63. P5A prefetch 확인은 이제 복원된 FAB prefetch가 절대 404가 아님을 기대합니다.
  - P5A 재방문 12/12.
  - Keyset 순회/tie script 통과.
  - P5B 52/52.
- **DB baseline:** 만들고 soft delete한 test post가 물리 row로 남으므로 suite마다 schema를 다시 seed했습니다. 최종 상태: posts 42(삭제 3), comments 2,076(삭제 81), 좋아요 0, `seed_history = assignment-v1`.

---

# 요청자 / 소유권

## V-010 — 기본 요청자

**요구사항:** `USER-003`

### 단계

`x-user-id` header나 cookie 없이 요청자에 따라 달라지는 endpoint를 호출합니다.

### 기대 결과

- 요청은 `apr_tester`가 보낸 것으로 취급됩니다.

### 실행 근거 — 2026-09-29

- Resolver test가 header/cookie 없음 fallback을 검증했습니다.
- 다른 임시 요청자가 like row를 가진 동안 header/cookie 없는 runtime 상세 read는 `isLiked: false`를 유지했습니다.

### P8-B 당시 후보 근거 — 2026-09-30

- header나 cookie 없는 toggle은 `post_likes.user_id = apr_tester`로 저장되었고 header `apr_tester`에게만 보였으며, identity 없이 만든 post의 author는 `apr_tester`였습니다.

**결과:** `PASS`

---

## V-011 — Header 요청자

**요구사항:** `USER-001`

### 단계

다음과 함께 요청자에 따라 달라지는 endpoint를 호출합니다.

```http
x-user-id: <known-author>
```

### 기대 결과

- Ownership과 `isLiked`가 header user 기준으로 판단됩니다.

### 실행 근거 — 2026-09-29

- Resolver test가 header만 있는 경우와 header 우선 원칙을 다뤘습니다.
- Runtime 상세 read는 `x-user-id`가 임시 좋아요 요청자와 같을 때만 `isLiked: true`를 반환했고, 일치하는 cookie 위에 다른 header를 주면 false였습니다.

### P8-B 당시 후보 근거 — 2026-09-30

- header만 있을 때 좋아요 요청자는 true로 읽혔습니다. cookie도 있을 때 header 우선이 양방향 모두 성립했고, 빈 header는 cookie로 fallback했습니다. header 요청자는 ownership도 결정했습니다(비소유자 PATCH/DELETE에 403).

**결과:** `PASS`

---

## V-012 — Cookie 요청자

**요구사항:** `USER-002`, `START-003`

### 단계

1. `/dev-user`로 user를 고릅니다.
2. 요청자 identity에 따라 달라지는 frontend/API 작업을 합니다.

### 기대 결과

- `x-user-id` cookie가 전송됩니다.
- Backend가 그 cookie user 기준으로 ownership/좋아요 상태를 판단합니다.

### P2A 부분 근거 — 2026-09-29

- Resolver test와 직접 runtime API 요청으로 backend가 `x-user-id` cookie를 받아 그 요청자 기준으로 `isLiked`를 판단함을 검증했습니다.
- 수정 불가 `/dev-user` frontend → backend의 credential browser 흐름은 당시 실행하지 않았으므로, 이 결합 시나리오는 당시 열려 있었습니다.

### P3 부분 근거 — 2026-09-29

- 공용 browser client `frontend/lib/api/client.ts`는 모든 요청에 `credentials: "include"`를 적용합니다. browser session이 아니라 직접 호출로 확인했습니다.
- 당시 어떤 product 화면도 browser 요청을 보내지 않았습니다. 전체 `/dev-user` → product 요청 → 요청자 반영 흐름은 P4/P5 또는 P8로 미뤘습니다.

### P4 근거 — 2026-09-29

- API로 최신 post에 `mintcream`의 임시 좋아요를 만들었습니다. Chromium에서 수정하지 않은 `/dev-user` page로 `mintcream`을 고르자 `/`로 이동했고, 목록 요청이 `Cookie: x-user-id=mintcream`을 실어 그 post를 `isLiked: true`로 반환했습니다. 이후 좋아요를 되돌렸습니다(`isLiked: false`, `likeCount`는 `1796`으로 복원).
- 이는 credential을 실은 browser 요청과 요청자별 좋아요 판단을 검증합니다. browser에 반영되는 ownership(본인 글 메뉴, 본인 댓글 삭제)은 당시 아직 render되지 않아 P5/P6로 남았습니다.

### P5A 근거 — 2026-09-29

- `/dev-user`에서 `mintcream`을 고르자 `apr_tester`의 좋아요와 독립적인 그 요청자의 `isLiked`로 상세가 보였고, cookie user의 toggle은 `mintcream`에게만 기록되었습니다. ownership UI(본인 글 메뉴, 본인 댓글 삭제)는 당시 아직 없었으므로(P5B/P6) 결합 시나리오는 당시 열려 있었습니다.

### P5B 근거 — 2026-09-29

- 이제 browser에서 댓글 ownership이 cookie 요청자를 따릅니다. `/dev-user` → `mintcream` 후 만든 댓글의 작성자는 `mintcream`이고 mintcream의 댓글에만 `삭제`가 보이며, 기본 `apr_tester` load에서는 보이지 않습니다. 게시글 소유자 메뉴(P6)는 당시 아직 없었으므로 결합 시나리오는 당시 열려 있었습니다.

### P6 근거 — 2026-09-29

- post 메뉴가 생기면서, `/dev-user` 후 실제 browser session에서 요청자에 따라 달라지는 모든 동작을 관찰했습니다. P4–P6에 걸쳐 credential cookie, 요청자의 좋아요 상태, 댓글 ownership, post ownership을 다룹니다. `mintcream`은 자신의 post 메뉴를 보고 `apr_tester`는 보지 못하며, backend는 mintcream의 post에 대한 `apr_tester` PATCH/DELETE에 403을 반환합니다.

### P8-B 당시 후보 근거 — 2026-09-30

- 앞의 부분 메모("당시 열려 있었음")는 historical이며, 이 결합 시나리오는 닫혔습니다.
- 당시 후보에서 직접 cookie만 있는 API read가 cookie 요청자를 판별했습니다. browser에서 `/dev-user` → `mintcream`은 product 요청에 `x-user-id=mintcream`을 보냈고, 좋아요 상태, 댓글 작성자, `삭제`가 이를 따랐습니다. `/dev-user` → `Roidl_08`은 post 2의 소유자 메뉴를 보였고, `apr_tester`는 보지 못했습니다.

**결과:** `PASS`

---

# 게시글 목록

## V-020 — 최신순 게시글 Pagination

**요구사항:** `LIST-003`, `LIST-004`, `LIST-005`, `API-001`, `API-031`, `API-032`

### 단계

1. 첫 post page를 요청합니다.
2. ID/timestamp와 pagination 데이터를 기록합니다.
3. 다음 page를 요청합니다.
4. 끝까지 계속합니다.

### 기대 결과

- 각 page는 최신순입니다.
- page는 전체 collection이 아니라 범위가 정해진 부분집합을 담습니다.
- 인접 page 사이에 중복 post가 없습니다.
- `total`은 현재 page 크기가 아니라 읽을 수 있는 전체 수입니다.

### 실행 근거 — 2026-09-29

- limit 7 순회가 읽을 수 있는 post 39개를 6 page로 다뤘고, PostgreSQL 순서·ID와 정확히 같았으며 `total: 39`가 안정적이고, page 범위가 정해져 있었으며, 중복·누락 없이 마지막 `nextCursor: null`이었습니다.
- 이는 P2A backend pagination 시나리오를 통과합니다. 당시 frontend infinite scroll(`LIST-004`, `LIST-005`의 UI 부분)은 `TODO`였습니다.

### P4 Frontend 근거 — 2026-09-29

- Browser 목록이 `IntersectionObserver`가 일으킨 `fetchNextPage`로 post 39개를 10/10/10/9로 로드했고, 중복·누락 없이 API와 같은 순서였으며 마지막 page 이후 요청이 없었습니다. "P4 Post List Evidence" 참고.

### P8-B 당시 후보 근거 — 2026-09-30

- 위의 "당시 `TODO`" 메모는 historical입니다. P4가 frontend 부분을 닫았습니다.
- 당시 API 순회 limit 7/10/50이 total 39와 null 마지막 cursor로 PostgreSQL 순서와 정확히 같았습니다. 잘못된 limit과 cursor는 400을 반환했습니다. browser는 no-cache로 다시 build한 image를 포함해, 연결된 요청 4개로 10/10/10/9를 다시 확인했고 끝 이후 요청은 없었습니다.

### 이후 근거 — 2026-10-02/03

- page size는 이후 15가 되었고 첫 page는 server render됩니다. `7da54e8`에서 읽을 수 있는 post 39개는 SSR page와 cursor 요청 두 개(`15/15/9`)로 로드되며, 첫 page에 대한 browser 요청이 없고, 중복이 없으며, 끝 이후 요청도 없습니다(PR #45 끝까지 순회; clean clone은 "F-16"). 위의 최신순 계약은 그대로입니다.

**결과:** `PASS`

---

## V-021 — 삭제된 게시글 제외

**요구사항:** `SEED-003`, `DEL-003`, `DEL-004`, `API-035`

### 단계

1. `deletedPostIds`에 있는 seed ID를 찾습니다.
2. post 목록을 요청합니다.
3. 그 ID의 상세를 요청합니다.

### 기대 결과

- 삭제된 post가 목록에 없습니다.
- 일반 상세 API가 삭제된 post를 읽을 수 있는 내용으로 노출하지 않습니다.

### 실행 근거 — 2026-09-29

- 전체 post 순회에서 seed 삭제 ID `9`, `30`, `31`이 모두 제외되었습니다.
- 그 ID 각각에 대해 상세와 댓글 endpoint가 HTTP 404를 반환했습니다.

### P8-B 당시 후보 근거 — 2026-09-30

- seed 삭제 9, 30, 31은 모든 순회에서 없었습니다. post 9 상세와 댓글은 404 `POST_NOT_FOUND`를 반환했습니다. P8-B 중 삭제한 post는 목록과 total에서 사라졌고, browser는 직접 방문 시 not-found를 보였습니다.

**결과:** `PASS`

---

# 게시글 작성 / 수정

## V-030 — 게시글 Validation

**요구사항:** `EDIT-001`, `EDIT-002`, `EDIT-003`, `EDIT-004`

### 시나리오

- 빈 제목
- 빈 내용
- 20자보다 긴 제목
- 유효한 제목/내용

### 기대 결과

- 유효하지 않은 입력은 저장할 수 없습니다.
- 필수 입력이 유효하지 않으면 frontend 완료 action이 비활성입니다.
- 직접 호출해도 backend가 유효하지 않은 payload를 거부합니다.
- 유효한 payload는 성공합니다.

### P2B Backend 근거 — 2026-09-29

- 직접 POST 요청이 없거나 빈 제목, 21 code point 제목, 없거나 빈 내용, 잘못된 JSON에 HTTP 400 JSON error를 반환했습니다.
- emoji code point 정확히 20개인 제목은 HTTP 201을 반환해, UTF-16 unit이 아니라 code point 기준 validation임을 확인했습니다.
- 자동 test가 null/blank field, 20/21 code point 경계, emoji surrogate pair를 다룹니다.
- 당시 frontend 비활성 부분이 구현되지 않았으므로, 이 UI/backend 결합 시나리오는 그 시점에 PASS로 표시하지 않았습니다.

### P6 UI 근거 — 2026-09-29

- UI: 유효하지 않은 제목/내용(빈 값, 공백만, emoji 포함 20 code point 초과)은 `작성 완료` / `수정 완료`를 비활성화하고 요청을 보내지 않습니다. ASCII 20개와 emoji 20 code point는 받아들여집니다. "P6 Post CRUD Evidence" 참고.

### P8-B 당시 후보 근거 — 2026-09-30

- P2B의 "PASS로 표시하지 않음" 메모는 historical입니다. P6가 UI 부분을 닫았습니다.
- 당시 후보에서 backend는 유효하지 않은 payload 10개를 400 JSON으로 거부하고 row를 만들지 않았습니다: 빈, 없는, 공백만, U+3000 제목; 빈, 없는, 공백만 내용; ASCII 21개, emoji 21개, 한국어 21 code point. ASCII 20개, emoji 20개, 한국어 20 code point는 받아들였습니다(201). PATCH는 emoji 21개 제목을 거부했습니다.
- UI는 같은 잘못된 경우에 `작성 완료`를 비활성화했고, 강제 `requestSubmit()`도 요청 0개를 보냈으며, 20 code point 제목 세 개는 활성화했습니다.

**결과:** `PASS`

---

## V-031 — 게시글 작성 흐름

**요구사항:** `EDIT-005`, `EDIT-006`, `API-002`

### 단계

1. UI에서 유효한 post를 만듭니다.
2. 이동을 관찰합니다.
3. Toast를 관찰합니다.
4. post가 최신순 목록에 나오는지 확인합니다.

### 기대 결과

- post가 현재 요청자로 만들어집니다.
- UI가 목록으로 돌아갑니다.
- 완료 toast가 보입니다.
- 새 post가 올바른 순서로 보입니다.

### P2B Backend 근거 — 2026-09-29

- `POST /api/posts`가 생성 ID `43`, 요청자 author `p2b_user_a`, `likeCount: 0`, `commentCount: 0`, `isLiked: false`와 함께 HTTP 201을 반환했습니다.
- PostgreSQL은 `base_like_count = 0`, `is_deleted = false`로 물리 row를 유지했고, 다음 목록 read에서 맨 앞에 놓였습니다.
- UI 이동과 toast 동작은 frontend phase로 남았으므로, 전체 시나리오는 당시 열려 있었습니다.

### P6 UI 근거 — 2026-09-29

- UI: FAB가 작성 화면을 열고; 성공하면 새 post가 맨 앞인 목록과 1회 toast로 돌아가며; 실패는 입력을 유지하고 아무것도 만들지 않고; 같은 tick의 double submit은 POST 하나를 보냅니다. "P6 Post CRUD Evidence" 참고.

### P8-B 당시 후보 근거 — 2026-09-30

- P2B의 "당시 열려 있었음" 메모는 historical입니다.
- 당시 browser 실행: FAB → `/posts/new` → 20 code point 제목으로 POST 1번(201) → 새 post가 맨 앞, 목록 total +1, 완료 toast가 한 번 나타났다가 사라진 `/`. 서버 post의 author는 `apr_tester`, count는 0이었습니다. 연속 375px 경로에서도 반복했습니다.

**결과:** `PASS`

---

## V-032 — 본인 게시글 수정

**요구사항:** `DETAIL-012`부터 `DETAIL-017`, `API-004`

### 단계

1. 현재 요청자가 소유한 post를 엽니다.
2. action 메뉴를 엽니다.
3. 수정을 고릅니다.
4. 제목/내용이 채워져 있는지 확인합니다.
5. 유효한 값으로 고치고 저장합니다.
6. 상세를 다시 가져옵니다.

### 기대 결과

- 소유자에게 수정 action이 있습니다.
- 기존 값이 채워져 있습니다.
- 작성과 같은 validation 규칙이 적용됩니다.
- 수정한 값이 저장됩니다.

### P2B Backend 근거 — 2026-09-29

- 소유자 PATCH가 수정된 post object와 함께 HTTP 200을 반환했고, 이어진 상세 read와 PostgreSQL query로 저장을 확인했습니다.
- `author`와 `createdAt`은 바뀌지 않았고, PATCH는 POST와 같은 validator를 씁니다.
- action 메뉴 표시, 수정 form 재사용, 채우기는 frontend 작업으로 남았으므로, 전체 시나리오는 당시 열려 있었습니다.

### P6 UI 근거 — 2026-09-29

- UI: 본인 글 trigger → sheet(P6 당시 bottom sheet) → 정확히 채워지고 같은 validation을 쓰는 수정 form; PATCH 성공이 상세와 cache된 목록을 갱신하고 reload가 같음; 실패는 값을 유지. "P6 Post CRUD Evidence" 참고.

### P8-B 당시 후보 근거 — 2026-09-30

- P2B의 "당시 열려 있었음" 메모는 historical입니다.
- 당시 browser 실행: 본인 글 메뉴 → sheet → `수정하기` → 정확한 제목/내용 채움 → PATCH 1번(200) → 상세가 갱신되었고, 목록 cache가 새 제목을 보였으며, reload와 API가 `createdAt`이 바뀌지 않은 채 같았습니다. API PATCH 응답은 완전한 단일 post object였습니다.

**결과:** `PASS`

---

## V-033 — 다른 사용자 게시글 수정 거부

**요구사항:** `DETAIL-019`, `API-034`

### 단계

1. 요청자 A를 씁니다.
2. 요청자 B가 쓴 post를 API로 직접 PATCH합니다.

### 기대 결과

- Mutation이 거부됩니다.
- Post는 그대로입니다.

### 실행 근거 — 2026-09-29

- 요청자 `p2b_user_b`가 요청자 `p2b_user_a`의 읽을 수 있는 post를 PATCH해 JSON error 계약과 함께 HTTP 403을 받았습니다.
- 직접 PostgreSQL query로 제목이 그대로임을 확인했고, 이후 소유자가 같은 row를 성공적으로 PATCH했습니다.

### P6 UI 근거 — 2026-09-29

- 다른 사용자 post에는 post 메뉴가 render되지 않고, `/posts/{other}/edit`은 form을 보이지 않으며, 직접 PATCH/DELETE는 여전히 403을 반환합니다(`apr_tester` → Roidl_08과 → mintcream으로 확인).

### P8-B 당시 후보 근거 — 2026-09-30

- 읽을 수 있는 post에 대한 비소유자 PATCH는 403을 반환했고 제목은 그대로였습니다. P8-B post, UI로 만든 post(`mintcream`), seed post 2에서 확인했습니다.
- `apr_tester`는 post 2에서 메뉴를 보지 못했고, `/posts/2/edit`은 form 없이 권한 없음 안내를 보였습니다.

**결과:** `PASS`

---

# Soft Delete

## Deletion Consistency Evidence — 2026-09-29

*시점:* 아래 cache 근거는 `4e53dbd` 당시의 동작입니다. 당시 cached 목록은 page size만 다른 같은 collection이었습니다. 이후 검색·정렬이 추가되면서 현재 `removePost`는 로드된 row를 모든 목록에서 제거하고, `total`은 filter 없는 목록과 row가 로드된 목록에서만 줄이며, 검색 목록은 invalidate합니다("F-08", `docs/ARCHITECTURE.md` "Deletion Consistency Model").

- 수정 commit `4e53dbd6fb951015f595ae980da1fe845929b60b`(`frontend/lib/query/postListCache.ts`만; backend, schema, API 변경 없음).
- **근본 원인:** 삭제된 post가 로드된 목록 page에 없으면 `removePost()`가 일찍 반환해, cache된 `total`이 1 크게 남았습니다(예: scroll해서 닿지 않은 post를 삭제한 경우).
- **방법:** 다시 seed한 뒤 Compose production stack(Chromium 375×778, API, `psql`)에 대해 임시 script를 실행해 **28/28**로 통과했습니다. page의 QueryClient는 React fiber introspection(test 전용)으로 읽었고, 목록 첫 page refetch를 막아 cache 갱신만 관찰했습니다.
- **Browser cache 시나리오:**
  - **B. 로드되지 않은 목록 삭제:** 첫 page 로드(10 rows, total 39). 소유자가 로드되지 않은 post 18을 client 측 `router.push`로 열어 삭제했습니다. refetch 전에 cache된 total은 모든 page에서 39→38로 서버와 같았고, 로드된 row는 그대로였습니다. post 18의 상세와 댓글 query는 제거되었습니다(`["posts","detail",18]` 아래 남은 것 없음). 끝까지 infinite scroll한 결과가 중복·누락 없이 서버의 38 ID와 같았고, 뒤로 가기는 삭제된 상세로 돌아가지 않았습니다.
  - **A. 로드된 목록 삭제:** post 5(row 5). total 38→37로 서버와 같음. cache에서 그 row만 제거. query 제거, 전체 scroll이 서버와 같음, 뒤로 가기가 돌아가지 않음.
  - **C. Cursor row 삭제:** 실행 시점의 row 10은 post 12였고, 첫 page의 `nextCursor`는 `…|12`로 decode되었습니다. 작성자(`cos_holic`)가 삭제했습니다. total 37→36으로 서버와 같고, 처음 9 row는 유지되었습니다. 다음 page는 삭제된 row의 cursor `…|12`로 요청되었고, 전체 목록이 중복·누락 없이 서버(36)와 같았습니다.
- **Parent 삭제 matrix**(`apr_tester`의 새 post, `apr_tester`와 `mintcream`의 댓글, 두 사람의 좋아요):
  - 삭제 전: `commentCount` 2, `likeCount` 2로 읽을 수 있음. DB: post `f`, comments `false,false`, like row 2개.
  - 읽을 수 있는 동안 `mintcream` DELETE와 PATCH는 403을 반환했고 post는 읽을 수 있게 남았습니다. 소유자 DELETE는 204를 반환했습니다.
  - 삭제 후 DB: post `t`, 두 댓글은 여전히 `false`, like row 2개 유지.
  - 이후 모든 route가 소유자와 비소유자 모두에게 404를 반환했습니다: GET post, PATCH, 다시 DELETE, 좋아요 toggle, GET comments, POST comment, 기존 댓글 각각의 DELETE(작성자 본인 포함).
  - 그 404 호출은 DB를 바꾸지 않았습니다. 존재한 적 없는 post는 GET, PATCH, DELETE, toggle에 404를 반환했습니다.
- **단독 댓글(post 13 살아 있음):**
  - 비소유자 DELETE는 403; 댓글은 목록에 남고 `is_deleted = f`.
  - 소유자 DELETE는 204, 소유자나 비소유자의 반복은 404.
  - row는 `is_deleted = t`로 유지되어 목록에서 제외되었고, `total`과 `commentCount`는 기본값으로 돌아왔습니다.
- **동시성 smoke**(15 round, 각각 새 post에서 post DELETE와 댓글 작성, 좋아요 toggle, 수정, 기존 댓글 삭제를 함께 발생):
  - post DELETE는 항상 204였고, 이후 post는 읽을 수 없었습니다.
  - 하위 작업은 삭제 전 성공 또는 404만 반환했습니다(댓글 작성 `201`/`404`, 좋아요 `200`/`404`, 수정 `200`/`404`, 댓글 삭제 `204`/`404`). 5xx나 deadlock은 없었습니다.
- **Backend:** matrix가 정책과 같았으므로 바꾸지 않았습니다. 기존 mock test가 service 수준의 404 경로를 이미 다룹니다(`rejectsCreationForDeletedOrMissingParent`, `treatsDeletedOrMissingCommentAsNotFound`, `treatsDeletedOrMissingPostAsNotFound`, `rejectsLikeToggleForDeletedOrMissingPost`). 댓글 삭제의 parent 가시성은 SQL(읽을 수 있는 parent join)에 있으므로 mock이 아니라 PostgreSQL matrix로 증명했습니다. `gradlew clean test`가 49/49로 통과했습니다.
- **각각 다시 seed한 뒤 회귀:** P6 74/74(실행 시점 cursor row 사용), P5B 52/52, P4 48/48, P5A 63/63, 재방문 12/12. `npm run typecheck`, clean `npm run build`, `git diff --check`가 깨끗했습니다.
- **DB baseline**은 다시 seed해 복원했습니다: posts 42(삭제 3), comments 2,076(삭제 81), 좋아요 0, `seed_history = assignment-v1`.

## V-040 — 본인 게시글 Soft Delete

**요구사항:** `DEL-001`, `DEL-003`, `DEL-004`, `DETAIL-018`, `API-005`

### 단계

1. 본인 post를 만들거나 찾습니다.
2. DB row 존재를 기록합니다.
3. API/UI로 삭제합니다.
4. DB row를 다시 query합니다.
5. Post 목록을 query합니다.
6. 상세를 query합니다.

### 기대 결과

- DB row가 삭제 marker/state와 함께 여전히 존재합니다.
- Resource가 목록에서 사라집니다.
- 일반 상세 API로 resource를 읽을 수 없습니다.
- 본인 post 삭제 후 UI가 목록으로 돌아갑니다.

### P2B Backend 근거 — 2026-09-29

- 소유자 DELETE가 HTTP 204를 반환했습니다. PostgreSQL에는 여전히 ID `43`의 row가 정확히 하나, `is_deleted = true`로 있었습니다.
- 목록은 post를 제외했고, 상세와 댓글 read는 404를 반환했습니다. PATCH, 좋아요 toggle, 반복 DELETE, 댓글 작성도 404를 반환했습니다.
- 당시 UI의 목록 복귀 동작이 구현되지 않았으므로, 이 API/UI 결합 시나리오는 그 시점에 PASS로 표시하지 않았습니다.

### P6 UI 근거 — 2026-09-29

- UI: sheet(P6 당시 bottom sheet) → 확인 → DELETE 1번 → post 없는 목록(cache와 서버), 직접 URL은 not found, DB row `is_deleted = true`; 취소와 실패는 post를 남깁니다. "P6 Post CRUD Evidence" 참고.

### P8-B 당시 후보 근거 — 2026-09-30

- P2B의 "PASS로 표시하지 않음" 메모는 historical입니다.
- 당시 browser 실행: sheet → dialog → DELETE 1번 → row 없는 `/`, total 39로 복귀, 직접 URL not found, row `is_deleted = true`, 반복 DELETE는 404.
- Parent 삭제 matrix: post row, 하위 댓글, like row가 유지되었습니다. 모든 product route가 소유자와 비소유자 모두에게 404를 반환했고, 그 호출은 database를 바꾸지 않았습니다.

**결과:** `PASS`

---

## V-041 — 다른 사용자 게시글 삭제 거부

**요구사항:** `DETAIL-020`, `API-034`

### 단계

1. 요청자 A를 씁니다.
2. 요청자 B가 쓴 post를 DELETE합니다.

### 기대 결과

- 삭제가 거부됩니다.
- Post는 일반 사용자에게 계속 읽을 수 있습니다.

### 실행 근거 — 2026-09-29

- 요청자 `p2b_user_b`가 요청자 `p2b_user_a`의 읽을 수 있는 post를 삭제하려다 HTTP 403을 받았습니다.
- PostgreSQL은 여전히 `is_deleted = false`를 보고했고, 소유자는 계속 읽을 수 있었으며 이후 post를 삭제했습니다.

### P6 UI 근거 — 2026-09-29

- 다른 사용자 post에는 post 메뉴가 render되지 않고, `/posts/{other}/edit`은 form을 보이지 않으며, 직접 PATCH/DELETE는 여전히 403을 반환합니다(`apr_tester` → Roidl_08과 → mintcream으로 확인).

### P8-B 당시 후보 근거 — 2026-09-30

- 비소유자 DELETE는 post가 읽을 수 있는 채로 403 `FORBIDDEN`을 반환했습니다. P8-B post, UI로 만든 post(`mintcream`), seed post 2에서 확인했습니다. post가 삭제된 뒤에는 비소유자 호출이 ownership 확인 전에 404를 반환했습니다.

**결과:** `PASS`

---

# 좋아요

## V-050 — 요청자별 좋아요 Toggle

**요구사항:** `SEED-010`, `DETAIL-001`, `DETAIL-002`, `DETAIL-003`, `API-006`, `API-027`, `API-028`, `API-030`

### 단계

1. 좋아요 toggle 전에 요청자 A로 seed post를 가져와 `isLiked`, `likeCount`를 기록합니다.
2. 좋아요를 한 번 toggle합니다.
3. 요청자 A로 다시 가져옵니다.
4. 요청자 B로 같은 post를 가져옵니다.
5. 요청자 A로 다시 toggle합니다.

### 기대 결과

- 첫 toggle 전에 요청자 A는 `isLiked: false`를 받습니다.
- 첫 toggle은 요청자 A의 `isLiked`를 뒤집고 `likeCount`를 1 바꿉니다.
- 요청자 B의 `isLiked`는 독립적으로 판단됩니다.
- 요청자 A의 두 번째 toggle은 상태/count 변화를 되돌립니다.

### 실행 근거 — 2026-09-29

- application like row가 0인 상태에서, 요청자 A는 검증 post에서 `isLiked: false`와 count `0`을 읽었습니다.
- 요청자 A가 on, 요청자 B는 독립적으로 false를 읽고 on, 이어서 A와 B가 각각 off했습니다. 응답과 상세 read는 독립적인 요청자 상태와 함께 count `0 → 1 → 2 → 1 → 0`을 보였습니다.
- PostgreSQL은 검증 post에 대해 `post_likes` row 0으로 끝났습니다. 응답에는 `isLiked`와 파생 `likeCount`가 모두 있었습니다.
- 이는 backend/API 시나리오를 통과합니다. 당시 시각적 icon 동작은 frontend 요구사항으로 남았고, 이 근거로 올리지 않았습니다.

### P5A UI 근거 — 2026-09-29

- 상세 좋아요 button이 toggle되고, 표시 count와 하트(좋아요 시 `#FF8DA1` 채움)를 갱신하며, reload 후 backend와 일관되고, 중복 요청을 막으며, `/dev-user` 전환 후 요청자별 상태를 보입니다. "P5A Post Detail / Like / Comment Read Evidence" 참고.

### P8-B 당시 후보 근거 — 2026-09-30

- 위의 "올리지 않음" 메모는 historical입니다. P5A가 시각 부분을 닫았습니다.
- post 13의 API: A와 B가 독립적인 `isLiked`로 `856 → 857 → 858 → 857 → 856`, 목록이 요청자 상태를 반영.
- Browser: 상세와 목록의 하트/count가 동기화되었고, `mintcream`은 전환 후 독립 상태를 봤습니다.
- 최종 상태: 856, 읽을 수 있는 post의 like row 0.

**결과:** `PASS`

---

# 댓글

## V-060 — 처음 댓글 2개

**요구사항:** `DETAIL-004`, `DETAIL-005`, `DETAIL-006`, `DETAIL-007`

### 단계

다음 post로 test합니다.

- 댓글 0개
- 댓글 1개
- 댓글 2개
- 댓글 3개 이상

### 기대 결과

- 댓글은 최신순입니다.
- 처음에는 최대 2개만 보입니다.
- "더보기"는 total >= 3일 때만 나타납니다.

### 실행 근거 — 2026-09-29

- 375×778 Chromium에서 posts 10 (0), 24 (1), 1 (2), 3 (3), 4 (27): 더보기 전 최대 2개, API와 같은 최신순, 3과 27에서만 더보기, post마다 정확히 첫 `limit=2` 댓글 요청 1개. 댓글 0개 state는 `댓글 (0)`만 보입니다.
- review 수정 후 같은 session에서 post 4를 재방문해도 최대 2개만 보였고 새 `limit=2` 요청 1개를 보냈습니다("P5A Review Fix — Same-Session Revisit" 참고).

### P8-B 당시 후보 근거 — 2026-09-30

- browser가 posts 10/24/1/3/4 (0/1/2/3/27)를 다시 확인했습니다: 각각 `limit=2` 요청 1개, API 최신순으로 최대 2개 표시, 3과 27에서만 더보기. post 2 (1,411)도 더보기와 함께 2개를 보였습니다.
- 같은 session에서 post 4를 재방문하자 다시 새 `limit=2` 요청 1개와 continuation 0개였습니다.

**결과:** `PASS`

---

## V-061 — 더보기 후 Infinite Scroll

**요구사항:** `DETAIL-008`, `API-031`, `API-032`

### 단계

1. 여러 page에 걸칠 만큼 댓글이 있는 post를 엽니다.
2. 처음 2개만 보이는지 확인합니다.
3. "더보기"를 누릅니다.
4. Scroll해 이후 page를 로드합니다.

### 기대 결과

- 추가 댓글 pagination은 "더보기" 이후에만 시작합니다.
- Page는 범위가 정해져 있습니다.
- 중복 댓글이 없습니다.
- 댓글은 최신순을 유지합니다.

### 실행 근거 — 2026-09-29

- 더보기 전에는 sentinel을 화면에 scroll해도 첫 `limit=2` page 외의 댓글 요청이 없었습니다. 더보기 후에는(당시 continuation 20) `nextCursor`로 이어진 범위가 정해진 `limit=20` page가 sentinel로 `nextCursor: null`까지 로드되었고, 최신순, 중복 없음, 끝 이후 요청 없음이었습니다(posts 3, 4; 댓글 1,411개인 post 2는 점진적 로드).
- 이전에 continuation page를 로드한 뒤 같은 session에서 재방문해도 같은 gate가 유지됩니다: 더보기 전 continuation 요청 0, 더보기 시 cache된 댓글이 한꺼번에 노출되지 않음(수정 `ddc1a9d`).

### P8-B 당시 후보 근거 — 2026-09-30

- Browser: scroll 후에도 더보기 전 요청 없음. 더보기 후 `nextCursor`로 이어진 `limit=20` page가 댓글 3개와 27개를 API 순서로 모두 render했고, 더보기가 사라졌으며, null cursor 이후에는 아무것도 없었습니다. post 2는 범위가 정해진 `limit=20` page로 로드되었습니다.
- API: post 2 (1,411)를 `2`→`50`(30 pages)과 `2`→`20`(72 pages)으로 범위가 정해진 전체 순회를 했고, 중복·누락 없이 PostgreSQL과 정확히 같았습니다.

### 이후 근거 — 2026-10-02/03

- continuation 크기는 현재 30입니다(`limit=2` 후 `limit=30`). `7da54e8`에서: post 8이 `2 → 30 → 30`으로 backend 순서대로 58/58까지 순회했고 끝 이후 요청은 없었습니다(PR #45); post #2는 continuation 요청 47개로 댓글 1,411개를 중복 없이 모두 로드했고, gate는 여전히 더보기 전에 아무것도 보내지 않았습니다(`docs/PERFORMANCE.md` §15, "F-05"). 처음 2개라는 요구사항은 그대로입니다.

**결과:** `PASS`

---

## V-062 — 댓글 작성

**요구사항:** `DETAIL-009`, `API-008`

### 단계

1. 현재 요청자로 댓글을 작성합니다.
2. 상세를 새로고침/다시 가져옵니다.

### 기대 결과

- 댓글이 현재 요청자를 작성자로 저장됩니다.
- 댓글이 최신순에 맞게 나타납니다.
- `commentCount`가 1 늘어납니다.

### 실행 근거 — 2026-09-29

- 요청자 `p2b_user_b`가 comment ID `2077`을 만들고, 요청자를 작성자로 하는 HTTP 201을 받았습니다.
- 다음 댓글 read가 그것을 맨 앞에 반환했고, PostgreSQL은 읽을 수 있게 유지했으며, parent post의 파생 `commentCount`는 `0`에서 `1`로 바뀌었습니다.
- 빈 댓글 내용은 HTTP 400 `INVALID_CONTENT`를 반환했습니다.

### P5B UI 근거 — 2026-09-29

- 고정 composer로 browser에서 0→1, 1→2, 2→3(더보기 나타남), 27→28, 여러 page를 펼친 상태에서 작성했습니다. 서버 확인 후에만 반영: 실패는 입력과 count를 유지하고, 같은 tick의 double submit은 POST 하나를 보냅니다. "P5B Comment Mutation Evidence" 참고.

### P8-B 당시 후보 근거 — 2026-09-30

- Browser: post 1에서 `apr_tester`가 POST 1번을 했고, 댓글이 요청자를 작성자로 맨 앞에 나타났습니다. heading은 더보기와 함께 2→3, 목록 count는 +1이었습니다.
- `mintcream`이 작성자가 `mintcream`인 댓글을 만들었습니다.
- API: 정확한 댓글 형태로 201, 최신순, 파생 `commentCount` +1.

**결과:** `PASS`

---

## V-063 — 본인 댓글 Soft Delete

**요구사항:** `DEL-002`, `DEL-005`, `DEL-006`, `DETAIL-010`, `DETAIL-011`, `API-009`

### 단계

1. 본인 댓글을 만들거나 찾습니다.
2. 소유 post의 `commentCount`를 기록합니다.
3. 댓글을 삭제합니다.
4. DB row가 삭제 marker/state와 함께 남아 있는지 확인합니다.
5. 댓글과 post 상세를 다시 가져옵니다.

### 기대 결과

- DB row가 남습니다.
- 댓글이 댓글 목록에서 사라집니다.
- Post `commentCount`가 1 줄어듭니다.

### 실행 근거 — 2026-09-29

- 댓글 작성자가 comment ID `2077`을 삭제하고 HTTP 204를 받았습니다.
- PostgreSQL은 row를 `is_deleted = true`로 유지했고, 댓글 `total`과 parent post의 파생 `commentCount`가 모두 `1`에서 `0`으로 바뀌었습니다.
- 반복 삭제는 HTTP 404를 반환했습니다.

### P5B UI 근거 — 2026-09-29

- 확인 dialog로 browser에서 3→2(더보기 사라짐), 2→1, 1→0, continuation page 안, cursor 경계 row를 삭제했습니다. 각 댓글이 사라졌고, 댓글 total, 상세 count, cache된 목록 count가 1 줄었으며 reload 후 서버와 같았습니다. "P5B Comment Mutation Evidence" 참고.

### P8-B 당시 후보 근거 — 2026-09-30

- Browser: 취소는 DELETE를 보내지 않았고, 확인은 정확히 1번 보냈습니다. 댓글이 사라졌고 heading과 목록 count가 3→2로 돌아왔습니다. row는 `is_deleted = true`로 남았고, 반복 DELETE는 404를 반환했습니다.
- `mintcream`이 UI로 본인 댓글을 삭제했고 count가 돌아왔습니다.

**결과:** `PASS`

---

## V-064 — 다른 사용자 댓글 삭제 거부

**요구사항:** `DETAIL-010`, `API-034`

### 단계

1. 요청자 A를 씁니다.
2. 요청자 B가 쓴 댓글을 DELETE합니다.

### 기대 결과

- 삭제가 거부됩니다.
- 댓글은 계속 읽을 수 있습니다.

### 실행 근거 — 2026-09-29

- post 소유자 `p2b_user_a`가 `p2b_user_b`의 댓글을 삭제하려다 HTTP 403을 받아, post ownership이 댓글 ownership을 뜻하지 않음을 증명했습니다.
- PostgreSQL은 여전히 댓글을 읽을 수 있다고 보고했고, 이어서 요청자 B가 본인 댓글을 성공적으로 삭제했습니다.

### P5B UI 근거 — 2026-09-29

- UI는 요청자 본인 댓글에만 `삭제`를 render합니다(`apr_tester`와 `mintcream`으로, 다른 사용자 post 위의 본인 댓글 포함해 확인). backend는 다른 사용자의 DELETE에 계속 403을 반환합니다.

### P8-B 당시 후보 근거 — 2026-09-30

- `apr_tester`는 mintcream의 댓글을 `삭제` 없이 봤습니다. 직접 `apr_tester` DELETE는 row 변경 없이 403을 반환했습니다.
- post 소유자의 다른 요청자 댓글 DELETE와 비소유자의 seed 댓글 DELETE 모두 403을 반환했습니다.

**결과:** `PASS`

---

## V-065 — Seed에서 삭제된 댓글은 세지 않음

**요구사항:** `SEED-004`, `SEED-008`, `SEED-009`, `DEL-006`

### 단계

1. `deletedCommentIds`에 있는 댓글이 하나 이상 있는 post를 고릅니다.
2. seed 데이터에서 기대하는 읽을 수 있는 댓글 수를 계산합니다.
3. Post 상세/목록 항목을 가져옵니다.
4. 댓글 목록을 가져옵니다.

### 기대 결과

- 삭제된 seed 댓글이 댓글 API에 없습니다.
- `commentCount`는 읽을 수 있는 댓글만 셉니다.

### 실행 근거 — 2026-09-29

- 읽을 수 있는 post 39개 모두의 API `commentCount`가 PostgreSQL `comments WHERE is_deleted = FALSE` 수와 같았습니다.
- 읽을 수 있는 모든 post의 전체 댓글 순회가 PostgreSQL의 읽을 수 있는 ID/순서와 정확히 같았고, seed에서 삭제된 comment ID 81개는 하나도 나오지 않았습니다.

### P8-B 당시 후보 근거 — 2026-09-30

- 대표 순회(0/1/2/3/27/1,411)가 PostgreSQL의 읽을 수 있는 row와 같았고, `total`과 상세 `commentCount`가 일치했습니다.
- 삭제된 post 아래의 댓글은 접근할 수 없었고(404) 어떤 count에도 없었습니다. seed에서 삭제된 comment ID 81개는 그대로입니다.

**결과:** `PASS`

---

# API 계약

## V-070 — 게시글 응답 형태

**요구사항:** `API-010`부터 `API-023`

### 확인

- 목록 envelope에 `items`, `total`이 있음
- 각 post에 필수 field가 있음
- 상세는 `items` envelope가 아닌 단일 object
- 상세에 `isLiked`, `commentCount`가 있음
- patch가 수정된 post object를 반환

### 실행 근거 — 2026-09-29

- 목록 응답은 `items`, 읽을 수 있는 전체 `total`, 구현이 추가한 `nextCursor`를 가진 JSON envelope였습니다.
- 순회한 모든 post item은 정확히 `id`, `title`, `content`, `likeCount`, `commentCount`, `isLiked`, `author`, ISO-8601 `createdAt`을 가졌습니다.
- 상세는 같은 field를 `items` envelope 없는 단일 object로 반환했습니다.
- P2B mutation 요청과 응답은 JSON을 썼습니다. 잘못된 JSON은 `{ "code": "INVALID_REQUEST", "message": "Request body is invalid." }`와 함께 HTTP 400을 반환했습니다.
- PATCH는 수정된 내용, 요청자별 `isLiked`, 파생 `commentCount`를 가진 같은 완전한 단일 post object 형태를 반환했습니다.

### P8-B 당시 후보 근거 — 2026-09-30

- 당시 목록 page는 정확히 `{ items, total, nextCursor }`였고, item은 정확히 필수 field 여덟 개와 ISO-8601 `createdAt`이었습니다. 상세는 envelope 없이 목록 item과 byte 단위로 같았습니다. PATCH는 완전한 수정 object를, 좋아요 toggle은 정확히 `{ isLiked, likeCount }`를 반환했습니다.
- 오류는 HTML 없는 JSON `{ code, message }`였습니다.

**결과:** `PASS`

---

## V-071 — 댓글 응답 형태

**요구사항:** `API-024`, `API-025`, `API-026`

### 확인

- 댓글 목록에 `items`, `total`이 있음
- 댓글 item에 `id`, `postId`, `content`, `author`, `createdAt`이 있음

### 실행 근거 — 2026-09-29

- 모든 댓글 page가 `items`, 읽을 수 있는 전체 `total`, `nextCursor` envelope를 썼습니다.
- 순회한 모든 댓글 item은 정확히 `id`, `postId`, `content`, `author`, ISO-8601 `createdAt`을 가졌습니다.

### P8-B 당시 후보 근거 — 2026-09-30

- 대표 순회와 1,411개 댓글 순회의 모든 댓글 page가 `items`/`total`/`nextCursor` envelope를 썼습니다. 모든 item은 일치하는 `postId`와 함께 정확히 필수 field 다섯 개를 가졌습니다.

**결과:** `PASS`

---

# UI / 시각 검증

## V-080 — 375px Mobile Layout

**요구사항:** `UI-001`, `UI-002`, `UI-003`

이 항목은 375px에서 필수 화면의 runtime layout을 확인합니다. 공식 디자인 비교는 V-081이 별도로 다룹니다.

### 단계

375px viewport에서 필수 화면을 확인합니다.

- 게시글 목록
- 게시글 상세
- 게시글 작성
- 게시글 수정
- 게시글 action 메뉴/bottom sheet

### 기대 결과

- 필수 기능이 375px에 맞고 사용할 수 있습니다.
- Desktop 전용 layout은 필요 없습니다.

### P4 부분 근거 — 2026-09-29

- 게시글 목록만, `375 × 778`에서: document `scrollWidth` 375(가로 scroll 없음), 48px header, y=56의 목록, x=16의 343×93 content 영역을 가진 118px row, 마지막 row를 가리지 않는 x=310, y=679의 50×50 FAB(하단 padding 120px).
- 당시 상세, 작성, 수정, action sheet 화면이 없었으므로 이 시나리오는 그 시점에 완료되지 않았습니다.

### P5A 부분 근거 — 2026-09-29

- `375 × 778`의 게시글 상세: 375×48 sticky navigation, y=56부터 content, document `scrollWidth` 375. 당시 작성, 수정, action sheet 화면은 없었습니다.

### P5B 부분 근거 — 2026-09-29

- y=692의 고정 375×86 composer와 (24, 257)의 327×264 삭제 dialog가 있는 상세: 끊김 없는 긴 댓글을 포함해 가로 overflow 없음. 당시 작성, 수정, action sheet 화면은 여전히 없었습니다.

### P6 근거 — 2026-09-29

- 이제 모든 필수 화면이 있었고 `375 × 778`에서 확인했습니다: 목록, 상세, 본인 글 action sheet(P6 당시 bottom sheet), 게시글 삭제 dialog, 작성(긴 내용을 고정 action까지 scroll), 수정. 각각 document `scrollWidth` 375(가로 scroll 없음)였고, 고정 control은 사용할 수 있으며 content를 가리지 않았습니다. 앞선 부분 근거는 목록(P4), 상세(P5A), composer와 댓글 dialog(P5B)를 다뤘습니다.

### P8-B 회귀 근거 — 2026-09-30

- 앞의 "완료되지 않음" 부분 메모는 historical입니다.
- 실제 375px viewport(`innerWidth` 375)의 연속 Chromium 경로가 `/dev-user` → 목록 → infinite scroll → 상세 → 좋아요 → 댓글 작성/삭제 → 작성 → toast → 수정 → 삭제 → 목록을 다뤘습니다. 댓글 dialog, action sheet, post dialog를 포함한 모든 화면의 `scrollWidth`가 375였습니다.
- FAB, 댓글 composer, form action bar가 viewport 안에 고정되어 있었습니다. page error, console error, 4xx/5xx 응답이 없었습니다.

**결과:** `PASS`

---

## V-081 — 공식 디자인 Fidelity

**요구사항:** `UI-004`

이 항목은 공식 Figma 디자인과의 비교입니다. 375px runtime layout은 V-080이 다룹니다.

### 전제 조건

공식 디자인 source는 Figma file `Ee1gp5NC6G1sQIvssuu4Sr`, Page 1 (`0:1`)에 있습니다. 디자인 audit와 frame mapping은 `docs/UI_SPEC.md`에 기록되어 있습니다.

### 확인

- typography
- spacing
- colors
- radii
- icons
- disabled state
- 떠 있는 작성 button
- 게시글 action 메뉴/bottom sheet
- toast

### 기대 결과

구현이 실용적인 범위에서 공식 디자인에 가깝고, 의도적 차이는 모두 문서화되어 있습니다.

### P4 부분 근거 — 2026-09-29

- 한계: 공식 Figma node는 이전에 docs/UI_SPEC.md로 audit되어 있었지만, Starter-plan tool-call 한도가 소진되어 새 MCP 비교를 할 수 없었습니다.
- node `755:8831`의 `docs/UI_SPEC.md` `DESIGN_DEFINED` 값과 Chromium에서 측정했습니다: frame 영역(header 48, 목록 y=56, row 118, x=16/y=+12의 content 343×93, divider 343×1), 18×18 원형 avatar, x=310/y=679의 FAB 50×50, app title Bold 20/28 `#292828`, post 제목 Medium 16/24 `#292828`, preview Regular 14/18 `#979EA9`, 작성자 `#5E5E5E`와 count/시간 `#979EA9`인 metadata Regular 12/16, Pretendard 로드.
- 이는 Figma frame이 아니라 기록된 spec 값과의 일치를 증명합니다. 제목/preview clamp, metadata 간격, icon path/크기, FAB 빨강/shadow, 내부 간격은 `docs/UI_SPEC.md`에 기록된 `PRODUCT_DECISION` 값이며 Figma와 비교하지 않았습니다. divider는 Figma 정확 mapping이 아니라 구현 선택으로 기존 `--color-border-light` token을 씁니다.
- 목록 화면만 있었습니다. 그 단계의 결과는 `NOT_RUN`이었습니다.

### P5A 부분 근거 — 2026-09-29

- 상세 typography가 `docs/UI_SPEC.md` `DESIGN_DEFINED` 값과 같았습니다(제목 Bold 18/26, 작성자 Bold 15/22, 본문 Regular 14/18, heading Bold 16/24, `#292828`, 좋아요 `#FF8DA1`). layout offset, avatar 크기, 댓글 typography, icon, 더보기 button은 공식 frame의 local PNG export(`755-10420`, `755-10774`, `755-12645`, `755-13104`, `769-142`)에서 추정한 `PRODUCT_DECISION` 값이며 Figma node 측정값이 아닙니다. local PNG와의 비교는 V-081이 아니었고, 그 단계의 결과는 `NOT_RUN`이었습니다.

### P5B 부분 근거 — 2026-09-29

- Composer 영역, dialog geometry, dialog 문구는 `docs/UI_SPEC.md`를 따랐습니다. 내부 간격, 색, `등록` control은 PNG에서 추정한 `PRODUCT_DECISION` 값(local export `755-12175`, `755-13104`, `755-12645`)이며 Figma node 측정값이 아닙니다. 그 단계의 결과는 `NOT_RUN`이었습니다.

### P6 부분 근거 — 2026-09-29

- 작성/수정 고정 action(68 + 26 영역 안 y=690의 343×56), action sheet(y=554의 375×224), 게시글 삭제 dialog(24, 257의 327×264), toast(97.5, 657의 181×40)가 `docs/UI_SPEC.md`와 같았습니다. 다른 값은 PNG 추정 `PRODUCT_DECISION`입니다. 새 Figma 비교는 실행하지 않았으므로 그 단계의 결과는 `NOT_RUN`이었습니다.

### P7-A Local SVG와 Chromium 근거 — 2026-09-30

- `755-8831-post-list.svg`는 text와 geometry가 보존된 375×778 viewBox를 제공합니다: 118px row, 18px avatar/작성자 row, 16/24 제목, 14/18 preview, 12/16 metadata, x=16/y=173의 divider. 목록은 이제 link, query, cache, pagination 동작을 바꾸지 않고 SVG hierarchy avatar/작성자 → 제목 → preview → metadata를 따릅니다.
- SVG는 `#DADEE3`로 채운 중립 댓글/하트 glyph를 직접 제공하며, 하트에는 `#DADEE3` 1.4px stroke도 있습니다. Chromium DOM 근거로 목록이 그 vector를 쓰고 `post.isLiked`가 true일 때 하트만 `#FF8DA1`로 바꾼다는 것을 확인했습니다. count text는 중립으로 남습니다.
- `npm run typecheck`, `npm run build`, `git diff --check`, Compose frontend 재build, `/health`가 통과했습니다. Chromium은 고친 순서, 중립 row, 목록 첫 row의 `isLiked: true` 분홍 하트를 render했습니다.
- 이 환경의 Chrome/Edge headless capture는 최소 CSS viewport가 375px보다 넓어, 375px bitmap에서 오른쪽 정렬 시간과 고정 FAB가 잘렸습니다. DOM/CSS 근거로 두 요소가 기존 오른쪽 정렬/고정 규칙과 함께 존재함을 확인했습니다. 이는 새 Figma MCP 비교 근거가 아니며, 그 단계의 V-081은 `NOT_RUN`이었습니다.

### P7-B 화면 전반 Runtime 회귀 — 2026-09-30

- 연속 browser session 하나에서 Chrome DevTools device metrics가 `innerWidth=375`, `clientWidth=375`, `devicePixelRatio=1`, document `scrollWidth=375`를 제공했습니다. 이전 `--window-size=375,778` headless 실행은 대신 `innerWidth=500`을 보고했는데, 이는 product layout 결함이 아니라 도구의 최소 window 한계입니다. 이 때문에 product CSS를 바꾸지 않았습니다.
- 목록: row는 y=56/174/292에서 118px를 유지했고, 첫 row는 SVG 기반 작성자/제목/preview/metadata 순서, x=16의 18px avatar, x=359에서 끝나는 오른쪽 정렬 시간, 343px metadata 영역, 채운 중립 glyph, 좋아요 하트만 분홍, (310,679)의 50px FAB, sentinel을 유지했고 overflow가 없었습니다. 기존의 긴 제목/preview row는 row 흔들림 없이 한 줄로 잘렸습니다. seed 데이터에는 긴 작성자 fixture가 없어, test 데이터를 지어내지 않고 기존 truncation 규칙만 확인했습니다.
- 상세: 본인 글 좋아요 함/안 함 state, nav, byline, post 본문, 고정 composer(0,692,375x86), action sheet(0,554,375x224), post/댓글 dialog(24,257,327x264)가 overflow 없이 render되었습니다. Escape와 scrim 닫기가 동작했습니다. 다른 사용자 post에는 소유자 메뉴가 없었고, 댓글 1,411/1/0개 state는 처음 댓글 2/1/0개를 render했으며 더보기는 댓글이 많은 state에만 있었습니다.
- 작성/수정: 빈 작성은 하단 action을 비활성으로 유지했고; 유효한 작성은 (97,657)에 50px FAB와 겹치지 않는 1회성 181x40 toast를 보였다가 사라졌습니다. test post를 열어, 수정에서 채워진 값을 확인하고, 상세로 저장한 뒤 UI dialog로 삭제했으며, 목록에 없었습니다.
- 기능 smoke: post 43 좋아요가 true/1 -> false/0으로 바뀌고, 목록에서 중립으로 보인 뒤 true/1로 복원; 목록 sentinel이 10 -> 20 고유 row를 로드; UI로 댓글 작성/삭제가 0 -> 1 -> 0; `/dev-user`로 `Roidl_08`(post 2 메뉴 표시)로 바꾸고 다시 `apr_tester`(같은 메뉴 숨김)로. Compose service는 healthy였고 `/health`는 `{"status":"ok","db":"up"}`을 반환했습니다. 첫 댓글 자동화 시도는 없는 textarea를 대상으로 해 요청을 보내지 않았고, 입력을 고친 위 실행이 통과했습니다.
- 발견 분류: 확인한 모든 항목은 `NO_CHANGE`, 다만 원시 headless window 크기 동작은 `TOOLING_LIMITATION`. product code 변경을 정당화하는 명확한 `FUNCTIONAL_BUG`, `DESIGN_MISMATCH`, `P7_A_REGRESSION`은 없었습니다. 이는 local PNG/SVG/runtime 근거일 뿐이며, 그 단계의 V-081은 `NOT_RUN`이었습니다.

### P8-B 상태 — 2026-09-30

- P8-B는 동작 acceptance만이었고 공식 Figma file `Ee1gp5NC6G1sQIvssuu4Sr`과의 새 비교는 실행하지 않았습니다. 그 375px runtime 회귀는 디자인 fidelity 근거가 아닙니다.
- 그 단계에서 V-081은 남은 최종 sign-off 항목이었습니다.

### P8-C 메모 — 2026-09-30

- P8-C는 `design/design-retouch/`의 의도적 UX 보정을 구현했습니다(`PRODUCT_DECISION`, 공식 Figma 근거 아님). 그 목록과 상세 변경은 의도적으로 `design/design-export/`와 다르므로 fidelity 근거가 아닙니다.
- 이후 V-081 비교는 이를 의도적 차이로 기록해야 했습니다. 여기에는 공식 본인 글 bottom sheet(830:139)를 post `⋮`에 붙는 non-modal popover로 바꾼 것이 포함됩니다. 삭제 확인 dialog와 그 scrim은 그대로입니다.
- 보정한 build에 대한 집중 375px 회귀가 통과했습니다(목록과 infinite scroll, 좋아요 동기화, 더보기 gate, 댓글과 post CRUD, ownership 메뉴, `/dev-user` 전환, 연속 E2E; overflow와 오류 없음). 동작은 바뀌지 않았습니다.

### Historical Status — 2026-10-03 (`NOT_RUN`)

- P8-C 이후 375×778에서 runtime과 시각 회귀가 여러 번 통과했습니다(접근성 대비 PR #35, first-page SSR PR #36, prefetch PR #38, edge/failure audit PR #45, clean-clone gate "F-16"). 이들은 구현과 `docs/UI_SPEC.md`에 대한 runtime·layout 확인이지, 최종 구현을 공식 Figma file `Ee1gp5NC6G1sQIvssuu4Sr`과 새로 비교한 것이 아닙니다. 그런 비교는 실행하지 않았으므로 그 날짜의 V-081은 `NOT_RUN`이었습니다.
- 이후 V-081 비교는 다음 의도적 차이도 기록해야 했습니다: PR #35는 디자인 보조색 `#979EA9`를 source 값으로 유지하되, 사용자가 읽어야 하는 text(목록 preview/count/시간, 선택되지 않은 정렬, 검색 label, 날짜, 좋아요 수, 댓글 시간/count/action, dialog 본문, 상태 안내, placeholder)와 의미 있는 icon/focus border를 `#6E7785`(흰 배경에서 4.53:1)로 render합니다. dialog 취소 label은 `#EAECEE` 위에 `#5E5E5E`를 씁니다. 비활성, 읽기 전용, 눌림 feedback state는 `#979EA9`를 유지합니다(`docs/UI_SPEC.md` §6.3).

### Final V-081 Audit — 2026-10-04

- **Base:** `main` `04dd668ae9d80d899edd8d363b42db5096bf4ac8`, 깨끗한 working tree.
- **공식 source:** Figma file `Ee1gp5NC6G1sQIvssuu4Sr`, Page 1 (`0:1`). 공식 integration으로 현재 file identity, Page 1 node tree, frame ID/이름, mapping된 frame 12개 모두의 `375×778` 크기를 직접 다시 확인했습니다.
- **시각 baseline:** V-081 동안 현재 공식 file/node mapping을 다시 확인한, repository에 보존된 공식 Figma frame export입니다. 이 audit 중에 Figma에서 새로 export한 screenshot이 아닙니다.
- **Runtime 비교:** Chromium에서 `375×778` CSS viewport, DPR 1, zoom 1, 안정된 seed/요청자 context의 production Compose build.
- **범위:** 공식 export 12개 전체와 수정 state가 채워진 공유 작성/수정 form. 대표 비교 세 개가 게시글 목록, 게시글 상세, 공식 bottom sheet와 현재 anchored popover를 다룹니다.
- **분류:** 공식 디자인이 정의한 요소를 runtime geometry와 시각 표현과 비교했습니다. 차이는 `docs/UI_SPEC.md`, `docs/ENGINEERING_NOTES.md`, `design/design-retouch/readme.md`, 위 P8-C 메모에 이미 있던 product decision을 기준으로 분류했습니다. runtime 전용 loading/error/retry, mutation lifecycle, focus, 취소, cache, scroll 동작은 시각 불일치 분류에서 제외했습니다. pixel-difference threshold는 쓰지 않았습니다.
- **발견:** 우발적 불일치 `NONE`; 이전에 문서화된 의도적 차이 범주 11개; production 변경 불필요.
- **근거:** [`DESIGN_COMPARISON.md`](DESIGN_COMPARISON.md).
- **Historical 구분:** 앞의 `NOT_RUN` 항목들은 그 날짜에 유효합니다. local PNG/SVG/runtime 근거만으로는 현재 공식 source mapping을 세울 수 없었기 때문입니다. 최종 audit는 먼저 공식 Figma file/node mapping을 다시 확인하고, repository에 보존된 공식 export를 현재 runtime state에 대응시켰기 때문에 통과할 수 있었습니다.
- **현재 적용 여부:** 실행 baseline은 `04dd668`입니다. 그 뒤 현재 `main` `f3f79fbc61a9215014419f58e91a596ede252a1e`까지 `frontend/`, `design/` 등 `docs/`·`README.md` 밖의 변경이 없음을 git diff로 확인했으므로, 이 결과는 현재 production UI에도 적용됩니다. V-081을 현재 `main`에서 다시 실행한 것은 아닙니다.

**결과:** `PASS`

---

# 최종 문서

## V-090 — README 정확성

**요구사항:** `DOC-001`부터 `DOC-009`

### 단계

README를 최종 code와 대조해 검토합니다.

### 기대 결과

- 기술 선택이 실제 dependency/구현과 같습니다.
- Server state 설명이 실제 code와 같습니다.
- 계층 설명이 실제 file/function을 짚습니다.
- Seed 설명이 실제 initializer/schema와 같습니다.
- Test가 정확히 설명되어 있습니다.
- Trade-off가 실제 결정을 반영합니다.
- 해당하면 UX 차이/개선이 문서화되어 있습니다.

### 실행 근거 — 2026-10-01

README를 `main` `ef8bb75`와 대조해 검토했습니다. CI, read 취소, dialog focus 가두기에 맞춰 동기화한 뒤 `README.md`를 교체했고, staging draft는 제거했습니다.

- **File과 function:** 이름이 나온 class가 모두 존재합니다.
  - `PostController`, `CommentController`;
  - `PostReadService`, `PostMutationService`, `CommentMutationService`;
  - `PostRepository`, `CommentRepository`, `PostLikeRepository`;
  - `IdentitySequenceRepository.synchronizeWithSeededRows()`;
  - `SeedInitializer`, `SeedHistoryRepository`, `SeedReadiness`;
  - `RequesterResolver`, `MutationValidator`, `ApiExceptionHandler`.

  이름이 나온 frontend file도 존재합니다: `keys.ts`, `postListCache.ts`, `useInfiniteScrollTrigger`, `features/post-form`, `client.ts`, `community.ts`, `ConfirmDialog.tsx`.
- **동작 주장이 code와 같음:**
  - `@Transactional` `SeedInitializer.run()`과 `assignment-v1` marker;
  - `afterCommit`에서 열리는 readiness와 `/health` 503 `starting`;
  - `V1` identity column, `is_deleted`, `base_like_count`, `post_likes(post_id, user_id)` primary key;
  - 두 repository의 row-value `(created_at, id) < (…)`;
  - `ILIKE … ESCAPE`, `limit + 1`, 20 code point 제목 규칙;
  - ownership 확인 전의 `FOR UPDATE`;
  - `postListCache.ts` invalidation 규칙;
  - page size 15, 2, 30, `rootMargin` 200px, debounce 300 ms; *(2026-10-01 기준 historical. post 목록은 이제 `rootMargin` 500px를 넘기며(PR #38), 200px는 댓글이 쓰는 공용 기본값으로 남았습니다. "F-04" 참고.)*
  - `gcTime: 0`, 상세 link에만 `prefetch={false}`, 108px row, 44px toolbar, 144px popover;
  - likes 목록 중복 제거;
  - query function → `request()` → `fetch`로 이어지는 `AbortSignal` 경로;
  - `ConfirmDialog`의 Tab과 pending focus 처리;
  - CI job 단계.
- **측정:**
  - SQL-plan 수치가 `docs/PERFORMANCE.md`와 같습니다.
  - page size 요청 수는 데이터에서 따라 나옵니다: post 39개는 4/3/2, continuation 댓글 1,409개는 141/71/47. 적용된 `15/15/9` 순회는 PR #23에 있습니다.
  - 비교용 수치(p95 248 ms, 424 B, 250 ms 멈춤, fold 아래 8 vs 13 row)는 이전 측정 session에서 나온 것이며 repository의 다른 곳에는 기록되어 있지 않습니다.
  - *(page size 결정의 근거로는 이후 대체됨. post 39개 전체 순회 비교에는 데이터 끝 효과가 섞여 있었습니다. post 크기는 first-page SSR 이후 일회용 160-post dataset에서 고정 깊이로 다시 측정했고(`docs/PERFORMANCE.md` §13, 15/15 유지), 댓글 continuation은 post #2에서 고정 깊이로 다시 측정했습니다(§15, 30 유지). "F-03" 참고.)*
- **Link:** 외부 참조 11개가 모두 200을 반환했고, fragment anchor 4개가 존재합니다.
- **깨끗한 text:** `README.md`에 PR 번호, SHA, phase 이름, 요구사항·시나리오 ID, 상태 label, staging text가 남아 있지 않습니다.
- **Architecture 문서:** `docs/ARCHITECTURE.md`도 같은 code와 대조해 README와 모순되지 않게 했습니다.
  - ADR-004는 이제 실제 `V1` schema를 보입니다: `is_deleted BOOLEAN`, `updated_at` 없음, `post_likes.created_at` 없음, `TEXT` author. `deleted_at` 제안은 기각한 대안으로 남겼습니다.
  - ADR-002, 003, 010, 013은 구현 참조와 함께 `OPEN`/`PROVISIONAL`에서 `DECIDED`로 바뀌었습니다.
  - 초기 backend/frontend 구조 제안은 실제 package와 folder tree로 대체되었습니다.
  - P0와 phase update section은 history로 표시했고, 이전 P1–P3 미결 결정 표를 맞췄습니다. 당시 한 항목이 남았습니다: commit된 DB integration test suite가 없음.
  - *(이후 대체됨. PR #42가 commit된 PostgreSQL integration test suite(Testcontainers `postgres:16-alpine`, 실제 Flyway `V1`과 `SeedInitializer`)를 추가했고, PR #44가 동시성 test를 더했습니다: `7da54e8`에서 6 suites / 31 tests. "F-07"과 "F-09" 참고.)*
- **다루지 않음:** 이는 문서 정확성 review입니다. runtime acceptance를 다시 실행하지 않으며, `V-081`(공식 디자인 fidelity)을 통과시키지 않습니다.

*현재 적용 여부:* 위 V-090은 2026-10-01 `ef8bb75`에 대한 실행 근거입니다. 이후 README는 문서 정리에서 다시 쓰였고 2026-10-04 `3c902dc` 기준으로 다시 audit되었습니다(`docs/REQUIREMENTS.md` §13). `3c902dc` 이후 현재 `main`까지 production/config code 변경은 없습니다. 이는 새 V-090 실행이 아닙니다.

**결과:** `PASS`

---

# 최종 Engineering 검증 — 2026-10-02 through 2026-10-04

F-01부터 F-16은 V-090 이후 `main` @ `7da54e8a361d2b8212a46e8b9e8091b8240ce3fa`까지 수행한 engineering 검증을 기록합니다. F-17은 이후 추가된 commit된 Playwright 회귀를 기록하며, 당시 base `main` @ `3c902dc9126b2eb2ec0713844b3d7cbec237e80f`에서 검증했습니다. 각 section은 무엇을 확인했는지, 방법, 핵심 근거, 결과, 한계를 적습니다. 상세 측정 표는 `docs/PERFORMANCE.md` §13–§17에 있습니다. PR body(#40–#47)에 F-01–F-16의 전체 audit log가 있으며, 그 browser helper script와 일회용 stack은 commit하지 않았습니다. F-17은 다릅니다. 그 Playwright flow 세 개는 commit되어 CI에서 실행됩니다. 따로 적지 않으면 browser 실행은 375×778 Chromium이었고, mutation은 영구 기본 volume이 아니라 일회용 Compose project나 Testcontainers database에서만 실행했습니다.

*현재 적용 여부:* `7da54e8` 이후 현재 `main`까지 `docs/`·`README.md` 밖의 변경은 F-17의 Playwright/CI/Vitest 구성(`.github/workflows/ci.yml`, `frontend/package.json`·lockfile, `playwright.config.ts`, `tests/e2e/core-flows.spec.ts`, `vitest.config.ts`, `frontend/.gitignore`)뿐이고, `3c902dc` 이후에는 그런 변경도 없습니다. product runtime code는 F-01–F-16의 범위 이후 바뀌지 않았습니다.

**Backend test 수 이력(각 날짜에 맞는 값):** 26 (P2A) → 49 (P5B/P6) → 51 (P8-B) → 60 (post discovery, CI) → 83 (PR #42, PostgreSQL integration test +23) → **91** (PR #44, 동시성 test +8; 현재).
**Frontend test 이력:** 없음 → Vitest 15 tests (PR #43) → **21 tests / 4 files** (PR #45; 현재).

## F-01 — First-Page SSR과 Lighthouse

**확인한 것:** 기본 목록의 첫 page를 server render하면 실제 첫 content 경로가 짧아지는지, 그리고 이 app에서 Lighthouse를 어떻게 읽어야 하는지.

**SSR 전(client 전용 목록).** PR #36 전 `main`에서 목록 HTML에는 loading 문구만 있었고, browser가 hydration 후 `/api/posts`를 가져왔습니다. Lighthouse 13.5.0 CLI, `--throttling-method=devtools`, 기본 mobile 412×823 (DPR 1.75), Docker production frontend:

- 2026-10-02 `80cc2fb` baseline: Performance 75/79/76, LCP 3,439/3,365/3,464 ms; LCP element는 첫 post preview; browser `/api/posts` 1.
- 2026-10-02 `d5d545a`(3회): Performance median 77, LCP median 3,467 ms (3,380–3,624), TBT median 472 ms, browser `/api/posts` 1.
- Trace: HTML ≈0.6 s → JS ≈2.0 s → `/api/posts` 시작 ≈2.3 s → 응답 ≈2.9 s(server 작업 ≈25 ms, 나머지는 emulate한 latency 562.5 ms) → LCP. 요청은 hydration 전에 시작할 수 없었습니다.

**SSR 구현(PR #36).** `app/page.tsx`가 기본 첫 page를 server에서 가져와 `initialData`로 넘기고, 이후 page, likes, 검색은 client 요청으로 남습니다. Lighthouse 6회(같은 방법, 2026-10-02):

| 실행 | 1 | 2 | 3 | 4 | 5 | 6 | Median |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Performance | 93 | 87 | 94 | 92 | 93 | 93 | 93 |
| LCP (ms) | 2,401 | 2,819 | 2,313 | 2,509 | 2,194 | 2,183 | 2,357 |
| TBT (ms) | 147 | 205 | 120 | 130 | 200 | 192 | 169.9 |

- 6회 모두 A11y / Best Practices / SEO 100/100/100; CLS 0.0003; browser 첫 `/api/posts` 0; LCP element `p.PostListItem_preview`; `/` First Load JS 121 kB(build), `/`는 static에서 dynamic으로 바뀜.
- 기능 확인(두 요청자 경우 모두): SSR HTML 15 rows, SSR HTML / DOM / backend의 `isLiked` 일치, scroll 시 중복 없이 39 rows, 좋아요, 검색, 뒤로 가기 경로 3개, hydration error 0(`Date.now()` 대조군에서 실패한 59 s / 59 min 59 s / 23 h 59 min 59 s 상대 시간 집중 test 포함).
- 당시 실패 fallback(timeout 1.5 s): 연결 거부 → 0.04–0.18 s에 200, blackhole → 1.54–1.60 s에 200, non-2xx → 200; 각각 client fetch로 복구했고, SSR과 browser가 모두 실패하면 기존 error/retry UI를 보였습니다. *(timeout은 이후 F-02에서 2 s로 바뀜.)*

**접근성과 SEO(PR #35, SSR 전).** Lighthouse Accessibility 96 → 100(`color-contrast`), SEO 91 → 100(`meta-description`), Best Practices 100 유지. 세 실행 모두에서, 그리고 `/posts/1`과 `/posts/new`에서도.

**Font payload audit(2026-10-02, `d5d545a`, code 변경 아님).** 2,057,688 B Pretendard variable file은 공식 `pretendard@1.3.9` file과 byte 단위로 같고 현대 한글 음절 11,172개를 모두 다룹니다. 공식 variable dynamic subset(file 92개)은 font 전송을 344,778 B(요청 13개)로 줄이고 simulated Performance를 73 → 82로 올렸지만, devtools throttling에서는 LCP를 3,477 → 4,007 ms, TBT를 517 → 755 ms로 늘렸습니다(render-blocking CSS 1.0 → 13.7 KB, font 요청 13개가 API·JS와 경쟁). 현재 file로 `*.woff2`를 막아도 LCP가 바뀌지 않았으므로(3,477 vs 3,604 ms) LCP 경로에 있지 않습니다. 결정: `KEEP_CURRENT`. 이는 측정한 LCP 경로에서 blocker가 아니었다는 뜻이지, 2 MB font가 항상 문제가 없다는 뜻이 아닙니다.

**Simulated vs devtools throttling.** 같은 PR #36 build를 Lighthouse 기본 simulated throttling으로 측정하면 LCP 12,179 / 12,197 / 12,203 ms, Performance 56이었습니다. simulation은 2 MB font download를 LCP graph에 넣지만, 관찰된 paint는 font를 기다리지 않습니다(font는 약 14 s에 끝나며 content paint보다 훨씬 늦음). 따라서 이후 현재 build의 ≈12 s simulated 결과(Chrome 154, Lighthouse 12.8.2로 보고됨)는 과거 simulated 동작과 같으며, 그 자체로 회귀가 아닙니다. simulated throttling이 "틀린" 것이 아니라 두 방법의 model이 다르며, 같은 browser, Lighthouse version, throttling 방법, 화면, dataset에서만 비교할 수 있습니다. 이 app의 회귀 확인은 devtools throttling을 씁니다(F-15).

## F-02 — SSR Seed Freshness와 Server Timeout (PR #41)

상세: `docs/PERFORMANCE.md` §17.

- `SEED_FRESH_MS` 30 s → **5 s**. throttling hard load: 0 s는 5/5회 hydration 중복 fetch, 5, 15, 30 s는 각각 0/5. hydration 시 관찰한 seed 나이는 약 1.65 s여서 5 s가 통과한 가장 짧은 후보였습니다. client fetch가 한 번이라도 일어나면 목록 staleTime은 다시 0입니다.
- `SERVER_FETCH_TIMEOUT_MS` 1.5 s → **2 s**. server에만 1.8 s 지연을 걸었을 때 2 s budget은 실제 post LCP 2.98 s, 1.5 s fallback은 4.04 s였습니다. blackhole backend에서 2 s는 0.54 s 더 기다리고 여전히 client fallback과 함께 HTTP 200을 반환합니다.
- 변경 후 확인: SSR HTML 15 rows, browser 첫 목록 요청 0, page error 0. PR #45가 실제 backend `docker pause`로 fallback을 다시 확인했습니다: `/`는 약 2.2 s에 row 없이 200을 반환했고, client가 첫 page를 가져왔으며, hydration error는 0이었습니다.
- Trade-off: 5 s 이후 focus나 remount가 refetch할 수 있고, 멈춘 backend는 HTML을 최대 2 s 늦춥니다.

## F-03 — Page-Size 결정

- **Posts — `KEEP_15_15`**(`docs/PERFORMANCE.md` §13, 2026-10-02). 이전의 post 39개 전체 순회 비교(V-090 측정)는 historical입니다. SSR 이후 SSR 크기와 continuation 크기를 일회용 160-post dataset(seed 분포 유지)에서 375×778, CPU 4×, devtools mobile throttling으로 따로 측정했습니다.
  - SSR 10/15/20: LCP median 2,278 / 2,260 / 2,372 ms, range가 겹침(noise); row +5 ≈ 압축 HTML +0.9 KB, DOM element +100; backend fetch와 Next TTFB 차이 ≈10 ms. 15는 fold 아래 8 rows(~1.2 화면)를 유지합니다.
  - 고정 깊이 30·45 posts에서 continuation 10/15/20: 10은 요청이 가장 많았고 읽기 속도에서도 loading 문구와 빈 영역이 나타난 유일한 후보였습니다. 20은 15보다 일관된 이득이 없었고 혼합 크기 분기가 필요했을 것입니다.
  - 크기는 전체 row 수에서 끌어내지 않습니다: cursor pagination은 요청마다 `limit + 1` rows를 읽고, 중요한 것은 post가 몇 개 있는지(42, 1,000, 100,000)가 아니라 한 session이 얼마나 소비하는지입니다.
- **Comments — 처음 2(product 계약), continuation `KEEP_30`**(`docs/PERFORMANCE.md` §15, 2026-10-03, 댓글 1,411개인 post #2). 10/20/30/50에서 첫 더보기 click → row 639 / 660 / 670 / 725 ms; 댓글 120개까지 요청 13 / 7 / 5 / 3; 훑을 때 추가 loading 노출 8 / 2 / 1 / 2; 25개에서 일찍 떠날 때 쓰지 않은 댓글 6 / 16 / 6 / 26. 30을 바꿀 이유를 준 후보는 없었습니다.
- 한계: emulate한 기기와 network profile 하나, scripted scroll 속도, HTTP/1.1 `next start`; 나열한 후보만 비교했으므로, 이 값들은 비교한 값 중 가장 합리적인 것이지 전역 최적값이 아닙니다.

## F-04 — Prefetch 거리 (`rootMargin`)

`docs/PERFORMANCE.md` §14 (PR #38, 2026-10-02). post 목록은 `0px 0px 500px 0px`를 넘기고, hook 기본값은 200px로 유지되어 댓글이 계속 씁니다. post 목록에 대한 이전 "200px" 서술은 historical입니다.

- 모든 후보에서 요청 → row가 약 0.66 s(API 600–611 ms + DOM 48–78 ms) 걸렸습니다. ≈600 px/s로 훑으면 약 400 px 이동입니다.
- 60 posts까지 빠르게 훑기(5회): 200px는 모든 page 경계에서 기다렸고(15 of 15 cycle, 최장 305 ms, 누적 670 ms); 300px는 절반에서(10 of 20, 누적 419 ms); 400px는 대기는 없었지만 모든 실행에서 157–230 ms 빈 영역; 500px는 대기와 빈 영역 없음(loading 문구 1회, 171–272 ms).
- 읽기 속도: 어떤 후보도 기다리지 않았습니다. 어떤 후보도 scroll 전에 page를 요청하거나, 요청을 연쇄하거나, 일찍 떠날 때 더 로드하지 않았습니다. 500px의 비용은 checkpoint에서 미리 가져온 최대 한 page(15 rows, 5.6 KB)입니다.
- 160-post dataset에서 branch 검증: scroll 없이 요청 없음, 훑을 때 대기·빈 영역 없음, post 160개 전체 순회에서 중복·누락 없고 끝 이후 요청 없음; production 회귀(SSR, 좋아요, 검색, 뒤로 가기) 통과.
- 더 큰 값은 test하지 않았습니다. 측정한 geometry(첫 375×778 viewport 아래 934 px의 sentinel)로 보면, 1,000 px처럼 약 934 px 이상의 margin은 scroll 전에 page 2를 요청하므로, 더 큰 margin이 자동으로 더 좋은 것은 아닙니다. 높이 약 1,212 px 이상의 viewport도 500px에서 page 2를 한 번 미리 가져옵니다(geometry, 측정 아님).
- 댓글은 200px 유지: 읽기에서는 대기가 없었고, 훑기에서 120개 중 짧은 대기(158–201 ms)가 한 번 있었습니다(`KEEP_COMMENT_ROOT_MARGIN_200_FOR_NOW`).

## F-05 — 댓글 규모와 Virtualization

`docs/PERFORMANCE.md` §15. post #2가 가장 큰 thread(읽을 수 있는 댓글 1,411개)입니다.

- continuation 30으로 전체 로드(3회, 각각 continuation 요청 47개): 댓글 1,411개에서 DOM 약 10,000 element, JS heap 약 11–14 MB. append당 응답 → DOM이 약 30 ms에서 65–88 ms로 늘었고; 50 ms 넘는 long task는 600개까지 구간당 0–1개에서 1,000–1,411 사이 13–14개(최장 72–82 ms)로 늘었습니다.
- Scroll은 나빠지지 않았습니다: 모든 checkpoint에서 probe가 10 ms 안에 scroll 위치를 갱신했고, 첫 checkpoint 이후 50 ms 넘는 frame이 없었습니다.
- `gcTime: 0` 재확인: 댓글 122개나 1,411개를 로드한 뒤 재방문하면 `limit=2` 요청 1개를 보내고, 더보기와 함께 댓글 2개를 render했으며, 3.5 s 동안 continuation을 보내지 않았습니다.
- 결정: `VIRTUALIZATION_NOT_NEEDED_NOW`. 이 dataset과 profile에서 측정된 비용(약 1,000개 이후 append long task, 100 ms 미만)은 별도 rendering layer를 정당화하지 않았습니다. 이는 virtualization이 필요 없다는 일반적 주장이 아니며, 늘어나는 append 비용이 더 긴 실제 대화나 더 느린 기기에서 다시 볼 신호입니다.

## F-06 — Client Read Retry 정책 (PR #40, runtime은 PR #45)

상세: `docs/PERFORMANCE.md` §16. 공유 QueryClient 정책 하나(`retryOnceUnlessDeterministicClientError`): `ApiError` 4xx → retry 없음; network, 5xx, parse, 알 수 없는 read 실패 → 기본 1 s 지연으로 retry 1회. mutation은 TanStack Query 기본값인 retry 0을 유지합니다. *(그 전의 TanStack Query 기본값은 시도 4회, 영구 실패까지 약 7 s였습니다. §16 참고.)*

| 실패 | 요청 | 결과 |
| --- | ---: | --- |
| 404 상세 | 1 | not-found, retry 없음 |
| 400 continuation (`INVALID_CURSOR`) | 1 | 다음 page error, 수동 retry 동작 |
| 500 후 성공 | 2 | 성공, error UI 안 보임 |
| network 후 성공 | 2 | 성공, error UI 안 보임 |
| 잘못된 JSON 후 성공 | 2 | 성공, error UI 안 보임 |
| 500 두 번 / 잘못된 JSON 두 번 | 2 | 최종 error, 수동 복구, unhandled rejection 없음 |
| 취소(대체되거나 unmount) | — | abort, retry 안 함, error state 없음 |

- 이유: read는 idempotent이므로 retry 한 번이 사용자를 몇 초씩 붙잡지 않고 일시 장애를 흡수합니다. write retry는 idempotency key 없이 side effect를 중복시킬 수 있습니다.
- 한계: 모든 4xx를 결정적인 것으로 취급합니다. 429처럼 retry할 만한 4xx가 생기면 이 분류를 다시 봐야 합니다.

## F-07 — 실제 PostgreSQL Integration (PR #42)

- **방법:** `PostgresIntegrationTest`가 test JVM마다 `postgres:16-alpine` Testcontainers container 하나를 시작합니다. 전체 Spring context가 실제 Flyway `V1` migration과 `seed/seed.json`을 쓰는 실제 `SeedInitializer`를 실행하고, test는 production `JdbcClient` repository와 service를 호출합니다. 기대 순서는 원시 `posts`/`post_likes` row에서 Java로 계산합니다(원시 SQL/JDBC oracle). test는 rollback합니다.
- **범위:** page 경계를 넘는 같은 `created_at` 동점과 바뀌는 limit(2→3→1→5)의 latest keyset; 유효 count 동점과 `created_at`/`id` 동점 처리의 likes keyset; 검색(대소문자 무시, 제목/본문, 미끼와 함께 문자 그대로의 `%`, `_`, `\`, 결과와 total에서 삭제 post 제외); `base_like_count` + 관계 좋아요 집계; 요청자 `isLiked`; 읽을 수 있는 `commentCount`; 동점과 바뀌는 크기의 댓글 keyset과 가장 큰 thread 전체 순회; count와 삭제 flag가 일치하는 정확히 1회 seed 적재; `MAX(id)`보다 앞선 identity sequence; schema 제약(중복 좋아요 PK, 댓글 FK, `base_like_count >= 0`); 소유자/비소유자/없음 결과의 mutation 영속성.
- **당시 수:** backend test 83개, 그중 23개가 새 PostgreSQL integration test(현재는 F-09와 함께 91 / 31).
- **민감도:** `PostRepository`에 주입한 bug 세 개가 각각 잡혔습니다(`id ASC` 동점 처리, `post_likes`를 무시한 likes 정렬, backslash escaping 비활성화). 명시적 `ESCAPE '\'`만 제거한 것은 잡히지 않는데, backslash가 이미 PostgreSQL의 기본 LIKE escape라 동작이 바뀌지 않으므로 예상된 결과입니다.
- **Dependency 메모:** Boot가 관리하는 1.21.0이 Docker Engine 29가 거부하는 Docker API 1.32를 요청했기 때문에 Testcontainers를 `1.21.4`로 고정합니다. Docker daemon이 없으면 이 test는 skip하지 않고 실패합니다.

## F-08 — Frontend Cache 일관성 (PR #43)

- **방법:** 일회용 Compose project(`apr-cache-audit`), 375×778 Chromium, oracle로 backend REST 전체 cursor 순회와 SQL(QueryClient는 확인만 하고 oracle로 쓰지 않음). 깨끗한 실행 한 번에서 runtime 확인 **148/148**이 통과했습니다.
- **범위:** 작성(빠른 submit 3번에 POST 1개, total +1로 맨 앞 추가, 작성 전 cursor에서 이어 읽기 중복 0 누락 0, likes/검색 invalidate), 수정(제자리 교체, 이전·새 검색 membership 갱신), 좋아요(page-1/page-2 경계를 포함한 likes 목록 재정렬), 댓글 작성/삭제(접힘과 펼침), likes page-1 cursor row에서 post 삭제, 실패 경우(좋아요, 댓글, 삭제, 수정, 작성의 500과 network: 요청 1개, retry 없음, cache 그대로, 입력 유지), 뒤로 가기와 hard refresh, `/dev-user`로 여러 요청자.
- **결과:** `CACHE_CONSISTENCY_VERIFIED`. production code는 바뀌지 않았습니다: audit는 기존 cache 전략을 확인했고, `lib/query/postListCache.ts`에 대한 결정적 test 15개(각각 guard를 제거하면 실패함을 보임)를 가진 Vitest runner와 CI의 `npm test`를 추가했습니다. 현재 `removePost`/`addCreatedPost`/`replacePost`/`setPostLike` 규칙은 이 audit의 동작과 같습니다(`docs/ARCHITECTURE.md` "Post Discovery 반영 (검색 + 정렬)"의 cache matrix).
- **기대 동작 메모:** invalidate된 검색 목록은 refetch가 도착할 때까지 잠깐 cache된 row를 보이며, stale 상태가 남지 않습니다.

## F-09 — Mutation 동시성 (PR #44, Part A)

- **방법:** Testcontainers PostgreSQL에서 `MutationConcurrencyIntegrationTest`(8 tests). 각 worker는 자기 transaction에서 실제 service proxy를 호출하고, 별도 JDBC transaction이 `SELECT … FOR UPDATE`를 잡으며, `pg_stat_activity`가 worker가 lock을 기다린다고 보여 준 뒤에만 worker를 풀어 줍니다.
- **시나리오:** 같은 user가 동시에 두 번 toggle, 두 user가 동시에 좋아요, 댓글 작성 두 개, 댓글 작성 / 좋아요 toggle / 수정이 기다리는 동안 삭제 commit(각각 404, row 생성·변경 없음), 소유자가 두 번 삭제(성공 1, 404 1), 강제하지 않은 post 삭제 vs 댓글 삭제 경합(실행당 ×20, 유효한 두 순서만 관찰).
- **민감도:** `FOR UPDATE`를 제거하자 8개 중 4개 test가 실패했고, 두 좋아요 toggle 경합에서 실제 `DuplicateKeyException`이 났습니다.
- **안정성:** class가 연속 10/10회 통과했고, `gradlew clean test`가 3/3회(91 tests) 통과했습니다.
- **결과:** `CONCURRENCY_INVARIANTS_VERIFIED`. 이 test에서 deadlock은 관찰되지 않았고, 현재 lock 순서에 명백한 cycle은 없습니다(post 삭제는 row 하나를 잠금). deadlock이 절대 일어날 수 없다는 증명은 아닙니다. 같은 post의 좋아요는 post row에서 직렬화됩니다(granularity trade-off).
- **현재 lock 의미(method별):** transaction은 원자성/rollback 경계이고, row lock은 경합 mutation 직렬화용이며 transaction이 자동으로 row lock을 거는 것은 아닙니다. post 생성은 잠글 기존 row가 없고; post 수정/삭제/좋아요는 읽을 수 있는 post를 잠그며; 댓글 생성은 읽을 수 있는 parent post를 잠그고; 댓글 삭제는 읽을 수 있는 댓글과 parent를 함께 잠급니다(`FOR UPDATE OF p, c`). `docs/ARCHITECTURE.md` ADR-012 참고.

## F-10 — 변하는 좋아요 순위 (PR #44, Part B)

- likes 정렬 page 각각은 올바르게 정렬되고 cursor chain은 항상 끝나지만, 여러 요청에 걸친 순회는 **snapshot 일관성이 없습니다**: cursor가 요청 사이에 바뀔 수 있는 좋아요 수를 저장하기 때문입니다.
- 실제 REST와 cursor로 일회용 stack에서 재현했습니다.
  - **누락:** page 1이 B에서 끝난 뒤 다른 user의 좋아요로 C가 B와 동점이면서 더 최신이 되어 cursor 위로 올라갔습니다. 이전 순회는 44 row 중 43개를 찾았고(C 누락) 그래도 정상적으로 끝났습니다.
  - **중복:** page 1이 A에서 끝난 뒤 좋아요 취소로 A가 내려갔고, 이전 cursor의 page 2가 A를 다시 반환했습니다(원시 44 row, 고유 43).
  - 두 pattern 모두 검색 안에서도 재현됩니다. 같은 좋아요 변동에서 latest 정렬은 중복 0, 누락 0이었습니다.
- Frontend: 목록의 중복 제거가 중복을 숨기지만(54 row, 고유 id 54), 누락된 row는 복구할 수 없습니다. page 1부터 새로 순회하면 현재 순위가 복원됩니다.
- 결정: `ACCEPTED_MUTABLE_RANKING_TRADEOFF`. snapshot이나 versioned cursor는 이 과제를 넘는 schema, API, cache 복잡도를 더합니다.

## F-11 — READ COMMITTED에서 Page와 Total (PR #44, Part C)

- 의도적으로 변동이 큰 stress 실행(writer 하나가 loop로 post를 만들고 soft delete, reader 넷이 30 s 동안 검색 page 하나를 호출)에서 read 26,288개 중 4,204개가 `items.length ≠ total`이었고, 항상 ±1이었습니다. (audit 요약은 약 4,018 write를 보고했고, PR body가 read 수를 기록합니다.)
- 원인: READ COMMITTED에서 page query와 `COUNT` query는 별도 snapshot을 가진 별도 statement이므로, 그 사이의 commit이 하나를 1만큼 움직일 수 있습니다.
- 이 수치는 인공 stress 환경에서 나온 것이며 일반 traffic의 불일치율이 아닙니다.
- 결정: 현재 metadata 의미로 수용(`total`은 같은 요청에서 오지만 같은 snapshot은 아님); 단일 snapshot transaction이나 더 강한 isolation은 추가하지 않았습니다.

## F-12 — Edge, 실패, 복구 (PR #45)

일회용 stack `apr-edge-audit`, 375×778 Chromium, route interception과 실제 backend `docker stop` / `docker pause`로 장애 주입.

- **입력:** 잘못된 post ID(`0`, `-1`, `1.5`, `abc`, `001`, 매우 큰 값)는 요청 없이 not-found를 보입니다; 없음, max-safe-integer, 삭제된 ID는 GET 1번 → 404; 비소유자 수정은 PATCH 없이 권한 없음 안내; `limit` 0/51 → 400, 1/50 → 200; 잘못된 sort, 잘못되거나 다른 sort의 cursor → 400; 100 code point 초과 `q` → 400, 정확히 100 → 200.
- **끝 상태:** 결과 0개 검색은 메시지를 보이고 추가 요청을 보내지 않습니다; post 순회(latest: SSR page + 2 pages, likes: 3 pages)와 댓글 순회(post 8: `2 → 30 → 30`, 58/58)는 중복 없이, 끝 이후 요청 없이 끝납니다.
- **실패와 복구:** post와 댓글 continuation의 500/network 장애는 2번 시도하고, 로드된 row를 유지하며, scroll에 자동 retry하지 않고, 수동 retry로 복구해 순회를 완료합니다; 실패한 첫 검색은 최종 error를 보이고 복구됩니다.
- **취소:** debounce는 입력한 단어에 요청 1개를 보냅니다; 대체된 검색과 빠른 sort 변경은 retry나 error UI 없이 abort되며, 최종 row는 backend와 같습니다.
- **가용성:** 멈춘 backend는 최종 error로 이어지고, 재시작 후 수동 retry는 reload 없이 성공합니다; backend가 pause된 SSR은 client로 fallback합니다(F-02).
- **이동:** history 뒤로/앞으로, 작성 draft 뒤로, 좋아요 + 검색 + 상세 + 수정 뒤로가 mutation 없이 view를 복원합니다; hard refresh는 문서화된 대로 검색/정렬을 초기화합니다(`lib/postListView.ts`).
- **결과:** `EDGE_FAILURE_BEHAVIOR_VERIFIED`; production bug 없음. 순수 Vitest 계약 test(`parsePostId`, retry 정책, `normalizePostQuery`)만 추가해 frontend는 4 files 21 tests가 되었습니다.
- **수용한 trade-off:** 검색 입력의 `maxLength=100`은 UTF-16 unit을 셉니다(emoji에 대해 API보다 엄격); 검색이 열린 동안 sort를 바꿀 수 없습니다(toolbar 설계).

## F-13 — Full-Stack CI Gate (PR #46)

- `7da54e8`의 CI job: `backend`(Testcontainers와 함께 `./gradlew test`), `frontend`(`npm ci` → `npm run typecheck` → `npm test` → `npm run build`), `full-stack-smoke`(둘 다 끝난 뒤 실행).
- `full-stack-smoke`(`scripts/ci-full-stack-smoke.sh`)는 깨끗한 GitHub Actions runner에서 production image를 build하고 확인합니다: `docker compose config`, 모든 service healthy, `/health` `{status: ok, db: up}`, latest post API, 그 post를 담은 frontend SSR HTML, `seed_history` `assignment-v1` = 1, frontend build에 고정된 browser API base.
- 민감도: `API_BASE_URL`을 일시적으로 없는 내부 host로 바꾸자 SSR assertion이 실패했습니다.
- `main` merge commit `7da54e8`의 CI 실행이 성공했습니다(#45와 #46도).
- 이 section은 historical PR #46 gate입니다. 이후 PR #50이 같은 `full-stack-smoke` job에 commit된 Playwright 회귀를 더했습니다. F-17 참고.
- 한계: `main`에 branch protection이 켜져 있지 않습니다(GitHub API `protected: false`). gate는 그 merge의 `main`에 존재했고 통과했지만, merge를 기술적으로 막지는 않았습니다.

## F-14 — Dependency 보안

- **이력:** PR #33이 security advisory에 맞춰 Next.js/React를 갱신했고; PR #34가 Next.js 15.5.24 → 15.5.27(Maintenance LTS)과 sharp 0.34.5 → 0.35.5를 Next가 선언한 범위 안에서 옮겼습니다(npm audit 3 → 2; 남은 두 개는 PostCSS 8.4.31과 PostCSS를 통한 Next). PR #47이 최종 patch를 적용했습니다.
- **PR #47:** root `overrides` 항목이 `postcss`를 `8.5.28`로 고정해, `next@15.5.27`이 요청하는 취약한 `8.4.31`을 대체합니다(≥ 8.5.23이 source-map advisory를 patch). 이는 Next.js 자체가 선언한 version이 아니라 project override이며, typecheck, test, build, production Compose, CI로 호환성을 확인했습니다. Spring Boot 3.5.0 → **3.5.16**(Spring Framework 6.2.7 → 6.2.19), 같은 3.5.x 계열; Testcontainers는 `1.21.4` 유지.
- **결과:** `npm audit` 0, `npm audit --omit=dev` 0(`7da54e8`와 clean clone에서 재확인). 현재 `main`의 `package.json`·`build.gradle` 값(Next 15.5.27, React 19.1.9, PostCSS override 8.5.28, Spring Boot 3.5.16)은 이와 같습니다.
- **보안 review 범위**(최종 보안 audit, 가능한 곳은 code에서 재확인): SQL은 요청 값을 이어 붙이지 않고 `JdbcClient` parameter binding을 씁니다; sort는 enum whitelist(아니면 `INVALID_SORT`); `dangerouslySetInnerHTML` 등 원시 HTML 주입 경로 없음; fetch는 설정된 API base만 대상(사용자가 정하는 host 없음); custom redirect나 middleware 없음; CORS는 기본 `http://localhost:*`와 `http://127.0.0.1:*`이며 실제 배포에는 `APP_CORS_ALLOWED_ORIGIN_PATTERNS`가 필요; 실제 secret 없음; GitGuardian 확인 통과; CI는 `contents: read`로 실행.
- **한계:** Docker image 취약점 scan 없음(Docker Scout는 설치했지만 login하지 않음); Spring Boot 4 major migration은 범위 밖; 이 review는 모든 보안 문제가 없다고 주장하지 않습니다.

## F-15 — 최종 Lighthouse 회귀 확인 (2026-10-03)

**확인한 것:** 당시 `main`(`7da54e8`)이 복원한 과거 방법으로 측정했을 때 PR #36 SSR build와 같은 성능 수준인지. F-01은 SSR 전/후 비교이고, F-15는 그 이후의 회귀 확인입니다.

- **환경:** Windows 11 Pro; `CHROME_PATH`로 Chrome **153.0.8010.53**(실행해서 확인한 `chrome.exe` binary; 대기 중인 154 update는 활성 아님); Lighthouse **13.5.0** CLI; `--throttling-method=devtools`(request latency 562.5 ms, download 1,474.56 kbps, upload 675 kbps, CPU 4×); 기본 mobile 412×823 DPR 1.75; `--chrome-flags="--headless=new"`; 실행마다 storage 초기화; `curl` warm-up 1회; `localhost:3000`의 Docker production stack; 영구 dataset(post 42, 읽을 수 있는 39, 첫 post id 1), 과거 실행과 같은 상태. 실행 warning 0.

| 실행 | Perf | A11y | BP | SEO | FCP | LCP | TBT | CLS | Speed Index |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 1 | 95 | 100 | 100 | 100 | 2,255 | 2,255 | 109 | 0.0003 | 2,709 |
| 2 | 91 | 100 | 100 | 100 | 2,349 | 2,349 | 212 | 0.0003 | 2,757 |
| 3 | 93 | 100 | 100 | 100 | 2,246 | 2,246 | 182 | 0.0003 | 2,642 |
| 4 | 93 | 100 | 100 | 100 | 2,238 | 2,238 | 172 | 0.0003 | 2,639 |
| 5 | 90 | 100 | 100 | 100 | 2,820 | 2,820 | 0 | 0.0003 | 3,181 |
| 6 | 94 | 100 | 100 | 100 | 2,296 | 2,296 | 147 | 0.0003 | 2,697 |

| Median (6개 중 3번째와 4번째의 평균) | Historical (PR #36) | 당시 main | 차이 |
| --- | ---: | ---: | ---: |
| Performance | 93 | 93 | 0 |
| FCP | 2,357 ms | 2,275 ms | −82 ms |
| LCP | 2,357 ms | 2,275 ms | −82 ms |
| TBT | 169.9 ms | 159.2 ms | −10.7 ms |
| CLS | 0.0003 | 0.0003 | 0 |
| Speed Index | 2,764 ms | 2,703 ms | −61 ms |

- LCP element가 과거 실행과 같았습니다(loading shell이 아닌 SSR post preview); font는 13.9–15.0 s에 끝나 LCP보다 훨씬 늦었습니다; 모든 실행에서 browser `/api/posts` 0; `/` First Load JS 121 kB.
- 같은 stack에서 sanity: 좋아요, 검색("촉촉", 9), 상세와 댓글(`limit=2`, `댓글(1,411)`, 더보기)이 console error 0으로 동작했습니다.
- **결과:** `FINAL_RUNTIME_REGRESSION_PASS`, browser version 단서 없음(정확한 Chrome 153 build 사용).

## F-16 — Clean-Clone Release Gate (2026-10-03)

**확인한 것:** project local 상태 없이 GitHub `main`만으로 project가 재현되는지.

- **Source:** 새 directory로 새 `git clone`; branch `main`, HEAD `7da54e8`이 `origin/main`과 같음; 깨끗한 tree. `node_modules`, `.next`, backend `build/`, `.env`, 이전 database가 없었고 복사하지도 않았습니다. 추적되는 `.env`, `env_file`/dotenv 참조, 절대 path가 없고, 모든 Compose와 Spring 설정에 기본값이 있습니다.
- **Toolchain:** Git 2.52.0.windows.1, Java 17.0.17, Node 24.12.0(engine `>=22.12.0`), npm 11.6.2, Docker 29.8.1, Compose 5.5.1.
- **Frontend:** `npm ci`(취약점 0), `npm run typecheck`, Vitest 21 tests / 4 files, production build: 모두 PASS; `/` 6.22 kB, First Load JS 121 kB; `package-lock.json` 변경 없음.
- **Backend:** `build/` 없이 `gradlew.bat clean test`: BUILD SUCCESSFUL, 18 suites, 91 tests, 0 failures; Testcontainers integration 6 suites 31 tests 통과.
- **격리된 Compose:** project `apr-final-release-gate`, port DB 55432, backend 58080, frontend 53000(`DB_PORT`/`BACKEND_PORT`/`FRONTEND_PORT`); `docker compose config -q`와 `up -d --build --wait` 통과; db, backend, frontend healthy; 새 volume `apr-final-release-gate_dbdata`.
- **새 seed vs `seed/seed.json`:** file에서 계산한 기대값 posts 42 / comments 2,076; 실제 posts 42(삭제 3, 읽을 수 있는 39), comments 2,076(삭제 81), `post_likes` 0, `seed_history` `assignment-v1` = 1, Flyway `V1` 적용, 중복 없음.
- **Identity:** seed 최대 post 42 → 만든 post 43; seed 최대 comment 2,076 → 만든 comment 2,077; 충돌 없음.
- **API / SSR:** `/health` 200 `{db: up, status: ok}`; latest(total 39, 첫 id 1), likes(첫 id 25), 검색 "촉촉"(9), post 2 상세(댓글 1,411), 댓글 `limit=2`; `/`는 post 제목 15개, 첫 post 제목과 `/posts/1` link와 함께 200, loading만 있는 HTML 아님; browser에서 hydration 목록 요청 0, 좋아요/검색/상세/댓글 동작, console error 0.
- **연결:** SSR은 Compose 안에서 `http://backend:8080/api`를 썼고; client bundle에는 `http://localhost:58080/api`가 있고 `localhost:8080`은 없었으며; browser는 `localhost:53000`과 `localhost:58080`만 요청했고 이전 stack port로의 요청은 0이었습니다.
- **Full-stack smoke:** release-gate project와 port로 `scripts/ci-full-stack-smoke.sh`: PASS.
- **재생성 후 영속성:** marker post 43(`RELEASE_GATE_171454`, 요청자 `x-user-id: release_gate`), comment 2077, 좋아요 하나를 만들었습니다. 전: posts 43, comments 2,077, `post_likes` 1, `seed_history` 1. **`-v` 없이** `docker compose down` 후 `up -d --wait`: 모든 service healthy, `/health` 200, `/` 200, marker post, 댓글, 좋아요(likeCount 1, isLiked true)가 여전히 있음; posts 43, comments 2,077, `seed_history` 1, seed 범위 그대로(id ≤ 42인 post 42개, id ≤ 2,076인 comment 2,076개): seed 중복 없음. 이후 marker는 API로 삭제했습니다(204, 이후 404).
- **무결성과 정리:** clone의 `git status`는 깨끗하게 유지되었습니다(`diff --check` PASS; ignore된 build 출력만). project 이름을 확인한 뒤 release-gate container, network, volume, image를 제거했고; 원래 repository와 영구 기본 volume은 건드리지 않았습니다.
- **결과:** `CLEAN_CLONE_RELEASE_GATE_VERIFIED`.

---

## F-17 — Commit된 Playwright Core 회귀 (PR #50, 2026-10-03/04)

**확인한 것:** 가장 가치 있는 browser 회귀가 commit되어 production Compose stack을 대상으로 실행되는지, 그리고 자동화 범위를 과장하지 않는지.

- **Repository 상태:** PR #50이 `main`에 `3c902dc9126b2eb2ec0713844b3d7cbec237e80f`로 merge되었습니다. 구현 commit은 `06691f8`, 실패 artifact 후속은 `5891ae2`입니다.
- **설정:** `frontend/playwright.config.ts`는 375×778 Chromium, worker 1, retry 0을 씁니다. screenshot은 실패 시에만 찍고, trace는 실패 시 보관하며, 출력은 `frontend/test-results`로 갑니다.
- **Production 대상:** `full-stack-smoke` job이 `npm ci` 후 Playwright Chromium을 설치하고, `scripts/ci-full-stack-smoke.sh`로 production Compose stack을 build·시작한 뒤, 그 stack을 대상으로 `npm run test:e2e`를 실행합니다.
- **Core flow 1:** seed 기반 검색, likes 정렬, mobile control 배치와 가로 overflow 없음.
- **Core flow 2:** 처음 댓글이 정확히 2개, 더보기가 limit 30으로 이어지고, SPA 재방문이 다시 댓글 2개와 닫힌 더보기 gate로 시작.
- **Core flow 3:** 일회용 post를 만들고, 삭제 dialog의 Tab/Shift+Tab 가두기, Escape와 trigger focus 복원을 확인하며, 확인 전에는 browser DELETE가 전송되지 않음을 증명합니다. `finally` 정리는 production DELETE endpoint를 쓰며, 그 endpoint는 row를 `is_deleted = TRUE`로 soft delete하고, 일회용 CI volume은 job 끝에 제거됩니다.
- **당시 최신 main CI:** GitHub Actions run `37132210952`가 `backend`, `frontend`, `full-stack-smoke`를 통과했고; Playwright는 `3 passed (3.4s)`를 보고했습니다.
- **실패 근거:** job 실패 시 `actions/upload-artifact@v7`가 `frontend/test-results`를 `playwright-failure-artifacts`로 7일간 upload합니다. `continue-on-error: true`와 `if-no-files-found: ignore`가 artifact 처리가 원래 실패를 가리지 못하게 합니다.
- **기존 진단과 정리:** 실패 시 Compose `ps`와 log는 여전히 실행됩니다. `docker compose down -v --remove-orphans`는 여전히 `if: always()`로 실행됩니다.
- **한계:** 이는 선택한 회귀 보호이지 모든 과거 browser 근거의 자동화가 아닙니다. 전체 cursor 순회, 모든 장애 주입과 CRUD/ownership 조합, Firefox/WebKit, visual regression, CI의 Lighthouse는 범위 밖입니다. 공식 Figma 비교 V-081은 자동화하지 않고 수동 audit로 수행했습니다.
- **현재 적용 여부:** 실행 baseline은 `3c902dc`입니다. 이후 현재 `main`까지 `.github/workflows/ci.yml`, `playwright.config.ts`, `tests/e2e/core-flows.spec.ts`는 바뀌지 않았고, 위 wiring(Chromium 설치, `npm run test:e2e`, artifact 7일, `if: always()` 정리)이 그대로 있습니다.
- **결과:** `PLAYWRIGHT_CORE_REGRESSION_VERIFIED`.

---

# 최종 Engineering 검증 요약

| 영역 | 근거 | 상태 |
| --- | --- | --- |
| 현재 `main` | `f3f79fbc61a9215014419f58e91a596ede252a1e`. 아래 baseline들 이후 `docs/`·`README.md` 밖의 변경 없음(`7da54e8` 이후는 F-17 Playwright/CI 구성만) | — |
| V-081 실행 baseline | `04dd668ae9d80d899edd8d363b42db5096bf4ac8`, V-081 audit base | — |
| Frontend test | Vitest 21 tests / 4 files | PASS |
| Backend test | 91 tests / 18 suites, PostgreSQL Testcontainers test 31개 포함 | PASS |
| PostgreSQL 정확성 | F-07 (실제 Flyway, seed, repository, 주입 bug 민감도) | VERIFIED |
| Frontend cache 일관성 | F-08, 148/148 | VERIFIED |
| Mutation 동시성 | F-09 (8 tests, `FOR UPDATE` 민감도) | VERIFIED |
| Edge / 실패 / 복구 | F-12 | VERIFIED |
| Full-stack CI | F-13 smoke + F-17 Playwright (job 3개) | `main`에서 PASS |
| Browser 회귀 | F-17, Chromium 375×778, core flow 3개 | PASS |
| 공식 디자인 fidelity | V-081, 공식 export 12개 + 수정 state | PASS |
| Dependency audit | `npm audit` 0 / `--omit=dev` 0, Boot 3.5.16 | PASS |
| 성능 회귀 | F-15: Perf median 93, LCP median 2,275 ms, A11y/BP/SEO 100 | PASS |
| 재현성 | F-16 clean clone | VERIFIED |

Engineering 검증 범위: 위에 기록한 조건(Windows 개발 host 하나, Docker Compose, emulate한 mobile 기기, 일회용 dataset과 seed dataset)에서 **VERIFIED**입니다. V-081 공식 디자인 fidelity는 최종 `375×778` Chromium 비교에서 `PASS`입니다. 모든 환경이나 production 조건에서의 검증을 주장하지 않습니다.

---

# 최종 Sign-Off

## 원래 sign-off 기준과 상태

project 시작 시 정한 기준을 현재 상태와 함께 남깁니다.

| 원래 기준 | 현재 상태 |
| --- | --- |
| `V-001`, `V-002`, `V-003`, `V-004`가 통과한다. | 충족 (`PASS`; F-16이 clean clone에서 기동, seed, idempotency를 다시 확인) |
| 적용되는 동작 시나리오가 모두 통과한다. | 충족 (`V-010`–`V-071`, `V-080` `PASS`) |
| 필수 요구사항이 모두 `VERIFIED`다. | 부분 충족. `docs/REQUIREMENTS.md`의 요구사항 132개 중 131개가 `VERIFIED`입니다(`UI-004`는 V-081로 `VERIFIED`). `SUB-003`은 2026-10-05 manual GitHub Settings verification에서 collaborator invitation 생성이 확인되어 `VERIFIED`이며, recipient acceptance는 `Pending Invite` 상태입니다. repository URL 메일 제출 단계 `SUB-006`만 `TODO`입니다. |
| 이제 사용할 수 있는 공식 디자인 source와 시각 구현을 비교한 뒤 `V-081`이 통과한다. | 충족 (`PASS`, baseline `04dd668`, 이후 UI 변경 없음; [`DESIGN_COMPARISON.md`](DESIGN_COMPARISON.md) 참고) |
| README가 최종 구현을 정확히 반영한다. | `V-090`은 2026-10-01의 historical evidence입니다. 최종 문서 정리가 PR #50 Playwright/CI 동작을 포함해 README와 지원 문서를 `3c902dc` 기준으로 다시 audit했고, 그 이후 현재 `main`까지 production/config code 변경은 없습니다 |

## 검증한 engineering gate

- Release gate `V-001`–`V-004`, 모든 동작 시나리오 `V-010`–`V-071`와 `V-080`: `PASS`(위의 날짜별 근거, 동작이 바뀐 곳은 이후 section이 다시 확인).
- `V-081`: `PASS`; 공식 file/node mapping을 다시 확인했고, 보존된 공식 export 12개 전체와 수정 form을 `375×778`에서 비교했습니다.
- `V-090` README 정확성은 그 날짜의 historical evidence로 남습니다. 현재 문서 snapshot은 `3c902dc` 기준으로 다시 audit되었고, F-17이 이후의 browser 회귀 범위를 기록합니다.
- PostgreSQL 정확성(F-07), frontend cache 일관성(F-08), 동시성 불변식(F-09), edge/실패 처리(F-12), SSR과 성능 회귀(F-01, F-02, F-15), dependency 보안(F-14), full-stack CI(F-13), clean-clone 재현성(F-16).

## 완료 상태

- **Engineering 검증:** 완료(`ENGINEERING_VERIFICATION_COMPLETE`). 제품 동작, UI, engineering 검증은 위 조건에서 끝났습니다.
- **과제 제출:** 아직 완료가 아닙니다(`PENDING_EXTERNAL_SUBMISSION_STEPS`). collaborator invitation 생성은 `SUB-003` `VERIFIED`로 확인되었지만 recipient acceptance는 pending이고, repository 밖에서 수행할 최종 URL 메일 제출 `SUB-006`이 `TODO`로 남아 있습니다.

## 수용한 trade-off

- likes 정렬 순회는 요청 간 snapshot 일관성이 없습니다: row가 중복되거나(UI가 숨김) 새 순회 전까지 누락될 수 있습니다(F-10).
- READ COMMITTED에서 동시 write가 있으면 page와 그 `total`이 1 다를 수 있습니다(F-11).
- 2 MB Pretendard file을 유지합니다: LCP 경로에 있지 않고, 측정한 subset 대안은 실제 LCP를 더 느리게 했습니다(F-01).
- 현재 규모에서 댓글 virtualization 없음(F-05).
- Spring Boot 4 major upgrade 없음(F-14).
- `main`에 branch protection을 강제하지 않습니다; CI gate는 있지만 merge를 기술적으로 막지 않습니다(F-13).
- 같은 post의 좋아요는 post row에서 직렬화됩니다(F-09); SSR page는 멈춘 backend에서 fallback 전까지 최대 2 s 기다립니다(F-02).

## 남은 항목 / 주장하지 않는 것

- `SUB-003`은 `VERIFIED`입니다. 2026-10-05 manual GitHub Settings verification에서 `APRCORPORATION` collaborator invitation 생성이 확인되었고, recipient acceptance는 `Pending Invite` / `Awaiting APRCORPORATION's response` 상태입니다. acceptance 전이므로 active collaborator access가 이미 활성화되었다고 주장하지 않습니다.
- `SUB-006`은 `TODO`입니다. 최종 repository URL 메일 제출은 아직 수행하지 않았습니다.
- production traffic과 부하 동작, production observability, 실제 인증(요청자 header/cookie는 과제 계약), Docker image 취약점 scan, 배포/CD는 검증하지 않았습니다.
- deadlock이 없음은 일반적으로 증명되지 않았습니다. 동시성 test에서 deadlock은 관찰되지 않았고 명백한 lock cycle도 찾지 못했습니다(F-09).
- 성능 수치는 emulate한 기기와 host 하나에서 나왔습니다. 실제 기기와 HTTP/2 production network는 측정하지 않았습니다.
