# 요구사항 추적 매트릭스

이 문서는 `ASSIGNMENT.md`의 요구사항을 구현·검증 가능한 단위로 정규화해 추적합니다.

기준 문서는 `ASSIGNMENT.md`입니다. 이 문서가 원문과 충돌하면 `ASSIGNMENT.md`를 따르고 이 문서를 고칩니다. 표의 Status 열은 항상 현재 상태이며, 아래 phase 메모(P3, P4, P5A, P5B, P6, P8-B 등)는 그 시점의 상태와 근거를 기록합니다.

## 상태

- `TODO`: 아직 구현되지 않았거나 외부 단계가 남음
- `IMPLEMENTED`: 구현했지만 acceptance 검증 전
- `VERIFIED`: 근거로 검증됨
- `BLOCKED`: 입력 누락이나 미해결 선행 조건으로 진행할 수 없음

---

## 1. 제출 및 저장소

| ID | Requirement | Source | Status |
|---|---|---|---|
| SUB-001 | Repository 이름은 `apr-product-engineer-assignment`이다. | §1 | VERIFIED |
| SUB-002 | Repository는 private이다. | §1 | VERIFIED |
| SUB-003 | `APRCORPORATION`을 collaborator로 추가한다. | §1 | TODO |
| SUB-004 | `main`이 아닌 별도 branch에 commit한다. | §1 | VERIFIED |
| SUB-005 | PR을 작성하고 `main`에 병합한다. | §1 | VERIFIED |
| SUB-006 | 최종 repository 주소를 메일로 제출한다. | §1 | TODO |

> 2026-10-04 당시 제출 상태 점검: GitHub는 repository를 요구된 이름의 private repository로 보고했습니다. 작업은 `main`이 아닌 branch와 병합된 PR로 진행했고, 그 시점의 예로 PR #50이 `main` `3c902dc`에 병합되어 있었습니다.
>
> 이후 재확인(읽기 전용, `main` `0ec0e3d` 시점): repository는 여전히 `PRIVATE`이고 병합된 PR은 57개입니다. collaborator는 소유자 `ppsssj`뿐이며 `APRCORPORATION`에 대한 collaborator 권한이나 대기 중인 invitation은 없습니다. 따라서 `SUB-003`은 `TODO`입니다. 최종 주소의 메일 제출도 repository 밖의 외부 제출 단계이므로 `SUB-006`도 `TODO`입니다. 두 항목은 자동으로 완료 처리하지 않습니다.

---

## 2. 필수 기술 스택 및 실행 환경

| ID | Requirement | Source | Status |
|---|---|---|---|
| ENV-001 | Backend는 Java 17을 사용한다. | §2 | VERIFIED |
| ENV-002 | Backend는 Spring Boot 3.x를 사용한다. | §2 | VERIFIED |
| ENV-003 | Frontend는 TypeScript를 사용한다. | §2 | VERIFIED |
| ENV-004 | Frontend는 Next.js 15 App Router를 사용한다. | §2 | VERIFIED |
| ENV-005 | Database는 PostgreSQL이다. | §2 | VERIFIED |
| ENV-006 | `docker compose up`으로 DB, backend, frontend가 기동된다. | §2, §7 | VERIFIED |
| ENV-007 | `GET /health` 경로를 그대로 유지한다. | §3 | VERIFIED |
| ENV-008 | `GET /health`는 HTTP 200을 반환한다. | §3 | VERIFIED |
| ENV-009 | `FRONTEND_PORT`로 port를 바꿀 수 있다. | §7 | VERIFIED |
| ENV-010 | `BACKEND_PORT`로 port를 바꿀 수 있다. | §7 | VERIFIED |
| ENV-011 | `DB_PORT`로 port를 바꿀 수 있다. | §7 | VERIFIED |
| ENV-012 | README에 정상 기동에 필요한 사전 조건을 빠짐없이 적는다. | §7 | VERIFIED |

