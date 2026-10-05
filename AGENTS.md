# Agent Instructions

## Mission

Implement the APR Product Engineer assignment faithfully, with correctness, maintainability, and verifiable behavior.

The goal is not to maximize abstraction or add speculative features. The goal is to satisfy the assignment requirements with a simple, explainable implementation whose behavior can be verified.

## Source of Truth and Document Roles

The assignment and provided contracts have the following authority:

1. `ASSIGNMENT.md` is the final source of truth for functional scope and assignment requirements.
2. Existing starter repository behavior and immutable starter contracts are provided contracts.
3. Official Figma/design assets are presentation and design evidence.
4. `seed/seed.json` is the source for provided data.

Current repository documents have distinct roles:

- `docs/REQUIREMENTS.md` — current requirement status (`요구사항 추적 매트릭스`)
- `docs/ARCHITECTURE.md` — current architecture and ADRs (`아키텍처 결정 기록`)
- `docs/UI_SPEC.md` — UI, design, and product contract (`UI 명세`)
- `docs/VERIFICATION.md` — executed verification evidence (`인수 검증 기록`)
- `docs/PERFORMANCE.md` — raw benchmark and performance measurement evidence (`성능 감사 및 측정 기록`)
- `docs/DESIGN_COMPARISON.md` — final official Figma-to-runtime comparison (`공식 Figma 최종 비교`)
- `docs/ENGINEERING_NOTES.md` — decision and judgment history (`엔지니어링 노트`)
- `docs/IMPLEMENTATION_PLAN.md` — historical phase and implementation history only (`구현 계획`); it is not the authority for the current task or next action.

Agent assumptions do not override any of these sources or contracts.

Never weaken, reinterpret, or remove an assignment requirement for implementation convenience.

## Non-Negotiable Constraints

- Do not modify `frontend/app/dev-user/`.
- Preserve the `GET /health` path and its ready-state HTTP 200 contract. Current readiness behavior may return HTTP 503 with `starting` before the seed transaction commits.
- Preserve all IDs provided by `seed/seed.json`.
- Seed initialization must be idempotent across application restarts.
- Posts and comments must use soft delete. Never physically delete the row for normal product deletion flows.
- Deleted posts/comments must behave as absent from API responses and UI.
- Deleted comments must not contribute to `commentCount`.
- Resolve the current user from `x-user-id` header or cookie; default to `apr_tester` if neither exists.
- Do not implement a login flow.
- Do not introduce a user table unless a newly discovered hard requirement makes it necessary.
- Do not implement comment editing.
- Do not load an entire collection when pagination is required.
- `docker compose up` must start the complete application.
- Port overrides through `FRONTEND_PORT`, `BACKEND_PORT`, and `DB_PORT` must remain functional.
- API paths and required response shapes defined in `ASSIGNMENT.md` are contracts.

## Scope Discipline

- Implement only requirements that are explicit in `ASSIGNMENT.md`, required by the official design, or necessary to make those requirements work correctly.
- Do not add speculative product features.
- Do not replace provided starter behavior unless necessary.
- Prefer the smallest architecture that satisfies the assignment.
- Avoid premature abstraction and framework-heavy solutions that make the implementation harder to explain.

## Evidence Discipline

- High-impact architecture choices must be recorded in `docs/ARCHITECTURE.md`.
- Separate assignment/starter facts, official documentation, and engineering judgment.
- Prefer first-party documentation links over blogs or unsourced claims.
- Record alternatives considered and the trade-off accepted.
- Do not turn an unverified repository fact into an assumption. Mark it `OPEN` or `PROVISIONAL` instead.
- If implementation evidence contradicts an architecture decision, update the decision before continuing.

## Working Protocol

Before editing:

1. Read the relevant requirement IDs in `docs/REQUIREMENTS.md`.
2. Inspect the existing implementation and starter contracts related to the task.
3. Read the relevant current authority documents, such as `docs/ARCHITECTURE.md`, `docs/UI_SPEC.md`, or `docs/VERIFICATION.md`.
4. Confirm the scope of the focused task requested by the user.
5. Identify any unresolved high-impact decision that would block correct implementation.
6. If blocked by an unresolved decision, stop and report the decision point instead of silently choosing a high-impact architecture.

Consult `docs/IMPLEMENTATION_PLAN.md` only when the historical phase sequence, context, or evolution is relevant. It does not determine the current task.

During implementation:

- Work only within the user-approved focused task and PR scope unless explicitly instructed otherwise.
- Keep domain/business rules outside presentation-only code.
- Keep persistence details out of controllers and UI components.
- Validate permissions and ownership on the backend even if the frontend hides the action.
- Treat deleted resources consistently across list, detail, count, mutation, and pagination behavior.
- Keep API behavior deterministic and testable.
- Reuse existing starter infrastructure where possible.
- Do not silently change requirements to match an easier implementation.

