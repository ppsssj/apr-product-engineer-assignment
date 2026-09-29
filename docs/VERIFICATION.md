# Acceptance Verification

This document defines executable acceptance scenarios for the APR assignment.

Do not mark a requirement `VERIFIED` only because the code appears correct. Run the relevant scenario and record evidence.

## Result Values

- `NOT_RUN`
- `PASS`
- `FAIL`
- `BLOCKED`

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

**Result:** `NOT_RUN`

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

**Result:** `NOT_RUN`

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
- States (forced with Playwright route interception): initial loading (`게시글을 불러오는 중입니다.`), empty (`아직 작성된 게시글이 없습니다.`), initial error after TanStack's default retries (4 attempts) with a working `다시 시도`, next-page loading (`게시글을 더 불러오는 중입니다.`), and next-page error that keeps the 10 loaded rows, does not auto-retry on further scrolling, and recovers via its own `다시 시도`.
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
  - Deleting the first page's cursor-boundary row (post 11, row 10, as `booster_0`) also removed only that row.
  - In both cases, scrolling on continued from the cached cursor, the full list equaled the server, and there was no skip or duplicate.
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

**Result:** `PASS`

---

# Soft Delete

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

**Result:** `NOT_RUN`

---

# Final Sign-Off

Do not sign off until:

- `V-001`, `V-002`, `V-003`, `V-004` pass.
- All applicable behavior scenarios pass.
- All mandatory requirements are `VERIFIED`.
- `V-081` passes after visual implementation is compared with the now-available official design source.
- README accurately reflects the final implementation.
