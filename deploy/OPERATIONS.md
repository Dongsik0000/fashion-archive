# p1 서버 운영 가이드

## 구성

- 외부 요청: nginx 80/443
- 애플리케이션: Docker의 Tomcat 9, `127.0.0.1:8080`
- 데이터베이스: 호스트 PostgreSQL 16, `127.0.0.1:5432`
- 업로드 파일: `/srv/fashion-uploads/{로그인 아이디}/` (사용자별 폴더, 첫 업로드 때 생성)
- 앱 비밀값: `/etc/fashion-archive/app.env`
- nginx 설정 원본: `deploy/nginx-fashion-archive.conf`

Tomcat과 PostgreSQL은 loopback에만 바인딩한다. 외부 요청은 nginx를 통해서만 받는다.

## 코드 변경 후 재배포

서버의 `/srv/devgear/fashion-archive`에서 실행한다.

```bash
mvn test
mvn -q package -DskipTests
sudo docker compose up -d --build
curl -I https://devgear.kr/p1/
```

## Docker 운영 명령

```bash
sudo docker compose ps
sudo docker compose logs -f app
sudo docker compose restart app
sudo docker compose down
sudo docker compose up -d
```

`restart: unless-stopped` 정책 때문에 서버가 재부팅돼도 앱이 다시 시작된다.

## 관리자 로그인

초기 로그인 정보는 서버의 root 전용 파일에 있다.

```bash
sudo cat /etc/fashion-archive/admin-initial-password
```

로그인 주소는 `/p1/login`이다. 비밀번호를 별도 비밀번호 관리자에 저장한 뒤 초기 비밀번호 파일은 삭제한다.

## 회원가입 코드

`/p1/signup`에서 가입하려면 가입 코드가 필요하다. 코드는 `/etc/fashion-archive/app.env`의 `FASHION_SIGNUP_CODE`에 둔다. 값이 비어 있거나 없으면 아무도 가입할 수 없다. 값을 바꾼 뒤에는 `sudo docker compose up -d`로 컨테이너를 다시 만든다.

아이디는 영문 소문자·숫자·`_`·`-`로 3~30자만 쓸 수 있다. 아이디가 사진 폴더 이름으로 쓰이기 때문이다. 같은 IP에서 가입 코드를 5번 틀리면 5분 동안 가입이 막힌다. 가입한 계정은 `users.role`이 `USER`로 저장되고, 자기 사진만 보고 수정할 수 있다.

## DBeaver

Main 설정:

- Host: `localhost`
- Port: `5432`
- Database: `fashion`
- User: `fashion_app`
- Password: `sudo sed -n 's/^FASHION_DB_PASSWORD=//p' /etc/fashion-archive/app.env`로 확인

SSH 설정:

- Host/IP: 서버 공인 IP
- Port: `22`
- User: `ubuntu`
- Authentication: 서버 접속에 쓰는 개인 키

PostgreSQL 5432 포트를 외부에 열 필요가 없다. DBeaver가 SSH 터널을 통해 서버의 localhost로 연결한다.

## PC의 Docker CLI에서 서버 관리

PC의 Docker Desktop 또는 Docker CLI에서 SSH context를 만든다.

```bash
docker context create p1-server --docker host=ssh://ubuntu@SERVER_IP
docker --context p1-server ps
docker context use default
```

서버의 `ubuntu` 사용자는 docker 그룹에 속한다. 그룹 변경은 다음 SSH 로그인부터 적용된다. Docker 그룹은 root와 같은 수준의 권한을 가지므로 SSH 개인 키를 안전하게 보관한다.

## HTTPS

`https://devgear.kr/p1/`로 서비스한다. 인증서는 Certbot(nginx 플러그인)이 발급·갱신한다. 공인 IP나 HTTP로 들어온 요청은 nginx가 `https://devgear.kr`로 301 리다이렉트한다.