> P8-B 당시 근거:
>
> - `ENV-001`: Java 17은 `build.gradle` toolchain `JavaLanguageVersion.of(17)`과 `gradle:8.14-jdk17` / `eclipse-temurin:17-jre` Docker stage에서 옵니다. runtime log는 `Java 17.0.20.1`을 보고했습니다.
> - `ENV-002`: 당시 Spring Boot는 `org.springframework.boot` plugin `3.5.0`이었고 backend는 `gradlew clean test`를 통과했습니다. *(이후 보안 dependency patch로 현재는 `3.5.16`이며, 요구사항인 3.x 범위 안입니다.)*
> - `ENV-005`: PostgreSQL은 Compose `postgres:16-alpine`이고, Flyway가 `PostgreSQL 16.15`를 보고했습니다.
> - `ENV-006`–`ENV-008`: V-001(seed readiness health gate를 포함한 P8-A clean-volume 기동)과 `/health` 200인 P8-B 정상 stack.
> - `ENV-009`–`ENV-011`: V-002(P8-A 대체 port 13000/18080/25432).
> - 당시 `ENV-012`는 P9(README) 범위였습니다. 이후 README에서 검증되었습니다.
>
> `ENV-008`의 의미: 과제는 `GET /health`의 경로와 200 상태 코드를 유지하라고 하며, `docker compose`가 이 endpoint로 기동 완료를 판단합니다. 현재 구현은 seed transaction commit 전 기동 중에는 503 `starting`을 반환하고, seed가 commit되어 ready가 된 뒤에는 200을 반환합니다. Compose healthcheck가 healthy로 판단하는 시점에 200이므로 요구사항을 만족합니다. "process가 뜬 순간부터 항상 200"이라는 뜻이 아닙니다(`docs/ARCHITECTURE.md` ADR-013).

---

## 3. Starter 계약

| ID | Requirement | Source | Status |
|---|---|---|---|
| START-001 | `frontend/app/dev-user/`는 수정하지 않고 그대로 둔다. | §3 | VERIFIED |
| START-002 | 기존 CORS 동작이 credential을 포함한 browser 요청과 계속 호환된다. | §3 | VERIFIED |
| START-003 | cookie에 의존하는 frontend browser 요청은 credential을 함께 보낸다. | §3 | VERIFIED |

> P3 당시: `ENV-003`/`ENV-004`는 당시 Next.js 15.4.6에서 `app/` router를 대상으로 strict `tsc --noEmit`과 `next build`로 검증했습니다 *(현재 Next.js는 `15.5.27`)*. `frontend/lib/api/client.ts`의 공용 client가 항상 `credentials: "include"`를 보내므로 당시 `START-003`은 `IMPLEMENTED`였고, 실제 browser에서 `/dev-user` → product 요청 흐름을 확인한 뒤에만 `VERIFIED`로 올리기로 했습니다. `START-001`, `START-002`, `USER-004`는 P3에서 바뀌지 않았고 최종 acceptance 대상으로 남겼습니다.

> P4 당시: `START-003`을 `VERIFIED`로 올렸습니다. 실제 Chromium session에서 수정하지 않은 `/dev-user` page로 `mintcream`을 고르자 `/`로 이동했고, 목록 요청 `GET /api/posts?limit=10`이 `Cookie: x-user-id=mintcream`을 실었으며, 응답은 그 user의 임시 좋아요를 `isLiked: true`로 보고했습니다(V-012 P4 근거).

> P8-B:
>
> - `START-001`: `git diff 38a9755 HEAD -- frontend/app/dev-user/`가 비어 있습니다(V-004).
> - `START-002`: `CorsConfig.java`도 starter에서 바뀌지 않았습니다. P8-B의 `:3000` → `:8080` browser 요청은 모두 `x-user-id` cookie를 실었고, preflight가 필요한 PATCH와 DELETE를 포함해 성공했습니다(`docs/VERIFICATION.md` "P8-B Full Behavioral Acceptance").

---

## 4. Seed 데이터

