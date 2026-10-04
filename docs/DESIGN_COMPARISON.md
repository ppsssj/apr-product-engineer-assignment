# Official Figma Comparison

## Scope

This document records the final V-081 comparison of the official assignment
design with the production implementation on `main` at
`04dd668ae9d80d899edd8d363b42db5096bf4ac8`.

Official Figma-defined elements were compared against the current 375×778
Chromium implementation. Differences were classified against pre-existing
product decisions and runtime-only behavior. No blocking accidental mismatch
was found.

## Method

- Official Figma: file `Ee1gp5NC6G1sQIvssuu4Sr`, Page 1 (`0:1`).
- The official integration was used during V-081 to revalidate the current
  file identity, Page 1 node tree, frame IDs/names, and the `375×778` size of
  all 12 mapped frames.
- The visual baselines below are repository-preserved exports of the official
  Figma frames whose current official file/node mapping was revalidated during
  V-081. They are not screenshots newly exported from Figma during this audit.
- Current implementation: production Compose build, Chromium CSS viewport
  `375×778`, DPR `1`, zoom `1`, and a stable seed/requester context.
- The comparison used frame metadata, rendered geometry, visual hierarchy,
  and the rationale that pre-dated V-081. It did not use a pixel-difference
  threshold: browser font rasterization, renderer scaling, and device scale
  can produce non-semantic pixel differences.

## Classification

- `DESIGN_DEFINED`: presentation or state explicitly evidenced by the official
  Figma frame.
- `PRODUCT_DECISION`: an intentional implementation choice already documented
  before V-081.
- `RUNTIME_ONLY`: dynamic behavior that a static Figma frame does not define.
- `ACCIDENTAL_MISMATCH`: an undocumented difference in a design-defined area.

## Coverage Matrix

All mapped official frames are `375×778`. The two list exports are preserved at
4× (`1500×3112`); the other ten exports are preserved at 3× (`1125×2334`).

| Repository-preserved export | Figma node | Official state | Current implementation state | Result |
| --- | --- | --- | --- | --- |
| [`755-8831-post-list.png`](../design/design-export/755-8831-post-list.png) | `755:8831` | Post list | `/`, latest list | Compared |
| [`784-412-post-list-created-toast.png`](../design/design-export/784-412-post-list-created-toast.png) | `784:412` | List after creation, toast visible | Successful create → `/` | Compared |
| [`755-10420-detail-own-unliked.png`](../design/design-export/755-10420-detail-own-unliked.png) | `755:10420` | Own post, unliked | `/posts/1`, unliked | Compared |
| [`755-10774-detail-own-liked.png`](../design/design-export/755-10774-detail-own-liked.png) | `755:10774` | Own post, liked | `/posts/1`, liked | Compared |
| [`830-139-detail-own-action-sheet.png`](../design/design-export/830-139-detail-own-action-sheet.png) | `830:139` | Own-post action bottom sheet | Own-post anchored popover | Compared |
| [`755-11707-post-delete-dialog.png`](../design/design-export/755-11707-post-delete-dialog.png) | `755:11707` | Post-delete confirmation | Own-post delete dialog | Compared |
| [`755-12175-comment-delete-dialog.png`](../design/design-export/755-12175-comment-delete-dialog.png) | `755:12175` | Comment-delete confirmation | Own-comment delete dialog | Compared |
| [`755-12645-detail-other-many-comments.png`](../design/design-export/755-12645-detail-other-many-comments.png) | `755:12645` | Other-user post, many comments + More | `/posts/2`, initial two + More | Compared |
| [`755-13104-detail-other-one-comment.png`](../design/design-export/755-13104-detail-other-one-comment.png) | `755:13104` | Other-user post, one comment | `/posts/24` | Compared |
| [`769-142-detail-other-no-comments.png`](../design/design-export/769-142-detail-other-no-comments.png) | `769:142` | Other-user post, zero comments | `/posts/10` | Compared |
| [`755-14007-create-empty.png`](../design/design-export/755-14007-create-empty.png) | `755:14007` | Empty create form, disabled action | `/posts/new`, empty | Compared |
| [`755-14046-create-valid.png`](../design/design-export/755-14046-create-valid.png) | `755:14046` | Populated create form, enabled action | `/posts/new`, valid input | Compared |
| Shared create-form geometry | — | No separate edit frame | `/posts/1/edit`, prefilled values | Compared separately |

## Representative Comparisons

The text and seed content differ between the official examples and the stable
runtime seed. The comparison concerns structure, state, and presentation—not
literal content identity.

