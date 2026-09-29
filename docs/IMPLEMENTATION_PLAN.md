# Implementation Plan

This document is the operational state for the assignment. Keep it concise and current.

## Current State

- Current Phase: `P2 — Backend Domain & API`
- Status: `IN_PROGRESS`
- Last Verified Commit: `7df1ce3` (P1 and Figma audit merged); P2A read API acceptance completed on `2026-09-29`.
- Last Updated: `2026-09-29`

### Known Blockers

- No blocker remains for P2A review.
- P2B mutation work is intentionally deferred and is not a blocker for the scoped P2A PR.

### Next Action

- Review the P2A PR only.
- Do not start P2B until explicitly requested.

### P0 Completion Update

- Completed static audit of the assignment/instruction documents, non-generated repository tree, backend/frontend manifests and sources, Compose configuration, public assets, and valid seed JSON.
- Recorded verified starter contracts, seed facts, missing design assets, and ADR reconciliations in `docs/ARCHITECTURE.md`.
- P0 exit criteria are satisfied: no product code, dependency, package installation, schema, compose behavior, seed data, README, or immutable `/dev-user` change was made.
- Historical record: P0 is complete. Its audit state is not the current phase.

### Architecture Review Status

- Status: `COMPLETE`
- Final decisions are recorded in `docs/ARCHITECTURE.md`: Spring JDBC/JdbcClient, Flyway plus `ApplicationRunner` seed lifecycle, identity IDs, boolean soft delete, base-count likes, keyset pagination, TanStack Query v5, focused client boundaries, CSS Modules, IntersectionObserver, native relative time, and backend-authoritative validation/ownership.
- No high-impact architecture decision remains unresolved. Runtime verification of those decisions is deferred to their implementation phases.
- Historical record: Architecture Review is complete. Its decision gate is not active work.

### UI Specification Status

- Status: `COMPLETE_FUNCTIONAL`, `DESIGN_AUDITED`
- Official source: Figma file `Ee1gp5NC6G1sQIvssuu4Sr`, Page 1 (`0:1`), with 12 identified 375×778 frames inspected.
- `docs/UI_SPEC.md` reconciles assignment behavior with the actual list, toast, own/other detail, liked/unliked, comment-count, action-sheet, delete-confirmation, and create-form states.
- Design-only out-of-scope elements do not expand the assignment or P2 API contract. Loading/error visuals remain product decisions.
- P1 remains complete; P2A is complete and P2 remains in progress until P2B is implemented and verified.

### Figma Design Audit Status

- Status: `COMPLETE`
- The official Figma source is `AVAILABLE`; the repository-local `.fig` file remains absent but no longer blocks design inspection.
- No backend, frontend, schema, dependency, Compose, seed, or assignment-source change is part of this audit.
- P2-impact review found no new endpoint or response-field requirement.

### P2A Status

- Status: `COMPLETE`
- Implemented only `GET /api/posts`, `GET /api/posts/{postId}`, and `GET /api/posts/{postId}/comments`, plus requester resolution, strict cursor pagination, read DTO/error contracts, and focused tests.
- Real PostgreSQL acceptance covered all 39 readable posts, all readable-comment pages, seed-deleted resource exclusion, requester-specific `isLiked`, and invalid pagination inputs.
- P2B Status: `NOT_STARTED`; no mutation endpoint, mutation validation, or ownership mutation enforcement is included.

---

## Phase Rules

For each phase:

1. Inspect relevant existing code and requirements.
2. Confirm unresolved architecture decisions.
3. Implement only the current phase.
4. Run relevant automated checks.
5. Run relevant acceptance scenarios in `VERIFICATION.md`.
6. Update requirement statuses with evidence.
7. Record changed files, risks, and remaining work.
8. Stop before the next phase unless explicitly instructed to continue.

A phase is complete only when its exit criteria are satisfied.

---

## P0 — Repository Audit

### Goal

Understand the starter repository and map assignment requirements to the existing codebase without changing product behavior.

### Tasks

