# Fashion Archive

계절·품목별 코디 사진과 "사진 속 아이템과 비슷한 제품" 링크를 모아 두는 개인 큐레이션 사이트.
JSP + Spring MVC(eGovFrame 4.1) + MyBatis + PostgreSQL. 사진 파일은 서버 로컬 디스크에 저장하고 nginx가 정적 파일로 서빙한다.

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
| `FASHION_UPLOAD_DIR` | 사진을 저장할 서버 디렉터리(컨테이너 내부 경로, 호스트 디렉터리를 볼륨으로 마운트) | `/data/fashion-uploads` |
| `FASHION_UPLOAD_BASE_URL` | 위 디렉터리를 서빙하는 공개 URL(nginx가 static으로 매핑) | (필수) |

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
3. 사진 저장 디렉터리 생성 후, 그 경로를 웹 서버(nginx 등)에서 정적 파일로 서빙하도록 설정하고 그 공개 URL을 `FASHION_UPLOAD_BASE_URL`로 지정.
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
  -e FASHION_UPLOAD_DIR=/data/fashion-uploads -e FASHION_UPLOAD_BASE_URL=https://<domain>/uploads \
  -v /host/path/fashion-uploads:/data/fashion-uploads \
  fashion-archive
```

- 컨테이너는 `/p1` 컨텍스트로 배포된다(리버스 프록시로 `/p1/` 하위 경로에 붙이기 위함). 로컬에서 바로 확인할 때는 http://localhost:8080/p1/ 로 접속한다.
- 로컬 Docker에서 호스트 Postgres에 붙을 때는 `host.docker.internal`(Linux 호스트에서 컨테이너를 실행할 때는 `--network host`를 쓰거나 호스트의 실제 IP를 사용).
- `-v` 볼륨 마운트로 컨테이너 밖 호스트 디렉터리에 사진을 저장해야 컨테이너를 재생성해도 파일이 남는다. 웹 서버(nginx 등)가 그 호스트 디렉터리를 `FASHION_UPLOAD_BASE_URL` 경로로 정적 서빙하도록 별도 설정이 필요하다.
- 컨테이너 호스팅(Railway / Render / Fly.io 등)에 올릴 경우 영구 볼륨과 정적 파일 서빙 방식은 서비스별로 다르므로 배포 시점에 확인한다.
