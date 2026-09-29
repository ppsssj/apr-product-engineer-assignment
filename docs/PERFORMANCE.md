# Performance Audit

Audit/measurement only. No product code, schema, migration, dependency, or API change was made. Measured on 2026-09-29 (≈10:40–11:06 UTC) against `main` at `e521fd8` (P5A merged).

Verdict: **`OPTIMIZATION_RECOMMENDED`** — for one specific change only.

- No user-visible performance bottleneck was found on the provided seed and the evaluator-facing workload. The service is not slow, and nothing needs fixing for the assignment to work.
- The measurements did reveal a structural inefficiency: the keyset cursor predicate is not used as a PostgreSQL index bound, so deep pages re-scan every earlier row linearly.
- A very small change (a row-value comparison) makes it a true index seek with the same API contract and ordering. That single change is selected; every other candidate is kept as is, deferred, or rejected with its reason.

A non-performance readiness finding (§9) is recorded for a separate decision.

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
| Row-value keyset predicate `(created_at, id) < (:t, :id)` in both `findReadablePage` queries | **SELECTED** | Deep comment page 42 → 3–4 buffers, 0.29 → ≈0.11 ms SQL; makes pagination a true index seek, so cost stays constant with depth. No measurable API-level gain today. | Two SQL predicate edits; same ordering and results (verified on post 2). Needs a repository/API regression over full traversals. Relies on both columns having the same (DESC) direction in the index. |
| JDBC batch (or multi-row) seed INSERT | **DEFERRED** (low-risk, one-time gain) | ≈0.3–0.6 s once per fresh volume. | Small change inside the existing transaction; idempotency and sequence sync unchanged. The gain is one-time and inside startup noise. |
| Merge `existsReadable` + `COUNT` (e.g. one statement returning existence and count) | **DEFERRED** | Saves one round trip per comment page, roughly ≈1–2 ms (inferred from 1-statement vs 3-statement endpoints, not a controlled test). | Must keep the 404 for a deleted/missing post with zero comments; a slightly less obvious query. |
| Denormalized `posts.comment_count` / cached totals | **REJECTED** (higher complexity) | Removes 0.4 ms COUNTs and subplans (≈0.3 ms per list page). | Must be maintained on comment create/soft-delete and post delete, set correctly at seed time (69 comments on deleted posts, 81 deleted IDs), kept transactionally consistent under concurrent writes, and backfilled for drift. High risk to `DEL-006`/`SEED-008`/`SEED-009` for a sub-millisecond gain. |
| Partial index `… WHERE is_deleted = FALSE` on comments | **REJECT** | Deleted rows are 3.9 % of comments (57 of 1,468 for post 2); the COUNT already prefers a 34-page Seq Scan. | Extra index to maintain for no measured benefit. |
| Omit `total` on continuation pages | **REJECT** | Would remove 71 COUNTs per traversal. | Violates the required `{ items, total }` envelope (`ASSIGNMENT.md` §5) and weakens count consistency after P5B mutations. |
| Response caching of comment totals | **REJECT** | Same as above. | Invalidation on create/delete; unnecessary at < 1 ms. |
| `ANALYZE posts` / planner tuning | **KEEP** (no change) | Plans are already correct despite the misestimate. | None needed. |

**KEEP:** correlated-subquery derived counts in list/detail (≤0.7 ms per list page), per-page `total`, requester `EXISTS`, current indexes, single-transaction seed.

## 11. Decision and Next Steps

1. **Selected:** the row-value keyset predicate, implemented in a separate small PR after this audit is merged. The PR must re-measure before and after with `EXPLAIN (ANALYZE, BUFFERS)` and API timing, and prove identical results over full traversals.
2. **Deferred:** seed batching and the EXISTS + COUNT merge. Both are low-risk, but their measured gain is one-time or within noise.
3. **Rejected:** denormalized `comment_count`, cached totals, partial index, omitting `total`.
4. **Separate decision:** the first-boot readiness window (§9) is a correctness/UX topic, not performance.
5. P5B follows the optimization PR.