- Inspect `docker-compose.yml`.
- Inspect backend build/configuration.
- Inspect existing `/health` implementation.
- Inspect existing CORS configuration.
- Inspect frontend App Router structure.
- Confirm `frontend/app/dev-user/` contents and treat it as immutable.
- Inspect `seed/seed.json` structure and data types.
- Inspect provided public assets and fonts.
- Confirm whether the official Figma asset exists.
- Map existing starter behavior to requirement IDs.
- Identify architecture decisions required before P1/P2/P3.

### Exit Criteria

- Starter contracts are documented.
- Seed shape is understood.
- No mandatory provided behavior has been accidentally modified.
- Missing assets/blockers are recorded.
- Required architecture decisions are listed.

### Status

`COMPLETE`

---

## Architecture Decision Gate — before P1/P3

### Goal

Convert architecture research into repository-verified decisions before implementation.

### Evidence Standard

For each high-impact choice:

1. cite the assignment/starter fact that creates the constraint;
2. cite first-party technical documentation when framework/database behavior matters;
3. record the selected option;
4. record at least one realistic alternative;
5. record why the selected option fits this assignment;
6. record the trade-off being accepted;
7. mark unresolved repository-dependent claims `OPEN` or `PROVISIONAL`.

### Current Record

See `docs/ARCHITECTURE.md`.

### Exit Criteria

- Persistence access decision is repository-compatible.
- Migration/schema strategy is repository-compatible.
- Seed initialization lifecycle is resolved.
- Pagination contract is fixed.
- Frontend server-state strategy is fixed.
- Styling strategy is compatible with the starter.
- Every decision has traceable evidence and explicit trade-offs.

### Status

`COMPLETE`

---

## P1 — Database & Seed Foundation

### Goal

Create a durable persistence model and idempotent seed initialization.

### Requirement Focus

- `SEED-*`
- `DEL-*`
- relevant `USER-*`
- relevant `ENV-*`

### Tasks

- Finalize DB schema.
- Implement post persistence.
- Implement comment persistence.
- Implement like persistence/state model.
- Implement soft-delete representation.
- Implement seed import.
- Preserve seed IDs.
- Apply `deletedPostIds` / `deletedCommentIds`.
- Ensure restart idempotency.
- Ensure deleted comments do not affect counts.

### Exit Criteria

- Database starts successfully.
- Seed records are queryable.
- Seed IDs are preserved.
- Repeated startup does not duplicate data.
- Deleted seed entities remain persisted but excluded from normal reads.

### Status

`COMPLETE`

---

## P2 — Backend Domain & API

### Goal

Implement the assignment API with ownership, validation, pagination, and consistent deleted-resource behavior.

### Requirement Focus

- `USER-*`
- `DEL-*`
- `API-*`
- relevant `EDIT-*`

### Tasks

- Resolve requester from header/cookie/default.
- Implement post list/detail/create/update/delete.
- Implement comment list/create/delete.
- Implement like toggle.
- Implement ownership enforcement.
- Implement input validation.
- Implement pagination.
- Return required JSON shapes.
- Handle deleted/missing resources consistently.
- Add backend tests for critical business rules.

### Exit Criteria

- All mandatory API routes exist.
- Response contracts match the assignment.
- Ownership restrictions are backend-enforced.
- Pagination does not fetch the entire collection.
- Deleted resources behave as absent.

### Status

`IN_PROGRESS`

---

## P3 — Frontend Foundation

### Goal

Establish a simple frontend structure for API communication, server state, routing, and shared UI behavior.

### Requirement Focus

- `START-*`
- `UI-*`
- architecture decisions for state/styling

### Tasks

- Finalize server-state strategy.
- Finalize styling strategy.
- Implement API client with credentialed requests where required.
- Define shared response/request types.
- Define route structure.
- Define reusable UI primitives only where justified.
- Add relative-time utility.
- Preserve `/dev-user` unchanged.

### Exit Criteria

- API calls can resolve the current user through the provided cookie flow.
- Shared request/response handling is stable.
- No product screen relies on duplicated API logic.

### Status

`NOT_STARTED`

---

## P4 — Post List

### Goal

Implement the 375px post-list experience with newest-first infinite scrolling.

### Requirement Focus

- `LIST-*`
- relevant `UI-*`
- relevant `API-001`, `API-031`, `API-032`

### Tasks

