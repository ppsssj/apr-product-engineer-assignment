# UI Specification

## 1. Scope

- Target: mobile web (`ASSIGNMENT_DEFINED`)
- Reference viewport: 375px (`ASSIGNMENT_DEFINED`)
- Desktop-specific adaptation: out of scope (`ASSIGNMENT_DEFINED`)
- Official design source: `MISSING`; `design/` is empty although `ASSIGNMENT.md` references `design/메디큐브톡.fig` (`BLOCKED`)

This specification defines functional structure and behavior only. Exact spacing, typography, color, radius, shadow, icon geometry, component height, and animation timing remain `BLOCKED` until an official Figma file or reference image is supplied.

## 2. Evidence Status

| Source | Status | Notes |
| --- | --- | --- |
| `ASSIGNMENT.md` | `AVAILABLE` | Defines mobile scope, required flows, data fields, interaction behavior, and APIs. |
| Repository starter | `AVAILABLE` | Provides App Router, immutable `/dev-user`, Pretendard, and three avatar assets. |
| Figma | `MISSING` | Expected file is absent from the empty `design/` directory. |
| Screenshots/reference images | `MISSING` | No image reference exists in the repository. |

## 3. Screen Inventory

| UI ID | Screen / state | Evidence | Visual status |
| --- | --- | --- |
| UI-001 | Post List | `ASSIGNMENT_DEFINED` | `BLOCKED` |
| UI-002 | Post Detail | `ASSIGNMENT_DEFINED` | `BLOCKED` |
| UI-003 | Create Post | `ASSIGNMENT_DEFINED` | `BLOCKED` |
| UI-004 | Edit Post | `ASSIGNMENT_DEFINED` | `BLOCKED` |
| UI-005 | Post Action Bottom Sheet | `ASSIGNMENT_DEFINED` | `BLOCKED` |
| UI-006 | Completion toast | `ASSIGNMENT_DEFINED` | `BLOCKED` |

Loading, empty, error, next-page loading, mutation-pending, and expanded-comment states are UI states, not separate routes (`PRODUCT_DECISION`).

## 4. Navigation / User Flow

```text
Post List
  -> Post Detail
  -> Create Post

Post Detail (own post)
  -> Post Action Bottom Sheet
  -> Edit Post
  -> Delete Post -> Post List

Create Post success -> Post List + completion toast
Edit Post success -> updated Post Detail
```

- Screen relationships and post-delete/create-success destinations are `ASSIGNMENT_DEFINED`.
- Concrete URL paths are not prescribed and remain `PRODUCT_DECISION` for the implementation phase.
- No login or authentication screen is introduced (`ASSIGNMENT_DEFINED`, `USER-004`). Requester selection remains the immutable `/dev-user` starter flow.

## 5. Shared Functional Rules

- Browser API requests that rely on the requester cookie use `credentials: "include"` (`ASSIGNMENT_DEFINED`, `START-003`).
- A resource whose deletion succeeds disappears from the user’s normal view; no deleted placeholder is introduced (`ASSIGNMENT_DEFINED`, `DEL-007`).
- `isLiked`, comment deletion actions, and post action menus are derived from the current requester; backend ownership enforcement remains authoritative (`ASSIGNMENT_DEFINED`, `USER-005`, `USER-006`, `API-034`).
- Remote list/detail/mutation state follows the decided TanStack Query strategy; local input, sheet, and toast state stays local (`REPOSITORY_DEFINED` via `docs/ARCHITECTURE.md`).
- Exact loading/error presentation is intentionally unspecified; only behavior is defined here (`PRODUCT_DECISION`).

## 6. Screen Specifications

### UI-001 — Post List

#### Purpose

Browse readable posts in newest-first order and enter detail or creation flows (`ASSIGNMENT_DEFINED`).

#### Requirement IDs

`UI-001`–`UI-003`, `UI-006`–`UI-007`, `LIST-001`–`LIST-006`, `DEL-003`, `DEL-007`, `API-001`, `API-011`–`API-021`, `API-031`–`API-032`.

#### Data Dependencies

- `GET /posts`, with `items`, `total`, and the architecture-defined cursor continuation (`ASSIGNMENT_DEFINED` + `REPOSITORY_DEFINED`).
- Every card requires `id`, `title`, `content`, `likeCount`, `commentCount`, `author`, and `createdAt` (`ASSIGNMENT_DEFINED`).

#### Structure

```text
PostList
  PostCard*
  NextPageSentinel
FloatingWriteAction
```