| ID | Requirement | Source | Status |
|---|---|---|---|
| SEED-001 | `seed/seed.json`의 게시글은 기동 후 저장되어 조회 가능하다. | §3 | VERIFIED |
| SEED-002 | `seed/seed.json`의 댓글은 기동 후 저장되어 조회 가능하다. | §3 | VERIFIED |
| SEED-003 | `deletedPostIds` 항목은 삭제된 상태로 저장되고 일반 조회에서 제외된다. | §3 | VERIFIED |
| SEED-004 | `deletedCommentIds` 항목은 삭제된 상태로 저장되고 일반 조회에서 제외된다. | §3 | VERIFIED |
| SEED-005 | seed의 `id`를 그대로 유지한다. | §3 | VERIFIED |
| SEED-006 | 새로 작성하는 게시글 ID는 자유롭게 부여하되 보존된 seed ID와 충돌하지 않는다. | §3 | VERIFIED |
| SEED-007 | 새로 작성하는 댓글 ID는 자유롭게 부여하되 보존된 seed ID와 충돌하지 않는다. | §3 | VERIFIED |
| SEED-008 | `commentCount`는 seed에서 읽지 않고 댓글로부터 계산한다. | §3 | VERIFIED |
| SEED-009 | 삭제된 댓글은 `commentCount`에서 제외한다. | §3, §4 | VERIFIED |
| SEED-010 | seed에 좋아요 기록이 없으므로 모든 요청자의 처음 `isLiked`는 false이다. | §3 | VERIFIED |
| SEED-011 | 처음 `likeCount`는 seed 값에서 시작한다. | §3 | VERIFIED |
| SEED-012 | 애플리케이션을 다시 기동해도 seed row가 중복 적재되지 않는다. | §3 | VERIFIED |

---

## 5. 현재 사용자 및 소유권

| ID | Requirement | Source | Status |
|---|---|---|---|
| USER-001 | 현재 사용자는 `x-user-id` 요청 header로 판단할 수 있다. | §4 | VERIFIED |
| USER-002 | 현재 사용자는 `x-user-id` cookie로 판단할 수 있다. | §4 | VERIFIED |
| USER-003 | header와 cookie가 모두 없으면 현재 사용자는 `apr_tester`이다. | §4 | VERIFIED |
| USER-004 | 로그인/인증 UI는 구현하지 않는다. | §4 | VERIFIED |
| USER-005 | 게시글 소유는 `post.author == currentUser`로 판단한다. | §4 | VERIFIED |
| USER-006 | 댓글 소유는 `comment.author == currentUser`로 판단한다. | §4 | VERIFIED |
| USER-007 | 과제에는 별도 사용자 table이 필요 없다. | §4 | VERIFIED |

> P8-B:
>
> - `USER-004`: frontend route는 `/`, `/dev-user`, `/posts/new`, `/posts/[postId]`, `/posts/[postId]/edit`뿐이고, frontend source에 login, sign-in, password code가 없습니다. 요청자는 starter `/dev-user` cookie나 `x-user-id` header로만 정해집니다.
> - `USER-007`: 원문의 뜻은 "별도 사용자 table이 필요 없다"이지 "만들면 안 된다"가 아닙니다. 현재 구현은 user table 없이 해결했습니다. Flyway `V1` schema는 `posts`, `comments`, `post_likes`, `seed_history`만 만들고, 소유권은 `author`와 판별된 요청자 문자열을 비교합니다. P8-B에서 게시글과 댓글 모두 다시 확인했습니다.

---

## 6. 공통 UI 동작

| ID | Requirement | Source | Status |
|---|---|---|---|
| UI-001 | 제품은 mobile web으로 구현한다. | §4 | VERIFIED |
| UI-002 | 기준 viewport는 375px이다. | §4 | VERIFIED |
| UI-003 | Desktop 대응은 필요 없다. | §4 | VERIFIED |
| UI-004 | 공식 Figma 디자인을 가능한 한 그대로 반영한다(제공되는 경우). | §4 | VERIFIED |
| UI-005 | 디자인에 없거나 흐름이 명확하지 않은 부분은 임의로 완성할 수 있다. | §4 | VERIFIED |
| UI-006 | Avatar는 제공된 avatar 이미지 세 개 중 하나를 쓴다. | §4 | VERIFIED |
| UI-007 | Avatar 선택 방식은 구현이 정할 수 있다. | §4 | VERIFIED |
| UI-008 | 의도적으로 개선한 UI/UX가 있으면 README에 무엇을 왜 바꿨는지 적는다. | §4 | VERIFIED |

