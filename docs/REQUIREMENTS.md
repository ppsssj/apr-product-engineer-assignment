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
| SUB-001 | Repository name is `apr-product-engineer-assignment`. | §1 | TODO |
| SUB-002 | Repository is private. | §1 | TODO |
| SUB-003 | `APRCORPORATION` is added as collaborator. | §1 | TODO |
| SUB-004 | Work is committed on a branch other than `main`. | §1 | TODO |
| SUB-005 | A PR is created and merged into `main`. | §1 | TODO |
| SUB-006 | Final repository URL can be submitted by email. | §1 | TODO |

---

## 2. Mandatory Stack & Runtime

| ID | Requirement | Source | Status |
|---|---|---|---|
| ENV-001 | Backend uses Java 17. | §2 | TODO |
| ENV-002 | Backend uses Spring Boot 3.x. | §2 | TODO |
| ENV-003 | Frontend uses TypeScript. | §2 | TODO |
| ENV-004 | Frontend uses Next.js 15 App Router. | §2 | TODO |
| ENV-005 | Database is PostgreSQL. | §2 | TODO |
| ENV-006 | `docker compose up` starts DB, backend, and frontend. | §2, §7 | TODO |
| ENV-007 | `GET /health` path remains unchanged. | §3 | TODO |
| ENV-008 | `GET /health` returns HTTP 200. | §3 | TODO |
| ENV-009 | `FRONTEND_PORT` override works. | §7 | TODO |
| ENV-010 | `BACKEND_PORT` override works. | §7 | TODO |
| ENV-011 | `DB_PORT` override works. | §7 | TODO |
| ENV-012 | README documents any runtime prerequisites needed for successful startup. | §7 | TODO |

---

## 3. Starter Contracts

| ID | Requirement | Source | Status |
|---|---|---|---|
| START-001 | `frontend/app/dev-user/` remains unchanged. | §3 | TODO |
| START-002 | Existing CORS behavior remains compatible with credentialed browser requests. | §3 | TODO |
| START-003 | Frontend browser requests that rely on the cookie send credentials. | §3 | TODO |

---

## 4. Seed Data

| ID | Requirement | Source | Status |
|---|---|---|---|
| SEED-001 | Posts from `seed/seed.json` are persisted and queryable after startup. | §3 | VERIFIED |
| SEED-002 | Comments from `seed/seed.json` are persisted and queryable after startup. | §3 | VERIFIED |
| SEED-003 | `deletedPostIds` entries are persisted as deleted and excluded from normal reads. | §3 | IMPLEMENTED |
| SEED-004 | `deletedCommentIds` entries are persisted as deleted and excluded from normal reads. | §3 | IMPLEMENTED |
| SEED-005 | IDs from the seed are preserved exactly. | §3 | VERIFIED |
| SEED-006 | Newly created post IDs may be generated independently without conflicting with preserved seed IDs. | §3 | VERIFIED |
| SEED-007 | Newly created comment IDs may be generated independently without conflicting with preserved seed IDs. | §3 | VERIFIED |
| SEED-008 | `commentCount` is derived from comments rather than read from the seed. | §3 | IMPLEMENTED |
| SEED-009 | Deleted comments are excluded from `commentCount`. | §3, §4 | IMPLEMENTED |
| SEED-010 | Initial `isLiked` is false for every requester because the seed contains no like history. | §3 | IMPLEMENTED |
| SEED-011 | Initial `likeCount` starts from the seed value. | §3 | VERIFIED |
| SEED-012 | Repeated application startup does not duplicate seed rows. | §3 | VERIFIED |

---

## 5. Current User & Ownership

| ID | Requirement | Source | Status |
|---|---|---|---|
| USER-001 | Current user may be resolved from `x-user-id` request header. | §4 | TODO |
| USER-002 | Current user may be resolved from `x-user-id` cookie. | §4 | TODO |
| USER-003 | If neither header nor cookie is present, current user is `apr_tester`. | §4 | TODO |
| USER-004 | Login/authentication UI is not implemented. | §4 | TODO |
| USER-005 | Post ownership is determined by `post.author == currentUser`. | §4 | TODO |
| USER-006 | Comment ownership is determined by `comment.author == currentUser`. | §4 | TODO |
| USER-007 | A separate user table is not required for the assignment. | §4 | TODO |

---

## 6. Global UI Behavior

