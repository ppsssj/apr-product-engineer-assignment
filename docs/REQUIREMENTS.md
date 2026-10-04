# Requirements Traceability Matrix

This document normalizes `ASSIGNMENT.md` into traceable implementation requirements.

`ASSIGNMENT.md` remains the canonical source of truth. If this file conflicts with it, follow `ASSIGNMENT.md` and fix this document.

## Status

- `TODO`: not implemented
- `IMPLEMENTED`: implemented but not acceptance-verified
- `VERIFIED`: verified with evidence
- `BLOCKED`: blocked by missing input or unresolved prerequisite

---

## 1. Submission & Repository

| ID | Requirement | Source | Status |
|---|---|---|---|
| SUB-001 | Repository name is `apr-product-engineer-assignment`. | §1 | VERIFIED |
| SUB-002 | Repository is private. | §1 | VERIFIED |
| SUB-003 | `APRCORPORATION` is added as collaborator. | §1 | TODO |
| SUB-004 | Work is committed on a branch other than `main`. | §1 | VERIFIED |
| SUB-005 | A PR is created and merged into `main`. | §1 | VERIFIED |
| SUB-006 | Final repository URL can be submitted by email. | §1 | TODO |

> Submission-state audit (2026-10-04): GitHub reports the repository as
> private with the required name. Work was completed through non-`main`
> branches and merged pull requests, including PR #50 on `main` at
> `3c902dc`. `APRCORPORATION` has neither collaborator access nor a pending
> invitation, so `SUB-003` remains `TODO`. Sending the final repository URL by
> email is an external submission step, so `SUB-006` also remains `TODO`.

---

## 2. Mandatory Stack & Runtime

| ID | Requirement | Source | Status |
|---|---|---|---|
| ENV-001 | Backend uses Java 17. | §2 | VERIFIED |
| ENV-002 | Backend uses Spring Boot 3.x. | §2 | VERIFIED |
| ENV-003 | Frontend uses TypeScript. | §2 | VERIFIED |
| ENV-004 | Frontend uses Next.js 15 App Router. | §2 | VERIFIED |
| ENV-005 | Database is PostgreSQL. | §2 | VERIFIED |
| ENV-006 | `docker compose up` starts DB, backend, and frontend. | §2, §7 | VERIFIED |
| ENV-007 | `GET /health` path remains unchanged. | §3 | VERIFIED |
| ENV-008 | `GET /health` returns HTTP 200. | §3 | VERIFIED |
| ENV-009 | `FRONTEND_PORT` override works. | §7 | VERIFIED |
| ENV-010 | `BACKEND_PORT` override works. | §7 | VERIFIED |
| ENV-011 | `DB_PORT` override works. | §7 | VERIFIED |
| ENV-012 | README documents any runtime prerequisites needed for successful startup. | §7 | VERIFIED |

> P8-B:
>
> - `ENV-001`: Java 17 comes from the `build.gradle` toolchain
>   `JavaLanguageVersion.of(17)` and the `gradle:8.14-jdk17` /
>   `eclipse-temurin:17-jre` Docker stages. The runtime log reports
>   `Java 17.0.20.1`.
> - `ENV-002`: Spring Boot comes from the `org.springframework.boot` plugin
>   `3.5.0`, and the backend passed `gradlew clean test`.
> - `ENV-005`: PostgreSQL is Compose `postgres:16-alpine`; Flyway reported
>   `PostgreSQL 16.15`.
> - `ENV-006`–`ENV-008`: V-001 (P8-A clean-volume boot with the seed-readiness
>   health gate) plus the P8-B healthy stack with `/health` 200.
> - `ENV-009`–`ENV-011`: V-002 (P8-A alternate ports 13000/18080/25432).
> - `ENV-012` belongs to P9 (README).

---

## 3. Starter Contracts

| ID | Requirement | Source | Status |
|---|---|---|---|
| START-001 | `frontend/app/dev-user/` remains unchanged. | §3 | VERIFIED |
| START-002 | Existing CORS behavior remains compatible with credentialed browser requests. | §3 | VERIFIED |
| START-003 | Frontend browser requests that rely on the cookie send credentials. | §3 | VERIFIED |