> 최종 V-081 audit(2026-10-04): `UI-004`는 `VERIFIED`입니다. 공식 Figma file/node mapping을 직접 다시 확인했고, repository에 보존된 공식 export 12개 전체와 공유 수정 form state를 375×778 Chromium 구현과 비교했습니다. 차이는 V-081 전에 이미 문서화된 `PRODUCT_DECISION`으로 분류되었고, 우발적 불일치(`ACCIDENTAL_MISMATCH`)는 `NONE`이었습니다. 비교 기준은 frame metadata, rendered geometry, visual hierarchy이며 pixel-difference threshold는 쓰지 않았습니다. `docs/DESIGN_COMPARISON.md`와 `docs/VERIFICATION.md`의 V-081을 참고하세요.
>
> P4 당시: `UI-006`/`UI-007`은 `VERIFIED`였습니다. 목록의 모든 avatar가 `/avatar-1.png`–`/avatar-3.png` 중 하나였고, `frontend/lib/avatar.ts` `avatarFor()`(`stableHash(author) % 3`)가 골랐으며, 한 작성자는 row와 reload에 관계없이 같았습니다. 그 시점에는 목록 화면만 있었고 V-080은 모든 필수 화면이 필요했으며 V-081은 실행 전이었으므로 `UI-001`, `UI-002`, `UI-004`는 `TODO`였습니다.
>
> P8-B:
>
> - `UI-005`를 `VERIFIED`로 올렸습니다. 과제와 Figma가 정의하지 않은 흐름을 완성하고 browser에서 확인했습니다.
>   - retry가 있는 loading, empty, error state;
>   - 삭제되었거나 없거나 잘못된 게시글의 not-found;
>   - 권한 없는 수정 안내;
>   - toast 표시 시간;
>   - 입력을 유지하는 실패 처리.
>
>   그 값은 `docs/UI_SPEC.md`에 `PRODUCT_DECISION`으로 기록했습니다.
> - P8-B 당시 `UI-004`는 공식 Figma source와의 V-081 비교를 기다리며 `TODO`였습니다(이후 위 최종 audit에서 `VERIFIED`).
> - 당시 `UI-008`(README)은 P9 범위였습니다. 이후 README에서 검증되었습니다.

---

## 7. Soft Delete 정책

| ID | Requirement | Source | Status |
|---|---|---|---|
| DEL-001 | 게시글을 삭제해도 DB row를 물리적으로 지우지 않는다. | §4 | VERIFIED |
| DEL-002 | 댓글을 삭제해도 DB row를 물리적으로 지우지 않는다. | §4 | VERIFIED |
| DEL-003 | 삭제된 게시글은 목록 응답에 나오지 않는다. | §4 | VERIFIED |
| DEL-004 | 삭제된 게시글은 일반 상세 조회로 접근할 수 없다. | §4 | VERIFIED |
| DEL-005 | 삭제된 댓글은 댓글 목록 응답에 나오지 않는다. | §4 | VERIFIED |
| DEL-006 | 삭제된 댓글은 `commentCount`에서 제외한다. | §4 | VERIFIED |
| DEL-007 | UI는 삭제된 resource가 더 이상 없는 것처럼 동작한다. | §4 | VERIFIED |

> Deletion Policy Hardening(`4e53dbd`, 2026-09-29): DEL 요구사항은 하나의 model을 따릅니다(`docs/ARCHITECTURE.md` "Deletion Consistency Model").
> - 삭제된 게시글의 하위 데이터는 물리적으로 그대로 남지만 접근할 수 없습니다.
> - 읽을 수 없는 resource는 ownership 확인 전에 404를 반환하고, 반복 DELETE도 404를 반환합니다.
> - 당시에는 cached 목록이 page size만 다른 같은 collection이었으므로, 서버가 확인한 게시글 삭제는 로드된 page 밖의 게시글이어도 모든 cached 목록 `total`을 줄였습니다.
>
> 이후 검색·정렬이 추가되면서 현재 cache 규칙은 다음과 같습니다(`frontend/lib/query/postListCache.ts` `removePost`). 로드된 row는 모든 목록에서 제거하고, `total`은 filter 없는 목록과 그 row가 로드된 목록에서 줄입니다. 검색 목록은 로드되지 않은 범위의 membership과 `total`을 알 수 없으므로 invalidate합니다.
>
> 근거는 `docs/VERIFICATION.md` "Deletion Consistency Evidence"(28/28)에 있습니다.

---

## 8. 게시글 목록

