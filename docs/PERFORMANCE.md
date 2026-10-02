# Performance Audit

Audit/measurement only. No product code, schema, migration, dependency, or API change was made. Measured on 2026-09-29 (≈10:40–11:06 UTC) against `main` at `e521fd8` (P5A merged).

Verdict: **`OPTIMIZATION_RECOMMENDED`** — for one specific change only.

- No user-visible performance bottleneck was found on the provided seed and the evaluator-facing workload. The service is not slow, and nothing needs fixing for the assignment to work.
- The measurements did reveal a structural inefficiency: the keyset cursor predicate is not used as a PostgreSQL index bound, so deep pages re-scan every earlier row linearly.
- A very small change (a row-value comparison) makes it a true index seek with the same API contract and ordering. That single change is selected; every other candidate is kept as is, deferred, or rejected with its reason.

A non-performance readiness finding (§9) is recorded for a separate decision.

§13 is a separate, later frontend benchmark (2026-10-02) of the post-list batch sizes; it does not change the backend audit above.

## 0. Audit Narrative

| Stage | Content |
| --- | --- |
| **Before audit — concerns** | 2,118 row-by-row seed INSERTs; a 1,411-comment COUNT; the same COUNT/EXISTS repeated on every comment page; deep-page cost of keyset pagination; soft-delete filtering |
| **Measurement** | Idle-host clean/seeded startup runs, `psql` INSERT replay, `EXPLAIN (ANALYZE, BUFFERS)` cold/warm at shallow, middle, and deep cursors, API latency (50 samples), full traversal of post #2, SQL statement counts from `log_statement` |
| **Not bottlenecks** | Seed import (≈0.9 s once), COUNT (≈0.39 ms), soft-delete filtering (3.9 % of rows); therefore no need for a partial index, a denormalized `comment_count`, or cached totals |
| **Confirmed inefficiencies** | (1) The OR-shaped cursor predicate is evaluated as a `Filter`, so deep pages scan linearly. (2) EXISTS + COUNT repeat on every comment page; this exists but is not a bottleneck on this workload |
| **Decision** | Implement **only the row-value keyset predicate**. Several candidates were measured; one was changed |

---

## 1. Dataset

Recomputed directly from `seed/seed.json` (403,921 bytes):

| Item | Value |
| --- | --- |
| Posts | 42 (deleted `9`, `30`, `31` → 39 readable) |
| Comments | 2,076 rows; 81 deleted IDs → 1,995 non-deleted rows |
| Comments on deleted posts | 69 rows (never readable) |
| Readable comments on readable posts | 1,926 |
| Per readable post: min / median / mean / p90 / max | 0 / 7 / 49.4 / 37.6 / **1,411** |
| Top 5 posts (readable comments) | #2: 1,411 · #8: 58 · #34: 56 · #6: 40 · #19: 37 |
| Buckets (0 / 1–2 / 3–20 / 21–60 / >60) | 2 / 5 / 21 / 10 / 1 posts |

The distribution is extremely skewed: post 2 alone holds **73.3 %** of readable comments (top 5: 83.2 %). Post 2 has 1,468 physical comment rows, 57 of them soft-deleted. It is the worst case for every comment path and is used below.

## 2. Current Data Path

Code read: `V1__create_community_schema.sql`, `SeedInitializer`, `SeedDataReader`, `PostRepository`, `CommentRepository`, `PostLikeRepository`, `IdentitySequenceRepository`, `SeedHistoryRepository`, `PostReadService`, `PostMutationService`, cursor classes.

**Seed import** (`SeedInitializer.run`, `ApplicationRunner`):

- One `@Transactional` method. It checks `seed_history` for `assignment-v1` and returns immediately if present (idempotency).
- It parses the whole JSON with Jackson, then issues **one `JdbcClient.update()` per row**: 42 post INSERTs followed by 2,076 comment INSERTs, with explicit IDs and `is_deleted` precomputed from `deletedPostIds`/`deletedCommentIds`. No JDBC batching.
- It then runs `setval(pg_get_serial_sequence(t,'id'), MAX(id), true)` for `posts` and `comments`, and inserts the `seed_history` marker in the same transaction, so a failure leaves neither data nor marker.
- Total: 2,118 INSERTs + 2 `setval` + 1 existence check + 1 marker INSERT.

**Reads** (no explicit transaction; each statement runs in autocommit). Statement counts were confirmed with temporary PostgreSQL `log_statement = 'all'`, reset afterwards:

