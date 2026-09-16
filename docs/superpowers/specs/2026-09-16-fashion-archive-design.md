# Fashion Archive — 설계 문서

작성일: 2026-09-16 (참고 프로젝트 `C:\dev\workspace\design` 관례 반영판)

## 1. 목적

관리자가 코디·단품 사진을 카테고리(봄/여름/가을/겨울/악세사리/신발)별로 등록하고, 각 사진에 "사진 속 아이템과 비슷한 제품" 링크를 아이템 이름별로 묶어 두는 개인 큐레이션 사이트. 방문자는 카테고리 메뉴 → 사진 그리드 → 사진 상세 → 외부 제품 링크로 이동한다.

## 2. 확정 사항

| 항목 | 결정 |
|---|---|
| 등록 주체 | 관리자 1명. 회원 확장 대비로 `photo.owner_id`만 미리 둔다 |
| 사진 단위 | 코디 사진과 단품 사진 모두. 링크는 `item_label`(아우터, 바지 등)로 묶는다 |
| 카테고리 | 사진 1장이 여러 카테고리에 속할 수 있다 (다대다) |
| 프레임워크 | eGovFrame 4.1 (Spring 5.3.20, `javax.servlet`), JSP + Tiles 3, MyBatis, Java 17 실행 / 11 타깃. 참고 프로젝트와 동일 |
| 설정 방식 | XML (`web.xml`, `dispatcher-servlet.xml`, `context-datasource.xml`) + `egovProps/globals.properties` |
| 빌드 | Maven, WAR 패키징. 외부 Tomcat 9 |
| DB | Supabase PostgreSQL. commons-dbcp + `SqlSessionTemplate` |
| 이미지 저장 | 서버 경유 업로드 → Supabase Storage (REST, Java 표준 `HttpClient`). DB에는 공개 URL만 저장 |
| 로그인 | 세션 `loginId` + `HandlerInterceptor` (참고 프로젝트 방식). 판정 로직은 `CmmnApiController`에, Service는 DAO 위임만. 비밀번호 해시는 **BCrypt** (참고 프로젝트의 SHA-256 대신) |
| 프런트 | jQuery + `App.post()`(fetch JSON) 모듈 패턴, Pretendard, `reset.css`/`common.css` 골격 재사용 |
| 배포 | `tomcat:9-jdk17` Docker 이미지 + WAR. 호스팅(Railway/Render/Fly.io)은 배포 시점에 가격 확인 후 선택 |
| 협업 | 설정·골격은 Claude, 화면·기능은 단계별로 담당을 나눈다 |

### 참고 프로젝트에서 가져오는 것 / 가져오지 않는 것

가져옴: `pom.xml` 골격(eGov 의존성, 버전), `web.xml`·`dispatcher-servlet.xml`·`context-datasource.xml` 구조, Tiles 레이아웃 방식, `Response`/`Constants`/`SessionUtil`/인터셉터 패턴, DAO+`SQL_PATH` 패턴, `common.js`의 `App.ajax/post/get/isEmpty/formToObject`, Pretendard 폰트, `reset.css`, `common.css`.

가져오지 않음: 대시보드·관리 화면과 그 CSS(`style.css`), Chart.js, 감사로그 AOP, Apache POI, commons-net, protobuf, json-simple, hsqldb, log4jdbc, 로그인 실패 잠금(아래 8절 참고).

### 검토 후 제외한 선택지

- Vercel: Java 네이티브 런타임 없음. 이 스택에서는 사용하지 않는다.
- Spring 6 / Jakarta / Tomcat 10: 참고 프로젝트와 맞지 않아 철회.
- Spring Security 필터 체인: 참고 프로젝트가 인터셉터 방식이므로 동일하게 간다. `spring-security-core`만 BCrypt 용도로 유지.
- 브라우저 직접 업로드, 서버 로컬 디스크 저장: 앞선 검토대로 제외.

## 3. 화면과 URL

### 화면 컨트롤러 (뷰 이름 반환)