| ID | Requirement | Source | Status |
|---|---|---|---|
| LIST-001 | 게시글 시간은 상대 시간으로 표시한다. | §4 | VERIFIED |
| LIST-002 | 상대 시간은 현재 시각을 기준으로 계산한다. | §4 | VERIFIED |
| LIST-003 | 게시글은 최신순이다. | §4 | VERIFIED |
| LIST-004 | 게시글 목록은 무한 스크롤이다. | §4 | VERIFIED |
| LIST-005 | 끝에 닿으면 전체를 한 번에 불러오지 않고 다음 page를 불러온다. | §4, §5 | VERIFIED |
| LIST-006 | 오른쪽 아래 작성 버튼은 게시글 작성 화면으로 이동한다. | §4 | VERIFIED |

> P4 당시: `LIST-001`/`LIST-002`는 모든 row에서 `frontend/lib/time/relativeTime.ts` 결과를 `Date.now()` 기준으로 render했습니다. `LIST-004`/`LIST-005`는 browser에서 검증했습니다. 당시 page size `limit=10`으로 sentinel이 가까워질 때만 10/10/10/9 page를 가져왔고, 읽을 수 있는 게시글 39개가 API 순서대로 한 번씩 render되었으며, 마지막 `nextCursor: null` 이후에는 요청이 없었습니다. *(이후 측정을 거쳐 현재 page size는 15입니다. `docs/PERFORMANCE.md` §13.)* 작성 화면이 P6에서 만들어질 예정이었으므로 당시 `LIST-006`은 `IMPLEMENTED`였고, `DEL-007`은 상세/댓글 UI가 생길 때까지 `TODO`였습니다. 다만 목록은 이미 읽을 수 있는 게시글 39개만 render했습니다(seed에서 삭제된 `9`, `30`, `31` 없음).

---

## 9. 게시글 상세

| ID | Requirement | Source | Status |
|---|---|---|---|
| DETAIL-001 | 좋아요는 toggle이다. | §4 | VERIFIED |
| DETAIL-002 | 좋아요 toggle은 표시되는 숫자를 바꾼다. | §4 | VERIFIED |
| DETAIL-003 | 좋아요 toggle은 좋아요 상태/icon을 바꾼다. | §4 | VERIFIED |
| DETAIL-004 | 댓글은 최신순이다. | §4 | VERIFIED |
| DETAIL-005 | 상세는 처음에 댓글을 최대 2개까지만 표시한다. | §4 | VERIFIED |
| DETAIL-006 | 댓글이 3개 이상일 때만 "더보기"를 보인다. | §4 | VERIFIED |
| DETAIL-007 | 댓글이 2개 이하면 "더보기"를 보이지 않는다. | §4 | VERIFIED |
| DETAIL-008 | "더보기"를 누른 뒤부터 나머지 댓글을 무한 스크롤로 불러온다. | §4 | VERIFIED |
| DETAIL-009 | 사용자는 댓글을 작성할 수 있다. | §4 | VERIFIED |
| DETAIL-010 | 댓글 작성자만 그 댓글을 삭제할 수 있다. | §4 | VERIFIED |
| DETAIL-011 | 삭제한 댓글은 표시된 목록에서 사라진다. | §4 | VERIFIED |
| DETAIL-012 | 게시글 작성자만 게시글 메뉴를 보고 쓸 수 있다. | §4 | VERIFIED |
| DETAIL-013 | 본인 글 메뉴에서 수정과 삭제를 고를 수 있다. | §4 | VERIFIED |
| DETAIL-014 | 수정 화면은 작성 화면과 같은 layout/흐름을 쓴다. | §4 | VERIFIED |
| DETAIL-015 | 수정 화면은 기존 제목/내용이 채워진 상태로 열린다. | §4 | VERIFIED |
| DETAIL-016 | 수정을 저장하면 게시글이 바뀐다. | §4 | VERIFIED |
| DETAIL-017 | 수정에도 작성과 같은 검증을 적용한다. | §4 | VERIFIED |
| DETAIL-018 | 본인 게시글을 삭제하면 목록으로 이동한다. | §4 | VERIFIED |
| DETAIL-019 | 남의 게시글은 수정할 수 없다. | §4 | VERIFIED |
| DETAIL-020 | 남의 게시글은 삭제할 수 없다. | §4 | VERIFIED |

> P5A 당시: `DETAIL-001`–`DETAIL-008`은 Compose production frontend를 대상으로 375×778 browser 실행에서 `VERIFIED`가 되었습니다(V-050 UI 근거, V-060, V-061). `DETAIL-009`/`DETAIL-010`은 P2B backend 근거로 `VERIFIED`였지만 그 UI(댓글 composer, 본인 댓글 삭제)는 P5B 범위였고 `DETAIL-011`도 그랬습니다. `DETAIL-012`–`DETAIL-018`은 P6 범위였습니다.

