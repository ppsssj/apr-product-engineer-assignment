# README 교체 검토안

`README.md`는 수정하지 않았습니다. 이 파일은 최종 제출용 README 후보를 검토하기 위한 문서이며, 실제 교체가 끝나면 삭제합니다.

---

# 1. 현재 README 원본

````markdown
# 메디큐브톡 커뮤니티

> 이 파일은 **제출용 README의 템플릿**입니다.
> `## 실행` 절은 그대로 두시고 (변경한 게 있으면 고쳐주세요),
> 아래 빈 절들을 채워주세요. 과제 내용은 [`ASSIGNMENT.md`](./ASSIGNMENT.md)를 확인해주세요.

---

## 실행

```bash
docker compose up
```

| | 주소 |
| --- | --- |
| 프론트엔드 | http://localhost:3000 |
| 백엔드 | http://localhost:8080 |
| 기동 확인 | http://localhost:8080/health · `GET /health` 는 유지해주세요 |

포트가 이미 사용 중이면 환경 변수로 바꿀 수 있습니다.

```bash
FRONTEND_PORT=13000 BACKEND_PORT=18080 DB_PORT=25432 docker compose up --build
```

`BACKEND_PORT`를 바꾸면 프론트엔드를 다시 빌드해야 해서 `--build`가 필요합니다.
`NEXT_PUBLIC_API_BASE_URL`이 빌드 시점에 클라이언트 번들로 인라인되기 때문입니다.

<details>
<summary>참고 — 제공된 스타터의 구성</summary>

```
seed/seed.json        초기 데이터
docker-compose.yml    DB(PostgreSQL 16) + 백엔드 + 프론트엔드
backend/              Spring Boot 3.5 / Java 17. DB 연결 · GET /health · CORS 설정만 있습니다.
frontend/             Next.js 15 App Router. 기본 설정과 app/dev-user/ 만 있습니다.
```

- **DB 호스트 포트 기본값은 `15432`** 입니다. `5432`는 이미 쓰고 있는 경우가 많아 피했습니다.
- `seed/`는 백엔드 컨테이너의 `/seed`에 읽기 전용으로 마운트됩니다. (`APP_SEED_PATH`)
- DB 데이터는 named volume(`dbdata`)에 남습니다. `docker compose down`을 해도 유지되고,
  **초기화하려면 `docker compose down -v`** 를 쓰세요.
- 브라우저에서 호출할 때와 컨테이너 안에서 호출할 때 주소가 달라, API 주소를 두 개 줍니다.
  - `NEXT_PUBLIC_API_BASE_URL` — 브라우저에서 (클라이언트 컴포넌트)
  - `API_BASE_URL` — 컨테이너 안에서 (서버 컴포넌트 · 라우트 핸들러)
- **`/dev-user`** 에서 요청자를 바꿀 수 있습니다. 고르면 `x-user-id` 쿠키가 설정됩니다.
  쿠키가 없으면 기본 요청자는 `apr_tester`입니다.
- 백엔드 의존성에는 `spring-boot-starter-jdbc`만 들어 있습니다. ORM은 직접 고르세요.
- 테스트 환경(`spring-boot-starter-test` · JUnit · Mockito)은 준비되어 있습니다.
  테스트를 작성하면 그대로 동작합니다.
- 폰트는 `frontend/public/fonts/`의 **Pretendard**를 쓰면 됩니다. `globals.css`에 적용되어 있습니다.
- **CORS는 설정되어 있습니다** (`CorsConfig.java`). `http://localhost:*`(및 `127.0.0.1:*`) 오리진을
  자격 증명과 함께 허용하므로 포트를 바꿔도 동작합니다. 브라우저에서 직접 호출할 때는 요청에
  `credentials: "include"`를 붙여야 `x-user-id` 쿠키가 전송됩니다.

로컬 개발은 compose 없이 하셔도 됩니다. DB만 띄우고 각각 실행하는 쪽이 빠릅니다.

```bash
docker compose up db
cd backend  && ./gradlew bootRun
cd frontend && npm install && npm run dev
```

</details>

---

## 기술 선택

무엇을 골랐고 왜 그렇게 정했는지 적어주세요.
(상태 관리, 스타일링, ORM, 그 외 주요 라이브러리)

<!-- 여기에 작성 -->

---

## 상태 설계

서버에서 받아온 데이터를 어디에 두었고, 왜 그렇게 했는지 적어주세요.

<!-- 여기에 작성 -->

---

## 구조

프론트엔드와 백엔드의 계층을 어떤 기준으로 나눴는지 적어주세요.

<!-- 여기에 작성 -->

---

## 데이터 적재

`seed/seed.json`을 어떤 방식으로 적재했는지, 어떤 스키마에 담았는지 적어주세요.

<!-- 여기에 작성 -->

---

## 개선한 점 · 남은 과제 (선택)

디자인에 없어서 임의로 정한 것, 개선한 UI/UX, 시간이 더 있었다면 하고 싶었던 것이
있으면 적어주세요. **없다면 이 절은 비워두거나 지우셔도 됩니다.**

<!-- 여기에 작성 -->
````

---

# 2. 제출용 README 교체안

# 메디큐브톡 커뮤니티

APR Product Engineer 인턴 과제의 메디큐브톡 모바일 커뮤니티를 프론트엔드와 백엔드까지 구현한 저장소입니다. 제공된 Spring Boot/JDBC/PostgreSQL 및 Next.js 골격 위에 커뮤니티 도메인, 영속성, API, 서버 상태 관리와 375px 모바일 UI를 완성했습니다.

## 실행

Docker와 Docker Compose가 필요합니다. 저장소 루트에서 다음 한 줄로 PostgreSQL, 백엔드, 프론트엔드를 실행합니다.

```bash
docker compose up
```

| 대상 | 기본 주소 |
| --- | --- |
| 프론트엔드 | http://localhost:3000 |
| 백엔드 API | http://localhost:8080/api |
| 기동 확인 | http://localhost:8080/health |
| 개발용 요청자 전환 | http://localhost:3000/dev-user |