| Endpoint | SQL statements per request |
| --- | --- |
| `GET /posts?limit=n` | 2: page SELECT (`LIMIT n+1`, per row: like-count subquery, comment-count subquery, requester `EXISTS`) + `COUNT(*)` of readable posts |
| `GET /posts/{id}` | 1: SELECT with the same three derived values |
| `GET /posts/{id}/comments` | 3: `existsReadable` + page SELECT (`LIMIT n+1`) + `COUNT(*)` of the post's readable comments, **on every page** |
| `POST /posts/{id}/likes/toggle` | 6 in one transaction: BEGIN, `SELECT … FOR UPDATE`, like `EXISTS`, INSERT/DELETE, detail SELECT, COMMIT |

**Indexes:** `posts_pkey`, `posts (created_at DESC, id DESC)`, `comments_pkey`, `comments (post_id, created_at DESC, id DESC)`, `post_likes_pkey (post_id, user_id)`.

## 3. Measurement Environment

| Item | Value |
| --- | --- |
| DB | PostgreSQL 16.15 (Docker `postgres:16-alpine`), `shared_buffers` 128 MB, `work_mem` 4 MB |
| Backend | Compose production image, Temurin 17.0.20.1, Spring Boot 3.5.0 |
| Host | Windows 11 + Docker 29.8.1 on WSL2 (kernel 6.18), 6 vCPU, ~3.8 GB |
| DB contents | seed only: 42/3 posts, 2,076/81 comments, 0 likes, `seed_history = assignment-v1` |
| Relation sizes | `comments` heap 34 pages (520 kB total), comment index 18 pages; `posts` 80 kB |
| Statistics | `comments` auto-analyzed; `posts` (42 rows) is below the autovacuum threshold and has **never been analyzed**, so the planner estimates 235 rows (see §6) |

API latency includes the Windows → WSL2 port-forwarding hop. Absolute values are environment-specific; the comparisons between rows matter more.

The database was reset several times for seed runs and was restored to the seed baseline afterwards. Temporary scripts, SQL, and logging are not committed.

## 4. Seed Import Baseline

Method: backend stopped, `public` schema dropped (Flyway recreates it), backend started. **The host stayed idle during startup**, and results were read afterwards. Spring Boot logs `Started … in` just before ApplicationRunners run, and `seed_history.imported_at` is written at the end of the seed transaction. The seed segment is the gap between the two (JSON parse + INSERTs + `setval` + marker).

| Mode (5 runs each) | Seed segment | `Started … in` (JVM + context + Flyway) |
| --- | --- | --- |
| Clean DB | median **911 ms** (842–1,334) | median 3.09 s (3.05–5.32) |
| Already seeded | ≈0 (one `EXISTS`) | median 4.28 s (3.02–5.09) |

- Startup-time variance (≈2 s) is larger than any clean-vs-seeded difference in `Started … in`. The seed adds ≈0.9 s once per fresh volume.
- DB-side replay in a throwaway `bench` schema through `psql` (5 runs): 2,118 row-by-row INSERTs in one transaction took median **≈430 ms**; the same data as two multi-row INSERTs took **≈100 ms**. Batching could remove roughly 0.3–0.6 s from the one-time clean start.
- Methodology note: a first attempt polled the DB with `docker compose exec` every 50 ms during startup and measured 12–14 s seed segments. That was host contention, not seed cost, and was discarded.

## 5. API Baseline

Node `fetch` against `http://localhost:8080/api`, 5 warm-up + 50 measured sequential requests:

| Operation | Median | p95 | Min–max | SQL/request |
| --- | --- | --- | --- | --- |
| `GET /posts?limit=10` | 4.99 ms | 6.01 | 4.46–7.64 | 2 |
| `GET /posts/1` (2 comments) | 3.54 | 4.77 | 2.96–6.05 | 1 |
| `GET /posts/2` (1,411 comments) | 6.15 | 8.43 | 3.21–9.63 | 1 |
| `GET /posts/2/comments?limit=2` | 7.28 | 10.81 | 6.29–15.32 | 3 |
| `… limit=20` first continuation | 8.51 | 14.60 | 6.52–18.44 | 3 |
| `… limit=20` mid (page 36) | 7.23 | 11.05 | 6.45–12.58 | 3 |
| `… limit=20` last page | 7.20 | 11.38 | 6.18–12.99 | 3 |
| `GET /posts/1/comments?limit=2` (2 comments) | 6.61 | 18.39 | 5.60–21.92 | 3 |