> P3: `ENV-003`/`ENV-004` are verified by strict `tsc --noEmit` and `next build` over the `app/` router with Next.js 15.4.6. `START-003` is `IMPLEMENTED` because the shared client in `frontend/lib/api/client.ts` always sends `credentials: "include"`; it becomes `VERIFIED` only after a real browser `/dev-user` → product request flow in P4/P5 or P8. `START-001`, `START-002`, and `USER-004` are unchanged by P3 and remain for final acceptance.

> P4: `START-003` is `VERIFIED` by a real Chromium session that selected `mintcream` on the unchanged `/dev-user` page and was redirected to `/`; the list request `GET /api/posts?limit=10` carried `Cookie: x-user-id=mintcream`, and the response reported that user's temporary like as `isLiked: true` (V-012 P4 evidence).

> P8-B:
>
> - `START-001`: `git diff 38a9755 HEAD -- frontend/app/dev-user/` is empty
>   (V-004).
> - `START-002`: `CorsConfig.java` is likewise unchanged from the starter.
>   Every P8-B browser request from `:3000` to `:8080` carried the
>   `x-user-id` cookie and succeeded, including preflighted PATCH and DELETE
>   (see "P8-B Full Behavioral Acceptance" in `docs/VERIFICATION.md`).

---

## 4. Seed Data

| ID | Requirement | Source | Status |
|---|---|---|---|
| SEED-001 | Posts from `seed/seed.json` are persisted and queryable after startup. | §3 | VERIFIED |
| SEED-002 | Comments from `seed/seed.json` are persisted and queryable after startup. | §3 | VERIFIED |
| SEED-003 | `deletedPostIds` entries are persisted as deleted and excluded from normal reads. | §3 | VERIFIED |
| SEED-004 | `deletedCommentIds` entries are persisted as deleted and excluded from normal reads. | §3 | VERIFIED |
| SEED-005 | IDs from the seed are preserved exactly. | §3 | VERIFIED |
| SEED-006 | Newly created post IDs may be generated independently without conflicting with preserved seed IDs. | §3 | VERIFIED |
| SEED-007 | Newly created comment IDs may be generated independently without conflicting with preserved seed IDs. | §3 | VERIFIED |
| SEED-008 | `commentCount` is derived from comments rather than read from the seed. | §3 | VERIFIED |
| SEED-009 | Deleted comments are excluded from `commentCount`. | §3, §4 | VERIFIED |
| SEED-010 | Initial `isLiked` is false for every requester because the seed contains no like history. | §3 | VERIFIED |
| SEED-011 | Initial `likeCount` starts from the seed value. | §3 | VERIFIED |
| SEED-012 | Repeated application startup does not duplicate seed rows. | §3 | VERIFIED |

---

## 5. Current User & Ownership

| ID | Requirement | Source | Status |
|---|---|---|---|
| USER-001 | Current user may be resolved from `x-user-id` request header. | §4 | VERIFIED |
| USER-002 | Current user may be resolved from `x-user-id` cookie. | §4 | VERIFIED |
| USER-003 | If neither header nor cookie is present, current user is `apr_tester`. | §4 | VERIFIED |
| USER-004 | Login/authentication UI is not implemented. | §4 | VERIFIED |
| USER-005 | Post ownership is determined by `post.author == currentUser`. | §4 | VERIFIED |
| USER-006 | Comment ownership is determined by `comment.author == currentUser`. | §4 | VERIFIED |
| USER-007 | A separate user table is not required for the assignment. | §4 | VERIFIED |

> P8-B:
>
> - `USER-004`: the frontend routes are only `/`, `/dev-user`, `/posts/new`,
>   `/posts/[postId]`, and `/posts/[postId]/edit`, and the frontend source has
>   no login, sign-in, or password code. The requester comes only from the
>   starter `/dev-user` cookie or the `x-user-id` header.
> - `USER-007`: the Flyway `V1` schema creates only `posts`, `comments`,
>   `post_likes`, and `seed_history`. Ownership compares `author` with the
>   resolved requester string, re-verified in P8-B for posts and comments.