포트가 겹치면 다음처럼 변경할 수 있습니다.

```bash
FRONTEND_PORT=13000 BACKEND_PORT=18080 DB_PORT=25432 docker compose up --build
```

`BACKEND_PORT`는 프론트엔드의 `NEXT_PUBLIC_API_BASE_URL` 빌드 인자에 반영되므로, 값을 바꿀 때는 `--build`로 클라이언트 번들을 다시 만들어야 합니다. DB 데이터는 `dbdata` named volume에 유지되며, 로컬 데이터를 완전히 초기화할 때만 `docker compose down -v`를 사용합니다.

## 과제를 어떻게 해석했는가

이 과제의 출발점은 완성된 커뮤니티를 다듬는 일이 아니라, 실행 골격과 데이터·디자인을 받아 제품의 빈 경계를 스스로 정의하는 일이었습니다. 따라서 화면 개수만 채우기보다 빈 DB가 같은 상태로 재현되는지, 읽기 규칙이 API와 SQL에서 일관적인지, 긴 목록을 모바일 네트워크에서 끝까지 탐색할 수 있는지, mutation 뒤 여러 화면이 같은 사실을 보여 주는지를 먼저 설계 기준으로 삼았습니다.

제품 성격은 사람을 팔로우하고 프로필을 소비하는 SNS보다 글과 댓글을 중심으로 대화를 다시 찾는 커뮤니티로 해석했습니다. 제공 API와 seed에는 팔로우·팔로워, 프로필 상세, 인플루언서 등급, 개인화 신호가 없었기 때문에 이를 새로 발명하지 않았습니다. 대신 제목과 본문을 먼저 읽게 하는 목록, 최신순을 보존한 검색·좋아요순 탐색, 댓글 흐름에 집중했습니다. 이 해석은 뒤의 UI hierarchy와 검색·정렬 범위를 정하는 기준이 됐습니다.

### 제공된 출발점과 구현한 범위

APR이 제공한 범위는 Java 17/Spring Boot 3.5 프로젝트와 Spring Web/JDBC·PostgreSQL 연결, `GET /health`, CORS, Compose, `seed/seed.json`, Next.js 15 App Router 골격, `/dev-user`, 아바타와 Pretendard 자산입니다. 즉 실행 환경과 입력 데이터는 있었지만 community schema, 데이터 적재, 도메인 API와 실제 화면 동작은 구현 대상이었습니다.

그 위에 Flyway 스키마, 멱등한 시드 적재와 readiness gate, 게시글·댓글·좋아요 영속성, REST API, 소프트 삭제와 소유권 검증, keyset 페이지네이션, TanStack Query 캐시, 게시글 CRUD·댓글·좋아요, 무한 스크롤, 검색·정렬과 모바일 UI를 구현했습니다. 댓글 수정과 로그인은 과제 범위대로 구현하지 않았습니다. 이 경계를 명시한 이유는 starter가 보장한 것과 이 구현에서 판단하고 검증한 것을 섞지 않기 위해서입니다.

## 기술 선택과 설계가 만들어진 과정

### Backend persistence — Spring `JdbcClient`

이 과제의 읽기 규칙은 단순한 row CRUD보다 “삭제되지 않은 행만 조회”, “댓글 수를 파생”, “시드 좋아요와 요청자 좋아요를 합산”, “정렬마다 다른 keyset 조건 적용”에 가깝습니다. 따라서 ORM mapping을 새로 도입할지, 스타터의 Spring JDBC 기반에서 SQL을 명시할지를 결정해야 했습니다.

도메인이 게시글·댓글·좋아요로 작고 query 의미를 리뷰에서 직접 확인하는 편이 중요하다고 판단해 `JdbcClient`와 SQL repository를 선택했습니다. Spring Framework가 설명하는 `JdbcClient`는 Spring JDBC의 named/positional parameter query와 update를 하나의 fluent API로 제공합니다.[^spring-jdbc-client] 이 capability가 현재 `PostRepository`, `CommentRepository`, `PostLikeRepository`의 작은 명시적 query 집합에 맞았습니다. 대신 JPA보다 SQL, row mapping과 DB 종속 동작을 직접 관리하는 비용을 수용했습니다.

이 선택은 구현 중 soft-delete predicate, requester별 `isLiked`, derived count, 검색 조건, 정렬별 cursor bound가 추가될 때 의미가 더 분명해졌습니다. SQL 자체를 설계 대상으로 두었기 때문에 논리 조건을 그대로 검토하고 `EXPLAIN`으로 index 사용과 cursor scan을 추적한 뒤 predicate만 교체할 수 있었습니다. 이는 JPA로 구현할 수 없다는 뜻이 아니라, query가 실제로 어떻게 실행되는지를 확인하며 발전시킨 이번 작업 방식에 명시적 SQL이 잘 맞았다는 뜻입니다.

### Schema migration — Flyway

평가자는 빈 PostgreSQL에서 시작할 수 있고, schema 변경과 과제용 `seed.json` 적재는 수명주기가 다릅니다. 애플리케이션 코드에서 테이블을 즉석 생성하거나 schema와 seed를 한 초기화 절차에 섞으면, 구조 변경 이력과 “과제 데이터가 이미 들어갔는가”를 구분하기 어렵습니다.

그래서 schema는 `V1__create_community_schema.sql`, seed는 `SeedInitializer`가 맡도록 분리했습니다. Spring Boot는 시작 시 Flyway migration을 실행하며 PostgreSQL에는 `flyway-database-postgresql` 모듈을 사용하도록 문서화하고 있고, 현재 `backend/build.gradle`도 그 구성을 따릅니다.[^spring-flyway] Flyway의 `flyway_schema_history`는 적용 migration, 성공 여부와 checksum을 추적합니다.[^flyway-history] 이 선택은 빈 DB 재현성과 변경 이력을 얻는 대신, 적용된 versioned migration을 임의 수정하지 않고 다음 migration으로 전진해야 하는 규율을 요구합니다. 과제 seed의 멱등성은 별도의 `seed_history`가 담당합니다.

