# 구현 계획

이 문서는 과제 구현 과정의 역사적 계획과 phase 진행 순서를 보존합니다. phase, 위험, 결정을 어떤 순서로 다뤘는지 기록할 뿐이며, 현재 해야 할 일의 기준 문서가 아닙니다. 현재 상태의 기준 문서는 다음과 같습니다.

- 현재 구조: `docs/ARCHITECTURE.md`
- 현재 요구사항과 status: `docs/REQUIREMENTS.md`
- 실행 근거: `docs/VERIFICATION.md`
- 성능 측정: `docs/PERFORMANCE.md`
- UI 계약: `docs/UI_SPEC.md`
- 최종 V-081 디자인 비교: `docs/DESIGN_COMPARISON.md`
- 결정 과정: `docs/ENGINEERING_NOTES.md`

**현재 상태 참고**(현재 `main` `25d22296ca3c9d9a433e8de3817818f5fc8e6d46` 기준, 정확한 상태는 `docs/REQUIREMENTS.md`와 `docs/VERIFICATION.md`):

- Engineering 검증은 완료되었습니다(`ENGINEERING_VERIFICATION_COMPLETE`).
- V-081은 2026-10-04 최종 audit에서 `PASS`였고 `ACCIDENTAL_MISMATCH`는 `NONE`이며, `UI-004`는 `VERIFIED`입니다.
- 요구사항 132개 중 130개가 `VERIFIED`이고, repository 밖 외부 제출 단계 `SUB-003`과 `SUB-006`이 `TODO`로 남아 있습니다(`PENDING_EXTERNAL_SUBMISSION_STEPS`).

## 당시 최종 상태 Snapshot — 2026-10-04

*시점:* 이 block은 2026-10-04 00:46(+0900) commit `48a3439`에서 작성되었고, 같은 날 PR #51(`04dd668`)로 merge되었습니다. 최종 V-081 audit(`0ee713f`, 2026-10-04 17:14, PR #52)보다 앞섭니다. 아래 내용은 그 시점에 맞는 snapshot입니다.

- P0–P7 구현 phase는 `COMPLETE`입니다.
- P8 engineering 검증은 `COMPLETE WITH DOCUMENTED GAP`입니다. runtime, Testcontainers, 동시성, CI, 선택한 Playwright gate가 통과했습니다. 당시 V-081 공식 Figma 비교는 `NOT_RUN`이었으므로 pixel 수준 fidelity는 주장하지 않았습니다.
- P9 repository 문서와 PR workflow는 `COMPLETE`입니다. 최종 문서 정리는 이 historical plan과 별도로 관리합니다.
- 당시 최종 문서 정리의 base `main`은 PR #50을 포함한 `3c902dc9126b2eb2ec0713844b3d7cbec237e80f`였습니다.
- repository 밖 단계가 남아 있었습니다: `APRCORPORATION`을 collaborator로 추가하고 최종 URL을 메일로 보내기.

*이후:* 같은 날 최종 V-081 audit(baseline `04dd668`)가 `PASS`했고 `ACCIDENTAL_MISMATCH`는 `NONE`이었으므로, 당시 문서화한 gap은 해소되었습니다. 이 audit는 pixel-difference threshold를 쓰지 않았으므로 pixel-perfect를 주장하지 않습니다. 외부 제출 단계 두 개는 여전히 `TODO`입니다. 현재 상태는 `docs/VERIFICATION.md`와 `docs/REQUIREMENTS.md`를 참고하세요.

### Historical status 안내

- 아래 phase section의 status, 날짜, 값은 그 시점의 기록입니다. 날짜, branch 이름, test 수, 상태를 의도적으로 그대로 남겼으며, 현재 지시가 아닙니다. 현재 값은 `docs/ARCHITECTURE.md`, `docs/REQUIREMENTS.md`, `docs/VERIFICATION.md`에 있습니다.
- 예: P4 page size 10, P5A 댓글 continuation 20, P6 bottom sheet, P6 당시 cache 동작, 당시 V-081 `NOT_RUN`, 당시 test 수와 branch/SHA는 모두 historical 값입니다.
- P8-C UI/UX 보정과 post 검색/좋아요순 정렬은 review 후 merge되었습니다.
- 첫 기동 readiness blocker는 해결되었습니다. `/health`는 seed transaction이 commit될 때까지 503을 유지합니다.
- 당시 V-081은 local PNG/runtime 근거로 완료를 추정한 작업이 아니라 명시적인 검증 gap으로 남겼습니다. *(이후 위 "이후" 메모대로 `PASS`.)*