---

## 6. Global UI Behavior

| ID | Requirement | Source | Status |
|---|---|---|---|
| UI-001 | Product is implemented as mobile web. | §4 | VERIFIED |
| UI-002 | Primary target viewport is 375px. | §4 | VERIFIED |
| UI-003 | Desktop-specific adaptation is not required. | §4 | VERIFIED |
| UI-004 | Official Figma design is reflected as closely as possible when available. | §4 | VERIFIED |
| UI-005 | Undefined design/flow details may be completed reasonably. | §4 | VERIFIED |
| UI-006 | Avatar uses one of the three provided avatar assets. | §4 | VERIFIED |
| UI-007 | Avatar selection policy may be implementation-defined. | §4 | VERIFIED |
| UI-008 | Any intentional UI/UX improvement is documented in README with rationale. | §4 | VERIFIED |

> Final V-081 audit (2026-10-04): `UI-004` is `VERIFIED`. The official Figma
> file/node mapping was directly revalidated, all 12 repository-preserved
> official exports plus the shared edit-form state were compared with the
> 375×778 Chromium implementation, and no accidental mismatch was found. See
> `docs/DESIGN_COMPARISON.md` and V-081 in `docs/VERIFICATION.md`.
>
> Historical P4 status: `UI-006`/`UI-007` were `VERIFIED` — every list avatar
> was one of `/avatar-1.png`–`/avatar-3.png`, chosen by
> `frontend/lib/avatar.ts` `avatarFor()` (`stableHash(author) % 3`), identical
> for one author across rows and reloads. At that stage `UI-001`, `UI-002`, and
> `UI-004` were still `TODO`: only the list screen existed, V-080 needed every
> required screen, and V-081 had not been run.
>
> P8-B:
>
> - `UI-005` is `VERIFIED`. Flows the assignment and Figma leave undefined
>   were completed and exercised in the browser:
>   - loading, empty, and error states with retry;
>   - not-found for deleted, missing, or invalid posts;
>   - the no-permission edit notice;
>   - toast lifetime;
>   - failure handling that keeps input.
>
>   Their values are recorded as `PRODUCT_DECISION`s in `docs/UI_SPEC.md`.
> - Historical P8-B status: `UI-004` stayed `TODO` pending V-081 comparison
>   against the official Figma source.
> - `UI-008` (README) belongs to P9.

---

## 7. Soft Delete Policy

| ID | Requirement | Source | Status |
|---|---|---|---|
| DEL-001 | Deleting a post does not physically delete the DB row. | §4 | VERIFIED |
| DEL-002 | Deleting a comment does not physically delete the DB row. | §4 | VERIFIED |
| DEL-003 | Deleted posts are absent from list responses. | §4 | VERIFIED |
| DEL-004 | Deleted posts are inaccessible through normal detail reads. | §4 | VERIFIED |
| DEL-005 | Deleted comments are absent from comment list responses. | §4 | VERIFIED |
| DEL-006 | Deleted comments are excluded from `commentCount`. | §4 | VERIFIED |
| DEL-007 | UI behaves as if deleted resources no longer exist. | §4 | VERIFIED |

> Deletion Policy Hardening (`4e53dbd`): the DEL requirements follow one model (`docs/ARCHITECTURE.md` "Deletion Consistency Model"):
> - Children of a deleted post stay physically unchanged but unreachable.
> - Unreadable resources return 404 before any ownership check, and a repeated DELETE returns 404.
> - A confirmed post delete lowers every cached list total, including for posts outside the loaded pages.
>
> Evidence is in `docs/VERIFICATION.md` "Deletion Consistency Evidence" (28/28).

---

## 8. Post List