After implementation:

1. Run the relevant automated checks.
2. Run the relevant acceptance checks from `docs/VERIFICATION.md`.
3. Update only the current documents actually affected by the work, and only when needed:
   - requirement status changes → `docs/REQUIREMENTS.md`
   - new executed verification evidence → `docs/VERIFICATION.md`
   - architecture decision changes → `docs/ARCHITECTURE.md`
   - UI contract changes → `docs/UI_SPEC.md`
   - performance measurements → `docs/PERFORMANCE.md`
4. Do not update `docs/IMPLEMENTATION_PLAN.md` to track current work; it is a historical record.
5. Report changed files.
6. Report verification evidence.
7. Report remaining risks, blockers, or assumptions.

Never report `PASS` or `VERIFIED` without executing the associated verification.

## Requirement Status Rules

Use these meanings in `docs/REQUIREMENTS.md`:

- `TODO`: not implemented.
- `IMPLEMENTED`: code exists but acceptance behavior has not yet been verified.
- `VERIFIED`: behavior has been verified with evidence.
- `BLOCKED`: cannot be completed because required input or a decision is missing.

Do not use `VERIFIED` as a synonym for “looks correct”.

## Architectural Decisions

High-impact technical choices must be documented in `docs/ARCHITECTURE.md` when that file is introduced.

Examples:

- ORM / persistence approach
- pagination strategy
- server-state strategy
- styling approach
- database schema choices
- soft-delete representation
- seed initialization strategy

For each significant decision, record:

- decision
- alternatives considered
- rationale
- trade-offs

Do not invent retrospective rationale after the implementation unless the reasoning actually reflects the chosen design.

## UI Rules

- Target viewport: 375px mobile web.
- Use the official Figma/design asset as the visual source of truth when available.
- If the official design asset is missing, do not fabricate claims of pixel-perfect fidelity.
- Functional work that does not depend on exact visual values may continue.
- Any intentional UI/UX deviation from the provided design must be documented for the final README.

## Final Release Gate

Determine engineering/repository readiness from the current status and evidence in `docs/REQUIREMENTS.md` and `docs/VERIFICATION.md`; do not hard-code status counts here.

Before the engineering/repository work is considered complete:

- All mandatory engineering/repository requirement IDs identified by `docs/REQUIREMENTS.md` must be `VERIFIED`.
- `docker compose up` must boot the full stack.
- `GET /health` must return 200 in the ready state.
- Frontend and backend must work with non-default port overrides.
- Seed initialization must remain idempotent after restart.
- Deleted resources must remain excluded consistently.
- Ownership restrictions must be verified from API behavior.
- Infinite-scroll flows must not fetch the full collection in one request.
- The final README must describe actual implementation decisions and trade-offs using real files/functions.

Claim full assignment submission completion only after the external `SUB` steps in `docs/REQUIREMENTS.md` are also complete. Do not perform external actions such as adding a collaborator or sending submission email without explicit user instruction.

## Git / Branch / Commit / PR Workflow

All repository changes must follow this workflow unless the user explicitly instructs otherwise.

### Pre-Change Git Check

Before changing tracked files:

1. Inspect the current branch.
2. Inspect `git status`.
3. Confirm the working tree does not contain unrelated user changes that could be overwritten or accidentally committed.
4. Confirm the current work belongs to the user-approved focused task and PR scope.

Never discard, reset, overwrite, or amend user-authored work merely to make the working tree clean.

### Branch Rules

- Never perform assignment work directly on `main`.
- Create or reuse a dedicated branch for each coherent focused task.
- Keep one branch and PR focused on one coherent scope.
- Do not mix unrelated follow-up work into the same branch or PR.
- If an existing branch already represents the current task, reuse it instead of creating a duplicate branch.
- After a task is merged, use a new branch for a new focused task.

Recommended branch naming:

```text
feat/<short-description>
fix/<short-description>
docs/<short-description>
test/<short-description>
refactor/<short-description>
chore/<short-description>
```

Branch naming is descriptive guidance, not a reason to rename a valid existing branch unnecessarily.

### Git Naming Autonomy

- Unless the user explicitly provides a branch name, choose an appropriate branch name automatically from the focused task.
- Unless the user explicitly provides a commit message, write an appropriate commit message automatically.
- Unless the user explicitly provides a PR title or description, generate them automatically from the completed work and verification evidence.
- Do not ask the user to choose routine Git names or wording when they can be derived safely from the task.
- Branch names should remain concise ASCII/English slugs for tooling compatibility.
- Commit message summaries, PR titles, and PR descriptions must be written in Korean so the user can review them quickly.
- Conventional commit type prefixes such as `feat`, `fix`, `docs`, `test`, `refactor`, and `chore` may remain in English.
- Merge remains approval-gated and requires explicit user authorization.

