# UI 명세

## 1. 범위와 Source 우선순위

- 대상: mobile web (`ASSIGNMENT_DEFINED`)
- 기준 viewport: 375px (`ASSIGNMENT_DEFINED`)
- Desktop 대응: 범위 밖 (`ASSIGNMENT_DEFINED`)
- 공식 디자인 source: [Figma `assignment`](https://www.figma.com/design/Ee1gp5NC6G1sQIvssuu4Sr/assignment?node-id=0-1), Page 1 (`DESIGN_DEFINED`, `AVAILABLE`)
- 초기 audit 날짜: `2026-09-29`

`ASSIGNMENT.md`가 기능의 의미와 범위를 정하는 기준입니다. Figma는 presentation을 정하고 state를 보여 주지만, product 요구사항을 추가하거나 과제가 정한 동작을 덮어쓰지 않습니다.

이 문서의 시간축:

| 시점 | 내용 | 이 문서의 위치 |
| --- | --- | --- |
| 2026-09-29 | 공식 Figma 초기 audit와 UI 명세 | §3–§11 |
| P4–P7-A | 단계별 구현 결정(당시 snapshot) | §7의 "P4/P5A/P5B/P6 당시 구현 결정", "P7-A" |
| P8-C | 의도적 UX 보정(현재 구현 기준) | §12 |
| 2026-09-30 이후 | 게시글 탐색 toolbar(현재 구현 기준) | §13 |
| 2026-10-04 | 최종 V-081 비교 `PASS` | §3, [`DESIGN_COMPARISON.md`](DESIGN_COMPARISON.md) |

§6의 값은 공식 Figma 근거이고, §7의 phase 표는 그 시점의 구현 값이며, 현재 구현이 공식 값과 의도적으로 다른 부분은 §12–§13에 있습니다.

## 2. Evidence 분류

- `ASSIGNMENT_DEFINED`: `ASSIGNMENT.md` 또는 그 추적 requirement ID에 명시된 요구사항.
- `DESIGN_DEFINED`: 공식 Figma source에서 관찰한 구조나 시각 값.
- `PRODUCT_DECISION`: 과제나 디자인과 모순되지 않으면서, 정해지지 않은 product state를 완성하기 위해 필요한 동작.
- `ARCHITECTURE_DECISION`: `docs/ARCHITECTURE.md`에서 승인한 구현 방향(API, state, cache, pagination 등).

Pretendard와 avatar 세 개 같은 repository asset은 Figma 근거가 아니라 repository 사실로 기록합니다.

## 3. Evidence 상태

| Source | Status | Notes |
| --- | --- | --- |
| `ASSIGNMENT.md` | `AVAILABLE` | mobile 범위, 필수 흐름, data field, 상호작용, API를 정의합니다. |
| Repository starter | `AVAILABLE` | App Router, 수정 불가 `/dev-user`, Pretendard, avatar asset 세 개를 제공합니다. |
| 공식 Figma | `AVAILABLE` | File key `Ee1gp5NC6G1sQIvssuu4Sr`, Page 1 (`0:1`); 제공된 top-level frame 12개를 audit했습니다. |
| Local `design/` asset | `NOT_PRESENT` | `ASSIGNMENT.md`는 `design/메디큐브톡.fig` 제공을 설명하지만, 이 Git repository에는 `.fig` 파일이 commit되어 있지 않습니다(`design/`에는 `design-export/`와 `design-retouch/`만 있음). 접근 가능한 공식 Figma URL이 기준 source이므로 inspection을 막지 않았습니다. |

Audit 근거는 실제 Figma node tree와 render된 frame 출력에서 수집했습니다. 아래의 정확한 값은 frame/node 근거가 그 값을 드러낸 경우에만 적었습니다.

최종 V-081 비교는 `2026-10-04`에 완료되어 `PASS`였고, 우발적 불일치(`ACCIDENTAL_MISMATCH`)는 `NONE`이었습니다. 비교 기준은 공식 mapping, frame metadata, rendered geometry, visual hierarchy, 그리고 V-081 전에 이미 문서화된 product decision입니다. 전체 비교 범위, 대표 근거, 의도적 차이 분류는 [`DESIGN_COMPARISON.md`](DESIGN_COMPARISON.md)에 있습니다. V-081 baseline(`04dd668`) 이후 현재 main까지 `frontend/`와 `design/`에는 변경이 없습니다.

## 4. 화면 및 State 목록

audit한 top-level frame은 모두 `375 × 778`입니다(`DESIGN_DEFINED`).

| Figma node | Figma name | Mapped UI/state | Evidence |
| --- | --- | --- | --- |
| `755:8831` | 메디큐브톡 | SCREEN-001 게시글 목록 | `DESIGN_DEFINED` |
| `784:412` | 메디큐브톡(작성 완료) | SCREEN-001 + SCREEN-006 작성 완료 toast | `DESIGN_DEFINED` |
| `755:10420` | 메디큐브톡_상세(본인 글) | SCREEN-002 본인 글, 좋아요 안 함 (`좋아요 42`) | `DESIGN_DEFINED` |
| `755:10774` | 메디큐브톡_상세(본인 글) | SCREEN-002 본인 글, 좋아요 함 (`좋아요 43`) | `DESIGN_DEFINED` |
| `830:139` | 메디큐브톡_상세(본인 글) | SCREEN-002 본인 글 + SCREEN-005 action bottom sheet | `DESIGN_DEFINED` |
| `755:11707` | 메디큐브톡_상세(글 삭제) | 게시글 삭제 확인 overlay | `DESIGN_DEFINED` |
| `755:12175` | 메디큐브톡_상세(댓글 삭제) | 댓글 삭제 확인 overlay | `DESIGN_DEFINED` |
| `755:12645` | 메디큐브톡_상세(타인 글) | SCREEN-002 다른 사용자 글, 댓글 여러 개 + 더보기 | `DESIGN_DEFINED` |
| `755:13104` | 메디큐브톡_상세(타인 글) | SCREEN-002 다른 사용자 글, 댓글 1개 | `DESIGN_DEFINED` |
| `769:142` | 메디큐브톡_상세(타인 글) | SCREEN-002 다른 사용자 글, 댓글 없음 (`댓글 (0)`) | `DESIGN_DEFINED` |
| `755:14007` | 메디큐브톡 작성 | SCREEN-003 빈/유효하지 않은 form, 제출 비활성 | `DESIGN_DEFINED` |
| `755:14046` | 메디큐브톡 작성(내용O) | SCREEN-003 입력된/유효한 form, 제출 활성 | `DESIGN_DEFINED` |

SCREEN-004 수정에는 별도로 audit한 top-level mockup이 없습니다. 기존 값을 채운 채 SCREEN-003을 재사용하는 것은 `ASSIGNMENT_DEFINED`입니다. 공유 form 구조 덕분에 재사용할 수 있지만, 그것이 별도의 수정 state를 시각적으로 정의하지는 않습니다.

Loading, 요청 오류, 다음 page loading, mutation pending, 제출 오류 화면은 audit한 frame에 없습니다. 공식 Figma는 이 state들을 정의하지 않습니다. 초기 audit 당시에는 시각 처리가 구현 단계의 미결정 항목이었고, 이후 구현에서 `PRODUCT_DECISION`으로 정해졌습니다(§7의 phase 표, §10, §12).

## 5. Navigation과 사용자 흐름

```text
Post List
  -> Post Detail
  -> Create Post

Post Detail (own post)
  -> Post actions (수정하기 / 삭제하기)
  -> Edit Post (reuse Create form with prefilled values)
  -> Delete Confirmation -> delete -> Post List

Post Detail (own comment, regardless of post owner)
  -> Comment Delete Confirmation -> delete -> updated detail

Create Post success -> Post List + completion toast
Edit Post success -> updated Post Detail
```

- 목적지와 ownership 동작은 `ASSIGNMENT_DEFINED`입니다.
- 공식 디자인에서 "Post actions"는 scrim이 있는 bottom sheet(`830:139`, `DESIGN_DEFINED`)입니다. 현재 구현은 `⋮` 옆에 붙는 `PostActionMenu` popover이며 이는 P8-C의 `PRODUCT_DECISION`입니다(§12). 두 경우 모두 수정과 삭제를 고를 수 있습니다.
- 확인 overlay, form의 활성/비활성 state, toast 위치는 `DESIGN_DEFINED`입니다.
- 구체적인 URL path는 `PRODUCT_DECISION`입니다.
- 로그인 화면은 만들지 않습니다(`ASSIGNMENT_DEFINED`, `USER-004`).

## 6. Design-Defined 시각 기반

이 section의 값은 모두 공식 Figma 근거입니다. 현재 구현이 의도적으로 다른 값을 쓰는 경우(예: row 108px, popover)는 §12–§13에 있으며, 여기의 공식 값을 바꾸지 않습니다.

### 6.1 Viewport, 영역, 간격

- audit한 top-level frame은 모두 `375 × 778`입니다.
- 주요 가로 content inset은 16px이고, 이에 맞는 content 너비는 343px입니다.
- 목록 header 높이는 48px이고, 목록 content는 y=56에서 시작합니다.
- 목록 row는 118px 높이이며, x=16, y=12의 343×93 내부 content 영역과 343×1 divider를 가집니다.
- 떠 있는 작성 control은 x=310, y=679의 50×50입니다.
- 상세 frame은 375×48 상단 navigation 영역, y=56에서 시작하는 scroll content, y=692의 고정 375×86 댓글 composer/safe-area 영역을 씁니다.
- 작성 form은 16px 가로 inset과 고정 하단 action 영역(375×68 action 영역 + 375×26 safe-area 영역)을 씁니다.
- 확인 dialog는 전체 화면 overlay 위의 x=24, y=257, width=327, height=264를 차지합니다.
- 본인 글 action sheet는 전체 화면 overlay 위의 x=0, y=554, width=375, height=224를 차지합니다.

이 측정값은 `DESIGN_DEFINED`입니다. 375px 대상 밖의 responsive 동작은 audit한 frame이 정의하지 않습니다.

### 6.2 Typography

audit한 frame에서는 Pretendard가 주된 family이며, repository도 Pretendard를 제공합니다.

| Figma에서 관찰한 역할 | Family / style | Size / line height | Color |
| --- | --- | --- | --- |
| App title | Pretendard Bold | 20 / 28px | `#292828` |
| 목록 게시글 제목 | Pretendard Medium | 16 / 24px | `#292828` |
| 목록 preview | Pretendard Regular | 14 / 18px | `#979EA9` |
| 목록 작성자/count/시간 | Pretendard Regular | 12 / 16px | `#5E5E5E` 또는 `#979EA9` |
| 상세 제목 | Pretendard Bold | 18 / 26px | `#292828` |
| 상세 작성자 | Pretendard Bold | 15 / 22px | `#292828` |
| 상세 본문 | Pretendard Regular | 14 / 18px | `#292828` |
| 상세 section/count label | Pretendard Bold | 16 / 24px | `#292828` |
| 작성 label | Pretendard Bold | 14 / 22px | `#292828` |
| 작성 입력/placeholder | Pretendard Regular | 14 / 18px | `#292828` / `#979EA9` |
| Toast | Pretendard Regular | 14 / 18px | `#FFFFFF` |

펼친 본인 글 frame `830:139`에는 Apple SD Gothic Neo와 font가 섞인 legacy layer가 있지만, 같은 주요 state들은 Pretendard를 씁니다. Apple SD Gothic Neo를 전역 token으로 삼을 근거는 없으므로, 이후 component별 검토가 달리 증명하지 않는 한 구현은 repository가 제공한 Pretendard를 일관되게 씁니다(`PRODUCT_DECISION`).

### 6.3 색, Divider, Radius

audit한 node에서 공통으로 관찰한 정확한 단색:

- surface: `#FFFFFF`
- 주 text: `#292828`
- 보조 text: `#979EA9`
- 지원 text: `#5E5E5E`
- 밝은 divider/border: `#DDE0E4`, `#EAECEE`, `#DADEE3`
- 비활성 action surface: `#F3F4F8`
- 좋아요 state accent(좋아요 함 상세 variant에만 있음): `#FF8DA1`
- modal/sheet scrim: 60% 불투명도의 검정
- toast surface: 70% 불투명도의 검정

접근성 조정(`PRODUCT_DECISION`): 디자인의 보조색 `#979EA9`는 source 값으로 유지하지만, 흰 배경에서 2.70:1로 일반 text의 WCAG AA 4.5:1에 못 미칩니다. 사용자가 읽어야 하는 text(목록 preview/count/시간, 선택되지 않은 정렬, 검색 label, 날짜, 좋아요 수, 댓글 시간/count/action, dialog 본문, 상태 안내, placeholder)와 의미를 가진 icon/focus border는 `--color-text-secondary-readable` `#6E7785`(같은 hue, 4.53:1)를 씁니다. dialog 취소 label은 `#EAECEE` 위에 있어 그 값이 3.82:1이므로 지원 text `#5E5E5E`(5.47:1)를 씁니다. 비활성, 읽기 전용(pending), 눌림 feedback state는 `#979EA9`를 유지합니다. 아래 §7의 phase 표와 §12–§13에 적힌 `#979EA9` 중 위 목록에 해당하는 text는 현재 구현에서 `#6E7785`로 render됩니다.

관찰한 0이 아닌 corner radius 집합:

- 목록 variant: 5px, 14px
- 목록 toast variant에 추가로: 6px
- 상세 variant: 4px, 12px, 20px
- overlay variant에 추가로: 8px
- 작성 variant: 4px, 12px

node 근거가 관계를 분명히 보여 주지 않은 radius는 의미 token으로 지정하지 않았습니다.

### 6.4 Control과 Icon

- 상단 navigation은 뒤로 가기 control을 쓰고, 본인 글 variant는 게시글 action trigger를 추가로 보입니다(`DESIGN_DEFINED` + `ASSIGNMENT_DEFINED`).
- 목록은 흰 연필 icon이 있는 빨간 원형 floating action을 씁니다(`DESIGN_DEFINED`).
- 목록 metadata는 댓글/좋아요 icon을 쓰고, 상세는 좋아요 함 state를 따로 보입니다(`DESIGN_DEFINED`).
- 삭제 확인은 명시적인 취소/확인 action을 쓰고, 본인 글 action sheet는 icon이 있는 수정/삭제 row를 씁니다(`DESIGN_DEFINED`).
- 정확한 SVG path data는 이 audit에 필요하지 않았으며 design token으로 올리지 않았습니다.

## 7. 화면 명세

각 화면에서 "기능 계약"은 현재도 유효한 과제 계약, "디자인 조정"은 공식 Figma 근거, "P4/P5A/P5B/P6 당시 구현 결정"과 "P7-A"는 그 시점의 구현 snapshot입니다. 현재 구현은 §12–§13을 함께 봐야 합니다.

### SCREEN-001 — 게시글 목록

#### 기능 계약

- 읽을 수 있는 게시글을 최신순으로 보고 상세/작성 흐름으로 들어갑니다(`ASSIGNMENT_DEFINED`).
- infinite scroll은 범위가 정해진 cursor continuation으로 구현합니다(`ARCHITECTURE_DECISION`).
- 제목, preview, 작성자/avatar, 좋아요 수, 댓글 수, 상대 시간을 표시합니다(`ASSIGNMENT_DEFINED`).

#### 디자인 조정

- Figma는 118px row, 16px page inset, 18px 원형 avatar, 제목/preview hierarchy, metadata icon, 오른쪽 정렬 상대 시간, divider, 오른쪽 아래 50px 작성 control로 이루어진 compact 세로 목록을 보여 줍니다(`DESIGN_DEFINED`).
- Figma는 내용이 채워진 상태만 보여 줍니다. empty/loading/error/목록 끝 표시는 `PRODUCT_DECISION`입니다.
- 안정적인 avatar mapping은 `stableHash(author) % 3`입니다(`PRODUCT_DECISION`). 과제는 제공된 asset 중 무엇이든 허용하며 avatar fidelity를 채점하지 않습니다.

#### P4 당시 구현 결정

P4 동안에는 node `755:8831`을 Figma MCP로 새로 비교할 수 없었습니다(Starter-plan tool-call 한도 소진). 그래서 P4는 위의 `DESIGN_DEFINED` 값을 기록된 대로 구현하고 나머지는 최소한의 `PRODUCT_DECISION` 값으로 채웠습니다. 아래 값은 어느 것도 Figma와 정확히 같다는 주장이 아닙니다. 이후 P7-A와 P8-C(§12)에서 일부가 바뀌었습니다.

| Detail | P4 선택 | Evidence |
| --- | --- | --- |
| Divider 색 | 기존 `--color-border-light` (`#DDE0E4`) | audit한 밝은 border 색 중 구현이 고른 값이며 Figma 정확 mapping이 아님 |
| 제목 | 한 줄, ellipsis | `PRODUCT_DECISION` |
| Preview | 최대 2줄, ellipsis; 제목 아래 4px | `PRODUCT_DECISION` |
| Metadata row | 343×93 영역 바닥에 고정; avatar→이름 4px, 항목 간격 8px, icon→count 2px | `PRODUCT_DECISION` |
| Metadata 색 | 작성자 `#5E5E5E`; 댓글/좋아요 수와 시간 `#979EA9` | 두 색 모두 `DESIGN_DEFINED`; 각 항목에 배정한 것은 `PRODUCT_DECISION` |
| 댓글/좋아요/연필 icon | inline SVG 근사(metadata 14px, 연필 24px), `currentColor` | `PRODUCT_DECISION`; Figma path는 추출하지 않음 |
| Header | sticky, 16px inset의 왼쪽 정렬 title | `PRODUCT_DECISION` |
| FAB | 고정 `right: 15px; bottom: 49px`(375×778의 x=310, y=679에서 계산); 빨강 `#F5364F`, shadow `0 2px 8px rgba(0,0,0,.16)`를 CSS Module 안에 둠 | Geometry `DESIGN_DEFINED`; 색과 shadow는 `PRODUCT_DECISION`이며 전역 token 아님 |
| Loading / empty / error / 다음 page loading / 다음 page error | 가운데 정렬 보조색 text; 오류에는 `다시 시도` button 추가 | `PRODUCT_DECISION` |

#### P7-A 당시 SVG 기반 목록 보정

`755-8831-post-list.svg`는 목록의 `375×778` 좌표계와 text metadata를 그대로 보존합니다. 그래서 P7-A는 118px 높이와 row 전체 link를 유지한 채 row hierarchy를 avatar/작성자 → 제목 → preview → 댓글/좋아요/시간으로 고쳤습니다. SVG는 18px avatar를 x=16/y=68에, 작성자를 x=40에, 제목을 x=16에, preview를 x=16에, 343×1 divider를 x=16/y=173에 둡니다. 유지보수 가능한 CSS는 text baseline을 직접 쓰지 않고 이에 대응하는 18/24/18/16 row rhythm을 씁니다. *(이후 P8-C에서 제목 위 작성자 row를 없애고 제목 → preview → metadata, 108px row로 바꿨습니다. §12 참고.)*

정본 목록 SVG는 `#DADEE3`로 채운 중립 댓글·하트 glyph를 씁니다(하트에는 1.4px stroke도 있음). P7-A는 목록 icon을 같은 vector로 맞췄습니다. `post.isLiked`는 이제 목록 하트만 기존 상세 accent `#FF8DA1`로 칠합니다. 목록 reference에는 좋아요 함 variant가 없으므로 이는 목록 디자인 주장이 아니라 `UX_CONSISTENCY`입니다. 목록 API가 요청자별 댓글 참여를 노출하지 않으므로, 요청자가 댓글을 단 글의 accent는 보류했습니다.

### SCREEN-002 — 게시글 상세

#### 기능 계약

- 게시글을 읽고, 요청자별 좋아요를 toggle하고, 댓글을 읽기/작성/삭제합니다(`ASSIGNMENT_DEFINED`).
- 처음에는 최신 댓글을 최대 2개만 보이고, 전체가 3개 이상일 때만 더보기를 보이며, 더보기 이후에만 댓글 infinite scroll을 시작합니다(`ASSIGNMENT_DEFINED` + `ARCHITECTURE_DECISION`).
- 게시글 수정/삭제 UI는 게시글 소유자에게만, 댓글 삭제는 댓글 소유자에게만 보입니다(`ASSIGNMENT_DEFINED`).

#### 디자인 조정

- Figma는 고정 상단 navigation, 게시글 제목/작성자/날짜/본문 hierarchy, 좋아요/count control, 댓글 heading/count, 댓글 목록, 조건부 `댓글 더보기`, 고정 하단 댓글 composer를 보여 줍니다(`DESIGN_DEFINED`).
- 좋아요 안 함/함 본인 글 variant는 `좋아요 42`를 `좋아요 43`으로 바꿉니다. `#FF8DA1`은 좋아요 함 variant에만 있고 좋아요 안 함 variant에는 없습니다(`DESIGN_DEFINED`).
- 다른 사용자 글 variant는 댓글 0개, 1개, 여러 개를 명시적으로 다룹니다. 댓글 0개 state는 `댓글 (0)`을 보이고 composer를 유지하며, 별도의 empty-state 문구를 더하지 않습니다(`DESIGN_DEFINED`).
- 댓글 여러 개 frame은 댓글 2개와 `댓글 더보기`를 보여, 과제의 처음 2개 규칙과 일치합니다(`DESIGN_DEFINED` + `ASSIGNMENT_DEFINED`).
- 다른 사용자 글에서도 본인 댓글 삭제 affordance가 보여, 댓글 ownership이 게시글 ownership과 독립임을 확인시켜 줍니다(`DESIGN_DEFINED`, `ASSIGNMENT_DEFINED`와 일치).

#### P5A 당시 구현 결정

P5A는 상세 읽기, 좋아요 toggle, 댓글 읽기/이어 읽기를 구현했습니다. composer와 본인 댓글 삭제는 P5B에서 추가했습니다(아래). P5A 당시에는 본인 글 `⋯` 메뉴, 수정, 삭제를 아직 render하지 않았고, 이후 P6에서 추가했습니다. 아래 값은 `755:10420`, `755:10774`, `755:12645`, `755:13104`, `769:142`의 local PNG export(375×778, 3×)에서 추정한 값이며 Figma node 측정값이 아닙니다.

| Detail | P5A 선택 | Evidence |
| --- | --- | --- |
| 상단 navigation | sticky 375×48, x=16의 24px 뒤로 화살표(`뒤로 가기`); history back, 직접 load면 `/` | 영역 `DESIGN_DEFINED`; 동작 `PRODUCT_DECISION` |
| 게시글 header | y≈80의 제목(content 시작 24px 아래); 36px avatar; 그 오른쪽에 10px 간격으로 작성자/날짜 | PNG 추정 `PRODUCT_DECISION` |
| 게시글 날짜 | local time의 `YY.MM.DD`, 14/18 `#979EA9` | 형식은 PNG에서 관찰; 크기 `PRODUCT_DECISION` |
| 본문 | byline 24px 아래, `white-space: pre-wrap` | Type `DESIGN_DEFINED`; 간격 `PRODUCT_DECISION` |
| 좋아요 control | 24px 하트 + `좋아요 n` 14/18 `#979EA9`, 본문 32px 아래; 좋아요 안 함은 `#979EA9` outline, 좋아요 함은 `--color-liked`로 채움 | `#FF8DA1` 좋아요 accent `DESIGN_DEFINED`; icon 그림과 크기 `PRODUCT_DECISION` |
| Section band | 8px, `--color-disabled-surface` (`#F3F4F8`) 재사용 | PNG 관찰; token 재사용 `PRODUCT_DECISION` |
| 댓글 heading | `댓글 (n)` Bold 16/24, `ko-KR` 천 단위 구분, 48px row, 전체 너비 `--color-border-light` divider | Type `DESIGN_DEFINED`; row/divider `PRODUCT_DECISION` |
| 댓글 항목 | 30px avatar, 작성자 Bold 15/22 `#292828`, 상대 시간 12/16 `#979EA9`, 내용 Regular 15/22 `#292828` | PNG 추정 `PRODUCT_DECISION` (PNG는 상대·절대 댓글 날짜가 섞여 있으며, P5A는 모두 상대 시간을 씀) |
| 더보기 | `댓글 더보기` + chevron, 343×40, 1px `--color-border-light`, 12px radius, 14/18 `#979EA9` | 존재 `DESIGN_DEFINED`; geometry PNG 추정 |
| Loading / not-found / error | 가운데 정렬 보조 text; not-found에는 `목록으로`, 오류에는 `다시 시도`; continuation 오류는 로드된 댓글 아래 | `PRODUCT_DECISION` |

#### P5B 당시 구현 결정

composer 영역(y=692의 375×86), dialog frame(x=24, y=257의 327×264, 전체 화면 60 % 검정 scrim), dialog 문구는 `DESIGN_DEFINED`입니다. 아래 값은 local PNG export `755:12175`, `755:13104`, `755:12645`에서 추정했으며 Figma node 측정값이 아닙니다.

| Detail | P5B 선택 | Evidence |
| --- | --- | --- |
| Composer layout | 1px `--color-border-light` 상단 divider; y=702에서 시작하는 343×40 pill field(radius 20, 1px border); field 아래 36px | 영역 `DESIGN_DEFINED`; 분할은 PNG 추정 |
| Composer text | `댓글을 입력하세요.` placeholder, 14/18, `#979EA9` | Placeholder `DESIGN_DEFINED`; 크기 `PRODUCT_DECISION` |
| 제출 control | field 안의 `등록` text button, Bold 14/18; 비활성은 `#979EA9`, 활성은 `#292828` | `PRODUCT_DECISION` — 디자인에는 제출 control이 없음 |
| 제출 규칙 | 공백만 있는 입력은 비활성(backend `isBlank` 규칙); 내용은 그대로 전송; IME 조합 중이 아니면 Enter로 제출; 한 줄 입력 | `PRODUCT_DECISION` |
| 작성 오류 | field 위의 `댓글을 등록하지 못했습니다. 다시 시도해 주세요.` 12/16 `#5E5E5E`; 영역이 위로 늘어남 | `PRODUCT_DECISION` |
| 삭제 affordance | 내용 6px 아래의 `삭제` text button 12/16 `#979EA9`, `comment.author`가 요청자와 같을 때만 | 위치는 PNG 관찰; 표시 규칙 `ASSIGNMENT_DEFINED` |
| Dialog | radius 20; 제목 Bold 18/26 `#292828`; 본문 15/22 `#979EA9` 두 줄; 130×56 button(radius 12, 간격 15px, 하단 inset 40px); `취소`는 `#EAECEE` 위 `#979EA9`, `예`는 `#292828` 위 흰색, 둘 다 Bold 16/24 | 문구/frame `DESIGN_DEFINED`; 크기와 색은 PNG 추정. radius는 audit한 집합 안이고 `#EAECEE`는 audit한 border 색 중 하나 |
| Dialog 동작 | `alertdialog`, 열리면 `취소`에 focus; 요청 pending이 아니면 Escape나 scrim click으로 취소; 오류 시 dialog 유지(`댓글을 삭제하지 못했습니다. 다시 시도해 주세요.`) | `PRODUCT_DECISION` |
| 표시용 요청자 | `x-user-id` cookie(첫 nonblank 값), 없으면 `apr_tester`; cookie만 있는 요청에 대해 `RequesterResolver`를 따름 | `PRODUCT_DECISION`; 최종 판단은 backend |

### SCREEN-003 — 게시글 작성

#### 기능 계약

- 제목과 내용은 필수이고, 제목은 최대 20자이며, 유효하지 않은 form은 제출할 수 없습니다(`ASSIGNMENT_DEFINED`).
- 성공하면 SCREEN-001로 돌아가 SCREEN-006을 보입니다(`ASSIGNMENT_DEFINED`).

#### 디자인 조정

- 빈 frame은 제목/내용 placeholder와 비활성 고정 하단 `작성 완료` action을 보여 줍니다(`DESIGN_DEFINED`).
- 입력된 frame은 입력한 제목/내용과 활성 고정 하단 action을 보여 줍니다(`DESIGN_DEFINED`).
- 제목 placeholder는 `(최대 20자)`를 명시합니다(`DESIGN_DEFINED`, `ASSIGNMENT_DEFINED`와 일치).
- 제출 중/오류 state는 나오지 않으므로 `PRODUCT_DECISION`입니다.

#### P6 당시 구현 결정 (SCREEN-003–006)

영역과 문구는 `DESIGN_DEFINED`입니다. 작성 action 375×68 + safe area 26, `(최대 20자)`가 있는 제목 placeholder, 비활성/활성 action, y=554의 sheet 375×224, 게시글 삭제 dialog 문구와 frame, x=97.5, y=657의 toast 181×40입니다. 아래 값은 local PNG export `755-14007`, `755-14046`, `830-139`, `755-11707`, `784-412`에서 추정했으며 Figma node 측정값이 아닙니다.

| Detail | P6 선택 | Evidence |
| --- | --- | --- |
| 공유 form | 작성과 수정에 `PostForm` 하나; heading `글작성` / `글수정`, action `작성 완료` / `수정 완료` | 재사용 `ASSIGNMENT_DEFINED`; 수정 문구 `PRODUCT_DECISION` |
| Form layout | y≈82의 label Bold 14/22; 그 10px 아래 밑줄 제목 field(비면 밝은 선, 채우면 `#292828`); 32px 아래 내용 textarea; 내용 placeholder는 디자인의 두 문장 | Type `DESIGN_DEFINED`; 위치는 PNG 추정 |
| Action button | y=690의 343×56, radius 12, Bold 16/24 흰색; 활성 `#292828`, 비활성 `#DADEE3` | PNG 관찰. PNG의 button이 `#F3F4F8` surface보다 어두워 비활성 채움에 audit한 `#DADEE3`를 씀 |
| Validation | blank = 모든 code point가 Java whitespace(`isBlank`); 제목 ≤ 20 Unicode code point(`codePointCount`); 길이 초과 안내 `제목은 최대 20자까지 입력할 수 있습니다.`; 값은 trim하지 않고 전송; 제목에서 Enter는 내용으로 이동 | 규칙은 backend `MutationValidator`를 따름; 안내 문구 `PRODUCT_DECISION` |
| 오류 | action 위의 `게시글을 등록하지 못했습니다.` / `게시글을 수정하지 못했습니다.` + `다시 시도해 주세요.`; 다른 사용자의 수정 URL에는 `이 게시글을 수정할 권한이 없습니다.` | `PRODUCT_DECISION` |
| 메뉴 trigger | 48px navigation 오른쪽의 24px 세로 ellipsis, 본인 글에만(`게시글 메뉴`) | 위치는 PNG 관찰; 표시 `ASSIGNMENT_DEFINED` |
| Action sheet | 상단 radius 20; 24px icon과 Regular 16/24 label이 있는 60px row; divider inset 24px; 343×56 `취소`(radius 12, `#292828`); Escape, scrim, `취소`로 닫힘 | PNG 추정 `PRODUCT_DECISION` *(P6 당시 `PostActionSheet`. 이후 P8-C에서 `PostActionMenu` popover로 바뀜, §12)* |
| 게시글 삭제 dialog | P5B `ConfirmDialog`에 `삭제한 글은 복구할 수 없습니다.` / `정말 삭제 하시겠습니까?` | 문구 `DESIGN_DEFINED`; 재사용 `PRODUCT_DECISION` |
| Toast | 2.5 s, motion 없음, `pointer-events: none`, in-memory 1회성 flash로 작성마다 한 번 표시(reload 후에는 표시 안 함) | 지속 시간/motion `PRODUCT_DECISION` *(이후 P8-C에서 entrance motion 추가, §12)* |
| 성공 후 이동 | 작성 → 목록, 수정 → 상세; 앱 안에서 열었으면 연 화면의 history entry로 돌아가고, 아니면 replace | `PRODUCT_DECISION` |

`784-412` toast PNG는 audit한 목록 frame `755-8831`과 다른 목록 row layout(제목 위 작성자)도 보여 줍니다. P6는 그 PNG를 toast에만 썼고, 목록 layout 차이는 P7 검토로 넘겼습니다.

### SCREEN-004 — 게시글 수정

- 기존 제목/내용을 채운 채 SCREEN-003을 재사용하고 같은 validation을 적용합니다(`ASSIGNMENT_DEFINED`).
- 별도 수정 frame이나 수정 전용 시각 token은 없었습니다(`DESIGN_DEFINED` 관찰).
- 권한 없는 직접 접근은 backend가 처리합니다(`ARCHITECTURE_DECISION`).

### SCREEN-005 — Post Action Bottom Sheet

이 identifier는 공식 Figma state 이름을 따라 유지합니다.

- **공식 디자인(`DESIGN_DEFINED`):** frame `830:139`는 scrim과 함께 `수정하기`, `삭제하기`, 취소가 있는 bottom sheet를 보여 줍니다.
- sheet는 본인 글에만 나타납니다(`ASSIGNMENT_DEFINED`).
- 수정은 SCREEN-004로 가고, 삭제는 먼저 게시글 삭제 확인을 엽니다(`DESIGN_DEFINED` 흐름 detail, 과제와 일치).
- 닫기 gesture와 animation timing은 static frame이 정의하지 않으므로 `PRODUCT_DECISION`입니다.
- **현재 구현(`PRODUCT_DECISION`):** P6 당시에는 이 bottom sheet를 `PostActionSheet`로 구현했고, P8-C에서 `⋮` 옆의 `PostActionMenu` popover(수정하기 / 삭제하기, scrim과 취소 row 없음)로 바꿨습니다. 수정·삭제를 고를 수 있다는 과제 계약은 그대로입니다. 자세한 값은 §12, V-081 분류는 `INTENTIONAL_DEVIATION`입니다.

### SCREEN-006 — 작성 완료 Toast

- Frame `784:412`는 x=97.5, y=657의 181×40 toast에 `게시글이 등록되었습니다.`를 보여 줍니다(`DESIGN_DEFINED`).
- 70% 불투명도 검정 위의 흰 14/18px text이고, 관찰한 radius는 6px입니다(`DESIGN_DEFINED`).
- 작성 성공 후 표시는 `ASSIGNMENT_DEFINED`이고, 지속 시간과 motion은 `PRODUCT_DECISION`입니다.

### 삭제 확인

- 게시글 삭제: 제목 `삭제하기`, 본문 `삭제한 글은 복구할 수 없습니다. 정말 삭제 하시겠습니까?`, action `취소` / `예` (`DESIGN_DEFINED`).
- 댓글 삭제: 같은 구조에 `삭제한 댓글은 복구할 수 없습니다.` (`DESIGN_DEFINED`).
- 확인 UI는 presentation state입니다. 과제의 soft-delete 의미를 바꾸지 않습니다(`ASSIGNMENT_DEFINED`).
- 현재 dialog의 compact geometry와 빨간 파괴적 확인은 P8-C의 의도적 차이입니다(retouch 기록, [`DESIGN_COMPARISON.md`](DESIGN_COMPARISON.md) "의도적 차이").

## 8. 기능 요구와 디자인 조정

이 section은 2026-09-29 초기 audit 시점에 기능 명세와 공식 Figma를 맞춰 본 결과입니다.

### 기존 기능 명세와 일치

- 375px mobile layout, 게시글 목록/상세/작성 흐름, 본인/타인 ownership variant, 좋아요 toggle 시각 표현, 댓글 수, 처음 2개 + 더보기 동작, 본인 글 action, 작성 validation, 삭제 확인, 작성 완료 toast가 모두 `ASSIGNMENT.md`와 일치합니다.
- audit한 디자인은 요구되는 soft-delete, 요청자, pagination, validation 의미와 모순되지 않습니다.

### Figma로 새로 확인한 시각 동작

- 정확한 375×778 frame 크기와 공유 page 영역.
- 본인/타인 글 variant와 좋아요 안 함/함 시각 variant.
- 명시적인 댓글 0개와 1개 state.
- 게시글 삭제 전의 bottom sheet와 게시글/댓글 삭제용 가운데 확인 dialog.
- 고정 하단 댓글 composer와 작성 action.
- Toast 문구, 크기, 위치.
- 작성 action의 비활성/활성 처리.

### 초기 audit에서 blocker로 잘못 분류했다가 확인된 항목

초기 audit 이전에는 local `.fig`가 없어 아래 항목을 blocker로 두었지만, 공식 Figma URL로 확인할 수 있었습니다.

- 공식 디자인 source 사용 가능 여부.
- 화면 구성, 간격 pattern, typography hierarchy, 핵심 색, 관찰한 radius, floating action, sheet/dialog 구조, toast 위치, form의 비활성/활성 state.

### Figma에는 있지만 과제 범위 밖인 항목

- 댓글 수정(`수정`), 답글(`답글쓰기`), 신고(`신고하기`), 댓글별 좋아요, `첫 댓글로 이동`, BEST/badge 동작, 인기 content section.
- 이 요소들은 디자인에는 있지만 과제 범위 밖이므로 구현 요구사항, database field, API endpoint를 만들지 않습니다. 미구현 TODO가 아닙니다.

### 과제가 요구하지만 디자인이 명시하지 않은 항목

- Loading, 요청 오류, pagination pending/끝, mutation 실패 state.
- Infinite scroll 방식과 cursor 표현.
- 기존 내용을 채운 별도 수정 화면 state.
- 권한 없는 직접 API 동작과 삭제된 resource의 HTTP 동작.
- 375px 대상 밖의 responsive 동작.

이 항목들은 해당하는 대로 `PRODUCT_DECISION` 또는 `ARCHITECTURE_DECISION`으로 다룹니다.

## 9. P2 당시 Backend 영향

P2 당시 판단: 디자인 audit는 새 backend endpoint나 response field를 만들지 않습니다. 현재 backend도 과제의 9개 API와 `GET /health`만 제공하며, 디자인 전용 endpoint는 추가되지 않았습니다.

- 본인 글과 다른 사용자 글의 action 차이는 요청자 기반 ownership과 backend 권한 확인을 뒷받침합니다(`ASSIGNMENT_DEFINED`). 기존 `author`와 요청자 판별로 충분하며, 디자인 전용 `isOwner` field는 필요 없습니다.
- 다른 사용자 글의 본인 댓글 삭제는 독립적인 댓글 ownership 확인을 뒷받침합니다(`ASSIGNMENT_DEFINED`).
- 좋아요 안 함/함 variant는 요청자별 `isLiked`와 `likeCount`를 뒷받침하며, 기존 게시글·toggle 계약으로 충분합니다.
- 댓글 state는 정확한 `commentCount`, 댓글 `total`, 삭제 댓글 제외, 범위가 정해진 최신순 pagination을 뒷받침합니다. 기존 계약과 architecture가 정한 cursor로 충분합니다.
- 게시글/댓글 확인 overlay는 frontend state이며 DELETE 계약은 바뀌지 않습니다.
- 디자인에만 있는 답글, 신고, 댓글 수정, 댓글별 좋아요, 인기 content는 P2에서 추가하지 않기로 했습니다(이후에도 추가하지 않음).

## 10. Loading, Empty, Error, 접근성 결정

이 표는 초기 audit에서 공식 Figma가 정의하지 않아 구현이 정해야 했던 영역을 기록합니다. 이후 구현에서 모두 `PRODUCT_DECISION`으로 정했습니다(§7의 phase 표, §12).

| Screen | Figma 근거 | 구현이 정해야 했던 영역 |
| --- | --- | --- |
| SCREEN-001 | 채워진 목록만 | Loading, empty, error, 다음 page pending, 목록 끝 (`PRODUCT_DECISION`) |
| SCREEN-002 | 댓글 0/1/여러 개, 좋아요 안 함/함, 삭제 overlay | 상세/댓글 error, 더보기 pending, mutation pending/실패 (`PRODUCT_DECISION`) |
| SCREEN-003 | 빈-유효하지 않음, 채워진-유효함 | 제출 중과 제출 오류 표시 (`PRODUCT_DECISION`) |
| SCREEN-004 | 공유 작성 구조만 | 채우기 loading/error와 권한 없음 표시 (`PRODUCT_DECISION`) |
| SCREEN-005 | 열린 sheet와 삭제 확인 | 닫기 motion과 삭제 실패 (`PRODUCT_DECISION`) |

접근성은 구현의 책임입니다.

- 의미 있는 button과 form control;
- icon만 있는 action의 accessible name;
- 유효하지 않은 제출의 실제 disabled state;
- keyboard로 focus할 수 있는 control과 sheet/dialog state의 focus 관리.

이 항목들은 `PRODUCT_DECISION`이며 Figma 근거를 주장하지 않습니다. 현재 구현에서는 공용 `ConfirmDialog`의 focus 가두기와 복귀, `PostActionMenu`의 Escape 시 trigger로 focus 복귀, sort button의 `aria-pressed`, icon button의 accessible name 등으로 적용되어 있고, 근거는 `docs/VERIFICATION.md`에 있습니다.

## 11. 추적성

SCREEN-001부터 SCREEN-006까지는 기존 기능 의미를 유지합니다. 위의 Figma frame ID가 각 시각 state의 디자인 근거입니다. 충돌하면 `ASSIGNMENT_DEFINED` 동작이 우선하고, `DESIGN_DEFINED` 값은 presentation을 안내하며, 정해지지 않은 product state는 `PRODUCT_DECISION`, API·state·pagination·권한 방식은 `ARCHITECTURE_DECISION`입니다.

## 12. P8-C 의도적 UX 보정

이 section은 현재 구현 기준입니다. `design/design-retouch/`에는 공식 Figma 근거가 아니라 의도적인 `PRODUCT_DECISION` reference가 있습니다. 그 reference가 `design/design-export/`와 의도적으로 다른 곳에서는, 구현에 한해 위의 목록·상세·작성 값을 대체합니다. 나머지는 공식 값을 유지합니다. 근거와 detail은 [`design/design-retouch/readme.md`](../design/design-retouch/readme.md)에 있습니다. 이 차이들은 V-081 전에 문서화되어 있었고, V-081에서 `INTENTIONAL_DEVIATION`으로 분류되었습니다.

- **SCREEN-001 목록:**
  - row는 제목 → preview → metadata 순서이며, 제목 위 작성자 row를 없앴습니다.
  - row는 retouch의 98px가 아니라 108px입니다(공식은 118px). 시각 검토에서 98px가 너무 빽빽해 간격만 늘렸고(14/6/13/14), type 크기는 그대로입니다.
  - Metadata는 [18px avatar + 작성자, 최대 132px] [댓글 · 좋아요] … [시간]입니다.
  - count는 두 reference처럼 천 단위 구분 기호를 씁니다.
  - app title은 가운데 정렬이고 header에 1px `#EAECEE` divider가 있습니다.
  - FAB 채움은 두 reference와 같은 `#C2102F`입니다.
  - pagination의 마지막 항목 뒤에만, 기존 하단 FAB 여백 앞에 가운데 정렬된 inline `↑ 위로 가기` control이 나타납니다. 맨 위로 scroll하며, reduced motion을 요청하면 부드러운 motion을 쓰지 않습니다. 공식 Figma에 없는 control이므로 `PRODUCT_ADDITION`입니다.
- **SCREEN-002 상세:**
  - 가운데 정렬된 `메디큐브톡` app-bar title.
  - y=78의 24px 작성자 row가 본인 글 `⋮`(40×40 target)를 담습니다.
  - 제목 → 본문.
  - footer는 왼쪽에 좋아요 action, 오른쪽에 12/16 날짜를 둡니다.
  - 다른 사용자 글은 `⋮` 없이 같은 geometry를 유지합니다.
- **SCREEN-003 작성:** 사용자가 제공한 `design/design-retouch/png/메디큐브톡 작성.png`, `design/design-retouch/png/메디큐브톡 작성(내용O).png`, `design/design-retouch/svg/메디큐브톡 작성.svg`, `design/design-retouch/svg/메디큐브톡 작성(내용O).svg`는 1px `#EAECEE` 하단 divider가 있는 48px app bar, 기존 40×40 뒤로 가기 target, 가운데 정렬 `메디큐브톡` title, 같은 너비의 오른쪽 spacer를 더합니다. 기존 form geometry와 고정 action은 그대로입니다. 이는 `INTENTIONAL_DEVIATION / UX_CONSISTENCY`이며 공식 작성 frame fidelity 주장이 아닙니다.
- **SCREEN-004 수정:** 수정 전용 Figma나 retouch reference는 없습니다. 수정은 공유 작성 shell을 재사용하므로 가운데 app title을 `PRODUCT_DECISION / UX_CONSISTENCY`로 물려받으며, 수정 전용 Figma fidelity를 주장하지 않습니다.
- **SCREEN-005 게시글 action:** P8-C는 공식 bottom action sheet(830:139)를 게시글 `⋮` 4px 아래에 붙는 non-modal popover(`PostActionMenu`)로 바꿨습니다.
  - 너비 144px, 44px row 두 개(수정하기 / 삭제하기)이며, 오른쪽 끝은 x=359입니다.
  - scrim과 `취소` row가 없습니다. 바깥 누르기, Escape(focus는 `⋮`로 돌아감), 두 action 중 하나가 닫습니다.
  - 근거: command가 두 개뿐이므로 trigger 옆에 두면 action 근접성이 좋아지고 불필요한 modal 무게가 사라집니다.
  - 삭제는 여전히 scrim이 있는 modal 확인 dialog를 엽니다.
  - §6의 sheet 값과 §7의 P6 action sheet 값은 공식 디자인과 P6 당시 구현을 설명하며, P8-C 구현이 아닙니다.
- **상호작용:**
  - 전체에 눌림 state.
  - 게시글 action popover, dialog, toast에 120–180ms entrance motion(opacity와 transform만; 닫기는 즉시).
  - `prefers-reduced-motion`이면 motion을 끕니다.
  - 댓글 삭제 target은 text를 옮기지 않고 커졌습니다.
- **보류:** 요청자가 댓글을 단 게시글의 목록 댓글 icon accent. 검토했지만 보류했습니다. 요구사항이 아니며 TODO가 아닙니다.
  - 목록 API는 요청자별 참여 state(예: `hasCommented`)를 노출하지 않습니다.
  - `commentCount > 0`은 누군가 댓글을 달았다는 뜻이지 요청자가 달았다는 뜻이 아닙니다.
  - 정확히 보이려면 API 변경이나 row별 댓글 조회(N+1)가 필요해, 가짜 state도 API 확장도 받아들이지 않았습니다.
- **댓글 heading(code 수준 보정, reference image 없음):** `댓글`은 Bold 16/24 `#292828`을 유지하고, count는 4px 간격의 Medium 14/20으로 낮췄습니다. count 색은 §6.3의 읽기용 보조색 `#6E7785`입니다(디자인 source 값 `#979EA9`를 접근성 조정). 49px heading box와 댓글 위치는 그대로입니다.

## 13. 게시글 탐색 Toolbar (PRODUCT_DECISION)

이 toolbar는 공식 Figma에도 retouch image에도 없습니다. 의도적인 product 추가이므로 V-081은 이를 fidelity가 아니라 차이로 기록했습니다(`INTENTIONAL_DEVIATION`). API와 cache 설계는 `docs/ARCHITECTURE.md`의 "Post Discovery 반영 (검색 + 정렬)"에 있습니다.

- **배치:** 48px `메디큐브톡` title 바로 아래의 44px row.
  - title은 scroll되어 사라지고, 이 row만 `position: sticky; top: 0`이며 흰 surface와 1px `#EAECEE` 하단 divider를 가집니다. 목록이 sticky 띠 두 개에 약 92px를 쓰지 않게 합니다.
  - 목록 row는 이제 y=92에서 시작하며(이전 56), 108px row rhythm은 그대로입니다.
- **일반 mode:** `최신순 좋아요순 │ 🔍 검색`.
  - 정렬은 목록의 지속 state이므로 하나의 compact group으로 먼저 옵니다. label은 18px 간격이고 `최신순`은 16px inset에서 시작합니다. pill이나 dropdown 없는 일반 text button 두 개입니다.
  - 짧은 16px `#EAECEE` divider가 이어집니다.
  - 그다음 search button 하나(accessible name `게시글 검색`)가 row의 나머지를 오른쪽 끝까지 채웁니다. icon과 `검색`은 divider 13px 뒤에 왼쪽 정렬되고, 남는 빈 공간도 같은 button이므로 추가 tab stop이나 보이는 box 없이 넓은 hit area를 가집니다.
  - 눌림 feedback은 icon과 label만 흐리게 합니다. keyboard focus는 search target 전체에 ring을 보입니다.
  - 선택된 정렬은 `#292828` SemiBold, 다른 정렬은 Medium이며 §6.3에 따라 읽기용 보조색 `#6E7785`입니다. 빨강은 쓰지 않습니다(identity와 파괴적 역할에만 씀).
  - 모든 control은 44px 높이 target을 가지며, 정렬에는 `aria-pressed`가 있습니다.
- **검색 mode:** 같은 평평한 44px row가 `← 🔍 게시글 검색` state로 바뀝니다.
  - ←(`검색 닫기`), 장식용 보조색 search icon, page inset까지 row의 나머지를 채우는 border 없는 투명 input(`게시글 검색`, 자동 focus)이 있습니다.
  - pill, field box, × button은 없습니다.
  - input text는 16px이라 iOS가 focus 시 page를 확대하지 않습니다.
  - field에 focus가 있으면 별도 ring 대신 row의 하단 divider가 읽기용 보조색 `#6E7785`로 진해집니다.
  - ←와 Escape만 나가는 방법입니다. query를 지우고 filter 없는 목록을 복원하며, focus는 `검색`으로 돌아갑니다.
  - 고른 정렬은 결과에 계속 적용되고, 닫은 뒤에도 선택된 상태입니다.
- **동작:**
  - 입력은 300 ms debounce되며, Enter는 즉시 확정합니다.
  - 정렬이나 검색이 바뀌면 새 목록을 첫 page부터 보여 주고, toolbar가 고정된 상태면 그 아래로 scroll을 되돌립니다.
  - 빈 검색 결과는 `검색 결과가 없습니다.`이며, 빈 커뮤니티 문구와 구분됩니다.
