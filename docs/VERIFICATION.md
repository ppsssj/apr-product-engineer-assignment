# Acceptance Verification

This document defines executable acceptance scenarios for the APR assignment.

Do not mark a requirement `VERIFIED` only because the code appears correct. Run the relevant scenario and record evidence.

## Result Values

- `NOT_RUN`
- `PASS`
- `FAIL`
- `BLOCKED`

---

# Release Gates

The assignment is not release-ready unless every gate below passes.

## V-001 — Full Stack Boot

**Requirements:** `ENV-006`, `ENV-007`, `ENV-008`

### Steps

1. Stop previous assignment containers.
2. Start with:

```bash
docker compose up --build
```

3. Verify backend health:

```bash
curl -i http://localhost:8080/health
```

4. Open frontend at `http://localhost:3000`.

### Expected

- DB starts.
- Backend starts.
- Frontend starts.
- `/health` responds HTTP 200.
- Frontend is reachable.

### Execution Evidence — 2026-09-29

- Clean-volume `docker compose up --build` completed successfully.
- `db`, `backend`, and `frontend` reached `healthy`.
- `GET /health` returned HTTP 200.
- Flyway migration `V1` completed successfully.

**Result:** `PASS`

---

## V-002 — Alternate Port Boot

**Requirements:** `ENV-009`, `ENV-010`, `ENV-011`

### Steps

Start with non-default ports, for example:

```bash
FRONTEND_PORT=13000 BACKEND_PORT=18080 DB_PORT=25432 docker compose up --build
```

Verify:

```bash
curl -i http://localhost:18080/health
```

Open frontend at `http://localhost:13000`.

### Expected

- All services start successfully on the supplied ports.
- Frontend communicates with backend successfully.

**Result:** `NOT_RUN`

---

## V-003 — Seed Idempotency

**Requirements:** `SEED-001` through `SEED-012`

### Steps

1. Start from a clean DB volume.
2. Start application and record post/comment row counts.
3. Stop application without deleting DB volume.
4. Start application again.
5. Record row counts again.
6. Before any like toggle, confirm `post_likes` is empty, so no requester has an initial like relation.
7. Parse `seed/seed.json` as UTF-8 with Node.js and compare every seeded post’s `likeCount` with `posts.base_like_count`.
8. Compare the full deleted post/comment ID sets and every post's non-deleted comment count between the parsed seed and the database.

### Expected

- Seed rows exist after first startup.
- IDs match seed IDs.
- Already-deleted seed IDs remain persisted as deleted.
- Second startup does not duplicate seed rows.
- Readable totals remain stable.
- Every requester begins with `isLiked: false` for seeded posts.
- Every seeded post begins with its supplied `likeCount`.
- Database deleted-ID sets exactly match `deletedPostIds` and `deletedCommentIds`.
- Per-post readable comment counts exclude every ID in `deletedCommentIds`.

### Execution Evidence — 2026-09-29

- Initial clean-volume state: 42 posts, 2,076 comments, 3 deleted posts, 81 deleted comments, 0 `post_likes`, and one `seed_history` row for `assignment-v1`.
- Preserved ranges: post IDs `1..42`; comment IDs `1..2076`.
- Generated identity checks produced post ID `43` and comment ID `2077`, without conflicting with preserved seed IDs.
- Persistent-volume backend restart retained 42 posts, 2,076 comments, 0 `post_likes`, and one `assignment-v1` seed-history row.
- A modified existing seed row retained its modified value across the backend restart, proving startup did not overwrite existing data; the value was restored after verification.
- Node.js parsed the actual `seed/seed.json` as UTF-8 (`JSON.parse`: 42 posts, 2,076 comments). It compared complete, ordered PostgreSQL results rather than samples:
  - `seed.deletedPostIds` exactly equaled `SELECT id FROM posts WHERE is_deleted = TRUE ORDER BY id` (3 of 3 IDs).
  - `seed.deletedCommentIds` exactly equaled `SELECT id FROM comments WHERE is_deleted = TRUE ORDER BY id` (81 of 81 IDs).
  - All 42 `seed.posts[].likeCount` values equaled their corresponding `posts.base_like_count` values (42 of 42 rows).
  - For all 42 posts, the count of seed comments excluding `deletedCommentIds` equaled the database count where `comments.is_deleted = FALSE` (42 of 42 posts).
- `backend/gradlew.bat test --no-daemon` completed with `BUILD SUCCESSFUL`.

**Result:** `PASS`

---

## V-004 — Immutable Starter Contracts

**Requirements:** `START-001`, `ENV-007`, `ENV-008`

### Steps

- Compare `frontend/app/dev-user/` against the starter version.
- Call `/health`.
- Switch users through `/dev-user` and confirm `x-user-id` cookie behavior still works.

### Expected

- `frontend/app/dev-user/` is unchanged.
- `/health` still returns 200.
- User switch behavior still works.

**Result:** `NOT_RUN`