### Frontend server state — TanStack Query

같은 게시글이 목록과 상세에 반복해서 나타나고, 좋아요·댓글·수정·삭제 뒤에는 여러 화면의 copy가 함께 바뀌어야 합니다. 각 component의 `useState`/`useEffect`로 page 누적, loading, 취소와 mutation 후 동기화를 따로 구현할지, API 데이터를 전담하는 cache 계층을 둘지가 핵심 질문이었습니다.

이 데이터는 브라우저가 소유하는 전역 상태가 아니라 서버가 원본인 상태이므로 TanStack Query를 선택했습니다. 공식 문서의 `useInfiniteQuery`는 `pages`, `pageParam`, `getNextPageParam`과 `fetchNextPage`로 누적 목록을 모델링하고,[^tanstack-infinite] query key는 query data를 고유하게 식별하는 cache 기준입니다.[^tanstack-query-keys] 또한 mutation 성공 후 관련 query invalidation과 query function의 `AbortSignal` 소비를 지원합니다.[^tanstack-invalidation][^tanstack-cancellation] 이는 이 과제의 무한 스크롤, sort/search별 cache, stale 검색 취소에 직접 대응합니다. 대신 production dependency와 `QueryClientProvider`가 추가되고, 어떤 cache를 직접 갱신하고 invalidate할지는 여전히 애플리케이션이 설계해야 합니다.

### Styling — CSS Modules

375px 한 화면에 맞춘 간격·타이포그래피·pressed/focus 상태를 세밀하게 표현해야 했지만, 과제 규모에서 utility framework나 UI kit를 새로 도입하면 design token보다 설정과 추상화가 더 커질 수 있었습니다. 그래서 Pretendard, reset과 공통 token만 `globals.css`에 두고 나머지는 component별 CSS Modules로 작성했습니다. Next.js는 `.module.css`의 class name을 고유하게 생성해 local scope와 충돌 방지를 제공합니다.[^next-css-modules] 추가 framework 없이 스타일의 근거가 각 feature 옆에 남는 대신 반복되는 CSS 선언을 일부 감수했습니다.

## 데이터 모델과 seed 입력을 함께 읽은 과정

처음에는 화면에 필요한 `posts`와 `comments`만 옮기면 될 것처럼 보였지만, seed가 표현하는 사실과 API가 요구하는 사실의 모양이 달랐습니다. 삭제 목록은 별도 ID 집합이고 좋아요는 사용자별 관계가 아닌 집계이며, 원본 ID는 보존해야 하지만 새 데이터에는 자동 ID가 필요했습니다. 이 차이를 초기화 코드에서 임시로 보정하지 않고 schema에 드러내는 쪽을 택했습니다.

### Soft delete

과제는 삭제된 resource를 화면과 API에서는 없어진 것처럼 보이게 하되 DB row는 물리적으로 보존하라고 요구합니다. 별도 archive table로 옮기거나 `deleted_at`을 둘 수도 있었지만, seed는 삭제된 ID만 제공하고 삭제 시각은 제공하지 않습니다. 존재하지 않는 시각을 만들어 넣기보다 현재 상태만 나타내는 `posts.is_deleted`와 `comments.is_deleted`를 선택했습니다.

모든 normal read와 count에 이 조건을 명시해야 한다는 비용이 생기지만, 삭제 정책이 SQL에서 확인 가능하고 seed의 deleted ID도 같은 model로 표현됩니다. 게시글 삭제 때 child row까지 삭제 상태로 다시 쓰지는 않습니다. 댓글 자체가 명시적으로 삭제된 상태와, 댓글은 남아 있지만 parent 게시글이 unreadable해 접근할 수 없는 상태를 구분하면서도 제품 API에서는 둘 다 노출하지 않습니다. unreadable parent의 댓글 조회·생성·삭제를 차단하고, 명시적으로 삭제된 댓글은 `commentCount`에서도 제외합니다.

### Likes

시드는 좋아요 총수만 주고 누가 눌렀는지는 주지 않으며, 아직 애플리케이션에서 만들어진 requester 관계가 없으므로 초기 `isLiked`는 모든 요청자에게 false여야 합니다. 반면 이후 API의 `isLiked`는 현재 요청자 기준이어야 합니다. 총수를 mutable counter 하나로 두면 요청자 관계를 잃고, 과거 사용자 행을 만들어 채우면 존재하지 않는 사용자와 관계를 발명하게 됩니다. 그래서 알려진 두 사실을 분리했습니다.

```text
likeCount = posts.base_like_count + COUNT(post_likes rows)
```

시드 집계는 `base_like_count`, 애플리케이션에서 생성된 관계만 `post_likes(post_id, user_id)`에 저장합니다. composite primary key가 같은 요청자의 중복 행을 막고 `isLiked`는 그 행의 존재로 계산합니다. 이 model은 입력 사실을 보존하지만, 표시 count마다 관계 행 aggregate가 필요하다는 대가가 있습니다. 제공 데이터 규모에서는 denormalized counter의 동기화 위험보다 이 명확성을 택했습니다.

### IDs

시드 ID는 URL과 관계의 기준이므로 그대로 보존해야 하지만 새 row의 ID는 자동 생성되어야 합니다. 단순 auto-increment만 믿으면 명시적 seed ID와 이후 생성 ID가 충돌할 수 있고, 반대로 모든 ID를 애플리케이션에서 만들면 DB가 제공하는 동시성 안전한 할당을 버리게 됩니다. 두 경로를 한 column에서 지원하기 위해 `GENERATED BY DEFAULT AS IDENTITY`를 선택했습니다. PostgreSQL에서 이 형태는 명시값이 있으면 그 값을 우선하고, 생략하면 implicit sequence 값을 사용합니다.[^postgres-identity] 명시적 seed insert는 sequence를 자동으로 전진시키지 않으므로 `IdentitySequenceRepository.synchronizeWithSeededRows()`가 최대 ID에 맞춥니다. 별도 ID allocator를 만들지 않는 대신 seed 이후 sequence sync가 필수입니다.

