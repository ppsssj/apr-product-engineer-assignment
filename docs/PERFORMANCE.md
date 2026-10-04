# 성능 감사 및 측정 기록

이 문서는 여러 날짜와 commit에서 수행한 측정을 시간순으로 누적한 기록입니다. 각 section의 환경, version, 수치는 **그 측정 당시의 조건**이며 현재 dependency version으로 바꾸지 않았습니다.

**현재 적용 상태 요약** (현재 code 기준, 근거는 각 section):

- comment·post keyset predicate: row-value `(created_at, id) < (…)` 적용 완료(§12)
- post 목록 page size: SSR 15 / continuation 15(§13), sentinel `rootMargin` 500px(§14)
- 댓글: 첫 요청 2개(product 계약), continuation 30, `rootMargin` 200px 유지, virtualization 미도입(§15)
- 댓글 query `gcTime: 0` 유지(§15.9)
- client read retry: 4xx는 retry 없음, network/5xx/알 수 없는 오류는 1회. mutation 자동 retry 없음(§16)
- SSR: `SEED_FRESH_MS` 5s, `SERVER_FETCH_TIMEOUT_MS` 2s(§17)
- 첫 기동 readiness window(§9)는 이후 `SeedReadiness` afterCommit gate로 해결

---

**§0–§11: 초기 backend 성능 감사 (2026-09-29).** 감사·측정만 수행했고 product code, schema, migration, dependency, API는 바꾸지 않았습니다. 2026-09-29(≈10:40–11:06 UTC)에 `main` `e521fd8`(P5A merge 시점)을 대상으로 측정했습니다.

당시 판정: **`OPTIMIZATION_RECOMMENDED`** — 특정한 변경 하나에 대해서만. *(이후: 선택한 변경은 §12에서 적용·재측정되어 `COMPLETE`가 되었습니다.)*

- 제공된 seed와 평가자가 접하는 workload에서 사용자가 체감할 성능 병목은 발견되지 않았습니다. 서비스는 느리지 않았고, 과제 동작을 위해 고쳐야 할 것도 없었습니다.
- 다만 측정에서 구조적 비효율 하나가 드러났습니다. 이번 query·index·data의 실행 계획에서 keyset cursor 조건이 PostgreSQL index 경계로 쓰이지 않아, 깊은 page일수록 앞선 row를 선형으로 다시 훑었습니다.
- 아주 작은 변경(row-value 비교)으로 API 계약과 정렬을 유지한 채 실제 index seek로 만들 수 있었습니다. 이 변경 하나만 선택했고, 나머지 후보는 이유와 함께 유지·보류·기각했습니다.

성능이 아닌 readiness 관찰(§9)은 별도 결정 대상으로 기록했습니다.

§13–§15는 이후(2026-10-02/03)에 별도로 수행한 frontend benchmark로, post 목록 batch size와 prefetch 거리, 댓글 continuation과 긴 목록 rendering을 다룹니다. 위 backend 감사 결과를 바꾸지 않습니다. §16–§17은 2026-10-03의 client retry와 SSR budget 감사입니다.

## 0. 감사 흐름

| 단계 | 내용 |
| --- | --- |
| **감사 전 — 우려 사항** | row 단위 seed INSERT 2,118개; 1,411개 댓글에 대한 COUNT; 댓글 page마다 반복되는 같은 COUNT/EXISTS; keyset pagination의 깊은 page 비용; soft delete filter |
| **측정** | host가 유휴인 상태의 clean/seeded 기동, `psql` INSERT replay, 얕은·중간·깊은 cursor에서의 cold/warm `EXPLAIN (ANALYZE, BUFFERS)`, API latency(50 sample), post #2 전체 순회, `log_statement`로 센 SQL statement 수 |
| **병목 아님** | seed 적재(1회 ≈0.9 s), COUNT(≈0.39 ms), soft delete filter(row의 3.9 %). 따라서 partial index, 비정규화 `comment_count`, cached total이 필요 없음 |
| **확인된 비효율** | (1) OR 형태의 cursor 조건이 `Filter`로 평가되어 깊은 page가 선형으로 scan됨. (2) 댓글 page마다 EXISTS + COUNT가 반복됨. 존재하지만 이 workload에서 병목은 아님 |
| **결정** | **row-value keyset 조건만** 구현. 여러 후보를 측정했고 바꾼 것은 하나 |

---

## 1. 데이터셋

`seed/seed.json`(403,921 bytes)에서 직접 다시 계산했습니다.

| 항목 | 값 |
| --- | --- |
| Posts | 42 (삭제 `9`, `30`, `31` → 읽을 수 있는 post 39) |
| Comments | 2,076 rows; 삭제 ID 81 → 삭제되지 않은 row 1,995 |
| 삭제된 post의 댓글 | 69 rows (절대 읽을 수 없음) |
| 읽을 수 있는 post의 읽을 수 있는 댓글 | 1,926 |
| 읽을 수 있는 post당 min / median / mean / p90 / max | 0 / 7 / 49.4 / 37.6 / **1,411** |
| 상위 5개 post (읽을 수 있는 댓글) | #2: 1,411 · #8: 58 · #34: 56 · #6: 40 · #19: 37 |
| 구간 (0 / 1–2 / 3–20 / 21–60 / >60) | 2 / 5 / 21 / 10 / 1 posts |

분포가 극단적으로 치우쳐 있습니다. post 2 하나가 읽을 수 있는 댓글의 **73.3 %**를 가집니다(상위 5개: 83.2 %). post 2에는 물리적 댓글 row가 1,468개 있고 그중 57개가 soft delete되어 있습니다. 모든 댓글 경로의 최악 사례이므로 아래에서 이 post를 사용합니다.

## 2. 당시 데이터 경로

*2026-09-29 `e521fd8` 기준의 code 경로입니다. 이후 검색·likes 정렬 등이 추가되었으므로 현재 전체 code를 설명하지 않습니다.*

읽은 code: `V1__create_community_schema.sql`, `SeedInitializer`, `SeedDataReader`, `PostRepository`, `CommentRepository`, `PostLikeRepository`, `IdentitySequenceRepository`, `SeedHistoryRepository`, `PostReadService`, `PostMutationService`, cursor class.

**Seed 적재** (`SeedInitializer.run`, `ApplicationRunner`):

- `@Transactional` method 하나입니다. `seed_history`에 `assignment-v1`이 있는지 확인하고, 있으면 바로 반환합니다(idempotency).
- Jackson으로 JSON 전체를 parse한 뒤 **row마다 `JdbcClient.update()`를 한 번씩** 호출합니다. post INSERT 42개, 이어서 comment INSERT 2,076개이며, 명시적 ID와 `deletedPostIds`/`deletedCommentIds`에서 미리 계산한 `is_deleted`를 씁니다. JDBC batching은 없습니다.
- 그다음 `posts`와 `comments`에 대해 `setval(pg_get_serial_sequence(t,'id'), MAX(id), true)`를 실행하고, 같은 transaction에서 `seed_history` marker를 insert합니다. 실패하면 데이터도 marker도 남지 않습니다.
- 합계: INSERT 2,118개 + `setval` 2개 + 존재 확인 1개 + marker INSERT 1개.

**Read** (명시적 transaction 없음; 각 statement는 autocommit). statement 수는 임시로 PostgreSQL `log_statement = 'all'`을 켜서 확인했고, 이후 원복했습니다.

| Endpoint | 요청당 SQL statement |
| --- | --- |
| `GET /posts?limit=n` | 2: page SELECT(`LIMIT n+1`, row마다 like-count subquery, comment-count subquery, requester `EXISTS`) + 읽을 수 있는 post의 `COUNT(*)` |
| `GET /posts/{id}` | 1: 같은 파생 값 세 개를 가진 SELECT |
| `GET /posts/{id}/comments` | 3: `existsReadable` + page SELECT(`LIMIT n+1`) + 해당 post의 읽을 수 있는 댓글 `COUNT(*)`, **page마다** |
| `POST /posts/{id}/likes/toggle` | transaction 하나 안에서 6개: BEGIN, `SELECT … FOR UPDATE`, like `EXISTS`, INSERT/DELETE, detail SELECT, COMMIT |

**Index:** `posts_pkey`, `posts (created_at DESC, id DESC)`, `comments_pkey`, `comments (post_id, created_at DESC, id DESC)`, `post_likes_pkey (post_id, user_id)`.

## 3. 측정 환경 (당시)

| 항목 | 값 |
| --- | --- |
| DB | PostgreSQL 16.15 (Docker `postgres:16-alpine`), `shared_buffers` 128 MB, `work_mem` 4 MB |
| Backend | Compose production image, Temurin 17.0.20.1, Spring Boot 3.5.0 |
| Host | Windows 11 + WSL2 위 Docker 29.8.1 (kernel 6.18), 6 vCPU, ~3.8 GB |
| DB 내용 | seed만: posts 42/3, comments 2,076/81, likes 0, `seed_history = assignment-v1` |
| Relation 크기 | `comments` heap 34 pages (전체 520 kB), comment index 18 pages; `posts` 80 kB |
| 통계 | `comments`는 auto-analyze됨; `posts`(42 rows)는 autovacuum threshold 아래라 **한 번도 analyze되지 않아** planner가 235 rows로 추정 (§6 참고) |

API latency에는 Windows → WSL2 port forwarding hop이 포함됩니다. 절대값은 환경에 따라 다르며, row 사이의 비교가 더 중요합니다.

seed 측정을 위해 database를 여러 번 reset했고, 이후 seed baseline으로 복원했습니다. 임시 script, SQL, logging 설정은 commit하지 않았습니다.

## 4. Seed 적재 기준선

방법: backend를 멈추고 `public` schema를 drop한 뒤(Flyway가 다시 생성) backend를 기동했습니다. **기동 중에는 host를 유휴 상태로 두고** 결과는 나중에 읽었습니다. Spring Boot는 ApplicationRunner 실행 직전에 `Started … in`을 log하고, `seed_history.imported_at`은 seed transaction 끝에서 기록됩니다. seed 구간은 그 둘 사이의 간격입니다(JSON parse + INSERT + `setval` + marker).