> P5B 당시: `DETAIL-009`와 `DETAIL-010`은 P2B backend 근거로 `VERIFIED`를 유지하면서 UI acceptance가 더해졌습니다. 고정 composer는 서버가 판별한 요청자(기본 `apr_tester`, `/dev-user` 후 `mintcream`)를 작성자로 하는 댓글을 만들고, `삭제`는 남의 게시글 위에서도 요청자 본인 댓글에만 나타납니다. backend는 다른 사용자의 삭제에 계속 403을 반환합니다. `DETAIL-011`을 `VERIFIED`로 올렸습니다. 확인한 삭제는 접힌 목록과 펼친 목록에서 댓글을 제거하고, 댓글 total, 상세 count, cached 목록 count를 갱신하며, reload 후에도 댓글이 없습니다. 근거는 V-062–V-064와 `docs/VERIFICATION.md` "P5B Comment Mutation Evidence"입니다. 당시 `DEL-007`은 P6 게시글 삭제 UI가 생길 때까지 `TODO`였습니다.

> P6 당시(`1f8a21f`, browser 근거는 `docs/VERIFICATION.md` "P6 Post CRUD Evidence"):
> - `DETAIL-012`/`DETAIL-013`: 게시글 작성자만 `게시글 메뉴` trigger를 보고, 당시에는 이 trigger가 수정하기 / 삭제하기 / 취소가 있는 bottom sheet(`PostActionSheet`)를 열었습니다. `apr_tester`와 `mintcream`으로 양방향 확인했습니다. *(이후 `0c86e1d`(2026-09-30)에서 trigger 옆의 `PostActionMenu` popover로 바뀌었고, 현재 메뉴에는 수정하기와 삭제하기만 있으며 취소 row는 없습니다. 과제 요구는 메뉴에서 수정·삭제를 고를 수 있는 것이므로 `DETAIL-013`에 영향이 없습니다. `docs/DESIGN_COMPARISON.md` "게시글 메뉴" 참고.)*
> - `DETAIL-014`–`DETAIL-017`: 수정은 작성 form을 재사용하고, 정확한 값을 채우며, PATCH로 저장하고, 같은 검증(blank, Unicode code point 기준 20자)을 적용합니다.
> - `DETAIL-018`: 본인 게시글 삭제 후 목록으로 돌아갑니다.
> - `DETAIL-019`/`DETAIL-020`: backend 근거를 유지합니다. UI도 남의 게시글에서는 trigger를 숨기고 `/posts/{other}/edit`에서 form을 보이지 않으며, backend는 PATCH/DELETE에 계속 403을 반환합니다.
> - `EDIT-004`: 잘못된 입력에서는 `작성 완료` / `수정 완료`가 비활성이고 요청을 보내지 않습니다.
> - `EDIT-005`/`EDIT-006`: 작성에 성공하면 목록으로 돌아가 `게시글이 등록되었습니다.` toast를 한 번 보입니다.
> - `EDIT-007`: 댓글 수정 UI와 endpoint가 없습니다.
> - `LIST-006`: FAB가 동작하는 작성 화면을 엽니다.
> - `DEL-007`: 삭제된 게시글은 cached 목록과 다시 가져온 목록에서 사라지고, 그 상세/수정 URL은 not-found를 보입니다. 삭제된 댓글과 seed에서 삭제된 resource도 같습니다(P4/P5B).
> - `UI-001`–`UI-003`: V-080 `PASS`. 모든 필수 화면을 375×778에서 확인했고 desktop 전용 layout은 없습니다.

---

## 10. 게시글 작성/수정 검증

| ID | Requirement | Source | Status |
|---|---|---|---|
| EDIT-001 | 게시글 제목은 필수이다. | §4 | VERIFIED |
| EDIT-002 | 게시글 내용은 필수이다. | §4 | VERIFIED |
| EDIT-003 | 게시글 제목은 최대 20자이다. | §4 | VERIFIED |
| EDIT-004 | 필수 입력이 유효하지 않으면 저장/완료 action은 비활성이다. | §4 | VERIFIED |
| EDIT-005 | 작성에 성공하면 게시글 목록으로 이동한다. | §4 | VERIFIED |
| EDIT-006 | 작성에 성공하면 완료 toast를 보인다. | §4 | VERIFIED |
| EDIT-007 | 댓글 수정은 구현하지 않는다. | §4 | VERIFIED |