| ID | Requirement | Source | Status |
|---|---|---|---|
| LIST-001 | Post timestamps are shown as relative time. | §4 | VERIFIED |
| LIST-002 | Relative time is calculated against current time. | §4 | VERIFIED |
| LIST-003 | Posts are ordered newest first. | §4 | VERIFIED |
| LIST-004 | Post list uses infinite scroll. | §4 | VERIFIED |
| LIST-005 | Reaching the end loads the next page rather than all posts at once. | §4, §5 | VERIFIED |
| LIST-006 | Floating/right-bottom write action navigates to the post creation screen. | §4 | VERIFIED |

> P4: `LIST-001`/`LIST-002` render `frontend/lib/time/relativeTime.ts` output against `Date.now()` for every row. `LIST-004`/`LIST-005` were verified in the browser: 10/10/10/9 pages fetched only as the `IntersectionObserver` sentinel approached, all 39 readable posts rendered once in API order, and no request followed the final `nextCursor: null`. `LIST-006` is `IMPLEMENTED`: the FAB navigates to `/posts/new`, but that creation screen is built in P6, so the requirement is verified there. `DEL-007` stays `TODO` until detail/comment UI exists, although the list already renders only the 39 readable posts (seed-deleted `9`, `30`, `31` absent).

---

## 9. Post Detail

| ID | Requirement | Source | Status |
|---|---|---|---|
| DETAIL-001 | Like action is a toggle. | §4 | VERIFIED |
| DETAIL-002 | Like toggle updates displayed count. | §4 | VERIFIED |
| DETAIL-003 | Like toggle updates visual liked state/icon. | §4 | VERIFIED |
| DETAIL-004 | Comments are ordered newest first. | §4 | VERIFIED |
| DETAIL-005 | Detail initially displays at most 2 comments. | §4 | VERIFIED |
| DETAIL-006 | “More” control appears only when comment total is 3 or more. | §4 | VERIFIED |
| DETAIL-007 | “More” control does not appear when total comments are 2 or fewer. | §4 | VERIFIED |
| DETAIL-008 | After “More” is activated, remaining comments load through infinite scroll. | §4 | VERIFIED |
| DETAIL-009 | User can create a comment. | §4 | VERIFIED |
| DETAIL-010 | Only the author of a comment can delete that comment. | §4 | VERIFIED |
| DETAIL-011 | Deleted comment disappears from the displayed list. | §4 | VERIFIED |
| DETAIL-012 | Only the author of a post sees/uses the post action menu. | §4 | VERIFIED |
| DETAIL-013 | Own-post menu exposes edit and delete actions. | §4 | VERIFIED |
| DETAIL-014 | Edit screen reuses the create screen layout/flow. | §4 | VERIFIED |
| DETAIL-015 | Edit screen opens with existing title/content prefilled. | §4 | VERIFIED |
| DETAIL-016 | Saving an edit updates the post. | §4 | VERIFIED |
| DETAIL-017 | Editing enforces the same validation as creation. | §4 | VERIFIED |
| DETAIL-018 | Deleting own post navigates back to the list. | §4 | VERIFIED |
| DETAIL-019 | A user cannot edit another user’s post. | §4 | VERIFIED |
| DETAIL-020 | A user cannot delete another user’s post. | §4 | VERIFIED |

> P5A: `DETAIL-001`–`DETAIL-008` are `VERIFIED` by browser runs at 375×778 against the Compose production frontend (V-050 UI evidence, V-060, V-061). `DETAIL-009`/`DETAIL-010` keep their P2B backend `VERIFIED` status, but their UI (comment composer, own-comment delete) is P5B, as is `DETAIL-011`. `DETAIL-012`–`DETAIL-018` are P6.

> P5B: `DETAIL-009` and `DETAIL-010` stay `VERIFIED` (P2B backend evidence), now with UI acceptance. The fixed composer creates comments whose author is the server-resolved requester (`apr_tester` by default, `mintcream` after `/dev-user`), and `삭제` appears only on the requester's own comments, including on other users' posts. The backend still returns 403 for a cross-user delete. `DETAIL-011` is `VERIFIED`: a confirmed delete removes the comment from the collapsed and expanded lists, updates the comment total, detail count, and cached list count, and the comment stays absent after reload. Evidence is in V-062–V-064 and "P5B Comment Mutation Evidence" in `docs/VERIFICATION.md`. `DEL-007` stays `TODO` until the P6 post-delete UI exists.