Only the functional hierarchy is defined; card layout and visual hierarchy are `BLOCKED`.

#### Interactions and States

- Selecting a post enters UI-002 (`ASSIGNMENT_DEFINED`).
- The write action enters UI-003 and is functionally positioned as the required floating/right-bottom action (`ASSIGNMENT_DEFINED`, `LIST-006`); its size, icon, and exact position are `BLOCKED`.
- Initial loading, success, empty, request failure, next-page pending, and end-of-list are required behavior states (`PRODUCT_DECISION`).
- A sentinel requests the next bounded cursor page only when a next cursor exists and another request is not pending (`REPOSITORY_DEFINED`). It must never fetch the complete collection in one request (`ASSIGNMENT_DEFINED`).

#### Content and Ownership

- Render relative time from `createdAt`, calculated against the current time (`ASSIGNMENT_DEFINED`, `LIST-001`–`LIST-002`).
- Render post author and one supplied avatar asset (`ASSIGNMENT_DEFINED`, `UI-006`).
- Stable avatar mapping is `stableHash(author) % 3` (`PRODUCT_DECISION`): the same author receives the same supplied avatar across screens. This favors recognizability over random variation.

### UI-002 — Post Detail

#### Purpose

Read one post, toggle its requester-specific like, and read/create/delete comments (`ASSIGNMENT_DEFINED`).

#### Requirement IDs

`DETAIL-001`–`DETAIL-013`, `DEL-004`–`DEL-007`, `USER-005`–`USER-006`, `API-003`, `API-006`–`API-009`, `API-022`–`API-035`.

#### Data Dependencies

- `GET /posts/{postId}` for post fields, `isLiked`, and `commentCount`.
- `GET /posts/{postId}/comments` for cursor-paginated newest-first comments.
- Like, comment-create, and comment-delete mutation endpoints (`ASSIGNMENT_DEFINED`).

#### Structure

```text
PostDetail
  PostContent
  LikeToggle
  CommentSummary
  InitialCommentList (at most 2)
  MoreCommentsAction (conditional)
  ExpandedCommentList + NextPageSentinel (after More)
  CommentComposer
  OwnPostActionMenuTrigger (conditional)
```

#### Interactions and States

- Like toggles `isLiked` and updates displayed `likeCount` (`ASSIGNMENT_DEFINED`, `DETAIL-001`–`DETAIL-003`).
- Comments are newest-first. Initially show at most two; show More only when total is at least three (`ASSIGNMENT_DEFINED`, `DETAIL-004`–`DETAIL-007`).
- Activating More enables cursor-based infinite continuation; it is not active before that action (`ASSIGNMENT_DEFINED`, `DETAIL-008`).
- Comment create updates the rendered comment state and count after success (`ASSIGNMENT_DEFINED`, `DETAIL-009`, `DEL-006`).
- Initial loading, detail-not-readable, comment loading, More pending, mutation pending, and mutation failure are functional states (`PRODUCT_DECISION`).

#### Ownership Behavior

- A comment deletion action is available only for a comment whose `author` equals the current requester; successful deletion removes it from this view (`ASSIGNMENT_DEFINED`, `DETAIL-010`–`DETAIL-011`).
- An own post shows the UI-005 trigger; another user’s post exposes no edit/delete UI (`ASSIGNMENT_DEFINED`, `DETAIL-012`–`DETAIL-013`, `DETAIL-019`–`DETAIL-020`).
- UI visibility is not the authorization boundary; the backend rejects unauthorized mutation attempts (`ASSIGNMENT_DEFINED`, `API-034`).

### UI-003 — Create Post

#### Purpose

Create a new post for the current requester (`ASSIGNMENT_DEFINED`).

#### Requirement IDs

`EDIT-001`–`EDIT-006`, `API-002`, `USER-003`.

#### Structure and Content

```text
PostForm
  TitleInput
  ContentInput
  SubmitAction
```

Title and content are required; title has a maximum length of 20 characters (`ASSIGNMENT_DEFINED`). Exact control styling, labels, and form geometry are `BLOCKED`.

#### Interactions and States

- The submit action is disabled when either required field is invalid (`ASSIGNMENT_DEFINED`, `EDIT-004`).
- Successful submission returns to UI-001 and displays UI-006 (`ASSIGNMENT_DEFINED`, `EDIT-005`–`EDIT-006`).
- Initial, invalid, submitting, submission-error, and success-navigation states are functional states (`PRODUCT_DECISION`).

### UI-004 — Edit Post

