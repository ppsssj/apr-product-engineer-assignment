# Acceptance Verification

This document defines executable acceptance scenarios for the APR assignment.

Do not mark a requirement `VERIFIED` only because the code appears correct. Run the relevant scenario and record evidence.

## Result Values

- `NOT_RUN`
- `PASS`
- `FAIL`
- `BLOCKED`

## How to Read This Document (Historical vs Current)

This is the detailed master verification record. It keeps every phase's
evidence with its date, including values that later changed.

- Sections and headings with a date (for example "P4 … — 2026-09-29" or
  "P8-B Current-Candidate Evidence — 2026-09-30") describe the code **as it
  was on that date**. "Current candidate" in those headings means the candidate
  of that phase, not today's `main`.
- Values such as the post page size `10`, comment continuation `20`, the
  `rootMargin` `200px` on the post list, the 30 s SSR seed freshness, the 1.5 s
  SSR timeout, TanStack Query's default retries (4 attempts), and backend test
  counts `26/49/51/60/83` are correct for their dates and are kept as
  historical evidence. Where a later decision replaced them, the section says
  so.
- The current values are in "Final Configuration Snapshot" below, and the
  final engineering evidence is in "Final Engineering Verification —
  2026-10-02/03" near the end.

## Final Configuration Snapshot — `main` @ `7da54e8`

Reference for the current values only; it does not replace the evidence
below. Each value was read from the code on
`7da54e8a361d2b8212a46e8b9e8091b8240ce3fa`.

**Frontend**

| Item | Current value | Source |
| --- | --- | --- |
| Next.js | 15.5.27 | `frontend/package.json` |
| React / React DOM | 19.1.9 / 19.1.9 | `frontend/package.json` |
| Node engine | `>=22.12.0` | `frontend/package.json` `engines` |
| Post list page size (SSR first page and continuation) | 15 / 15 | `features/post-list/postListConfig.ts` `POST_LIST_PAGE_SIZE` |
| Comments: initial / continuation | 2 / 30 | `features/post-detail/useComments.ts` |
| Comment query cache | `gcTime: 0` (per visit) | `useComments.ts` |
| Post-list prefetch distance | `rootMargin` `0px 0px 500px 0px` | `postListConfig.ts` `POST_LIST_PREFETCH_ROOT_MARGIN` |
| Shared infinite-scroll default (used by comments) | `rootMargin` `0px 0px 200px 0px` | `lib/hooks/useInfiniteScrollTrigger.ts` |
| Search debounce | 300 ms | `PostListScreen.tsx` `SEARCH_DEBOUNCE_MS` |
| First-page SSR | default view only (`latest`, no search) | `app/page.tsx`, `lib/api/server.ts` |
| SSR seed freshness | 5 s (`staleTime` only while `dataUpdateCount === 0`, then 0) | `usePostList.ts` `SEED_FRESH_MS = 5_000` |
| SSR backend timeout | 2,000 ms, then `null` → client fetch | `lib/api/server.ts` `SERVER_FETCH_TIMEOUT_MS = 2000` |
| Read query retry | `ApiError` 4xx: no retry; network, 5xx, unknown: one retry (default 1 s delay) | `lib/query/retryPolicy.ts`, `app/providers.tsx` |
| Mutation retry | 0 (TanStack Query default; no mutation sets `retry`) | — |
| Font | Pretendard official variable font (`PretendardVariable.woff2`, 2,057,688 B) retained | `public/fonts`, `app/globals.css` |
| Frontend tests | Vitest, 4 files / 21 tests | `lib/**/*.test.ts` |

**Backend**

| Item | Current value | Source |
| --- | --- | --- |
| Java | 17 (Gradle toolchain) | `backend/build.gradle` |
| Spring Boot | 3.5.16 | `backend/build.gradle` |
| Database | PostgreSQL 16 (`postgres:16-alpine`) | `docker-compose.yml` |
| Schema | Flyway `V1__create_community_schema.sql` | `backend/src/main/resources/db/migration` |
| Data access | Spring `JdbcClient` repositories | `persistence/*Repository.java` |
| Test database | Testcontainers PostgreSQL, version override `1.21.4` | `build.gradle` `ext['testcontainers.version']` |
| Deletion | soft delete (`is_deleted`) | `V1` |
| Pagination | keyset `(created_at, id)` row-value; likes `(effective likes, created_at, id)` | repositories |
| Mutation locking | `SELECT … FOR UPDATE` on the readable target before ownership checks | `findReadableMutationTargetForUpdate` |
| Backend tests | 18 suites / 91 tests (6 PostgreSQL integration suites / 31 tests) | `gradlew clean test` |

**Security / CI**

| Item | Current value |
| --- | --- |
| PostCSS | root `overrides` `8.5.28` (replaces the `8.4.31` that `next@15.5.27` requests) |
| `npm audit` / `npm audit --omit=dev` | 0 / 0 |
| CI jobs | `backend`, `frontend`, `full-stack-smoke`; `permissions: contents: read` |
| Branch protection on `main` | not enabled (GitHub API `protected: false`) |

---

# Release Gates

The assignment is not release-ready unless every gate below passes.

## V-001 — Full Stack Boot

**Requirements:** `ENV-006`, `ENV-007`, `ENV-008`

### Steps

1. Stop previous assignment containers.
2. Start with:

```bash
docker compose up --build
```

3. Verify backend health:

```bash
curl -i http://localhost:8080/health
```

4. Open frontend at `http://localhost:3000`.

### Expected

- DB starts.
- Backend starts.
- Frontend starts.
- `/health` responds HTTP 200.
- Frontend is reachable.

### Execution Evidence — 2026-09-29

- Clean-volume `docker compose up --build` completed successfully.
- `db`, `backend`, and `frontend` reached `healthy`.
- `GET /health` returned HTTP 200.
- Flyway migration `V1` completed successfully.

### P8-A Current-Main Evidence — 2026-09-30

- The final default-port `docker compose up --build -d` build completed and
  started `db` before `backend`, with frontend gated on backend health. All
  three services became healthy; `/health` returned HTTP 200 with
  `{"status":"ok","db":"up"}`, `/api/posts?limit=1` returned total 39,
  and the frontend root returned HTTP 200.
- Three fresh-volume observations before the readiness fix showed health 200
  before seed-backed data was reliably ready. The first directly observed
  `/api/posts` total 0 after health 200; see the dedicated investigation below.
- After the fix, three additional fresh-volume starts kept `/health` at 503
  until seed readiness had committed. Every first API response observed after
  health 200 had total 39. Docker therefore could not report a healthy backend
  while the readable seed was absent.

### P8-B Current-Candidate Note — 2026-09-30

- P8-B changed no product code. The rebuilt default-port stack stayed healthy
  with `/health` 200 and total 39. The P8-A clean-volume evidence still applies
  and was not rerun destructively.

**Result:** `PASS`

---

## V-002 — Alternate Port Boot

**Requirements:** `ENV-009`, `ENV-010`, `ENV-011`

### Steps

Start with non-default ports, for example:

```bash
FRONTEND_PORT=13000 BACKEND_PORT=18080 DB_PORT=25432 docker compose up --build
```

Verify:

```bash
curl -i http://localhost:18080/health
```

Open frontend at `http://localhost:13000`.

### Expected

- All services start successfully on the supplied ports.
- Frontend communicates with backend successfully.

### P8-A Current-Main Evidence — 2026-09-30

- With PowerShell-scoped `FRONTEND_PORT=13000`, `BACKEND_PORT=18080`, and
  `DB_PORT=25432`, `docker compose up --build -d` produced three healthy
  services bound to exactly those host ports. `GET http://localhost:18080/health`
  returned 200 and the alternate API returned total 39.
- Chromium loaded `http://localhost:13000/` with 10 list rows. Its actual
  browser resource was `http://localhost:18080/api/posts?limit=10`; no request
  used `localhost:8080`. The temporary PowerShell port variables were removed
  after the run and a default-port stack was rebuilt successfully.

### P8-B Current-Candidate Note — 2026-09-30

- There is no Compose, port, or frontend API-base change since P8-A, so the
  alternate-port evidence still applies. It was not rerun.

**Result:** `PASS`

---

## V-003 — Seed Idempotency

**Requirements fully verified by this scenario:** `SEED-001`, `SEED-002`, `SEED-005`, `SEED-006`, `SEED-007`, `SEED-011`, `SEED-012`

**Persistence evidence in this scenario only:** `SEED-003`, `SEED-004`, `SEED-008`, `SEED-009`, `SEED-010` (their later API evidence is recorded in the P2A scenarios below)

### Steps

1. Start from a clean DB volume.
2. Start application and record post/comment row counts.
3. Stop application without deleting DB volume.
4. Start application again.
5. Record row counts again.
6. Before any like toggle, confirm `post_likes` is empty, so no requester has an initial like relation.
7. Parse `seed/seed.json` as UTF-8 with Node.js and compare every seeded post’s `likeCount` with `posts.base_like_count`.
8. Compare the full deleted post/comment ID sets and every post's non-deleted comment count between the parsed seed and the database.

### Expected

- Seed rows exist after first startup.
- IDs match seed IDs.
- Already-deleted seed IDs remain persisted as deleted.
- Second startup does not duplicate seed rows.
- Database counts of non-deleted comments remain stable.
- The initial `post_likes` persistence state is empty.
- Every seeded post begins with its supplied `likeCount`.
- Database deleted-ID sets exactly match `deletedPostIds` and `deletedCommentIds`.
- Per-post readable comment counts exclude every ID in `deletedCommentIds`.

### Execution Evidence — 2026-09-29

- Initial clean-volume state: 42 posts, 2,076 comments, 3 deleted posts, 81 deleted comments, 0 `post_likes`, and one `seed_history` row for `assignment-v1`.
- Preserved ranges: post IDs `1..42`; comment IDs `1..2076`.
- Generated identity checks produced post ID `43` and comment ID `2077`, without conflicting with preserved seed IDs.
- Persistent-volume backend restart retained 42 posts, 2,076 comments, 0 `post_likes`, and one `assignment-v1` seed-history row.
- A modified existing seed row retained its modified value across the backend restart, proving startup did not overwrite existing data; the value was restored after verification.
- Node.js parsed the actual `seed/seed.json` as UTF-8 (`JSON.parse`: 42 posts, 2,076 comments). It compared complete, ordered PostgreSQL results rather than samples:
  - `seed.deletedPostIds` exactly equaled `SELECT id FROM posts WHERE is_deleted = TRUE ORDER BY id` (3 of 3 IDs).
  - `seed.deletedCommentIds` exactly equaled `SELECT id FROM comments WHERE is_deleted = TRUE ORDER BY id` (81 of 81 IDs).
  - All 42 `seed.posts[].likeCount` values equaled their corresponding `posts.base_like_count` values (42 of 42 rows).
  - For all 42 posts, the count of seed comments excluding `deletedCommentIds` equaled the database count where `comments.is_deleted = FALSE` (42 of 42 posts).
- `backend/gradlew.bat test --no-daemon` completed with `BUILD SUCCESSFUL`.

### P8-A Current-Main Evidence — 2026-09-30

- On a fresh P8-A volume, Node.js parsed the actual UTF-8 `seed/seed.json` and
  compared full database results: 42 posts, 2,076 comments, 3 deleted posts,
  81 deleted comments, 0 likes, and exactly one `assignment-v1` history row.
  All post IDs and comment IDs, both complete deleted-ID sets, all 42 base
  like counts, and all 42 per-post readable-comment counts matched the source;
  the readable post API total was 39.
- P8-B note: the P8-B pre-test database was still `42|3|2076|81|0|1` and the
  seed and seed runner were unchanged. P8-B then added only runtime-generated,
  soft-deleted acceptance rows (see "P8-B Full Behavioral Acceptance").
- After `docker compose stop` then `docker compose up -d` without deleting the
  volume, the state remained `42|2076|3|81|0|1` and the readable total remained
  39. No seed row, deleted marker, like row, or seed-history marker duplicated.

### Acceptance Boundary

- This `PASS` records the completed P1 persistence/seed verification; it is not final API acceptance for every composite `SEED-*` requirement.
- P2A subsequently verified `SEED-003` through V-021 and `SEED-004`, `SEED-008`, and `SEED-009` through V-065.
- P2A verified the initial-read portion of `SEED-010`; P2B subsequently completed the requester-specific toggle evidence in V-050.

**Result:** `PASS`

---

## V-004 — Immutable Starter Contracts

**Requirements:** `START-001`, `ENV-007`, `ENV-008`

### Steps

- Compare `frontend/app/dev-user/` against the starter version.
- Call `/health`.
- Switch users through `/dev-user` and confirm `x-user-id` cookie behavior still works.

### Expected

- `frontend/app/dev-user/` is unchanged.
- `/health` still returns 200.
- User switch behavior still works.

### P8-A Current-Main Evidence — 2026-09-30

- `git rev-list --max-parents=0 HEAD` identified starter commit
  `38a975520f5086fe6a6be98058e7d659bfcca8c9`; its diff to current HEAD for
  `frontend/app/dev-user/page.tsx` is empty, and its file history contains only
  that starter commit.
- `/health` remains the same path and returned HTTP 200 only after readiness.
  In Chromium, `/dev-user` selected `mintcream`, set
  `x-user-id=mintcream`, and the actual frontend API requests carried that
  cookie. Post 1 was liked as `mintcream`, read unliked as `apr_tester`, then
  read liked again as `mintcream`; the relation was toggled off and the cookie
  restored to `apr_tester`, leaving `post_likes` at zero.

### P8-B Current-Candidate Note — 2026-09-30

- `git diff 38a9755 HEAD` is still empty for `frontend/app/dev-user/` (and
  `CorsConfig.java`). `/dev-user` switching to `mintcream`, `Roidl_08`, and
  `apr_tester` drove requester-specific like, comment, and post-ownership
  behavior in the browser.

**Result:** `PASS`

---

## P8-A First-Boot Readiness Investigation — 2026-09-30

`T1` is first TCP acceptance on backend port 8080; `T2` is first `/health`
HTTP 200; `T3` is first successful list API response; `T4` is first observed
list total 39; `T5` is the matching DB confirmation of
`42|2076|3|81|0|1`. Timings are seconds from the start of each close polling
window; the initial image-build time is intentionally outside those values.

- Before the fix, three clean volumes showed T2 before T4: run 1
  `65.380/71.748/72.016/72.860/72.860`, including an actual API total 0 at T3;
  run 2 `.032/.235/.371/.371/.371`; run 3 `.033/.229/.359/.359/.359`.
  `HealthController` checked only a DB connection while transactional
  `SeedInitializer` ran as an `ApplicationRunner`, so the first run proved a
  release blocker rather than timing noise.
- The fix adds `SeedReadiness`: `SeedInitializer` registers readiness with the
  current transaction's `afterCommit`, and `/health` returns 503 until that
  flag is true. It does not change the path, eventual 200 response, seed data,
  or Docker healthcheck contract.
- After the fix, three more clean volumes observed no health-200/empty-seed
  combination: run 1 `10.099/17.071/16.112/17.104/17.104` (the internal API
  was still reachable with total 0 before health; the first post-health API
  had total 39); run 2 `.033/.233/.366/.366/.366`; run 3
  `.031/.231/.377/.377/.377`. Docker health could only become healthy after
  seed availability.

**Classification:** `BENIGN_INTERNAL_GAP`

### P8-A Static Check Note

- `backend/gradlew.bat clean test --no-daemon`, `npm run typecheck`,
  `npm run build`, and `git diff --check` passed. The local
  `frontend/.next` target was resolved but its explicit recursive removal was
  blocked by this execution environment's safety policy, so the local npm
  build is not claimed as clean. The frontend Docker build context excludes
  `.next` via `frontend/.dockerignore` and successfully performed the
  production build used for the runtime images.

---

## Post Discovery (search + sort) Evidence — 2026-09-30

- **Scope:** branch `feat/post-list-search-sort`, stacked on the P8-C branch.
  This is a `PRODUCT_DECISION` beyond the assignment; the design is in
  `docs/ARCHITECTURE.md` "Post Discovery Update" and `docs/UI_SPEC.md` §13.
  Its temporary verifiers stayed in the session scratchpad.
- **Backend tests:** `gradlew clean test` passed 60 tests, 0 failures (51
  before). New tests cover:
  - default sort and query normalization;
  - `INVALID_SORT` and `INVALID_QUERY`;
  - cross-sort cursor rejection;
  - `PostLikeCursorCodec` round-trip and malformed payloads;
  - LIKE escaping.