## schema와 seed를 분리한 초기 데이터 적재

첫 구현부터 schema migration과 과제 데이터 적재는 분리했습니다. Flyway는 구조의 버전과 checksum을 관리하고, `SeedInitializer`와 `seed_history`는 이 저장소의 고정 입력이 한 번 반영됐는지를 판단합니다. 첫 실행에는 2,118개 원본 row가 모두 들어가야 하고, 재기동은 중복되면 안 되며, 실패한 적재를 완료로 기록해서도 안 되기 때문입니다. 고정된 과제 seed에는 단일 marker가 단순하고 충분하지만, 운영 데이터가 버전별로 바뀌는 제품이라면 content version별 migration 전략이 추가로 필요합니다.

```text
Flyway schema migration
  → SeedInitializer(ApplicationRunner)
  → seed/seed.json 읽기
  → 원본 ID로 posts/comments 삽입
  → deletedPostIds/deletedCommentIds 반영
  → identity sequence 동기화
  → seed_history에 assignment-v1 기록
  → transaction commit
```

`SeedInitializer.run()` 전체가 하나의 transaction이므로 posts/comments와 `assignment-v1` marker는 함께 commit되거나 rollback됩니다. marker가 이미 있으면 재기동 때 seed를 다시 쓰지 않아 중복을 만들거나 사용자가 만든·수정한 runtime data를 원본으로 되돌리지 않습니다. sequence sync는 marker 기록 전에 실행되며, 실패 후 재시작하더라도 marker가 없으므로 적재를 다시 시도합니다. PostgreSQL sequence 자체가 transaction rollback의 대상이라고 가정하지 않고, marker가 없으면 전체 적재와 sync를 다시 수행하는 복구 경로를 기준으로 삼았습니다. `SeedHistoryRepository`는 과제 seed의 멱등성을 판단하고, Flyway의 schema history와 역할이 다릅니다.

## “서버가 켜짐”과 “서비스 준비 완료”를 분리한 과정

초기 `/health`는 DB에 query가 되는지만 확인했습니다. 이 기준에서는 Tomcat과 DB가 먼저 살아난 뒤 `ApplicationRunner`의 seed transaction이 아직 commit되지 않은 짧은 구간에도 200을 반환했고, clean start 측정에서 health는 성공했지만 게시글 API의 `total`은 아직 0인 상태를 실제로 관찰했습니다. 프로세스가 살아 있다는 의미와 제품 데이터까지 요청을 받을 준비가 됐다는 의미를 같게 본 것이 원인이었습니다.

이를 확인한 뒤 `SeedReadiness`를 추가하고, 이미 적재된 재기동 경로와 새 적재 경로 모두 transaction의 `afterCommit`에서만 ready를 열도록 바꿨습니다. 그 전의 `/health`는 DB 연결이 살아 있어도 503과 `starting`을 반환합니다. 따라서 Compose가 backend를 healthy로 판단할 때 API 데이터도 준비되어 있습니다. readiness를 얻는 대신 최초 기동은 seed가 끝날 때까지 healthy가 되지 않으며, row별 insert의 단순성과 실패 위치 명확성을 위해 clean start 약 0.9초의 일회성 비용을 수용했습니다.

## 페이지네이션 선택과 query 재최적화

목록을 한 번에 전부 가져오면 안 되고, 댓글은 한 게시글에 1,411개까지 치우쳐 있습니다. 가장 단순한 후보였던 `OFFSET`은 앞쪽에 새 row가 생기는 동안 page 경계가 흔들릴 수 있고, PostgreSQL도 큰 `OFFSET`의 skipped row를 server가 여전히 계산한다고 설명합니다.[^postgres-limit] 모바일 무한 스크롤은 임의 page 이동보다 앞에서 본 항목 다음을 안정적으로 이어 받는 일이 중요하므로, 최신순 게시글과 댓글은 마지막 `(created_at, id)` 뒤를 찾는 keyset pagination으로 시작했습니다.

첫 predicate는 의미가 가장 직접적인 `created_at < cursorCreatedAt OR (created_at = cursorCreatedAt AND id < cursorId)`였습니다. 전체 순회와 동일 timestamp fixture에서는 중복·누락 없이 정확했지만, 큰 댓글 데이터로 `EXPLAIN (ANALYZE, BUFFERS)`를 확인하자 이 조건이 composite index의 경계가 아니라 filter로 평가됐습니다. 깊어질수록 제거한 row와 buffer가 `3 → 728 → 1,459`, `6 → 23 → 42`로 늘어 API가 정확하다는 사실과 DB가 keyset의 구조적 이점을 얻는다는 사실이 같지 않음을 확인했습니다.

API 계약과 cursor 형식은 그대로 두고 repository predicate만 `(created_at, id) < (?, ?)` row-value comparison으로 바꿨습니다. PostgreSQL이 문서화한 left-to-right 비교 semantics와 `(created_at DESC, id DESC)` index ordering이 맞물려 조건이 `Index Cond`로 내려갔고, 가장 깊은 측정의 buffer는 약 3으로 유지됐습니다.[^postgres-row-comparison] 측정 SQL은 약 `0.285ms → 0.112ms`였지만 작은 로컬 API의 체감 향상 자체보다, page가 깊어져도 불필요한 scan work가 늘지 않는 구조를 얻은 것이 핵심입니다. 변경 뒤에도 전체 순회와 timestamp tie fixture로 같은 결과를 다시 검증했습니다.