---

## 11. 필수 API

Base path: `http://localhost:8080/api` (Source: §5)

| ID | Method | Path | Requirement | Status |
|---|---|---|---|---|
| API-001 | GET | `/posts` | 최신순 게시글 목록을 반환한다. | VERIFIED |
| API-002 | POST | `/posts` | `{ title, content }`로 게시글을 작성한다. | VERIFIED |
| API-003 | GET | `/posts/{postId}` | 게시글 상세 하나를 반환한다. | VERIFIED |
| API-004 | PATCH | `/posts/{postId}` | `{ title, content }`로 본인 게시글을 수정한다. | VERIFIED |
| API-005 | DELETE | `/posts/{postId}` | 본인 게시글을 soft delete한다. | VERIFIED |
| API-006 | POST | `/posts/{postId}/likes/toggle` | 요청자별 좋아요 상태를 toggle한다. | VERIFIED |
| API-007 | GET | `/posts/{postId}/comments` | 최신순 댓글을 반환한다. | VERIFIED |
| API-008 | POST | `/posts/{postId}/comments` | `{ content }`로 댓글을 작성한다. | VERIFIED |
| API-009 | DELETE | `/comments/{commentId}` | 본인 댓글을 soft delete한다. | VERIFIED |

---

## 12. API 응답 계약

| ID | Requirement | Source | Status |
|---|---|---|---|
| API-010 | 요청/응답 본문은 JSON이다. | §5 | VERIFIED |
| API-011 | `GET /posts` 응답에 `items`가 있다. | §5 | VERIFIED |
| API-012 | `GET /posts` 응답에 `total`이 있다. | §5 | VERIFIED |
| API-013 | `GET /posts`의 `total`은 이번 응답의 개수가 아니라 조건에 맞는 조회 가능한 전체 개수이다. | §5 | VERIFIED |
| API-014 | 게시글 목록 항목에 `id`가 있다. | §5 | VERIFIED |
| API-015 | 게시글 목록 항목에 `title`이 있다. | §5 | VERIFIED |
| API-016 | 게시글 목록 항목에 `content`가 있다. | §5 | VERIFIED |
| API-017 | 게시글 목록 항목에 `likeCount`가 있다. | §5 | VERIFIED |
| API-018 | 게시글 목록 항목에 `commentCount`가 있다. | §5 | VERIFIED |
| API-019 | 게시글 목록 항목에 요청자 기준 `isLiked`가 있다. | §5 | VERIFIED |
| API-020 | 게시글 목록 항목에 `author`가 있다. | §5 | VERIFIED |
| API-021 | 게시글 목록 항목에 `createdAt`이 있다. | §5 | VERIFIED |
| API-022 | `GET /posts/{postId}`는 `items` 봉투 없이 게시글 객체 하나를 반환한다. | §5 | VERIFIED |
| API-023 | `GET /posts/{postId}`에 `isLiked`와 `commentCount`가 있다. | §5 | VERIFIED |
| API-024 | 댓글 목록 응답에 `items`가 있다. | §5 | VERIFIED |
| API-025 | 댓글 목록 응답에 `total`이 있다. | §5 | VERIFIED |
| API-026 | 댓글 항목에 `id`, `postId`, `content`, `author`, `createdAt`이 있다. | §5 | VERIFIED |
| API-027 | 좋아요 toggle 응답에 `isLiked`가 있다. | §5 | VERIFIED |
| API-028 | 좋아요 toggle 응답에 `likeCount`가 있다. | §5 | VERIFIED |
| API-029 | PATCH 응답은 수정된 게시글 객체를 반환한다. | §5 | VERIFIED |
| API-030 | `isLiked`는 요청자 기준으로 계산한다. | §5 | VERIFIED |
| API-031 | 목록 API는 구현이 직접 설계한 방식으로 나눠서 불러올 수 있다. | §5 | VERIFIED |
| API-032 | 목록 API는 한 번의 요청으로 전체 collection을 반환하지 않는다. | §5 | VERIFIED |
| API-033 | 성공·오류 상태 코드는 구현이 직접 정해 일관되게 사용한다(코드 값 자체는 채점 대상이 아님). | §5 | VERIFIED |
| API-034 | 소유권이 없는 작업은 backend 동작으로 거부된다. | §5 | VERIFIED |
| API-035 | 삭제된 resource는 API 동작에서 일관되게 제외/거부된다. | §5 | VERIFIED |

