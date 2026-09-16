# Fashion Archive

계절·품목별 코디 사진과 "사진 속 아이템과 비슷한 제품" 링크를 모아 두는 개인 큐레이션 사이트.
JSP + Spring MVC(eGovFrame 4.1) + MyBatis + PostgreSQL. 사진 파일은 Supabase Storage.

## 구조

- 화면 컨트롤러(`*Controller`)는 뷰 이름만 반환하고, 데이터·기능은 `*ApiController`(JSON) + 화면 JS(`resources/js/app/**`)가 처리한다.
- `Service`는 DAO 위임만, `DAO`는 `SqlSessionTemplate` + `sqlmap/mappers/**/*.xml`.
- 조회 결과 `Map`의 키는 컬럼명 그대로(`image_url`, `item_label`).
- 로그인은 세션 + `LoginInterceptor`(`/admin/**`, `/api/admin/**`), 비밀번호는 BCrypt.

## 환경변수

| 이름 | 설명 | 기본값 |
|---|---|---|
| `FASHION_DB_URL` | JDBC URL | `jdbc:postgresql://localhost:5432/fashion` |
| `FASHION_DB_USER` | DB 사용자 | `postgres` |
| `FASHION_DB_PASSWORD` | DB 비밀번호 | (필수) |
| `FASHION_SUPABASE_URL` | `https://xxxx.supabase.co` | (필수) |
| `FASHION_SUPABASE_SERVICE_KEY` | Supabase `service_role` 키. 서버에서만 쓴다 | (필수) |
| `FASHION_SUPABASE_BUCKET` | 공개(Public) 버킷 이름 | `photos` |

## 로컬 실행

1. DB 생성과 스키마 적용 (PowerShell). psql 클라이언트 인코딩을 UTF-8로 맞춰야 한글 시드가 깨지지 않는다.
   ```
   psql -U postgres -c "CREATE DATABASE fashion ENCODING 'UTF8';"
   $env:PGCLIENTENCODING='UTF8'; psql -U postgres -d fashion -f src/main/resources/schema.sql
   ```
2. 관리자 계정 1회 등록. `src/test/java/com/fashion/PasswordHashTool.java`를 실행해 BCrypt 해시를 만든 뒤 **psql 대화형**에서 입력한다. (PowerShell `-c "..."` 안에서는 `$2a$10$...`의 `$`가 변수로 치환되어 해시가 잘린다.)
   ```
   psql -U postgres -d fashion
   INSERT INTO users (login_id, password_hash) VALUES ('admin', '$2a$10$...');
   ```
3. Supabase: 프로젝트 생성 → Storage에 Public 버킷 `photos` 생성 → Project Settings › API에서 URL과 `service_role` 키 확보.
4. IntelliJ Tomcat 실행 설정(Startup/Connection › Environment Variables, Run/Debug 각각)에 위 환경변수 입력 → 실행 → http://localhost:8080/

## 테스트

```
mvn test
```

## 배포 (Docker)

```
mvn -q package -DskipTests
docker build -t fashion-archive .
docker run --rm -p 8080:8080 \
  -e FASHION_DB_URL=jdbc:postgresql://<host>:5432/<db> -e FASHION_DB_USER=... -e FASHION_DB_PASSWORD=... \
  -e FASHION_SUPABASE_URL=... -e FASHION_SUPABASE_SERVICE_KEY=... -e FASHION_SUPABASE_BUCKET=photos \
  fashion-archive
```

- Supabase Postgres에 붙일 때는 대시보드 Connect › JDBC 항목의 URL을 쓴다. 로컬 Docker에서 호스트 Postgres에 붙을 때는 `host.docker.internal`.
- 컨테이너 호스팅(Railway / Render / Fly.io 등)은 Git 연결 후 위 환경변수를 입력하면 된다. 각 서비스의 현재 요금·무료 티어는 배포 시점에 확인한다.