정렬은 `(created_at DESC, id DESC)`로 고정해 timestamp tie도 deterministic하게 만들고, cursor는 두 값을 URL-safe Base64로 감춰 client가 내부 형식에 의존하지 않게 했습니다. `PostReadService`는 `limit + 1`행으로 다음 page를 판정합니다. 이 방식은 deep page와 concurrent insert에 적합하지만 cursor가 page number처럼 읽기 쉽거나 임의 page 이동에 적합하지는 않습니다.

브라우저에서는 별도 scroll listener 계산 대신 `IntersectionObserver` sentinel과 `useInfiniteQuery`를 연결했습니다. `useInfiniteScrollTrigger`는 진행 중 요청을 끄고 200px 여유에서 다시 arm해 한 cycle당 한 요청만 보냅니다.

현재 제품 값은 다음과 같습니다.

| 흐름 | 값 | 동작 |
| --- | ---: | --- |
| 게시글 목록 | `POST_LIST_PAGE_SIZE = 15` | 최초와 후속 페이지 모두 최대 15개 |
| 댓글 최초 | `INITIAL_COMMENT_COUNT = 2` | 과제 요구대로 최초 정확히 최대 2개 |
| 댓글 “더보기” 이후 | `COMMENT_PAGE_SIZE = 30` | cursor별 최대 30개씩 연속 로드 |
| scroll 선조회 | `rootMargin = 200px` | sentinel 도달 전에 다음 요청 시작 |
| 검색 입력 | `SEARCH_DEBOUNCE_MS = 300` | 입력이 멈춘 뒤 검색어 확정 |

댓글의 최초 2개는 page-size 최적화 대상이 아니라 명시적 제품 계약입니다. 그래서 detail 재방문 때 query를 제거해 다시 `limit=2`부터 시작하고, 사용자가 “더보기”를 누르기 전에는 continuation sentinel을 활성화하지 않습니다. 그 이후 page만 30개씩 받습니다.

## 직관으로 시작해 측정으로 다시 정한 제품 상수

첫 구현은 동작을 완성하기 위한 보수적 초기값으로 게시글 10개, 댓글 continuation 20개, `rootMargin` 200px, 검색 debounce 300ms를 사용했습니다. 이 값들은 일반적인 관례를 근거로 확정한 것이 아니므로 기능이 안정된 뒤 375×778 Chromium, 고정 4G(지연 200ms, 다운로드 1.6Mbps), clean seed 39 readable posts 조건에서 후보를 한 번에 하나씩 바꿔 비교했습니다. 최초 댓글 `limit=2`는 과제 계약이므로 실험 대상에서 제외했습니다.

- 게시글 `10/15/20`: 전체 순회 요청은 각각 `4/3/2`회였습니다. 15는 10보다 요청 경계를 하나 줄이면서, 최초 화면 아래 항목이 8개로 20의 13개보다 적었습니다. 20이 15보다 아낀 것은 요청 1회와 약 424B의 envelope/header overhead였으므로 초기 선조회를 더 늘리지 않았습니다.
- 댓글 continuation `10/20/30`: readable 댓글 1,411개인 seed post #2에서 각각 `141/71/47`회였습니다. 20→30은 continuation 요청을 약 34% 줄였고 p95는 248ms로 유지됐으며 30개 append에서 관찰 가능한 rendering pause가 없었습니다. 같은 최종 댓글을 받되 요청/envelope/header 반복만 줄인 결과입니다.
- `rootMargin 100/200/400px`: 100px은 loading UI가 노출됐고, 400px은 사용자 scroll 전 page 2를 요청했습니다. 두 현상이 없었던 200px을 유지했습니다.
- 검색 debounce `200/300/500ms`: 같은 검색어를 사람처럼 연속 입력했을 때 200ms는 짧은 250ms 멈춤에도 요청을 시작했고, 500ms는 최종 입력 뒤 결과가 그만큼 늦게 보였습니다. 300ms는 중간 pause를 흡수하면서 500ms보다 최종 결과에 약 200ms 빨리 도달했습니다.

측정 결과 게시글과 댓글 page size만 `10 → 15`, `20 → 30`으로 바꾸고, `rootMargin=200px`와 debounce 300ms는 유지했습니다. 적용 뒤 게시글은 `15/15/9`의 3회 응답으로 39개, 댓글은 최초 2개 뒤 30개 단위로 1,411개를 끝까지 순회해 중복·누락과 종료 후 추가 요청이 없음을 다시 확인했습니다. 값 하나가 모든 환경의 정답이라는 뜻은 아니며, 이 seed·viewport·network profile에서 사용자에게 loading gap을 보이지 않으면서 선조회와 요청 반복을 줄인 균형점입니다.

## 검색·정렬을 추가한 이유와 요청 제어

39개 목록에서도 특정 대화를 다시 찾거나 반응이 큰 글을 보는 탐색 경로가 없었습니다. 사람·프로필 중심 기능을 새로 만들기보다 대화 재발견을 돕는 것이 앞서 정한 커뮤니티 해석에 맞았습니다. 다만 과제 계약은 최신순이므로 이를 기본값으로 보존하고, 좋아요순과 제목·본문 검색을 선택 기능으로 추가했습니다. 이미 받은 client page만 필터링하면 “검색 결과 없음”이 전체 데이터의 결론처럼 보이는 오류가 생기므로 `PostController` → `PostReadService` → `PostRepository`에서 server-side로 처리합니다.

검색은 `%`, `_`, `\\`를 literal로 escape한 뒤 제목 또는 본문에 case-insensitive substring match를 적용합니다. 최신순 cursor와 좋아요순 `(likeCount, createdAt, id)` cursor는 형식을 분리해 다른 ordering에 재사용하면 거절합니다. query key에도 sort와 검색어를 포함해 각 조합이 첫 page부터 독립적으로 시작합니다. likes ordering은 탐색 가치를 얻는 대신 count가 concurrent mutation으로 움직일 수 있어 snapshot ranking까지는 보장하지 않습니다.