### 1. Post List

<table>
  <tr>
    <th>Official Figma</th>
    <th>Current implementation — Chromium 375×778</th>
  </tr>
  <tr>
    <td><img src="../design/design-export/755-8831-post-list.png" width="375" alt="Repository-preserved export of official post-list frame 755:8831"></td>
    <td><img src="assets/design-comparison/post-list-runtime.png" width="375" alt="Current production post list in Chromium at 375 by 778"></td>
  </tr>
</table>

| Preserved | Intentional change | Reason | Existing rationale source |
| --- | --- | --- | --- |
| 375px frame, Pretendard type/color basis, compact divided list, 18px avatars, right-aligned time, 50px bottom-right FAB | A 44px latest/likes/search discovery toolbar | Adds bounded discovery without a separate screen | [UI_SPEC §13](UI_SPEC.md), [ENGINEERING_NOTES §14](ENGINEERING_NOTES.md) |
| Title/preview/metadata content remains visible in each row | Title → preview → author/reaction/time content-first order | Prioritizes product experience and question content | [P8-C retouch record](../design/design-retouch/readme.md), [ENGINEERING_NOTES §14](ENGINEERING_NOTES.md) |
| Compact mobile rhythm | 108px rows instead of the official 118px or retouch 98px | 98px was too dense in 375×778 review; 108px preserved readability and touch separation | [UI_SPEC §12](UI_SPEC.md), [retouch record](../design/design-retouch/readme.md) |
| App identity and list controls remain at the top | Centered app title scrolls; only the toolbar is sticky | Avoids keeping two 48/44px bands permanently onscreen | [UI_SPEC §12–13](UI_SPEC.md), [ENGINEERING_NOTES §14](ENGINEERING_NOTES.md) |

**Classification:** `DESIGN_DEFINED` elements preserved with documented
`PRODUCT_DECISION` changes. **Verdict:** no accidental mismatch.

### 2. Post Detail

<table>
  <tr>
    <th>Official Figma</th>
    <th>Current implementation — Chromium 375×778</th>
  </tr>
  <tr>
    <td><img src="../design/design-export/755-12645-detail-other-many-comments.png" width="375" alt="Repository-preserved export of official other-user detail frame 755:12645"></td>
    <td><img src="assets/design-comparison/post-detail-runtime.png" width="375" alt="Current production other-user detail with initial comments and More in Chromium at 375 by 778"></td>
  </tr>
</table>

| Preserved | Intentional change | Reason | Existing rationale source |
| --- | --- | --- | --- |
| Title/body/like/comments hierarchy, initial two comments, More, fixed composer, major typography and color roles | Compact 24px byline before title, date in the footer, consistent own/other geometry | Keeps post identity compact and avoids vertical movement between ownership states | [UI_SPEC §12](UI_SPEC.md), [retouch record](../design/design-retouch/readme.md) |
| `댓글 (n)` heading and its 49px box | Count uses Medium 14/20 while label remains Bold 16/24 | Treats the count as supporting metadata without moving comments | [UI_SPEC §12](UI_SPEC.md), [retouch record](../design/design-retouch/readme.md) |
| Fixed comment-composer region and placeholder | `등록` affordance inside the field | Makes the assignment-required create action explicit | [UI_SPEC P5B decisions](UI_SPEC.md) |

**Classification:** `DESIGN_DEFINED` structure plus documented
`PRODUCT_DECISION` refinements. **Verdict:** no accidental mismatch.

### 3. Post Menu

<table>
  <tr>
    <th>Official Figma</th>
    <th>Current implementation — Chromium 375×778</th>
  </tr>
  <tr>
    <td><img src="../design/design-export/830-139-detail-own-action-sheet.png" width="375" alt="Repository-preserved export of official own-post action bottom sheet frame 830:139"></td>
    <td><img src="assets/design-comparison/post-menu-runtime.png" width="375" alt="Current production own-post anchored action popover in Chromium at 375 by 778"></td>
  </tr>
</table>

- **Figma:** full-screen scrim, bottom sheet, `수정하기`, `삭제하기`, and
  `취소`.
- **Implementation:** trigger-adjacent `144×98` popover, two 44px command
  rows, no scrim, and no cancel row.
- **Safety preserved:** destructive delete still opens the modal confirmation
  dialog with its scrim.
- **Existing rationale:** only two commands are present; keeping them next to
  the trigger improves spatial/action locality and removes unnecessary modal
  weight. See [UI_SPEC §12](UI_SPEC.md),
  [ENGINEERING_NOTES §14](ENGINEERING_NOTES.md), the
  [P8-C retouch record](../design/design-retouch/readme.md), and the historical
  [V-081 P8-C note](VERIFICATION.md).