| URL | 뷰 이름 | 화면 | 권한 |
|---|---|---|---|
| `GET /` | `/fashion/photo/list` | 홈. 상단 6개 카테고리 메뉴, 본문은 최근 사진 그리드 | 공개 |
| `GET /c/{slug}` | `/fashion/photo/list` | 카테고리별 사진 그리드 | 공개 |
| `GET /photos/{id}` | `/fashion/photo/detail` | 사진 상세. 큰 이미지, 메모, `item_label`별 링크 목록(새 탭). 로그인 시 같은 화면에 추가/수정/삭제 UI 노출 | 공개 |
| `GET /login` | `/login/loginMain` (레이아웃 없음) | 로그인 | 공개 |
| `GET /logout` | 리다이렉트 `/` | 세션 무효화 | 로그인 |
| `GET /admin/photos/new` | `/fashion/admin/photoForm` | 사진 등록 폼 | 로그인 |
| `GET /admin/photos/{id}/edit` | `/fashion/admin/photoForm` | 사진 수정 폼 | 로그인 |

- Tiles: `/fashion/*/*` → 레이아웃 `fashionLayout.jsp`(헤더에 카테고리 메뉴 + 로그인/로그아웃) + 본문 `/WEB-INF/jsp/fashion/{1}/{2}.jsp`. `/login/*`는 Tiles 패턴에 걸리지 않아 `InternalResourceViewResolver`로 단독 렌더링 (참고 프로젝트와 동일 구조).

### API 컨트롤러 (`@ResponseBody`, `Response.of(code, message, data)`)

| URL | 입력 | 동작 | 권한 |
|---|---|---|---|
| `POST /api/login` | JSON `{loginId, password}` | BCrypt 검증 → 세션 `loginId` 저장 | 공개 |
| `POST /api/admin/photos` | multipart: `file`, `title`, `memo`, `categoryIds[]` | Storage 업로드 → INSERT. 응답 `data.id` | 로그인 |
| `POST /api/admin/photos/{id}` | JSON `{title, memo, categoryIds[]}` | 사진 정보 수정 | 로그인 |
| `POST /api/admin/photos/{id}/delete` | 없음 | 사진 삭제 (DB CASCADE → Storage 파일 삭제) | 로그인 |
| `POST /api/admin/photos/{id}/links` | JSON `{itemLabel, url, title}` | 링크 추가 | 로그인 |
| `POST /api/admin/links/{linkId}` | JSON `{itemLabel, url, title}` | 링크 수정 | 로그인 |
| `POST /api/admin/links/{linkId}/delete` | 없음 | 링크 삭제 | 로그인 |

- 응답 코드: `Constants.SUCCESS="00"`, `Constants.FAIL="99"`, 로그인 실패 `"01"`. 검증 실패는 `FAIL` + `message`.
- 인터셉터 보호 범위: `/admin/**`, `/api/admin/**`. 비로그인 시 Ajax(`X-Requested-With: XMLHttpRequest`)면 `{"sessionExpired":true}`, 화면 요청이면 `/login`으로 리다이렉트.
- 상세 페이지 관리자 UI: 각 링크가 입력칸(아이템, URL, 제목) + [저장] [삭제], 하단에 [추가] 폼. 저장/삭제/추가는 `App.post()`로 호출 후 해당 영역만 다시 그린다. 아이템 이름은 `<datalist>`로 기존 이름 제안.
- 페이지네이션 없음. 검색·좋아요·댓글·조회수·회원가입 화면은 범위 밖.

## 4. 프로젝트 구조