검색 input과 서버 query를 같은 state로 즉시 연결하면 글자마다 요청이 생겼습니다. debounce는 짧은 입력 연속을 한 요청으로 묶어 요청 수를 줄이지만, 이미 시작한 이전 요청을 멈추지는 못합니다. 그래서 `usePostList`의 query function이 TanStack Query가 제공하는 `AbortSignal`을 실제 `fetch`에 전달하도록 했습니다.[^tanstack-cancellation] query key 분리로 서로 다른 검색 결과 cache가 섞이지 않게 하고, signal 소비로 더 이상 필요한 화면이 아닌 이전 요청도 취소합니다. 실제 검증에서는 다섯 글자를 연속 입력했을 때 확정 요청 한 번만 발생했고, 일부러 느리게 만든 이전 검색은 abort된 뒤 최종 검색 결과가 유지됐습니다. 즉 300ms debounce와 cancellation은 각각 요청 생성 억제와 이미 생성된 요청의 수명이라는 다른 문제를 해결합니다.

## 서버 상태와 cache 전략을 정한 과정

같은 게시글은 최신 목록, 검색 결과, 좋아요순 목록과 상세 cache에 동시에 존재합니다. 여기에 수정·삭제·좋아요·댓글 수 변경이 생기므로 화면마다 `useEffect`로 fetch와 동기화를 따로 관리하면 어느 copy가 오래된 상태인지 추적하기 어려웠습니다. 이 문제가 API 데이터를 local/global UI state와 분리하고 TanStack Query를 server-state 계층으로 둔 직접적인 이유였습니다.

서버가 원본인 게시글 목록·상세, 댓글 page와 좋아요는 query cache에 둡니다. `frontend/lib/query/keys.ts`는 page size, `latest`/`likes`, 정규화된 검색어를 key에 포함해 서로 다른 결과와 cursor chain이 섞이지 않게 합니다. cursor 자체는 infinite query의 `pageParam`입니다. 반면 폼 입력, 검색창 열림, 메뉴·확인 dialog와 댓글 “더보기”처럼 화면을 닫으면 끝나는 상태는 local state에 둡니다. 이 경계 덕분에 UI만을 위한 별도 global store는 추가하지 않았습니다.

cache를 도입한 뒤에도 mutation 이후의 선택은 남았습니다. 모든 목록을 무조건 invalidate하면 정확하지만 network 낭비와 화면 깜빡임이 생기고, 반대로 모든 결과를 client에서 직접 고치면 검색 membership이나 likes ordering을 잘못 추론할 수 있습니다. 구현을 진행하며 “서버 응답만으로 결과를 정확히 증명할 수 있는가”를 기준으로 두 극단 사이의 규칙을 정했습니다. TanStack Query의 mutation 후 invalidation은 관련 query를 stale 처리하고 재조회하는 공식 mechanism입니다.[^tanstack-invalidation]

- 최신순 목록의 loaded post 좋아요 수와 댓글 수는 직접 반영합니다.
- 좋아요 toggle 뒤 likes-sort 목록은 순서가 바뀔 수 있어 invalidate합니다.
- 게시글 수정 뒤 검색 목록은 검색 포함 여부가 바뀔 수 있어 invalidate합니다.
- 생성 글의 검색 포함 여부나 likes 순위도 client가 단정하지 않고 재조회합니다.

이 규칙은 `frontend/lib/query/postListCache.ts`에 모았습니다. 모든 mutation은 server-confirmed 방식으로, 성공 전 cache를 바꾸지 않아 rollback 복잡성과 false success를 피합니다. 그 대가로 optimistic UI보다 좋아요 반응이 한 network round-trip 늦습니다.

## 검증과 소유권

버튼을 숨기거나 submit을 disable하는 것만으로는 직접 API 호출을 막을 수 없습니다. 따라서 frontend validation은 빠른 UX feedback만 담당하고, backend `MutationValidator`가 제목 필수·20 Unicode code point 이하·본문/댓글 nonblank 규칙을 다시 검사합니다.

과제는 login이나 user table 없이 requester header/cookie를 계약으로 줍니다. 그래서 `RequesterResolver`의 우선순위를 `x-user-id` header → 같은 이름의 cookie → `apr_tester`로 한곳에 고정하고 `author == requester`를 소유권으로 사용했습니다. `PostMutationService`와 `CommentMutationService`가 readable target을 lock한 뒤 소유권을 확인하므로 UI affordance를 우회한 mutation도 거절됩니다. 실제 인증 체계로 확장할 수 있는 model은 아니지만, 과제 계약 밖의 user domain을 만들지 않으면서 backend를 authoritative boundary로 유지합니다.

## Figma를 제품 동작으로 옮긴 과정

공식 Figma는 핵심 화면과 visual hierarchy를 제공하지만 모든 runtime 상태를 정의한 명세는 아닙니다. 그래서 보이는 모양을 그대로 복제하는 것과, 디자인에 없는 제품 규칙을 임의로 만드는 일을 구분했습니다. 기본 viewport, typography, 색상, 작성·상세·삭제 확인의 의미는 유지하고, 빠진 동작은 앞서 정한 “글과 대화 중심 커뮤니티” 해석과 실제 375×778 실행 결과를 기준으로 채웠습니다.

목록의 첫 판단 대상은 작성자보다 글 내용이라고 보고 제목 → 본문 preview → 작성자·반응·시간 순서의 content-first hierarchy를 택했습니다. 처음 retouch한 98px 행은 한 화면에 더 많이 보였지만 정보가 과밀했고, 실제 viewport에서 읽기와 touch 분리가 나아지는 108px rhythm으로 조정했습니다. 이 과정에서도 팔로우·팔로워, 상세 프로필, 인플루언서 표시나 개인화 feed처럼 API와 데이터가 뒷받침하지 않는 SNS 기능은 추가하지 않았습니다.