#### Purpose

Edit an own post with the same form structure and validation as UI-003 (`ASSIGNMENT_DEFINED`).

#### Requirement IDs

`DETAIL-014`–`DETAIL-017`, `DETAIL-019`, `EDIT-001`–`EDIT-004`, `API-004`, `API-029`.

#### Interactions and Ownership

- The form starts with the existing title and content (`ASSIGNMENT_DEFINED`, `DETAIL-015`).
- Save applies the same required/max-length validation and updates rendered post data after success (`ASSIGNMENT_DEFINED`, `DETAIL-016`–`DETAIL-017`).
- This screen is only reachable through own-post UI; direct unauthorized attempts remain a backend concern (`ASSIGNMENT_DEFINED`, `DETAIL-019`, `API-034`).

### UI-005 — Post Action Bottom Sheet

#### Purpose

Expose own-post edit and delete actions (`ASSIGNMENT_DEFINED`).

#### Requirement IDs

`DETAIL-012`–`DETAIL-013`, `DETAIL-018`, `DETAIL-020`, `API-005`.

#### Structure and Interactions

```text
PostActionBottomSheet
  EditAction -> UI-004
  DeleteAction -> delete mutation -> UI-001
```

- The menu is available only for an own post (`ASSIGNMENT_DEFINED`).
- Delete success navigates to UI-001 and the deleted post is absent from normal reads (`ASSIGNMENT_DEFINED`, `DETAIL-018`, `DEL-003`–`DEL-004`).
- Sheet geometry, overlay treatment, dismissal motion, and icons are `BLOCKED`.

### UI-006 — Completion Toast

#### Purpose

Give the required completion feedback after successful post creation (`ASSIGNMENT_DEFINED`, `EDIT-006`).

#### Visual Status

Presentation, location, color, duration, and motion are `BLOCKED`; only its creation-success trigger is defined.

## 7. Soft Delete UX

- A deleted post returns the user to UI-001 and no longer appears in lists or normal detail reads (`ASSIGNMENT_DEFINED`, `DEL-003`–`DEL-004`, `DETAIL-018`).
- A deleted comment disappears from UI-002 and no longer contributes to visible `commentCount` (`ASSIGNMENT_DEFINED`, `DEL-005`–`DEL-007`).
- Deleted-resource placeholders are not introduced (`PRODUCT_DECISION`), because the assignment requires behavior as if the resource no longer exists.

## 8. Loading / Empty / Error State Matrix

| Screen | Loading | Empty | Error | Mutation pending |
| --- | --- | --- | --- | --- |
| UI-001 | Initial bounded page request | No readable posts | List request failure | N/A |
| UI-002 | Detail or initial comments request | Zero comments is valid | Detail/comments request failure or unreadable post | Like/comment/create/delete request pending |
| UI-003 | N/A | Empty form is initial state | Create failure | Submit pending |
| UI-004 | Existing post prefill request | N/A | Prefill/update failure or unauthorized post | Save pending |
| UI-005 | N/A | N/A | Delete failure | Delete pending |

The behavioral states above are `PRODUCT_DECISION`; their visual treatment is `BLOCKED`.

## 9. Design Tokens and Accessibility

### Design Tokens

- Font availability: Pretendard Variable is `REPOSITORY_DEFINED` by `frontend/app/globals.css` and `frontend/public/fonts/`.
- Exact type scale, colors, spacing, radius, shadow, icon dimensions, and animation values: `BLOCKED`.

### Accessibility Baseline

- Actions use semantic `button` elements (`PRODUCT_DECISION`).
- Form controls have an accessible name/label (`PRODUCT_DECISION`).
- Disabled submit uses the actual `disabled` state (`PRODUCT_DECISION`).
- Icon-only controls have an accessible name and interactive controls remain keyboard-focusable (`PRODUCT_DECISION`).

## 10. Open UI Decisions

The following cannot be closed without the missing design source:

- exact spacing, type scale, weights, colors, radii, shadows, and component dimensions;
- card/header/bottom-sheet/toast composition;
- icon appearance and disabled/pressed visual states;
- loading, empty, and error visual treatments;
- animation and transition timing.

## 11. Traceability

The screen sections link every required interaction to `docs/REQUIREMENTS.md` IDs. No additional route, visual token, or product behavior is presented as assignment-defined. Architecture-defined behavior is explicitly marked as `REPOSITORY_DEFINED`; stable avatar mapping and non-visual state handling are marked as `PRODUCT_DECISION`.