### P7 Backlog (기록만, 구현 안 함)

- P7-A 완료: SVG 기반 목록 hierarchy와 중립 icon; `post.isLiked`는 이제 기존 `#FF8DA1` accent를 하트에만 `UX_CONSISTENCY`로 씁니다.
- 요청자가 댓글을 단 글의 목록 accent는 목록 API에 요청자별 댓글 참여 상태가 없어 보류했습니다. 요구사항이 아니며 TODO도 아닌, 보류한 product 아이디어입니다(현재도 `docs/UI_SPEC.md` §12의 "보류").
- P7-B 완료: 375px browser 확인에서 명확한 HIGH/MEDIUM 불일치나 P7-A 회귀를 찾지 못했습니다. 당시 V-081은 `NOT_RUN`이었고, local PNG/SVG 근거는 새 Figma 비교가 아니었습니다. *(이후 2026-10-04 최종 audit에서 `PASS`.)*

### Performance Audit Gate 상태

- Audit `COMPLETE`(측정만; product code, schema, migration, dependency, API 변경 없음).
- Seed: 읽을 수 있는 post 39개; post당 읽을 수 있는 댓글 median 7, 최대 1,411 (post #2 ≈73.3 %).
- 빈 상태 seed 구간 median 911 ms, 새 volume마다 1회. Read endpoint median 3.5–8.5 ms. 댓글 1,411개 전체 순회는 요청 72개 / statement 216개 / median 523 ms.
- 고치기로 고른 구조적 비효율: 당시 실행 계획에서 keyset cursor가 index 경계가 아니라 `Filter`로 평가되었습니다(깊은 page가 선형으로 scan). page마다 반복되는 EXISTS + COUNT는 존재하지만 병목이 아니므로 그대로 둡니다.
- 선택한 최적화 `COMPLETE`: row-value 조건이 이제 `Index Cond`입니다. post #2의 깊은 page buffer가 결과는 같은 채 42에서 3으로 줄었습니다. API latency는 noise 범위 안에서 변하지 않았습니다.

### P0 완료 Update

- 과제/지시 문서, 생성되지 않은 repository tree, backend/frontend manifest와 source, Compose 설정, public asset, 유효한 seed JSON의 정적 audit를 완료했습니다.
- 확인한 starter 계약, seed 사실, 없는 디자인 asset, ADR 조정을 `docs/ARCHITECTURE.md`에 기록했습니다. *(P0 당시에는 repository 안의 `.fig`와 디자인 asset이 없었습니다. 이후 공식 Figma URL로 inspection이 가능해졌습니다. 아래 "Figma Design Audit 상태" 참고.)*
- P0 완료 기준을 충족했습니다: product code, dependency, package 설치, schema, compose 동작, seed 데이터, README, 수정 불가 `/dev-user` 변경이 없었습니다.
- Historical 기록: P0는 완료되었습니다. 그 audit 상태는 현재 phase가 아닙니다.

### Architecture Review 상태

- Status: `COMPLETE`
- 최종 결정은 `docs/ARCHITECTURE.md`에 기록되어 있습니다: Spring JDBC/JdbcClient, Flyway와 `ApplicationRunner` seed lifecycle, identity ID, boolean soft delete, base-count 좋아요, keyset pagination, TanStack Query v5, 좁은 client boundary, CSS Modules, IntersectionObserver, native 상대 시간, backend가 최종 판단하는 validation/ownership.
- 당시 미결로 남은 영향 큰 architecture 결정은 없었습니다. 그 결정들의 runtime 검증은 각 구현 phase로 미뤘습니다. 이후의 수정(예: 첫 page SSR)은 `docs/ARCHITECTURE.md`를 참고하세요.
- Historical 기록: Architecture Review는 완료되었습니다. 그 결정 gate는 진행 중인 작업이 아닙니다.

### UI 명세 상태

- Status: `COMPLETE_FUNCTIONAL`, `DESIGN_AUDITED`
- 공식 source: Figma file `Ee1gp5NC6G1sQIvssuu4Sr`, Page 1 (`0:1`), 확인한 375×778 frame 12개를 inspection했습니다.
- `docs/UI_SPEC.md`가 과제 동작을 실제 목록, toast, 본인/타인 상세, 좋아요 함/안 함, 댓글 수, action sheet, 삭제 확인, 작성 form state와 맞췄습니다.
- 디자인에만 있는 범위 밖 요소는 과제나 P2 API 계약을 넓히지 않습니다. Loading/error 화면은 product decision으로 남습니다.
- 당시 UI 명세 gate를 닫던 시점의 상태: P1, P2, P3, P4, P5A는 완료되었고 P5B는 시작 전이었습니다. *(이후 P5B–P9 모두 완료.)*

### Figma Design Audit 상태

- Status: `COMPLETE`
- 공식 Figma source는 `AVAILABLE`입니다. repository 안의 `.fig` file은 여전히 없지만 디자인 inspection을 더 이상 막지 않습니다.
- 이 audit에는 backend, frontend, schema, dependency, Compose, seed, 과제 source 변경이 포함되지 않습니다.
- P2 영향 review에서 새 endpoint나 response field 요구사항을 찾지 못했습니다.
- 이것은 `docs/UI_SPEC.md`를 쓰기 위한 초기 Figma 디자인 audit입니다. 최종 구현과 공식 디자인을 비교하는 V-081은 별개이며, 2026-10-04에 수행되었습니다.

### P2A 상태

- Status: `COMPLETE`
- `GET /api/posts`, `GET /api/posts/{postId}`, `GET /api/posts/{postId}/comments`와 요청자 판별, 엄격한 cursor pagination, read DTO/error 계약, 집중 test만 구현했습니다.
- 실제 PostgreSQL acceptance가 읽을 수 있는 post 39개 전체, 읽을 수 있는 댓글 page 전체, seed 삭제 resource 제외, 요청자별 `isLiked`, 잘못된 pagination 입력을 다뤘습니다.
- P2B Status: `COMPLETE`; 필수 mutation route 전체, 수동 validation, service layer ownership, transaction, soft delete, JSON error가 자동 test와 실제 PostgreSQL acceptance를 통과했습니다.

### P2B 상태

- Status: `COMPLETE`
- post 작성/수정/soft delete, 요청자별 좋아요 toggle, 댓글 작성/soft delete, 명시적인 Unicode 인식 validation, 일관된 JSON error 처리를 구현했습니다.
- 실제 PostgreSQL acceptance가 ownership 분리, 파생 count, soft delete 후 남는 row, 삭제 resource 거부, P2A read 회귀, 깨끗한 seed baseline 복원을 다뤘습니다. *(동시성 test와 method별 lock 정리는 이후 PR #44 등에서 추가되었습니다. `docs/VERIFICATION.md` F-09.)*

### P3 상태

- Status: `COMPLETE`
- TanStack Query v5 provider boundary, credential을 싣는 공용 API client, transport type, endpoint 함수 9개, query-key factory, route 계약, 상대 시간 utility, `DESIGN_DEFINED` CSS token을 추가했습니다.
- product 화면, feature hook, P4 code는 추가하지 않았습니다. P3 당시 `app/page.tsx`는 starter placeholder로 남았고 `/dev-user`는 그대로였습니다. 실제 목록은 P4에서 구현했습니다.
- `npm ci`, `tsc --noEmit`, `next build`, healthy Compose runtime, P2 read smoke로 검증했습니다. 당시 browser cookie 흐름(`START-003`, `V-012`)은 P4/P5 또는 P8 acceptance로 남겼습니다.

### P4 상태

- Status: `COMPLETE`
- `/`가 `useInfiniteQuery`(page size 10, cursor는 `pageParam`)와 `IntersectionObserver` sentinel로 post 목록을 render하며, 상대 시간, 결정적 avatar, 상세 link, 작성 FAB, loading/empty/error/다음 page loading/다음 page error state를 CSS Modules로 갖습니다. *(이후 performance audit에서 page size는 15/15로 바뀌었습니다.)*
- 375×778 Chromium에서 검증(48/48 checks): 읽을 수 있는 post 39개에 대한 10/10/10/9 pagination, 중복 없음, API 순서, 끝 이후 요청 없음, `/dev-user` cookie 흐름, `UI_SPEC.md` geometry/typography. 상세와 작성 route는 link만 있고 P5/P6에서 만들었습니다. 당시 V-080/V-081은 `NOT_RUN`이었습니다.

### P5A 상태

- Status: `COMPLETE`
- `/posts/[postId]`가 post 상세, 상세와 목록 cache에 동기화되는 non-optimistic 좋아요 toggle, 최신순 댓글을 render합니다: 처음 최대 2개(`limit=2`), total >= 3일 때만 `댓글 더보기`, 더보기 이후에만 sentinel continuation(`limit=20`). Loading/not-found/error와 continuation state를 처리합니다. *(P5A 당시 continuation은 20이었고, 이후 performance audit에서 30으로 바뀌었습니다.)*
- 375×778 Chromium에서 검증(63/63 checks), 같은 session 재방문 회귀(12/12), P4 회귀 suite(48/48). `DETAIL-001`–`DETAIL-008`은 `VERIFIED`; V-060/V-061 `PASS`.
- Review 수정: 댓글 query가 `gcTime: 0`을 써서 전체 reload 없는 재방문이 처음 2개 댓글부터 다시 시작합니다. cache된 continuation page는 더 이상 더보기 전에 다시 가져오거나 더보기 시 한꺼번에 노출되지 않습니다.
- P5A 당시 범위 밖이라 render하지 않은 것: 댓글 composer, 본인 댓글 삭제와 그 확인(P5B); 본인 글 메뉴, 수정, 삭제, 작성(P6).

### P5B 상태

- Status: `COMPLETE`
- 고정 composer가 댓글을 만듭니다. 본인 댓글(cookie 요청자 기준, post 소유자와 무관)에는 확인 dialog와 함께 `삭제`가 보입니다. 모든 mutation은 서버 확인 후 반영되어 댓글 목록, 그 `total`, 상세 `commentCount`, cache된 목록 count를 동기화하며, 같은 tick 중복을 막는 in-flight guard가 있습니다.
- 375×778 Chromium에서 검증(P5B checks 52/52): 경계 0→1→2→3과 3→2→1→0, 27→28, 펼친 상태의 작성, continuation과 cursor 경계 삭제, 목록 refetch를 막은 목록 동기화, POST/DELETE 실패 경로, `/dev-user` 사용자 간 ownership, backend 403. 회귀: 재방문 12/12, P4 48/48, P5A 63/63, keyset 순회/tie. `DETAIL-009`/`DETAIL-010`은 UI 근거를 갖고 `DETAIL-011`은 `VERIFIED`.
- 다시 seed해 baseline 복원: posts 42/3, comments 2,076/81, 좋아요 0.

### P6 상태

- Status: `COMPLETE`
- 작성(`/posts/new`), 수정(`/posts/[postId]/edit`, 공유 form, 정확한 채움), 본인 글 action sheet(P6 당시 bottom sheet `PostActionSheet`), 게시글 삭제 확인이 있습니다. Validation은 backend와 같습니다(Java `isBlank`, 20 code point). mutation은 서버 확인 후 반영되어 목록과 상세 cache를 동기화합니다. 작성 toast는 한 번만 보입니다. 다른 사용자 post에는 메뉴와 수정 form이 없습니다. *(action sheet는 이후 P8-C에서 `PostActionMenu` popover로 바뀌었습니다.)*
- 375×778 Chromium에서 검증(74/74), 중간 row와 cursor 경계 row의 cache된 목록 삭제, `/dev-user` 사용자 간 ownership, 모든 실패 경로 포함. V-012, V-030, V-031, V-032, V-040, V-080은 `PASS`. 당시 V-081은 `NOT_RUN`이었습니다.
- Deletion Policy Hardening(`4e53dbd`): 확인한 삭제는 이제 post가 로드되지 않았어도 모든 cached 목록 total을 줄입니다(당시 목록은 page size만 다른 같은 collection). soft delete model(휴면 하위 데이터, 403보다 먼저 404, 반복 DELETE 404, parent/child mutation 직렬화)을 backend 변경 없이 검증했습니다(28/28). *(이후 검색·정렬이 추가되면서 현재 cache 규칙은 로드된 row를 모든 목록에서 제거하고, total은 filter 없는 목록과 row가 로드된 목록에서만 줄이며, 검색 목록은 invalidate합니다. `docs/ARCHITECTURE.md` "Deletion Consistency Model".)*

---

## Phase 진행 규칙

당시 작업 진행 규칙입니다. 각 phase마다:

1. 관련 기존 code와 요구사항을 확인합니다.
2. 미결 architecture 결정을 확인합니다.
3. 현재 phase만 구현합니다.
4. 관련 자동 확인을 실행합니다.
5. `VERIFICATION.md`의 관련 acceptance 시나리오를 실행합니다.
6. 근거와 함께 요구사항 status를 갱신합니다.
7. 바뀐 file, 위험, 남은 작업을 기록합니다.
8. 명시적으로 계속하라는 지시가 없으면 다음 phase 전에 멈춥니다.

phase는 완료 기준을 충족해야만 완료입니다.

---

## P0 — Repository Audit

### 목표

product 동작을 바꾸지 않고 starter repository를 이해하고, 과제 요구사항을 기존 codebase에 대응시킵니다.

### 작업

- `docker-compose.yml`을 확인합니다.
- backend build/설정을 확인합니다.
- 기존 `/health` 구현을 확인합니다.
- 기존 CORS 설정을 확인합니다.
- frontend App Router 구조를 확인합니다.
- `frontend/app/dev-user/` 내용을 확인하고 수정 불가로 다룹니다.
- `seed/seed.json` 구조와 data type을 확인합니다.
- 제공된 public asset과 font를 확인합니다.
- 공식 Figma asset이 있는지 확인합니다.
- 기존 starter 동작을 requirement ID에 대응시킵니다.
- P1/P2/P3 전에 필요한 architecture 결정을 찾습니다.

### 완료 기준

- Starter 계약이 문서화되어 있습니다.
- Seed 형태를 이해했습니다.
- 제공된 필수 동작을 실수로 바꾸지 않았습니다.
- 없는 asset/blocker가 기록되어 있습니다.
- 필요한 architecture 결정이 나열되어 있습니다.

### 상태

`COMPLETE`

---

## Architecture Decision Gate — P1/P3 이전

### 목표

구현 전에 architecture 조사를 repository에서 확인한 결정으로 바꿉니다.

### 근거 기준

영향이 큰 선택마다:

1. 제약을 만드는 과제/starter 사실을 인용합니다;
2. framework/database 동작이 중요하면 1차 기술 문서를 인용합니다;
3. 고른 선택지를 기록합니다;
4. 현실적인 대안을 하나 이상 기록합니다;
5. 고른 선택지가 이 과제에 맞는 이유를 기록합니다;
6. 수용하는 trade-off를 기록합니다;
7. repository에 달린 미결 주장은 `OPEN` 또는 `PROVISIONAL`로 표시합니다.

### 현재 기록

`docs/ARCHITECTURE.md`를 보세요.

### 완료 기준

- Persistence 접근 결정이 repository와 호환됩니다.
- Migration/schema 전략이 repository와 호환됩니다.
- Seed 초기화 lifecycle이 결정되었습니다.
- Pagination 계약이 고정되었습니다.
- Frontend server-state 전략이 고정되었습니다.
- Styling 전략이 starter와 호환됩니다.
- 모든 결정이 추적 가능한 근거와 명시적 trade-off를 갖습니다.

### 상태

`COMPLETE`

---

## P1 — Database & Seed 기반

### 목표

지속적인 persistence model과 idempotent seed 초기화를 만듭니다.

### 요구사항 범위

- `SEED-*`
- `DEL-*`
- 관련 `USER-*`
- 관련 `ENV-*`

### 작업

- DB schema를 확정합니다.
- post persistence를 구현합니다.
- comment persistence를 구현합니다.
- 좋아요 persistence/state model을 구현합니다.
- soft delete 표현을 구현합니다.
- seed import를 구현합니다.
- seed ID를 보존합니다.
- `deletedPostIds` / `deletedCommentIds`를 적용합니다.
- 재시작 idempotency를 보장합니다.
- 삭제된 댓글이 count에 영향을 주지 않게 합니다.

### 완료 기준

- Database가 성공적으로 시작됩니다.
- Seed record를 조회할 수 있습니다.
- Seed ID가 보존됩니다.
- 반복 기동이 데이터를 중복시키지 않습니다.
- 삭제된 seed entity는 저장되어 있지만 일반 read에서 제외됩니다.

### 상태

`COMPLETE`

---

## P2 — Backend Domain & API

### 목표

ownership, validation, pagination, 일관된 삭제 resource 동작을 갖춘 과제 API를 구현합니다.

### 요구사항 범위

- `USER-*`
- `DEL-*`
- `API-*`
- 관련 `EDIT-*`

### 작업

- header/cookie/기본값으로 요청자를 판별합니다.
- post 목록/상세/작성/수정/삭제를 구현합니다.
- 댓글 목록/작성/삭제를 구현합니다.
- 좋아요 toggle을 구현합니다.
- ownership 강제를 구현합니다.
- 입력 validation을 구현합니다.
- pagination을 구현합니다.
- 필수 JSON 형태를 반환합니다.
- 삭제되었거나 없는 resource를 일관되게 처리합니다.
- 중요한 business rule에 backend test를 추가합니다.

### 완료 기준

- 필수 API route가 모두 있습니다.
- 응답 계약이 과제와 같습니다.
- Ownership 제한이 backend에서 강제됩니다.
- Pagination이 전체 collection을 가져오지 않습니다.
- 삭제된 resource는 없는 것처럼 동작합니다.

### 상태

`COMPLETE`

---

## P3 — Frontend 기반

### 목표

API 통신, server state, routing, 공유 UI 동작을 위한 단순한 frontend 구조를 세웁니다.

### 요구사항 범위

- `START-*`
- `UI-*`
- state/styling에 대한 architecture 결정

### 작업

- server-state 전략을 확정합니다.
- styling 전략을 확정합니다.
- 필요한 곳에 credential을 싣는 API client를 구현합니다.
- 공유 응답/요청 type을 정의합니다.
- route 구조를 정의합니다.
- 정당한 경우에만 재사용 UI primitive를 정의합니다.
- 상대 시간 utility를 추가합니다.
- `/dev-user`를 그대로 둡니다.

### 완료 기준

- API 호출이 제공된 cookie 흐름으로 현재 user를 판별할 수 있습니다.
- 공유 요청/응답 처리가 안정적입니다.
- 어떤 product 화면도 중복된 API logic에 의존하지 않습니다.

### 상태

`COMPLETE`

---

## P4 — Post List

### 목표

최신순 infinite scroll이 있는 375px post 목록 경험을 구현합니다.

### 요구사항 범위

- `LIST-*`
- 관련 `UI-*`
- 관련 `API-001`, `API-031`, `API-032`

### 작업

- post card를 render합니다.
- 상대 timestamp를 render합니다.
- 최신순 paginated loading을 구현합니다.
- infinite scroll을 구현합니다.
- 작성 이동을 구현합니다.
- loading/empty/error state를 구현합니다.
- 사용할 수 있으면 공식 디자인에 맞춥니다.

### 완료 기준

- 첫 page가 올바르게 render됩니다.
- 추가 page는 필요할 때만 로드됩니다.
- Pagination 중 중복 항목이 생기지 않습니다.
- 작성 action이 올바르게 이동합니다.

### 상태

`COMPLETE`

---

## P5 — Post Detail, Comments & Likes

### 목표

post 상세, 요청자별 좋아요, 처음 댓글 2개 동작, 댓글 이어 읽기 흐름을 구현합니다.

### 요구사항 범위

- `DETAIL-001`부터 `DETAIL-011`
- 관련 `API-*`

### 작업

- post 상세를 render합니다.
- 좋아요 toggle을 구현합니다.
- 처음에는 댓글을 최대 2개만 로드합니다.
- total >= 3일 때만 "더보기"를 보입니다.
- "더보기" 이후 infinite scroll로 이어갑니다.
- 댓글 작성을 구현합니다.
- 본인 댓글 삭제를 구현합니다.
- mutation 후 count/목록 상태를 동기화합니다.

### 완료 기준

- toggle 후 좋아요 상태/count가 일관됩니다.
- 처음 댓글 제한이 지켜집니다.
- "더보기" 표시 logic이 올바릅니다.
- Infinite scroll은 "더보기" 이후에만 시작합니다.
- 댓글 삭제가 목록과 `commentCount`를 모두 갱신합니다.

### 상태

`COMPLETE` — P5A(`DETAIL-001`–`DETAIL-008`)와 P5B(`DETAIL-009`–`DETAIL-011` UI, mutation cache 동기화)가 완료되었습니다.

---

## P6 — Post Create / Edit / Delete

### 목표

post 작성과 ownership으로 제어되는 post 관리를 구현합니다.

### 요구사항 범위

- `DETAIL-012`부터 `DETAIL-020`
- `EDIT-*`
- 관련 `API-*`

### 작업

- 작성 화면을 구현합니다.
- 필수 field validation을 구현합니다.
- 제목 최대 길이 20을 강제합니다.
- 유효하지 않으면 완료 action을 비활성화합니다.
- 작성 성공 후 목록으로 이동합니다.
- 완료 toast를 보입니다.
- 수정에 작성 layout/흐름을 재사용합니다.
- 수정 시 기존 제목/내용을 채웁니다.
- 목록으로 돌아가는 본인 post 삭제를 구현합니다.
- 다른 user가 UI나 API로 수정/삭제할 수 없게 합니다.

### 완료 기준

- 작성/수정 validation이 요구사항과 같습니다.
- 작성 성공 시 toast를 보이고 목록으로 돌아갑니다.
- 수정 채움/저장이 동작합니다.
- 삭제가 목록으로 돌아갑니다.
- 다른 user의 mutation 시도는 실패합니다.

### 상태

`COMPLETE`

---

## P7 — Visual Integration & UX Polish

### 목표

과제 의미를 바꾸지 않고 구현된 흐름을 공식 디자인에 맞추고 UX edge case를 해결합니다.

### 요구사항 범위

- `UI-*`
- `LIST-*`, `DETAIL-*`, `EDIT-*`의 최종 시각 측면

### 작업

- 디자인 source의 공식 간격, typography, 색, radius, icon, sheet, 비활성 state를 적용합니다.
- 375px viewport를 확인합니다.
- bottom-sheet/메뉴 상호작용을 확인합니다.
- toast 위치/동작을 확인합니다.
- loading, empty, error state를 확인합니다.
- 의도적 디자인 차이/개선을 문서화합니다.

### 완료 기준

- 필수 흐름이 공식 디자인과 시각적으로 일관됩니다.
- 시각 작업으로 기능 회귀가 생기지 않았습니다.
- 의도적 차이는 모두 문서화되어 있습니다.

### 상태

`COMPLETE` — P7-A가 local SVG 근거로 목록 hierarchy를 고치고, 중립 목록 glyph를 `#DADEE3`로 맞췄으며, `post.isLiked` 하트 전용 accent를 `UX_CONSISTENCY`로 추가했습니다. 이어서 P7-B가 실제 375 CSS-px emulation에서 필수 화면 matrix 전체와 기능 smoke를 확인했고, 구체적 근거로 정당화되는 presentation 수정은 찾지 못했습니다. 이 phase snapshot 시점에 P8은 시작 전이었습니다.

---

## P8 — Full Integration & Acceptance Verification

### 목표

평가자가 실행할 법한 방식으로 application을 검증합니다.

### 요구사항 범위

- 모든 필수 ID
- 모든 release gate

### 작업

- `docker compose up`으로 빈 상태에서 시작합니다.
- `/health`를 확인합니다.
- frontend 접속을 확인합니다.
- 대체 port override를 확인합니다.
- 재시작 후 seed idempotency를 확인합니다.
- 모든 ownership 규칙을 확인합니다.
- soft delete를 확인합니다.
- pagination/infinite scroll을 확인합니다.
- 요청자별 `isLiked`를 확인합니다.
- 자동 test suite를 실행합니다.
- 최종 acceptance 시나리오를 실행합니다.

### 완료 기준

- 필수 요구사항이 모두 `VERIFIED`입니다.
- 모든 release gate 시나리오가 통과합니다.
- 제출을 막는 알려진 blocker가 남아 있지 않습니다.

### 상태

`COMPLETE WITH DOCUMENTED GAP`(P8 당시 종료 상태) — 동작과 engineering acceptance는 완료되었습니다. 당시 V-081 공식 디자인 비교는 `NOT_RUN`이었으므로 project는 pixel 수준 Figma fidelity를 주장하지 않았습니다.

- **P8-A(merge됨):** 첫 기동 readiness gap을 재현하고, health readiness가 seed transaction commit을 따르도록 바꿨으며, 빈 volume 기동, 대체 port, idempotency, starter 계약을 검증했습니다.
- **P8-B:** `79ebd67`에서 product 변경 없이 다시 검증했습니다.
  - 요청자 판별과 pagination;
  - 응답 형태;
  - 요청자별 좋아요;
  - 댓글 read와 mutation 흐름;
  - post validation, CRUD, ownership;
  - soft delete와 오류 경로;
  - 연속 375px end-to-end 경로;
  - backend test, typecheck, clean local과 no-cache Docker build.

  당시 동작(behavioral) 범위의 필수 요구사항은 모두 `VERIFIED`였습니다. 이는 과제 요구사항 전체가 `VERIFIED`라는 뜻이 아닙니다. 이후 phase에서 Testcontainers integration/동시성 suite, CI, dependency·clean-clone audit, commit된 Playwright 회귀 3개가 추가되었습니다. 당시 `UI-004`는 V-081을 실행하지 않아 열려 있었습니다.

*이후:* 2026-10-04 최종 V-081 audit(baseline `04dd668`)가 `PASS`, `ACCIDENTAL_MISMATCH` `NONE`으로 끝나 `UI-004`는 `VERIFIED`가 되었습니다(`docs/DESIGN_COMPARISON.md`, `docs/VERIFICATION.md` "Final V-081 Audit"). P8의 당시 status token은 기록으로 그대로 둡니다.

---

## P9 — README & Submission PR

### 목표

실제 구현에 기반한 평가자용 문서를 만들고 repository 제출 workflow를 완료합니다.

### 요구사항 범위

- `DOC-*`
- `SUB-*`

### 작업

- 실제 기술 선택을 문서화합니다.
- server-state 설계를 문서화합니다.
- frontend/backend 계층을 문서화합니다.
- seed 전략을 문서화합니다.
- test와 그 중요성을 문서화합니다.
- 실제 file/function을 짚습니다.
- 대안과 trade-off를 기록합니다.
- 의도적 UX 개선/차이를 기록합니다.
- 최종 PR을 만듭니다.
- `main`에 merge합니다.
- collaborator/private repository 설정을 확인합니다.

### 완료 기준

- README가 구현 현실과 같습니다.
- 최종 PR이 merge되었습니다.
- Repository를 제출할 준비가 되었습니다.

### 상태

`COMPLETE` — 평가자용 문서, 요구사항 추적, branch/PR workflow가 완료되었습니다. 이 `COMPLETE`는 메일 제출까지 끝났다는 뜻이 아닙니다. `SUB-003`(collaborator 권한)과 `SUB-006`(메일 제출)은 repository 밖 단계로 남아 있으며, 현재 상태는 이 historical plan이 아니라 `docs/REQUIREMENTS.md`에서 추적합니다.