초기 화면은 피드 소비에는 충분했지만 특정 대화를 다시 찾는 진입점이 없었습니다. 검색과 최신순·좋아요순을 추가하되 별도 화면으로 content를 밀어내지 않고 44px discovery toolbar 한 줄에 모았습니다. app title까지 고정하면 작은 화면의 세로 공간을 계속 점유하므로 title은 scroll되고 discovery toolbar만 sticky로 남겼습니다. 검색을 열면 새 modal이나 pill을 겹치지 않고 같은 toolbar가 `← · 검색 icon · input`의 flat mode로 바뀌며, Escape와 뒤로 닫기, focus 복귀, iOS 확대를 피하는 16px input을 포함합니다.

본인 글의 action은 처음에는 모바일 관례를 따라 bottom sheet로 해석할 수 있었습니다. 하지만 실제 command가 수정과 삭제 두 개뿐이라 화면 전체 폭, scrim과 별도 cancel affordance가 action의 무게보다 컸습니다. 최종 구현은 `⋮` 바로 아래 144px non-modal popover로 바꿔 trigger와 action의 공간적 관계를 보존했습니다. 바깥 클릭과 Escape로 닫고 trigger로 focus를 돌려보냅니다. 반면 삭제는 되돌리기 어려운 별도 단계이므로 중앙 confirmation dialog와 scrim을 유지하고, 확인 전에는 실제 mutation을 보내지 않습니다.

loading과 error UI는 빠진 상태가 아니라 코드에 구현돼 있습니다. 목록은 최초 loading·최초 error·다음 page loading·다음 page error와 retry를, 상세는 loading·not found·error를, 댓글은 최초와 continuation의 loading/error/retry를 각각 구분합니다. 작성·수정·삭제·좋아요 같은 mutation도 pending 동안 중복 실행을 막고 실패 메시지를 표시합니다. 정적 Figma가 이 상태를 그리지 않았기 때문에 runtime에서 보완한 것이며, 디자인 미구현을 그대로 방치한 것이 아닙니다.

keyboard focus, Escape 종료, popover·dialog를 연 trigger로의 focus 복귀, destructive confirmation, `prefers-reduced-motion` 대응도 같은 이유로 추가했습니다. 애니메이션은 상태 변화를 설명하는 범위로 제한하고 reduced-motion 환경에서는 제거합니다. 세부 근거와 공식 디자인/제품 결정의 경계는 `docs/UI_SPEC.md` 및 `design/design-retouch/readme.md`에 기록했습니다. 다만 공식 Figma 원본과 새로 캡처한 모든 화면을 pixel 단위로 대조한 결과까지 주장하지는 않습니다.

## 제품 동작을 끝까지 다듬으며 바뀐 선택

댓글 query는 처음에 다른 server state처럼 shared `QueryClient`의 cache 수명을 그대로 사용했습니다. 한 번 “더보기”로 continuation page를 받은 뒤 같은 SPA session에서 상세를 다시 방문하자, cached page가 먼저 복구되어 사용자가 누르기 전부터 최초 2개 계약을 우회하는 문제가 드러났습니다. 댓글 continuation의 재사용 가치보다 방문마다 명시적 gate를 지키는 일이 중요하다고 판단해 이 query에 `gcTime: 0`을 적용했습니다. 이제 상세 재방문도 다시 `limit=2`에서 시작하고 “더보기” 전에는 continuation sentinel이 열리지 않습니다. 대가로 방금 본 긴 댓글 목록도 재방문하면 다시 요청합니다.

목록의 각 제목 link에는 Next.js의 route prefetch가 기본 적용돼 있었습니다. 전체 39개를 순회해 보니 detail route 선조회가 40회 발생했고 상당수가 중간 취소됐지만, 동적 route shell은 약 40~150B에 그쳤고 실제 클릭에는 route/chunk 요청이 다시 필요했습니다. 클릭 제목이 보이기까지의 측정도 prefetch를 켠 경우가 더 빠르지 않았습니다. 그래서 반복되는 게시글 detail link만 `prefetch={false}`로 바꾸고, 작성처럼 정적이며 재사용 가치가 있는 link는 기본 동작을 유지했습니다. prefetch 전체를 끈 것이 아니라 실제 hit 가능성과 비용을 route 성격별로 다시 판단한 결과입니다.

## 최종 구조

앞의 결정을 코드에서 유지하기 위해 HTTP 처리, 소유권·transaction, SQL을 변경 이유가 다른 경계로 나눴습니다. 한 class에 섞으면 같은 규칙을 endpoint마다 반복하고 backend authorization을 UI 상태에 의존하기 쉽기 때문입니다.

- Controller: `PostController`, `CommentController`가 HTTP parameter/body와 응답 경계를 담당합니다.
- Service: `PostReadService`, `PostMutationService`, `CommentMutationService`가 검증, 소유권, transaction과 도메인 규칙을 담당합니다.
- Repository: `PostRepository`, `CommentRepository`, `PostLikeRepository`가 SQL과 row mapping을 담당합니다.
- Common: `RequesterResolver`, `MutationValidator`, cursor codec과 공통 error handler가 횡단 규칙을 한곳에 둡니다.

프론트엔드의 App Router page는 route parameter를 feature에 연결하는 얇은 경계로 두고, `features/post-list`, `features/post-detail`, `features/post-form`으로 사용자 흐름을 나눴습니다. API client·query cache·공통 hook은 `frontend/lib`에 둡니다. 이 분리는 소규모 과제에 계층 수를 추가하지만, UI가 숨긴 버튼은 보안 경계가 아니며 service authorization이 최종 판단이라는 점을 분명하게 합니다.

## 테스트와 검증

이 구현의 위험은 happy path보다 boundary에 있습니다. 같은 timestamp의 cursor, 삭제 parent의 child 접근, header/cookie 우선순위, 재기동 seed와 mutation 뒤 여러 cache가 대표적입니다. 그래서 단위 test만으로 끝내지 않고 실제 PostgreSQL·Compose·375×778 Chromium을 층별로 사용했습니다.