| 모드 (각 5회) | Seed 구간 | `Started … in` (JVM + context + Flyway) |
| --- | --- | --- |
| Clean DB | median **911 ms** (842–1,334) | median 3.09 s (3.05–5.32) |
| 이미 seed됨 | ≈0 (`EXISTS` 1회) | median 4.28 s (3.02–5.09) |

- 기동 시간의 변동(≈2 s)이 `Started … in`의 clean·seeded 차이보다 큽니다. seed는 새 volume마다 한 번 ≈0.9 s를 더합니다.
- 일회용 `bench` schema에서 `psql`로 DB 측 replay(5회): transaction 하나 안의 row 단위 INSERT 2,118개는 median **≈430 ms**, 같은 데이터를 multi-row INSERT 두 개로 넣으면 **≈100 ms**였습니다. batching은 1회성 clean start에서 대략 0.3–0.6 s를 줄일 수 있습니다.
- 방법론 메모: 처음에는 기동 중 50 ms마다 `docker compose exec`로 DB를 polling해 seed 구간이 12–14 s로 측정되었습니다. 이는 seed 비용이 아니라 host 경합이었으므로 폐기했습니다.

## 5. API 기준선

Node `fetch`로 `http://localhost:8080/api`에 warm-up 5회 + 측정 50회를 순차 요청했습니다.

| 작업 | Median | p95 | Min–max | 요청당 SQL |
| --- | --- | --- | --- | --- |
| `GET /posts?limit=10` | 4.99 ms | 6.01 | 4.46–7.64 | 2 |
| `GET /posts/1` (댓글 2개) | 3.54 | 4.77 | 2.96–6.05 | 1 |
| `GET /posts/2` (댓글 1,411개) | 6.15 | 8.43 | 3.21–9.63 | 1 |
| `GET /posts/2/comments?limit=2` | 7.28 | 10.81 | 6.29–15.32 | 3 |
| `… limit=20` 첫 continuation | 8.51 | 14.60 | 6.52–18.44 | 3 |
| `… limit=20` 중간 (page 36) | 7.23 | 11.05 | 6.45–12.58 | 3 |
| `… limit=20` 마지막 page | 7.20 | 11.38 | 6.18–12.99 | 3 |
| `GET /posts/1/comments?limit=2` (댓글 2개) | 6.61 | 18.39 | 5.60–21.92 | 3 |

댓글 page latency는 댓글 2개짜리 post와 1,411개짜리 post가 같고, 첫 page부터 마지막 page까지 평탄합니다. 요청당 비용은 COUNT 크기가 아니라 **statement 수**를 따릅니다(1개 → ≈3.5 ms, 3개 → ≈7 ms).

**최악 사례 전체 순회** (post 2, UI 방식: `limit=2` 후 `limit=20` page), 10회:

- 요청 72개, 댓글 1,411개, 고유 1,411개(중복·누락 없음), 최신순. 결과는 `limit=50` 순회와 같았습니다. 모든 page에서 `total`은 1,411이었고 마지막 `nextCursor`는 `null`이었습니다.
- 경과 시간 median **523 ms** (p95 594, 458–594).
- SQL: `existsReadable` 72 × + page SELECT 72 × + `COUNT(*)` 72 × = **216 statements**; `log_statement`로 이 순회에서 정확히 216개임을 확인했습니다. 같은 `1411`이 72번 계산됩니다.

## 6. SQL 실행 계획

repository와 동등한 SQL의 `EXPLAIN (ANALYZE, BUFFERS)`입니다. "Cold"는 `docker compose restart db` 직후 첫 실행(shared_buffers는 비었지만 OS page cache는 따뜻할 수 있음), "Warm"은 이어진 5회의 median입니다.