---

# Requester / Ownership

## V-010 — Default Requester

**Requirements:** `USER-003`

### Steps

Call a requester-sensitive endpoint without `x-user-id` header or cookie.

### Expected

- Request is treated as coming from `apr_tester`.

**Result:** `NOT_RUN`

---

## V-011 — Header Requester

**Requirements:** `USER-001`

### Steps

Call a requester-sensitive endpoint with:

```http
x-user-id: <known-author>
```

### Expected

- Ownership and `isLiked` are evaluated for the header user.

**Result:** `NOT_RUN`

---

## V-012 — Cookie Requester

**Requirements:** `USER-002`, `START-003`

### Steps

1. Select a user through `/dev-user`.
2. Perform frontend/API operations that depend on requester identity.

### Expected

- `x-user-id` cookie is sent.
- Backend evaluates ownership/like state for that cookie user.

**Result:** `NOT_RUN`

---

# Post List

## V-020 — Newest-First Post Pagination

**Requirements:** `LIST-003`, `LIST-004`, `LIST-005`, `API-001`, `API-031`, `API-032`

### Steps

1. Request the first post page.
2. Record IDs/timestamps and pagination data.
3. Request the next page.
4. Continue until the end.

### Expected

- Each page is newest-first.
- Pages contain bounded subsets, not the entire collection.
- No duplicate post appears across adjacent pages.
- `total` represents the full readable count rather than current page size.

**Result:** `NOT_RUN`

---

## V-021 — Deleted Posts Excluded

**Requirements:** `DEL-003`, `DEL-004`, `API-035`

### Steps

1. Identify a seed ID listed in `deletedPostIds`.
2. Request post list.
3. Request detail for that ID.

### Expected

- Deleted post is not present in the list.
- Normal detail API does not expose the deleted post as readable content.

**Result:** `NOT_RUN`

---

# Post Creation / Editing

## V-030 — Post Validation

**Requirements:** `EDIT-001`, `EDIT-002`, `EDIT-003`, `EDIT-004`

### Scenarios

- empty title
- empty content
- title longer than 20 characters
- valid title/content

### Expected

- Invalid input cannot be saved.
- Frontend completion action is disabled for invalid required input.
- Backend rejects invalid payloads even if called directly.
- Valid payload succeeds.

**Result:** `NOT_RUN`

---

## V-031 — Create Post Flow

**Requirements:** `EDIT-005`, `EDIT-006`, `API-002`

### Steps

1. Create a valid post from the UI.
2. Observe navigation.
3. Observe toast.
4. Confirm post appears in newest-first list.

### Expected

- Post is created for current requester.
- UI returns to list.
- Completion toast is shown.
- New post is visible in the correct order.

**Result:** `NOT_RUN`

---

## V-032 — Edit Own Post

**Requirements:** `DETAIL-012` through `DETAIL-017`, `API-004`

### Steps

1. Open a post owned by the current requester.
2. Open action menu.
3. Choose edit.
4. Verify title/content are prefilled.
5. Modify valid values and save.
6. Re-fetch detail.

### Expected

- Edit action is available for owner.
- Existing values are prefilled.
- Same validation rules as create apply.
- Updated values persist.

**Result:** `NOT_RUN`

---

## V-033 — Reject Editing Another User’s Post

**Requirements:** `DETAIL-019`, `API-034`

### Steps

1. Use requester A.
2. Attempt to PATCH a post authored by requester B directly through the API.

### Expected

- Mutation is rejected.
- Post remains unchanged.

**Result:** `NOT_RUN`

---

# Soft Delete

## V-040 — Soft Delete Own Post

**Requirements:** `DEL-001`, `DEL-003`, `DEL-004`, `DETAIL-018`, `API-005`

### Steps

1. Create or identify an own post.
2. Record DB row existence.
3. Delete through API/UI.
4. Query DB row again.
5. Query post list.
6. Query detail.

### Expected

- DB row still exists with deleted marker/state.
- Resource disappears from list.
- Resource is not readable through normal detail API.
- UI returns to list after own-post deletion.

**Result:** `NOT_RUN`

---

## V-041 — Reject Deleting Another User’s Post

**Requirements:** `DETAIL-020`, `API-034`

### Steps

1. Use requester A.
2. Attempt to DELETE a post authored by requester B.

### Expected

- Deletion is rejected.
- Post remains readable for normal users.

**Result:** `NOT_RUN`

---

# Likes

## V-050 — Requester-Specific Like Toggle

**Requirements:** `DETAIL-001`, `DETAIL-002`, `DETAIL-003`, `API-006`, `API-027`, `API-028`, `API-030`

### Steps

1. Fetch a post as requester A and record `isLiked`, `likeCount`.
2. Toggle like once.
3. Fetch again as requester A.
4. Fetch same post as requester B.
5. Toggle again as requester A.

### Expected