---

## 13. README / 평가 문서

| ID | Requirement | Source | Status |
|---|---|---|---|
| DOC-001 | README가 상태 관리, ORM, styling 같은 기술 선택을 설명한다. | §6 | VERIFIED |
| DOC-002 | README가 서버에서 받은 데이터를 어디에/어떻게 두는지 설명한다. | §6 | VERIFIED |
| DOC-003 | README가 frontend/backend 계층을 나눈 기준을 설명한다. | §6 | VERIFIED |
| DOC-004 | README가 `seed/seed.json`을 어떻게 적재·저장하는지 설명한다. | §6 | VERIFIED |
| DOC-005 | README가 실제 구현 파일/함수를 짚는다. | §6 | VERIFIED |
| DOC-006 | README가 중요한 선택의 이유를 설명한다. | §6 | VERIFIED |
| DOC-007 | README가 관련 trade-off와 고르지 않은 대안을 설명한다. | §6 | VERIFIED |
| DOC-008 | 테스트를 작성했다면 README가 무엇을 왜 테스트했는지 설명한다(테스트 자체는 필수 아님). | §6 | VERIFIED |
| DOC-009 | 의도적인 UI/UX 차이나 개선이 있으면 README에 적는다. | §4, §6 | VERIFIED |

> 최종 문서 정리(2026-10-04 당시, base `main` `3c902dc` 기준 audit): 평가자용 README를 다시 점검했고, 과제가 요구한 상위 section을 유지했습니다. README는 실제 기술 선택, server state/cache 규칙, 책임 경계, seed lifecycle, 측정으로 정한 요청 단위, trade-off, 의도적 UI 결정을 기록합니다. 자세한 결정 과정은 `docs/ENGINEERING_NOTES.md`로 옮겼고, architecture, 성능 원자료, 실행 근거는 각 기준 문서에 둡니다. 이후의 문서 PR은 README의 이 내용을 바꾸지 않았고, 현재 README도 기술 선택, 상태 설계, 구조, 데이터 적재, 개선한 점 section을 갖고 있습니다.
>
> `DOC-008`: 테스트는 과제에서 필수가 아니지만 이 repository에는 테스트가 있으므로 README가 이를 설명합니다. README는 commit된 자동화와 더 넓은 acceptance를 구분합니다. backend test에는 실제 PostgreSQL Testcontainers coverage가 있고, frontend Vitest는 cache/retry 규칙을 다루며, CI는 production Compose stack을 띄운 뒤 commit된 Chromium Playwright 회귀 3개를 실행합니다. 전체 cursor 순회, 모든 실패/소유권 조합, 다른 browser, visual regression, Lighthouse는 그 Playwright 범위 밖입니다.
>
> Playwright는 선택한 동작을 검증할 뿐 공식 Figma fidelity를 검증하지 않습니다. 그 비교는 별도로 완료되었습니다. V-081은 `PASS`이고 `UI-004`는 `VERIFIED`입니다(`docs/DESIGN_COMPARISON.md`).

---

## 14. 완료 판정 기준

이 기준은 `ASSIGNMENT.md`의 문장이 아니라, 이 추적 문서가 완료를 판단하려고 정한 repository 내부 기준입니다. 이 문서 기준으로 완료라고 하려면:

- 위의 필수 요구사항이 모두 `VERIFIED`이거나,
- 필수가 아니거나 조건부인 항목은 그 사실이 명시적으로 문서화되어 있고,
- `docs/VERIFICATION.md`의 release-gate 시나리오(repository 내부 검증 gate)가 모두 통과해야 합니다.

**현재 상태:** 아직 완료가 아닙니다. 남은 `TODO`는 외부 제출 단계인 `SUB-003`(`APRCORPORATION` collaborator 추가)과 `SUB-006`(repository 주소 메일 제출)입니다. 나머지 요구사항은 모두 `VERIFIED`입니다.
