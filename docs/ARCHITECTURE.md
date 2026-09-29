# Architecture Decisions

This document records architecture choices for the APR Product Engineer assignment.

`ASSIGNMENT.md` remains the canonical source of truth. Architecture exists to explain **how and why** the requirements are implemented; it must never weaken or reinterpret them.

## Evidence Policy

Every high-impact decision must separate facts from judgment.

Evidence types:

- **Assignment evidence** — explicit requirement from `ASSIGNMENT.md`.
- **Starter evidence** — behavior/configuration verified in the provided starter repository.
- **Official documentation** — first-party framework/database/browser documentation.
- **Engineering judgment** — our conclusion from the evidence, with alternatives and trade-offs stated explicitly.

Decision status:

- `DECIDED` — enough evidence exists to implement this choice.
- `PROVISIONAL` — current preferred choice, but repository/design inspection can still change it.
- `OPEN` — do not implement the affected area until the decision is resolved.

Do not cite blog posts or AI-generated claims when an official source is available.

---

# P0 Repository Audit — 2026-09-29

This section supersedes the earlier “not yet repository-verified” boundary below. It records only facts observed in this repository and the resulting status of each proposed decision; it does not implement any product behavior.

## Repository Facts

### Backend

- `backend/build.gradle` uses Gradle Groovy with Java toolchain 17, Spring Boot `3.5.0`, and Spring dependency management `1.1.7`.
- Declared runtime/application dependencies are `spring-boot-starter-web`, `spring-boot-starter-jdbc`, and runtime PostgreSQL JDBC. Tests use `spring-boot-starter-test` plus an explicit Mockito agent. JPA, Flyway, Liquibase, and a validation starter are not declared.
- The base package is `com.apr.community`. The only current Java sources are `CommunityApplication`, `HealthController`, and `CorsConfig`; there are no domain, service, repository, schema, or migration sources.
- `application.yml` accepts datasource URL/user/password overrides, defaults local PostgreSQL to port `15432`, exposes server port `8080`, and reads `APP_SEED_PATH` (default `../seed/seed.json`).
- `HealthController` handles `GET /health`, obtains a datasource connection, and returns HTTP 200 with `status: ok` and `db: up`; connection failures produce HTTP 503. `CorsConfig` permits configured localhost/127.0.0.1 origin patterns, credentials, and the required HTTP methods.

### Frontend

- `frontend/package.json` uses Next.js `15.4.6`, React/React DOM `19.1.0`, and TypeScript `^5`; npm is the package manager because `package-lock.json` is present.
- The app uses the App Router (`app/` with `layout.tsx` and `page.tsx`). Scripts are `dev`, `build`, and `start`; no lint or standalone typecheck script exists.
- No styling framework, server-state library, or data-fetching library is declared. `globals.css` defines the Pretendard Variable font face from `public/fonts/PretendardVariable.woff2` and base styles.
- `frontend/app/dev-user/page.tsx` is present and sets/clears the `x-user-id` cookie client-side; selecting a user redirects to `/`. It remains immutable.
- Public assets are `avatar-1.png`, `avatar-2.png`, `avatar-3.png`, Pretendard Variable, and its OFL license.

### Runtime and Seed

- `docker-compose.yml` defines `db`, `backend`, and `frontend`. DB is `postgres:16-alpine`, uses named volume `dbdata`, and maps `${DB_PORT:-15432}`. Backend waits for DB health, mounts `./seed:/seed:ro`, uses `APP_SEED_PATH=/seed/seed.json`, maps `${BACKEND_PORT:-8080}`, and has a `/health` healthcheck. Frontend waits for backend health, maps `${FRONTEND_PORT:-3000}`, receives build-time `NEXT_PUBLIC_API_BASE_URL`, and uses container-only `API_BASE_URL=http://backend:8080/api`.
- `seed/seed.json` is valid JSON with top-level keys `posts`, `comments`, `deletedPostIds`, and `deletedCommentIds`. It contains 42 unique post IDs (1–42), 2,076 unique comment IDs (1–2,076), 3 deleted-post IDs, and 81 deleted-comment IDs. Every deleted ID refers to a corresponding seed record.
- Post fields are `id`, `title`, `content`, `likeCount`, `author`, and `createdAt`; comment fields are `id`, `postId`, `content`, `author`, and `createdAt`. All observed fields are non-null. IDs and `likeCount` are numbers; remaining fields are strings. `commentCount` is absent. Timestamps are ISO UTC strings. Every comment references an existing post; posts 10 and 23 have no comments.
- `design/` exists but contains no files. The expected repository-local Figma asset and any screenshot/reference image were therefore `MISSING` during P0. A later official Figma URL became available and is recorded in the Figma Design Audit Update below.

## Starter Contracts Confirmed

- The assignment’s immutable `/dev-user`, `GET /health` HTTP 200, CORS/credentialed-request, read-only seed mount, and port-override contracts all have corresponding starter code/configuration.
- The health endpoint validates DB reachability, so the backend healthcheck is intentionally dependent on a usable database connection.
- No contradiction was found between the inspected starter configuration and these contracts. Runtime behavior itself has not been executed during P0.

## ADR Status Reconciliation

| ADR | Resulting status | Repository evidence and reason |
| --- | --- | --- |
| ADR-001 Spring JDBC | `DECIDED` | `spring-boot-starter-jdbc` and PostgreSQL JDBC are already declared; this path needs no additional persistence dependency. |
| ADR-002 Flyway | `OPEN` | Flyway/PostgreSQL Flyway modules and migration files are absent. Spring Boot documents that Flyway requires the appropriate module on the classpath, so adoption requires an explicit architecture decision and dependency change. |
| ADR-003 IDs | `PROVISIONAL` | The seed’s explicit unique IDs are verified, but no schema or sequence exists yet. |
| ADR-004 data model / soft delete | `PROVISIONAL` | Soft-delete behavior is assigned, but there is no existing schema or persistence code to validate the proposed shape. |
| ADR-005 likes | `DECIDED` | Seed supplies aggregate `likeCount` but no historical requester-like records; keeping a base count plus application-created requester rows preserves known data. |
| ADR-006 cursor pagination | `PROVISIONAL` | Pagination is mandatory, but the assignment leaves its wire format to the implementation and no current API exists. |
| ADR-007 TanStack Query | `DECIDED` | P0: no server-state package was installed. P3 added `@tanstack/react-query` 5.104.0 (lockfile-resolved) and `app/providers.tsx`. |
| ADR-008 Next.js boundary | `DECIDED` | The verified Next.js App Router starter and immutable client-side cookie selector support server route shells with focused client interaction boundaries. |
| ADR-009 CSS Modules | `DECIDED` | No competing styling system is installed. P3 fixed `globals.css` as reset/font/token scope; component styles start as `.module.css` in P4. |
| ADR-010 infinite-scroll trigger | `PROVISIONAL` | The browser primitive needs no dependency, but its integration depends on the unresolved pagination/server-state approach. |
| ADR-011 relative time | `DECIDED` | The native formatter needs no dependency and seed timestamps are UTC ISO strings. |
| ADR-012 validation / ownership / errors | `DECIDED` | P2B implements explicit dependency-free validation, service-layer ownership/transactions, and consistent JSON errors. |
| ADR-013 seed initialization | `OPEN` | Seed path/mount and persistent DB volume are verified, but the chosen migration mechanism and startup hook do not exist. |