**Classification:** `PRODUCT_DECISION`. **Verdict:** `INTENTIONAL_DEVIATION`.

## DESIGN_DEFINED Summary

| Element/state | Official evidence | Runtime evidence | Result |
| --- | --- | --- | --- |
| Frame | 12 frames at `375×778` | `375×778`, DPR 1, zoom 1 | Match |
| FAB | `50×50` at `(310,679)` | `50×50` at `(310,679)` | Match |
| Creation toast | `181×40` at approximately `(97.5,657)` | `181×40` at approximately `(97,658)` | Match |
| Create action | `343×56`, disabled/enabled states | `343×56`, radius 12, both states | Match |
| Like states | unliked and `#FF8DA1` liked variants | Both states rendered | Match |
| Comment coverage | zero, one, many; initial two + More | zero, one, many; initial two + More | Match |
| Delete dialog core | 327px width, radius 20, 56px actions | 327px width, radius 20, 56px actions | Match; height/color change documented below |
| Edit geometry | Shared create structure; no edit-specific frame | Shared form with prefilled title/content | Match within the shared-form contract |

## Intentional Deviations

Every deviation below had a rationale before V-081; this audit did not create
new product justification.

| Intentional deviation | Existing rationale source | Result |
| --- | --- | --- |
| Discovery toolbar | [UI_SPEC §13](UI_SPEC.md), [ENGINEERING_NOTES §14](ENGINEERING_NOTES.md) | `INTENTIONAL_DEVIATION` |
| Content-first list hierarchy | [UI_SPEC §12](UI_SPEC.md), [retouch record](../design/design-retouch/readme.md), [ENGINEERING_NOTES §14](ENGINEERING_NOTES.md) | `INTENTIONAL_DEVIATION` |
| 108px row rhythm | [UI_SPEC §12](UI_SPEC.md), [retouch record](../design/design-retouch/readme.md) | `INTENTIONAL_DEVIATION` |
| Centered title / sticky-toolbar split | [UI_SPEC §12–13](UI_SPEC.md), [ENGINEERING_NOTES §14](ENGINEERING_NOTES.md) | `INTENTIONAL_DEVIATION` |
| Compact detail hierarchy | [UI_SPEC §12](UI_SPEC.md), [retouch record](../design/design-retouch/readme.md) | `INTENTIONAL_DEVIATION` |
| Bottom sheet → anchored popover | [UI_SPEC §12](UI_SPEC.md), [retouch record](../design/design-retouch/readme.md), [ENGINEERING_NOTES §14](ENGINEERING_NOTES.md) | `INTENTIONAL_DEVIATION` |
| Common create/edit form shell | [UI_SPEC §12 and P6 decisions](UI_SPEC.md) | `INTENTIONAL_DEVIATION` |
| Compact dialog / red destructive confirmation | [retouch record](../design/design-retouch/readme.md), [UI_SPEC §12](UI_SPEC.md) | `INTENTIONAL_DEVIATION` |
| Readable supporting text `#6E7785` | [UI_SPEC §6.3](UI_SPEC.md), [V-081 historical status](VERIFICATION.md) | `INTENTIONAL_DEVIATION` |
| Reduced comment-count emphasis | [UI_SPEC §12](UI_SPEC.md), [retouch record](../design/design-retouch/readme.md) | `INTENTIONAL_DEVIATION` |
| Composer submit affordance | [UI_SPEC P5B decisions](UI_SPEC.md) | `INTENTIONAL_DEVIATION` |

## Runtime-only States

| State | Why outside Figma fidelity |
| --- | --- |
| Loading, request error, retry | Depend on network/runtime outcomes absent from static frames |
| Mutation pending/failure | Depend on request lifecycle |
| Infinite-scroll loading/error | Dynamic pagination behavior |
| Focus trap, Escape, keyboard loop | Accessibility interaction rather than static presentation |
| Cancellation, request timing, cache behavior | Application architecture |
| Sticky behavior while scrolling | Cannot be established by one static frame |

These states remain verified by their existing runtime evidence, but they are
classified as `OUTSIDE_FIGMA_FIDELITY` for V-081.

## Accidental Mismatches

`NONE`

## Result

`V-081 PASS`

The official mapping, all 12 exported states, the shared edit-form state, and
three representative comparisons provide sufficient evidence for `UI-004`.
