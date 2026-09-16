# Fashion Archive — 설계 문서

작성일: 2026-09-16

## 1. 목적

관리자가 코디·단품 사진을 카테고리(봄/여름/가을/겨울/악세사리/신발)별로 등록하고, 각 사진에 "사진 속 아이템과 비슷한 제품" 링크를 아이템 이름별로 묶어 두는 개인 큐레이션 사이트. 방문자는 카테고리 메뉴 → 사진 그리드 → 사진 상세 → 외부 제품 링크로 이동한다.

## 2. 확정 사항

| 항목 | 결정 |
|---|---|
| 등록 주체 | 관리자 1명. 회원 확장 대비로 `photo.owner_id`만 미리 둔다 |
| 사진 단위 | 코디 사진과 단품 사진 모두. 링크는 `item_label`(아우터, 바지 등)로 묶는다 |
| 카테고리 | 사진 1장이 여러 카테고리에 속할 수 있다 (다대다) |
| 스택 | 전통 Spring MVC 6.x (web.xml, 외부 Tomcat 10.1) + JSP + MyBatis + PostgreSQL, JDK 17 |
| 빌드 | Maven, WAR 패키징 |
| DB | Supabase PostgreSQL (JDBC, HikariCP). 로컬 개발 시 로컬 Postgres도 가능 |
| 이미지 저장 | 서버 경유 업로드 → Supabase Storage (REST, Java 표준 HttpClient). DB에는 공개 URL만 저장 |
| 로그인 | Spring Security 폼 로그인, `users` 테이블, BCrypt |
| 배포 | `tomcat:10.1-jdk17` Docker 이미지 + WAR. 호스팅(Railway/Render/Fly.io)은 배포 시점에 가격 확인 후 선택 |

### 검토 후 제외한 선택지

- Vercel: Java 네이티브 런타임 없음. 컨테이너 이미지 배포는 가능하나 JVM+Tomcat 운용 적합성 미확인. 이 스택에서는 사용하지 않는다.
- 브라우저 직접 업로드(서명 URL): 관리자 1명 규모에서 복잡도만 증가.
- 서버 로컬 디스크 저장: 호스팅 볼륨에 종속, 유실 위험.

## 3. 화면과 URL

| URL | 화면 | 권한 |
|---|---|---|
| `GET /` | 홈. 상단 6개 카테고리 메뉴, 본문은 최근 사진 그리드 | 공개 |
| `GET /c/{slug}` | 카테고리별 사진 그리드 | 공개 |
| `GET /photos/{id}` | 사진 상세. 큰 이미지, 메모, `item_label`별로 묶인 링크 목록(새 탭). 관리자 로그인 시 같은 화면에 링크 추가/수정/삭제 폼 노출 | 공개 |
| `GET /login`, `POST /login` | 로그인 | 공개 |
| `GET /admin/photos/new`, `POST /admin/photos` | 사진 등록 (파일, 제목, 메모, 카테고리 체크박스) | ADMIN |
| `GET /admin/photos/{id}/edit`, `POST /admin/photos/{id}` | 사진 수정 (제목, 메모, 카테고리) | ADMIN |
| `POST /admin/photos/{id}/delete` | 사진 삭제 | ADMIN |
| `POST /admin/photos/{id}/links` | 링크 추가 | ADMIN |
| `POST /admin/links/{linkId}` | 링크 수정 (item_label, url, title) | ADMIN |
| `POST /admin/links/{linkId}/delete` | 링크 삭제 | ADMIN |

- 상세 페이지의 관리자 UI: 각 링크가 pre-filled 입력칸(아이템, URL, 제목) + [저장] [삭제]로 표시되고, 하단에 [추가] 폼. `<sec:authorize>`로 노출 제어.
- 아이템 이름 입력은 자유 텍스트이되, 해당 사진에 이미 있는 이름을 `<datalist>`로 제안한다.
- 페이지네이션 없음 (전체 조회). 수백 장 이상이 되면 `?page=` 추가.
- 검색·좋아요·댓글·조회수·회원가입 화면은 범위 밖.

## 4. 프로젝트 구조

```
fashion-archive/
├── pom.xml
├── Dockerfile
├── src/main/java/com/example/fashion/
│   ├── config/      WebConfig, DataConfig, SecurityConfig
│   ├── controller/  PhotoController (/, /c/{slug}, /photos/{id}), AdminController (/admin/**)
│   ├── service/     PhotoService (트랜잭션, 링크 그룹핑), ImageStorage (Supabase Storage HTTP)
│   ├── mapper/      PhotoMapper, CategoryMapper, ProductLinkMapper, UserMapper
│   └── domain/      Photo, Category, ProductLink, User
├── src/main/resources/
│   ├── mapper/*.xml
│   ├── application.properties   (환경변수 참조)
│   └── schema.sql
└── src/main/webapp/
    ├── WEB-INF/web.xml
    ├── WEB-INF/views/*.jsp       home, category, photo-detail, login, admin/photo-form, error/404, error/500
    └── static/style.css
```