> P6 (`1f8a21f`, browser evidence in "P6 Post CRUD Evidence" in `docs/VERIFICATION.md`):
> - `DETAIL-012`/`DETAIL-013`: only the post author sees the `게시글 메뉴` trigger, which opens 수정하기 / 삭제하기 / 취소. This was checked for `apr_tester` and `mintcream`, in both directions.
> - `DETAIL-014`–`DETAIL-017`: edit reuses the create form, prefills exact values, saves through PATCH, and applies the same validation (blank, 20 Unicode code points).
> - `DETAIL-018`: own-post delete returns to the list.
> - `DETAIL-019`/`DETAIL-020` keep backend evidence. The UI also hides the trigger on other users' posts and shows no form on `/posts/{other}/edit`, and the backend still returns 403 for PATCH/DELETE.
> - `EDIT-004`: invalid input disables `작성 완료` / `수정 완료` and sends no request.
> - `EDIT-005`/`EDIT-006`: a successful create returns to the list with a one-time `게시글이 등록되었습니다.` toast.
> - `EDIT-007`: no comment edit UI or endpoint exists.
> - `LIST-006`: the FAB opens the working create screen.
> - `DEL-007`: deleted posts disappear from cached and refetched lists and their detail/edit URLs show not-found. Deleted comments and seed-deleted resources behave the same (P4/P5B).
> - `UI-001`–`UI-003`: V-080 `PASS`; every required screen was checked at 375×778, and no desktop-specific layout exists.

---

## 10. Post Creation / Editing Validation

| ID | Requirement | Source | Status |
|---|---|---|---|
| EDIT-001 | Post title is required. | §4 | VERIFIED |
| EDIT-002 | Post content is required. | §4 | VERIFIED |
| EDIT-003 | Post title maximum length is 20 characters. | §4 | VERIFIED |
| EDIT-004 | Save/complete action is disabled when required input is invalid. | §4 | VERIFIED |
| EDIT-005 | Successful creation navigates back to the post list. | §4 | VERIFIED |
| EDIT-006 | Successful creation shows a completion toast. | §4 | VERIFIED |
| EDIT-007 | Comment editing is not implemented. | §4 | VERIFIED |

---

## 11. Required API Surface

Base path: `http://localhost:8080/api`

| ID | Method | Path | Requirement | Status |
|---|---|---|---|---|
| API-001 | GET | `/posts` | Return newest-first post list. | VERIFIED |
| API-002 | POST | `/posts` | Create post from `{ title, content }`. | VERIFIED |
| API-003 | GET | `/posts/{postId}` | Return single post detail. | VERIFIED |
| API-004 | PATCH | `/posts/{postId}` | Update own post from `{ title, content }`. | VERIFIED |
| API-005 | DELETE | `/posts/{postId}` | Soft-delete own post. | VERIFIED |
| API-006 | POST | `/posts/{postId}/likes/toggle` | Toggle requester-specific like state. | VERIFIED |
| API-007 | GET | `/posts/{postId}/comments` | Return newest-first comments. | VERIFIED |
| API-008 | POST | `/posts/{postId}/comments` | Create comment from `{ content }`. | VERIFIED |
| API-009 | DELETE | `/comments/{commentId}` | Soft-delete own comment. | VERIFIED |

---

## 12. API Response Contracts