```
fashion-archive/
├── pom.xml                                   참고 프로젝트 골격에서 불필요 의존성 제거
├── Dockerfile                                FROM tomcat:9-jdk17
├── src/main/java/com/fashion/
│   ├── cmmn/
│   │   ├── controller/CmmnController         /login, /logout
│   │   ├── controller/CmmnApiController      POST /api/login
│   │   ├── interceptor/LoginInterceptor
│   │   ├── storage/SupabaseStorage           upload(MultipartFile) → URL, delete(url)
│   │   ├── service/CmmnService, impl/CmmnServiceImpl   사용자 조회 (DAO 위임)
│   │   ├── dao/CmmnDAO
│   │   └── util/Response, Constants, SessionUtil, Validation
│   └── photo/
│       ├── controller/PhotoController        /, /c/{slug}, /photos/{id}, /admin/photos/**
│       ├── controller/PhotoApiController     /api/admin/**
│       ├── service/PhotoService, impl/PhotoServiceImpl
│       └── dao/PhotoDAO, LinkDAO
├── src/main/resources/
│   ├── egovProps/globals.properties          환경변수 참조 (${env:...} 대신 시스템 프로퍼티/환경변수 치환)
│   ├── spring/context-datasource.xml
│   ├── sqlmap/mybatis-config.xml
│   ├── sqlmap/mappers/fashion/cmmn/cmmn.xml
│   ├── sqlmap/mappers/fashion/photo/photo.xml, link.xml
│   ├── log4j2.xml
│   └── schema.sql
└── src/main/webapp/
    ├── common/taglib.jsp
    ├── WEB-INF/web.xml
    ├── WEB-INF/config/dispatcher-servlet.xml
    ├── WEB-INF/tiles/tiles-layout.xml
    ├── WEB-INF/layout/fashionLayout.jsp      헤더(카테고리 메뉴, 로그인/로그아웃) + body
    ├── WEB-INF/jsp/login/loginMain.jsp
    ├── WEB-INF/jsp/fashion/photo/list.jsp, detail.jsp
    ├── WEB-INF/jsp/fashion/admin/photoForm.jsp
    ├── WEB-INF/jsp/error.jsp
    ├── META-INF/context.xml                  SameSite=Lax 쿠키
    └── resources/
        ├── css/reset.css, common.css (재사용), fashion.css (신규)
        ├── fonts/ (Pretendard woff2만)
        └── js/common/common.js (재사용 + App.upload 추가), app/photo/list.js, detail.js, photoForm.js, app/login/login.js
```

계층 규칙 (참고 프로젝트와 동일)
- `Controller`: 뷰 이름 반환. `ApiController`: `@ResponseBody`, try/catch 후 `Response.of()`.
- `Service` 인터페이스 + `impl`. 트랜잭션은 `@Transactional`(`context-datasource.xml`에 `tx:annotation-driven`).
- `DAO`: `@Repository`, `SqlSessionTemplate` 주입, `SQL_PATH` 상수 + 메서드명.
- 파라미터·결과는 `Map<String,Object>`. 조회 결과 키는 컬럼명 그대로(snake_case: `login_id`, `image_url`, `item_label`). MyBatis `mapUnderscoreToCamelCase`는 HashMap 결과에 적용되지 않으므로 쓰지 않는다.
- 상세 조회 시 링크를 `LinkedHashMap<String, List<Map>>`(item_label 기준, 등록 순서 유지)로 그룹핑해서 JSP에 넘긴다.

환경변수 → `globals.properties`
```
Globals.Fashion.Postgre.Url / UserName / Password
Globals.Fashion.Supabase.Url / ServiceKey / Bucket
```
`globals.properties`에는 키만 두고 값은 `${환경변수}`로 치환한다(`PropertyPlaceholderConfigurer`의 시스템 환경변수 폴백). 비밀값은 커밋하지 않는다.

의존성: `org.egovframe.rte.ptl.mvc`, `org.egovframe.rte.psl.dataaccess`, `javax.servlet-api`(provided), jstl, tiles 3, `postgresql`, `commons-dbcp`, `spring-security-core`(BCrypt), `jackson-databind`, log4j2, `commons-fileupload`(multipart), `junit-jupiter`(test).

## 5. DB 스키마

