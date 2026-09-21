# Fashion Archive

계절·품목별 코디 사진과 "사진 속 아이템과 비슷한 제품" 링크를 모아 두는 개인 큐레이션 사이트입니다.

## 기술 스택

- **Backend**: Java, Spring MVC (eGovFrame 4.1), MyBatis
- **Database**: PostgreSQL
- **View**: JSP, Tiles, JSTL
- **인증/보안**: Spring Security(BCrypt), 세션 기반 로그인
- **인프라**: Docker(Tomcat 9), nginx 리버스 프록시

## 주요 기능

- 계절·품목별 코디 사진 등록 및 조회
- 사진 속 아이템과 비슷한 제품 링크 등록·수정·삭제
- 관리자 로그인 및 사진 업로드/수정/삭제 (세션 기반 인증)
- 업로드 사진은 서버 로컬 디스크에 저장하고 nginx가 정적 파일로 서빙

## 화면 구성

- `/`, `/c/{slug}` — 코디 사진 목록(전체/카테고리별)
- `/photos/{id}` — 사진 상세 (비슷한 제품 링크 포함)
- `/login` — 관리자 로그인
- `/admin/photos/new`, `/admin/photos/{id}/edit` — 사진 등록/수정 (로그인 필요)