## External Evidence Checked

- [Spring JDBC 6.2 documentation](https://docs.spring.io/spring-framework/reference/6.2/data-access/jdbc.html) confirms Spring’s JDBC abstraction owns connection/resource lifecycle while application code supplies SQL.
- [Spring Boot 3.5 database-initialization documentation](https://docs.spring.io/spring-boot/3.5/how-to/data-initialization.html) recommends one schema-initialization mechanism and states that PostgreSQL Flyway use requires the database-specific Flyway module on the classpath.

Existing external citations not listed above remain `NOT_EXTERNALLY_VERIFIED` in this P0 pass; they are not treated as repository facts.

---

# Architecture Review Finalization — 2026-09-29

**Review status:** `COMPLETE`
**Authoritative record:** This section supersedes the prior proposed ADR text and status labels when they conflict. It finalizes the implementation direction only; it does not add dependencies, schema, or product code.

## Shared Constraints and Invariants

- **Assignment evidence:** Java 17/Spring Boot 3.x, Next.js 15 App Router, PostgreSQL, full-stack `docker compose up`, soft deletion, requester-specific likes, bounded pagination, seed-ID preservation, and the immutable `/dev-user`/`GET /health` contracts are mandatory.
- **Repository evidence:** JDBC and PostgreSQL are already declared; JPA, migration tooling, state/data libraries, and UI libraries are absent. The seed has 42 posts, 2,076 comments, explicit IDs, aggregate `likeCount`, and no historical user-like data or `commentCount`.
- **Scope invariant:** no user table, invented historical like users, deletion timestamps fabricated from seed data, speculative distributed architecture, or unnecessary abstraction layer.

## Final Decisions

| Decision | Status | Decision and fit | Alternatives considered / accepted trade-off | Implementation verification |
| --- | --- | --- | --- | --- |
| Persistence | `DECIDED` | Use Spring JDBC `JdbcClient` with explicit SQL repositories. It is compatible with the declared JDBC starter and keeps soft-delete, ownership, aggregate count, like, and pagination queries visible. | Add JPA/Spring Data JPA; rejected because it adds an undeclared ORM dependency and does not remove the assignment’s query-specific work. | Compile and integration-test CRUD, soft-delete filters, ownership, and row mapping. |
| Schema migration | `DECIDED` | Add Flyway for versioned schema migration only; load the provided JSON in application code rather than duplicate it as migration data. The persistent DB volume needs schema-history semantics across restarts. | `schema.sql`; simpler and dependency-free, but less explicit for schema evolution. Flyway costs an added dependency and PostgreSQL module. | Clean-volume and restart tests show migrations apply once and schema history is stable. |
| Seed lifecycle | `DECIDED` | Run idempotent JSON import through an `ApplicationRunner` after Flyway. Insert explicit IDs, preserve later user mutations, and record completed import only after the transaction succeeds. | Reset/update rows at every startup; rejected because it overwrites user-created or edited state. | Startup/restart test: no duplicate records, deleted IDs stay deleted, and sequences allow a new row. |
| IDs | `DECIDED` | Use `BIGINT GENERATED BY DEFAULT AS IDENTITY`; import explicit seed IDs, then align each sequence above its seeded maximum. | Application-side ID allocation; rejected as extra state without assignment value. | Seed IDs remain exact; first created post/comment receives a non-conflicting ID. |
| Soft delete | `DECIDED` | Use `is_deleted BOOLEAN NOT NULL DEFAULT FALSE` for posts and comments. The seed supplies deletion membership, not a deletion instant, so a boolean represents known facts without inventing timestamps. | `deleted_at`; rejected because it fabricates a seed timestamp. | Deleted rows remain stored but are excluded from list/detail/count/mutation reads. |
| Likes | `DECIDED` | Store `posts.base_like_count` plus `post_likes(post_id, user_id)` with a unique key. Calculate `likeCount` from both and `isLiked` from the requester row. | Mutable aggregate counter only, or invented historical like users; rejected because both lose/alter seed facts. | Toggle twice as one requester and fetch as another; count and `isLiked` remain correct. |
| Pagination | `DECIDED` | Use cursor/keyset pagination ordered by `(created_at DESC, id DESC)` and keep required `items`/`total`, adding `nextCursor`. This fits newest-first infinite scrolling under concurrent creates/deletes. | Offset pagination; simpler but can drift while scrolling. It also still needs a separate total count. | Bounded page requests are deterministic and adjacent pages have no duplicates/skips. |
| Frontend server state | `DECIDED` | Add TanStack Query v5 for API-backed lists, detail, mutations, invalidation, and infinite pages; keep form/sheet/toast state local. | Hand-written `fetch` + effects/state, Redux, Zustand; rejected because they add bespoke cache logic or a broader client-state store. TanStack Query adds one dependency/provider boundary. | React 19 build plus query invalidation and `useInfiniteQuery` behavior tests. |
| Client/server boundary | `DECIDED` | Keep App Router route shells server by default; isolate interactive, cookie-dependent community features behind client boundaries and send browser API requests with `credentials: "include"`. | Mark every component client-side; rejected because it broadens the client bundle and loses the App Router default boundary. | `/dev-user` cookie drives requester-sensitive browser calls without changing the immutable page. |
| Styling | `DECIDED` | Use existing `globals.css` for Pretendard/reset/tokens and CSS Modules for component/page styles. No UI framework is needed for this mobile take-home. | Tailwind or a UI kit; rejected because neither is installed and both add configuration/dependency surface. | Next build and 375px visual review once the design asset is supplied. |
| Infinite-scroll trigger | `DECIDED` | Use an `IntersectionObserver` sentinel guarded by `hasNextPage` and `isFetchingNextPage` for both post continuation and post-more comment continuation. | Window-scroll coordinate calculations; rejected as less direct and more fragile. | One intersection produces at most one next-page request while a request is pending. |
| Relative time | `DECIDED` | Use native `Intl.RelativeTimeFormat("ko", { numeric: "always" })` behind a small threshold utility. Seed UTC timestamps provide suitable absolute instants. | Date library; rejected because formatting is the only requirement. | Fixed-clock unit tests cover seconds/minutes/hours/days and Korean numeric output. |
| Validation and ownership | `DECIDED` | Frontend validation is UX only; backend validation and ownership checks are authoritative, and multi-step mutations use a service transaction boundary. Exact Bean Validation dependency/error payload types remain an implementation detail. | UI-only authorization; rejected because direct API calls must be protected. | Direct API tests reject invalid and other-user mutations; accepted mutations remain atomic. |

## External Evidence

- **Spring Framework 6.2 — [JdbcClient](https://docs.spring.io/spring-framework/reference/data-access/jdbc/core.html):** `JdbcClient` has been available since Spring Framework 6.1 as a fluent unified JDBC query/update API. This supports the declared Spring Boot 3.5 JDBC path.
- **Spring Boot 3.5 — [Database Initialization](https://docs.spring.io/spring-boot/3.5/how-to/data-initialization.html):** recommends one schema-initialization mechanism; Flyway runs on startup and PostgreSQL requires its database-specific Flyway module.
- **Spring Boot 3.5 — [SpringApplication](https://docs.spring.io/spring-boot/3.5/reference/features/spring-application.html):** `ApplicationRunner` runs just before `SpringApplication.run` completes, which supports one-time startup bootstrap work.
- **PostgreSQL 16 — [Identity Columns](https://www.postgresql.org/docs/16/ddl-identity-columns.html):** `GENERATED BY DEFAULT` identity columns permit explicitly supplied values while generating values when omitted.
- **PostgreSQL — [LIMIT and OFFSET](https://www.postgresql.org/docs/current/queries-limit.html):** page subsets need deterministic ordering and skipped offset rows are still computed; this supports the keyset trade-off.
- **TanStack Query v5 — [Infinite Queries](https://tanstack.com/query/latest/docs/framework/react/guides/migrating-to-v5):** infinite queries model paginated page data and v5 provides bounded retained pages through `maxPages`.
- **Next.js — [Server and Client Components](https://nextjs.org/docs/app/getting-started/server-and-client-components):** pages/layouts are Server Components by default; Client Components are appropriate for browser APIs, events, effects, and state.
- **MDN — [Intersection Observer API](https://developer.mozilla.org/en-US/docs/Web/API/Intersection_Observer_API):** provides asynchronous intersection observation and explicitly lists infinite scroll as a use case.
- **MDN — [Intl.RelativeTimeFormat](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/RelativeTimeFormat):** documents the native locale-sensitive relative-time formatter.

## Follow-up Risks

- The current `/health` endpoint confirms DB reachability, not seed completion. P1 must verify that Compose startup does not expose an unseeded application to the frontend.
- The official Figma source is now `AVAILABLE` and audited in `docs/UI_SPEC.md`; visual implementation and fidelity sign-off remain future work, not architecture blockers.
- Flyway/TanStack Query/any validation dependency are approved architecture choices, not changes made by this review. Their exact resolved versions must be taken from the applicable ecosystem BOM/package metadata during implementation.

---

# Figma Design Audit Update — 2026-09-29

- Official source: Figma file `Ee1gp5NC6G1sQIvssuu4Sr`, Page 1 (`0:1`).
- Twelve 375×778 top-level frames were audited for list, toast, own/other detail, liked/unliked, 0/1/many comments, own-post actions, post/comment delete confirmation, and disabled/enabled create-form states.
- `ASSIGNMENT.md` remains authoritative. Design-only comment editing, replies, reports, per-comment likes, badges, and popular-content sections do not expand product or API scope.
- The audit introduces no new P2 endpoint or response field. Existing ownership, `isLiked`/`likeCount`, `commentCount`, comments `total`, pagination, and delete contracts cover the assignment-defined behavior shown by the design.
- Loading/error/pending visuals and a distinct edit mockup are not present; those remain product decisions while edit-form reuse remains assignment-defined.
- Detailed frame mapping, exact observed values, and evidence taxonomy are recorded in `docs/UI_SPEC.md`.

---

# P2A Read API Implementation Update — 2026-09-29

- Requester resolution is implemented by one `RequesterResolver` with deterministic precedence `x-user-id` header > `x-user-id` cookie > `apr_tester`. Blank or whitespace-only values are absent, and no user table is introduced.
- Both read collections accept optional `limit` and `cursor`. The default limit is 20, the accepted range is 1–50, and invalid limits or cursors return HTTP 400 JSON errors.
- The canonical order is `(created_at DESC, id DESC)`. The next-page predicate compares the same timestamp and ID pair, and repositories fetch `limit + 1` rows to decide whether `nextCursor` exists without using `OFFSET`.
- The opaque cursor payload is the exact `Instant` text plus the numeric ID, separated by `|`, encoded as URL-safe Base64 without padding. Decoding rejects non-canonical Base64, invalid timestamps, non-positive/invalid IDs, and malformed payload structure.
- `GET /api/posts`, `GET /api/posts/{postId}`, and `GET /api/posts/{postId}/comments` are the only P2A endpoints. Post page rows derive `likeCount`, requester-specific `isLiked`, and readable `commentCount` in one bounded SQL query; a separate count query supplies full `total`.
- Missing and soft-deleted posts both return the same 404 JSON contract. Comment reads first require a readable parent post. At the P2A cutoff, mutation validation and ownership enforcement were intentionally deferred to P2B.

---

# P2B Mutation API Implementation Update — 2026-09-29

- The six required mutation routes use focused request/response records and keep the P2A read contract unchanged. Post create/update share one explicit validator; title length is measured with `String.codePointCount`, blank checks do not trim the stored value, and comment content has no invented maximum length.
- Rejecting blank comment content is a `PRODUCT_DECISION`: the assignment requires comment creation but gives no explicit blank rule. A nonblank requirement prevents empty persisted comments without introducing a length cap.
- `PostMutationService` and `CommentMutationService` are the authoritative ownership and transaction boundaries. Repositories expose SQL primitives and do not encode HTTP status semantics.
- Every mutation method is service-level `@Transactional`. Readable target rows are locked before ownership checks or multi-step changes; like toggle serializes on the readable post row and relies on the existing `(post_id, user_id)` primary key.
- Post and comment deletion only set `is_deleted = TRUE`. Comment deletion checks comment authorship independently from post ownership, and a deleted parent makes comment reads and mutations unavailable without cascading child deletion.
- The JSON error contract is `400` for validation or unreadable JSON, `403` for a readable resource owned by another requester, and `404` for missing or soft-deleted targets. Success statuses are `201` create, `200` update/toggle, and `204` delete.
- Clean-volume PostgreSQL acceptance verified post/comment identity generation, requester ownership, Unicode title validation, requester-isolated likes, derived counts, retained soft-deleted rows, P2A read regression, and restoration to the seed baseline.

---

# P3 Frontend Foundation Update — 2026-09-29

- **Server state (ADR-007):** `@tanstack/react-query` is the only added production dependency; `package-lock.json` resolves it to 5.104.0. `frontend/app/providers.tsx` is a `"use client"` `AppProviders` component that creates one `QueryClient` per browser session via `useState` and only renders `QueryClientProvider`. No Devtools, Redux, Zustand, SWR, or axios; no separate client global state store.
- **Client/server boundary (ADR-008):** `app/layout.tsx` remains a Server Component and wraps `children` in `AppProviders`. Pages stay Server Components by default; wrapping in a client provider does not make them client components. Requester-sensitive product data will be fetched from focused client feature boundaries in P4–P6 so the browser sends the `/dev-user` cookie. No server-side cookie-forwarding API layer is introduced.
- **Shared API client:** `frontend/lib/api/client.ts` `request()` is the single fetch entry point. It resolves `NEXT_PUBLIC_API_BASE_URL` (fallback `http://localhost:8080/api`, matching the Compose build arg), joins paths, encodes query strings with `URLSearchParams`, always sends `credentials: "include"` and `cache: "no-store"`, sets `Content-Type: application/json` only when a body exists, returns `undefined` for 204 without parsing, and throws `ApiError { status, code, message }` using the backend `{ code, message }` body with an `HTTP_ERROR` / `NETWORK_ERROR` / `INVALID_RESPONSE` fallback.
- **Transport types:** `frontend/lib/api/types.ts` mirrors the backend `PostResponse`, `CommentResponse`, `PageResponse`, `LikeToggleResponse`, `PostMutationRequest`, `CommentCreateRequest`, and `ApiErrorResponse` field names exactly. Timestamps remain ISO strings at the API boundary.
- **Endpoint functions:** `frontend/lib/api/community.ts` defines all nine assignment endpoints. Feature hooks (`useQuery`/`useMutation`) and invalidation policy are deferred to P4–P6 where the cache requirements are concrete.
- **Query keys:** `frontend/lib/query/keys.ts` `postKeys` uses only stable serializable values (string segments, numeric post IDs, and a `{ limit: params.limit ?? null }` object). `cursor` is excluded because infinite-query pagination carries it as `pageParam`. No mutable or non-deterministic values are placed in keys.
- **Route contract (`PRODUCT_DECISION`):** `/` list, `/posts/{postId}` detail, `/posts/new` create, `/posts/{postId}/edit` edit, `/dev-user` starter page, exposed through `frontend/lib/routes.ts`. Route pages themselves are implemented in P4–P6.
- **Relative time (ADR-011):** thresholds are seconds < 60s, minutes < 60m, hours < 24h, days < 30d, months < 365d, then years; future instants format as `… 후`; unparseable input returns `null`. Rendering against the current clock in client components must account for server/client hydration differences when P4 uses it.
- **Styling (ADR-009):** `globals.css` owns reset/font/base plus a small set of `DESIGN_DEFINED` tokens from `docs/UI_SPEC.md` (surface, primary/secondary/support text, light border, disabled surface, liked accent, scrim, toast surface, 16px page inset). No token was invented beyond the audited values.

---

# 1. Current Evidence Boundary

## Verified from assignment / provided README

- Backend: Java 17 + Spring Boot 3.x.
- Frontend: TypeScript + Next.js 15 App Router.
- Database: PostgreSQL.
- Full stack must boot with `docker compose up`.
- The provided README describes the starter backend as Spring Boot 3.5 with `spring-boot-starter-jdbc` already present.
- The provided README describes PostgreSQL 16 in the starter compose configuration.
- The provided README states that `seed/` is mounted read-only at `/seed` through `APP_SEED_PATH`.
- The provided README states that browser API requests must use `credentials: "include"` when relying on the `x-user-id` cookie.
- The assignment requires paginated loading rather than fetching complete collections.
- The assignment requires soft deletion for posts and comments.
- The seed preserves explicit numeric IDs and does not contain per-user like history.

## Not yet repository-verified

The full starter repository has not yet been audited in this documentation pass. Therefore the following must remain subject to P0 verification:

- Exact Gradle dependency set and plugins.
- Exact `docker-compose.yml` service/healthcheck definitions.
- Exact backend package names.
- Exact frontend package dependencies and lint/build scripts.
- Exact `seed/seed.json` field types and maximum IDs.
- Whether a styling/state library is already installed.
- Whether the official Figma file exists in the actual starter repository.

Do not convert these unknowns into assumptions.

---

# ADR-001 — Persistence Access: Spring JDBC `JdbcClient`, not JPA

**Status:** `DECIDED`

## Decision

Use Spring JDBC with `JdbcClient` and explicit SQL repositories. Do not add JPA/Hibernate unless P0 reveals a hard requirement that materially changes the data model.

## Evidence

- Starter evidence: the provided README says `spring-boot-starter-jdbc` is already included and persistence technology is left to the candidate.
- Spring Framework 6.2 documents Spring JDBC as handling connection/resource lifecycle, exception translation, and transaction participation while application code owns SQL.
  - https://docs.spring.io/spring-framework/reference/6.2/data-access/jdbc.html
- Spring Framework 6.2 provides `JdbcClient` as a fluent facade for common JDBC query/update operations.
  - https://docs.spring.io/spring-framework/reference/6.2/data-access/jdbc/core.html

## Why this fits this assignment

The domain is intentionally small: posts, comments, requester-specific likes, and seed metadata. The difficult parts are not object-graph persistence; they are explicit query semantics:

- newest-first pagination,
- requester-specific `isLiked`,
- derived `commentCount`,
- soft-delete filtering,
- ownership checks,
- exact API response shapes.

Explicit SQL keeps those rules visible in code and avoids adding entity lifecycle, lazy-loading, dirty-checking, and ORM mapping behavior that the assignment does not need.

## Alternatives considered

### Spring Data JPA / Hibernate

Advantages:
- less CRUD boilerplate,
- familiar repository/entity model,
- automatic change tracking.

Why not selected:
- the assignment has few tables and no rich aggregate graph,
- important reads already require custom query design,
- adds a larger abstraction surface that must be explained and tested.

## Trade-off accepted

We write more SQL and mapping code ourselves. In exchange, database behavior is explicit and directly reviewable.

---

# ADR-002 — Schema Management: Flyway for schema, application code for assignment seed

**Status:** `OPEN`

## Decision

Use Flyway versioned SQL migrations for schema evolution. Do not mix Flyway with `schema.sql`/`data.sql` for schema creation.

Load `seed/seed.json` through application startup logic after the schema exists; do not convert the assignment-provided JSON into a hidden SQL copy.

## Evidence

- Spring Boot 3.5 recommends using a single schema initialization mechanism and documents Flyway integration.
  - https://docs.spring.io/spring-boot/3.5/how-to/data-initialization.html
- The same Spring Boot page states that PostgreSQL requires the database-specific Flyway module and that migrations conventionally live under `classpath:db/migration`.
  - https://docs.spring.io/spring-boot/3.5/how-to/data-initialization.html
- Flyway documents versioned migrations as ordered, applied once, and tracked in schema history with checksums.
  - https://documentation.red-gate.com/fd/versioned-migrations-273973333.html

## Why this fits this assignment

The evaluator must be able to clone the repository and get the same schema with `docker compose up`. A versioned SQL migration makes the schema explicit, reviewable, and reproducible.

The assignment explicitly makes `seed/seed.json` the source of initial data, so keeping seed parsing in application code preserves that source of truth instead of maintaining a second copy of the same data in SQL.

## Trade-off accepted

Flyway adds a dependency and one migration convention. That cost is acceptable because schema creation becomes deterministic and inspectable.

---

# ADR-003 — IDs: PostgreSQL identity columns that also accept seed IDs

**Status:** `PROVISIONAL`

## Decision

Prefer `BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY` for post/comment IDs.

- Seed import inserts the assignment-provided IDs explicitly.
- Newly created rows omit the ID and use the identity sequence.
- After first seed import, align the identity sequence above the maximum imported ID before accepting generated IDs.

Final SQL waits for direct inspection of `seed/seed.json` during P0.

## Evidence

- Assignment evidence: seed IDs must be preserved exactly; IDs for new posts/comments may be assigned freely.
- PostgreSQL documents that `GENERATED BY DEFAULT AS IDENTITY` accepts explicit user-provided values while still generating values when an ID is omitted.
  - https://www.postgresql.org/docs/current/ddl-identity-columns.html
- PostgreSQL documents `pg_get_serial_sequence` for retrieving the sequence attached to an identity column.
  - https://www.postgresql.org/docs/current/functions-info.html
- PostgreSQL documents `setval` for aligning a sequence value.
  - https://www.postgresql.org/docs/current/functions-sequence.html

## Why this fits this assignment

This satisfies both competing requirements without maintaining a custom in-application ID allocator:

1. imported rows retain their original numeric IDs;
2. product-created rows receive generated numeric IDs.

## Risk to verify

Explicit identity values do not by themselves guarantee that the backing sequence advances above those values. P1 must verify sequence alignment and create-after-seed behavior.

---

# ADR-004 — Data Model and Soft Delete

**Status:** `PROVISIONAL`

## Decision

Use three product tables plus one seed metadata table:

```text
posts
comments
post_likes
seed_history
```

Proposed shape:

```text
posts
- id BIGINT PK / identity
- title VARCHAR(20) NOT NULL
- content TEXT NOT NULL
- author VARCHAR(...) NOT NULL
- base_like_count INTEGER NOT NULL
- created_at TIMESTAMPTZ NOT NULL
- updated_at TIMESTAMPTZ NULL
- deleted_at TIMESTAMPTZ NULL

comments
- id BIGINT PK / identity
- post_id BIGINT NOT NULL FK -> posts.id
- content TEXT NOT NULL
- author VARCHAR(...) NOT NULL
- created_at TIMESTAMPTZ NOT NULL
- deleted_at TIMESTAMPTZ NULL

post_likes
- post_id BIGINT NOT NULL FK -> posts.id
- user_id VARCHAR(...) NOT NULL
- created_at TIMESTAMPTZ NOT NULL
- PRIMARY KEY (post_id, user_id)

seed_history
- seed_key VARCHAR(...) PRIMARY KEY
- imported_at TIMESTAMPTZ NOT NULL
```

Final string lengths and exact constraints wait for P0 inspection of the actual seed.

## Evidence

- Assignment evidence: a separate user table is not required; ownership is `author == requester`.
- Assignment evidence: posts/comments must remain physically present after deletion but disappear from normal reads.
- PostgreSQL documents foreign keys as database-level referential integrity.
  - https://www.postgresql.org/docs/current/tutorial-fk.html
- PostgreSQL `timestamp with time zone` normalizes timestamp values internally and is suitable for absolute instants such as the assignment's ISO timestamps.
  - https://www.postgresql.org/docs/current/datatype-datetime.html

## Soft-delete rule

Normal product deletion is:

```text
deleted_at: NULL -> current timestamp
```

All normal list/detail/count queries must treat `deleted_at IS NOT NULL` as absent.

This includes:

- post list,
- post detail,
- comment list,
- `commentCount`,
- mutation targets.

## Why `deleted_at` rather than a boolean

A boolean would satisfy the minimum requirement. `deleted_at` carries the same state while also preserving when the transition happened, with no extra table or event model.

## Index policy

Start with only query-shaped indexes justified by actual reads, for example:

```text
posts(created_at DESC, id DESC)
comments(post_id, created_at DESC, id DESC)
```

Do not add speculative indexing merely because PostgreSQL supports it. The seed contains only 42 posts and 2,076 comments. If later query plans justify partial indexes for `deleted_at IS NULL`, add them with evidence.

PostgreSQL documentation:
- multicolumn indexes: https://www.postgresql.org/docs/current/indexes-multicolumn.html
- partial indexes: https://www.postgresql.org/docs/current/indexes-partial.html

---

# ADR-005 — Likes: Historical Base Count + Requester Like Rows

**Status:** `DECIDED`

## Decision

Represent seed likes and requester likes separately:

```text
posts.base_like_count
post_likes(post_id, user_id)
```

API response:

```text
likeCount = base_like_count + count(post_likes for the post)
isLiked   = whether (post_id, current_user) exists in post_likes
```

## Evidence

- Assignment evidence: seed data contains `likeCount` but no like history.
- Assignment evidence: therefore `isLiked` starts false for every requester.
- Assignment evidence: like is a requester-specific toggle and the count must change with the toggle.

## Why this model

The seed count represents historical likes whose user identities do not exist in the input. Inventing users for those likes would fabricate data.

Keeping the historical count as a base value and storing only likes created through this application preserves every fact that actually exists in the seed.

It also avoids maintaining two mutable sources of truth (`like_count` counter + like rows) that could drift apart.

## Trade-off accepted

`likeCount` requires an aggregate over requester-generated likes. At this assignment's data size, correctness and clarity are more valuable than denormalizing a mutable counter.

---

# ADR-006 — Pagination: Cursor/keyset pagination with deterministic ordering

**Status:** `DECIDED`

## Decision

Use cursor-based pagination for both posts and comments.

Canonical order:

```text
ORDER BY created_at DESC, id DESC
```

The cursor represents the final `(created_at, id)` pair from the current page. The API keeps the required `items` and `total` fields and adds `nextCursor`.

Implemented wire contract:

```text
GET /api/posts?limit=20&cursor=<opaque>
GET /api/posts/{postId}/comments?limit=20&cursor=<opaque>

default limit: 20
valid limit: 1..50
omitted cursor: first page
invalid limit or cursor: HTTP 400
```

The opaque cursor is URL-safe Base64 without padding over `<exact Instant text>|<id>`. Its timestamp is not reduced to epoch milliseconds. The decoder accepts only the canonical encoding and valid positive IDs. Repositories use `LIMIT + 1`, return at most the requested limit, and never use `OFFSET`.

Example conceptual response:

```json
{
  "items": [],
  "total": 39,
  "nextCursor": "opaque-value"
}
```

## Evidence

- Assignment evidence: posts and comments are newest-first and must be loaded in pages rather than all at once.
- PostgreSQL states that `LIMIT` should be paired with an `ORDER BY` that constrains rows into a unique order, otherwise page subsets can be unpredictable.
- PostgreSQL also states that rows skipped by a large `OFFSET` still have to be computed.
  - https://www.postgresql.org/docs/current/queries-limit.html

## Why this fits this assignment

Infinite scrolling and concurrent creation are a better match for a position-based cursor than a page number. Adding `id` as a tie-breaker makes ordering deterministic even when multiple rows have the same timestamp.

## Why not offset pagination

Offset pagination would be acceptable for the tiny seed from a raw performance perspective and is simpler to explain. It was not selected because insertion of a newer row can shift later offsets while the user is scrolling, increasing the chance of duplicates/skips.

## Trade-off accepted

The backend must encode/decode a cursor and the client cannot jump directly to arbitrary page numbers. The product has no page-number UI, so that cost is irrelevant here.

---

# ADR-007 — Frontend Server State: TanStack Query; local UI state stays local

**Status:** `DECIDED` — provider and query-key foundation implemented in P3 (see P3 Frontend Foundation Update).

## Decision

Use TanStack Query for remote/server state:

- post list pages,
- post detail,
- comment pages,
- like mutation,
- create/update/delete invalidation.

Use component-local React state for ephemeral UI state:

- form input,
- bottom-sheet open/close,
- local confirmation state,
- temporary presentation state.

Do not add Redux/Zustand unless a concrete cross-feature client-state requirement appears later.

## Evidence

- Next.js documentation explicitly lists React Query as a valid community-library approach for fetching in Client Components.
  - https://nextjs.org/docs/15/app/getting-started/fetching-data
- TanStack Query provides `useInfiniteQuery` with page parameters and next-page resolution, which maps directly to this assignment's infinite-scroll requirement.
  - https://tanstack.com/query/latest/docs/framework/react/reference/functions/useInfiniteQuery

## Why this fits this assignment

Most mutable shared state is not truly client state; it is backend data that needs cache invalidation after likes, comments, edits, and deletes. TanStack Query solves that problem directly and avoids building a bespoke cache/infinite-pagination layer.

## Trade-off accepted

This adds one frontend dependency and requires deliberate query-key/invalidation design. The reduction in custom asynchronous state machinery is worth that cost.

---

# ADR-008 — Next.js Boundary: server route shells, client interactive features

**Status:** `DECIDED`

## Decision

Keep App Router pages/layouts as Server Components by default, but render the interactive community feature through focused Client Components.

Browser-to-backend requests that depend on `x-user-id` use:

```ts
credentials: "include"
```

Do not duplicate the same community data-fetching flow once on the server and again in TanStack Query without a demonstrated benefit.

## Evidence

- The provided README explicitly requires `credentials: "include"` for browser requests that use the requester cookie.
- Next.js documents that Client Components are appropriate for state, event handlers, lifecycle logic, browser APIs, and custom hooks, while layouts/pages are Server Components by default.
  - https://nextjs.org/docs/app/getting-started/server-and-client-components

## Why this fits this assignment

The core screens are interaction-heavy: infinite scroll, like toggling, comment creation/deletion, bottom sheets, forms, toast feedback, and requester switching. A focused client feature boundary keeps that interaction model straightforward while still avoiding making the whole application a Client Component.

---

# ADR-009 — Styling: CSS Modules + global design tokens

**Status:** `DECIDED` — P3 token foundation is in `frontend/app/globals.css`; component CSS Modules begin in P4.

## Decision

Default to CSS Modules for component styles and `globals.css` for:

- Pretendard font setup already supplied by the starter,
- reset/base styles,
- CSS custom properties derived from the official Figma design.

Do not add a UI component framework.

Re-evaluate only if P0 shows that the starter already includes a different intentional styling system.

## Evidence

- The provided README says Pretendard is already present under `frontend/public/fonts/` and applied in `globals.css`.
- Next.js supports CSS Modules out of the box and locally scopes generated class names.
  - https://nextjs.org/docs/15/app/guides/migrating/from-create-react-app#step-5-styles
- Next.js also documents CSS Modules, Global CSS, Tailwind, Sass, and other supported styling approaches.
  - https://nextjs.org/docs/app/getting-started/css

## Why CSS Modules over Tailwind for this assignment

The evaluation includes matching a supplied mobile design. CSS Modules make exact spacing, typography, state styles, and component-level selectors directly visible without introducing a utility framework/configuration solely for a small take-home.

This is not a claim that CSS Modules are generally superior to Tailwind. It is a scope-specific choice.

## Trade-off accepted

We write more authored CSS and must manage shared tokens ourselves. In return, no additional styling dependency is required and Figma-derived values remain easy to audit.

---

# ADR-010 — Infinite Scroll Trigger: `IntersectionObserver`

**Status:** `PROVISIONAL`

## Decision

Use a sentinel element observed with `IntersectionObserver` to request the next TanStack Query page.

Guard the callback with `hasNextPage` and `isFetchingNextPage`.

## Evidence

- MDN documents `IntersectionObserver` as asynchronously observing when a target intersects a viewport/ancestor and notes that it avoids continuous main-thread visibility polling.
  - https://developer.mozilla.org/en-US/docs/Web/API/Intersection_Observer_API

## Why this fits this assignment

The UI requirement is “load the next page when the user reaches the end,” not “react to every scroll position.” A sentinel intersection is the direct browser primitive for that condition.

## Trade-off accepted

Observer lifecycle must be handled correctly, but this is simpler and less error-prone than custom scroll-position calculations.

---

# ADR-011 — Relative Time: native `Intl.RelativeTimeFormat`

**Status:** `DECIDED` — implemented as `formatRelativeTime` in `frontend/lib/time/relativeTime.ts` (P3).

## Decision

Use a small project utility that converts an absolute timestamp into seconds/minutes/hours/days and formats the final value with:

```ts
new Intl.RelativeTimeFormat("ko", { numeric: "always" })
```

Do not add a date library only for this requirement.

## Evidence

- Assignment evidence: list timestamps must be relative to the current time.
- MDN documents `Intl.RelativeTimeFormat` as the native language-sensitive relative-time formatter and marks it widely available.
  - https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/RelativeTimeFormat

## Why `numeric: "always"`

The assignment examples use forms such as `3분 전`, `2일 전`. `numeric: "always"` keeps output numeric rather than switching some values to words such as “어제”.

## Trade-off accepted

We still own threshold selection (seconds vs minutes vs hours vs days), but avoid a dependency for a small formatting rule.

---

# ADR-012 — Backend Validation, Ownership, Transactions, and Error Handling

**Status:** `DECIDED`

## Decision

### Validation

Backend request DTOs use a small explicit validator without adding a validation dependency:

- title: non-null, nonblank, and at most 20 Unicode code points via `codePointCount`
- post content: non-null and nonblank
- comment content: non-null and nonblank, with no maximum length added

Blank checks are validation-only; accepted values are stored without automatic trimming.

Frontend validation mirrors these rules for UX, but backend validation is authoritative.

### Ownership

Resolve requester deterministically:

```text
x-user-id header > x-user-id cookie > apr_tester
```

The precedence is an implementation decision because the assignment allows header or cookie but does not specify conflict precedence.

Ownership checks live in the service/application layer, not only in UI visibility.

### Transactions

Use service-level `@Transactional` around mutation methods. Readable targets are locked before ownership-sensitive changes, and like toggles serialize on the post row while the existing composite primary key preserves requester uniqueness.

### Errors

Use `@RestControllerAdvice` for consistent JSON errors.

Implemented status semantics:

```text
400 validation / malformed input
403 authenticated-by-assignment requester does not own resource
404 missing or soft-deleted resource
201 successful creation
200 successful read/update/toggle
204 successful delete
```

The assignment explicitly allows status-code selection as long as behavior is correct.

## Evidence

- Repository evidence: `MutationValidator` performs dependency-free null/blank and Unicode code-point validation, while `ApiExceptionHandler` maps unreadable JSON to the shared error body.
- Spring Framework documents declarative `@Transactional` support.
  - https://docs.spring.io/spring-framework/reference/6.2/data-access/transaction/declarative/annotations.html
- Spring MVC documents `@RestControllerAdvice` as controller-wide exception handling that renders response bodies.
  - https://docs.spring.io/spring-framework/reference/6.2/web/webmvc/mvc-controller/ann-advice.html

## Trade-off accepted

This creates a small amount of explicit DTO/error mapping code, but keeps validation, authorization, and HTTP semantics out of repository code and prevents the frontend from becoming the security boundary.

---

# ADR-013 — Seed Initialization: transactional, idempotent, source-preserving

**Status:** `OPEN`

## Decision

Use the runtime `APP_SEED_PATH` as the only assignment seed source.

Proposed flow:

```text
Flyway schema ready
    ↓
check seed_history
    ↓ already imported
skip
    ↓ not imported
read seed.json
    ↓
transaction:
  insert posts with explicit IDs
  insert comments with explicit IDs
  apply deletedPostIds / deletedCommentIds
  record seed_history marker
    ↓
align post/comment identity sequences
```

Do not re-apply seed values on every restart because doing so could overwrite user edits made after first boot.

Exact startup hook remains `OPEN` until P0 verifies the starter lifecycle and compose healthcheck timing.

## Evidence

- Assignment evidence: data must be queryable after application startup and repeated startup must not duplicate seed data.
- Provided README: `seed/` is mounted read-only at `/seed` via `APP_SEED_PATH`, while PostgreSQL data persists in a named volume.
- Spring Boot 3.5 documents database initialization ordering and mechanisms.
  - https://docs.spring.io/spring-boot/3.5/how-to/data-initialization.html

## Why use a seed marker rather than only `COUNT(*) > 0`

Table non-emptiness does not prove that the assignment seed was completely and successfully imported. A specific marker written only after the import transaction succeeds gives the operation explicit semantics.

## Open question for P0

Determine the cleanest lifecycle hook that guarantees the seed has finished before the application should be considered ready, without violating the required `/health` behavior.

---

# 2. Proposed Backend Structure

**Status:** `PROVISIONAL` until package names are audited.

Keep the structure shallow and feature-oriented rather than introducing interfaces/abstractions with no concrete second implementation.

```text
backend/.../
├── common/
│   ├── error/
│   └── requester/
├── post/
│   ├── PostController
│   ├── PostService
│   ├── PostRepository
│   └── dto/
├── comment/
│   ├── CommentController
│   ├── CommentService
│   ├── CommentRepository
│   └── dto/
├── like/
│   ├── LikeService
│   └── LikeRepository
└── seed/
    ├── SeedLoader
    └── SeedService
```

Layer responsibility:

```text
Controller  HTTP translation only
Service     ownership / validation-adjacent domain orchestration / transactions
Repository  SQL and row mapping
DTO         API contract
```

Do not introduce repository interfaces purely to mimic enterprise architecture. Add an interface only when it creates an actual testing or implementation boundary.

---

# 3. Proposed Frontend Structure

**Status:** `PROVISIONAL` until P0 and `UI_SPEC.md`.

```text
frontend/
├── app/
│   ├── dev-user/          # immutable starter code
│   └── ...routes
├── features/
│   ├── posts/
│   ├── comments/
│   └── likes/
├── components/
│   └── shared visual primitives only when reused
└── lib/
    ├── api/
    ├── query/
    └── time/
```

Principles:

- route files compose screens;
- feature code owns product behavior;
- one shared API client owns base URL, JSON handling, and `credentials: "include"`;
- TanStack Query owns remote state;
- local component state owns ephemeral presentation state;
- exact component breakdown is informed by the audited screen/state inventory in `docs/UI_SPEC.md` and will be finalized during frontend implementation.

---

# 4. Architecture Decisions Still Open Before P1/P3

The following may not be finalized from documentation alone:

| Decision | Why still open | Evidence needed |
|---|---|---|
| exact column lengths/types | seed shape has not been directly inspected | actual `seed/seed.json` |
| exact seed startup hook | must align with starter lifecycle/healthcheck | actual backend + compose |
| exact Flyway dependencies | depends on current Gradle file and dependency management | actual build file |
| final styling choice | should not fight an already-installed starter system | actual `package.json` / styles |
| exact frontend component tree | URL route contract is fixed in P3 (`frontend/lib/routes.ts`); component decomposition belongs to P4–P6 | audited `docs/UI_SPEC.md` plus implemented screen code |
| automated DB integration-test mechanism | should fit existing test/build environment | actual Gradle/CI setup |

These are not permission for an agent to guess. Resolve them in the applicable implementation phase using the audited design and repository evidence.

---

# 5. Source Index

## Assignment / starter sources

- `ASSIGNMENT.md`
- provided `README.md` template
- actual starter repository files once P0 audit is available

## Spring / backend

- Spring Framework 6.2 — JDBC
  - https://docs.spring.io/spring-framework/reference/6.2/data-access/jdbc.html
- Spring Framework 6.2 — JDBC Core / JdbcClient
  - https://docs.spring.io/spring-framework/reference/6.2/data-access/jdbc/core.html
- Spring Boot 3.5 — SQL Databases
  - https://docs.spring.io/spring-boot/3.5/reference/data/sql.html
- Spring Boot 3.5 — Database Initialization / Flyway
  - https://docs.spring.io/spring-boot/3.5/how-to/data-initialization.html
- Spring Framework 6.2 — Validation
  - https://docs.spring.io/spring-framework/reference/6.2/web/webmvc/mvc-controller/ann-validation.html
- Spring Framework 6.2 — `@Transactional`
  - https://docs.spring.io/spring-framework/reference/6.2/data-access/transaction/declarative/annotations.html
- Spring Framework 6.2 — Controller Advice
  - https://docs.spring.io/spring-framework/reference/6.2/web/webmvc/mvc-controller/ann-advice.html
- Flyway — Versioned Migrations
  - https://documentation.red-gate.com/fd/versioned-migrations-273973333.html

## PostgreSQL

- LIMIT / OFFSET
  - https://www.postgresql.org/docs/current/queries-limit.html
- Identity Columns
  - https://www.postgresql.org/docs/current/ddl-identity-columns.html
- Sequence Functions
  - https://www.postgresql.org/docs/current/functions-sequence.html
- System Information / `pg_get_serial_sequence`
  - https://www.postgresql.org/docs/current/functions-info.html
- Date / Time Types
  - https://www.postgresql.org/docs/current/datatype-datetime.html
- Foreign Keys
  - https://www.postgresql.org/docs/current/tutorial-fk.html
- Multicolumn Indexes
  - https://www.postgresql.org/docs/current/indexes-multicolumn.html
- Partial Indexes
  - https://www.postgresql.org/docs/current/indexes-partial.html

## Next.js / frontend

- Next.js 15 — Fetching Data
  - https://nextjs.org/docs/15/app/getting-started/fetching-data
- Next.js — Server and Client Components
  - https://nextjs.org/docs/app/getting-started/server-and-client-components
- Next.js 15 — CSS Modules in App Router migration guide
  - https://nextjs.org/docs/15/app/guides/migrating/from-create-react-app#step-5-styles
- Next.js — CSS
  - https://nextjs.org/docs/app/getting-started/css
- TanStack Query — `useInfiniteQuery`
  - https://tanstack.com/query/latest/docs/framework/react/reference/functions/useInfiniteQuery

## Browser APIs

- MDN — Intersection Observer API
  - https://developer.mozilla.org/en-US/docs/Web/API/Intersection_Observer_API
- MDN — `Intl.RelativeTimeFormat`
  - https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/RelativeTimeFormat