백엔드 JUnit/Mockito tests는 requester 우선순위, Unicode validation, cursor 정밀도와 malformed input, seed parsing/readiness, mutation 소유권, soft-delete/not-found, 좋아요와 검색·정렬 계약을 다룹니다. 관련 test는 `backend/src/test/java/com/apr/community` 아래에 있습니다.

`docs/VERIFICATION.md`에는 실제 PostgreSQL/Compose 및 Chromium으로 다음을 검증한 근거가 있습니다.

- fresh volume 전체 기동, `/health` readiness와 alternate ports
- 원본 ID·삭제 상태·sequence·`seed_history` 멱등성
- 39개 readable post와 최다 1,411개 댓글의 cursor 완전 순회, tie 순서, 중복·누락·종료 조건
- requester header/cookie 전환, 소유권, 좋아요 격리, post CRUD와 comment create/delete
- soft-deleted row의 물리적 보존과 API/cache에서의 제거
- latest/likes/search 조합, stale 검색 요청 취소와 mutation 후 membership/order 갱신
- 375×778의 필수 화면, fixed control, horizontal overflow와 loading/error 흐름

문서만 보고 통과로 간주하지 않고 실행 증거가 있는 항목만 `PASS`/`VERIFIED`로 기록했습니다. 성능 데이터셋과 SQL plan 측정은 `docs/PERFORMANCE.md`에 분리했습니다.

## 트레이드오프와 남은 제한

- likes-sort는 mutable `likeCount`를 순위에 사용합니다. 요청 중 다른 사용자의 좋아요로 행이 이동하면 페이지 사이 중복 가능성이 있어 UI에서 같은 post ID의 첫 항목만 렌더링합니다. 과제 범위에서 snapshot/versioned ranking은 추가하지 않았습니다.
- 네트워크 pagination은 한 번에 1,411개 댓글을 받지 않게 하지만 사용자가 끝까지 순회하면 DOM node도 결국 누적됩니다. 이 데이터 규모와 모바일 과제 범위에서는 virtualization을 추가하지 않았습니다.
- 검색은 title/content substring에 한정했습니다. 전문 검색 index, 형태소 분석과 relevance ranking은 39개 seed와 과제 범위에 비해 과도해 추가하지 않았습니다.
- 공식 Figma 원본과 새로 캡처한 모든 화면을 fresh pixel-level로 대조하지는 않았습니다. 기록된 design evidence와 375×778 runtime을 검증했지만 pixel-perfect 일치를 주장하지 않습니다.

더 자세한 결정 배경은 `docs/ARCHITECTURE.md`, 요구사항 traceability는 `docs/REQUIREMENTS.md`, 검증 절차와 결과는 `docs/VERIFICATION.md`를 참고할 수 있습니다.

[^spring-jdbc-client]: [Spring Framework — Unified JDBC Query/Update Operations: `JdbcClient`](https://docs.spring.io/spring-framework/reference/data-access/jdbc/core.html#jdbc-JdbcClient)
[^spring-flyway]: [Spring Boot — Database Initialization: Execute Flyway Database Migrations on Startup](https://docs.spring.io/spring-boot/how-to/data-initialization.html#howto.data-initialization.migration-tool.flyway)
[^flyway-history]: [Redgate Flyway — Schema History Table](https://documentation.red-gate.com/flyway/flyway-concepts/migrations/flyway-schema-history-table)
[^tanstack-infinite]: [TanStack Query — Infinite Queries](https://tanstack.com/query/latest/docs/framework/react/guides/infinite-queries)
[^tanstack-query-keys]: [TanStack Query — Query Keys](https://tanstack.com/query/latest/docs/framework/react/guides/query-keys)
[^tanstack-invalidation]: [TanStack Query — Invalidations from Mutations](https://tanstack.com/query/latest/docs/framework/react/guides/invalidations-from-mutations)
[^tanstack-cancellation]: [TanStack Query — Query Cancellation](https://tanstack.com/query/latest/docs/framework/react/guides/query-cancellation)
[^next-css-modules]: [Next.js — CSS Modules](https://nextjs.org/docs/app/getting-started/css#css-modules)
[^postgres-identity]: [PostgreSQL — Identity Columns](https://www.postgresql.org/docs/current/ddl-identity-columns.html)
[^postgres-limit]: [PostgreSQL — `LIMIT` and `OFFSET`](https://www.postgresql.org/docs/current/queries-limit.html)
[^postgres-row-comparison]: [PostgreSQL — Row Constructor Comparison](https://www.postgresql.org/docs/current/functions-comparisons.html#ROW-WISE-COMPARISON)

---

# 3. 교체 전 검토 체크리스트

- [x] 과제 README 요구사항(기술 선택·상태 설계·구조·데이터 적재·테스트)을 모두 설명했다.
- [x] 설명을 실제 file/class/function에 연결했다.
- [x] APR 제공 starter와 추가 구현을 구분했다.
- [x] 주요 기술 선택을 문제·대안·선택·적합성·공식 문서 근거·trade-off 순서로 설명하고, 공식/primary source만 인용했다.
- [x] 의도적인 UI/UX 개선 및 공식 디자인과의 차이를 기록했다.
- [x] 선택하지 않은 대안과 현재 제한을 trade-off로 기록했다.
- [x] 기본 `docker compose up`과 기본 endpoint를 상단에 안내했다.
- [x] alternate ports와 `BACKEND_PORT` 변경 시 `--build`가 필요한 이유를 설명했다.
- [x] Flyway가 없는 것처럼 보이는 기존 starter 문구 등 현재와 맞지 않는 설명을 교체안에서 제거했다.
- [x] 성능 수치는 선행 실측과 repository evidence로 확인된 범위만 사용하고, page size가 원본 data 자체를 줄인다고 표현하지 않았다.
- [x] 제출용 교체안에 빈 절, TODO, template comment를 남기지 않았다.