- **API against PostgreSQL, 31/31.** It seeded a marker dataset (like ties,
  `%`/`_` decoys, a soft-deleted match, a deleted comment). Results:
  - Default equals `sort=latest` and PostgreSQL newest-first order.
  - Full likes traversals match `(base + likes) DESC, created_at DESC, id DESC`.
  - Search at `limit=2` matched PostgreSQL in both sorts with no duplicate or
    skip, and ties were ordered by created_at then id.
  - The filtered `total` equals the readable matches.
  - `100%` and `a_b` match literally (decoys `1000` and `axb` excluded), and
    `\` is safe. Matching is case-insensitive.
  - Content-only matches are found and soft-deleted posts are excluded.
  - Every cursor misuse returns 400 `INVALID_CURSOR`. `INVALID_LIMIT`,
    `INVALID_SORT` and `INVALID_QUERY` (over 100 code points) are enforced.
  - Requester `isLiked` and `commentCount` are correct.
  - Query plan: one statement for the likes page with a single `post_likes`
    aggregate, and the comment subplan ran `loops=11` for an 11-row page (no
    N+1).
- **Browser at a true 375×778, 29/29:**
  - The initial load is exactly one list request.
  - The toolbar is one 44px row; when scrolled it is sticky at top 0 and the
    title scrolls away. No overflow in either mode.
  - A sort switch starts a new first page. Likes infinite scroll equals the
    API, and no latest/likes cursor crossed lists.
  - The viewport returns under the toolbar after a sort change from a deep
    scroll.
  - Typing 5 characters sent one debounced request. A slow older search was
    cancelled (`net::ERR_ABORTED`) and did not overwrite the newer results.
  - Title, content and no-result searches work; clear, Escape and ← close
    work; the sort survives closing.
  - Search with likes sorts correctly, and a like moved a post up after
    returning.
  - Create, edit-out and delete refreshed search membership after returning.
  - No page errors and no failed responses.
- **Regression:** 160/160 across list, like/requester switch, comments,
  comment mutations, post CRUD, E2E, the post-action popover, and comment
  identity with the delete dialog.
- **Static:**
  - `npm run typecheck`, a clean `npm run build`, and `git diff --check`
    pass.
  - No dependency, schema, Compose, seed, or README change.
- **Data:** all temporary likes were reverted, and temporary posts and
  comments were soft-deleted.
- **V-081:** stays `NOT_RUN`; the toolbar is an intentional deviation.

---

## CI, Read Cancellation, and Dialog Focus Evidence — 2026-10-01

Browser checks used headless Chromium at 375×778, driven by temporary
Playwright scripts that stayed in the session scratchpad. Each branch build ran
with `next start` against the Compose backend.

- **CI (`.github/workflows/ci.yml`, PR #25):**
  - Runs on pull requests to `main` and pushes to `main`, with read-only
    `contents` permission.
  - `backend` job: Ubuntu, Temurin 17, `./gradlew test` with the repository
    wrapper. The job runs `chmod +x` first because `backend/gradlew` is
    tracked as `100644`.
  - `frontend` job: Ubuntu, Node.js 22, `npm ci` → `npm run typecheck` →
    `npm run build`.
  - No PostgreSQL service or Compose: the 12 backend test classes start no
    Spring context and need no database (`HealthControllerTest` mocks
    `DataSource`).
  - *(Historical — superseded. As of `7da54e8`: the `frontend` job also runs
    `npm test` (PR #43); the `backend` job runs 91 tests including 31
    PostgreSQL Testcontainers integration tests (PR #42, #44), which need the
    runner's Docker daemon; and a third `full-stack-smoke` job builds and
    checks the production Compose stack (PR #46). See "Final Engineering
    Verification".)*
  - Before push: `gradlew.bat test --rerun-tasks` ran 60 tests with 0
    failures, and `npm ci`, typecheck and build exited 0.
  - Actions: the first PR run (36852207676) passed both jobs. The `main` push
    runs after the PR #25, #26 and #27 merges also succeeded (36852647774,
    36855352902, 36856028261).
- **Read cancellation (PR #26):** `getPost` and `getComments` accept an
  optional `AbortSignal`, and `usePostDetail`/`useComments` pass the query
  function's signal, as `usePostList` already did.
  - Method: the target GET to post 2 was held 4 s by route interception. The
    test navigated from the list to `/posts/2` and went back after 0.7 s. A
    page marker confirmed client-side navigation, not a document reload.
  - Detail `GET /api/posts/2`: the fetch carried a signal, the signal fired,
    the fetch rejected with `AbortError`, and Chromium reported
    `net::ERR_ABORTED`. The detail query was left `pending`/`idle` with no
    error. No alert, console error or page error.
  - Initial comments `GET /api/posts/2/comments?limit=2`: the same result; the
    detail query stayed `success`.
  - Control: on the pre-change build, the same steps sent both requests
    without a signal and both completed with 200.
  - Regression: the list requested `limit=15&sort=latest`. Comments requested
    `limit=2`, then `limit=30` with a cursor after More. Like toggles were
    sent without a signal and returned 200 (net zero). A missing post sent one
    request, got 404 and was not retried.
  - Not browser-tested: leaving while a continuation page is loading.
- **ConfirmDialog focus containment (audit at `754abc4`, fix PR #27):**
  - Before:
    - In both the post and comment dialogs, Tab past `예` and Shift+Tab before
      `취소` reached the controls behind: back, post menu, like, comment
      delete, More and the comment input.
    - While a delete was pending, both buttons were disabled and Chromium
      moved focus to `<body>`. After the failed DELETE, the first Tab reached
      the back button.
  - After, 44/44 checks:
    - Focus starts on `취소` whether the dialog is opened by keyboard or
      mouse.
    - Tab ×6 and Shift+Tab ×6 alternate only between `예` and `취소`.
    - Escape, `취소` and the scrim close the dialog and return focus to
      `게시글 메뉴` or to that comment's `삭제`.
    - While pending: both buttons are disabled, Escape and the scrim are
      ignored, and 4 Tab plus 4 Shift+Tab never reach the background.
    - After a held DELETE was answered with 500: the error text appears, the
      buttons re-enable, focus is on `취소` and the next Tab goes to `예`.
  - Data: every DELETE was intercepted and answered by the script. Post 5 and
    comment 1527 still existed afterwards.
  - Background scroll is not locked (`scrollY` 0 → 800 on a wheel over the
    scrim). It is classified as polish and left out of scope.
- **Static:** `npm run typecheck`, `npm run build` and `git diff --check`
  passed for each change, and each PR's CI passed.

---

## P8-B Full Behavioral Acceptance — 2026-09-30

- **Candidate:** `feat/p8b-full-acceptance` at `79ebd67` (P8-A merge). No product
  code changed during P8-B. The Compose production stack was rebuilt from this
  commit; `db`, `backend`, and `frontend` were healthy and `/health` returned
  `{"status":"ok","db":"up"}`.
- **Pre-test baseline:** posts 42 (3 deleted), comments 2,076 (81 deleted),
  `post_likes` 0, one `assignment-v1` seed-history row; readable post total 39.
- **Method:** a temporary Node.js API verifier (108 checks, each state change
  cross-checked with `psql`) and a Playwright Chromium verifier with a true
  `375 × 778` viewport (`innerWidth` and document `scrollWidth` 375), run as
  independent sections plus one continuous end-to-end path. The scripts stayed in
  the session scratchpad and are not repository artifacts.

### Requester (V-010, V-011, V-012)

- A temporary like by `p8b_req_a` on post 13 (`user_id = p8b_req_a` in
  `post_likes`) read `isLiked: true` with only the header, true with only the
  cookie, and false with neither. With both, the header won in each direction
  (header other + cookie A → false; header A + cookie other → true). A blank
  header fell back to the cookie.
- Default: a toggle with no header or cookie stored `user_id = apr_tester`, was
  visible to header `apr_tester`, and not to another requester. A post created
  without identity had author `apr_tester` and was deletable with cookie
  `apr_tester`.
- Browser: `/dev-user` → `mintcream` made the product request carry
  `x-user-id=mintcream`; like state, comment authorship, and delete affordances
  followed that requester.

### List / pagination (V-020, V-021)

- API traversals at limits 7, 10, and 50 (6/4/1 pages, sizes `7/7/7/7/7/4`,
  `10/10/10/9`, `39`) exactly matched PostgreSQL
  `ORDER BY created_at DESC, id DESC` over readable rows. `total` was 39 on
  every page, there were no duplicates or skips, and the final `nextCursor` was
  `null`. Seed-deleted posts 9, 30, and 31 were absent. The default limit
  returned 20 rows.
- Invalid input: `limit` 0, 51, and -1 → 400 `INVALID_LIMIT`; `limit=abc` →
  400 `INVALID_PARAMETER`; malformed or empty `cursor` → 400
  `INVALID_CURSOR` on posts and comments.
- Browser: exactly one initial `limit=10` request; scrolling appended
  `10/10/10/9` rows through four requests chained by `nextCursor`. The 39 rows
  were unique and in API order, and further scrolling after the final page sent
  no request.

### Response shapes (V-070, V-071)

- List: `{ items, total, nextCursor }`. Each item has exactly `id`, `title`,
  `content`, `likeCount`, `commentCount`, `isLiked`, `author`, and `createdAt`,
  and `createdAt` is ISO-8601 UTC (sample `2026-08-30T13:55:54Z`).
- Detail: the same single object without an envelope, byte-identical to the
  list item. PATCH returned the complete updated single object with unchanged
  `author` and `createdAt`.
- Comments: `{ items, total, nextCursor }`, with items of exactly `id`,
  `postId`, `content`, `author`, and `createdAt`.
- Like toggle: exactly `{ isLiked, likeCount }`.

### Likes (V-050)

- API on post 13: A and B both started false with no like rows. The count went
  `856 → 857 → 858 → 857 → 856` (A on, B on, A off, B off). B stayed false
  while A was on, B stayed true after A went off, the list reflected A's
  `isLiked` and count, and the run ended with 0 like rows.
- Browser: the detail heart (`aria-pressed`) and count updated with exactly one
  toggle request. Back on the list, the row's heart showed the liked accent with
  the synced count. After switching to `mintcream` the post read unliked at
  +1; mintcream's toggle made +2, and the list reflected it. Both requesters
  toggled back, and reload and the database matched the original 856 with 0
  rows.

### Comment reads (V-060, V-061, V-065)

- API: posts 10 (0), 24 (1), 1 (2), 3 (3), and 4 (27) traversed `limit=2` and
  then `limit=20`, exactly matching PostgreSQL readable IDs and order, with
  matching `total` and detail `commentCount`. Post 2 (1,411) was fully
  traversed twice (`2` then `50`: 30 pages; `2` then `20`: 72 pages) with no
  duplicate or skip and a null final cursor.
- Browser: each post sent one `limit=2` request without a cursor and showed at
  most 2 comments in API order. More appeared only for 3 and 27. Scrolling
  before More sent nothing. After More, `limit=20` requests chained by
  `nextCursor` rendered all 3 and all 27 comments, More disappeared, and no
  request followed the null cursor.
- Revisit: returning to post 4 in the same session (no reload, verified by a
  window marker) made one fresh `limit=2` request, 0 continuations, and showed 2
  comments with More.
- Post 2 in the browser: 2 visible with More, then bounded `limit=20` pages
  (122 rows after 7 requests).

### Comment mutations (V-062, V-063, V-064)

- Browser as `apr_tester` on post 1 (2 comments): one POST put the new comment
  first with author `apr_tester`, the heading went to 3, More appeared, and the
  list count went +1.
- `삭제` appeared only on the requester's own comments. Cancel sent no DELETE;
  confirm sent exactly one. The comment disappeared, the heading and list count
  returned to 2, the row stayed with `is_deleted = true`, and a repeat DELETE
  returned 404.
- `mintcream` created a comment through the UI (author `mintcream`, `삭제`
  only on it). `apr_tester` saw it without `삭제` and a direct `apr_tester`
  DELETE returned 403 with the row unchanged. mintcream then deleted it through
  the UI.
- API: a post owner's DELETE of another requester's comment returned 403.
  Deleted comments were excluded from reads and counts, and a repeat by either
  owner or non-owner returned 404 `COMMENT_NOT_FOUND`.

### Post validation and CRUD (V-030, V-031, V-032, V-033, V-040, V-041)

- **Backend:** 400 JSON errors for empty, missing, whitespace-only, and
  U+3000-only titles; empty, missing, and whitespace-only content; and 21 ASCII,
  21 emoji, or 21 Korean code points. No row was created. 20 ASCII, 20 emoji
  (40 UTF-16 units), and 20 Korean code points returned 201. PATCH rejected a
  21-emoji title.
- **Frontend:** the same invalid cases kept `작성 완료` disabled, and a forced
  `requestSubmit()` sent 0 requests. The three 20-code-point titles enabled it,
  and the over-length hint appeared.
- **Create (browser):** the FAB opened `/posts/new`. A 20-code-point title
  (21 UTF-16 units) with multiline content made one POST (201). The app
  returned to `/` with the post first and the list total +1. The
  `게시글이 등록되었습니다.` toast appeared once and expired. The server author
  was `apr_tester` with 0 likes, 0 comments, and `isLiked: false`, and the
  detail matched.
- **Edit (browser):** the own-post menu opened the sheet, and `수정하기`
  opened the form with the exact title and content prefilled. One PATCH
  updated the detail, the list cache showed the edited title, and reload and
  the API confirmed persistence with `createdAt` unchanged.
- **Delete (browser):** `삭제하기` replaced the sheet with the dialog, and
  confirm made one DELETE and returned to `/` without the row. The total went
  back to 39, a direct visit showed `게시글을 찾을 수 없습니다.`, the row stayed
  with `is_deleted = true`, and a repeat DELETE returned 404.
- **Ownership:** post 2 (Roidl_08) showed no menu to `apr_tester`, and
  `/posts/2/edit` showed `이 게시글을 수정할 권한이 없습니다.` with no form.
  Switching to `Roidl_08` showed the menu. Non-owner PATCH and DELETE returned
  403 with the post unchanged and readable.

### Soft delete (V-040, V-063, V-065)

- A post by `p8b_owner` had comments by owner and non-owner plus two likes.
  While readable, the non-owner got 403 on PATCH and DELETE.
- After the owner's DELETE (204):
  - The post row stayed with `is_deleted = true`, both child comments stayed
    `is_deleted = false`, and both like rows remained.
  - The list excluded the post and the total dropped by one.
  - Every product route returned 404 `POST_NOT_FOUND` or `COMMENT_NOT_FOUND`
    for owner and non-owner alike: detail, comments read, PATCH, repeat DELETE,
    like toggle, comment create, and each child comment delete by its own
    author.
  - None of those 404s changed the database.
- This matches the documented model: readable post = not deleted; readable
  comment = not deleted and readable parent; effective like = readable parent
  and relation. A deleted resource returns 404 before any ownership check.

### Error paths

- Malformed JSON on post create, PATCH, and comment create → 400
  `INVALID_REQUEST`; blank comment → 400 `INVALID_CONTENT`.
- Unknown and seed-deleted posts (detail, comments, like) → 404
  `POST_NOT_FOUND`; unknown comment → 404 `COMMENT_NOT_FOUND`; non-numeric
  post ID → 400 `INVALID_PARAMETER`.
- Non-owner PATCH or DELETE of seed post 2 → 403 `FORBIDDEN`; non-owner delete
  of a seed comment → 403 with the row unchanged.
- Every error was a JSON `{ code, message }` body with no HTML or stack trace,
  and `/health` stayed 200.

### Continuous 375px end-to-end (V-080 regression)

- The path ran in one Chromium session, starting from a different cookie:
  `/dev-user` → `apr_tester` → list → infinite scroll → post 13 detail → like
  on → comment create → comment delete → like off → back → FAB → create →
  toast → open created → sheet → edit (prefilled) → save → sheet → delete →
  list.
- Every screen and overlay had `scrollWidth` 375. The FAB, comment composer,
  and form action bar stayed fixed and inside the viewport, and state updated
  without manual reload.
- There were no page errors, console errors, or 4xx/5xx responses. Post 13's
  like and comment state were restored.

### Findings (no product change)

- The browser's automatic `GET /favicon.ico` returns 404 because no favicon
  has existed since the starter. It is not an app request and has no functional
  effect; left unchanged under the no-speculative-change rule.
- Chromium logs `net::ERR_ABORTED` for UI DELETE requests after their `204`
  response arrives. The client intentionally does not read a 204 body (P3), so
  the unread stream is cancelled. The request succeeds, as the `requestfinished`
  trace and database confirm. Next.js RSC prefetches cancelled by navigation
  also report `ERR_ABORTED`. Neither is a failed product request.
- Five verifier mistakes were corrected without product changes:
  - an invalid percent-encoded cursor that Tomcat dropped;
  - a boolean-to-text cleanup comparison;
  - a 21-code-point "20" fixture, which the UI correctly refused;
  - a fixed-position check on the button instead of its fixed action bar;
  - Playwright `headers()` omitting the `Cookie` header, fixed by using
    `allHeaders()`.

### Automated / static checks

- `backend/gradlew.bat clean test --no-daemon`: `BUILD SUCCESSFUL`, 51 tests,
  0 failures/errors/skips.
- `npm run typecheck`: exit 0.
- Production build:
  - Local: the generated `frontend/.next` was removed (git-ignored; the
    removal was permitted this time), then `npm run build` compiled
    successfully with `/`, `/_not-found`, `/dev-user`, `/posts/[postId]`,
    `/posts/[postId]/edit`, and `/posts/new`. This is a clean local build.
  - Docker: `docker compose build --no-cache frontend` also succeeded; its
    context excludes `.next` and `node_modules` via `frontend/.dockerignore`.
    The image was redeployed healthy and the list browser section passed 7/7
    against it.
- `git diff --check`: clean.
- Runtime after the checks: `db`, `backend`, and `frontend` healthy; `/health`
  `{"status":"ok","db":"up"}`; `/api/posts?limit=1` total 39; frontend root 200.
- Starter contracts: `git diff 38a9755 HEAD` is empty for
  `frontend/app/dev-user/` and `CorsConfig.java`. Cross-origin browser requests
  from `:3000` to `:8080` with the `x-user-id` cookie, including preflighted
  PATCH and DELETE, succeeded throughout.

### Data restoration

- The readable product state matches the baseline: readable total 39, post 13
  back to 856 with no like rows on readable posts, every temporary comment on a
  readable post soft-deleted, and seed rows unchanged.
- Soft-deleted P8-B artifacts were kept on purpose (rows are never erased):
  - Posts 43–50 are all `is_deleted = true`.
  - Comments 2079–2084 are `is_deleted = true`.
  - Comments 2077 and 2078 and two `post_likes` rows sit under deleted post
    47. They are the parent-delete evidence: physically retained but
    unreachable and not counted.
- Final physical state: posts 50 (11 deleted), comments 2,084 (87 deleted),
  `post_likes` 2 (0 on readable posts), seed history 1. Seed-deleted posts
  `9,30,31` and all 81 seed-deleted comment IDs are unchanged.
- The immutable seed baseline `42|2076` therefore now includes these
  runtime-generated acceptance rows. A fresh volume still reseeds exactly
  (V-003).

---

## P2A Read API Runtime Acceptance — 2026-09-29

- `backend/gradlew.bat test --no-daemon` completed with `BUILD SUCCESSFUL`; 26 tests passed, covering cursor codec precision/malformed inputs, requester precedence/fallback, and read-web contracts/errors.
- `docker compose up -d --build` rebuilt and started the application against the existing P1 volume. `db`, `backend`, and `frontend` were all `healthy`; backend `/health` and the frontend root each returned HTTP 200.
- `GET /api/posts?limit=7` traversed 39 readable posts in 6 pages. API IDs exactly matched the PostgreSQL `(created_at DESC, id DESC)` result with no duplicate/skip, `total` stayed 39, every page contained at most 7 rows, and final `nextCursor` was null.
- Deleted post IDs `9`, `30`, and `31` were absent from the full traversal; both detail and comments reads for each returned 404 without content leakage.
- All 39 readable post responses matched PostgreSQL-derived `base_like_count + post_likes` and non-deleted comment counts. Before requester mutation evidence, `post_likes` contained zero rows and every readable post returned `isLiked: false`.
- Comments for all 39 readable posts were traversed at limit 50 (69 pages total) and exactly matched PostgreSQL readable-comment IDs and order. None of all 81 seed-deleted comment IDs appeared. Post 2 was additionally traversed at limit 2: all 1,411 readable comments across 706 pages, with no duplicate/skip and a null final cursor.
- A unique temporary `post_likes` row proved header and cookie requester matching, default `apr_tester`, and header-over-cookie precedence. The row was removed in cleanup and `post_likes` returned to zero rows.
- Malformed post/comment cursors returned HTTP 400 `INVALID_CURSOR`; limits 0 and 51 returned HTTP 400 `INVALID_LIMIT`.
- The runtime verifier parsed JSON with Node.js and was removed after execution; it is not a repository artifact.

---

## P2B Mutation API Runtime Acceptance — 2026-09-29

- A project-scoped clean-volume `docker compose up -d --build` made `db`, `backend`, and `frontend` healthy; `/health` returned HTTP 200 with database status `up`.
- The baseline contained 42 physical/39 readable posts, 2,076 physical/1,995 non-deleted comment rows, zero `post_likes`, and one `assignment-v1` seed-history row. API-readable comments additionally require a readable parent post.
- Post ID `43` was created for `p2b_user_a` with zero derived counts and false requester-like state, appeared first in the list, rejected requester B's PATCH/DELETE with 403, accepted owner PATCH/DELETE, and remained physically stored after soft deletion.
- Validation returned JSON 400 errors for missing/blank title or content, a 21-code-point title, blank comment, and malformed JSON. A title containing exactly 20 emoji code points returned 201.
- Two requesters independently toggled one post through count sequence `0 → 1 → 2 → 1 → 0`; each requester saw only their own `isLiked`, and the final database like-row count was zero.
- Comment ID `2077` was created by requester B. Post owner A could not delete it (403), requester B could (204), the row remained with `is_deleted = true`, the derived count returned from one to zero, and a repeated delete returned 404.
- After post deletion, list exclusion and 404 behavior were verified for detail, comments read/create, PATCH, like toggle, and repeated DELETE. The original 42 posts, 2,076 comments, seed-history marker, and seed-deleted read exclusions remained intact.
- Cleanup used only this APR Compose project's `down -v` followed by `up -d`. The restored baseline is 42/39 posts, 2,076 physical/1,995 non-deleted comment rows, zero likes, one seed-history row, and `/health` HTTP 200. The Node.js verifier was removed.

---

## P3 Frontend Foundation Evidence — 2026-09-29

- `npm ci` installed from the updated lockfile; `@tanstack/react-query` resolved to 5.104.0 with `@tanstack/query-core` 5.104.0 and no other new package.
- `npm run typecheck` (`tsc --noEmit`, strict) exited 0. `npm run build` (Next.js 15.4.6) succeeded with routes `/`, `/_not-found`, and `/dev-user`.
- `docker compose up -d --build` rebuilt backend and frontend against the existing volume; `db`, `backend`, and `frontend` were `healthy`. `GET /health`, frontend `/`, and frontend `/dev-user` each returned HTTP 200.
- P2 read smoke: `GET /api/posts?limit=2` returned HTTP 200 with `items` (2), `total` (39), and a string `nextCursor`; item fields exactly matched `frontend/lib/api/types.ts` `Post`.
- A temporary Node.js script exercised `frontend/lib/api/client.ts` `request()` directly: `credentials: "include"` on GET/POST/DELETE, no body or `Content-Type` on DELETE, JSON body plus `Content-Type` on POST, fallback base URL, path joining, `URLSearchParams` cursor encoding, 204 returning `undefined` without parsing, non-JSON and network failures mapped to `ApiError`, and against the live backend: page continuation by `nextCursor`, deleted post 9 → 404 `POST_NOT_FOUND`, malformed cursor → 400 `INVALID_CURSOR`. All 14 checks passed; the script was removed.
- A temporary fixed-clock script checked `formatRelativeTime`: `0초 전`, `30초 전`, `3분 전`, `3시간 전`, `2일 전`, `1개월 전`, `1년 전`, future `5분 후`, and `null` for an invalid timestamp. The script was removed.
- Boundary: no product screen issues browser API requests yet, so the credentialed browser cookie flow (`V-012`) is not executed by P3.

---

## P4 Post List Evidence — 2026-09-29

- `npm run typecheck` exited 0. A clean `npm run build` (after removing a stale local `.next`) succeeded with `/` now a client list screen.
- `docker compose up -d --build frontend` rebuilt the frontend against the running healthy `db`/`backend`; frontend `/` returned HTTP 200.
- A temporary Playwright (Chromium) script ran at a `375 × 778` viewport against that runtime and passed 48/48 checks; the script and its screenshots stayed in the session scratchpad and are not committed.
- Pagination: before scrolling exactly one request, `GET /api/posts?limit=10` (no cursor). Scrolling produced exactly four requests, all `limit=10`, each later cursor equal to the previous `nextCursor`; page sizes `10/10/10/9`, `total: 39` on every page, final `nextCursor: null`, and no further request after additional scrolling.
- Rendering: 39 rows, no duplicate post ID, DOM order identical to the API newest-first traversal; every row shows a relative time such as `29일 전` / `1개월 전`.
- Avatars: every row uses one of the three provided assets, one author maps to one avatar across rows, and the mapping is identical after reload.
- Navigation: clicking a row opened `/posts/1`; the write FAB opened `/posts/new`. Both pages are 404 until P5/P6, and the 60 observed 404s were only production `Link` prefetches/navigation to `/posts/{id}` and `/posts/new`.
- States (forced with Playwright route interception): initial loading (`게시글을 불러오는 중입니다.`), empty (`아직 작성된 게시글이 없습니다.`), initial error after TanStack's default retries (4 attempts) with a working `다시 시도`, next-page loading (`게시글을 더 불러오는 중입니다.`), and next-page error that keeps the 10 loaded rows, does not auto-retry on further scrolling, and recovers via its own `다시 시도`. *(Historical: the 4-attempt default and page size 10 applied on 2026-09-29. Since PR #40 a failed read makes 2 attempts (one retry; 4xx none), and the page size is 15. See "F-06".)*
- Regression found and fixed during this run: TanStack Query v5 sets `status: "error"` when only `fetchNextPage` fails, so the first build also rendered the initial-error block (two `다시 시도` buttons). The initial-error block now requires `data === undefined`; the rerun confirmed a single retry control.

---

## P5A Post Detail / Like / Comment Read Evidence — 2026-09-29

- Implementation commit `2af23aab5cf1009c6e39a359be5abb01a7c34566`. `npm run typecheck` exited 0; a clean `npm run build` produced `ƒ /posts/[postId]`; `git diff --check` was clean. `docker compose up -d --build frontend` rebuilt the production frontend against the healthy `db`/`backend`.
- A temporary Playwright (Chromium) script at `375 × 778` passed 63/63 checks; the script and screenshots stayed in the session scratchpad and are not committed.
- Detail: a list row opened `/posts/1` with the same title; the back button returned to `/`; direct `/posts/4` rendered title, author, provided avatar, `YY.MM.DD` date, content, `좋아요 1,109`, and `댓글 (27)`; nav 375×48, content from y=56, no horizontal scroll, title Bold 18/26, author Bold 15/22, body Regular 14/18, heading Bold 16/24 (`#292828`).
- Missing/deleted: `/posts/9` (seed-deleted) and `/posts/99999` showed `게시글을 찾을 수 없습니다.` with a `목록으로` link; the 404 was not retried (one detail request) and no comment request was sent; `/posts/abc` showed the same state without an API request. Detail loading and a forced 500 (after default retries) with a working `다시 시도` were also exercised.
- Like (post 13, `apr_tester`): unliked → liked with `856 → 857`, filled heart `rgb(255, 141, 161)` vs unliked `rgb(151, 158, 169)`, `aria-pressed` updated; reload matched the backend; liked → unliked restored `856`. Two clicks dispatched in the same tick plus a forced click during a held request produced exactly one toggle request, and the button was disabled while pending. A forced 500 left the state unliked at `856`, showed `좋아요를 반영하지 못했습니다. 다시 시도해 주세요.`, and re-enabled the button.
- Requester isolation: with `apr_tester` liking post 13 via the API, selecting `mintcream` on the unchanged `/dev-user` page showed the post unliked at `857`; toggling as `mintcream` gave `858` with both users liked on the backend. All temporary likes were reverted (post 13 back to `856`, neither user liked).
- List consistency: after liking the newest post from its detail page with list refetches frozen by route interception, returning to `/` showed the incremented count from the updated list cache. The like was reverted.
- Comment counts: post 10 (0) showed only `댓글 (0)` with no list, no More, and no empty message; post 24 (1) and post 1 (2) showed all comments newest-first without More. Each sent exactly one comment request, `limit=2` without cursor.
- More + infinite scroll: posts 3 (3 comments) and 4 (27) first sent one `limit=2` request and showed the two newest plus `댓글 더보기`; scrolling to the bottom before More sent no further request. After More, requests were `limit=20` with each cursor equal to the previous `nextCursor`; all comments rendered newest-first with no duplicate, More disappeared, the last `nextCursor` was `null`, and no request followed it (post 3: 2 requests, post 4: 3 requests). Post 2 (1,411) loaded incrementally in bounded `limit=20` pages.
- Comment states: initial loading, initial error (post body stays visible) with retry, continuation loading, and continuation error that keeps the 22 loaded comments, shows a single `다시 시도` without the initial-error block, does not auto-retry on scrolling, and recovers to 27.
- Regression found and fixed during this run: a same-tick double click sent two toggles because TanStack's `isPending` renders one scheduler tick after `mutate`; `useTogglePostLike` now guards with an in-flight ref. A leftover test like from the failed run was reverted before the passing run.
- P4 regression: the P4 script passed 48/48 (39 posts as 10/10/10/9, no duplicate, FAB → `/posts/new`) with zero 404 responses; list detail-link prefetch now issues background requests that succeed, and no `/posts/new` prefetch occurs.