Comment-page latency is the same for a 2-comment post and the 1,411-comment post, and flat from the first to the last page. Per-request cost tracks the **number of statements** (1 → ≈3.5 ms, 3 → ≈7 ms), not the size of the COUNT.

**Worst-case full traversal** (post 2, as the UI does it: `limit=2`, then `limit=20` pages), 10 runs:

- 72 requests, 1,411 comments, 1,411 unique (no duplicate or skip), newest-first. The result equals a `limit=50` traversal. `total` was 1,411 on every page, and the final `nextCursor` was `null`.
- Elapsed median **523 ms** (p95 594, 458–594).
- SQL: 72 × `existsReadable` + 72 × page SELECT + 72 × `COUNT(*)` = **216 statements**; `log_statement` confirmed exactly 216 for this traversal. The same `1411` is computed 72 times.

## 6. SQL Plans

`EXPLAIN (ANALYZE, BUFFERS)` of SQL equivalent to the repositories. "Cold" is the first run after `docker compose restart db` (empty shared_buffers; the OS page cache may still be warm). "Warm" is the median of the next 5 runs.

| SQL | Cold | Warm median | Buffers (warm) | Key plan facts |
| --- | --- | --- | --- | --- |
| A1 post list page 1 (`LIMIT 11`) | 1.16 ms | 0.70 ms | hit 95 | Index Scan `posts_created_at_id_idx`; subplans ×11: like count via `post_likes_pkey`, comment count via Bitmap on comment index (avg 147 rows), requester EXISTS |
| A2 post list page 3 (cursor) | 0.67 | 0.44 | hit 51 | same; cursor predicate is a **Filter**, 33 rows removed |
| B1 detail post 2 | 0.62 | 0.55 | hit 47 | `posts_pkey`; comment-count subplan reads 1,468 index entries, filters 57 deleted, 0.38 ms |
| B2 detail post 1 | 0.28 | 0.27 | hit 12 | same, 2 comments |
| C `COUNT` comments post 2 | 0.39 | 0.39 | hit 34 | **Seq Scan** of the whole table (1,411 of 2,076 rows match); planner choice is sound at this selectivity |
| D comment page initial (`LIMIT 3`) | 0.09 | 0.09 | hit 6 | Index Scan, `Index Cond: post_id`, `is_deleted` as filter, no sort |
| E1 first continuation (`LIMIT 21`) | 0.09 | 0.09 | hit 6 | cursor predicate is a Filter, 3 rows removed |
| E2 mid cursor (after #702) | 0.21 | 0.19 | hit 23 | Filter removes **728** rows |
| E3 deepest cursor (after #1,402) | 1.55 | 0.29 | hit 42 | Filter removes **1,459** rows |
| F `COUNT` posts | 0.08 | 0.07 | hit 2 | Seq Scan, 42 rows |
| G `existsReadable` | 0.10 | 0.10 | hit 5 | `posts_pkey` |

Findings:

- **The keyset cursor is not used as an index bound.** `created_at < :t OR (created_at = :t AND id < :id)` is evaluated as a Filter after `Index Cond: post_id = …`, so each page walks the post's index range from the newest row and discards everything before the cursor. Work grows linearly with depth (removed rows 3 → 728 → 1,459; buffers 6 → 23 → 42): it behaves like OFFSET. The posts list has the same shape (A2).
- A read-only check of the row-value form `(created_at, id) < (:t, :id)` showed `Index Cond: ((post_id = 2) AND (ROW(created_at, id) < ROW(…)))`: deepest page **3–4 buffers, ≈0.11 ms**; the mid page 4 buffers, ≈0.14 ms. The result set was identical to the current predicate. The same form becomes an Index Cond on `posts_created_at_id_idx`.
- Soft-delete filtering costs little: 57 of 1,468 index entries for post 2, 81 rows table-wide.
- The unanalyzed `posts` table misestimates rows (235 vs 39). It did not change any plan choice here.

## 7. Repeated Work

For a post with N readable comments traversed with the UI page sizes (2, then 20):

```text
requests = 1 + ceil((N − 2) / 20)
statements = 3 × requests   (existsReadable + page SELECT + COUNT)
```

For post 2 (N = 1,411): 72 requests → 72 EXISTS + 72 page SELECT + 72 COUNT = 216 statements, confirmed by statement logging. The COUNT result never changes during the traversal.

Measured cost of the repetition: 72 × COUNT ≈ 72 × 0.39 ms ≈ **28 ms**, and 72 × EXISTS ≈ 7 ms, out of a 523 ms traversal (≈7 %). The larger share is per-request fixed cost (HTTP, three autocommit round trips), about 7 ms per page. Users do not traverse 72 pages at once; the UI requests one page per scroll step.

`total` must be in every list response (`ASSIGNMENT.md` §5), and P5B will change comment counts through create/delete, so each page's `total` is also what keeps the client consistent.

## 8. Bottleneck Verdicts

| Hypothesis | Verdict | Evidence |
| --- | --- | --- |
| **A.** Row-by-row seed INSERT is a meaningful clean-start bottleneck | **NOT_CONFIRMED** | Seed segment median 911 ms, once per fresh volume, within 2 s of startup noise. Batching would save ≈0.3–0.6 s once (replay 430 → 100 ms DB-side). Measurable but not meaningful. |
| **B.** COUNT over 1,411 comments is itself a bottleneck | **NOT_CONFIRMED** | 0.39 ms warm (Seq Scan of 34 pages). Comment-page API latency is identical for 2 vs 1,411 comments. |
| **C.** Repeating the same COUNT per page matters more than one COUNT | **CONFIRMED** (relative), not a latency bottleneck | 72 identical COUNTs (+72 EXISTS) per full traversal, 216 statements; ≈35 ms of 523 ms. Per-request statement count, not COUNT size, drives API latency (1 statement ≈3.5 ms vs 3 ≈7 ms). |
| **D.** Keyset pagination + composite index stays stable at deep pages | **NOT_CONFIRMED** | The cursor predicate is a Filter, not an Index Cond. Scanned rows and buffers grow linearly (3 → 1,459 rows removed, 6 → 42 buffers). Absolute cost stays ≤0.29 ms warm, and API latency is flat (7.2 ms first/mid/last), so it is not user-visible at this scale. |

**Confirmed bottlenecks:** none that affect user-visible latency on the provided seed. Every read endpoint has a median under 10 ms, dominated by fixed per-request cost.

**Confirmed inefficiencies (not bottlenecks):** OFFSET-like deep keyset scans (D); repeated EXISTS + COUNT per comment page (C).

## 9. Readiness Observation (not performance)

During a clean start, `GET /health` returned 200 while `GET /api/posts` still reported `total: 0`. This was observed for ≈2 s in one curl-polled run. Tomcat accepts requests before the `ApplicationRunner` seed transaction commits, and Compose's backend healthcheck uses `/health`, so "healthy" can precede seed availability. Evaluators opening the app within the first seconds of a first boot could briefly see an empty list. Any fix must keep `GET /health` at its path with HTTP 200; this is a separate correctness/UX decision and was not changed here.

## 10. Optimization Candidates

| Candidate | Class | Expected gain (measured basis) | Complexity / risk |
| --- | --- | --- | --- |
| Row-value keyset predicate `(created_at, id) < (:t, :id)` in both `findReadablePage` queries | **SELECTED → COMPLETE** (§12) | Deep comment page 42 → 3–4 buffers, 0.29 → ≈0.11 ms SQL; makes pagination a true index seek, so cost stays constant with depth. No measurable API-level gain today. | Two SQL predicate edits; same ordering and results (verified on post 2). Needs a repository/API regression over full traversals. Relies on both columns having the same (DESC) direction in the index. |
| JDBC batch (or multi-row) seed INSERT | **DEFERRED** (low-risk, one-time gain) | ≈0.3–0.6 s once per fresh volume. | Small change inside the existing transaction; idempotency and sequence sync unchanged. The gain is one-time and inside startup noise. |
| Merge `existsReadable` + `COUNT` (e.g. one statement returning existence and count) | **DEFERRED** | Saves one round trip per comment page, roughly ≈1–2 ms (inferred from 1-statement vs 3-statement endpoints, not a controlled test). | Must keep the 404 for a deleted/missing post with zero comments; a slightly less obvious query. |
| Denormalized `posts.comment_count` / cached totals | **REJECTED** (higher complexity) | Removes 0.4 ms COUNTs and subplans (≈0.3 ms per list page). | Must be maintained on comment create/soft-delete and post delete, set correctly at seed time (69 comments on deleted posts, 81 deleted IDs), kept transactionally consistent under concurrent writes, and backfilled for drift. High risk to `DEL-006`/`SEED-008`/`SEED-009` for a sub-millisecond gain. |
| Partial index `… WHERE is_deleted = FALSE` on comments | **REJECT** | Deleted rows are 3.9 % of comments (57 of 1,468 for post 2); the COUNT already prefers a 34-page Seq Scan. | Extra index to maintain for no measured benefit. |
| Omit `total` on continuation pages | **REJECT** | Would remove 71 COUNTs per traversal. | Violates the required `{ items, total }` envelope (`ASSIGNMENT.md` §5) and weakens count consistency after P5B mutations. |
| Response caching of comment totals | **REJECT** | Same as above. | Invalidation on create/delete; unnecessary at < 1 ms. |
| `ANALYZE posts` / planner tuning | **KEEP** (no change) | Plans are already correct despite the misestimate. | None needed. |

**KEEP:** correlated-subquery derived counts in list/detail (≤0.7 ms per list page), per-page `total`, requester `EXISTS`, current indexes, single-transaction seed.

## 11. Decision and Next Steps

1. **Selected → COMPLETE:** the row-value keyset predicate (§12), implemented in a separate PR after this audit merged, with before/after re-measurement and identical results over full traversals.
2. **Deferred:** seed batching and the EXISTS + COUNT merge. Both are low-risk, but their measured gain is one-time or within noise.
3. **Rejected:** denormalized `comment_count`, cached totals, partial index, omitting `total`.
4. **Separate decision:** the first-boot readiness window (§9) is a correctness/UX topic, not performance.
5. P5B follows the optimization PR.


## 12. Selected Optimization — Row-value Keyset

Implemented in `b86f668` (`perf/row-value-keyset`). Measured on 2026-09-29 ≈11:25–11:40 UTC in the same Compose PostgreSQL 16.15 / WSL2 environment as §3.

### Problem

The cursor predicate `created_at < :t OR (created_at = :t AND id < :id)` is a correct keyset boundary, but PostgreSQL evaluated it as a `Filter` after `Index Cond: (post_id = …)`.

### Why it matters

Each page re-read the post's index range from the newest row and discarded every row before the cursor. Scan work grew linearly with page depth, which is exactly the OFFSET behavior keyset pagination was chosen to avoid.

### Root cause

The planner does not turn the OR-shaped predicate into a composite `(created_at, id)` range bound on the `(post_id, created_at DESC, id DESC)` / `(created_at DESC, id DESC)` indexes. A row-value comparison is a single composite bound that the btree can seek to.

### Change

Only the cursor predicate in `PostRepository.findReadablePage` and `CommentRepository.findReadablePage` changed:

```sql
AND (p.created_at, p.id) < (:cursorCreatedAt, :cursorId)
AND (c.created_at, c.id) < (:cursorCreatedAt, :cursorId)
```

Preconditions checked before the change:

- both queries order by `created_at DESC, id DESC` (same direction);
- `created_at` is `NOT NULL` and `id` is the primary key;
- the cursor is exactly `(Instant createdAt, long id)` (`CursorPosition`).

PostgreSQL compares row constructors left to right and decides at the first unequal pair ("Row Constructor Comparison", PostgreSQL docs, functions-comparisons). For non-null values, `(a, b) < (x, y)` is therefore exactly `a < x OR (a = x AND b < y)`. No other SQL, schema, index, or API field changed.

### Before / After — execution plans

`EXPLAIN (ANALYZE, BUFFERS)`, both forms measured in the same session, warm median of 5 runs after a DB restart. Rows removed are those removed by the scan node's filter; buffers are the scan node's (comments) or the whole statement's (posts, where per-row subplans dominate).

| Query | Before (OR) | After (row-value) |
| --- | --- | --- |
| Comments #2, shallow (after row 2) | `Index Cond: post_id` + cursor `Filter`; removed 3; buffers 6; 0.084 ms | `Index Cond: post_id AND ROW(created_at, id) < ROW(…)`; removed 1 (a soft-deleted row); buffers 3; 0.126 ms |
| Comments #2, middle (after row 702) | cursor `Filter`; removed **728**; buffers **23**; 0.194 ms | row-value `Index Cond`; removed 0; buffers **4**; 0.117 ms |
| Comments #2, deepest (after row 1,402) | cursor `Filter`; removed **1,459**; buffers **42**; 0.285 ms | row-value `Index Cond`; removed 1 (soft-deleted); buffers **3**; 0.112 ms |
| Posts page 2 (after row 10) | Index Scan, cursor `Filter`, removed 11; 50 buffers; 0.346 ms | Index Scan, row-value `Index Cond`, removed 0; 50 buffers; 0.497 ms |
| Posts page 4 (after row 30) | Seq Scan + Sort, removed 33; 0.482 ms | Seq Scan + Sort, removed 33; 0.475 ms |

- Every pair returned identical row IDs.
- Comments: scan work no longer depends on depth. Buffers stay at 3–4 from the first continuation to the last page, where they were 6 → 23 → 42 before.
- Posts: the table has 42 rows (2 heap pages). On page 4 the planner (with statistics collected at 11:28) chooses a Seq Scan + Sort for both forms, so the predicate form is irrelevant there; on page 2 the row-value bound is used as an `Index Cond`. Post-list cost is dominated by the per-row count subplans in both forms, so no gain is claimed for posts.
- Sub-0.2 ms differences between warm medians are within run-to-run variation. Cold-run numbers are not compared, because the row-value runs followed the OR runs in the same warmed cache.

### Correctness

Real PostgreSQL + provided seed, through the API, before and after the change (identical ID sequences for all 11 traversals):

- Posts: 39 readable posts with `limit=10` (4 pages) and `limit=3` (13 pages). Each post appeared exactly once, newest-first, equal to the DB order, final `nextCursor: null`, `total: 39`.
- Comments with the UI page sizes (2, then 20): posts 10 (0), 24 (1), 1 (2), 3 (3), 4 (27), 8 (58), and 2 (1,411 in 72 pages). All were complete, unique, newest-first, equal to the DB order, page counts as expected, and final cursor `null`.
- Same-timestamp ties: the seed has no equal timestamps, so a temporary fixture was used. It added five posts sharing one timestamp plus an older and a deleted post, and a post with six readable comments sharing one timestamp plus a deleted one and two older ones. Traversed with `limit=2` so page boundaries fall inside the tie group: ID DESC within the tie, no duplicate or skip across boundaries, deleted rows excluded, final cursor `null`. At the SQL level the OR and row-value forms returned identical rows for cursors inside the tie. The fixture (IDs ≥ 900000) was removed afterwards, restoring 42 posts / 2,076 comments / 0 likes.
- PostgreSQL statement logging showed the rebuilt backend issuing `AND (p.created_at, p.id) < ($2, $3)` and `AND (c.created_at, c.id) < ($2, $3)`.
- The API contract is unchanged: the P4 (48/48), P5A (63/63), and P5A revisit (12/12) browser suites passed against the rebuilt backend.

### SQL statement count

Unchanged by design. A full traversal of post #2 is still 72 requests = 72 `EXISTS` + 72 page `SELECT` + 72 `COUNT` = 216 statements (marker-delimited `log_statement` capture). The optimization changes how each page `SELECT` seeks, not how many statements run.

### API effect

Same script as §5 (5 warm-up + 50 sequential requests; full traversal ×10). Medians in ms:

| Operation | Before | After |
| --- | --- | --- |
| `GET /posts?limit=10` | 4.96 | 4.58 |
| `GET /posts/2/comments?limit=2` (no cursor, unaffected) | 7.10 | 8.37 |
| `… limit=20` first continuation | 7.68 | 7.46 |
| `… limit=20` middle (page 36) | 6.98 | 6.73 |
| `… limit=20` last page | 6.89 | 7.17 |
| Post #2 full traversal (72 requests) | 473 | 587 |

The first run right after the backend restart was slower on every endpoint, including ones without a cursor, and was treated as JVM warm-up; the table uses the second run. Endpoints the change cannot affect moved just as much (for example `GET /posts/2` 6.28 → 8.68 ms). **The API latency change is not material at this dataset size, but index work no longer grows with cursor depth.**

### Decision

The selected optimization is `COMPLETE`. Everything else stays as decided in §10: seed batching and the EXISTS + COUNT merge remain `DEFERRED`; denormalized `comment_count`, cached totals, the partial index, and omitting `total` remain `REJECTED`. COUNT is cheap (≈0.39 ms), the seed cost is one-time, denormalization adds consistency risk, and the partial index showed no measured benefit.

---

## 13. Frontend List Batch Size Benchmark — 2026-10-02

Measurement only; no product code, dependency, or API change. Base: `main` at `e12dce5` (first-page SSR merged). Verdict: **`KEEP_15_15`** — `SSR_SIZE = 15`, `CONTINUATION_SIZE = 15`.

### 13.1 Why

The post list had one constant, `POST_LIST_PAGE_SIZE = 15`, chosen earlier by traversing the 39 readable seed posts (`4/3/2` requests for `10/15/20`). Two weaknesses remained:

- With only 39 readable posts, every candidate soon hits the end of the data, so the last-page effect mixes into the comparison and 15 can look like "39 split into three requests".
- After first-page SSR, "how many posts the HTML carries" (`SSR_SIZE`) and "how many each later cursor request fetches" (`CONTINUATION_SIZE`) are separate technical decisions that act on different phases (first render vs. scrolling).

This benchmark measures the two independently on a dataset long enough that no candidate reaches its end.

### 13.2 Environment

| Item | Value |
| --- | --- |
| Base | `main` `e12dce5f1e73b5047e5c6689a0ff052cddd4691c` |
| Viewport | 375×778, DPR 2, mobile emulation |
| CPU | 4× slowdown |
| Network | Lighthouse devtools mobile throttling: request latency 562.5 ms, download 1.47 Mbps |
| Tools | Lighthouse 13.5.0 (`--throttling-method=devtools`); Playwright with the same conditions applied over CDP |
| Build | One production Docker image of a temporary copy of `main` in which only the two sizes were read from environment variables |
| Persistent compose DB | Not touched: posts 42, comments 2,076, post_likes 1 before and after |

### 13.3 Disposable dataset

A separate PostgreSQL + backend stack was seeded with **160 visible posts and 9,147 comments**, then removed.

- Built from the 39 readable seed posts, preserving their title length (12–21), content length (63–77), 9 authors, `likeCount` distribution, `commentCount` distribution (0–1,411), and `createdAt` spacing.
- Every `id` and `createdAt` is unique.
- 160 is not an input to the page-size choice. It only removes the end-of-data effect so that every candidate is compared at the same **fixed consumption depth of 30 and 45 posts**; the 160 posts were never read to the end. Page size is not derived from the total row count (see 13.7).

### 13.4 SSR_SIZE benchmark

`CONTINUATION_SIZE` fixed at 15. Lighthouse devtools, 5 runs per candidate (median, range). Backend fetch and Next TTFB are 20-sample medians measured without throttling.

| Metric | SSR 10 | SSR 15 | SSR 20 |
| --- | ---: | ---: | ---: |
| Backend fetch | 13.1 ms | 10.3 ms | 10.8 ms |
| Next TTFB | 27.7 ms | 22.9 ms | 22.7 ms |
| HTML transfer | 7.7 KB | 8.7 KB | 9.6 KB |
| HTML resource | 33.0 KB | 45.6 KB | 58.2 KB |
| SSR API payload | 3,583 B | 5,308 B | 7,032 B |
| DOM elements | 230 | 330 | 430 |
| Fully + partly visible posts | 6 + 1 | 6 + 1 | 6 + 1 |
| Off-screen SSR rows | 3 | 8 | 13 |
| LCP median | 2,278 ms | 2,260 ms | 2,372 ms |
| LCP range | 2,243–2,401 | 2,103–2,345 | 2,162–2,665 |
| TBT median | 180 ms | 156 ms | 164 ms |
| CLS | 0.0003 | 0.0003 | 0.0003 |
| JS complete | 2,532 ms | 2,520 ms | 2,526 ms |
| Browser initial `GET /api/posts` | 0 | 0 | 0 |

- Each 5 extra SSR rows add about 0.9 KB of compressed HTML and about 100 DOM elements, but the LCP ranges overlap: the difference is within measurement noise.
- The first viewport shows 6 full posts and 1 partial post for every candidate. SSR 15 keeps 8 rows (about 1.2 screens) ready below the fold.

**Phase A verdict: `SSR_SIZE_EQUIVALENT`.** 10 is smaller but showed no LCP or server-cost gain. 20 adds initial data and DOM without a consistent LCP gain and had a run with an outlying longest task. The current 15 was kept for Phase B.

### 13.5 CONTINUATION_SIZE benchmark

`SSR_SIZE` fixed at 15. Two automated scroll profiles, identical for every candidate, starting after hydration:

- **Fast skim:** 300 px every 500 ms (≈600 px/s), 5 runs.
- **Reading:** 150 px every 500 ms (≈300 px/s), 3 runs.

"Consumed" means the post's row has entered the viewport. Overfetch is `loaded − consumed`.

**Consume 30 — fast skim (median)**

| Metric | cont 10 | cont 15 | cont 20 |
| --- | ---: | ---: | ---: |
| Next-page requests | 2 | 1 | 1 |
| API bytes | 7.7 KB | 5.6 KB | 7.3 KB |
| HTML + API bytes | 16.4 KB | 14.3 KB | 16.0 KB |
| Loaded | 35 | 30 | 35 |
| Overfetch | 5 | 0 | 5 |
| Elapsed | 4,668 ms | 4,644 ms | 4,668 ms |
| Longest wait at the list end | 224 ms | 275 ms | 277 ms |
| Loading text visible | 417 ms | 749 ms | 277 ms |

**Consume 45 (fast / reading)**

| Metric | cont 10 | cont 15 | cont 20 |
| --- | ---: | ---: | ---: |
| Next-page requests | 3 / 4 | 2 / 3 | 2 / 2 |
| API bytes | 11.6 / 15.5 KB | 11.2 / 16.8 KB | 14.7 / 14.7 KB |
| Overfetch | 0 / 10 | 0 / 15 | 10 / 10 |
| Elapsed (fast) | 7,788 ms | 7,763 ms | 7,248 ms |
| Longest wait (fast) | 229 ms | 275 ms | 277 ms |
| Cumulative wait (fast) | 590 ms | 489 ms | 277 ms |
| Loading text visible (reading) | 188 ms, blank area seen | 165 ms | 236 ms |
| Wait at the list end (reading) | 0 | 0 | 0 |
| DOM elements (fast) | 950 | 950 | 1,149 |
| Scripting (fast) | 394 ms | 346 ms | 318 ms |
| Longest task (fast) | 55 ms | 58 ms | 73 ms |

No duplicate posts and no request errors in any run. Each next-page response took about 600–623 ms and its rows reached the DOM about 47–71 ms later. The list is not virtualized, so loaded rows stay in the DOM.

### 13.6 Interpretation

- **Continuation 10:** more requests at both depths, and the only candidate whose loading text and blank area appeared even at reading speed. Its smaller payload does not offset the more frequent page boundaries.
- **Continuation 20:** fast skimming sometimes crossed one boundary fewer, lowering the cumulative wait, but the reading profile showed no consistent gain; which candidate overfetched more or sent more bytes flipped with scroll timing, and more rows accumulate in the DOM. Pairing it with SSR 15 would also need a separate first-page size and branch rules.
- **Continuation 15:** fewer requests than 10, no consistent disadvantage against 20, and the same size as the SSR page, so the cache, query key, and pagination stay uniform.

**Observation (not a conclusion).** A next page became visible about 650 ms after its request (≈600–623 ms response + ≈47–71 ms to the DOM). The current `rootMargin` of 200 px gives about 0.33 s of lead at 600 px/s and about 0.67 s at 300 px/s. The fast-skim waits therefore look more sensitive to how early the next page is requested (prefetch distance) than to the page size. This was identified as a follow-up to verify; it does not show that 200 px is wrong or that another value is better.

### 13.7 Decision

**`KEEP_15_15`.** 15 was not chosen to split the seed's 39 readable posts into three requests. Measured on a dataset large enough to avoid the end of the data, with first-render cost and fixed-depth scrolling separated, the 10/15/20 candidates gave no reason to change 15/15.

- `SSR_SIZE` and `CONTINUATION_SIZE` are technically independent; these results gave no performance reason to make them differ, so the simpler equal value stays.
- Page size is not derived from the total number of posts. Cursor pagination reads only `limit + 1` rows per request; the total only changes how many requests a user who reads to the end makes. What matters for a typical session is the consumption depth, which is why the comparison used fixed depths of 30 and 45.

### 13.8 Limitations

- One emulated device, viewport, CPU factor, and network profile; it does not represent all user devices.
- Two scripted scroll profiles; they do not represent all real scroll behavior.
- HTTP/1.1 `next start` in Docker; an HTTP/2 production network was not measured.
- Only 10/15/20 were compared. This establishes the most reasonable of the compared candidates under these conditions, not an absolute or global optimum.
- The benchmark scripts, the patched copy, and the synthetic seed lived outside the repository and are not committed.

### 13.9 Next hypothesis

Benchmark `rootMargin` 200 px against larger values (for example 300/400/500 px) at the same fixed depths, measuring waits at the list end, loading-text exposure, and requests sent before the user scrolls. No `rootMargin` change is made or recommended until that is measured.