계층 규칙
- Controller: 파라미터 바인딩과 뷰 반환만. 로직 없음.
- Service: 트랜잭션 경계. `create()`는 Storage 업로드 → `photo` INSERT → `photo_category` INSERT. 상세 조회 시 링크를 `LinkedHashMap<String, List<ProductLink>>`(item_label 기준, 등록 순서 유지)로 그룹핑.
- ImageStorage: `String upload(MultipartFile)` → 공개 URL, `void delete(String url)`. 외부 저장소 의존은 이 클래스에만 둔다.
- Mapper: SQL은 전부 XML.

환경변수
```
DB_URL, DB_USER, DB_PASSWORD
SUPABASE_URL, SUPABASE_SERVICE_KEY, SUPABASE_BUCKET
ADMIN_USERNAME, ADMIN_PASSWORD_HASH
```
비밀값은 커밋하지 않는다.

의존성: spring-webmvc 6.x, spring-security-web/config/taglibs 6.x, mybatis, mybatis-spring, postgresql, HikariCP, jakarta.servlet.jsp.jstl, junit-jupiter(test). Lombok·이미지 SDK·Mockito 없음.

## 5. DB 스키마

```sql
CREATE TABLE users (
  id            BIGSERIAL PRIMARY KEY,
  username      VARCHAR(50)  NOT NULL UNIQUE,
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
- 초기 관리자: `schema.sql`에 비밀번호를 넣지 않는다. 앱 시작 시 `users`가 비어 있으면 `ADMIN_USERNAME`/`ADMIN_PASSWORD_HASH`로 1행 생성.

## 6. 입력 검증

| 항목 | 규칙 | 실패 시 |
|---|---|---|
| 이미지 파일 | 필수. 매직 바이트로 jpeg/png/webp 판별. 최대 10MB. 저장명은 `UUID + 확장자` | 폼 재표시 |
| 제목 | 필수, ≤100자 | 폼 재표시 |
| 카테고리 | ≥1개 | 폼 재표시 |
| 링크 URL | 필수. `java.net.URI` 파싱 후 scheme이 http/https | 상세로 리다이렉트 + 메시지 |
| item_label | 필수, trim 후 ≤30자 | 상세로 리다이렉트 + 메시지 |

- JSP 출력은 전부 이스케이프(`<c:out>` / `fn:escapeXml`).
- 모든 POST 폼에 CSRF 토큰(`<sec:csrfInput/>`).

## 7. 에러 처리

| 상황 | 처리 |
|---|---|
| 없는 사진/카테고리 | 404 (`@ResponseStatus(NOT_FOUND)` 예외 → `error/404.jsp`) |
| Storage 업로드 실패 | 트랜잭션 롤백, 폼 재표시 + "이미지 업로드 실패". 원인은 서버 로그 |
| DB INSERT 실패 (업로드 후) | 업로드한 파일 삭제 시도. 실패해도 고아 파일만 남고 데이터 불일치 없음 |
| DB 접속 실패 등 | 500 페이지. 세부 정보 비노출, 로그에 스택트레이스 |
| 비로그인 `/admin/**` | 로그인 페이지로 리다이렉트 |

## 8. 테스트

| 대상 | 방식 |
|---|---|
| 링크 그룹핑 | JUnit 5. 링크 5개 → item_label별 3그룹, 등록 순서 유지 |
| URL·파일 형식 검증 | JUnit 5. `javascript:`, 빈 값, 잘못된 매직 바이트 거부 |
| MyBatis 매퍼 | 로컬 Postgres에 `schema.sql` 적용 후 INSERT → SELECT → CASCADE 삭제 1회 확인 |
| 화면 흐름 | 로컬 Tomcat에서 등록 → 상세 → 링크 추가 → 수정 → 삭제 수동 1회 |

MockMvc·Storage 모킹 테스트는 넣지 않는다.

## 9. 실행과 배포

로컬
1. Postgres(로컬 또는 Supabase)에 `schema.sql` 실행
2. 환경변수 설정
3. `mvn package` → WAR를 Tomcat 10.1 `webapps/`에 배치 또는 IDE Tomcat 실행

배포
1. Supabase: 프로젝트 생성, 공개 버킷 1개, `schema.sql` 실행
2. Dockerfile
   ```
   FROM tomcat:10.1-jdk17
   RUN rm -rf /usr/local/tomcat/webapps/*
   COPY target/fashion-archive.war /usr/local/tomcat/webapps/ROOT.war
   ```
3. 컨테이너 호스팅에 Git 연결, 환경변수 입력, 배포. 호스팅과 Supabase 무료 티어 조건은 배포 시점에 확인한다.

## 10. 구현 순서

1. 프로젝트 골격 + 설정 + `schema.sql` → 빈 홈 화면 확인
2. 카테고리 메뉴 + 사진 목록/상세 조회 (읽기 전용)
3. Spring Security 로그인 + 초기 관리자 생성
4. 사진 등록/수정/삭제 + Supabase Storage 업로드
5. 링크 추가/수정/삭제
6. 에러 페이지, 스타일, Dockerfile, 배포