- First toggle flips requester A's `isLiked` and adjusts `likeCount` by one.
- Requester B's `isLiked` is independently evaluated.
- Second toggle for requester A reverses the state/count change.

**Result:** `NOT_RUN`

---

# Comments

## V-060 — Initial Two Comments

**Requirements:** `DETAIL-004`, `DETAIL-005`, `DETAIL-006`, `DETAIL-007`

### Steps

Test posts with:

- 0 comments
- 1 comment
- 2 comments
- 3+ comments

### Expected

- Comments are newest-first.
- At most 2 comments are initially visible.
- “More” appears only when total >= 3.

**Result:** `NOT_RUN`

---

## V-061 — More Then Infinite Scroll

**Requirements:** `DETAIL-008`, `API-031`, `API-032`

### Steps

1. Open a post with enough comments for multiple pages.
2. Confirm only initial 2 are visible.
3. Activate “More”.
4. Scroll to load subsequent pages.

### Expected

- Additional comment pagination begins only after “More”.
- Pages are bounded.
- No duplicate comments appear.
- Comments remain newest-first.

**Result:** `NOT_RUN`

---

## V-062 — Create Comment

**Requirements:** `DETAIL-009`, `API-008`

### Steps

1. Create a comment as current requester.
2. Refresh/re-fetch detail.

### Expected

- Comment persists with current requester as author.
- Comment appears according to newest-first ordering.
- `commentCount` increases by one.

**Result:** `NOT_RUN`

---

## V-063 — Soft Delete Own Comment

**Requirements:** `DEL-002`, `DEL-005`, `DEL-006`, `DETAIL-010`, `DETAIL-011`, `API-009`

### Steps

1. Create or identify own comment.
2. Record owning post `commentCount`.
3. Delete comment.
4. Verify DB row remains with deleted marker/state.
5. Re-fetch comments and post detail.

### Expected

- DB row remains.
- Comment disappears from comment list.
- Post `commentCount` decreases by one.

**Result:** `NOT_RUN`

---

## V-064 — Reject Deleting Another User’s Comment

**Requirements:** `DETAIL-010`, `API-034`

### Steps

1. Use requester A.
2. Attempt to DELETE a comment authored by requester B.

### Expected

- Deletion is rejected.
- Comment remains readable.

**Result:** `NOT_RUN`

---

## V-065 — Seed-Deleted Comments Do Not Count

**Requirements:** `SEED-003`, `SEED-004`, `SEED-008`, `SEED-009`, `DEL-006`

### Steps

1. Choose a post with one or more comments listed in `deletedCommentIds`.
2. Calculate expected readable comment count from seed data.
3. Fetch post detail/list item.
4. Fetch comment list.

### Expected

- Deleted seed comments are absent from comment API.
- `commentCount` equals readable comments only.

**Result:** `NOT_RUN`

---

# API Contract

## V-070 — Post Response Shape

**Requirements:** `API-010` through `API-023`

### Verify

- list envelope contains `items`, `total`
- each post contains required fields
- detail is a single object, not an `items` envelope
- detail includes `isLiked`, `commentCount`
- patch returns updated post object

**Result:** `NOT_RUN`

---

## V-071 — Comment Response Shape

**Requirements:** `API-024`, `API-025`, `API-026`

### Verify

- comment list contains `items`, `total`
- comment item contains `id`, `postId`, `content`, `author`, `createdAt`

**Result:** `NOT_RUN`

---

# UI / Visual Verification

## V-080 — 375px Mobile Layout

**Requirements:** `UI-001`, `UI-002`, `UI-003`

### Steps

Inspect required screens at 375px viewport:

- post list
- post detail
- create post
- edit post
- post action menu/bottom sheet

### Expected

- Required functionality fits and remains usable at 375px.
- No desktop-specific layout is required.

**Result:** `NOT_RUN`

---

## V-081 — Official Design Fidelity

**Requirements:** `UI-004`

### Prerequisite

Official design source must be available.

### Verify

- typography
- spacing
- colors
- radii
- icons
- disabled state
- floating write button
- post action menu/bottom sheet
- toast

### Expected

Implementation matches official design as closely as practical, with any intentional deviations documented.

**Result:** `BLOCKED`

---

# Final Documentation

## V-090 — README Accuracy

**Requirements:** `DOC-001` through `DOC-009`

### Steps

Review README against the final code.

### Expected

- Technical choices match actual dependencies/implementation.
- Server-state description matches actual code.
- Layering description references real files/functions.
- Seed explanation matches actual initializer/schema.
- Tests are described accurately.
- Trade-offs reflect actual decisions.
- UX deviations/improvements are documented if applicable.

**Result:** `NOT_RUN`

---

# Final Sign-Off

Do not sign off until:

- `V-001`, `V-002`, `V-003`, `V-004` pass.
- All applicable behavior scenarios pass.
- All mandatory requirements are `VERIFIED`.
- `V-081` is no longer blocked if the official design source becomes available.
- README accurately reflects the final implementation.
