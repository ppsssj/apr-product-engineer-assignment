# UI Specification

## 1. Scope and Source Priority

- Target: mobile web (`ASSIGNMENT_DEFINED`)
- Reference viewport: 375px (`ASSIGNMENT_DEFINED`)
- Desktop-specific adaptation: out of scope (`ASSIGNMENT_DEFINED`)
- Official design source: [Figma `assignment`](https://www.figma.com/design/Ee1gp5NC6G1sQIvssuu4Sr/assignment?node-id=0-1), Page 1 (`DESIGN_DEFINED`, `AVAILABLE`)
- Audit date: `2026-09-29`

`ASSIGNMENT.md` remains the source of truth for functional meaning and scope. Figma defines presentation and illustrates states, but does not add product requirements or override assignment behavior.

## 2. Evidence Taxonomy

- `ASSIGNMENT_DEFINED`: explicit requirement from `ASSIGNMENT.md` or its tracked requirement IDs.
- `DESIGN_DEFINED`: structure or visual value observed in the official Figma source.
- `PRODUCT_DECISION`: behavior needed to complete an unspecified product state without contradicting the assignment or design.
- `ARCHITECTURE_DECISION`: implementation direction approved in `docs/ARCHITECTURE.md`.

Repository assets such as Pretendard and the three avatars are recorded as repository facts, not as Figma evidence.

## 3. Evidence Status

| Source | Status | Notes |
| --- | --- | --- |
| `ASSIGNMENT.md` | `AVAILABLE` | Defines mobile scope, required flows, data fields, interactions, and APIs. |
| Repository starter | `AVAILABLE` | Provides App Router, immutable `/dev-user`, Pretendard, and three avatar assets. |
| Official Figma | `AVAILABLE` | File key `Ee1gp5NC6G1sQIvssuu4Sr`, Page 1 (`0:1`); 12 supplied top-level frames audited. |
| Local `design/` asset | `NOT_PRESENT` | The repository package lacks the `.fig` file, but the accessible official Figma URL is authoritative and removes the inspection blocker. |

Audit evidence was collected from the actual Figma node tree and rendered frame output. Exact values below are included only when the frame/node evidence exposed them.

## 4. Screen and State Inventory

All inspected top-level frames are `375 × 778` (`DESIGN_DEFINED`).

| Figma node | Figma name | Mapped UI/state | Evidence |
| --- | --- | --- | --- |
| `755:8831` | 메디큐브톡 | SCREEN-001 post list | `DESIGN_DEFINED` |
| `784:412` | 메디큐브톡(작성 완료) | SCREEN-001 + SCREEN-006 creation toast | `DESIGN_DEFINED` |
| `755:10420` | 메디큐브톡_상세(본인 글) | SCREEN-002 own post, unliked (`좋아요 42`) | `DESIGN_DEFINED` |
| `755:10774` | 메디큐브톡_상세(본인 글) | SCREEN-002 own post, liked (`좋아요 43`) | `DESIGN_DEFINED` |
| `830:139` | 메디큐브톡_상세(본인 글) | SCREEN-002 own post + SCREEN-005 action bottom sheet | `DESIGN_DEFINED` |
| `755:11707` | 메디큐브톡_상세(글 삭제) | Post-delete confirmation overlay | `DESIGN_DEFINED` |
| `755:12175` | 메디큐브톡_상세(댓글 삭제) | Comment-delete confirmation overlay | `DESIGN_DEFINED` |
| `755:12645` | 메디큐브톡_상세(타인 글) | SCREEN-002 other-user post, multiple comments + More | `DESIGN_DEFINED` |
| `755:13104` | 메디큐브톡_상세(타인 글) | SCREEN-002 other-user post, one comment | `DESIGN_DEFINED` |
| `769:142` | 메디큐브톡_상세(타인 글) | SCREEN-002 other-user post, no comments (`댓글 (0)`) | `DESIGN_DEFINED` |
| `755:14007` | 메디큐브톡 작성 | SCREEN-003 empty/invalid form, disabled submit | `DESIGN_DEFINED` |
| `755:14046` | 메디큐브톡 작성(내용O) | SCREEN-003 populated/valid form, enabled submit | `DESIGN_DEFINED` |

SCREEN-004 edit has no separate audited top-level mockup. Reusing SCREEN-003 with existing values prefilled remains `ASSIGNMENT_DEFINED`; the shared form structure makes reuse feasible but does not visually define a distinct edit state.

Loading, request-error, next-page loading, mutation-pending, and submission-error visuals are not present in the audited frames. Their behavior remains `PRODUCT_DECISION` and their visual treatment remains open for implementation review.

## 5. Navigation and User Flow

```text
Post List
  -> Post Detail
  -> Create Post

Post Detail (own post)
  -> Post Action Bottom Sheet
  -> Edit Post (reuse Create form with prefilled values)
  -> Delete Confirmation -> delete -> Post List

Post Detail (own comment, regardless of post owner)
  -> Comment Delete Confirmation -> delete -> updated detail

Create Post success -> Post List + completion toast
Edit Post success -> updated Post Detail
```

- Destinations and ownership behavior are `ASSIGNMENT_DEFINED`.
- The action sheet, confirmation overlays, enabled/disabled form states, and toast placement are `DESIGN_DEFINED`.
- Concrete URL paths remain a `PRODUCT_DECISION`.
- No login screen is introduced (`ASSIGNMENT_DEFINED`, `USER-004`).

## 6. Design-Defined Visual Foundation

### 6.1 Viewport, Regions, and Spacing

- Every audited top-level frame is `375 × 778`.
- Main horizontal content inset is 16px; the corresponding content width is 343px.
- The list header is 48px high. List content begins at y=56.
- Each list row is 118px high with a 343×93 inner content region at x=16, y=12 and a 343×1 divider.
- The floating write control is 50×50 at x=310, y=679.
- Detail frames use a 375×48 top navigation region, scroll content starting at y=56, and a fixed 375×86 comment-composer/safe-area region at y=692.
- The create form uses 16px horizontal insets and a fixed bottom action region: 375×68 action area plus a 375×26 safe-area region.
- Confirmation dialogs occupy x=24, y=257, width=327, height=264 over a full-screen overlay.
- The own-post action sheet occupies x=0, y=554, width=375, height=224 over a full-screen overlay.

These measurements are `DESIGN_DEFINED`; responsive behavior outside the 375px target is not defined by the audited frames.

### 6.2 Typography

Pretendard is the predominant family in the audited frames and is also provided by the repository.

| Role observed in Figma | Family / style | Size / line height | Color |
| --- | --- | --- | --- |
| App title | Pretendard Bold | 20 / 28px | `#292828` |
| List post title | Pretendard Medium | 16 / 24px | `#292828` |
| List preview | Pretendard Regular | 14 / 18px | `#979EA9` |
| List author/count/time | Pretendard Regular | 12 / 16px | `#5E5E5E` or `#979EA9` |
| Detail title | Pretendard Bold | 18 / 26px | `#292828` |
| Detail author | Pretendard Bold | 15 / 22px | `#292828` |
| Detail body | Pretendard Regular | 14 / 18px | `#292828` |
| Detail section/count labels | Pretendard Bold | 16 / 24px | `#292828` |
| Create label | Pretendard Bold | 14 / 22px | `#292828` |
| Create input/placeholder | Pretendard Regular | 14 / 18px | `#292828` / `#979EA9` |
| Toast | Pretendard Regular | 14 / 18px | `#FFFFFF` |

The expanded own-post frame `830:139` contains Apple SD Gothic Neo and mixed-font legacy layers while the equivalent primary states use Pretendard. Treating Apple SD Gothic Neo as a global token would be unsupported; the implementation should use the repository-provided Pretendard consistently (`PRODUCT_DECISION`) unless a later component-specific review proves otherwise.

### 6.3 Colors, Dividers, and Radii

Common exact solid colors observed in the audited nodes:

- surface: `#FFFFFF`
- primary text: `#292828`
- secondary text: `#979EA9`
- supporting text: `#5E5E5E`
- light dividers/borders: `#DDE0E4`, `#EAECEE`, `#DADEE3`
- disabled action surface: `#F3F4F8`
- liked-state accent present only in the liked detail variant: `#FF8DA1`
- modal/sheet scrim: black at 60% opacity
- toast surface: black at 70% opacity

Observed non-zero corner-radius sets are:

- list variants: 5px and 14px
- list toast variant additionally: 6px
- detail variants: 4px, 12px, and 20px
- overlay variants additionally: 8px
- create variants: 4px and 12px

The audit does not assign every radius to a semantic token where the node evidence did not make that relationship unambiguous.

### 6.4 Controls and Icons

- Top navigation uses a back control; own-post variants additionally expose the post action trigger (`DESIGN_DEFINED` + `ASSIGNMENT_DEFINED`).
- The list uses a red circular floating action with a white pencil icon (`DESIGN_DEFINED`).
- List metadata uses comment/like iconography; detail uses a distinct liked visual state (`DESIGN_DEFINED`).
- Delete confirmations use explicit cancel/confirm actions, while the own-post action sheet uses edit/delete rows with icons (`DESIGN_DEFINED`).
- The exact SVG path data was not needed for this audit and is not elevated to a design token.

## 7. Screen Specifications

### SCREEN-001 — Post List

#### Functional Contract

- Browse readable posts newest-first and enter detail/create flows (`ASSIGNMENT_DEFINED`).
- Use bounded cursor continuation for infinite scroll (`ARCHITECTURE_DECISION`).
- Render title, preview, author/avatar, like count, comment count, and relative time (`ASSIGNMENT_DEFINED`).

#### Design Reconciliation

- Figma confirms a compact vertical list with 118px rows, 16px page insets, 18px circular avatars, title/preview hierarchy, metadata icons, right-aligned relative time, dividers, and a bottom-right 50px write control (`DESIGN_DEFINED`).
- Figma illustrates populated content only. Empty/loading/error/end-of-list presentations remain `PRODUCT_DECISION`.
- Stable avatar mapping remains `stableHash(author) % 3` (`PRODUCT_DECISION`); the assignment permits any of the supplied assets and does not score avatar fidelity.

#### P4 Implementation Decisions

A fresh Figma MCP comparison of node `755:8831` was not possible during P4 (Starter-plan tool-call limit exhausted). P4 therefore implements the `DESIGN_DEFINED` values above as recorded and completes the rest with minimal `PRODUCT_DECISION` values. None of the values below is a Figma-exact claim.

| Detail | P4 choice | Evidence |
| --- | --- | --- |
| Divider color | existing `--color-border-light` (`#DDE0E4`) | Implementation choice among the audited light-border colors, not a Figma-exact mapping |
| Title | single line, ellipsis | `PRODUCT_DECISION` |
| Preview | up to 2 lines, ellipsis; 4px below the title | `PRODUCT_DECISION` |
| Metadata row | pinned to the bottom of the 343×93 region; avatar→name 4px, item gap 8px, icon→count 2px | `PRODUCT_DECISION` |
| Metadata colors | author `#5E5E5E`; comment/like counts and time `#979EA9` | Both colors are `DESIGN_DEFINED`; the assignment of each is `PRODUCT_DECISION` |
| Comment/like/pencil icons | inline SVG approximations (14px metadata, 24px pencil), `currentColor` | `PRODUCT_DECISION`; Figma paths not extracted |
| Header | sticky, left-aligned title at the 16px inset | `PRODUCT_DECISION` |
| FAB | fixed `right: 15px; bottom: 49px` (derived from x=310, y=679 in 375×778); red `#F5364F`, shadow `0 2px 8px rgba(0,0,0,.16)` kept local to the CSS Module | Geometry `DESIGN_DEFINED`; color and shadow `PRODUCT_DECISION`, not global tokens |
| Loading / empty / error / next-page loading / next-page error | centered secondary-color text; errors add a `다시 시도` button | `PRODUCT_DECISION` |

### SCREEN-002 — Post Detail

#### Functional Contract

- Read a post, toggle requester-specific like, and read/create/delete comments (`ASSIGNMENT_DEFINED`).
- Initially show at most two newest comments; show More only when total is at least three; begin comment infinite scroll only after More (`ASSIGNMENT_DEFINED` + `ARCHITECTURE_DECISION`).
- Show post edit/delete UI only for the post owner and comment deletion only for the comment owner (`ASSIGNMENT_DEFINED`).

#### Design Reconciliation

- Figma confirms a fixed top navigation, post title/author/date/body hierarchy, like/count control, comment heading/count, comment list, conditional `댓글 더보기`, and fixed bottom comment composer (`DESIGN_DEFINED`).
- Unliked/liked own-post variants change `좋아요 42` to `좋아요 43`; `#FF8DA1` appears in the liked variant and not the unliked variant (`DESIGN_DEFINED`).
- Other-user variants explicitly cover 0, 1, and many comments. The zero-comment state shows `댓글 (0)` and retains the composer; it does not add a separate empty-state message (`DESIGN_DEFINED`).
- The many-comments frame shows two comments and `댓글 더보기`, consistent with the assignment’s initial-two rule (`DESIGN_DEFINED` + `ASSIGNMENT_DEFINED`).
- An own-comment delete affordance appears even on an other-user post, confirming that comment ownership is independent of post ownership (`DESIGN_DEFINED`, consistent with `ASSIGNMENT_DEFINED`).

#### P5A Implementation Decisions

P5A implements detail read, like toggle, and comment read/continuation only. The comment composer and own-comment delete (P5B) and the own-post `⋯` menu, edit, and delete (P6) are intentionally not rendered yet; no non-functional placeholder controls were added. Values below were estimated from the local PNG exports of `755:10420`, `755:10774`, `755:12645`, `755:13104`, and `769:142` (375×778 at 3×). They are not Figma node measurements.

| Detail | P5A choice | Evidence |
| --- | --- | --- |
| Top navigation | sticky 375×48, 24px back arrow at x=16 (`뒤로 가기`); history back, or `/` on a direct load | Region `DESIGN_DEFINED`; behavior `PRODUCT_DECISION` |
| Post header | title at y≈80 (24px below content start); 36px avatar; author/date to its right with 10px gap | PNG-estimated `PRODUCT_DECISION` |
| Post date | `YY.MM.DD` in local time, 14/18 `#979EA9` | Format observed in PNG; size `PRODUCT_DECISION` |
| Body | 24px below byline, `white-space: pre-wrap` | Type `DESIGN_DEFINED`; spacing `PRODUCT_DECISION` |
| Like control | 24px heart + `좋아요 n` 14/18 `#979EA9`, 32px below body; outline `#979EA9` when unliked, filled `--color-liked` when liked | `#FF8DA1` liked accent `DESIGN_DEFINED`; icon drawing and sizes `PRODUCT_DECISION` |
| Section band | 8px, reuses `--color-disabled-surface` (`#F3F4F8`) | PNG-observed; token reuse `PRODUCT_DECISION` |
| Comment heading | `댓글 (n)` Bold 16/24 with `ko-KR` thousands separator, 48px row, full-width `--color-border-light` divider | Type `DESIGN_DEFINED`; row/divider `PRODUCT_DECISION` |
| Comment item | 30px avatar, author Bold 15/22 `#292828`, relative time 12/16 `#979EA9`, content Regular 15/22 `#292828` | PNG-estimated `PRODUCT_DECISION` (the PNG mixes relative and absolute comment dates; P5A uses relative time throughout) |
| More | `댓글 더보기` + chevron, 343×40, 1px `--color-border-light`, 12px radius, 14/18 `#979EA9` | Presence `DESIGN_DEFINED`; geometry PNG-estimated |
| Loading / not-found / error | centered secondary text; `목록으로` for not-found, `다시 시도` for errors; continuation errors below loaded comments | `PRODUCT_DECISION` |

### SCREEN-003 — Create Post

#### Functional Contract

- Title and content are required; title maximum is 20 characters; invalid forms cannot submit (`ASSIGNMENT_DEFINED`).
- Success returns to SCREEN-001 and presents SCREEN-006 (`ASSIGNMENT_DEFINED`).

#### Design Reconciliation

- The empty frame shows title/content placeholders and a disabled fixed-bottom `작성 완료` action (`DESIGN_DEFINED`).
- The populated frame shows entered title/content and an enabled fixed-bottom action (`DESIGN_DEFINED`).
- The title placeholder explicitly includes `(최대 20자)` (`DESIGN_DEFINED`, consistent with `ASSIGNMENT_DEFINED`).
- Submitting/error visual states are not shown and remain `PRODUCT_DECISION`.

### SCREEN-004 — Edit Post

- Reuses SCREEN-003 with existing title/content prefilled and identical validation (`ASSIGNMENT_DEFINED`).
- No separate edit frame or edit-specific visual token was found (`DESIGN_DEFINED` observation).
- Direct unauthorized access remains a backend concern (`ARCHITECTURE_DECISION`).

### SCREEN-005 — Post Action Bottom Sheet

- Frame `830:139` confirms a scrim and bottom sheet with `수정하기`, `삭제하기`, and cancel treatment (`DESIGN_DEFINED`).
- The sheet is shown only for an own post (`ASSIGNMENT_DEFINED`).
- Edit routes to SCREEN-004; delete first opens the post-delete confirmation (`DESIGN_DEFINED` flow detail, consistent with the assignment).
- Dismissal gesture and animation timing are not defined by the static frame and remain `PRODUCT_DECISION`.

### SCREEN-006 — Completion Toast

- Frame `784:412` shows `게시글이 등록되었습니다.` in a 181×40 toast at x=97.5, y=657 (`DESIGN_DEFINED`).
- It uses white 14/18px text on black at 70% opacity with a 6px observed radius (`DESIGN_DEFINED`).
- Trigger after successful creation is `ASSIGNMENT_DEFINED`; duration and motion remain `PRODUCT_DECISION`.

### Delete Confirmations

- Post delete: title `삭제하기`, body `삭제한 글은 복구할 수 없습니다. 정말 삭제 하시겠습니까?`, actions `취소` / `예` (`DESIGN_DEFINED`).
- Comment delete: the same structure with `삭제한 댓글은 복구할 수 없습니다.` (`DESIGN_DEFINED`).
- Confirmation UI is presentation state. It does not change the assignment’s soft-delete semantics (`ASSIGNMENT_DEFINED`).

## 8. Functional Reconciliation

### Matches Existing Functional Spec

- 375px mobile layout, post list/detail/create flows, own/other ownership variants, like toggle visuals, comment counts, initial-two + More behavior, own-post actions, create validation, delete confirmations, and creation toast all align with `ASSIGNMENT.md`.
- The audited design does not contradict the required soft-delete, requester, pagination, or validation semantics.

### Visual Behavior Newly Confirmed by Figma

- Exact 375×778 frame size and shared page regions.
- Own/other post variants and liked/unliked visual variants.
- Explicit 0-comment and 1-comment states.
- Bottom sheet before post deletion and centered confirmations for post/comment deletion.
- Fixed-bottom comment composer and create action.
- Toast copy, size, and placement.
- Disabled/enabled create action treatment.

### Previously Incorrectly Blocked

- Official design-source availability.
- Screen composition, spacing pattern, typography hierarchy, core colors, observed radii, floating action, sheet/dialog structure, toast placement, and disabled/enabled form states.

### Present in Figma but Outside Assignment Scope

- Comment editing (`수정`), replies (`답글쓰기`), reporting (`신고하기`), per-comment likes, `첫 댓글로 이동`, BEST/badge behavior, and popular-content sections.
- These elements do not create implementation requirements, database fields, or API endpoints.

### Required by Assignment but Not Explicitly Designed

- Loading, request error, pagination pending/end, and mutation failure states.
- Infinite-scroll mechanics and cursor representation.
- A distinct edit screen state with prefilled content.
- Unauthorized direct-API behavior and deleted-resource HTTP behavior.
- Responsive behavior outside the 375px target.

These remain `PRODUCT_DECISION` or `ARCHITECTURE_DECISION` as applicable.

## 9. P2 Backend Impact

The design audit introduces no new backend endpoint or response field.

- Own-post versus other-user actions reinforce requester-aware ownership and backend authorization (`ASSIGNMENT_DEFINED`). Existing `author` plus requester resolution is sufficient; no design-only `isOwner` field is required.
- Own-comment deletion on another user’s post reinforces independent comment ownership checks (`ASSIGNMENT_DEFINED`).
- Liked/unliked variants reinforce requester-specific `isLiked` and `likeCount`; the existing post and toggle contracts are sufficient.
- Comment states reinforce accurate `commentCount`, comments `total`, deleted-comment exclusion, and bounded newest-first pagination. Existing contracts plus the architecture-defined cursor are sufficient.
- Post/comment confirmation overlays are frontend state; DELETE contracts remain unchanged.
- Design-only replies, reports, comment edit, per-comment likes, and popular content must not be added in P2.

## 10. Loading, Empty, Error, and Accessibility Decisions

| Screen | Design evidence | Remaining decision |
| --- | --- | --- |
| SCREEN-001 | Populated list only | Loading, empty, error, next-page pending, end-of-list (`PRODUCT_DECISION`) |
| SCREEN-002 | 0/1/many comments, liked/unliked, delete overlays | Detail/comments error, More pending, mutation pending/failure (`PRODUCT_DECISION`) |
| SCREEN-003 | Empty-invalid and populated-valid | Submitting and submission-error presentation (`PRODUCT_DECISION`) |
| SCREEN-004 | Shared create structure only | Prefill loading/error and unauthorized presentation (`PRODUCT_DECISION`) |
| SCREEN-005 | Open sheet and delete confirmation | Dismissal motion and delete failure (`PRODUCT_DECISION`) |

Accessibility remains an implementation responsibility:

- semantic buttons and form controls;
- accessible names for icon-only actions;
- actual disabled state for invalid submit;
- keyboard-focusable controls and focus management for sheet/dialog states.

These are `PRODUCT_DECISION` and do not claim Figma evidence.

## 11. Traceability

SCREEN-001 through SCREEN-006 retain their existing functional meaning. Figma frame IDs above provide the design evidence for their visual states. `ASSIGNMENT_DEFINED` behavior prevails on conflict; `DESIGN_DEFINED` values guide presentation; unspecified product states remain `PRODUCT_DECISION`; API, state, pagination, and authorization mechanisms remain `ARCHITECTURE_DECISION`.
