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