- Render post cards.
- Render relative timestamps.
- Implement newest-first paginated loading.
- Implement infinite scroll.
- Implement write navigation.
- Implement loading/empty/error states.
- Align with official design when available.

### Exit Criteria

- Initial page renders correctly.
- Additional pages load only when needed.
- Duplicate items are not introduced during pagination.
- Write action navigates correctly.

### Status

`NOT_STARTED`

---

## P5 — Post Detail, Comments & Likes

### Goal

Implement post detail, requester-specific likes, initial-two-comments behavior, and comment continuation flow.

### Requirement Focus

- `DETAIL-001` through `DETAIL-011`
- relevant `API-*`

### Tasks

- Render post detail.
- Implement like toggle.
- Load at most 2 comments initially.
- Show “More” only when total >= 3.
- After “More”, continue via infinite scroll.
- Implement comment creation.
- Implement own-comment deletion.
- Keep count/list states synchronized after mutations.

### Exit Criteria

- Like state/count remain consistent after toggle.
- Initial comment limit is respected.
- “More” visibility logic is correct.
- Infinite scroll begins only after “More”.
- Deleting a comment updates both list and `commentCount`.

### Status

`NOT_STARTED`

---

## P6 — Post Create / Edit / Delete

### Goal

Implement post authoring and ownership-controlled post management.

### Requirement Focus

- `DETAIL-012` through `DETAIL-020`
- `EDIT-*`
- relevant `API-*`

### Tasks

- Implement create screen.
- Implement required-field validation.
- Enforce title max length 20.
- Disable completion action when invalid.
- Navigate to list after successful creation.
- Show completion toast.
- Reuse create layout/flow for edit.
- Prefill existing title/content for edit.
- Implement own-post delete with navigation back to list.
- Ensure other users cannot edit/delete through UI or API.

### Exit Criteria

- Create/edit validation matches requirements.
- Successful create shows toast and returns to list.
- Edit prefill/save works.
- Delete returns to list.
- Other-user mutation attempts fail.

### Status

`NOT_STARTED`

---

## P7 — Visual Integration & UX Polish

### Goal

Align the implemented flows with the official design and resolve UX edge cases without changing assignment semantics.

### Requirement Focus

- `UI-*`
- final visual aspects of `LIST-*`, `DETAIL-*`, `EDIT-*`

### Tasks

- Apply official spacing, typography, colors, radii, icons, sheets, and disabled states from the design source.
- Verify 375px viewport.
- Verify bottom-sheet/menu interactions.
- Verify toast placement/behavior.
- Verify loading, empty, and error states.
- Document intentional design deviations/improvements.

### Exit Criteria

- Mandatory flows are visually consistent with official design.
- No functional regression introduced by visual work.
- Any intentional deviation is documented.

### Status

`NOT_STARTED`

---

## P8 — Full Integration & Acceptance Verification

### Goal

Verify the application the way an evaluator is likely to run it.

### Requirement Focus

- all mandatory IDs
- all release gates

### Tasks

- Clean-start with `docker compose up`.
- Verify `/health`.
- Verify frontend reachability.
- Verify alternate port overrides.
- Verify seed idempotency after restart.
- Verify all ownership rules.
- Verify soft delete.
- Verify pagination/infinite scroll.
- Verify `isLiked` per requester.
- Run automated test suites.
- Run final acceptance scenarios.

### Exit Criteria

- All mandatory requirements are `VERIFIED`.
- All release-gate scenarios pass.
- No known blocker remains for submission.

### Status

`NOT_STARTED`

---

## P9 — README & Submission PR

### Goal

Produce evaluator-facing documentation based on the actual implementation and complete the repository submission workflow.

### Requirement Focus

- `DOC-*`
- `SUB-*`

### Tasks

- Document actual technical choices.
- Document server-state design.
- Document frontend/backend layering.
- Document seed strategy.
- Document tests and why they matter.
- Reference actual files/functions.
- Record alternatives and trade-offs.
- Record intentional UX improvements/deviations.
- Create final PR.
- Merge to `main`.
- Confirm collaborator/private repository settings.

### Exit Criteria

- README matches implementation reality.
- Final PR is merged.
- Repository is ready to submit.

### Status

`NOT_STARTED`