```sql
CREATE TABLE users (
  id            BIGSERIAL PRIMARY KEY,
  login_id      VARCHAR(50)  NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  role          VARCHAR(20)  NOT NULL DEFAULT 'ADMIN'
);

CREATE TABLE category (
  id         SMALLSERIAL PRIMARY KEY,
  slug       VARCHAR(30) NOT NULL UNIQUE,
  name       VARCHAR(30) NOT NULL,
  sort_order SMALLINT    NOT NULL
);
INSERT INTO category (slug, name, sort_order) VALUES
  ('spring','봄',1), ('summer','여름',2), ('autumn','가을',3), ('winter','겨울',4),
  ('accessory','악세사리',5), ('shoes','신발',6);

CREATE TABLE photo (
  id         BIGSERIAL PRIMARY KEY,
  owner_id   BIGINT       NOT NULL REFERENCES users(id),
  title      VARCHAR(100) NOT NULL,
  memo       TEXT,
  image_url  TEXT         NOT NULL,
  created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE photo_category (
  photo_id    BIGINT   NOT NULL REFERENCES photo(id) ON DELETE CASCADE,
  category_id SMALLINT NOT NULL REFERENCES category(id),
  PRIMARY KEY (photo_id, category_id)
);
CREATE INDEX ON photo_category (category_id);

CREATE TABLE product_link (
  id         BIGSERIAL PRIMARY KEY,
  photo_id   BIGINT      NOT NULL REFERENCES photo(id) ON DELETE CASCADE,
  item_label VARCHAR(30) NOT NULL,
  url        TEXT        NOT NULL,
  title      VARCHAR(100),
  sort_order INT         NOT NULL DEFAULT 0
);
CREATE INDEX ON product_link (photo_id, item_label, sort_order);
```

- 사진 삭제는 CASCADE로 매핑·링크가 함께 삭제된다. Storage 파일 삭제는 DB 삭제 후 호출하며, 실패 시 로그만 남긴다(고아 파일 허용, 불일치 금지).
- 관리자 계정: `PasswordHashTool`(test 소스)로 BCrypt 해시를 만든 뒤 psql **대화형**에서 1회 INSERT한다. PowerShell `-c "..."` 안에서는 `$2a$10$...`의 `$`가 변수로 치환되어 해시가 잘리므로 쓰지 않는다.
  ```sql
  INSERT INTO users (login_id, password_hash) VALUES ('admin', '$2a$10$...');
  ```

## 6. 입력 검증 (서버 측, `cmmn/util/Validation`)

| 항목 | 규칙 | 실패 시 |
|---|---|---|
| 이미지 파일 | 필수. 매직 바이트로 jpeg/png/webp 판별. 최대 10MB. 저장명은 `UUID + 확장자` | `FAIL` + 메시지 |
| 제목 | 필수, trim 후 ≤100자 | `FAIL` + 메시지 |
| 카테고리 | ≥1개, 존재하는 id만 | `FAIL` + 메시지 |
| 링크 URL | 필수. `java.net.URI` 파싱 후 scheme이 http/https | `FAIL` + 메시지 |
| item_label | 필수, trim 후 ≤30자 | `FAIL` + 메시지 |

- JSP 출력은 전부 이스케이프(`<c:out>` / `fn:escapeXml`). JS로 그리는 부분은 `textContent`만 사용.
- CSRF: `/api/admin/**`는 인터셉터에서 `X-Requested-With: XMLHttpRequest` 헤더를 요구한다(교차 출처에서는 이 헤더를 붙일 수 없어 단순 폼 전송이 차단됨). 세션 쿠키는 `SameSite=Lax` (`META-INF/context.xml`의 `CookieProcessor`).

## 7. 에러 처리

| 상황 | 처리 |
|---|---|
| 없는 사진/카테고리 (화면) | 404 → `error.jsp` (web.xml `error-page`) |
| 없는 사진/링크 (API) | `FAIL` + "대상을 찾을 수 없습니다" |
| Storage 업로드 실패 | 트랜잭션 롤백, `FAIL` + "이미지 업로드 실패". 원인은 서버 로그 |
| DB INSERT 실패 (업로드 후) | 업로드한 파일 삭제 시도. 실패해도 고아 파일만 남고 데이터 불일치 없음 |
| 예상 밖 예외 (API) | `ApiController` try/catch → `FAIL` + 일반 메시지, 스택트레이스는 로그 |
| 예상 밖 예외 (화면) | 500 → `error.jsp` |
| 비로그인 보호 경로 접근 | Ajax: `{"sessionExpired":true}` → `App.sessionExpired()`가 `/login`으로 이동. 화면: `/login` 리다이렉트 |