| ID | Requirement | Source | Status |
|---|---|---|---|
| UI-001 | Product is implemented as mobile web. | §4 | TODO |
| UI-002 | Primary target viewport is 375px. | §4 | TODO |
| UI-003 | Desktop-specific adaptation is not required. | §4 | TODO |
| UI-004 | Official Figma design is reflected as closely as possible when available. | §4 | TODO |
| UI-005 | Undefined design/flow details may be completed reasonably. | §4 | TODO |
| UI-006 | Avatar uses one of the three provided avatar assets. | §4 | TODO |
| UI-007 | Avatar selection policy may be implementation-defined. | §4 | TODO |
| UI-008 | Any intentional UI/UX improvement is documented in README with rationale. | §4 | TODO |

> The official Figma source is now available and audited in `docs/UI_SPEC.md`. `UI-004` remains `TODO` because acquiring/auditing the design is not the same as implementing or acceptance-verifying visual fidelity.

---

## 7. Soft Delete Policy

| ID | Requirement | Source | Status |
|---|---|---|---|
| DEL-001 | Deleting a post does not physically delete the DB row. | §4 | TODO |
| DEL-002 | Deleting a comment does not physically delete the DB row. | §4 | TODO |
| DEL-003 | Deleted posts are absent from list responses. | §4 | TODO |
| DEL-004 | Deleted posts are inaccessible through normal detail reads. | §4 | TODO |
| DEL-005 | Deleted comments are absent from comment list responses. | §4 | TODO |
| DEL-006 | Deleted comments are excluded from `commentCount`. | §4 | TODO |
| DEL-007 | UI behaves as if deleted resources no longer exist. | §4 | TODO |

---

## 8. Post List

| ID | Requirement | Source | Status |
|---|---|---|---|
| LIST-001 | Post timestamps are shown as relative time. | §4 | TODO |
| LIST-002 | Relative time is calculated against current time. | §4 | TODO |
| LIST-003 | Posts are ordered newest first. | §4 | TODO |
| LIST-004 | Post list uses infinite scroll. | §4 | TODO |
| LIST-005 | Reaching the end loads the next page rather than all posts at once. | §4, §5 | TODO |
| LIST-006 | Floating/right-bottom write action navigates to the post creation screen. | §4 | TODO |

---

## 9. Post Detail

| ID | Requirement | Source | Status |
|---|---|---|---|
| DETAIL-001 | Like action is a toggle. | §4 | TODO |
| DETAIL-002 | Like toggle updates displayed count. | §4 | TODO |
| DETAIL-003 | Like toggle updates visual liked state/icon. | §4 | TODO |
| DETAIL-004 | Comments are ordered newest first. | §4 | TODO |
| DETAIL-005 | Detail initially displays at most 2 comments. | §4 | TODO |
| DETAIL-006 | “More” control appears only when comment total is 3 or more. | §4 | TODO |
| DETAIL-007 | “More” control does not appear when total comments are 2 or fewer. | §4 | TODO |
| DETAIL-008 | After “More” is activated, remaining comments load through infinite scroll. | §4 | TODO |
| DETAIL-009 | User can create a comment. | §4 | TODO |
| DETAIL-010 | Only the author of a comment can delete that comment. | §4 | TODO |
| DETAIL-011 | Deleted comment disappears from the displayed list. | §4 | TODO |
| DETAIL-012 | Only the author of a post sees/uses the post action menu. | §4 | TODO |
| DETAIL-013 | Own-post menu exposes edit and delete actions. | §4 | TODO |
| DETAIL-014 | Edit screen reuses the create screen layout/flow. | §4 | TODO |
| DETAIL-015 | Edit screen opens with existing title/content prefilled. | §4 | TODO |
| DETAIL-016 | Saving an edit updates the post. | §4 | TODO |
| DETAIL-017 | Editing enforces the same validation as creation. | §4 | TODO |
| DETAIL-018 | Deleting own post navigates back to the list. | §4 | TODO |
| DETAIL-019 | A user cannot edit another user’s post. | §4 | TODO |
| DETAIL-020 | A user cannot delete another user’s post. | §4 | TODO |

---

## 10. Post Creation / Editing Validation

| ID | Requirement | Source | Status |
|---|---|---|---|
| EDIT-001 | Post title is required. | §4 | TODO |
| EDIT-002 | Post content is required. | §4 | TODO |
| EDIT-003 | Post title maximum length is 20 characters. | §4 | TODO |
| EDIT-004 | Save/complete action is disabled when required input is invalid. | §4 | TODO |
| EDIT-005 | Successful creation navigates back to the post list. | §4 | TODO |
| EDIT-006 | Successful creation shows a completion toast. | §4 | TODO |
| EDIT-007 | Comment editing is not implemented. | §4 | TODO |