| ID | Requirement | Source | Status |
|---|---|---|---|
| API-010 | Request/response bodies are JSON. | §5 | VERIFIED |
| API-011 | `GET /posts` response contains `items`. | §5 | VERIFIED |
| API-012 | `GET /posts` response contains `total`. | §5 | VERIFIED |
| API-013 | `GET /posts` `total` means total matching readable rows, not current page size. | §5 | VERIFIED |
| API-014 | Post list item includes `id`. | §5 | VERIFIED |
| API-015 | Post list item includes `title`. | §5 | VERIFIED |
| API-016 | Post list item includes `content`. | §5 | VERIFIED |
| API-017 | Post list item includes `likeCount`. | §5 | VERIFIED |
| API-018 | Post list item includes `commentCount`. | §5 | VERIFIED |
| API-019 | Post list item includes requester-specific `isLiked`. | §5 | VERIFIED |
| API-020 | Post list item includes `author`. | §5 | VERIFIED |
| API-021 | Post list item includes `createdAt`. | §5 | VERIFIED |
| API-022 | `GET /posts/{postId}` returns a single post object rather than an `items` envelope. | §5 | VERIFIED |
| API-023 | `GET /posts/{postId}` includes `isLiked` and `commentCount`. | §5 | VERIFIED |
| API-024 | Comment list response contains `items`. | §5 | VERIFIED |
| API-025 | Comment list response contains `total`. | §5 | VERIFIED |
| API-026 | Comment item includes `id`, `postId`, `content`, `author`, `createdAt`. | §5 | VERIFIED |
| API-027 | Like toggle response contains `isLiked`. | §5 | VERIFIED |
| API-028 | Like toggle response contains `likeCount`. | §5 | VERIFIED |
| API-029 | Patch response returns the updated post object. | §5 | VERIFIED |
| API-030 | `isLiked` is evaluated per requester. | §5 | VERIFIED |
| API-031 | List APIs support pagination designed by the implementation. | §5 | VERIFIED |
| API-032 | List APIs do not return the complete collection in one request. | §5 | VERIFIED |
| API-033 | Error/success status codes are chosen consistently by the implementation. | §5 | VERIFIED |
| API-034 | Unauthorized ownership operations are rejected by backend behavior. | §5 | VERIFIED |
| API-035 | Deleted resources are excluded/rejected consistently by API behavior. | §5 | VERIFIED |

---

## 13. README / Evaluation Documentation

| ID | Requirement | Source | Status |
|---|---|---|---|
| DOC-001 | README explains technical choices such as state management, ORM, and styling. | §6 | VERIFIED |
| DOC-002 | README explains where/how server state is stored. | §6 | VERIFIED |
| DOC-003 | README explains frontend/backend layering criteria. | §6 | VERIFIED |
| DOC-004 | README explains how `seed/seed.json` is loaded and persisted. | §6 | VERIFIED |
| DOC-005 | README references actual implementation files/functions. | §6 | VERIFIED |
| DOC-006 | README explains why important choices were made. | §6 | VERIFIED |
| DOC-007 | README explains relevant trade-offs / what alternatives were not chosen. | §6 | VERIFIED |
| DOC-008 | README explains tests written and their purpose if tests are included. | §6 | VERIFIED |
| DOC-009 | README documents intentional UI/UX deviations or improvements if any. | §4, §6 | VERIFIED |

> Final documentation refresh (2026-10-04): the evaluator-facing README was
> re-audited against base `main` `3c902dc` and retains the assignment's six
> required top-level sections. It records actual technology choices, server
> state/cache rules, responsibility boundaries, seed lifecycle, measured
> request units, trade-offs and intentional UI decisions. Detailed decision
> history moved to `docs/ENGINEERING_NOTES.md`; architecture, raw performance
> and execution evidence remain in their canonical documents.
>
> `DOC-008` now distinguishes committed automation from broader acceptance.
> Backend tests include real PostgreSQL Testcontainers coverage; frontend
> Vitest covers cache/retry rules; CI starts the production Compose stack and
> runs three committed Chromium Playwright regressions. Full cursor traversal,
> every failure/ownership combination, other browsers, visual regression and
> Lighthouse remain outside that Playwright set.
>
> Playwright verifies selected behavior, not official-Figma fidelity. That
> separate comparison is now complete: V-081 is `PASS`, and `UI-004` is
> `VERIFIED` (see `docs/DESIGN_COMPARISON.md`).

---

## 14. Completion Rule

The assignment is release-ready only when:

- every mandatory requirement above is `VERIFIED`, or
- an item is explicitly non-mandatory/conditional and documented accordingly,
- and all release-gate scenarios in `docs/VERIFICATION.md` pass.