## 8. 로그인

- `POST /api/login` → `cmmnService.selectUserByLoginId(loginId)` → `passwordEncoder.matches()` → 성공 시 세션에 `loginId`, `userId` 저장. 판정은 컨트롤러가 하고 Service/DAO는 조회만 한다 (참고 프로젝트 `CmmnApiController.login()`과 같은 구조).
- 세션 타임아웃 60분, 쿠키 전용 추적 (참고 프로젝트 `web.xml`과 동일).
- 로그인 실패 잠금은 넣지 않는다. 관리자 1명이라 잠기면 DB를 직접 고쳐야 풀린다. BCrypt 자체가 느린 해시라 무차별 대입 비용이 크다. 공격 흔적이 보이면 그때 실패 카운터를 추가한다.

## 9. 테스트

| 대상 | 방식 |
|---|---|
| 링크 그룹핑 | JUnit 5. 링크 5개 → item_label별 3그룹, 등록 순서 유지 |
| `Validation` | JUnit 5. `javascript:`, 빈 값, 잘못된 매직 바이트 거부 |
| MyBatis 매퍼 | Postgres에 `schema.sql` 적용 후 INSERT → SELECT → CASCADE 삭제 1회 확인 |
| 화면 흐름 | 로컬 Tomcat 9에서 로그인 → 등록 → 상세 → 링크 추가 → 수정 → 삭제 수동 1회 |

참고 프로젝트는 `skipTests=true`이지만 이 프로젝트는 위 두 단위 테스트를 `mvn test`로 돌린다.

## 10. 실행과 배포

로컬
1. Postgres(로컬 또는 Supabase)에 `schema.sql` 실행
2. 환경변수 설정 (DB, Supabase, 관리자 해시)
3. `mvn package` → WAR를 Tomcat 9 `webapps/`에 배치 또는 IDE Tomcat 실행

배포
1. Supabase: 프로젝트 생성, 공개 버킷 1개, `schema.sql` 실행
2. Dockerfile
   ```
   FROM tomcat:9-jdk17
   RUN rm -rf /usr/local/tomcat/webapps/*
   COPY target/fashion-archive.war /usr/local/tomcat/webapps/ROOT.war
   ```
3. 컨테이너 호스팅에 Git 연결, 환경변수 입력, 배포. 호스팅과 Supabase 무료 티어 조건은 배포 시점에 확인한다.

## 11. 구현 순서와 담당

| 단계 | 내용 | 담당 |
|---|---|---|
| 1 | pom, web.xml, dispatcher/datasource XML, Tiles 레이아웃, `common.js`/CSS 이식, `schema.sql`, 빈 홈 화면 | Claude |
| 2 | `Response`/`Constants`/`SessionUtil`/인터셉터/`Validation`, 로그인 화면+API, 초기 관리자 생성 | Claude |
| 3 | 카테고리 메뉴 + 사진 목록/상세 조회 (읽기 전용) | 단계 시작 시 결정 |
| 4 | `SupabaseStorage` + 사진 등록/수정/삭제 | 단계 시작 시 결정 |
| 5 | 링크 추가/수정/삭제 (상세 페이지 관리자 UI) | 단계 시작 시 결정 |
| 6 | 에러 페이지, 스타일 마무리, Dockerfile, 배포 | 단계 시작 시 결정 |

각 단계 시작 시 무엇을 왜 만드는지 설명하고 담당을 정한다. 사용자가 작성한 코드는 Claude가 리뷰하고, Claude가 작성한 코드는 사용자가 실행해 확인한다.