| SQL | Cold | Warm median | Buffers (warm) | 핵심 plan 사실 |
| --- | --- | --- | --- | --- |
| A1 post 목록 page 1 (`LIMIT 11`) | 1.16 ms | 0.70 ms | hit 95 | Index Scan `posts_created_at_id_idx`; subplan ×11: `post_likes_pkey`로 like count, comment index Bitmap으로 comment count(평균 147 rows), requester EXISTS |
| A2 post 목록 page 3 (cursor) | 0.67 | 0.44 | hit 51 | 위와 같음; cursor 조건이 **Filter**, 33 rows 제거 |
| B1 상세 post 2 | 0.62 | 0.55 | hit 47 | `posts_pkey`; comment-count subplan이 index entry 1,468개를 읽고 삭제된 57개를 거름, 0.38 ms |
| B2 상세 post 1 | 0.28 | 0.27 | hit 12 | 위와 같음, 댓글 2개 |
| C post 2 댓글 `COUNT` | 0.39 | 0.39 | hit 34 | table 전체 **Seq Scan**(2,076 rows 중 1,411개 일치); 이 selectivity에서는 타당한 planner 선택 |
| D 댓글 첫 page (`LIMIT 3`) | 0.09 | 0.09 | hit 6 | Index Scan, `Index Cond: post_id`, `is_deleted`는 filter, sort 없음 |
| E1 첫 continuation (`LIMIT 21`) | 0.09 | 0.09 | hit 6 | cursor 조건이 Filter, 3 rows 제거 |
| E2 중간 cursor (#702 이후) | 0.21 | 0.19 | hit 23 | Filter가 **728** rows 제거 |
| E3 가장 깊은 cursor (#1,402 이후) | 1.55 | 0.29 | hit 42 | Filter가 **1,459** rows 제거 |
| F posts `COUNT` | 0.08 | 0.07 | hit 2 | Seq Scan, 42 rows |
| G `existsReadable` | 0.10 | 0.10 | hit 5 | `posts_pkey` |

발견:

- **keyset cursor가 index 경계로 쓰이지 않았습니다.** 이번 query·index·data의 실행 계획에서 `created_at < :t OR (created_at = :t AND id < :id)`는 `Index Cond: post_id = …` 뒤의 Filter로 평가되었습니다. 그래서 page마다 해당 post의 index 범위를 최신 row부터 훑고 cursor 앞의 row를 모두 버렸습니다. 작업량이 깊이에 따라 선형으로 늘어 OFFSET처럼 동작했습니다(제거 row 3 → 728 → 1,459; buffers 6 → 23 → 42). post 목록도 같은 형태입니다(A2).
- read-only로 row-value 형태 `(created_at, id) < (:t, :id)`를 확인하자 `Index Cond: ((post_id = 2) AND (ROW(created_at, id) < ROW(…)))`가 나왔습니다. 가장 깊은 page는 **3–4 buffers, ≈0.11 ms**, 중간 page는 4 buffers, ≈0.14 ms였습니다. 결과 집합은 당시 조건과 같았습니다. 같은 형태는 `posts_created_at_id_idx`에서도 Index Cond가 됩니다.
- soft delete filter 비용은 작았습니다. post 2에서 index entry 1,468개 중 57개, table 전체에서 81 rows입니다.
- analyze되지 않은 `posts` table은 row 수를 잘못 추정했지만(235 vs 39), 여기서 plan 선택을 바꾸지는 않았습니다.

## 7. 반복 작업

읽을 수 있는 댓글이 N개인 post를 UI page size(2, 이후 20)로 순회할 때:

```text
requests = 1 + ceil((N − 2) / 20)
statements = 3 × requests   (existsReadable + page SELECT + COUNT)
```

post 2(N = 1,411): 요청 72개 → EXISTS 72 + page SELECT 72 + COUNT 72 = 216 statements이며, statement logging으로 확인했습니다. 순회 중 COUNT 결과는 바뀌지 않습니다.

반복 비용 측정: COUNT 72회 ≈ 72 × 0.39 ms ≈ **28 ms**, EXISTS 72회 ≈ 7 ms로, 523 ms 순회의 ≈7 %입니다. 더 큰 몫은 요청당 고정 비용(HTTP, autocommit 왕복 세 번)으로 page당 약 7 ms입니다. 사용자는 72 page를 한 번에 순회하지 않고, UI는 scroll 단계마다 page 하나를 요청합니다.

`total`은 모든 목록 응답에 있어야 하며(`ASSIGNMENT.md` §5), 당시 다음 단계인 P5B에서 댓글 생성/삭제로 댓글 수가 바뀔 예정이었습니다. 따라서 page마다 오는 `total`이 client 일관성을 유지하는 역할도 합니다.

## 8. 병목 판정

| 가설 | 판정 | 근거 |
| --- | --- | --- |
| **A.** row 단위 seed INSERT가 의미 있는 clean start 병목이다 | **NOT_CONFIRMED** | seed 구간 median 911 ms, 새 volume마다 1회, 기동 noise 2 s 범위 안. batching은 1회 ≈0.3–0.6 s 절약(DB 측 replay 430 → 100 ms). 측정 가능하지만 의미 있지는 않음. |
| **B.** 댓글 1,411개에 대한 COUNT 자체가 병목이다 | **NOT_CONFIRMED** | warm 0.39 ms(34 pages Seq Scan). 댓글 page API latency가 댓글 2개와 1,411개에서 같음. |
| **C.** page마다 같은 COUNT를 반복하는 것이 COUNT 한 번보다 중요하다 | **CONFIRMED** (상대적), latency 병목은 아님 | 전체 순회당 같은 COUNT 72개(+EXISTS 72개), 216 statements; 523 ms 중 ≈35 ms. API latency를 좌우하는 것은 COUNT 크기가 아니라 요청당 statement 수(1개 ≈3.5 ms vs 3개 ≈7 ms). |
| **D.** keyset pagination + composite index가 깊은 page에서도 안정적이다 | **NOT_CONFIRMED** | cursor 조건이 Index Cond가 아니라 Filter. scan row와 buffer가 선형 증가(제거 row 3 → 1,459, buffers 6 → 42). 절대 비용은 warm ≤0.29 ms이고 API latency는 평탄(첫/중간/마지막 7.2 ms)해 이 규모에서는 사용자에게 보이지 않음. |

**확인된 병목:** 제공된 seed에서 사용자가 체감하는 latency에 영향을 주는 병목은 없습니다. 모든 read endpoint의 median은 10 ms 미만이며 요청당 고정 비용이 대부분입니다.

**확인된 비효율(병목 아님):** OFFSET처럼 동작하는 깊은 keyset scan(D); 댓글 page마다 반복되는 EXISTS + COUNT(C).

## 9. Readiness 관찰 (성능 아님)

*당시 관찰 기록입니다. 이후 별도 correctness 변경에서 해결되었습니다(아래 "이후").*

clean start 중 `GET /api/posts`가 아직 `total: 0`을 보고하는데 `GET /health`는 200을 반환했습니다. curl로 polling한 1회 실행에서 ≈2 s 동안 관찰되었습니다. Tomcat이 `ApplicationRunner` seed transaction commit 전에 요청을 받고, Compose의 backend healthcheck가 `/health`를 쓰므로 "healthy"가 seed 사용 가능 시점보다 앞설 수 있었습니다. 첫 기동 직후 몇 초 안에 앱을 연 평가자는 잠깐 빈 목록을 볼 수 있었습니다. 어떤 수정이든 `GET /health`의 경로와 HTTP 200을 유지해야 했습니다. 이는 별도의 correctness/UX 결정이었고 이 감사에서는 바꾸지 않았습니다.

*이후:* `SeedReadiness`가 seed transaction의 `afterCommit`에서 ready를 열고, 그 전까지 `/health`는 503 `starting`, ready 후에는 200을 반환하도록 수정되었습니다. 근거는 `docs/VERIFICATION.md` "P8-A First-Boot Readiness Investigation"과 `docs/ARCHITECTURE.md` ADR-013에 있습니다.

## 10. 최적화 후보

| 후보 | 분류 | 기대 이득 (측정 근거) | 복잡도 / 위험 |
| --- | --- | --- | --- |
| 두 `findReadablePage` query의 row-value keyset 조건 `(created_at, id) < (:t, :id)` | **SELECTED → COMPLETE** (§12) | 깊은 댓글 page 42 → 3–4 buffers, SQL 0.29 → ≈0.11 ms; pagination을 실제 index seek로 만들어 깊이와 무관하게 비용이 일정. 당시 API 수준의 측정 가능한 이득은 없음. | SQL 조건 두 곳 수정; 정렬과 결과 동일(post 2에서 확인). 전체 순회에 대한 repository/API 회귀 확인 필요. index의 두 column 방향이 같다는 점(DESC)에 의존. |
| JDBC batch(또는 multi-row) seed INSERT | **DEFERRED** (위험 낮음, 1회성 이득) | 새 volume마다 1회 ≈0.3–0.6 s. | 기존 transaction 안의 작은 변경; idempotency와 sequence sync는 그대로. 이득이 1회성이고 기동 noise 범위 안. |
| `existsReadable` + `COUNT` 병합 (예: 존재 여부와 count를 함께 반환하는 statement 하나) | **DEFERRED** | 댓글 page마다 왕복 1회 절약, 대략 ≈1–2 ms(statement 1개 vs 3개 endpoint에서 추정, 통제된 test 아님). | 댓글 0개인 삭제/없는 post의 404를 유지해야 함; query가 조금 덜 명확해짐. |
| 비정규화 `posts.comment_count` / cached total | **REJECTED** (복잡도 높음) | 0.4 ms COUNT와 subplan 제거(목록 page당 ≈0.3 ms). | 댓글 생성/soft delete와 post 삭제 시 유지, seed 시점의 정확한 초기화(삭제된 post의 댓글 69개, 삭제 ID 81개), 동시 쓰기에서의 transaction 일관성, drift backfill이 필요. 1 ms 미만의 이득에 비해 `DEL-006`/`SEED-008`/`SEED-009` 위험이 큼. |
| comments의 partial index `… WHERE is_deleted = FALSE` | **REJECT** | 삭제 row는 댓글의 3.9 %(post 2에서 1,468개 중 57개); COUNT는 이미 34-page Seq Scan을 선호. | 측정된 이득 없이 유지할 index만 늘어남. |
| continuation page에서 `total` 생략 | **REJECT** | 순회당 COUNT 71개 제거. | 필수 `{ items, total }` envelope 위반(`ASSIGNMENT.md` §5), P5B mutation 이후 count 일관성 약화. |
| 댓글 total 응답 caching | **REJECT** | 위와 같음. | 생성/삭제 시 invalidation 필요; 1 ms 미만이라 불필요. |
| `ANALYZE posts` / planner tuning | **KEEP** (변경 없음) | 잘못된 추정에도 plan은 이미 올바름. | 필요 없음. |

**KEEP:** 목록/상세의 correlated subquery 파생 count(목록 page당 ≤0.7 ms), page별 `total`, requester `EXISTS`, 당시 index, 단일 transaction seed.

## 11. 당시 결정과 다음 단계

1. **Selected → COMPLETE:** row-value keyset 조건(§12). 이 감사가 merge된 뒤 별도 PR에서 구현했고, 전후 재측정과 전체 순회에서 같은 결과를 확인했습니다.
2. **Deferred:** seed batching과 EXISTS + COUNT 병합. 둘 다 위험은 낮지만 측정된 이득이 1회성이거나 noise 범위 안입니다.
3. **Rejected:** 비정규화 `comment_count`, cached total, partial index, `total` 생략.
4. **별도 결정:** 첫 기동 readiness window(§9)는 성능이 아니라 correctness/UX 주제입니다. *(이후 `SeedReadiness`로 해결, §9 참고.)*
5. 당시 계획: P5B는 이 최적화 PR 다음에 진행.

<a name="12-selected-optimization--row-value-keyset"></a>

## 12. 선택한 최적화 — Row-value Keyset

`b86f668`(`perf/row-value-keyset`)에서 구현했습니다. 2026-09-29 ≈11:25–11:40 UTC에 §3과 같은 Compose PostgreSQL 16.15 / WSL2 환경에서 측정했습니다.

### 문제

cursor 조건 `created_at < :t OR (created_at = :t AND id < :id)`는 올바른 keyset 경계지만, PostgreSQL은 이를 `Index Cond: (post_id = …)` 뒤의 `Filter`로 평가했습니다.

### 중요한 이유

page마다 해당 post의 index 범위를 최신 row부터 다시 읽고 cursor 앞의 row를 모두 버렸습니다. scan 작업이 page 깊이에 따라 선형으로 늘었고, 이는 keyset pagination을 고르며 피하려던 OFFSET 동작 그대로였습니다.

### 근본 원인

이번 실행 계획에서 planner는 OR 형태의 조건을 `(post_id, created_at DESC, id DESC)` / `(created_at DESC, id DESC)` index 위의 `(created_at, id)` 복합 범위 경계로 바꾸지 않았습니다. row-value 비교는 btree가 seek할 수 있는 단일 복합 경계입니다.

### 변경

`PostRepository.findReadablePage`와 `CommentRepository.findReadablePage`의 cursor 조건만 바꿨습니다.

```sql
AND (p.created_at, p.id) < (:cursorCreatedAt, :cursorId)
AND (c.created_at, c.id) < (:cursorCreatedAt, :cursorId)
```

변경 전에 확인한 전제:

- 두 query 모두 `created_at DESC, id DESC`로 정렬(같은 방향);
- `created_at`은 `NOT NULL`이고 `id`는 primary key;
- cursor는 정확히 `(Instant createdAt, long id)`(`CursorPosition`).

PostgreSQL은 row constructor를 왼쪽에서 오른쪽으로 비교하고, 처음으로 같지 않은 쌍에서 결과를 정합니다("Row Constructor Comparison", PostgreSQL docs, functions-comparisons). 따라서 null이 아닌 값에서 `(a, b) < (x, y)`는 정확히 `a < x OR (a = x AND b < y)`입니다. 다른 SQL, schema, index, API field는 바꾸지 않았습니다.

### 변경 전후 — 실행 계획

`EXPLAIN (ANALYZE, BUFFERS)`, 두 형태를 같은 session에서 측정했고 DB 재시작 후 warm 5회 median입니다. 제거 row는 scan node의 filter가 제거한 row이고, buffers는 scan node의 값(comments) 또는 statement 전체의 값(row별 subplan이 지배적인 posts)입니다.

| Query | 변경 전 (OR) | 변경 후 (row-value) |
| --- | --- | --- |
| Comments #2, 얕음 (row 2 이후) | `Index Cond: post_id` + cursor `Filter`; 제거 3; buffers 6; 0.084 ms | `Index Cond: post_id AND ROW(created_at, id) < ROW(…)`; 제거 1 (soft delete된 row); buffers 3; 0.126 ms |
| Comments #2, 중간 (row 702 이후) | cursor `Filter`; 제거 **728**; buffers **23**; 0.194 ms | row-value `Index Cond`; 제거 0; buffers **4**; 0.117 ms |
| Comments #2, 가장 깊음 (row 1,402 이후) | cursor `Filter`; 제거 **1,459**; buffers **42**; 0.285 ms | row-value `Index Cond`; 제거 1 (soft delete); buffers **3**; 0.112 ms |
| Posts page 2 (row 10 이후) | Index Scan, cursor `Filter`, 제거 11; 50 buffers; 0.346 ms | Index Scan, row-value `Index Cond`, 제거 0; 50 buffers; 0.497 ms |
| Posts page 4 (row 30 이후) | Seq Scan + Sort, 제거 33; 0.482 ms | Seq Scan + Sort, 제거 33; 0.475 ms |

- 모든 쌍이 같은 row ID를 반환했습니다.
- Comments: scan 작업이 더 이상 깊이에 의존하지 않습니다. 이전에는 6 → 23 → 42였던 buffers가 첫 continuation부터 마지막 page까지 3–4로 유지됩니다.
- Posts: table이 42 rows(heap 2 pages)입니다. page 4에서 planner(11:28에 수집한 통계 기준)는 두 형태 모두 Seq Scan + Sort를 골랐으므로 조건 형태는 상관없었고, page 2에서는 row-value 경계가 `Index Cond`로 쓰였습니다. post 목록 비용은 두 형태 모두 row별 count subplan이 지배하므로 posts에 대한 이득은 주장하지 않습니다.
- warm median 사이의 0.2 ms 미만 차이는 실행 간 변동 범위 안입니다. row-value 실행이 같은 warm cache에서 OR 실행 뒤에 이어졌으므로 cold 수치는 비교하지 않았습니다.

### 정확성

실제 PostgreSQL + 제공 seed에서 API를 통해 변경 전후를 확인했습니다(11개 순회 모두 ID 순서 동일).

- Posts: 읽을 수 있는 post 39개를 `limit=10`(4 pages)과 `limit=3`(13 pages)으로 순회. 각 post가 정확히 한 번, 최신순, DB 순서와 동일, 마지막 `nextCursor: null`, `total: 39`.
- UI page size(2, 이후 20)로 댓글 순회: posts 10 (0), 24 (1), 1 (2), 3 (3), 4 (27), 8 (58), 2 (1,411개를 72 pages). 모두 완전하고 고유하며 최신순, DB 순서와 동일, page 수가 예상대로, 마지막 cursor `null`.
- 같은 timestamp tie: seed에는 같은 timestamp가 없어 임시 fixture를 썼습니다. 한 timestamp를 공유하는 post 5개와 더 오래된 post, 삭제된 post를 추가하고, 한 timestamp를 공유하는 읽을 수 있는 댓글 6개와 삭제된 댓글 1개, 더 오래된 댓글 2개를 가진 post를 추가했습니다. page 경계가 tie 묶음 안에 오도록 `limit=2`로 순회: tie 안에서 ID DESC, 경계를 넘는 중복·누락 없음, 삭제 row 제외, 마지막 cursor `null`. SQL 수준에서도 tie 안의 cursor에 대해 OR와 row-value 형태가 같은 row를 반환했습니다. fixture(ID ≥ 900000)는 이후 제거해 posts 42 / comments 2,076 / likes 0으로 복원했습니다.
- PostgreSQL statement logging에서 다시 build한 backend가 `AND (p.created_at, p.id) < ($2, $3)`와 `AND (c.created_at, c.id) < ($2, $3)`를 보내는 것을 확인했습니다.
- API 계약은 바뀌지 않았습니다. 다시 build한 backend에 대해 P4(48/48), P5A(63/63), P5A revisit(12/12) browser suite가 통과했습니다.

### SQL statement 수

설계상 변하지 않습니다. post #2 전체 순회는 여전히 요청 72개 = `EXISTS` 72 + page `SELECT` 72 + `COUNT` 72 = 216 statements입니다(marker로 구간을 나눈 `log_statement` 수집). 이 최적화는 각 page `SELECT`가 seek하는 방식을 바꿀 뿐, 실행되는 statement 수는 바꾸지 않습니다.

### API 영향

§5와 같은 script(warm-up 5회 + 순차 50회; 전체 순회 ×10). Median, ms:

| 작업 | 변경 전 | 변경 후 |
| --- | --- | --- |
| `GET /posts?limit=10` | 4.96 | 4.58 |
| `GET /posts/2/comments?limit=2` (cursor 없음, 영향 없음) | 7.10 | 8.37 |
| `… limit=20` 첫 continuation | 7.68 | 7.46 |
| `… limit=20` 중간 (page 36) | 6.98 | 6.73 |
| `… limit=20` 마지막 page | 6.89 | 7.17 |
| Post #2 전체 순회 (요청 72개) | 473 | 587 |

backend 재시작 직후 첫 실행은 cursor가 없는 endpoint를 포함해 모든 endpoint가 느렸으므로 JVM warm-up으로 보고, 표는 두 번째 실행을 씁니다. 이 변경이 영향을 줄 수 없는 endpoint도 비슷하게 움직였습니다(예: `GET /posts/2` 6.28 → 8.68 ms). **이 dataset 크기에서 API latency 변화는 의미 있는 수준이 아니지만, index 작업이 더 이상 cursor 깊이에 따라 늘지 않습니다.**

### 결정

선택한 최적화는 `COMPLETE`입니다. 나머지는 §10의 결정대로 둡니다. seed batching과 EXISTS + COUNT 병합은 `DEFERRED`, 비정규화 `comment_count`, cached total, partial index, `total` 생략은 `REJECTED`입니다. COUNT는 저렴하고(≈0.39 ms), seed 비용은 1회성이며, 비정규화는 일관성 위험을 더하고, partial index는 측정된 이득이 없었습니다.

---

<a name="13-frontend-list-batch-size-benchmark--2026-10-02"></a>

## 13. Frontend 목록 Batch Size Benchmark — 2026-10-02

측정만 수행했고 product code, dependency, API는 바꾸지 않았습니다. Base: `main` `e12dce5`(first-page SSR merge 시점). 판정: **`KEEP_15_15`** — `SSR_SIZE = 15`, `CONTINUATION_SIZE = 15`. *(현재 code도 `POST_LIST_PAGE_SIZE = 15`를 SSR과 continuation에 함께 씁니다.)*

### 13.1 측정 이유

당시 post 목록에는 상수 하나 `POST_LIST_PAGE_SIZE = 15`가 있었고, 이 값은 그 전에 읽을 수 있는 seed post 39개를 순회해 정한 것이었습니다(`10/15/20`에서 요청 `4/3/2`개). 두 가지 약점이 남아 있었습니다.

- 읽을 수 있는 post가 39개뿐이면 모든 후보가 곧 데이터 끝에 닿아, 마지막 page 효과가 비교에 섞이고 15가 "39개를 세 번에 나눈 값"처럼 보일 수 있습니다.
- first-page SSR 이후에는 "HTML이 담는 post 수"(`SSR_SIZE`)와 "이후 cursor 요청마다 가져오는 수"(`CONTINUATION_SIZE`)가 서로 다른 단계(첫 render vs scroll)에 작용하는 별개의 기술적 결정입니다.

이 benchmark는 어떤 후보도 끝에 닿지 않을 만큼 긴 dataset에서 두 값을 따로 측정합니다.

### 13.2 환경

| 항목 | 값 |
| --- | --- |
| Base | `main` `e12dce5f1e73b5047e5c6689a0ff052cddd4691c` |
| Viewport | 375×778, DPR 2, mobile emulation |
| CPU | 4× slowdown |
| Network | Lighthouse devtools mobile throttling: request latency 562.5 ms, download 1.47 Mbps |
| 도구 | Lighthouse 13.5.0 (`--throttling-method=devtools`); 같은 조건을 CDP로 적용한 Playwright |
| Build | 두 size만 environment variable에서 읽도록 바꾼 `main` 임시 복사본의 production Docker image 하나 |
| 영구 compose DB | 건드리지 않음: 전후 모두 posts 42, comments 2,076, post_likes 1 |

### 13.3 일회용 dataset

별도 PostgreSQL + backend stack에 **보이는 post 160개와 댓글 9,147개**를 넣고, 측정 후 제거했습니다.

- 읽을 수 있는 seed post 39개를 바탕으로 만들었고, 제목 길이(12–21), 본문 길이(63–77), 작성자 9명, `likeCount` 분포, `commentCount` 분포(0–1,411), `createdAt` 간격을 유지했습니다.
- 모든 `id`와 `createdAt`은 고유합니다.
- 160은 page size 선택의 입력이 아닙니다. 데이터 끝 효과를 없애 모든 후보를 같은 **고정 소비 깊이 30·45 posts**에서 비교하기 위한 것이며, 160개를 끝까지 읽지는 않았습니다. page size는 전체 row 수에서 끌어내지 않습니다(13.7 참고).

### 13.4 SSR_SIZE benchmark

`CONTINUATION_SIZE`는 15로 고정. Lighthouse devtools, 후보마다 5회(median, range). Backend fetch와 Next TTFB는 throttling 없이 잰 20-sample median입니다.

| 지표 | SSR 10 | SSR 15 | SSR 20 |
| --- | ---: | ---: | ---: |
| Backend fetch | 13.1 ms | 10.3 ms | 10.8 ms |
| Next TTFB | 27.7 ms | 22.9 ms | 22.7 ms |
| HTML transfer | 7.7 KB | 8.7 KB | 9.6 KB |
| HTML resource | 33.0 KB | 45.6 KB | 58.2 KB |
| SSR API payload | 3,583 B | 5,308 B | 7,032 B |
| DOM elements | 230 | 330 | 430 |
| 완전히 + 일부 보이는 post | 6 + 1 | 6 + 1 | 6 + 1 |
| 화면 밖 SSR row | 3 | 8 | 13 |
| LCP median | 2,278 ms | 2,260 ms | 2,372 ms |
| LCP range | 2,243–2,401 | 2,103–2,345 | 2,162–2,665 |
| TBT median | 180 ms | 156 ms | 164 ms |
| CLS | 0.0003 | 0.0003 | 0.0003 |
| JS complete | 2,532 ms | 2,520 ms | 2,526 ms |
| Browser 초기 `GET /api/posts` | 0 | 0 | 0 |

- SSR row가 5개 늘 때마다 압축 HTML이 약 0.9 KB, DOM element가 약 100개 늘지만 LCP range가 겹칩니다. 차이는 측정 noise 범위 안입니다.
- 모든 후보에서 첫 viewport에는 완전한 post 6개와 일부 post 1개가 보입니다. SSR 15는 fold 아래에 8 rows(약 1.2 화면)를 준비해 둡니다.

**Phase A 판정: `SSR_SIZE_EQUIVALENT`.** 10은 더 작지만 LCP나 server 비용 이득이 없었습니다. 20은 일관된 LCP 이득 없이 초기 데이터와 DOM을 늘렸고, longest task가 튀는 실행이 한 번 있었습니다. 당시 값인 15를 Phase B에서도 유지했습니다.

### 13.5 CONTINUATION_SIZE benchmark

`SSR_SIZE`는 15로 고정. hydration 뒤 시작하는, 모든 후보에 같은 자동 scroll profile 두 개:

- **빠르게 훑기:** 500 ms마다 300 px (≈600 px/s), 5회.
- **읽기:** 500 ms마다 150 px (≈300 px/s), 3회.

"소비"는 post row가 viewport에 들어온 것을 뜻합니다. Overfetch는 `loaded − consumed`입니다.

**30개 소비 — 빠르게 훑기 (median)**

| 지표 | cont 10 | cont 15 | cont 20 |
| --- | ---: | ---: | ---: |
| 다음 page 요청 | 2 | 1 | 1 |
| API bytes | 7.7 KB | 5.6 KB | 7.3 KB |
| HTML + API bytes | 16.4 KB | 14.3 KB | 16.0 KB |
| 로드됨 | 35 | 30 | 35 |
| Overfetch | 5 | 0 | 5 |
| 경과 시간 | 4,668 ms | 4,644 ms | 4,668 ms |
| 목록 끝에서 가장 긴 대기 | 224 ms | 275 ms | 277 ms |
| Loading 문구 노출 | 417 ms | 749 ms | 277 ms |

**45개 소비 (빠르게 / 읽기)**

| 지표 | cont 10 | cont 15 | cont 20 |
| --- | ---: | ---: | ---: |
| 다음 page 요청 | 3 / 4 | 2 / 3 | 2 / 2 |
| API bytes | 11.6 / 15.5 KB | 11.2 / 16.8 KB | 14.7 / 14.7 KB |
| Overfetch | 0 / 10 | 0 / 15 | 10 / 10 |
| 경과 시간 (빠르게) | 7,788 ms | 7,763 ms | 7,248 ms |
| 가장 긴 대기 (빠르게) | 229 ms | 275 ms | 277 ms |
| 누적 대기 (빠르게) | 590 ms | 489 ms | 277 ms |
| Loading 문구 노출 (읽기) | 188 ms, 빈 영역 관찰 | 165 ms | 236 ms |
| 목록 끝 대기 (읽기) | 0 | 0 | 0 |
| DOM elements (빠르게) | 950 | 950 | 1,149 |
| Scripting (빠르게) | 394 ms | 346 ms | 318 ms |
| Longest task (빠르게) | 55 ms | 58 ms | 73 ms |

어떤 실행에서도 중복 post와 요청 오류는 없었습니다. 각 다음 page 응답은 약 600–623 ms 걸렸고, 그 row가 DOM에 반영되기까지 약 47–71 ms가 더 걸렸습니다. 목록은 virtualize되지 않으므로 로드된 row는 DOM에 남습니다.

### 13.6 해석

- **Continuation 10:** 두 깊이 모두에서 요청이 더 많았고, 읽기 속도에서도 loading 문구와 빈 영역이 나타난 유일한 후보였습니다. 작은 payload가 더 잦은 page 경계를 상쇄하지 못했습니다.
- **Continuation 20:** 빠르게 훑을 때 경계를 하나 덜 넘어 누적 대기가 줄 때가 있었지만, 읽기 profile에서는 일관된 이득이 없었습니다. 어느 후보가 더 overfetch하거나 더 많은 bytes를 보내는지는 scroll timing에 따라 뒤바뀌었고, DOM에 더 많은 row가 쌓입니다. SSR 15와 짝을 지으면 첫 page size를 따로 두고 분기 규칙도 필요합니다.
- **Continuation 15:** 10보다 요청이 적고, 20 대비 일관된 단점이 없으며, SSR page와 크기가 같아 cache, query key, pagination이 균일하게 유지됩니다.

**관찰 (결론 아님).** 다음 page는 요청 후 약 650 ms 뒤에 보였습니다(응답 ≈600–623 ms + DOM까지 ≈47–71 ms). 당시 `rootMargin` 200 px는 600 px/s에서 약 0.33 s, 300 px/s에서 약 0.67 s의 여유를 줍니다. 따라서 빠르게 훑을 때의 대기는 page size보다 다음 page를 얼마나 일찍 요청하느냐(prefetch 거리)에 더 민감해 보였습니다. 이는 확인할 후속 항목으로 남겼으며, 200 px가 틀렸다거나 다른 값이 낫다는 뜻은 아닙니다.

### 13.7 결정

**`KEEP_15_15`.** 15는 seed의 읽을 수 있는 post 39개를 세 번의 요청으로 나누려고 고른 값이 아닙니다. 데이터 끝을 피할 만큼 큰 dataset에서 첫 render 비용과 고정 깊이 scroll을 분리해 측정한 결과, 10/15/20 후보는 15/15를 바꿀 이유를 주지 않았습니다.

- `SSR_SIZE`와 `CONTINUATION_SIZE`는 기술적으로 독립이지만, 이 결과는 둘을 다르게 할 성능상 이유를 주지 않았으므로 더 단순한 같은 값을 유지합니다.
- page size는 전체 post 수에서 끌어내지 않습니다. cursor pagination은 요청마다 `limit + 1` rows만 읽고, 전체 수는 끝까지 읽는 사용자의 요청 횟수만 바꿉니다. 일반적인 session에서 중요한 것은 소비 깊이이므로 고정 깊이 30·45로 비교했습니다.

### 13.8 한계

- emulate한 기기, viewport, CPU 배율, network profile이 각각 하나뿐이며 모든 사용자 기기를 대표하지 않습니다.
- scripted scroll profile 두 개뿐이며 모든 실제 scroll 동작을 대표하지 않습니다.
- Docker 안의 HTTP/1.1 `next start`; HTTP/2 production network는 측정하지 않았습니다.
- 10/15/20만 비교했습니다. 이 조건에서 비교한 후보 중 가장 합리적인 값을 정한 것이지 절대적·전역 최적값이 아닙니다.
- benchmark script, patch한 복사본, 합성 seed는 repository 밖에 두었고 commit하지 않았습니다.

### 13.9 당시 다음 가설

같은 고정 깊이에서 `rootMargin` 200 px를 더 큰 값(예: 300/400/500 px)과 비교하며 목록 끝 대기, loading 문구 노출, 사용자가 scroll하기 전에 보낸 요청을 측정한다. 측정 전에는 `rootMargin`을 바꾸지도 권장하지도 않는다. *(이후: §14에서 측정.)*

---

## 14. Infinite Scroll Prefetch 거리 Benchmark — 2026-10-02

Base: `main` `b23d0df`. 판정: post 목록은 **`CHANGE_TO_500`** — 이 조건에서 비교한 200/300/400/500 px 중 가장 합리적인 값이며 일반적 최적값이 아닙니다. 댓글 continuation은 측정하지 않았으므로 hook 기본값(200 px)을 유지합니다. *(현재 code: `POST_LIST_PREFETCH_ROOT_MARGIN = "0px 0px 500px 0px"`, 댓글은 hook 기본 200px.)*

### 14.1 측정 이유

§13은 page size 15/15를 유지했지만, 빠른 scroll 대기가 page 크기보다 다음 page를 언제 요청하느냐에 더 민감해 보인다는 점을 관찰했습니다. 여기서 바꾼 변수는 목록 sentinel의 아래쪽 `rootMargin`(`0px 0px Npx 0px`) 하나뿐입니다. page size(15/15), SSR, debounce, `staleTime`, retry, SSR timeout, API, query key는 그대로입니다.

### 14.2 환경

§13과 같습니다. 375×778, DPR 2, CPU 4×, CDP로 request latency 562.5 ms / download 1.47 Mbps, production Docker build, SSR 15, continuation 15, 같은 일회용 160-post / 9,147-comment dataset(영구 compose DB는 건드리지 않음: 42 / 2,076 / 1). `main` 임시 복사본의 image 하나가 margin을 environment variable에서 읽었고, container 네 개가 200/300/400/500을 실행했습니다. scroll profile: 빠르게 훑기 300 px / 500 ms (≈600 px/s, 5회), 읽기 150 px / 500 ms (≈300 px/s, 3회). 대기는 page가 document 끝까지 scroll된 시간이고, "빈 영역"은 마지막 row 아래에 row가 아닌 공간이 60 px 넘게 보이는 것입니다.

### 14.3 Latency 모델

다음 page cycle마다(모든 cycle, 모든 실행) median은 모든 margin에서 같았습니다. API 응답 600–611 ms, 응답 → DOM 삽입 48–78 ms(요청부터 row까지 ≈0.66 s). 600 px/s에서는 그동안 ≈400 px, 300 px/s에서는 ≈200 px를 이동합니다. 200/300 px에서는 사용자가 요청 시작 후 ≈530 ms에 로드된 목록 끝에 닿아, 필요한 ≈660 ms보다 빨랐습니다. 대기는 API나 DOM 삽입이 느려서가 아니라 요청 시작이 늦어서 생겼습니다.

### 14.4 후보별 geometry

측정값: viewport 778 px, header 48 px, toolbar 44 px, row 108 px, 첫 15 rows 1,620 px, sentinel top 1,712 px(첫 viewport 아래 934 px). row 단위 여유: 200 px ≈ 1.85, 300 px ≈ 2.78, 400 px ≈ 3.70, 500 px ≈ 4.63. page가 추가될 때마다 sentinel은 1,620 px 더 아래로 이동합니다.

### 14.5 Scroll 없음 / 한 번 scroll

| 지표 (각 3회) | 200 | 300 | 400 | 500 |
| --- | ---: | ---: | ---: | ---: |
| Continuation 요청, scroll 없음 (3.5 s) | 0 | 0 | 0 | 0 |
| Continuation 요청, 300 px 한 번 scroll 후 4 s 대기 | 0 | 0 | 0 | 0 |
| 로드된 row | 15 | 15 | 15 | 15 |

sentinel은 viewport 아래 934 px(scroll 없음)와 634 px(한 번 scroll 후)에 있어 모든 후보 margin 밖이었습니다. throttling 없이도 같았습니다.

### 14.6 빠르게 훑기 (5회, median)

| 지표 | 200 | 300 | 400 | 500 |
| --- | ---: | ---: | ---: | ---: |
| 30 / 45 / 60까지 요청 | 2 / 3 / 4 | 2 / 3 / 4 | 2 / 3 / 4 | 2 / 3 / 4 |
| 30 / 45 / 60 시점 로드 | 30 / 45 / 60 | 45 / 45 / 75 | 45 / 60 / 75 | 45 / 60 / 75 |
| 가장 긴 대기 (60까지) | 305 ms | 245 ms | 0 | 0 |
| 30 / 45 / 60까지 누적 대기 | 305 / 477 / 670 ms | 245 / 245 / 419 ms | 0 / 0 / 0 | 0 / 0 / 0 |
| 60까지 loading 문구 노출 (횟수) | 2,105 ms (4) | 844 ms (4) | 382 ms (2) | 208 ms (1) |
| 60까지 빈 영역 노출 | 670 ms | 419 ms | 192 ms | 0 |
| 60 시점 API bytes | 16.8 KB | 22.4 KB | 22.4 KB | 22.4 KB |
| 60까지 경과 시간 | 10,905 ms | 10,365 ms | 9,875 ms | 9,839 ms |
| 대기가 있었던 cycle | 15 of 15 | 10 of 20 | 0 of 20 | 0 of 20 |

요청 수는 checkpoint까지 시작된 요청이며 진행 중인 요청 하나를 포함합니다. 어떤 실행에서도 중복이나 요청 오류는 없었습니다. Range: 200 가장 긴 대기 256–321 ms, 300 224–262 ms; 400 빈 영역은 모든 실행에서 157–230 ms; 500 loading 문구 171–272 ms.

### 14.7 읽기 (3회, median)

60 posts까지 어떤 후보도 목록 끝에서 기다리거나 빈 영역을 보이지 않았습니다. 200 px는 loading 문구를 한 번 보였고(234 ms, range 192–234) 300/400/500은 보이지 않았습니다. 모든 margin에서 30 / 45 / 60 시점 요청은 2 / 3 / 4였고, 로드된 row도 200의 60 시점(60 vs 75)을 빼면 같았습니다.

### 14.8 일찍 떠나기 (5회)

사용자가 약 22개 post가 viewport에 들어올 때까지 훑은 뒤, 보이는 post를 열었습니다(앱 내 이동).

| 지표 | 200 | 300 | 400 | 500 |
| --- | ---: | ---: | ---: | ---: |
| 소비 | 23 | 23 | 24 | 24 |
| 로드 | 30 | 30 | 30 | 30 |
| 쓰지 않은 row | 7 | 7 | 6 | 6 |
| Continuation 요청 / API bytes | 1 / 5.6 KB | 1 / 5.6 KB | 1 / 5.6 KB | 1 / 5.6 KB |
| 떠날 때 진행 중 / 취소 | 0 / 0 | 0 / 0 | 0 / 0 | 0 / 0 |
| 떠나기 전 대기 | 240 ms | 247 ms | 0 | 0 |

이 깊이에서는 더 큰 margin이 추가 비용을 만들지 않았습니다. 200/300 사용자는 이미 한 번 기다렸습니다.

### 14.9 연쇄 prefetch

없었습니다. 어떤 후보도 scroll 없이 page를 요청하지 않았고, 한 번의 scroll이 요청을 일으키지 않았으며, 추가 scroll 없이 page 뒤에 또 요청이 이어지지 않았습니다. page가 추가되면 sentinel이 1,620 px 더 아래로 가서 어떤 후보 margin보다도 멀어지기 때문입니다. 더 큰 margin은 checkpoint에서 최대 한 page(15 rows, 5.6 KB)만 앞서 있었습니다. throttling 없이(각 3회)는 어떤 후보도 기다리지 않았고, 200 px는 loading 문구를 0–40 ms 보였으며, 요청 수와 로드 수는 같았습니다.

### 14.10 결정

**`CHANGE_TO_500`** (목록만).

- 우선순위 1 (빠른 scroll 공백): 200은 모든 page 경계에서, 300은 절반에서 기다렸습니다. 400은 대기는 없앴지만 모든 실행에서 빈 영역(157–230 ms)과 loading 문구가 보였습니다. 500은 대기와 빈 영역을 없앴고, 짧은 loading 문구 노출이 한 번 남았습니다.
- 우선순위 2 (읽기): 어떤 후보도 기다리지 않았고, 500은 loading 문구가 없었습니다.
- 우선순위 3–6 (이른 fetch, 일찍 떠날 때 overfetch, 연쇄, bytes): 400과 500은 같았습니다. scroll 전 요청 없음, 일찍 떠날 때 같은 단일 요청과 쓰지 않은 row 6개, 연쇄 없음, 최대 한 page 앞섬(일부 checkpoint에서 200 대비 +5.6 KB).
- 구현: 목록이 `POST_LIST_PREFETCH_ROOT_MARGIN = "0px 0px 500px 0px"`를 넘깁니다. 이 benchmark가 측정하지 않은 댓글 continuation은 hook 기본값(200 px)을 유지합니다.

일회용 dataset에서 branch 검증: scroll 없이 요청 없음(3/3), 빠르게 훑을 때 대기·빈 영역 없음(3/3, loading 문구 187–211 ms 한 번), post 160개 전체 순회에서 중복·누락 없음, 목록 요청 10개, 끝 이후 요청 없음.

### 14.11 한계

- emulate한 기기, viewport, CPU 배율, network profile이 각각 하나이고 scripted scroll 속도는 두 가지뿐입니다. 실제 기기와 scroll 동작은 다양합니다.
- 더 큰 viewport는 측정하지 않았습니다. geometry상 viewport가 약 1,212 px 이상이면 500 px margin에서 scroll 전에 sentinel에 닿아 page 2를 한 번 요청합니다(400 px에서는 1,312 px).
- Docker 안의 HTTP/1.1 `next start`; HTTP/2 production network는 측정하지 않았습니다.
- 200/300/400/500만 비교했습니다. 전역 최적값이 아니며 500 px 초과 값은 test하지 않았습니다.
- benchmark script, patch한 복사본, 합성 seed는 repository 밖에 두었습니다.

---

## 15. 댓글 Continuation과 긴 목록 Rendering Benchmark — 2026-10-03

Base: `main` `3514922`. 측정만 수행했고 product code는 바꾸지 않았습니다. 판정: continuation **`KEEP_30`**, rendering **`VIRTUALIZATION_NOT_NEEDED_NOW`**, 댓글 prefetch **`KEEP_COMMENT_ROOT_MARGIN_200_FOR_NOW`**.

### 15.1 측정 이유

`COMMENT_PAGE_SIZE = 30`은 그 전에 post #2의 댓글 1,411개 전체를 순회해 정한 값이었습니다(`10/20/30`에서 요청 `141/71/47`개). 즉 전체 크기가 비교를 이끌었습니다. 이 benchmark는 대신 고정 읽기 깊이에서 크기를 비교하고, 로드한 댓글을 모두 DOM에 유지하는 것이 사용자에게 보이는 문제가 되는지도 따로 확인합니다.

### 15.2 측정 당시 계약 (현재도 동일)

- `INITIAL_COMMENT_COUNT = 2`는 product 계약이라 바꾸지 않았습니다. 첫 요청은 `limit=2`, 최대 2개만 render하고 "댓글 더보기" 전에는 이어서 가져오지 않습니다.
- 더보기 이후 page는 `COMMENT_PAGE_SIZE`를 씁니다(backend 최대 50, `PageRequestParser.MAX_LIMIT`; `limit=51`은 400).
- 댓글 continuation은 hook 기본 `rootMargin`(200 px)을 쓰며, 여기서는 고정했습니다.
- `gcTime: 0`은 상세 화면이 unmount될 때 댓글 query를 제거합니다.

### 15.3 환경

375×778, DPR 2, CPU 4×, CDP로 request latency 562.5 ms / download 1.47 Mbps, production Docker build. 일회용 PostgreSQL + backend에 repository 자체 seed를 넣었습니다(posts 42, comments 2,076; post #2는 읽을 수 있는 댓글 1,411개, 각 14–32자). 영구 compose DB는 건드리지 않았습니다(42 / 2,076 / 1). `main` 임시 복사본의 image 하나가 요청 시점에 continuation size를 environment variable에서 읽었고, container 네 개가 10/20/30/50을 실행했습니다.

post #2의 geometry: 댓글 section은 272 px, 더보기 button은 511 px에서 시작하고, 고정 composer가 viewport의 692 px부터를 덮습니다. 댓글 row는 72 px입니다(median; 삭제 action이 있는 requester 본인 댓글은 최대 120 px). 댓글 row가 composer 위 영역에 들어오면 소비한 것으로 셉니다. 모든 후보에 같은 scroll profile은 더보기 직후 시작합니다. 빠르게 216 px / 500 ms (3 rows, ≈432 px/s, 5회), 읽기 72 px / 500 ms (1 row, ≈144 px/s, 3회).

### 15.4 초기 계약 확인

모든 후보의 57회 실행 전체에서: `limit=2` 요청 1개, render된 댓글 2개, heading `댓글(1,411)`, 더보기 표시, 더보기 전에 sentinel을 화면에 scroll해도 continuation 요청 없음.

### 15.5 첫 더보기 latency (5회, median)

| 지표 | 10 | 20 | 30 | 50 |
| --- | ---: | ---: | ---: | ---: |
| Click → 요청 | 9 ms | 10 ms | 11 ms | 10 ms |
| API | 607 ms | 619 ms | 633 ms | 666 ms |
| 응답 → DOM | 27 ms | 29 ms | 30 ms | 44 ms |
| Click → 새 row | 639 ms | 660 ms | 670 ms | 725 ms |
| Loading 문구 노출 | 672 ms | 714 ms | 752 ms | 853 ms |
| 응답 bytes | 1,914 B | 3,380 B | 4,906 B | 7,927 B |

### 15.6 고정 깊이 결과

아래 대기에는 첫 더보기 대기가 포함되지 않습니다(첫 continuation이 올 때까지 page는 끝에 있음; 15.5 참고).

**빠르게 (5회, median)**

| 지표 | 10 | 20 | 30 | 50 |
| --- | ---: | ---: | ---: | ---: |
| 30 / 60 / 120까지 요청 | 4 / 7 / 13 | 2 / 4 / 7 | 2 / 3 / 5 | 1 / 2 / 3 |
| 120 시점 로드 (overfetch) | 132 (12) | 122 (2) | 122 (2) | 152 (32) |
| 120까지 API bytes | 25.1 KB | 20.6 KB | 19.7 KB | 23.7 KB |
| 120까지 scroll 중 대기 | 314 ms | 141 ms | 187 ms | 167 ms |
| 120까지 추가 loading 문구 노출 횟수 | 8 | 2 | 1 | 2 |
| 120까지 빈 영역 (첫 더보기 제외) | 451 ms | 277 ms | 187 ms | 167 ms |
| 120 시점 DOM elements | 984 | 915 | 915 | 1,125 |
| 120까지 Scripting / style+layout | 780 / 362 ms | 745 / 347 ms | 672 / 353 ms | 679 / 354 ms |
| 더보기 이후 longest task | 213 ms | 222 ms | 254 ms | 264 ms |

**읽기 (3회, median)**

| 지표 | 10 | 20 | 30 | 50 |
| --- | ---: | ---: | ---: | ---: |
| 30 / 60 / 120까지 요청 | 4 / 7 / 13 | 2 / 4 / 7 | 2 / 3 / 5 | 1 / 2 / 3 |
| 120 시점 로드 (overfetch) | 132 (12) | 142 (22) | 152 (32) | 152 (32) |
| 120까지 API bytes | 25.1 KB | 23.9 KB | 24.5 KB | 23.7 KB |
| Scroll 중 대기 / 추가 loading 문구 / 빈 영역 | 0 / 0 / 0 | 0 / 0 / 0 | 0 / 0 / 0 | 0 / 0 / 0 |

고정 깊이 실행 어디에서도 중복 댓글과 요청 오류는 없었습니다. page당 API 시간(모든 cycle)은 586–641 ms, 응답 → DOM은 20–38 ms(median)였고 page size에 따라 조금씩 늘었습니다.

### 15.7 일찍 떠나기 (5회)

더보기 후 약 25개 댓글을 소비할 때까지 훑은 뒤 뒤로 가기 button으로 떠났습니다.

| 지표 | 10 | 20 | 30 | 50 |
| --- | ---: | ---: | ---: | ---: |
| 소비 | 26 | 26 | 26 | 26 |
| 로드 | 32 | 42 | 32 | 52 |
| 쓰지 않음 | 6 | 16 | 6 | 26 |
| Continuation API bytes | 5.8 KB | 6.8 KB | 4.9 KB | 7.9 KB |
| Continuation 요청 | 3 | 2 | 1 | 1 |
| 떠날 때 진행 중 / 취소 | 0 / 0 | 0 / 0 | 0 / 0 | 0 / 0 |

### 15.8 1,411개 전체 stress (continuation 30)

댓글 1,411개가 모두 로드될 때까지 page를 끝까지 반복 scroll했습니다(3회; 각 continuation 요청 47개). Script와 style/layout 시간은 이전 checkpoint 이후 구간의 합계입니다(구간당 page: 4, 6, 10, 14, 13). 각 checkpoint에서 loading이 멈춘 상태로 같은 probe를 실행했습니다. `scrollBy(0, 400)` 후 1 s 동안 frame 간격을 기록했습니다.

| 지표 (실행 median) | 100 | 300 | 600 | 1,000 | 1,411 |
| --- | ---: | ---: | ---: | ---: | ---: |
| Render된 댓글 | 122 | 302 | 602 | 1,022 | 1,411 |
| DOM elements | 928 | 2,195 | 4,309 | 7,263 | 10,001 |
| 사용 중 JS heap | 6.1 MB | 7.9 MB | 9.3 MB | 11.9–14.4 MB | 11.3–13.7 MB |
| 구간 내 scripting | 316 ms | 519 ms | 1,117 ms | 2,056 ms | 2,639 ms |
| 구간 내 style + layout | 182 ms | 341 ms | 232 ms | 338 ms | 400 ms |
| 구간 내 long task (>50 ms) | 0–1 | 1 | 0 | 2–7 | 13–14 |
| 구간 내 longest task | 0–128 ms | 238–299 ms | 0 ms | 65–69 ms | 72–82 ms |
| 마지막 page 응답 → DOM | 27–32 ms | 22–34 ms | 32–42 ms | 43–59 ms | 65–88 ms |
| Probe: scroll 명령 → 위치 갱신 | 9–10 ms | 0–6 ms | 3–7 ms | 2–8 ms | 1–8 ms |
| Probe: 1 s 안에 50 ms 넘는 frame | 1 | 0 | 0 | 0 | 0 |

중복은 없었고 모든 실행이 1,411에 도달했습니다. 한 실행에서 요청 하나가 이유가 저장되지 않은 채 실패로 기록되었습니다(그 실행도 중복 없이 1,411개를 모두 로드). 이유를 기록하도록 다시 실행했을 때는 실패가 없었습니다.

### 15.9 `gcTime` 재방문

목록에서 들어가 댓글을 로드하고, 앱 안에서 뒤로 간 뒤 목록에서 post #2를 다시 열었습니다(122개 후 2회, 1,411개 후 3회). 모든 재방문은 `limit=2` 요청 정확히 1개를 보내고, 댓글 2개를 render하고, 더보기를 보였으며, sentinel을 화면에 scroll한 3.5 s 동안 continuation 요청을 보내지 않았습니다. 로드했던 122개도 1,411개도 다시 나타나지 않았습니다. 이는 성능 결정이 아니라 초기 댓글 2개와 명시적 더보기 gate를 지키는 cache 동작의 확인입니다.

### 15.10 Virtualization 판정

**`VIRTUALIZATION_NOT_NEEDED_NOW`.** 댓글 1,411개에서 DOM은 약 10,000 element, heap은 약 14 MB까지 커지고 append 한 번의 비용이 늘었습니다. 응답 → DOM은 대략 30 ms → 65–88 ms, 50 ms 넘는 long task는 600개까지 구간당 0–1개였다가 1,000–1,411 사이에서 13–14개(최대 72–82 ms)가 되었습니다. scroll 자체는 나빠지지 않았습니다. 모든 checkpoint에서 probe는 10 ms 안에 위치를 갱신했고, 첫 checkpoint 이후로는 50 ms 넘는 frame이 없었습니다. 측정된 비용은 scroll이 아니라 약 1,000개 이후의 append에 있으며, emulate한 저사양 CPU에서도 task당 100 ms 미만입니다. 이 측정 조건에서는 append 비용 증가 신호는 있었지만 사용자에게 보이는 scroll 저하는 확인되지 않아, 지금 virtualization의 복잡도를 더할 근거가 부족했습니다. 더 긴 대화나 더 느린 기기가 나타나면 늘어나는 append 비용을 다시 확인할 신호로 삼습니다.

### 15.11 결정

- **Continuation: `KEEP_30`.** 10/20/30/50 중 30을 바꿀 측정된 이유가 없었습니다. 10은 요청이 가장 많고 훑을 때 loading 노출이 가장 많았습니다. 50은 요청이 적은 대신 첫 더보기가 30보다 약 55 ms 느렸고, 일찍 떠날 때 쓰지 않은 댓글 26개를 로드했으며(30은 6개), 더보기 이후 task가 가장 컸습니다. 20은 30과 비슷했지만 요청이 더 많고, 30/60개에서 loading 문구를 더 자주 보였으며, 일찍 떠날 때 쓰지 않은 댓글 16개를 남겼습니다.
- **댓글 prefetch: `KEEP_COMMENT_ROOT_MARGIN_200_FOR_NOW`.** 읽기에서는 어떤 크기에서도 대기, loading 문구, 빈 영역이 없었습니다. 30으로 훑을 때 120개 중 짧은 대기(158–201 ms)가 한 번 있었습니다. `rootMargin`을 다시 test할 만큼 반복되지 않았습니다.
- **Rendering:** virtualization 없음(15.10). *(현재 code: `INITIAL_COMMENT_COUNT = 2`, `COMMENT_PAGE_SIZE = 30`, 댓글 query `gcTime: 0`, virtualization 미도입.)*

### 15.12 한계

- emulate한 기기, viewport, CPU 배율, network profile이 각각 하나이고, 측정한 72 px row에서 고른 scripted scroll 속도 두 가지뿐입니다.
- 실제 최악 대화는 하나(post #2)뿐이며 더 긴 대화는 생성하지 않았습니다.
- long task와 heap은 Chrome의 `PerformanceObserver`와 `Performance.getMetrics` 값입니다. FPS는 측정하지 않았고 고정 probe 동안의 frame 간격만 측정했습니다.
- Docker 안의 HTTP/1.1 `next start`; HTTP/2는 측정하지 않았습니다.
- 10/20/30/50만 비교했습니다. 전역 최적값이 아닙니다.
- benchmark script, patch한 복사본, 일회용 DB는 repository 밖에 두었습니다.

## 16. Client Query Retry 실패 UX 감사 — 2026-10-03

**판정: `CHANGE_TO_ONE_RETRY_CLASSIFIED`.** 이 감사는 browser query retry만 바꿨습니다. 당시 30 s였던 SSR seed freshness, 1.5 s였던 server-fetch timeout(둘 다 이후 §17에서 변경), page size, root margin, API/backend 동작, schema, mutation 전략은 바꾸지 않았습니다.

### 당시 기준선

당시 설치된 `@tanstack/query-core` 5.104.0을 같은 browser query 기본값으로 `QueryObserver`를 통해 추적했습니다. 기본값은 retry 3회(시도 4회)이고, 바꾸지 않은 retry 지연은 `min(1000 * 2 ** failureCount, 30000)`, 즉 1 s, 2 s, 4 s입니다. source의 retry callback은 첫 실패에서 `failureCount === 0`을 받습니다. mutation은 mutation이 override를 주지 않는 한 별도로 `retry: 0`을 쓰며, 이 application의 mutation은 override를 주지 않습니다.

| 시나리오 | 시도 | 최종 / 복구 시간 | 관찰 |
| --- | ---: | ---: | --- |
| 목록/검색 영구 network 실패 | 4 | error까지 7.04 s | 목록 query와 같은 전역 기본값 |
| 목록/검색 영구 500 | 4 | error까지 7.04 s | 이 변경 전에는 `ApiError(500)`을 따로 분류하지 않음 |
| 상세 영구 network 실패 | 4 | error까지 7.04 s | 상세의 이전 override도 retry 3회 허용 |
| 상세 영구 500 | 4 | error까지 7.04 s | 상세의 이전 override도 retry 3회 허용 |
| 상세 404 | 1 | not-found error 경로까지 2 ms | 이전 상세 전용 callback은 404만 제외 |
| 첫 댓글 / continuation 영구 실패 | 4 | 최종 상태까지 약 7 s | 둘 다 전역 query 기본값 상속 |
| 한 번 실패 후 성공 | 2 | 1.01 s | 복구 전 최종 error 상태 없음 |
| 두 번 실패 후 성공 | 3 | 3.02 s | 두 번째 자동 retry가 약 2 s 추가 |

기존 목록·댓글 화면은 continuation이 실패해도 로드된 page를 유지하고 자체 `다시 시도` control을 보여 줍니다. infinite-scroll trigger는 그 명시적 retry 전까지 비활성입니다. 초기 query UI는 최종 상태 이후에만 나타나므로, 긴 대기는 error 깜빡임이 아니라 긴 loading 구간입니다.

### 취소와 장애 모델

`request()`는 React Query signal이 abort되면 abort된 fetch를 그대로 다시 throw합니다. observer 취소 추적에서 abort된 시도 1회, retry 0회, error 상태 없음을 확인했습니다. 검색/정렬 변경과 상세 unmount는 이미 그 signal을 `getPosts`, `getPost`, `getComments`로 전달하며, retry 정책은 취소를 가로채지 않습니다.

backend read client는 완료된 HTTP 실패를 `status`와 `code`를 가진 `ApiError`로 표현합니다. 이 service에서 현재 4xx read 응답은 모두 결정적인 요청/resource 실패이므로, 같은 요청을 반복해도 결과가 바뀌지 않습니다. 특히 없거나 soft delete된 post의 404는 즉시 not-found 경로로 가야 합니다. network 실패, 5xx 응답, 알 수 없는 query 실패는 retry 한 번으로 복구될 가능성이 있습니다.

### 결정과 trade-off

`frontend/lib/query/retryPolicy.ts`가 공유 QueryClient 정책 하나를 제공합니다(현재 code에서도 `providers.tsx`의 QueryClient 기본 `retry`로 적용되어 있습니다).

- `ApiError` 4xx: retry 없음;
- network, 5xx, 알 수 없는 query 실패: retry 1회;
- retry 지연: TanStack Query 기본값 그대로(retry 1회는 1 s 대기);
- mutation: 변경 없음, 자동 retry 없음.

영구 실패는 요청 4개·약 7초에서 요청 2개·약 1초로 줄고, 측정된 1회 실패 자동 복구는 유지됩니다. 두 번 실패 후 복구하려면 이제 사용자가 기존 명시적 retry를 골라야 합니다. 정보 없는 loading 상태에 사용자를 6초 더 묶어 두지 않기 위해 수용한 trade-off입니다. SSR은 별도의 실패 budget을 가지며 의도적으로 이 결정 밖에 둡니다.

### 한계

retry 시간은 live backend의 latency profile이 아니라, 설치된 TanStack Query package에 즉시 실패를 주입한 결정적 runtime 추적입니다. 당시 checkout에는 browser route-interception 도구가 설치되어 있지 않아, route별 새 visual timing 측정은 주장하지 않습니다. code 변경 후에는 typecheck, production build, live-stack smoke check, 같은 runtime fault 추적을 다시 실행합니다.

## 17. SSR Seed Freshness와 Server Fetch Timeout 감사 — 2026-10-03

### 측정 이유와 당시 흐름

home page는 기본 15-row latest page를 `cache: "no-store"`로 server에서 가져옵니다. 성공하면 결과가 `initialDataUpdatedAt: fetchedAt`과 함께 `usePostList()`의 `initialData`가 되고, 실패하거나 timeout되면 `null`을 반환해 기존 browser query가 첫 page를 가져옵니다. 두 값은 별개의 budget입니다. seed freshness는 불필요한 hydration fetch를 막고, server timeout은 CSR fallback 전에 HTML이 기다리는 시간의 상한을 정합니다.

### 환경

production `next start`, viewport 375×778, DPR 2, CPU 4× slowdown, latency 562.5 ms, download 1.47 Mbps. seed 후보마다 새 browser context 5개, timeout 시나리오는 3개를 썼습니다. delay proxy는 Next server의 upstream(`API_BASE_URL`)에만 영향을 주었고, browser fallback은 정상 backend로 직접 연결했습니다. 영구 Compose database는 read-only로 두었고 `43 / 2076 / 0`(posts/comments/likes)을 유지했습니다.

### Seed freshness

| staleTime | browser 초기 `/api/posts` | 중복 hydration fetch | DOM content loaded 시점 seed 나이 median |
| ---: | ---: | ---: | ---: |
| 0 s | 1 | 5/5 | 1.69 s |
| 5 s | 0 | 0/5 | 1.65 s |
| 15 s | 0 | 0/5 | 1.70 s |
| 30 s | 0 | 0/5 | 1.65 s |

0초 후보는 navigation 후 약 2.83 s에 같은 page를 다시 가져왔습니다. 5/15/30초에서는 SSR HTML에 post 15개가 있었고, browser 첫 page 요청과 hydration error가 없었습니다. 관찰된 느린 load의 나이는 1.8초 미만이었으므로, 5초는 test한 후보 중 분명한 여유를 가진 가장 짧은 window입니다. 측정된 중복 fetch 이득 없이 15–30초의 추가 freshness를 주지 않습니다.

freshness는 polling timer가 아닙니다. 15초 test 후보에서 stale이 되었다는 이유만으로 요청이 생기지 않았고, window 이후의 hidden→visible 전환은 refetch 1회를, window 이전의 같은 전환은 refetch 0회를 일으켰습니다. 당시 React Query source는 staleness를 `dataUpdatedAt + staleTime`으로 계산하고 상태 갱신만 예약합니다. 만료 전 앱 내 목록 → 상세 → 목록 재방문에서는 기존 QueryClient cache가 남아 목록 요청이 없었고, `initialData`가 이를 덮어쓰지 않았습니다.

**Seed 판정: `CHANGE_SEED_FRESH_TO_5S`.** trade-off는 5초 이후 focus나 remount가 더 새로운 server 데이터를 더 일찍 보여 줄 수 있다는 점입니다. 남은 stale window는 의도적으로 hydration 중복 방지 budget뿐입니다.

### Server timeout 장애 모델과 시간

정상 backend의 server-page endpoint는 6.4–8.5 ms(median 7.4 ms)였으므로, 모든 timeout 후보는 정상 경로에 측정된 영향이 없습니다. 아래 통제된 지연은 server 전용 proxy를 거쳤고, browser fallback은 계속 port 8080에서 가져올 수 있었습니다.

| timeout | server 지연 | HTML의 SSR post | browser fallback 요청 | TTFB 유사 median | 실제 post LCP median |
| ---: | ---: | --- | --- | ---: | ---: |
| 1000 ms | 1200 ms | 없음 | 1 | 1.89 s | 3.55 s |
| 1500 ms | 1200 ms | 있음 | 0 | 2.38 s | 2.37 s |
| 1500 ms | 1800 ms | 없음 | 1 | 2.38 s | 4.04 s |
| 2000 ms | 1800 ms | 있음 | 0 | 2.99 s | 2.98 s |
| 1500 ms | blackhole | 없음 | 1 | 2.40 s | 4.05 s |
| 2000 ms | blackhole | 없음 | 1 | 2.88 s | 4.59 s |

모든 timeout/blackhole 응답은 HTTP 200이었고, post 없는 fallback HTML을 반환해 console/hydration error 없이 hydrate한 뒤, 정상 browser 첫 page 요청을 정확히 1번 보내 post 15개를 보였습니다. 1.8초 지연에서 2초 후보는 1.5초 fallback보다 실제 post에 1.06초 빨리 도달했습니다. blackhole 비용은 0.54초였습니다. 판단 지표는 더 이른 loading 상태 FCP가 아니라 처음으로 유용한 *post* 내용입니다.

즉시 실패 경로도 도달할 수 없는 server 전용 upstream으로 확인했습니다. page는 SSR post 없이 HTTP 200을 반환하고, 정상 browser 요청 1번으로 post 15개를 render했으며, page error는 없었습니다.

**Timeout 판정: `CHANGE_SERVER_TIMEOUT_TO_2000`.** 측정한 1.8초 경계까지 유용한 SSR 응답을 유지합니다. 수용한 trade-off는 upstream을 쓸 수 없을 때 HTML이 0.5초 더 기다린다는 점이며, 이는 retry 정책이 아니라 범위가 정해진 CSR fallback 경로입니다.

### 종합 결정과 한계

`SEED_FRESH_MS`를 30 s에서 5 s로, `SERVER_FETCH_TIMEOUT_MS`를 1.5 s에서 2.0 s로 바꿨습니다. *(현재 code: `SEED_FRESH_MS = 5_000`, `SERVER_FETCH_TIMEOUT_MS = 2000`.)* SSR architecture, page size, client retry 정책, API, mutation 동작은 바꾸지 않았습니다. SSR fetch가 실패하면 browser fallback은 별도로 분류된 1회 retry query 정책을 씁니다. 이 감사의 server 전용 proxy는 browser backend를 정상으로 유지해 timeout 경계만 측정했습니다.

비교 범위는 throttling한 mobile profile 하나, local 정상 backend, 고정 인공 지연이며 production latency 분포가 아닙니다. 임시 worktree, proxy, local server, browser script는 repository 밖에 두었고 검증 후 제거했습니다.