### P5A Review Fix — Same-Session Revisit — 2026-09-29

- Defect (PR #9 review): comment pages survived in the shared `QueryClient` after leaving the detail screen. A temporary Playwright revisit script at `375 × 778` reproduced it against the unfixed production build (`14ab5a0`): on re-entering post 4 without a full reload, the mount refetch sent the two cached `limit=20` continuation requests before More, and More showed all 27 comments within 50 ms (6/12 checks failed).
- Fix commit `ddc1a9da6e4c91eb0d8dfe3b1a4c9c886dbf3c1c` (`useComments` `gcTime: 0`). `npm run typecheck` exited 0, a clean `npm run build` succeeded, `git diff --check` was clean, and the production frontend container was rebuilt.
- Revisit script against the fixed build, 12/12: visit 1 made one `limit=2` request, then More and scrolling loaded two continuation pages (27 shown). Going back with the in-app back button and clicking the same row (no full reload, verified by a window marker) produced exactly one fresh `limit=2` request and 0 continuation requests before More, even after scrolling; at most 2 comments were visible. 50 ms after More only 2 were shown. The first continuation was `limit=20` with the revisit's first-page `nextCursor` and rendered 22. Scrolling completed the cursor chain to 27 in API newest-first order with no duplicate, the final `nextCursor` was `null`, and no request followed it.
- Re-run on the same build: the P5A script passed 63/63 and the P4 script passed 48/48 (zero 404 responses). Post 13 like state was confirmed restored (`856`, unliked for `apr_tester` and `mintcream`).

---

## P5B Comment Mutation Evidence — 2026-09-29

- Implementation commit `07c93724df195b2158d459d98eddd399797faa9b` (frontend only; no backend, schema, or dependency change). `npm run typecheck` exited 0; a clean `npm run build` succeeded; `backend/gradlew.bat clean test` passed 49/49 unchanged; `git diff --check` was clean. The production frontend container was rebuilt from this code.
- A temporary Playwright (Chromium) script at `375 × 778` passed **52/52** checks against the Compose production stack. The script and screenshots stayed in the session scratchpad.
- Composer: fixed 375×86 region at y=692 on every post, including with 0 comments. Whitespace-only input disables `등록`, and Enter sends no request. The input clears only after a successful create. Content at the page bottom stays above the composer, and a long unbroken comment wraps with no horizontal overflow.
- Create boundaries on post 23 (skin_beginner's post, 0 comments) as `apr_tester`:
  - 0→1: `댓글 (1)`, server author `apr_tester`, no More.
  - 1→2: both newest-first, no More.
  - 2→3: collapsed still shows the 2 newest, `댓글 더보기` appears, and More reveals the third with no extra request.
  - Reload matched the server (3).
  - 3+→+1 on post 4 (27→28): new comment first, More kept, expanded list equal to the server.
- Delete boundaries on the same post:
  - 3→2 (first-page item, collapsed): More disappears and the remaining 2 are shown.
  - 2→1, then 1→0: only `댓글 (0)`, no new empty message, composer kept.
  - Reload matched the server (0).
  - Seed comment 1834 (`apr_tester`) on Roidl_08's post 26: `삭제` shown only on it. Cancel sent no DELETE. Confirm took 3→2, More disappeared, and the deletion persisted.
- Expanded multi-page state on post 8 (58):
  - After loading 2 continuation pages, deleting own comment 1613 (inside a continuation page) removed only it (`댓글 (57)`).
  - Creating while expanded put the new comment first (`댓글 (58)`) with no duplicate.
  - Scrolling to the end matched the server exactly (58).
- Cursor boundary: acting as `Jelin` (cookie), the last loaded row 1608 (position 42, the page's cursor row) was deleted. The next continuation request used the deleted row's `(createdAt, id)` cursor (`…|1608`), and the full list then equaled the server (57) with no skip or duplicate.
- Count sync (post 1, list row 1, list refetch frozen by route interception):
  - After a create, going back showed the list count +1.
  - Re-entering the detail showed `댓글 (3)`.
  - After a delete, the list count returned.
  - A list reload matched the server.
- Failures:
  - POST 500, 400, and network abort each kept the input text, added no comment, left the count unchanged, and showed `댓글을 등록하지 못했습니다. 다시 시도해 주세요.`. The retry created exactly one comment.
  - DELETE 500, 403, and 404 each kept the dialog open with `댓글을 삭제하지 못했습니다. 다시 시도해 주세요.`, re-enabled `예`, and left the comment and count unchanged. The retry succeeded.
- Duplicate prevention: two `requestSubmit()` calls in the same tick produced exactly 1 POST and 1 comment. Two clicks on `예` in the same tick produced exactly 1 DELETE.
- Dialog: `alertdialog` with `aria-modal="true"` and `aria-labelledby`, 327×264 at (24, 257), title `삭제하기`, body `삭제한 댓글은 복구할 수 없습니다.` / `정말 삭제 하시겠습니까?`, actions `취소` / `예`.
- Requester switch:
  - `/dev-user` → `mintcream`, then a create on Jelin's post 41: response and UI author `mintcream`, with `삭제` only on mintcream's comment.
  - A fresh `apr_tester` load showed no `삭제` on it.
  - Backend: an `apr_tester` DELETE of that comment returned 403; mintcream's own delete returned 204 and a repeat returned 404.
- Regressions:
  - The P5A revisit script passed 12/12 after the mutations (post 4 at 28 comments: fresh `limit=2`, 0 continuation requests before More).
  - After reseeding: P4 48/48 (0 unexpected 404s), P5A 63/63, and the keyset traversal/tie script (39 posts; post 2 with 1,411 comments in 72 pages; tie fixture) all passed.
- Test-script maintenance (not product changes): the older P5A/revisit scripts read comment text with `li p:last-child`; own comments now end with the `삭제` button, so they now read `p:nth-of-type(2)`. The P5A zero-comment "no extra empty message" check now excludes the composer form from the section text.
- DB baseline: the soft-deleted test comments remain as physical rows, so the baseline was restored by dropping the schema and letting Flyway and the seed runner recreate it. Final state: posts 42 (3 deleted), comments 2,076 (81 deleted), likes 0, `seed_history = assignment-v1`.

---

## P6 Post CRUD Evidence — 2026-09-29

- Implementation commit `1f8a21f95fbbc132266e9f3d76f6154504127fd1` (frontend only). Checks: `npm run typecheck` 0 errors, a clean `npm run build` (routes `○ /posts/new`, `ƒ /posts/[postId]/edit`), backend `gradlew clean test` 49/49, and `git diff --check` clean. The production frontend container was rebuilt from this code.
- A temporary Playwright (Chromium) script at `375 × 778` passed **74/74** checks after a reseed. The script and screenshots stayed in the session scratchpad.
- **Validation (create and edit):** empty title or content, whitespace-only, U+3000-only, a 21-character ASCII title, and a 21-emoji title (42 UTF-16 units) all kept the button disabled, and a forced `requestSubmit()` sent 0 requests. A 20-character ASCII title, a 20-emoji title (40 UTF-16 units), and a Korean title were accepted. The frontend counts code points like Java `codePointCount` and treats whitespace like Java `isBlank`. Values are sent untrimmed. Enter in the title does not submit.
- **Create:**
  - The FAB opens `/posts/new` (`LIST-006`).
  - POST 500 and network abort both kept the form values and URL, showed `게시글을 등록하지 못했습니다. 다시 시도해 주세요.`, showed no toast, and created nothing.
  - A same-tick double submit sent exactly 1 POST.
  - The server post had author `apr_tester`, `likeCount` 0, `commentCount` 0, `isLiked` false, and the exact 20-code-point title and multiline content.
  - With the list's first-page refetch frozen, the list showed the new post first from the cache update.
  - A full traversal then matched the server (total 40, unique, new first).
- **Toast:** `게시글이 등록되었습니다.` appeared once at 181×40, x=97.5, y=657 with `pointer-events: none`. It disappeared after about 2.5 s and did not reappear on a list revisit or reload.
- **Edit (own post):**
  - The trigger was visible and the sheet was 375×224 at y=554. Escape, scrim click, and `취소` each closed it.
  - `수정하기` opened `/posts/{id}/edit` with the same form (`글수정` / `수정 완료`) and exact prefill. An empty title or a 21-code-point title disabled the button.
  - PATCH 400, 403, 404, 500, and network failure each kept the values and URL, showed an error, allowed retry, and changed nothing on the server.
  - A same-tick double save sent exactly 1 PATCH.
  - On success the detail showed the response immediately, and Back returned to the list, which showed the edited title from the cache (first-page refetch frozen). After reload the server title and content matched and `createdAt` was unchanged.
- **Other users' posts:**
  - Post 2 (Roidl_08) had no trigger, and `/posts/2/edit` showed `이 게시글을 수정할 권한이 없습니다.` with no form.
  - `/posts/99999/edit` and `/posts/abc/edit` showed not-found.
  - PATCH and DELETE as `apr_tester` returned 403, and the post was unchanged.
- **Delete (own post):**
  - `삭제하기` closed the sheet and opened a 327×264 dialog at (24, 257) with `삭제한 글은 복구할 수 없습니다.` / `정말 삭제 하시겠습니까?`, so only one overlay was open. `취소` sent 0 DELETEs.
  - DELETE 403, 404, 500, and network failure each kept the dialog with `게시글을 삭제하지 못했습니다. 다시 시도해 주세요.` and a re-enabled `예`, stayed on the detail, and left the post readable.
  - A same-tick double confirm sent exactly 1 DELETE and replaced the URL with `/`.
  - With the list refetch frozen, the post was absent from the cached list. A full traversal matched the server (total 39).
  - A direct revisit showed not-found, and the DB row remained with `is_deleted = true`.
- **Cached-list deletes:**
  - Deleting a middle row (post 5, row 5) removed only that row from the cached page.
  - A second cached-page delete targeted post 11 as `booster_0`, also removing only that row.
  - In both cases, scrolling on continued from the cached cursor, the full list equaled the server, and there was no skip or duplicate.
  - Correction (Deletion Policy Hardening): because post 5 had already been deleted in the same run, post 11 was row 9 at that moment, not the page's cursor row. A true cursor-row delete was then verified separately (scenario C in "Deletion Consistency Evidence"), and the P6 script now picks row 10 at run time (it deleted post 12 at row 10/10 and passed).
- **`/dev-user` → `mintcream`:**
  - A created post had author `mintcream`, and mintcream saw the trigger while a fresh `apr_tester` load did not.
  - `apr_tester` PATCH/DELETE returned 403.
  - mintcream deleted it through the UI (then 404).
- **375×778 (V-080):** list, detail, action sheet, post delete dialog, create (with 80 content lines scrolled to the end), and edit all had `scrollWidth` 375. The fixed controls were visible and did not cover content.
- **Regression found and fixed before commit:** returning with `router.replace` after a successful create or edit stacked a duplicate history entry, so Back from the edited detail showed the same detail again. The form now remembers the in-app screen that opened it (`lib/formOrigin.ts`) and goes back to that entry; direct URL loads still replace.
- **Regressions after reseeding:**
  - P4 48/48 and P5A 63/63. The P5A prefetch check now expects the restored FAB prefetch to never 404.
  - P5A revisit 12/12.
  - Keyset traversal/tie script passed.
  - P5B 52/52.
- **DB baseline:** created and soft-deleted test posts leave physical rows, so the schema was reseeded after each suite. Final state: posts 42 (3 deleted), comments 2,076 (81 deleted), likes 0, `seed_history = assignment-v1`.

---

# Requester / Ownership

## V-010 — Default Requester

**Requirements:** `USER-003`

### Steps

Call a requester-sensitive endpoint without `x-user-id` header or cookie.

### Expected

- Request is treated as coming from `apr_tester`.

### Execution Evidence — 2026-09-29

- Resolver tests verified the no-header/no-cookie fallback.
- Runtime detail read without header/cookie remained `isLiked: false` while a different temporary requester had a like row.

### P8-B Current-Candidate Evidence — 2026-09-30

- A toggle with no header or cookie stored `post_likes.user_id = apr_tester`,
  was visible to header `apr_tester` only, and a post created without identity
  had author `apr_tester`.

**Result:** `PASS`

---

## V-011 — Header Requester

**Requirements:** `USER-001`

### Steps

Call a requester-sensitive endpoint with:

```http
x-user-id: <known-author>
```

### Expected

- Ownership and `isLiked` are evaluated for the header user.

### Execution Evidence — 2026-09-29

- Resolver tests covered header-only and header-over-cookie precedence.
- Runtime detail read returned `isLiked: true` only when `x-user-id` matched the temporary like requester; a different header over the matching cookie returned false.

### P8-B Current-Candidate Evidence — 2026-09-30

- With header only, the like requester read true. Header precedence held in
  both directions when a cookie was also present, and a blank header fell back
  to the cookie. Header requesters also drove ownership (403 for non-owner
  PATCH/DELETE).

**Result:** `PASS`

---

## V-012 — Cookie Requester

**Requirements:** `USER-002`, `START-003`

### Steps

1. Select a user through `/dev-user`.
2. Perform frontend/API operations that depend on requester identity.

### Expected

- `x-user-id` cookie is sent.
- Backend evaluates ownership/like state for that cookie user.

### P2A Partial Evidence — 2026-09-29

- Resolver tests and a direct runtime API request verified that the backend accepts the `x-user-id` cookie and evaluates `isLiked` for that requester.
- The immutable `/dev-user` frontend-to-backend credentialed browser flow has not been executed, so this combined scenario remains open.

### P3 Partial Evidence — 2026-09-29

- The shared browser client `frontend/lib/api/client.ts` applies `credentials: "include"` to every request; this was checked by direct invocation, not by a browser session.
- No product screen performs a browser request yet. The full `/dev-user` → product request → requester reflection flow is deferred to P4/P5 or P8.

### P4 Evidence — 2026-09-29

- A temporary like on the newest post was created for `mintcream` through the API. In Chromium, selecting `mintcream` on the unchanged `/dev-user` page redirected to `/`; the list request carried `Cookie: x-user-id=mintcream` and returned that post with `isLiked: true`. The like was toggled back afterwards (`isLiked: false`, `likeCount` restored to `1796`).
- This verifies the credentialed browser request and requester-specific like evaluation. Ownership reflected in the browser (own-post menu, own-comment delete) is not yet rendered and remains for P5/P6.

### P5A Evidence — 2026-09-29

- Selecting `mintcream` on `/dev-user` produced a detail view with that requester's own `isLiked`, independent of `apr_tester`'s like, and the cookie user's toggle was recorded for `mintcream` only. Ownership UI (own-post menu, own-comment delete) is still unbuilt (P5B/P6), so the combined scenario remains open.

### P5B Evidence — 2026-09-29

- Comment ownership now follows the cookie requester in the browser. After `/dev-user` → `mintcream`, created comments are authored by `mintcream` and only mintcream's comments show `삭제`; a default `apr_tester` load does not. The post-owner menu (P6) is still unbuilt, so the combined scenario remains open.

### P6 Evidence — 2026-09-29

- With the post menu in place, every requester-dependent behavior is now observed in a real browser session after `/dev-user`. Across P4–P6 this covers the credentialed cookie, the requester's like state, comment ownership, and post ownership. `mintcream` sees their own post menu and `apr_tester` does not, and the backend returns 403 for `apr_tester` PATCH/DELETE on mintcream's post.

### P8-B Current-Candidate Evidence — 2026-09-30

- The earlier partial notes ("remains open") are historical; this combined
  scenario is closed.
- On the current candidate, a direct cookie-only API read resolved the cookie
  requester. In the browser, `/dev-user` → `mintcream` sent
  `x-user-id=mintcream` on product requests, and like state, comment
  authorship, and `삭제` followed it. `/dev-user` → `Roidl_08` showed the
  post-2 owner menu, and `apr_tester` did not see it.

**Result:** `PASS`

---

# Post List

## V-020 — Newest-First Post Pagination

**Requirements:** `LIST-003`, `LIST-004`, `LIST-005`, `API-001`, `API-031`, `API-032`

### Steps

1. Request the first post page.
2. Record IDs/timestamps and pagination data.
3. Request the next page.
4. Continue until the end.

### Expected

- Each page is newest-first.
- Pages contain bounded subsets, not the entire collection.
- No duplicate post appears across adjacent pages.
- `total` represents the full readable count rather than current page size.

### Execution Evidence — 2026-09-29

- Limit-7 traversal covered 39 readable posts in 6 pages, exactly matching PostgreSQL order and IDs with stable `total: 39`, bounded pages, no duplicate/skip, and final `nextCursor: null`.
- This passes the P2A backend pagination scenario. Frontend infinite scrolling (`LIST-004`, UI portion of `LIST-005`) remains `TODO`.

### P4 Frontend Evidence — 2026-09-29

- The browser list loaded 39 posts as 10/10/10/9 through `IntersectionObserver`-triggered `fetchNextPage`, with no duplicate, no skip, API-identical order, and no request after the last page. See "P4 Post List Evidence".

### P8-B Current-Candidate Evidence — 2026-09-30

- The "remains `TODO`" note above is historical: P4 closed the frontend
  portion.
- Current API traversals at limits 7/10/50 matched PostgreSQL order exactly
  with total 39 and a null final cursor. Invalid limit and cursor returned 400.
  The browser re-verified 10/10/10/9 through four chained requests with no
  request after the end, including on the no-cache rebuilt image.

### Later Evidence — 2026-10-02/03

- The page size later became 15 and the first page is server-rendered: on
  `7da54e8` the 39 readable posts load as the SSR page plus two cursor
  requests (`15/15/9`) with no browser request for the first page, no
  duplicate, and no request after the end (PR #45 terminal traversal; clean
  clone in "F-16"). The newest-first contract above is unchanged.

**Result:** `PASS`

---

## V-021 — Deleted Posts Excluded

**Requirements:** `SEED-003`, `DEL-003`, `DEL-004`, `API-035`

### Steps

1. Identify a seed ID listed in `deletedPostIds`.
2. Request post list.
3. Request detail for that ID.

### Expected

- Deleted post is not present in the list.
- Normal detail API does not expose the deleted post as readable content.

### Execution Evidence — 2026-09-29

- Full post traversal excluded all seed-deleted IDs `9`, `30`, and `31`.
- Detail and comments endpoints returned HTTP 404 for each of those IDs.

### P8-B Current-Candidate Evidence — 2026-09-30

- Seed-deleted 9, 30, and 31 were absent from every traversal. Post 9 detail
  and comments returned 404 `POST_NOT_FOUND`. Posts deleted during P8-B
  disappeared from the list and total, and the browser showed not-found on
  direct visit.

**Result:** `PASS`

---

# Post Creation / Editing

## V-030 — Post Validation

**Requirements:** `EDIT-001`, `EDIT-002`, `EDIT-003`, `EDIT-004`

### Scenarios

- empty title
- empty content
- title longer than 20 characters
- valid title/content

### Expected

- Invalid input cannot be saved.
- Frontend completion action is disabled for invalid required input.
- Backend rejects invalid payloads even if called directly.
- Valid payload succeeds.

### P2B Backend Evidence — 2026-09-29

- Direct POST requests returned HTTP 400 JSON errors for missing/blank title, a 21-code-point title, missing/blank content, and malformed JSON.
- A title containing exactly 20 emoji code points returned HTTP 201, confirming code-point rather than UTF-16-unit validation.
- Automated tests cover null/blank fields, 20/21 code-point boundaries, and emoji surrogate pairs.
- The frontend-disabled portion remains unimplemented, so this combined UI/backend scenario is not marked PASS.

### P6 UI Evidence — 2026-09-29

- UI: invalid title/content (blank, whitespace-only, over 20 code points including emoji) disables `작성 완료` / `수정 완료` and sends no request; 20 ASCII and 20 emoji code points are accepted. See "P6 Post CRUD Evidence".

### P8-B Current-Candidate Evidence — 2026-09-30

- The P2B "not marked PASS" note is historical: P6 closed the UI portion.
- On the current candidate, the backend rejected 10 invalid payloads with 400
  JSON and created no row: empty, missing, whitespace, or U+3000 title; empty,
  missing, or whitespace content; and 21 ASCII, 21 emoji, or 21 Korean code
  points. It accepted 20 ASCII, 20 emoji, and 20 Korean code points (201).
  PATCH rejected a 21-emoji title.
- The UI disabled `작성 완료` for the same invalid cases, a forced
  `requestSubmit()` sent 0 requests, and the three 20-code-point titles
  enabled it.

**Result:** `PASS`

---

## V-031 — Create Post Flow

**Requirements:** `EDIT-005`, `EDIT-006`, `API-002`

### Steps

1. Create a valid post from the UI.
2. Observe navigation.
3. Observe toast.
4. Confirm post appears in newest-first list.

### Expected

- Post is created for current requester.
- UI returns to list.
- Completion toast is shown.
- New post is visible in the correct order.

### P2B Backend Evidence — 2026-09-29

- `POST /api/posts` returned HTTP 201 with generated ID `43`, requester author `p2b_user_a`, and `likeCount: 0`, `commentCount: 0`, `isLiked: false`.
- PostgreSQL retained the physical row with `base_like_count = 0` and `is_deleted = false`; the next list read placed it first.
- UI navigation and toast behavior remain for a frontend phase, so the full scenario remains open.

### P6 UI Evidence — 2026-09-29

- UI: the FAB opens the create screen; success returns to the list with the new post first and a one-time toast; failures keep input and create nothing; same-tick double submit sends one POST. See "P6 Post CRUD Evidence".

### P8-B Current-Candidate Evidence — 2026-09-30

- The P2B "remains open" note is historical.
- Current browser run: FAB → `/posts/new` → one POST (201) with a
  20-code-point title → `/` with the new post first, list total +1, and the
  completion toast once, which then expired. The server post had author
  `apr_tester` and zero counts. The continuous 375px path repeated this.

**Result:** `PASS`

---

## V-032 — Edit Own Post

**Requirements:** `DETAIL-012` through `DETAIL-017`, `API-004`

### Steps

1. Open a post owned by the current requester.
2. Open action menu.
3. Choose edit.
4. Verify title/content are prefilled.
5. Modify valid values and save.
6. Re-fetch detail.

### Expected

- Edit action is available for owner.
- Existing values are prefilled.
- Same validation rules as create apply.
- Updated values persist.

### P2B Backend Evidence — 2026-09-29

- The owner PATCH returned HTTP 200 with the updated post object; a following detail read and PostgreSQL query confirmed persistence.
- `author` and `createdAt` were unchanged, and PATCH uses the same validator as POST.
- Action-menu visibility, edit-form reuse, and prefill remain frontend work, so the full scenario remains open.

### P6 UI Evidence — 2026-09-29

- UI: own-post trigger → sheet → edit form with exact prefill and the same validation; PATCH success updates detail and cached list, reload matches; failures keep values. See "P6 Post CRUD Evidence".

### P8-B Current-Candidate Evidence — 2026-09-30

- The P2B "remains open" note is historical.
- Current browser run: own-post menu → sheet → `수정하기` → exact
  title/content prefill → one PATCH (200) → the detail updated, the list cache
  showed the new title, and reload and the API matched with `createdAt`
  unchanged. The API PATCH response was the full single post object.

**Result:** `PASS`

---

## V-033 — Reject Editing Another User’s Post

**Requirements:** `DETAIL-019`, `API-034`

### Steps

1. Use requester A.
2. Attempt to PATCH a post authored by requester B directly through the API.

### Expected

- Mutation is rejected.
- Post remains unchanged.

### Execution Evidence — 2026-09-29

- Requester `p2b_user_b` PATCHed requester `p2b_user_a`'s readable post and received HTTP 403 with the JSON error contract.
- A direct PostgreSQL query confirmed the title was unchanged; the owner subsequently PATCHed the same row successfully.

### P6 UI Evidence — 2026-09-29

- The post menu is not rendered on other users' posts, `/posts/{other}/edit` shows no form, and direct PATCH/DELETE still returns 403 (checked for `apr_tester` → Roidl_08 and → mintcream).

### P8-B Current-Candidate Evidence — 2026-09-30

- Non-owner PATCH on a readable post returned 403 and the title stayed
  unchanged. This was shown for a P8-B post, the UI-created post (`mintcream`),
  and seed post 2.
- `apr_tester` saw no menu on post 2, and `/posts/2/edit` showed the
  no-permission notice without a form.

**Result:** `PASS`

---

# Soft Delete

## Deletion Consistency Evidence — 2026-09-29

- Fix commit `4e53dbd6fb951015f595ae980da1fe845929b60b` (`frontend/lib/query/postListCache.ts` only; no backend, schema, or API change).
- **Root cause:** `removePost()` returned early when the deleted post was not among the loaded list pages, so the cached `total` stayed one too high (for example after deleting a post reached without scrolling to it).
- **Method:** a temporary script ran against the Compose production stack (Chromium 375×778, API, `psql`) after a reseed and passed **28/28**. The page's QueryClient was read through React fiber introspection (test-only), with the list's first-page refetch frozen so that only the cache update was observed.
- **Browser cache scenarios:**
  - **B. Unloaded-list delete:** first page loaded (10 rows, total 39). The owner then opened post 18, which was not loaded, through a client-side `router.push` and deleted it. Before any refetch the cached total was 39→38 on every page and equal to the server, and the loaded rows were unchanged. The detail and comment queries for post 18 were evicted (none left under `["posts","detail",18]`). Infinite scroll to the end equaled the server's 38 IDs with no duplicate or skip, and Back did not return to the deleted detail.
  - **A. Loaded-list delete:** post 5 (row 5). Total 38→37, equal to the server. Only that row was removed from the cache. Queries were evicted, the full scroll equaled the server, and Back did not return to it.
  - **C. Cursor-row delete:** row 10 at run time was post 12, and the first page's `nextCursor` decoded to `…|12`. Its author (`cos_holic`) deleted it. Total 37→36, equal to the server, and the first 9 rows were kept. The next page was requested with the deleted row's cursor `…|12`, and the full list equaled the server (36) with no duplicate or skip.
- **Parent delete matrix** (a new post by `apr_tester`, with comments by `apr_tester` and `mintcream` and likes by both):
  - Before the delete: readable with `commentCount` 2 and `likeCount` 2. DB: post `f`, comments `false,false`, 2 like rows.
  - While readable, `mintcream` DELETE and PATCH returned 403 and the post stayed readable. The owner DELETE returned 204.
  - DB after the delete: post `t`, both comments still `false`, 2 like rows kept.
  - Afterwards every route returned 404 for owner and non-owner: GET post, PATCH, DELETE again, like toggle, GET comments, POST comment, and DELETE of each existing comment (including by its own author).
  - None of those 404 calls changed the DB. A never-existing post returned 404 for GET, PATCH, DELETE, and toggle.
- **Comment standalone (post 13 alive):**
  - Non-owner DELETE returned 403; the comment stayed listed and `is_deleted = f`.
  - Owner DELETE returned 204, then 404 on a repeat by the owner or a non-owner.
  - The row was kept with `is_deleted = t` and excluded from the list, and `total` and `commentCount` returned to the base value.
- **Concurrency smoke** (15 rounds, each firing post DELETE together with comment create, like toggle, edit, and delete of an existing comment on a fresh post):
  - The post DELETE always returned 204, and the post was unreadable afterwards.
  - Child operations returned only success-before-delete or 404 (comment create `201`/`404`, like `200`/`404`, edit `200`/`404`, comment delete `204`/`404`), with no 5xx or deadlock.
- **Backend:** unchanged, since the matrix matched the policy. Existing mock tests already cover the service-level 404 paths (`rejectsCreationForDeletedOrMissingParent`, `treatsDeletedOrMissingCommentAsNotFound`, `treatsDeletedOrMissingPostAsNotFound`, `rejectsLikeToggleForDeletedOrMissingPost`). Parent visibility for comment delete lives in SQL (the readable-parent join), so it is proven by the PostgreSQL matrix rather than by mocks. `gradlew clean test` passed 49/49.
- **Regressions after reseeding each:** P6 74/74 (with the run-time cursor row), P5B 52/52, P4 48/48, P5A 63/63, revisit 12/12. `npm run typecheck`, a clean `npm run build`, and `git diff --check` were clean.
- **DB baseline** restored by reseeding: posts 42 (3 deleted), comments 2,076 (81 deleted), likes 0, `seed_history = assignment-v1`.

## V-040 — Soft Delete Own Post

**Requirements:** `DEL-001`, `DEL-003`, `DEL-004`, `DETAIL-018`, `API-005`

### Steps

1. Create or identify an own post.
2. Record DB row existence.
3. Delete through API/UI.
4. Query DB row again.
5. Query post list.
6. Query detail.

### Expected

- DB row still exists with deleted marker/state.
- Resource disappears from list.
- Resource is not readable through normal detail API.
- UI returns to list after own-post deletion.

### P2B Backend Evidence — 2026-09-29

- Owner DELETE returned HTTP 204. PostgreSQL still contained exactly one row for ID `43` with `is_deleted = true`.
- List excluded the post; detail and comments reads returned 404. PATCH, like toggle, repeated DELETE, and comment creation also returned 404.
- UI return-to-list behavior remains unimplemented, so this combined API/UI scenario is not marked PASS.

### P6 UI Evidence — 2026-09-29

- UI: sheet → confirmation → one DELETE → list without the post (cache and server), direct URL not found, DB row `is_deleted = true`; cancel and failures leave the post. See "P6 Post CRUD Evidence".

### P8-B Current-Candidate Evidence — 2026-09-30

- The P2B "not marked PASS" note is historical.
- Current browser run: sheet → dialog → one DELETE → `/` without the row,
  total back to 39, direct URL not found, row `is_deleted = true`, and a
  repeat DELETE returned 404.
- Parent-delete matrix: the post row, child comments, and like rows were
  retained. Every product route returned 404 for owner and non-owner, and none
  of those calls changed the database.

**Result:** `PASS`

---

## V-041 — Reject Deleting Another User’s Post

**Requirements:** `DETAIL-020`, `API-034`

### Steps

1. Use requester A.
2. Attempt to DELETE a post authored by requester B.

### Expected

- Deletion is rejected.
- Post remains readable for normal users.

### Execution Evidence — 2026-09-29

- Requester `p2b_user_b` attempted to delete requester `p2b_user_a`'s readable post and received HTTP 403.
- PostgreSQL still reported `is_deleted = false`; the owner could continue reading and later delete the post.

### P6 UI Evidence — 2026-09-29

- The post menu is not rendered on other users' posts, `/posts/{other}/edit` shows no form, and direct PATCH/DELETE still returns 403 (checked for `apr_tester` → Roidl_08 and → mintcream).

### P8-B Current-Candidate Evidence — 2026-09-30

- Non-owner DELETE returned 403 `FORBIDDEN` while the post stayed readable,
  shown for a P8-B post, the UI-created post (`mintcream`), and seed post 2.
  Once a post was deleted, non-owner calls returned 404 before any ownership
  check.

**Result:** `PASS`

---

# Likes

## V-050 — Requester-Specific Like Toggle

**Requirements:** `SEED-010`, `DETAIL-001`, `DETAIL-002`, `DETAIL-003`, `API-006`, `API-027`, `API-028`, `API-030`

### Steps

1. Before any like toggle, fetch a seeded post as requester A and record `isLiked`, `likeCount`.
2. Toggle like once.
3. Fetch again as requester A.
4. Fetch same post as requester B.
5. Toggle again as requester A.

### Expected

- Before the first toggle, requester A receives `isLiked: false`.
- First toggle flips requester A's `isLiked` and adjusts `likeCount` by one.
- Requester B's `isLiked` is independently evaluated.
- Second toggle for requester A reverses the state/count change.

### Execution Evidence — 2026-09-29

- With zero application like rows, requester A read `isLiked: false` and count `0` on the verification post.
- Requester A toggled on, requester B independently read false and toggled on, then A and B each toggled off. Responses and detail reads showed counts `0 → 1 → 2 → 1 → 0` with independent requester state.
- PostgreSQL ended with zero `post_likes` rows for the verification post. The response contained both `isLiked` and derived `likeCount`.
- This passes the backend/API scenario. Visual icon behavior remains a frontend requirement and is not promoted by this evidence.

### P5A UI Evidence — 2026-09-29

- The detail like button toggles, updates the displayed count and the heart (`#FF8DA1` filled when liked), stays consistent with the backend after reload, guards against duplicate requests, and shows requester-specific state after `/dev-user` switching. See "P5A Post Detail / Like / Comment Read Evidence".

### P8-B Current-Candidate Evidence — 2026-09-30

- The "not promoted" note above is historical: P5A closed the visual portion.
- API on post 13: A and B went `856 → 857 → 858 → 857 → 856` with
  independent `isLiked`, and the list reflected the requester state.
- Browser: detail and list heart/count stayed in sync, and `mintcream` saw
  independent state after switching.
- Final state: 856 and 0 like rows on readable posts.

**Result:** `PASS`

---

# Comments

## V-060 — Initial Two Comments

**Requirements:** `DETAIL-004`, `DETAIL-005`, `DETAIL-006`, `DETAIL-007`

### Steps

Test posts with:

- 0 comments
- 1 comment
- 2 comments
- 3+ comments

### Expected

- Comments are newest-first.
- At most 2 comments are initially visible.
- “More” appears only when total >= 3.

### Execution Evidence — 2026-09-29

- Posts 10 (0), 24 (1), 1 (2), 3 (3), and 4 (27) in Chromium at 375×778: at most 2 comments before More, newest-first matching the API, More only for 3 and 27, and exactly one initial `limit=2` comment request per post. The 0-comment state shows only `댓글 (0)`.
- After the review fix, a same-session revisit of post 4 again showed at most 2 comments and made one fresh `limit=2` request (see "P5A Review Fix — Same-Session Revisit").

### P8-B Current-Candidate Evidence — 2026-09-30

- The browser re-verified posts 10/24/1/3/4 (0/1/2/3/27): one `limit=2`
  request each, at most 2 visible in API newest-first order, and More only
  for 3 and 27. Post 2 (1,411) also showed 2 with More.
- A same-session revisit of post 4 again made one fresh `limit=2` request and
  0 continuations.

**Result:** `PASS`

---

## V-061 — More Then Infinite Scroll

**Requirements:** `DETAIL-008`, `API-031`, `API-032`

### Steps

1. Open a post with enough comments for multiple pages.
2. Confirm only initial 2 are visible.
3. Activate “More”.
4. Scroll to load subsequent pages.

### Expected

- Additional comment pagination begins only after “More”.
- Pages are bounded.
- No duplicate comments appear.
- Comments remain newest-first.

### Execution Evidence — 2026-09-29

- No comment request beyond the initial `limit=2` page before More, even after scrolling the sentinel into view. After More, bounded `limit=20` pages chained by `nextCursor` loaded through the sentinel until `nextCursor: null`, with newest-first order, no duplicate, and no request after the end (posts 3, 4; incremental loading on post 2 with 1,411 comments).
- The same gate holds on a same-session revisit after previously loading continuation pages: 0 continuation requests before More and no bulk exposure of cached comments on More (fix `ddc1a9d`).

### P8-B Current-Candidate Evidence — 2026-09-30

- Browser: no request before More even after scrolling. After More, `limit=20`
  pages chained by `nextCursor` rendered all 3 and all 27 comments in API
  order, More disappeared, and nothing followed the null cursor. Post 2 loaded
  in bounded `limit=20` pages.
- API: complete bounded traversals of post 2 (1,411) at `2`→`50` (30 pages)
  and `2`→`20` (72 pages) exactly matched PostgreSQL with no duplicate or skip.

### Later Evidence — 2026-10-02/03

- The continuation size is now 30 (`limit=2` then `limit=30`). On `7da54e8`:
  post 8 traversed `2 → 30 → 30` to 58/58 in backend order with no request
  after the end (PR #45); post #2 loaded all 1,411 comments in 47
  continuation requests with no duplicate, and the gate still sent nothing
  before More (`docs/PERFORMANCE.md` §15, "F-05").

**Result:** `PASS`

---

## V-062 — Create Comment

**Requirements:** `DETAIL-009`, `API-008`

### Steps

1. Create a comment as current requester.
2. Refresh/re-fetch detail.

### Expected

- Comment persists with current requester as author.
- Comment appears according to newest-first ordering.
- `commentCount` increases by one.

### Execution Evidence — 2026-09-29

- Requester `p2b_user_b` created comment ID `2077` and received HTTP 201 with the requester as author.
- The next comments read returned it first, PostgreSQL retained it as readable, and the parent post's derived `commentCount` changed from `0` to `1`.
- Blank comment content returned HTTP 400 `INVALID_CONTENT`.

### P5B UI Evidence — 2026-09-29

- Browser creation through the fixed composer at 0→1, 1→2, 2→3 (More appears), 27→28, and while expanded over multiple pages. Server-confirmed only: failures keep the input and counts, and a same-tick double submit sends one POST. See "P5B Comment Mutation Evidence".

### P8-B Current-Candidate Evidence — 2026-09-30

- Browser: `apr_tester` on post 1 made one POST, and the comment appeared
  first with the requester as author. The heading went 2→3 with More, and the
  list count went +1.
- `mintcream` created a comment authored as `mintcream`.
- API: 201 with the exact comment shape, newest-first, and derived
  `commentCount` +1.

**Result:** `PASS`

---

## V-063 — Soft Delete Own Comment

**Requirements:** `DEL-002`, `DEL-005`, `DEL-006`, `DETAIL-010`, `DETAIL-011`, `API-009`

### Steps

1. Create or identify own comment.
2. Record owning post `commentCount`.
3. Delete comment.
4. Verify DB row remains with deleted marker/state.
5. Re-fetch comments and post detail.

### Expected

- DB row remains.
- Comment disappears from comment list.
- Post `commentCount` decreases by one.

### Execution Evidence — 2026-09-29

- The comment author deleted comment ID `2077` and received HTTP 204.
- PostgreSQL retained the row with `is_deleted = true`; comments `total` and the parent post's derived `commentCount` both changed from `1` to `0`.
- A repeated delete returned HTTP 404.

### P5B UI Evidence — 2026-09-29

- Browser delete through the confirmation dialog at 3→2 (More disappears), 2→1, 1→0, inside a continuation page, and at the cursor-boundary row. Each comment disappeared, and the comment total, detail count, and cached list count dropped by one and matched the server after reload. See "P5B Comment Mutation Evidence".

### P8-B Current-Candidate Evidence — 2026-09-30

- Browser: cancel sent no DELETE, and confirm sent exactly one. The comment
  disappeared and the heading and list count returned 3→2. The row stayed
  `is_deleted = true`, and a repeat DELETE returned 404.
- `mintcream` deleted their own comment through the UI and the count
  returned.

**Result:** `PASS`

---

## V-064 — Reject Deleting Another User’s Comment

**Requirements:** `DETAIL-010`, `API-034`

### Steps

1. Use requester A.
2. Attempt to DELETE a comment authored by requester B.

### Expected

- Deletion is rejected.
- Comment remains readable.

### Execution Evidence — 2026-09-29

- Post owner `p2b_user_a` attempted to delete `p2b_user_b`'s comment and received HTTP 403, proving post ownership does not imply comment ownership.
- PostgreSQL still reported the comment as readable; requester B then deleted their own comment successfully.

### P5B UI Evidence — 2026-09-29

- The UI renders `삭제` only for the requester's own comments (checked for `apr_tester` and `mintcream`, including own comments on other users' posts). The backend still returns 403 for a cross-user DELETE.

### P8-B Current-Candidate Evidence — 2026-09-30

- `apr_tester` saw mintcream's comment without `삭제`. A direct `apr_tester`
  DELETE returned 403 with the row unchanged.
- The post owner's DELETE of another requester's comment and a non-owner's
  DELETE of a seed comment both returned 403.

**Result:** `PASS`

---

## V-065 — Seed-Deleted Comments Do Not Count

**Requirements:** `SEED-004`, `SEED-008`, `SEED-009`, `DEL-006`

### Steps

1. Choose a post with one or more comments listed in `deletedCommentIds`.
2. Calculate expected readable comment count from seed data.
3. Fetch post detail/list item.
4. Fetch comment list.

### Expected

- Deleted seed comments are absent from comment API.
- `commentCount` equals readable comments only.

### Execution Evidence — 2026-09-29

- All 39 readable posts' API `commentCount` values matched PostgreSQL `comments WHERE is_deleted = FALSE` counts.
- Full comment traversal for every readable post exactly matched PostgreSQL readable IDs/order; none of the 81 seed-deleted comment IDs appeared.

### P8-B Current-Candidate Evidence — 2026-09-30

- Representative traversals (0/1/2/3/27/1,411) matched PostgreSQL readable
  rows, and `total` and detail `commentCount` agreed.
- Comments under a deleted post were unreachable (404) and absent from any
  count. The 81 seed-deleted comment IDs are unchanged.

**Result:** `PASS`

---

# API Contract

## V-070 — Post Response Shape

**Requirements:** `API-010` through `API-023`

### Verify

- list envelope contains `items`, `total`
- each post contains required fields
- detail is a single object, not an `items` envelope
- detail includes `isLiked`, `commentCount`
- patch returns updated post object

### Execution Evidence — 2026-09-29

- List responses were JSON envelopes with `items`, full readable `total`, and the implemented `nextCursor` extension.
- Every traversed post item contained exactly `id`, `title`, `content`, `likeCount`, `commentCount`, `isLiked`, `author`, and ISO-8601 `createdAt`.
- Detail returned the same fields as a single object without an `items` envelope.
- P2B mutation requests and responses used JSON. Malformed JSON returned HTTP 400 with `{ "code": "INVALID_REQUEST", "message": "Request body is invalid." }`.
- PATCH returned the same complete single-post object shape with updated content, requester-specific `isLiked`, and derived `commentCount`.

### P8-B Current-Candidate Evidence — 2026-09-30

- Current list pages were exactly `{ items, total, nextCursor }` with items of
  exactly the eight required fields and ISO-8601 `createdAt`. Detail was
  byte-identical to the list item without an envelope. PATCH returned the full
  updated object, and the like toggle returned exactly `{ isLiked, likeCount }`.
- Errors were JSON `{ code, message }` without HTML.

**Result:** `PASS`

---

## V-071 — Comment Response Shape

**Requirements:** `API-024`, `API-025`, `API-026`

### Verify

- comment list contains `items`, `total`
- comment item contains `id`, `postId`, `content`, `author`, `createdAt`

### Execution Evidence — 2026-09-29

- Every comment page used the `items`, full readable `total`, and `nextCursor` envelope.
- All traversed comment items contained exactly `id`, `postId`, `content`, `author`, and ISO-8601 `createdAt`.

### P8-B Current-Candidate Evidence — 2026-09-30

- Every comment page in the representative and 1,411-comment traversals used
  the `items`/`total`/`nextCursor` envelope. Every item had exactly the five
  required fields with the matching `postId`.

**Result:** `PASS`

---

# UI / Visual Verification

## V-080 — 375px Mobile Layout

**Requirements:** `UI-001`, `UI-002`, `UI-003`

### Steps

Inspect required screens at 375px viewport:

- post list
- post detail
- create post
- edit post
- post action menu/bottom sheet

### Expected

- Required functionality fits and remains usable at 375px.
- No desktop-specific layout is required.

### P4 Partial Evidence — 2026-09-29

- Post list only, at `375 × 778`: document `scrollWidth` 375 (no horizontal scroll), 48px header, list at y=56, 118px rows with a 343×93 content region at x=16, and the 50×50 FAB at x=310, y=679 not blocking the last row (120px bottom padding).
- Detail, create, edit, and action-sheet screens do not exist yet, so this scenario is not complete.

### P5A Partial Evidence — 2026-09-29

- Post detail at `375 × 778`: 375×48 sticky navigation, content from y=56, document `scrollWidth` 375. Create, edit, and action-sheet screens do not exist yet.

### P5B Partial Evidence — 2026-09-29

- Detail with the fixed 375×86 composer at y=692 and the 327×264 delete dialog at (24, 257): no horizontal overflow, including with a long unbroken comment. Create, edit, and action-sheet screens are still missing.

### P6 Evidence — 2026-09-29

- All required screens now exist and were checked at `375 × 778`: list, detail, own-post action sheet, post delete dialog, create (long content scrolled to the fixed action), and edit. Each had document `scrollWidth` 375 (no horizontal scroll), with usable fixed controls that do not cover content. Earlier partial evidence covered the list (P4), the detail (P5A), and the composer and comment dialog (P5B).

### P8-B Regression Evidence — 2026-09-30

- The earlier "not complete" partial notes are historical.
- A continuous Chromium path with a true 375px viewport (`innerWidth` 375)
  covered `/dev-user` → list → infinite scroll → detail → like → comment
  create/delete → create → toast → edit → delete → list. Every screen,
  including the comment dialog, action sheet, and post dialog, had
  `scrollWidth` 375.
- The FAB, comment composer, and form action bar stayed fixed inside the
  viewport. There were no page or console errors and no 4xx/5xx responses.

**Result:** `PASS`

---

## V-081 — Official Design Fidelity

**Requirements:** `UI-004`

### Prerequisite

Official design source is available at Figma file `Ee1gp5NC6G1sQIvssuu4Sr`, Page 1 (`0:1`). The design audit and frame mapping are recorded in `docs/UI_SPEC.md`.

### Verify

- typography
- spacing
- colors
- radii
- icons
- disabled state
- floating write button
- post action menu/bottom sheet
- toast

### Expected

Implementation matches official design as closely as practical, with any intentional deviations documented.

### P4 Partial Evidence — 2026-09-29

- Limitation: Official Figma node had been previously audited into docs/UI_SPEC.md, but a fresh MCP comparison was unavailable because the Starter-plan tool-call limit was exhausted.
- Measured in Chromium against the `docs/UI_SPEC.md` `DESIGN_DEFINED` values for node `755:8831`: frame regions (header 48, list y=56, row 118, content 343×93 at x=16/y=+12, divider 343×1), avatar 18×18 circular, FAB 50×50 at x=310/y=679, app title Bold 20/28 `#292828`, post title Medium 16/24 `#292828`, preview Regular 14/18 `#979EA9`, metadata Regular 12/16 with author `#5E5E5E` and counts/time `#979EA9`, Pretendard loaded.
- This proves conformance to the recorded spec values, not to the Figma frame. Title/preview clamps, metadata gaps, icon paths/sizes, FAB red/shadow, and inner spacing are `PRODUCT_DECISION` values recorded in `docs/UI_SPEC.md` and were not compared with Figma. The divider uses the existing `--color-border-light` token as an implementation choice, not a Figma-exact mapping.
- Only the list screen exists. The result remains `NOT_RUN`.

### P5A Partial Evidence — 2026-09-29

- Detail typography matched the `docs/UI_SPEC.md` `DESIGN_DEFINED` values (title Bold 18/26, author Bold 15/22, body Regular 14/18, heading Bold 16/24, `#292828`, liked `#FF8DA1`). Layout offsets, avatar sizes, comment typography, icons, and the More button were estimated from local PNG exports of the official frames (`755-10420`, `755-10774`, `755-12645`, `755-13104`, `769-142`) and are `PRODUCT_DECISION` values, not Figma node measurements. Comparing against local PNGs does not constitute V-081; the result remains `NOT_RUN`.

### P5B Partial Evidence — 2026-09-29

- Composer region, dialog geometry, and dialog copy follow `docs/UI_SPEC.md`. Internal spacing, colors, and the `등록` control are PNG-estimated `PRODUCT_DECISION` values (local exports `755-12175`, `755-13104`, `755-12645`), not Figma node measurements. The result remains `NOT_RUN`.

### P6 Partial Evidence — 2026-09-29

- Create/edit fixed action (343×56 at y=690 in the 68 + 26 region), the action sheet (375×224 at y=554), the post delete dialog (327×264 at 24, 257), and the toast (181×40 at 97.5, 657) match `docs/UI_SPEC.md`. Other values are PNG-estimated `PRODUCT_DECISION`s. No fresh Figma comparison was run, so the result remains `NOT_RUN`.

### P7-A Local SVG and Chromium Evidence — 2026-09-30

- `755-8831-post-list.svg` provides a 375×778 viewBox with preserved text and
  geometry: 118px rows, 18px avatar/author row, 16/24 title, 14/18 preview,
  12/16 metadata, and a divider at x=16/y=173. The list now follows the SVG
  hierarchy avatar/author → title → preview → metadata without changing link,
  query, cache, or pagination behavior.
- The SVG directly supplies filled neutral comment/heart glyphs in `#DADEE3`;
  the heart also uses a `#DADEE3` 1.4px stroke. Chromium DOM evidence confirms
  the list uses those vectors and switches only the heart to `#FF8DA1` when
  `post.isLiked` is true. The count text remains neutral.
- `npm run typecheck`, `npm run build`, `git diff --check`, Compose frontend
  rebuild, and `/health` passed. Chromium rendered the corrected ordering,
  neutral rows, and an `isLiked: true` pink heart at the list's first row.
- Chrome/Edge headless capture has a minimum CSS viewport wider than 375px in
  this environment, clipping the right-aligned time and fixed FAB from its
  375px bitmap. DOM/CSS evidence confirms both elements remain present with
  their existing right-aligned/fixed rules. This is not fresh Figma MCP
  comparison evidence; V-081 remains `NOT_RUN`.

### P7-B Cross-screen Runtime Regression — 2026-09-30

- Chrome DevTools device metrics in one continuous browser session provided
  `innerWidth=375`, `clientWidth=375`, `devicePixelRatio=1`, and document
  `scrollWidth=375`. The previous `--window-size=375,778` headless launch
  instead reported `innerWidth=500`; this is a tooling minimum-window
  limitation, not a product layout defect. Product CSS was not changed for it.
- List: rows remained 118px at y=56/174/292; the first row preserved the
  SVG-backed author/title/preview/metadata order, x=16 18px avatar, right
  aligned time ending at x=359, 343px metadata region, filled neutral glyphs,
  pink liked heart only, 50px FAB at (310,679), sentinel, and no overflow.
  Existing long title/preview rows clipped to one line without row drift. The
  seeded data has no long-author fixture; its existing truncation rule was
  inspected without inventing test data.
- Detail: own liked/unliked state, nav, byline, post body, fixed composer
  (0,692,375x86), action sheet (0,554,375x224), and post/comment dialogs
  (24,257,327x264) rendered without overflow. Escape and scrim close worked.
  Other-user posts had no owner menu; 1,411/1/0-comment states rendered 2/1/0
  initial comments with More only for the many-comment state.
- Create/edit: empty create kept its bottom action disabled; a valid create
  showed a one-shot 181x40 toast at (97,657), clear of the 50px FAB, which
  expired. The test post was opened, prefilled in edit, saved back to detail,
  and then deleted through its UI dialog; it was absent from the list.
- Functional smoke: post 43 like changed true/1 -> false/0, showed neutral in
  the list, then restored true/1; list sentinel loaded 10 -> 20 unique rows;
  comment create/delete changed 0 -> 1 -> 0 through the UI; `/dev-user`
  switched to `Roidl_08` (post 2 menu shown) and back to `apr_tester` (same
  menu hidden). Compose services were healthy and `/health` returned
  `{"status":"ok","db":"up"}`. The first attempted comment automation
  addressed a nonexistent textarea and made no request; the corrected input
  run above passed.
- Finding classification: all inspected items `NO_CHANGE`, except the raw
  headless window-size behavior, which is `TOOLING_LIMITATION`. No clear
  `FUNCTIONAL_BUG`, `DESIGN_MISMATCH`, or `P7_A_REGRESSION` justified a product
  code change. This remains local PNG/SVG/runtime evidence only; V-081 stays
  `NOT_RUN`.

### P8-B Status — 2026-09-30

- P8-B was behavioral acceptance only and ran no fresh comparison against the
  official Figma file `Ee1gp5NC6G1sQIvssuu4Sr`. Its 375px runtime regression
  is not design-fidelity evidence.
- V-081 remains the outstanding final-sign-off item.

### P8-C Note — 2026-09-30

- P8-C implemented the intentional UX retouch in `design/design-retouch/`
  (`PRODUCT_DECISION`, not official Figma evidence). Those list and detail
  changes deliberately deviate from `design/design-export/`, so they are not
  fidelity evidence.
- A later V-081 comparison must record them as intentional deviations. These
  include replacing the official own-post bottom sheet (830:139) with a
  non-modal popover anchored to the post `⋮`; the delete confirmation dialog
  and its scrim are unchanged.
- Focused 375px regression on the retouched build passed (list and infinite
  scroll, like sync, More gate, comment and post CRUD, ownership menu,
  `/dev-user` switch, continuous E2E; no overflow and no errors). Behavior is
  unchanged.

### Status — 2026-10-03 (unchanged: `NOT_RUN`)

- After P8-C, runtime and visual regressions passed several times at 375×778
  (accessibility contrast PR #35, first-page SSR PR #36, prefetch PR #38, the
  edge/failure audit PR #45, and the clean-clone gate "F-16"). These are
  runtime and layout checks against the implementation and `docs/UI_SPEC.md`,
  not a fresh comparison of the final implementation with the official Figma
  file `Ee1gp5NC6G1sQIvssuu4Sr`. No such comparison was run, so V-081 stays
  `NOT_RUN`.
- A later V-081 comparison must also record this intentional deviation: PR
  #35 kept the design secondary `#979EA9` as the source value but renders text
  users must read (list preview/counts/time, unselected sort, search label,
  dates, like count, comment time/count/actions, dialog body, status notices,
  placeholders) and meaningful icons/focus borders in `#6E7785` (4.53:1 on
  white); the dialog cancel label uses `#5E5E5E` on `#EAECEE`. Disabled,
  read-only, and press-feedback states keep `#979EA9` (`docs/UI_SPEC.md`
  §6.3).

**Result:** `NOT_RUN`

---

# Final Documentation

## V-090 — README Accuracy

**Requirements:** `DOC-001` through `DOC-009`

### Steps

Review README against the final code.

### Expected

- Technical choices match actual dependencies/implementation.
- Server-state description matches actual code.
- Layering description references real files/functions.
- Seed explanation matches actual initializer/schema.
- Tests are described accurately.
- Trade-offs reflect actual decisions.
- UX deviations/improvements are documented if applicable.

### Execution Evidence — 2026-10-01

The README was reviewed against `main` at `ef8bb75`. It was synchronized with
CI, read cancellation and dialog focus containment, then replaced
`README.md`; the staging draft was removed.

- **Files and functions:** every named class exists:
  - `PostController`, `CommentController`;
  - `PostReadService`, `PostMutationService`, `CommentMutationService`;
  - `PostRepository`, `CommentRepository`, `PostLikeRepository`;
  - `IdentitySequenceRepository.synchronizeWithSeededRows()`;
  - `SeedInitializer`, `SeedHistoryRepository`, `SeedReadiness`;
  - `RequesterResolver`, `MutationValidator`, `ApiExceptionHandler`.

  The named frontend files also exist: `keys.ts`, `postListCache.ts`,
  `useInfiniteScrollTrigger`, `features/post-form`, `client.ts`,
  `community.ts` and `ConfirmDialog.tsx`.
- **Behavioral claims match the code:**
  - `@Transactional` `SeedInitializer.run()` and the `assignment-v1` marker;
  - readiness opened in `afterCommit`, and `/health` 503 `starting`;
  - `V1` identity columns, `is_deleted`, `base_like_count`, and the
    `post_likes(post_id, user_id)` primary key;
  - row-value `(created_at, id) < (…)` in both repositories;
  - `ILIKE … ESCAPE`, `limit + 1`, and the 20-code-point title rule;
  - `FOR UPDATE` before the ownership checks;
  - the `postListCache.ts` invalidation rules;
  - page sizes 15, 2 and 30, the 200px `rootMargin`, and the 300 ms
    debounce; *(Historical for 2026-10-01. The post list now passes
    `rootMargin` 500px (PR #38); 200px remains the shared default used by
    comments. See "F-04".)*
  - `gcTime: 0`, `prefetch={false}` on detail links only, and the 108px row,
    44px toolbar and 144px popover;
  - the likes-list dedupe;
  - the `AbortSignal` path from query function to `request()` to `fetch`;
  - the `ConfirmDialog` Tab and pending focus handling;
  - the CI job steps.
- **Measurements:**
  - SQL-plan figures match `docs/PERFORMANCE.md`.
  - Page-size request counts follow from the data: 39 posts give 4/3/2, and
    1,409 continuation comments give 141/71/47. The applied `15/15/9`
    traversal is in PR #23.
  - The comparison-only figures (p95 248 ms, 424 B, the 250 ms pause, 8 vs
    13 rows below the fold) come from the earlier measurement session and are
    not recorded elsewhere in the repository.
  - *(Superseded as the basis for the page-size decision. The 39-post
    whole-traversal comparison included an end-of-data effect. Post sizes
    were re-measured after first-page SSR on a disposable 160-post dataset at
    fixed depths (`docs/PERFORMANCE.md` §13, kept 15/15), and comment
    continuation at fixed depths on post #2 (§15, kept 30). See "F-03".)*
- **Links:** all 11 external references returned 200, and their 4 fragment
  anchors exist.
- **Clean text:** no PR numbers, SHAs, phase names, requirement or scenario
  IDs, status labels, or staging text remain in `README.md`.
- **Architecture document:** `docs/ARCHITECTURE.md` was audited against the
  same code so it does not contradict the README.
  - ADR-004 now shows the shipped `V1` schema: `is_deleted BOOLEAN`, no
    `updated_at`, no `post_likes.created_at`, `TEXT` authors. The `deleted_at`
    proposal is kept as a rejected alternative.
  - ADR-002, 003, 010 and 013 move from `OPEN`/`PROVISIONAL` to `DECIDED`,
    with implementation references.
  - The early backend/frontend structure proposals are replaced by the actual
    package and folder trees.
  - P0 and phase-update sections are marked as history, and the earlier P1–P3
    open-decision table is reconciled. One item stays open: there is no
    committed DB integration-test suite.
  - *(Superseded. PR #42 added a committed PostgreSQL integration-test suite
    (Testcontainers `postgres:16-alpine`, real Flyway `V1` and
    `SeedInitializer`), and PR #44 added concurrency tests to it: 6 suites /
    31 tests on `7da54e8`. See "F-07" and "F-09".)*
- **Not covered:** this is a document-accuracy review. It does not re-run
  runtime acceptance, and it does not make `V-081` (official design fidelity)
  pass.

**Result:** `PASS`

---

# Final Engineering Verification — 2026-10-02/03

This part records the engineering verification done after V-090, up to
`main` @ `7da54e8a361d2b8212a46e8b9e8091b8240ce3fa`. Each section lists what
was checked, the method, the key evidence, the result, and its limits.
Detailed measurement tables live in `docs/PERFORMANCE.md` §13–§17. PR bodies
(#40–#47) hold the full audit logs; temporary scripts and disposable stacks
were never committed. Unless stated otherwise, browser runs used Chromium at
375×778, and mutations ran only against disposable Compose projects or
Testcontainers databases, never the persistent default volume.

**Backend test count history (each correct for its date):** 26 (P2A) → 49
(P5B/P6) → 51 (P8-B) → 60 (post discovery, CI) → 83 (PR #42, +23 PostgreSQL
integration tests) → **91** (PR #44, +8 concurrency tests; current).
**Frontend test history:** none → 15 Vitest tests (PR #43) → **21 tests / 4
files** (PR #45; current).

## F-01 — First-Page SSR and Lighthouse

**What:** whether server-rendering the default list's first page shortens the
real first-content path, and how Lighthouse should be read for this app.

**Before SSR (client-only list).** On `main` before PR #36 the list HTML held
only the loading text; the browser fetched `/api/posts` after hydration.
Lighthouse 13.5.0 CLI, `--throttling-method=devtools`, default mobile
412×823 (DPR 1.75), Docker production frontend:

- 2026-10-02 baseline on `80cc2fb`: Performance 75/79/76, LCP
  3,439/3,365/3,464 ms; LCP element the first post preview; browser
  `/api/posts` 1.
- 2026-10-02 on `d5d545a` (3 runs): Performance median 77, LCP median
  3,467 ms (3,380–3,624), TBT median 472 ms, browser `/api/posts` 1.
- Trace: HTML ≈0.6 s → JS ≈2.0 s → `/api/posts` start ≈2.3 s → response
  ≈2.9 s (≈25 ms server work, rest the 562.5 ms emulated latency) → LCP.
  The request could not start before hydration.

**SSR implementation (PR #36).** `app/page.tsx` fetches the default first
page server-side and passes it as `initialData`; later pages, likes, and
search stay client requests. Six Lighthouse runs (same method, 2026-10-02):

| Run | 1 | 2 | 3 | 4 | 5 | 6 | Median |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Performance | 93 | 87 | 94 | 92 | 93 | 93 | 93 |
| LCP (ms) | 2,401 | 2,819 | 2,313 | 2,509 | 2,194 | 2,183 | 2,357 |
| TBT (ms) | 147 | 205 | 120 | 130 | 200 | 192 | 169.9 |

- A11y / Best Practices / SEO 100/100/100 in all six; CLS 0.0003; browser
  initial `/api/posts` 0; LCP element `p.PostListItem_preview`; `/` First
  Load JS 121 kB (build), `/` changed from static to dynamic.
- Functional checks (both requester cases): SSR HTML 15 rows, `isLiked`
  equal in SSR HTML / DOM / backend, 39 rows on scroll without duplicates,
  likes, search, three back-navigation paths, hydration errors 0 (including a
  focused 59 s / 59 min 59 s / 23 h 59 min 59 s relative-time test that failed
  on a `Date.now()` control).
- Failure fallback at the time (1.5 s timeout): connection refused → 200 in
  0.04–0.18 s, blackhole → 200 in 1.54–1.60 s, non-2xx → 200; each recovered
  through the client fetch, and SSR plus browser failure showed the existing
  error/retry UI.

**Accessibility and SEO (PR #35, before SSR).** Lighthouse Accessibility 96 →
100 (`color-contrast`) and SEO 91 → 100 (`meta-description`), Best Practices
100 unchanged, on all three runs and also on `/posts/1` and `/posts/new`.

**Font payload audit (2026-10-02, `d5d545a`, not a code change).** The
2,057,688 B Pretendard variable file is byte-identical to the official
`pretendard@1.3.9` file and covers all 11,172 modern Hangul syllables. The
official variable dynamic subset (92 files) cut font transfer to 344,778 B
(13 requests) and raised simulated Performance 73 → 82, but under devtools
throttling it made LCP 3,477 → 4,007 ms and TBT 517 → 755 ms (render-blocking
CSS 1.0 → 13.7 KB and 13 font requests competing with the API and JS). With
the current file, blocking `*.woff2` did not change LCP (3,477 vs 3,604 ms),
so it is not on the LCP path. Decision: `KEEP_CURRENT`.

**Simulated vs devtools throttling.** The same PR #36 build measured with
Lighthouse's default simulated throttling gave LCP 12,179 / 12,197 /
12,203 ms and Performance 56. The simulation counts the 2 MB font download
in its LCP graph, while the observed paint does not wait for the font (the
font finishes at about 14 s, well after the content paints). A later ≈12 s
simulated result on the current build (reported with Chrome 154 and
Lighthouse 12.8.2) therefore matches the historical simulated behavior and
is not by itself a regression. Simulated throttling is not "wrong": the two
methods apply different models, and results are comparable only under the
same browser, Lighthouse version, throttling method, screen, and dataset. The
regression check for this app uses devtools throttling (F-15).

## F-02 — SSR Seed Freshness and Server Timeout (PR #41)

Details: `docs/PERFORMANCE.md` §17.

- `SEED_FRESH_MS` 30 s → **5 s**. Throttled hard loads: 0 s caused a hydration
  duplicate fetch in 5/5 runs; 5, 15, and 30 s each had 0/5. The observed
  seed age at hydration was about 1.65 s, so 5 s was the shortest passing
  candidate. After any client fetch the list's staleTime is 0 again.
- `SERVER_FETCH_TIMEOUT_MS` 1.5 s → **2 s**. With a 1.8 s server-only delay,
  the 2 s budget gave a real-post LCP of 2.98 s versus 4.04 s for the 1.5 s
  fallback. On a blackhole backend 2 s waits 0.54 s longer and still returns
  HTTP 200 with the client fallback.
- Verified after the change: SSR HTML 15 rows, browser initial list request
  0, page errors 0. PR #45 re-checked the fallback with a real `docker pause`
  of the backend: `/` returned 200 at about 2.2 s with no rows, the client
  fetched the first page, and hydration errors were 0.
- Trade-off: after 5 s a focus or remount may refetch; a hung backend delays
  the HTML by up to 2 s.

## F-03 — Page-Size Decisions

- **Posts — `KEEP_15_15`** (`docs/PERFORMANCE.md` §13, 2026-10-02). The
  earlier 39-post whole-traversal comparison (V-090 measurements) is
  historical. After SSR, the SSR size and the continuation size were measured
  separately on a disposable 160-post dataset (seed distributions kept) at
  375×778, CPU 4×, devtools mobile throttling:
  - SSR 10/15/20: LCP median 2,278 / 2,260 / 2,372 ms with overlapping ranges
    (noise); +5 rows ≈ +0.9 KB compressed HTML and +100 DOM elements; backend
    fetch and Next TTFB differences ≈10 ms. 15 keeps 8 rows (~1.2 screens)
    below the fold.
  - Continuation 10/15/20 at fixed depths of 30 and 45 posts: 10 needed the
    most requests and was the only candidate whose loading text and blank
    area appeared at reading speed; 20 gave no consistent gain over 15 and
    would need mixed-size branching.
  - The size is not derived from the total row count: cursor pagination reads
    `limit + 1` rows per request, and what matters is how much one session
    consumes, not how many posts exist (42, 1,000, or 100,000).
- **Comments — initial 2 (product contract), continuation `KEEP_30`**
  (`docs/PERFORMANCE.md` §15, 2026-10-03, post #2 with 1,411 comments).
  First More click → rows 639 / 660 / 670 / 725 ms for 10/20/30/50; requests
  to 120 comments 13 / 7 / 5 / 3; extra loading appearances while skimming
  8 / 2 / 1 / 2; unused comments on a 25-comment early exit 6 / 16 / 6 / 26.
  No candidate gave a reason to change 30.
- Limits: one emulated device and network profile, scripted scroll speeds,
  HTTP/1.1 `next start`; only the listed candidates were compared, so these
  are the most reasonable compared values, not global optima.

## F-04 — Prefetch Distance (`rootMargin`)

`docs/PERFORMANCE.md` §14 (PR #38, 2026-10-02). The post list passes
`0px 0px 500px 0px`; the hook default stays 200px and is still used by
comments. Earlier "200px" statements for the post list are historical.

- Request → rows took about 0.66 s (API 600–611 ms + DOM 48–78 ms) for every
  candidate; at a ≈600 px/s skim that is about 400 px of travel.
- Fast skim to 60 posts (5 runs): 200px waited at every page boundary (15 of
  15 cycles, longest 305 ms, cumulative 670 ms); 300px at half (10 of 20,
  cumulative 419 ms); 400px no wait but a 157–230 ms blank area in every run;
  500px no wait and no blank area (loading text once, 171–272 ms).
- Reading speed: no candidate waited. No candidate requested a page before
  scrolling, chained requests, or loaded more on an early exit; the cost of
  500px is at most one page (15 rows, 5.6 KB) fetched ahead at checkpoints.
- Branch verification on the 160-post dataset: no request without scrolling,
  no wait or blank area while skimming, and a full 160-post traversal with no
  duplicate or missing post and no request after the end; production
  regression (SSR, likes, search, back navigation) passed.
- Larger values were not tested. From the measured geometry (sentinel 934 px
  below the first 375×778 viewport), a margin of about 934 px or more, such as
  1,000 px, would request page 2 before any scroll, so a larger margin is not
  automatically better. Viewports about 1,212 px tall or more would also
  prefetch page 2 once with 500px (geometry, not measured).
- Comments keep 200px: reading showed no waits; skimming showed one short wait
  (158–201 ms) in 120 comments (`KEEP_COMMENT_ROOT_MARGIN_200_FOR_NOW`).

## F-05 — Comment Scaling and Virtualization

`docs/PERFORMANCE.md` §15. Post #2 holds the largest thread (1,411 readable
comments).

- Full load at continuation 30 (3 runs, 47 continuation requests each): DOM
  about 10,000 elements and JS heap about 11–14 MB at 1,411 comments.
  Response → DOM per append rose from about 30 ms to 65–88 ms; long tasks
  over 50 ms went from 0–1 per window up to 600 comments to 13–14 (longest
  72–82 ms) between 1,000 and 1,411.
- Scrolling did not degrade: at every checkpoint the probe updated scroll
  position within 10 ms and showed no frame over 50 ms after the first
  checkpoint.
- `gcTime: 0` re-checked: revisiting after 122 or 1,411 loaded comments sent
  one `limit=2` request, rendered 2 comments with More, and sent no
  continuation for 3.5 s.
- Decision: `VIRTUALIZATION_NOT_NEEDED_NOW`. For this dataset and profile the
  measured cost (append long tasks past about 1,000 comments, under 100 ms)
  did not justify a separate rendering layer. This is not a general claim
  that virtualization is unnecessary; the growing append cost is the signal
  to revisit with longer real conversations or slower devices.

## F-06 — Client Read Retry Policy (PR #40, runtime in PR #45)

Details: `docs/PERFORMANCE.md` §16. One shared QueryClient policy
(`retryOnceUnlessDeterministicClientError`): `ApiError` 4xx → no retry;
network, 5xx, parse, and unknown read failures → one retry with the default
1 s delay. Mutations keep TanStack Query's default of 0 retries.

| Failure | Requests | Result |
| --- | ---: | --- |
| 404 detail | 1 | not-found, no retry |
| 400 continuation (`INVALID_CURSOR`) | 1 | next-page error, manual retry works |
| 500 then success | 2 | success, error UI never shown |
| network then success | 2 | success, error UI never shown |
| invalid JSON then success | 2 | success, error UI never shown |
| 500 twice / invalid JSON twice | 2 | terminal error, manual recovery, no unhandled rejection |
| cancelled (superseded or unmounted) | — | aborted, not retried, no error state |

- Why: reads are idempotent, so one retry absorbs transient faults without
  holding users for several seconds; a write retry could duplicate side
  effects without idempotency keys.
- Limit: every 4xx is treated as deterministic. If a retryable 4xx such as 429
  is introduced, this classification must be revisited.

## F-07 — Real PostgreSQL Integration (PR #42)

- **Method:** `PostgresIntegrationTest` starts one `postgres:16-alpine`
  Testcontainers container per test JVM; the full Spring context runs the
  real Flyway `V1` migration and the real `SeedInitializer` with
  `seed/seed.json`, and tests call the production `JdbcClient` repositories
  and services. Expected orderings are computed in Java from raw
  `posts`/`post_likes` rows (raw SQL/JDBC oracle). Tests roll back.
- **Scope:** latest keyset with same-`created_at` ties across a page boundary
  and changing limits (2→3→1→5); likes keyset with effective-count ties and
  `created_at`/`id` tie-breaks; search (case-insensitive, title/content,
  literal `%`, `_`, `\` with decoys, deleted posts excluded from results and
  total); `base_like_count` + relational likes aggregate; requester
  `isLiked`; readable `commentCount`; comment keyset with ties and changing
  sizes and the largest thread fully traversed; seed imported exactly once
  with matching counts and deleted flags; identity sequences ahead of
  `MAX(id)`; schema constraints (duplicate like PK, comment FK,
  `base_like_count >= 0`); mutation persistence with owner/non-owner/missing
  outcomes.
- **Counts at the time:** 83 backend tests, 23 of them new PostgreSQL
  integration tests (now 91 / 31 with F-09).
- **Sensitivity:** three bugs injected into `PostRepository` were each caught
  (an `id ASC` tie-breaker, a likes sort ignoring `post_likes`, disabled
  backslash escaping). Removing only the explicit `ESCAPE '\'` is not
  detected, which is expected: backslash is already PostgreSQL's default LIKE
  escape, so behavior does not change.
- **Dependency note:** Testcontainers is pinned to `1.21.4` because the
  Boot-managed 1.21.0 requested Docker API 1.32, which Docker Engine 29
  rejects. Without a Docker daemon these tests fail rather than skip.

## F-08 — Frontend Cache Consistency (PR #43)

- **Method:** a disposable Compose project (`apr-cache-audit`), Chromium at
  375×778, backend REST full-cursor traversal and SQL as the oracle (the
  QueryClient was only inspected, never used as the oracle). **148/148**
  runtime checks passed in one clean run.
- **Scope:** create (single POST for 3 rapid submits, prepend with total +1,
  continuation from the pre-create cursor with 0 duplicates and 0 missing,
  likes/search invalidated), edit (in-place replacement, old and new search
  membership refreshed), like (likes lists re-ranked including across the
  page-1/page-2 boundary), comment create/delete (collapsed and expanded),
  post delete at the likes page-1 cursor row, failure cases (500 and network
  for like, comment, delete, edit, create: one request, no retry, caches
  unchanged, input kept), back navigation and hard refresh, and multiple
  requesters via `/dev-user`.
- **Result:** `CACHE_CONSISTENCY_VERIFIED`. No production code changed: the
  audit confirmed the existing cache strategy and added a Vitest runner with
  15 deterministic tests for `lib/query/postListCache.ts` (each shown able to
  fail by removing a guard), plus `npm test` in CI.
- **Expected behavior noted:** an invalidated search list briefly shows its
  cached rows until the refetch lands; nothing stale persists.

## F-09 — Mutation Concurrency (PR #44, Part A)

- **Method:** `MutationConcurrencyIntegrationTest` (8 tests) on the
  Testcontainers PostgreSQL. Each worker calls the real service proxy in its
  own transaction; a separate JDBC transaction holds `SELECT … FOR UPDATE`
  and workers are released only after `pg_stat_activity` shows them waiting
  on the lock.
- **Scenarios:** same user toggling twice at once, two users liking at once,
  two comment creates, delete committing while a comment create / like
  toggle / update waits (each 404, no row created or changed), owner deleting
  twice (one success, one 404), and an unforced post-delete vs comment-delete
  race (×20 per run, only the two valid orders observed).
- **Sensitivity:** removing `FOR UPDATE` made 4 of 8 tests fail, including
  real `DuplicateKeyException`s from both like-toggle races.
- **Stability:** the class passed 10/10 consecutive runs; `gradlew clean test`
  passed 3/3 (91 tests).
- **Result:** `CONCURRENCY_INVARIANTS_VERIFIED`. No deadlock was observed in
  these tests, and the current lock ordering shows no obvious cycle (a post
  delete locks one row). This is not a proof that deadlocks can never occur.
  Same-post likes serialize on the post row (granularity trade-off).

## F-10 — Mutable Likes Ranking (PR #44, Part B)

- Each likes-sorted page is ordered correctly and the cursor chain always
  terminates, but a traversal across several requests is **not
  snapshot-consistent**: the cursor stores a like count that can change
  between requests.
- Reproduced on a disposable stack with real REST and cursors:
  - **Missing:** after page 1 ended at B, another user's like moved C to tie
    with B but newer, so C ranked above the cursor; the old traversal found
    43 of 44 rows (C missing) and still ended normally.
  - **Duplicate:** after page 1 ended at A, an unlike lowered A; the old
    cursor's page 2 returned A again (44 raw rows, 43 unique).
  - Both patterns also reproduce inside a search; the latest sort under the
    same like churn had 0 duplicates and 0 missing.
- Frontend: the list's dedupe hides duplicates (54 rows, 54 unique ids), but
  it cannot recover a missing row; a fresh traversal from page 1 restores the
  current ranking.
- Decision: `ACCEPTED_MUTABLE_RANKING_TRADEOFF`. Snapshot or versioned
  cursors would add schema, API, and cache complexity beyond this assignment.

## F-11 — Page and Total Under READ COMMITTED (PR #44, Part C)

- A deliberately high-churn stress run (one writer creating and
  soft-deleting posts in a loop, four readers calling one search page for
  30 s) observed 4,204 of 26,288 reads with `items.length ≠ total`, always by
  ±1. (The audit summary reported about 4,018 writes; the PR body records the
  read counts.)
- Cause: under READ COMMITTED the page query and the `COUNT` query are
  separate statements with separate snapshots, so a commit between them can
  shift one by one.
- These numbers come from an artificial stress environment and are not a
  mismatch rate for normal traffic.
- Decision: accepted as current metadata semantics (`total` comes from the
  same request but not the same snapshot); no single-snapshot transaction or
  stronger isolation was added.

## F-12 — Edge, Failure, and Recovery (PR #45)

Disposable stack `apr-edge-audit`, Chromium at 375×778, faults by route
interception plus real `docker stop` / `docker pause` of the backend.

- **Inputs:** malformed post IDs (`0`, `-1`, `1.5`, `abc`, `001`, huge) send
  no request and show not-found; missing, max-safe-integer, and deleted IDs
  send one GET → 404; a non-owner edit shows the no-permission notice with no
  PATCH; `limit` 0/51 → 400 and 1/50 → 200; invalid sort, malformed and
  cross-sort cursors → 400; `q` over 100 code points → 400, exactly 100 →
  200.
- **Terminal states:** a zero-result search shows its message and sends no
  further request; post traversal (latest: SSR page + 2 pages, likes: 3 pages)
  and comment traversal (post 8: `2 → 30 → 30`, 58/58) end with no
  duplicate and no request after the end.
- **Failures and recovery:** post and comment continuation 500/network faults
  make 2 attempts, keep loaded rows, do not auto-retry on scrolling, and
  recover with the manual retry to complete traversals; a failed initial
  search shows the terminal error and recovers.
- **Cancellation:** debounce sends one request for a typed word; superseded
  searches and rapid sort changes are aborted without retry or error UI, and
  the final rows match the backend.
- **Availability:** a stopped backend leads to the terminal error and a
  manual retry after restart succeeds without reload; SSR with a paused
  backend falls back to the client (F-02).
- **Navigation:** history back/forward, create draft back, likes + search +
  detail + edit back restore the view with no mutation; a hard refresh resets
  search/sort as documented (`lib/postListView.ts`).
- **Result:** `EDGE_FAILURE_BEHAVIOR_VERIFIED`; no production bug. Added only
  pure Vitest contract tests (`parsePostId`, retry policy,
  `normalizePostQuery`), bringing the frontend to 21 tests in 4 files.
- **Accepted trade-offs:** the search input's `maxLength=100` counts UTF-16
  units (stricter than the API for emoji); sort cannot change while search is
  open (toolbar design).

## F-13 — Full-Stack CI Gate (PR #46)

- CI jobs on `7da54e8`: `backend` (`./gradlew test` with Testcontainers),
  `frontend` (`npm ci` → `npm run typecheck` → `npm test` → `npm run build`),
  and `full-stack-smoke` (runs after both).
- `full-stack-smoke` (`scripts/ci-full-stack-smoke.sh`) on a clean GitHub
  Actions runner builds the production images and checks: `docker compose
  config`, all services healthy, `/health` `{status: ok, db: up}`, the latest
  post API, the frontend SSR HTML containing that post, `seed_history`
  `assignment-v1` = 1, and the browser API base baked into the frontend
  build.
- Sensitivity: temporarily pointing `API_BASE_URL` at a nonexistent internal
  host made the SSR assertion fail.
- The CI run for the `main` merge commit `7da54e8` succeeded (as did #45 and
  #46).
- Limit: branch protection is not enabled on `main` (GitHub API `protected:
  false`). The gates exist and passed on current `main`; merging is not
  technically blocked on them.

## F-14 — Dependency Security

- **History:** PR #33 updated Next.js/React for security advisories; PR #34
  moved Next.js 15.5.24 → 15.5.27 (Maintenance LTS) and sharp 0.34.5 → 0.35.5
  within Next's declared range (npm audit 3 → 2; the remaining two were
  PostCSS 8.4.31 and Next via PostCSS). PR #47 applied the final patches.
- **PR #47:** a root `overrides` entry pins `postcss` to `8.5.28`, replacing
  the vulnerable `8.4.31` that `next@15.5.27` requests (≥ 8.5.23 patches the
  source-map advisories). This is a project override, not a version Next.js
  itself declares; compatibility was confirmed by typecheck, tests, build,
  production Compose, and CI. Spring Boot 3.5.0 → **3.5.16** (Spring
  Framework 6.2.7 → 6.2.19) on the same 3.5.x line; Testcontainers stays
  `1.21.4`.
- **Result:** `npm audit` 0 and `npm audit --omit=dev` 0 (re-checked on
  `7da54e8` and in the clean clone).
- **Security review scope** (final security audit, re-checked in code where
  possible): SQL uses `JdbcClient` parameter binding with no concatenation of
  request values; sort is an enum whitelist (`INVALID_SORT` otherwise); no
  `dangerouslySetInnerHTML` or other raw HTML injection path; fetch targets
  only the configured API bases (no user-controlled host); no custom
  redirect or middleware; CORS defaults to `http://localhost:*` and
  `http://127.0.0.1:*` and needs `APP_CORS_ALLOWED_ORIGIN_PATTERNS` in a real
  deployment; no real secret found; GitGuardian checks passed; CI runs with
  `contents: read`.
- **Limits:** no Docker image vulnerability scan (Docker Scout was installed
  but not logged in); a Spring Boot 4 major migration is out of scope; this
  review does not claim the absence of all security issues.

## F-15 — Final Lighthouse Regression Check (2026-10-03)

**What:** whether current `main` (`7da54e8`) is in the same performance class
as the PR #36 SSR build when measured with the recovered historical method.

- **Environment:** Windows 11 Pro; Chrome **153.0.8010.53** (the
  `chrome.exe` binary, confirmed by launching it; the staged 154 update was
  not active) via `CHROME_PATH`; Lighthouse **13.5.0** CLI;
  `--throttling-method=devtools` (request latency 562.5 ms, download
  1,474.56 kbps, upload 675 kbps, CPU 4×); default mobile 412×823 DPR 1.75;
  `--chrome-flags="--headless=new"`; storage reset per run; one `curl`
  warm-up; Docker production stack on `localhost:3000`; persistent dataset
  (42 posts, 39 readable, first post id 1), the same state as the historical
  run. Run warnings 0.

| Run | Perf | A11y | BP | SEO | FCP | LCP | TBT | CLS | Speed Index |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 1 | 95 | 100 | 100 | 100 | 2,255 | 2,255 | 109 | 0.0003 | 2,709 |
| 2 | 91 | 100 | 100 | 100 | 2,349 | 2,349 | 212 | 0.0003 | 2,757 |
| 3 | 93 | 100 | 100 | 100 | 2,246 | 2,246 | 182 | 0.0003 | 2,642 |
| 4 | 93 | 100 | 100 | 100 | 2,238 | 2,238 | 172 | 0.0003 | 2,639 |
| 5 | 90 | 100 | 100 | 100 | 2,820 | 2,820 | 0 | 0.0003 | 3,181 |
| 6 | 94 | 100 | 100 | 100 | 2,296 | 2,296 | 147 | 0.0003 | 2,697 |

| Median (3rd and 4th of 6, averaged) | Historical (PR #36) | Current | Difference |
| --- | ---: | ---: | ---: |
| Performance | 93 | 93 | 0 |
| FCP | 2,357 ms | 2,275 ms | −82 ms |
| LCP | 2,357 ms | 2,275 ms | −82 ms |
| TBT | 169.9 ms | 159.2 ms | −10.7 ms |
| CLS | 0.0003 | 0.0003 | 0 |
| Speed Index | 2,764 ms | 2,703 ms | −61 ms |

- LCP element identical to the historical runs (the SSR post preview, not a
  loading shell); the font finished at 13.9–15.0 s, well after LCP; browser
  `/api/posts` 0 in every run; `/` First Load JS 121 kB.
- Sanity on the same stack: likes, search ("촉촉", 9), detail and comments
  (`limit=2`, `댓글(1,411)`, More) worked with 0 console errors.
- **Result:** `FINAL_RUNTIME_REGRESSION_PASS`, with no browser-version caveat
  (exact Chrome 153 build used).

## F-16 — Clean-Clone Release Gate (2026-10-03)

**What:** whether GitHub `main` alone reproduces the project with no
project-local state.

- **Source:** fresh `git clone` into a new directory; branch `main`, HEAD
  `7da54e8` equal to `origin/main`; clean tree. No `node_modules`, `.next`,
  backend `build/`, `.env`, or previous database was present or copied. No
  tracked `.env`, no `env_file`/dotenv reference, no absolute paths; every
  Compose and Spring setting has a default.
- **Toolchain:** Git 2.52.0.windows.1, Java 17.0.17, Node 24.12.0 (engine
  `>=22.12.0`), npm 11.6.2, Docker 29.8.1, Compose 5.5.1.
- **Frontend:** `npm ci` (0 vulnerabilities), `npm run typecheck`, Vitest 21
  tests / 4 files, production build: all PASS; `/` 6.22 kB, First Load JS
  121 kB; `package-lock.json` unchanged.
- **Backend:** `gradlew.bat clean test` from no `build/`: BUILD SUCCESSFUL, 18
  suites, 91 tests, 0 failures; 6 Testcontainers integration suites with 31
  tests passed.
- **Isolated Compose:** project `apr-final-release-gate`, ports DB 55432,
  backend 58080, frontend 53000 (`DB_PORT`/`BACKEND_PORT`/`FRONTEND_PORT`);
  `docker compose config -q` and `up -d --build --wait` passed; db, backend,
  frontend healthy; new volume `apr-final-release-gate_dbdata`.
- **Fresh seed vs `seed/seed.json`:** expected posts 42 / comments 2,076
  computed from the file; actual posts 42 (3 deleted, 39 readable), comments
  2,076 (81 deleted), `post_likes` 0, `seed_history` `assignment-v1` = 1,
  Flyway `V1` applied, no duplication.
- **Identity:** max seeded post 42 → created post 43; max seeded comment
  2,076 → created comment 2,077; no collision.
- **API / SSR:** `/health` 200 `{db: up, status: ok}`; latest (total 39, first
  id 1), likes (first id 25), search "촉촉" (9), detail of post 2 (1,411
  comments), comments `limit=2`; `/` 200 with 15 post titles, the first
  post's title and `/posts/1` link, and no loading-only HTML; in the browser,
  hydration list requests 0, likes/search/detail/comments working, console
  errors 0.
- **Wiring:** SSR used `http://backend:8080/api` inside Compose; the client
  bundle contained `http://localhost:58080/api` and no `localhost:8080`; the
  browser requested only `localhost:53000` and `localhost:58080`, with 0
  requests to the old stack ports.
- **Full-stack smoke:** `scripts/ci-full-stack-smoke.sh` with the release-gate
  project and ports: PASS.
- **Persistence across recreation:** created marker post 43
  (`RELEASE_GATE_171454`, requester `x-user-id: release_gate`), comment 2077,
  and a like. Before: posts 43, comments 2,077, `post_likes` 1,
  `seed_history` 1. After `docker compose down` **without `-v`** and `up -d
  --wait`: all services healthy, `/health` 200, `/` 200, the marker post,
  comment, and like (likeCount 1, isLiked true) still present; posts 43,
  comments 2,077, `seed_history` 1, seeded ranges unchanged (42 posts with id
  ≤ 42, 2,076 comments with id ≤ 2,076): no seed duplication. The marker was
  then deleted through the API (204, then 404).
- **Integrity and cleanup:** the clone's `git status` stayed clean
  (`diff --check` PASS; only ignored build outputs). The release-gate
  containers, network, volume, and images were removed after confirming the
  project name; the original repository and the persistent default volume
  were untouched.
- **Result:** `CLEAN_CLONE_RELEASE_GATE_VERIFIED`.

---

# Final Engineering Verification Summary

| Area | Evidence | Status |
| --- | --- | --- |
| Current `main` | `7da54e8a361d2b8212a46e8b9e8091b8240ce3fa`, CI success | — |
| Frontend tests | Vitest 21 tests / 4 files | PASS |
| Backend tests | 91 tests / 18 suites, incl. 31 PostgreSQL Testcontainers tests | PASS |
| PostgreSQL correctness | F-07 (real Flyway, seed, repositories, injected-bug sensitivity) | VERIFIED |
| Frontend cache consistency | F-08, 148/148 | VERIFIED |
| Mutation concurrency | F-09 (8 tests, `FOR UPDATE` sensitivity) | VERIFIED |
| Edge / failure / recovery | F-12 | VERIFIED |
| Full-stack CI | F-13 (3 jobs) | PASS on `main` |
| Dependency audit | `npm audit` 0 / `--omit=dev` 0, Boot 3.5.16 | PASS |
| Performance regression | F-15: Perf median 93, LCP median 2,275 ms, A11y/BP/SEO 100 | PASS |
| Reproducibility | F-16 clean clone | VERIFIED |

Engineering verification scope: **VERIFIED** for the conditions recorded above
(one Windows development host, Docker Compose, emulated mobile devices,
disposable and seeded datasets). Final assignment sign-off remains pending
`V-081` official design fidelity verification. This does not claim verification
of every environment or of production conditions.

---

# Final Sign-Off

## Original sign-off criteria and their status

The criteria defined at the start of the project are kept here with their
current status:

| Original criterion | Status on `7da54e8` |
| --- | --- |
| `V-001`, `V-002`, `V-003`, `V-004` pass. | Met (`PASS`; F-16 re-confirmed boot, seed, and idempotency from a clean clone) |
| All applicable behavior scenarios pass. | Met (`V-010`–`V-071`, `V-080` `PASS`) |
| All mandatory requirements are `VERIFIED`. | Met for the behavior scenarios above; `UI-004` depends on `V-081` |
| `V-081` passes after visual implementation is compared with the now-available official design source. | **Not met**: `V-081` remains `NOT_RUN` |
| README accurately reflects the final implementation. | `V-090` `PASS` on 2026-10-01; later README changes (for example PRs #37–#39) came with their own reviews and were not re-audited as a whole here |

## Verified engineering gates

- Release gates `V-001` to `V-004`, and all behavioral scenarios `V-010` to
  `V-071` and `V-080`: `PASS` (dated evidence above, re-confirmed by later
  sections where behavior changed).
- `V-090` README accuracy: `PASS` for its date; current values are in the
  snapshot above.
- PostgreSQL correctness (F-07), frontend cache consistency (F-08),
  concurrency invariants (F-09), edge/failure handling (F-12), SSR and
  performance regression (F-01, F-02, F-15), dependency security (F-14),
  full-stack CI (F-13), and clean-clone reproducibility (F-16).

## Accepted trade-offs

- Likes-sorted traversal is not snapshot-consistent across requests: rows can
  be duplicated (hidden by the UI) or missed until a fresh traversal (F-10).
- Under READ COMMITTED a page and its `total` can differ by one under
  concurrent writes (F-11).
- The 2 MB Pretendard file is kept: it is not on the LCP path, and the
  measured subset alternative made real LCP slower (F-01).
- No comment virtualization at the current scale (F-05).
- No Spring Boot 4 major upgrade (F-14).
- Branch protection is not enforced on `main`; CI gates exist but do not
  technically block merges (F-13).
- Same-post likes serialize on the post row (F-09); the SSR page waits up to
  2 s on a hung backend before falling back (F-02).

## Outstanding / not claimed

- `V-081` official design fidelity remains `NOT_RUN`: no fresh comparison of
  the final implementation with the official Figma file was run, and the
  intentional deviations (P8-C retouch, PR #35 contrast) are recorded for
  that comparison.
- Production traffic and load behavior, production observability, real
  authentication (the requester header/cookie is the assignment contract),
  Docker image vulnerability scanning, and deployment/CD are not verified.
- Deadlock freedom is not proven in general; deadlocks were not observed in
  the concurrency tests and no obvious lock cycle was found (F-09).
- Performance figures come from emulated devices and one host; real devices
  and HTTP/2 production networking were not measured.