---

## 11. Required API Surface

Base path: `http://localhost:8080/api`

| ID | Method | Path | Requirement | Status |
|---|---|---|---|---|
| API-001 | GET | `/posts` | Return newest-first post list. | TODO |
| API-002 | POST | `/posts` | Create post from `{ title, content }`. | TODO |
| API-003 | GET | `/posts/{postId}` | Return single post detail. | TODO |
| API-004 | PATCH | `/posts/{postId}` | Update own post from `{ title, content }`. | TODO |
| API-005 | DELETE | `/posts/{postId}` | Soft-delete own post. | TODO |
| API-006 | POST | `/posts/{postId}/likes/toggle` | Toggle requester-specific like state. | TODO |
| API-007 | GET | `/posts/{postId}/comments` | Return newest-first comments. | TODO |
| API-008 | POST | `/posts/{postId}/comments` | Create comment from `{ content }`. | TODO |
| API-009 | DELETE | `/comments/{commentId}` | Soft-delete own comment. | TODO |

---

## 12. API Response Contracts

| ID | Requirement | Source | Status |
|---|---|---|---|
| API-010 | Request/response bodies are JSON. | §5 | TODO |
| API-011 | `GET /posts` response contains `items`. | §5 | TODO |
| API-012 | `GET /posts` response contains `total`. | §5 | TODO |
| API-013 | `GET /posts` `total` means total matching readable rows, not current page size. | §5 | TODO |
| API-014 | Post list item includes `id`. | §5 | TODO |
| API-015 | Post list item includes `title`. | §5 | TODO |
| API-016 | Post list item includes `content`. | §5 | TODO |
| API-017 | Post list item includes `likeCount`. | §5 | TODO |
| API-018 | Post list item includes `commentCount`. | §5 | TODO |
| API-019 | Post list item includes requester-specific `isLiked`. | §5 | TODO |
| API-020 | Post list item includes `author`. | §5 | TODO |
| API-021 | Post list item includes `createdAt`. | §5 | TODO |
| API-022 | `GET /posts/{postId}` returns a single post object rather than an `items` envelope. | §5 | TODO |
| API-023 | `GET /posts/{postId}` includes `isLiked` and `commentCount`. | §5 | TODO |
| API-024 | Comment list response contains `items`. | §5 | TODO |
| API-025 | Comment list response contains `total`. | §5 | TODO |
| API-026 | Comment item includes `id`, `postId`, `content`, `author`, `createdAt`. | §5 | TODO |
| API-027 | Like toggle response contains `isLiked`. | §5 | TODO |
| API-028 | Like toggle response contains `likeCount`. | §5 | TODO |
| API-029 | Patch response returns the updated post object. | §5 | TODO |
| API-030 | `isLiked` is evaluated per requester. | §5 | TODO |
| API-031 | List APIs support pagination designed by the implementation. | §5 | TODO |
| API-032 | List APIs do not return the complete collection in one request. | §5 | TODO |
| API-033 | Error/success status codes are chosen consistently by the implementation. | §5 | TODO |
| API-034 | Unauthorized ownership operations are rejected by backend behavior. | §5 | TODO |
| API-035 | Deleted resources are excluded/rejected consistently by API behavior. | §5 | TODO |

---

## 13. README / Evaluation Documentation

| ID | Requirement | Source | Status |
|---|---|---|---|
| DOC-001 | README explains technical choices such as state management, ORM, and styling. | §6 | TODO |
| DOC-002 | README explains where/how server state is stored. | §6 | TODO |
| DOC-003 | README explains frontend/backend layering criteria. | §6 | TODO |
| DOC-004 | README explains how `seed/seed.json` is loaded and persisted. | §6 | TODO |
| DOC-005 | README references actual implementation files/functions. | §6 | TODO |
| DOC-006 | README explains why important choices were made. | §6 | TODO |
| DOC-007 | README explains relevant trade-offs / what alternatives were not chosen. | §6 | TODO |
| DOC-008 | README explains tests written and their purpose if tests are included. | §6 | TODO |
| DOC-009 | README documents intentional UI/UX deviations or improvements if any. | §4, §6 | TODO |

---

## 14. Completion Rule

The assignment is release-ready only when:

- every mandatory requirement above is `VERIFIED`, or
- an item is explicitly non-mandatory/conditional and documented accordingly,
- and all release-gate scenarios in `docs/VERIFICATION.md` pass.