### Commit Rules

- Commit only logically coherent work.
- Prefer a small number of meaningful commits over many checkpoint commits.
- Do not create meaningless messages such as `update`, `fix`, `wip`, or `changes`.
- Do not amend or rewrite commits that have already been shared unless explicitly instructed.
- Never include unrelated files in a commit.
- Never commit secrets, credentials, tokens, private keys, local environment overrides, or generated artifacts that do not belong in source control.
- Write the commit summary in Korean for user review.

Preferred commit format:

```text
<type>: <한국어 요약>
```

Preferred types:

- `feat`
- `fix`
- `docs`
- `test`
- `refactor`
- `chore`

Examples:

```text
docs: 스타터 저장소 아키텍처 감사 결과 정리
feat: 멱등한 시드 초기화 구현
feat: 게시글 커서 페이지네이션 구현
test: 소프트 삭제 동작 검증 추가
fix: 삭제된 댓글을 댓글 수에서 제외
```

Before committing:

1. Review the staged diff.
2. Confirm it matches the intended scope.
3. Run the relevant checks for the current focused task.
4. Confirm no secrets or local-only files are staged.
5. Record any known verification gap instead of hiding it.

### Push Rules

- Push only the current work branch.
- Never push assignment changes directly to `main`.
- Never force-push unless explicitly instructed.
- After pushing, confirm the remote branch exists and the pushed commit matches local `HEAD`.
- If push fails, report the actual failure instead of claiming the branch is published.

### Pull Request Rules

Open a PR only when the scoped task is reviewable.

A PR should normally be created when:

- the intended branch scope is complete enough to review,
- relevant checks have been run,
- known blockers are documented,
- the diff does not contain unrelated work.

PRs must target `main` unless the user explicitly specifies another base branch.

PR title should describe the delivered scope clearly and must be written in Korean.

PR descriptions must also be written in Korean. Keep file names, code identifiers, commands, library names, and requirement IDs in their original form when that improves precision.

Recommended PR body:

```md
## 요약

이 PR에서 변경한 내용과 변경 이유를 간결하게 설명합니다.

## 범위

이번 PR에 포함된 작업과 의도적으로 제외한 작업을 구분합니다.

## 주요 결정

리뷰에 필요한 아키텍처 또는 구현 결정과 그 근거를 정리합니다.

## 검증

실제로 실행한 명령, 테스트, acceptance scenario와 결과만 기록합니다.

## 리스크 / 후속 작업

알려진 제한, blocker, 후속 작업으로 미룬 내용을 기록합니다.
```

Example PR title:

```text
P0 스타터 저장소 감사 및 아키텍처 근거 정리
```

Do not claim checks passed unless they were actually executed or confirmed by CI.

### CI / Review Rules

After opening a PR:

1. Inspect CI/check status when available.
2. If checks fail, investigate the actual failing job before changing code.
3. Keep fixes scoped to the PR's intended purpose.
4. Re-run or confirm relevant checks after the fix.
5. Do not hide unresolved review comments or known failures.

A passing local test does not imply CI passed, and a passing CI check does not replace assignment-specific acceptance verification.

### Merge Rules

- Do not merge a PR immediately after opening it.
- Merge only when the user explicitly instructs or approves the merge for that PR/workflow.
- Before merge, confirm as applicable:
  - required CI/checks are passing,
  - the PR diff matches intended scope,
  - blocking review comments are resolved,
  - required verification has been recorded,
  - no secrets or unintended generated artifacts are present.
- Do not bypass failing required checks merely to finish a task.

After merge:

1. Confirm the PR is actually merged into `main`.
2. Record or report the PR number when available.
3. Record or report the resulting merged commit/SHA when available.
4. Update local repository state only when needed for the next task.
5. Stop before starting a new follow-up task unless the user explicitly instructs continuation.
6. Use a new branch for the next focused task.

### Focused Task Boundary Rule

Typical focused-task flow:

```text
inspect current state
    ↓
confirm focused task
    ↓
create or reuse task branch
    ↓
implement only the task scope
    ↓
verify
    ↓
review diff
    ↓
commit
    ↓
push
    ↓
open PR
    ↓
CI / review
    ↓
user-approved merge
    ↓
confirm merge
    ↓
stop
```

Do not silently chain unrelated focused tasks into one branch, PR, or merge unless explicitly instructed.
