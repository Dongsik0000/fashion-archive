# Fashion Archive Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 관리자가 계절·품목 카테고리별 코디 사진을 올리고, 사진마다 아이템 이름별 "비슷한 제품" 링크를 관리하며, 방문자가 카테고리 → 사진 → 링크로 이동하는 JSP/Spring MVC 사이트.

**Architecture:** 전통 Spring MVC(eGovFrame 4.1, XML 설정) 단일 WAR. 화면은 Tiles 레이아웃 + JSP, 변경 작업은 `ApiController`(JSON) + `App.post()`(fetch). DB는 PostgreSQL(MyBatis, `Map` 파라미터), 사진 파일은 Supabase Storage에 서버 경유 업로드. 로그인은 세션 + 인터셉터, 비밀번호는 BCrypt.

**Tech Stack:** JDK 17, Maven, eGovFrame 4.1 (Spring 5.3.20, MyBatis 3.5.10, commons-dbcp2), Tiles 3.0.5, JSTL, PostgreSQL 42.7.3, spring-security-core 5.6.10 (BCrypt만), Jackson 2.14.2, JUnit 5, Tomcat 9.0.93

**Spec:** `docs/superpowers/specs/2026-09-16-fashion-archive-design.md`

## Global Constraints

- **조회 결과 `Map`의 키는 컬럼명 그대로(snake_case: `login_id`, `image_url`, `item_label`)**. MyBatis의 `mapUnderscoreToCamelCase`는 `resultType=HashMap`에 적용되지 않는다(`MapWrapper.findProperty`가 이름을 그대로 반환. 참고 프로젝트도 `user_lock_yn`처럼 사용). 파라미터 Map의 `#{loginId}` 같은 이름은 우리가 정하므로 camelCase 유지.
- 패키지 루트 `com.fashion`. 계층: `controller`(뷰 이름 반환) / `ApiController`(`@ResponseBody`, `Response.of`) / `service` 인터페이스 + `impl` / `dao`(`SqlSessionTemplate` + `NS` 상수). 파라미터·결과는 `Map<String,Object>`.
- Servlet 3.1 / `javax.servlet`. Spring 6·Jakarta·Spring Boot 사용 금지.
- 설정은 XML. `web.xml` → `/WEB-INF/config/dispatcher-servlet.xml` + `classpath:spring/context-datasource.xml`. 값은 `egovProps/globals.properties`, 비밀값은 환경변수(`FASHION_*`)로만.
- 응답 코드: `Constants.SUCCESS="00"`, `Constants.LOGIN_FAIL="01"`, `Constants.FAIL="99"`.
- 보호 경로: `/admin/**`, `/api/admin/**`. `/api/**`는 `X-Requested-With: XMLHttpRequest` 헤더 필수.
- JSP 출력은 `<c:out>` / `fn:escapeXml`, JS는 `textContent`만. 외부 링크는 `target="_blank" rel="noopener noreferrer"`.
- 이미지: jpg/png/webp, 매직 바이트로 판별, 10MB 이하, 저장명 `UUID.확장자`.
- 커밋 메시지는 `feat:`/`chore:`/`test:`/`docs:` 접두어, 끝에 `Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>`.
- 환경변수 이름: `FASHION_DB_URL`, `FASHION_DB_USER`, `FASHION_DB_PASSWORD`, `FASHION_SUPABASE_URL`, `FASHION_SUPABASE_SERVICE_KEY`, `FASHION_SUPABASE_BUCKET`.

---

## 파일 구조

```
fashion-archive/
├── pom.xml
├── .gitignore
├── Dockerfile
├── README.md
├── src/main/java/com/fashion/
│   ├── cmmn/
│   │   ├── controller/CmmnController.java          /login, /logout
│   │   ├── controller/CmmnApiController.java       POST /api/login
│   │   ├── controller/GlobalModelAdvice.java       모든 화면에 categories 주입
│   │   ├── interceptor/LoginInterceptor.java
│   │   ├── storage/SupabaseStorage.java
│   │   ├── service/CmmnService.java, impl/CmmnServiceImpl.java
│   │   ├── dao/CmmnDAO.java
│   │   └── util/Response.java, Constants.java, Validation.java, NotFoundException.java
│   └── photo/
│       ├── controller/PhotoController.java         /, /c/{slug}, /photos/{id}, /admin/photos/**
│       ├── controller/PhotoApiController.java      /api/admin/**
│       ├── service/PhotoService.java, impl/PhotoServiceImpl.java
│       └── dao/PhotoDAO.java, LinkDAO.java
├── src/main/resources/
│   ├── egovProps/globals.properties
│   ├── spring/context-datasource.xml
│   ├── sqlmap/mybatis-config.xml
│   ├── sqlmap/mappers/fashion/cmmn/cmmn.xml
│   ├── sqlmap/mappers/fashion/photo/photo.xml, link.xml
│   ├── log4j2.xml
│   └── schema.sql
├── src/test/java/com/fashion/
│   ├── PasswordHashTool.java                        관리자 해시 생성용 main
│   ├── cmmn/util/ValidationTest.java
│   └── photo/service/PhotoServiceImplTest.java
└── src/main/webapp/
    ├── common/taglib.jsp
    ├── META-INF/context.xml                         SameSite=Lax
    ├── WEB-INF/web.xml
    ├── WEB-INF/config/dispatcher-servlet.xml
    ├── WEB-INF/tiles/tiles-layout.xml
    ├── WEB-INF/layout/fashionLayout.jsp
    ├── WEB-INF/jsp/error.jsp
    ├── WEB-INF/jsp/login/loginMain.jsp
    ├── WEB-INF/jsp/fashion/photo/list.jsp, detail.jsp
    ├── WEB-INF/jsp/fashion/admin/photoForm.jsp
    └── resources/
        ├── css/reset.css, fashion.css
        ├── fonts/Pretendard-{Regular,Medium,SemiBold,Bold}.woff2
        └── js/common/common.js, app/login/login.js, app/photo/photoForm.js, app/photo/detail.js
```

담당: 1·2단계(Task 1~8) Claude. 3단계부터는 각 단계 시작 시 담당을 정한다.

---

## 1단계. 프로젝트 골격

### Task 1: Maven 프로젝트와 XML 설정

**Files:**
- Create: `pom.xml`, `.gitignore`
- Create: `src/main/webapp/WEB-INF/web.xml`
- Create: `src/main/webapp/WEB-INF/config/dispatcher-servlet.xml`
- Create: `src/main/resources/spring/context-datasource.xml`
- Create: `src/main/resources/egovProps/globals.properties`
- Create: `src/main/resources/sqlmap/mybatis-config.xml`
- Create: `src/main/resources/log4j2.xml`

**Interfaces:**
- Produces: 빈 이름 `sqlSession`(`SqlSessionTemplate`), `passwordEncoder`(`BCryptPasswordEncoder`), `multipartResolver`. 프로퍼티 키 `Globals.Fashion.*`. 뷰 리졸버: Tiles(`/fashion/*/*`) → `InternalResourceViewResolver`(`/WEB-INF/jsp/{view}.jsp`).

- [ ] **Step 1: pom.xml 작성**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.fashion</groupId>
    <artifactId>fashion-archive</artifactId>
    <packaging>war</packaging>
    <version>1.0.0</version>
    <name>fashion-archive</name>

    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <maven.compiler.release>17</maven.compiler.release>
        <spring.maven.artifact.version>5.3.20</spring.maven.artifact.version>
        <org.egovframe.rte.version>4.1.0</org.egovframe.rte.version>
    </properties>

    <repositories>
        <repository>
            <id>egovframe</id>
            <url>https://maven.egovframe.go.kr/maven/</url>
            <releases><enabled>true</enabled></releases>
            <snapshots><enabled>false</enabled></snapshots>
        </repository>
    </repositories>

    <dependencies>
        <!-- 표준프레임워크 실행환경: spring-webmvc 5.3.20, mybatis 3.5.10, mybatis-spring 2.0.7, log4j2 포함 -->
        <dependency>
            <groupId>org.egovframe.rte</groupId>
            <artifactId>org.egovframe.rte.ptl.mvc</artifactId>
            <version>${org.egovframe.rte.version}</version>
            <exclusions>
                <exclusion>
                    <groupId>commons-logging</groupId>
                    <artifactId>commons-logging</artifactId>
                </exclusion>
            </exclusions>
        </dependency>
        <dependency>
            <groupId>org.egovframe.rte</groupId>
            <artifactId>org.egovframe.rte.psl.dataaccess</artifactId>
            <version>${org.egovframe.rte.version}</version>
        </dependency>

        <dependency>
            <groupId>javax.servlet</groupId>
            <artifactId>javax.servlet-api</artifactId>
            <version>3.1.0</version>
            <scope>provided</scope>
        </dependency>
        <!-- eGov 모듈에는 test 스코프로만 있어 직접 선언한다 (참고 프로젝트와 동일) -->
        <dependency>
            <groupId>javax.servlet.jsp.jstl</groupId>
            <artifactId>jstl-api</artifactId>
            <version>1.2</version>
        </dependency>
        <dependency>
            <groupId>taglibs</groupId>
            <artifactId>standard</artifactId>
            <version>1.1.2</version>
        </dependency>
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-dbcp2</artifactId>
            <version>2.9.0</version>
        </dependency>

        <dependency>
            <groupId>org.apache.tiles</groupId>
            <artifactId>tiles-core</artifactId>
            <version>3.0.5</version>
        </dependency>
        <dependency>
            <groupId>org.apache.tiles</groupId>
            <artifactId>tiles-servlet</artifactId>
            <version>3.0.5</version>
        </dependency>
        <dependency>
            <groupId>org.apache.tiles</groupId>
            <artifactId>tiles-jsp</artifactId>
            <version>3.0.5</version>
        </dependency>

        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <version>42.7.3</version>
        </dependency>

        <!-- BCrypt만 사용 -->
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-core</artifactId>
            <version>5.6.10</version>
        </dependency>

        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>2.14.2</version>
        </dependency>

        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.10.2</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <finalName>fashion-archive</finalName>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.10.1</version>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-war-plugin</artifactId>
                <version>3.3.2</version>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.2.5</version>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: .gitignore 작성**

```
target/
.idea/
*.iml
.deptree.txt
```

- [ ] **Step 3: web.xml 작성** — 루트 컨텍스트 없이 DispatcherServlet 하나에 모든 설정을 싣는다 (컨텍스트 분리 시 `@Transactional`·`${}` 치환이 자식 컨텍스트에 적용되지 않는 함정을 피하기 위해).

```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-app_3_1.xsd"
         version="3.1">
    <display-name>fashion-archive</display-name>

    <filter>
        <filter-name>encodingFilter</filter-name>
        <filter-class>org.springframework.web.filter.CharacterEncodingFilter</filter-class>
        <init-param>
            <param-name>encoding</param-name>
            <param-value>UTF-8</param-value>
        </init-param>
        <init-param>
            <param-name>forceEncoding</param-name>
            <param-value>true</param-value>
        </init-param>
    </filter>
    <filter-mapping>
        <filter-name>encodingFilter</filter-name>
        <url-pattern>/*</url-pattern>
    </filter-mapping>

    <servlet>
        <servlet-name>dispatcher</servlet-name>
        <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
        <init-param>
            <param-name>contextConfigLocation</param-name>
            <param-value>
                /WEB-INF/config/dispatcher-servlet.xml
                classpath:spring/context-datasource.xml
            </param-value>
        </init-param>
        <load-on-startup>1</load-on-startup>
        <!-- Servlet 3.1 표준 multipart. 파일 10MB, 요청 전체 12MB -->
        <multipart-config>
            <max-file-size>10485760</max-file-size>
            <max-request-size>12582912</max-request-size>
        </multipart-config>
    </servlet>
    <servlet-mapping>
        <servlet-name>dispatcher</servlet-name>
        <url-pattern>/</url-pattern>
    </servlet-mapping>

    <error-page>
        <error-code>404</error-code>
        <location>/WEB-INF/jsp/error.jsp</location>
    </error-page>
    <error-page>
        <error-code>500</error-code>
        <location>/WEB-INF/jsp/error.jsp</location>
    </error-page>

    <session-config>
        <session-timeout>60</session-timeout>
        <cookie-config>
            <http-only>true</http-only>
        </cookie-config>
        <tracking-mode>COOKIE</tracking-mode>
    </session-config>
</web-app>
```

- [ ] **Step 4: dispatcher-servlet.xml 작성**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:context="http://www.springframework.org/schema/context"
       xmlns:mvc="http://www.springframework.org/schema/mvc"
       xmlns:tx="http://www.springframework.org/schema/tx"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
                           http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context.xsd
                           http://www.springframework.org/schema/mvc http://www.springframework.org/schema/mvc/spring-mvc.xsd
                           http://www.springframework.org/schema/tx http://www.springframework.org/schema/tx/spring-tx.xsd">

    <mvc:resources mapping="/resources/**" location="/resources/" />
    <mvc:annotation-driven />
    <context:component-scan base-package="com.fashion" />
    <tx:annotation-driven />

    <!-- 1순위: Tiles. 정의에 없는 뷰 이름은 2순위 JSP 리졸버로 넘어간다 -->
    <bean id="tilesViewResolver" class="org.springframework.web.servlet.view.UrlBasedViewResolver">
        <property name="viewClass" value="org.springframework.web.servlet.view.tiles3.TilesView" />
        <property name="order" value="1" />
    </bean>
    <bean id="tilesConfigurer" class="org.springframework.web.servlet.view.tiles3.TilesConfigurer">
        <property name="definitions">
            <list>
                <value>/WEB-INF/tiles/tiles-layout.xml</value>
            </list>
        </property>
    </bean>
    <bean id="viewResolver" class="org.springframework.web.servlet.view.InternalResourceViewResolver">
        <property name="prefix" value="/WEB-INF/jsp/" />
        <property name="suffix" value=".jsp" />
        <property name="order" value="2" />
    </bean>

    <!-- web.xml의 multipart-config를 사용. 빈 이름은 반드시 multipartResolver -->
    <bean id="multipartResolver" class="org.springframework.web.multipart.support.StandardServletMultipartResolver" />

    <mvc:interceptors>
        <mvc:interceptor>
            <mvc:mapping path="/admin/**" />
            <mvc:mapping path="/api/admin/**" />
            <bean class="com.fashion.cmmn.interceptor.LoginInterceptor" />
        </mvc:interceptor>
    </mvc:interceptors>
</beans>
```

`LoginInterceptor`는 Task 5에서 만든다. Task 1~3을 기동 확인할 때는 `<mvc:interceptors>` 블록을 주석 처리해 두고, Task 5에서 주석을 푼다.

- [ ] **Step 5: context-datasource.xml 작성**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:context="http://www.springframework.org/schema/context"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
                           http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context.xsd">

    <!-- globals.properties 값 안의 ${FASHION_*}는 환경변수/시스템 프로퍼티로 다시 치환된다 -->
    <context:property-placeholder location="classpath:/egovProps/globals.properties" />

    <bean id="dataSource" class="org.apache.commons.dbcp2.BasicDataSource" destroy-method="close">
        <property name="driverClassName" value="org.postgresql.Driver" />
        <property name="url" value="${Globals.Fashion.Postgre.Url}" />
        <property name="username" value="${Globals.Fashion.Postgre.UserName}" />
        <property name="password" value="${Globals.Fashion.Postgre.Password}" />
        <property name="maxTotal" value="5" />
    </bean>

    <bean id="sqlSessionFactory" class="org.mybatis.spring.SqlSessionFactoryBean">
        <property name="dataSource" ref="dataSource" />
        <property name="configLocation" value="classpath:/sqlmap/mybatis-config.xml" />
        <property name="mapperLocations" value="classpath:/sqlmap/mappers/**/*.xml" />
    </bean>
    <bean id="sqlSession" class="org.mybatis.spring.SqlSessionTemplate" destroy-method="clearCache">
        <constructor-arg name="sqlSessionFactory" ref="sqlSessionFactory" />
    </bean>

    <bean id="transactionManager" class="org.springframework.jdbc.datasource.DataSourceTransactionManager">
        <property name="dataSource" ref="dataSource" />
    </bean>

    <bean id="passwordEncoder" class="org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder" />
</beans>
```

- [ ] **Step 6: globals.properties 작성** — 비밀값은 기본값 없이 둔다(미설정 시 기동 실패가 정상).

```properties
# 값은 환경변수로 치환된다. ${NAME:default} 형식은 미설정 시 default 사용.
Globals.Fashion.Postgre.Url=${FASHION_DB_URL:jdbc:postgresql://localhost:5432/fashion}
Globals.Fashion.Postgre.UserName=${FASHION_DB_USER:postgres}
Globals.Fashion.Postgre.Password=${FASHION_DB_PASSWORD}

Globals.Fashion.Supabase.Url=${FASHION_SUPABASE_URL}
Globals.Fashion.Supabase.ServiceKey=${FASHION_SUPABASE_SERVICE_KEY}
Globals.Fashion.Supabase.Bucket=${FASHION_SUPABASE_BUCKET:photos}
```

- [ ] **Step 7: mybatis-config.xml 작성**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE configuration PUBLIC "-//mybatis.org//DTD Config 3.0//EN" "http://mybatis.org/dtd/mybatis-3-config.dtd">
<configuration>
    <settings>
        <setting name="jdbcTypeForNull" value="NULL" />
        <setting name="logImpl" value="LOG4J2" />
        <setting name="callSettersOnNulls" value="true" />
    </settings>
</configuration>
```

- [ ] **Step 8: log4j2.xml 작성**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="WARN">
    <Appenders>
        <Console name="console" target="SYSTEM_OUT">
            <PatternLayout pattern="%d{HH:mm:ss.SSS} %-5level %logger{36} - %msg%n" />
        </Console>
    </Appenders>
    <Loggers>
        <!-- 매퍼 namespace가 com.fashion.* 이므로 이 설정으로 SQL이 찍힌다 -->
        <Logger name="com.fashion" level="DEBUG" additivity="false">
            <AppenderRef ref="console" />
        </Logger>
        <Root level="INFO">
            <AppenderRef ref="console" />
        </Root>
    </Loggers>
</Configuration>
```

- [ ] **Step 9: 컴파일 확인**

Run: `mvn -q compile`
Expected: 종료 코드 0. (아직 Java 소스가 없어도 의존성 해석이 성공해야 한다. egovframe 저장소에서 다운로드가 일어난다.)

- [ ] **Step 10: 커밋**

```bash
git add pom.xml .gitignore src/main/webapp/WEB-INF src/main/resources
git commit -m "chore: maven project skeleton with spring mvc xml config"
```

---

### Task 2: DB 스키마와 로컬 DB

**Files:**
- Create: `src/main/resources/schema.sql`

**Interfaces:**
- Produces: 테이블 `users(id, login_id, password_hash, role)`, `category(id, slug, name, sort_order)`, `photo(id, owner_id, title, memo, image_url, created_at)`, `photo_category(photo_id, category_id)`, `product_link(id, photo_id, item_label, url, title, sort_order)`.

- [ ] **Step 1: schema.sql 작성**

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

- [ ] **Step 2: 로컬 DB 생성** (사용자가 실행. postgres 비밀번호는 설치 시 정한 값)

```bash
psql -U postgres -c "CREATE DATABASE fashion ENCODING 'UTF8';"
```

- [ ] **Step 3: 스키마 적용**

```bash
psql -U postgres -d fashion -f src/main/resources/schema.sql
```
Expected: `CREATE TABLE` ×5, `INSERT 0 6`, `CREATE INDEX` ×2

- [ ] **Step 4: 확인**

```bash
psql -U postgres -d fashion -c "SELECT slug, name FROM category ORDER BY sort_order;"
```
Expected: spring/봄 … shoes/신발 6행

- [ ] **Step 5: 커밋**

```bash
git add src/main/resources/schema.sql
git commit -m "chore: postgres schema"
```

---

### Task 3: Tiles 레이아웃, 공통 JSP/CSS/JS, 빈 홈 화면

**Files:**
- Create: `src/main/webapp/common/taglib.jsp`
- Create: `src/main/webapp/WEB-INF/tiles/tiles-layout.xml`
- Create: `src/main/webapp/WEB-INF/layout/fashionLayout.jsp`
- Create: `src/main/webapp/WEB-INF/jsp/error.jsp`
- Create: `src/main/webapp/WEB-INF/jsp/fashion/photo/list.jsp` (빈 목록 버전)
- Create: `src/main/webapp/resources/css/reset.css` (참고 프로젝트 복사), `fashion.css`
- Create: `src/main/webapp/resources/fonts/Pretendard-{Regular,Medium,SemiBold,Bold}.woff2` (참고 프로젝트 복사)
- Create: `src/main/webapp/resources/js/common/common.js`
- Create: `src/main/java/com/fashion/cmmn/dao/CmmnDAO.java`
- Create: `src/main/resources/sqlmap/mappers/fashion/cmmn/cmmn.xml`
- Create: `src/main/java/com/fashion/cmmn/controller/GlobalModelAdvice.java`
- Create: `src/main/java/com/fashion/photo/controller/PhotoController.java` (홈만)

**Interfaces:**
- Produces: 모델 속성 `categories`(`List<Map>`: id, slug, name), JS 전역 `contextPath`, `App.get/post/upload/isEmpty/formToObject/error`. `CmmnDAO.selectCategoryList()`. 뷰 이름 `/fashion/photo/list`.

- [ ] **Step 1: taglib.jsp**

```jsp
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
```

- [ ] **Step 2: tiles-layout.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE tiles-definitions PUBLIC "-//Apache Software Foundation//DTD Tiles Configuration 3.0//EN"
        "http://tiles.apache.org/dtds/tiles-config_3_0.dtd">
<tiles-definitions>
    <definition name="fashionLayout" template="/WEB-INF/layout/fashionLayout.jsp">
        <put-attribute name="body" value="" />
    </definition>
    <definition name="/fashion/*/*" extends="fashionLayout">
        <put-attribute name="body" value="/WEB-INF/jsp/fashion/{1}/{2}.jsp" />
    </definition>
</tiles-definitions>
```

- [ ] **Step 3: 폰트·reset.css 복사**

```bash
mkdir -p src/main/webapp/resources/fonts src/main/webapp/resources/css
for w in Regular Medium SemiBold Bold; do cp "/c/dev/workspace/design/src/main/webapp/resources/fonts/woff2/Pretendard-$w.woff2" src/main/webapp/resources/fonts/; done
cp /c/dev/workspace/design/src/main/webapp/resources/css/reset.css src/main/webapp/resources/css/reset.css
```

- [ ] **Step 4: fashion.css** — 레이아웃 골격만. 3단계에서 그리드·상세 스타일을 추가한다.

```css
@font-face { font-family: 'Pretendard'; font-weight: 400; src: url('../fonts/Pretendard-Regular.woff2') format('woff2'); font-display: swap; }
@font-face { font-family: 'Pretendard'; font-weight: 500; src: url('../fonts/Pretendard-Medium.woff2') format('woff2'); font-display: swap; }
@font-face { font-family: 'Pretendard'; font-weight: 600; src: url('../fonts/Pretendard-SemiBold.woff2') format('woff2'); font-display: swap; }
@font-face { font-family: 'Pretendard'; font-weight: 700; src: url('../fonts/Pretendard-Bold.woff2') format('woff2'); font-display: swap; }

:root {
  --bg: #fafafa;
  --fg: #111;
  --muted: #777;
  --line: #e5e5e5;
  --accent: #111;
  --max: 1200px;
}
html { font-family: 'Pretendard', system-ui, sans-serif; color: var(--fg); background: var(--bg); }
a { color: inherit; text-decoration: none; }

.site-header { border-bottom: 1px solid var(--line); background: #fff; }
.site-header .inner { max-width: var(--max); margin: 0 auto; padding: 16px 20px; display: flex; align-items: center; gap: 24px; flex-wrap: wrap; }
.site-title { font-weight: 700; font-size: 20px; letter-spacing: -0.02em; }
.site-nav { display: flex; gap: 4px; flex-wrap: wrap; }
.site-nav a { padding: 6px 12px; border-radius: 999px; font-weight: 500; color: var(--muted); }
.site-nav a:hover, .site-nav a.active { background: var(--fg); color: #fff; }
.site-account { margin-left: auto; display: flex; gap: 12px; font-size: 14px; color: var(--muted); }

.site-main { max-width: var(--max); margin: 0 auto; padding: 32px 20px; }
.page-title { font-size: 22px; font-weight: 600; margin-bottom: 20px; }
.empty { color: var(--muted); padding: 60px 0; text-align: center; }
```

- [ ] **Step 5: common.js** — 참고 프로젝트 `App.ajax` 패턴을 jQuery 없이 옮기고 `App.upload`를 추가.

```js
// 전역 네임스페이스. 화면별 js는 App.xxx = (function(){ ... return {init:...}; })(); 로 등록한다.
var App = window.App || {};

App.error = function (title, message) {
    window.alert(title + '\n' + message);
};

App.sessionExpired = function () {
    App.error('로그인이 필요합니다.', '다시 로그인해주세요.');
    window.location.href = contextPath + '/login';
    return new Promise(function () {});   // 체인 중단
};

App._handle = function (fetchPromise, opts) {
    return fetchPromise
        .then(function (res) {
            if (!res.ok) throw new Error('HTTP ' + res.status);
            return res.json();
        })
        .then(function (data) {
            if (data && data.sessionExpired) return App.sessionExpired();
            return data;
        })
        .catch(function (err) {
            if (typeof opts.onError === 'function') opts.onError(err);
            else App.error('오류', '요청 처리 중 문제가 발생했습니다.');
            throw err;
        });
};

// App.ajax(url, data, opts): JSON 요청/응답. X-Requested-With 헤더는 인터셉터의 Ajax 판별 + CSRF 방어에 쓰인다.
App.ajax = function (url, data, opts) {
    opts = opts || {};
    var method = (opts.method || 'POST').toUpperCase();
    var fetchOpt = {
        method: method,
        headers: {
            'Content-Type': 'application/json; charset=UTF-8',
            'X-Requested-With': 'XMLHttpRequest'
        },
        credentials: 'same-origin'
    };
    if (method === 'GET') {
        if (data) url += (url.indexOf('?') === -1 ? '?' : '&') + new URLSearchParams(data).toString();
    } else {
        fetchOpt.body = JSON.stringify(data || {});
    }
    return App._handle(fetch(url, fetchOpt), opts);
};
App.get = function (url, data, opts) { return App.ajax(url, data, Object.assign({}, opts, { method: 'GET' })); };
App.post = function (url, data, opts) { return App.ajax(url, data, Object.assign({}, opts, { method: 'POST' })); };

// App.upload(url, formData): multipart 전송. Content-Type은 브라우저가 boundary와 함께 붙이므로 지정하지 않는다.
App.upload = function (url, formData, opts) {
    opts = opts || {};
    return App._handle(fetch(url, {
        method: 'POST',
        headers: { 'X-Requested-With': 'XMLHttpRequest' },
        credentials: 'same-origin',
        body: formData
    }), opts);
};

App.isEmpty = function (v) {
    if (v === null || v === undefined) return true;
    if (typeof v === 'string' || Array.isArray(v)) return v.length === 0;
    if (typeof v === 'object') return Object.keys(v).length === 0;
    return false;
};

// App.formToObject(form): <form> -> {name: value}. 같은 name이 여러 개면 배열.
App.formToObject = function (form) {
    var obj = {};
    new FormData(form).forEach(function (value, key) {
        if (obj[key] === undefined) obj[key] = value;
        else if (Array.isArray(obj[key])) obj[key].push(value);
        else obj[key] = [obj[key], value];
    });
    return obj;
};
```

- [ ] **Step 6: fashionLayout.jsp**

```jsp
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles"%>
<%@ include file="/common/taglib.jsp"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Fashion Archive</title>
    <link rel="stylesheet" href="<c:url value='/resources/css/reset.css'/>">
    <link rel="stylesheet" href="<c:url value='/resources/css/fashion.css'/>">
    <script>var contextPath = '${pageContext.request.contextPath}';</script>
    <script src="<c:url value='/resources/js/common/common.js'/>"></script>
</head>
<body>
<header class="site-header">
    <div class="inner">
        <a class="site-title" href="<c:url value='/'/>">Fashion Archive</a>
        <nav class="site-nav">
            <c:forEach var="cat" items="${categories}">
                <a href="<c:url value='/c/${cat.slug}'/>" class="${cat.slug eq currentSlug ? 'active' : ''}"><c:out value="${cat.name}"/></a>
            </c:forEach>
        </nav>
        <div class="site-account">
            <c:choose>
                <c:when test="${not empty sessionScope.loginId}">
                    <a href="<c:url value='/admin/photos/new'/>">사진 등록</a>
                    <a href="<c:url value='/logout'/>">로그아웃</a>
                </c:when>
                <c:otherwise>
                    <a href="<c:url value='/login'/>">로그인</a>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</header>
<main class="site-main">
    <tiles:insertAttribute name="body" />
</main>
</body>
</html>
```

- [ ] **Step 7: list.jsp (빈 목록 버전)** — 3단계 Task 10에서 그리드로 교체한다.

```jsp
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/common/taglib.jsp"%>
<h1 class="page-title"><c:out value="${pageTitle}"/></h1>
<p class="empty">아직 등록된 사진이 없습니다.</p>
```

- [ ] **Step 8: error.jsp**

```jsp
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true"%>
<%@ include file="/common/taglib.jsp"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>오류 - Fashion Archive</title>
    <link rel="stylesheet" href="<c:url value='/resources/css/reset.css'/>">
    <link rel="stylesheet" href="<c:url value='/resources/css/fashion.css'/>">
</head>
<body>
<main class="site-main">
    <h1 class="page-title">
        <c:choose>
            <c:when test="${pageContext.errorData.statusCode == 404}">페이지를 찾을 수 없습니다.</c:when>
            <c:otherwise>문제가 발생했습니다.</c:otherwise>
        </c:choose>
    </h1>
    <p><a href="<c:url value='/'/>">홈으로</a></p>
</main>
</body>
</html>
```

- [ ] **Step 9: cmmn.xml (카테고리 조회만. 사용자 SQL은 Task 6에서 추가)**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.fashion.cmmn.dao.CmmnDAO">

    <select id="selectCategoryList" resultType="java.util.HashMap">
        SELECT id, slug, name
          FROM category
         ORDER BY sort_order
    </select>

</mapper>
```

- [ ] **Step 10: CmmnDAO.java**

```java
package com.fashion.cmmn.dao;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Repository
public class CmmnDAO {

    private static final String NS = "com.fashion.cmmn.dao.CmmnDAO.";

    @Resource(name = "sqlSession")
    private SqlSessionTemplate sqlSession;

    public List<Map<String, Object>> selectCategoryList() {
        return sqlSession.selectList(NS + "selectCategoryList");
    }
}
```

- [ ] **Step 11: GlobalModelAdvice.java** — 화면 컨트롤러에만 적용해 API 호출마다 카테고리를 조회하지 않게 한다.

```java
package com.fashion.cmmn.controller;

import com.fashion.cmmn.dao.CmmnDAO;
import com.fashion.photo.controller.PhotoController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;
import java.util.Map;

@ControllerAdvice(assignableTypes = PhotoController.class)
public class GlobalModelAdvice {

    @Autowired
    private CmmnDAO cmmnDAO;

    @ModelAttribute("categories")
    public List<Map<String, Object>> categories() {
        return cmmnDAO.selectCategoryList();
    }
}
```

- [ ] **Step 12: PhotoController.java (홈만)**

```java
package com.fashion.photo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PhotoController {

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("pageTitle", "최근 사진");
        return "/fashion/photo/list";
    }
}
```

- [ ] **Step 13: `dispatcher-servlet.xml`의 `<mvc:interceptors>` 블록을 주석 처리** (LoginInterceptor는 Task 5에서 생성)

- [ ] **Step 14: 패키징 확인**

Run: `mvn -q package -DskipTests`
Expected: `target/fashion-archive.war` 생성, 종료 코드 0

- [ ] **Step 15: IntelliJ Tomcat 실행 설정** (아래 "IntelliJ 설정 안내" 절 참고) 후 기동

브라우저 `http://localhost:8080/` 접속.
Expected: 헤더에 "Fashion Archive" + 봄/여름/가을/겨울/악세사리/신발 메뉴 + "로그인" 링크, 본문 "최근 사진 / 아직 등록된 사진이 없습니다." 콘솔 로그에 `selectCategoryList` SQL이 찍힌다.

| 결과 | 의미 |
|---|---|
| 정상 화면 | 1단계 완료 |
| `Could not resolve placeholder 'FASHION_DB_PASSWORD'` | 실행 설정의 환경변수 누락 |
| `Connection refused` / `password authentication failed` | 로컬 Postgres 미기동 또는 비밀번호 불일치 |
| 404 | Deployment 탭의 Application context가 `/`가 아님 |
| 메뉴가 비어 있음 | schema.sql의 category INSERT 미실행 |

- [ ] **Step 16: 커밋**

```bash
git add -A
git commit -m "feat: tiles layout, common assets, empty home page"
```

---

## 2단계. 공통 유틸·인터셉터·로그인

### Task 4: Response, Constants, NotFoundException

**Files:**
- Create: `src/main/java/com/fashion/cmmn/util/Response.java`
- Create: `src/main/java/com/fashion/cmmn/util/Constants.java`
- Create: `src/main/java/com/fashion/cmmn/util/NotFoundException.java`

**Interfaces:**
- Produces: `Response.of(String code)`, `Response.of(String code, Object data)`, `Response.of(String code, String message, Object data)`; getters `getCode/getMessage/getData`. `Constants.SUCCESS/LOGIN_FAIL/FAIL/SESSION_LOGIN_ID/SESSION_USER_ID`. `NotFoundException`(→ HTTP 404).

- [ ] **Step 1: Response.java**

```java
package com.fashion.cmmn.util;

// Ajax 공통 응답 포맷. code: Constants.SUCCESS/FAIL 등, message: 사용자에게 보여줄 문구, data: 결과
public class Response {

    private final String code;
    private final String message;
    private final Object data;

    private Response(String code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static Response of(String code) {
        return new Response(code, null, null);
    }

    public static Response of(String code, Object data) {
        return new Response(code, null, data);
    }

    public static Response of(String code, String message, Object data) {
        return new Response(code, message, data);
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public Object getData() {
        return data;
    }
}
```

- [ ] **Step 2: Constants.java**

```java
package com.fashion.cmmn.util;

public final class Constants {

    public static final String SUCCESS = "00";
    public static final String LOGIN_FAIL = "01";
    public static final String FAIL = "99";

    public static final String SESSION_LOGIN_ID = "loginId";
    public static final String SESSION_USER_ID = "userId";

    private Constants() {
    }
}
```

- [ ] **Step 3: NotFoundException.java** — 화면 컨트롤러에서 던지면 Spring이 404로 응답하고 `web.xml`의 error.jsp가 뜬다.

```java
package com.fashion.cmmn.util;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
```

- [ ] **Step 4: 컴파일 후 커밋**

Run: `mvn -q compile`
```bash
git add src/main/java/com/fashion/cmmn/util
git commit -m "feat: common response, constants, not-found exception"
```

---

### Task 5: Validation 유틸 (TDD)

**Files:**
- Create: `src/test/java/com/fashion/cmmn/util/ValidationTest.java`
- Create: `src/main/java/com/fashion/cmmn/util/Validation.java`

**Interfaces:**
- Produces: `Validation.requireText(Object value, String field, int max) → String`(trim), `Validation.optionalText(Object value, String field, int max) → String|null`, `Validation.requireHttpUrl(Object value) → String`, `Validation.imageExtension(byte[] head) → "jpg"|"png"|"webp"`. 실패 시 모두 `IllegalArgumentException(사용자용 메시지)`.

- [ ] **Step 1: 실패하는 테스트 작성**

```java
package com.fashion.cmmn.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidationTest {

    @Test
    void requireText_trimsAndReturns() {
        assertEquals("아우터", Validation.requireText("  아우터 ", "아이템", 30));
    }

    @Test
    void requireText_rejectsEmptyAndTooLong() {
        assertThrows(IllegalArgumentException.class, () -> Validation.requireText("   ", "제목", 100));
        assertThrows(IllegalArgumentException.class, () -> Validation.requireText(null, "제목", 100));
        assertThrows(IllegalArgumentException.class, () -> Validation.requireText("a".repeat(101), "제목", 100));
    }

    @Test
    void optionalText_emptyBecomesNull() {
        assertNull(Validation.optionalText("  ", "메모", 10));
        assertEquals("메모", Validation.optionalText(" 메모 ", "메모", 10));
    }

    @Test
    void requireHttpUrl_acceptsHttpAndHttps() {
        assertEquals("https://musinsa.com/app/goods/1", Validation.requireHttpUrl(" https://musinsa.com/app/goods/1 "));
        assertEquals("http://example.com", Validation.requireHttpUrl("http://example.com"));
    }

    @Test
    void requireHttpUrl_rejectsOtherSchemesAndGarbage() {
        assertThrows(IllegalArgumentException.class, () -> Validation.requireHttpUrl("javascript:alert(1)"));
        assertThrows(IllegalArgumentException.class, () -> Validation.requireHttpUrl("ftp://example.com/a"));
        assertThrows(IllegalArgumentException.class, () -> Validation.requireHttpUrl("musinsa.com"));
        assertThrows(IllegalArgumentException.class, () -> Validation.requireHttpUrl("http://"));
        assertThrows(IllegalArgumentException.class, () -> Validation.requireHttpUrl(""));
    }

    @Test
    void imageExtension_detectsByMagicBytes() {
        assertEquals("jpg", Validation.imageExtension(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0, 0, 0}));
        assertEquals("png", Validation.imageExtension(new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0}));
        assertEquals("webp", Validation.imageExtension(new byte[]{'R', 'I', 'F', 'F', 1, 2, 3, 4, 'W', 'E', 'B', 'P'}));
    }

    @Test
    void imageExtension_rejectsOthers() {
        assertThrows(IllegalArgumentException.class, () -> Validation.imageExtension(new byte[]{'G', 'I', 'F', '8', '9', 'a', 0, 0, 0, 0, 0, 0}));
        assertThrows(IllegalArgumentException.class, () -> Validation.imageExtension(new byte[]{1, 2}));
        assertThrows(IllegalArgumentException.class, () -> Validation.imageExtension(null));
    }
}
```

- [ ] **Step 2: 실패 확인**

Run: `mvn -q test -Dtest=ValidationTest`
Expected: 컴파일 오류 (`Validation` 없음)

- [ ] **Step 3: Validation.java 작성**

```java
package com.fashion.cmmn.util;

import java.net.URI;
import java.net.URISyntaxException;

public final class Validation {

    private Validation() {
    }

    public static String requireText(Object value, String field, int max) {
        String s = value == null ? "" : value.toString().trim();
        if (s.isEmpty()) {
            throw new IllegalArgumentException(field + "을(를) 입력해주세요.");
        }
        if (s.length() > max) {
            throw new IllegalArgumentException(field + "은(는) " + max + "자 이하로 입력해주세요.");
        }
        return s;
    }

    public static String optionalText(Object value, String field, int max) {
        String s = value == null ? "" : value.toString().trim();
        if (s.isEmpty()) {
            return null;
        }
        return requireText(s, field, max);
    }

    public static String requireHttpUrl(Object value) {
        String s = requireText(value, "URL", 2000);
        try {
            URI uri = new URI(s);
            String scheme = uri.getScheme();
            boolean http = "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
            if (!http || uri.getHost() == null || uri.getHost().isEmpty()) {
                throw new IllegalArgumentException("http:// 또는 https://로 시작하는 URL을 입력해주세요.");
            }
            return s;
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("올바른 URL이 아닙니다.");
        }
    }

    // 확장자가 아니라 파일 앞 12바이트로 형식을 판별한다
    public static String imageExtension(byte[] head) {
        if (head != null && head.length >= 12) {
            if ((head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF) {
                return "jpg";
            }
            if ((head[0] & 0xFF) == 0x89 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G') {
                return "png";
            }
            if (head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
                    && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P') {
                return "webp";
            }
        }
        throw new IllegalArgumentException("jpg, png, webp 이미지만 등록할 수 있습니다.");
    }
}
```

- [ ] **Step 4: 통과 확인**

Run: `mvn -q test -Dtest=ValidationTest`
Expected: `Tests run: 7, Failures: 0`

- [ ] **Step 5: 커밋**

```bash
git add src/main/java/com/fashion/cmmn/util/Validation.java src/test/java/com/fashion/cmmn/util/ValidationTest.java
git commit -m "feat: input validation helpers with tests"
```

---

### Task 6: LoginInterceptor, SameSite 쿠키

**Files:**
- Create: `src/main/java/com/fashion/cmmn/interceptor/LoginInterceptor.java`
- Create: `src/main/webapp/META-INF/context.xml`
- Modify: `src/main/webapp/WEB-INF/config/dispatcher-servlet.xml` (`<mvc:interceptors>` 주석 해제)

**Interfaces:**
- Consumes: `Constants.SESSION_LOGIN_ID`
- Produces: 비로그인 Ajax → `{"sessionExpired":true}`, 비로그인 화면 → `/login` 리다이렉트, `/api/**`에 `X-Requested-With` 없음 → 403.

- [ ] **Step 1: LoginInterceptor.java**

```java
package com.fashion.cmmn.interceptor;

import com.fashion.cmmn.util.Constants;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

// /admin/**, /api/admin/** 에 매핑. 로그인 여부와 Ajax 여부를 판별한다.
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        boolean ajax = "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
        boolean api = request.getRequestURI().startsWith(request.getContextPath() + "/api/");

        // 교차 출처 폼 전송은 이 헤더를 붙일 수 없다 → CSRF 방어
        if (api && !ajax) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }

        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(Constants.SESSION_LOGIN_ID) != null) {
            return true;
        }

        if (ajax) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"sessionExpired\":true}");
        } else {
            response.sendRedirect(request.getContextPath() + "/login");
        }
        return false;
    }
}
```

- [ ] **Step 2: META-INF/context.xml** — 세션 쿠키에 `SameSite=Lax`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Context>
    <CookieProcessor sameSiteCookies="lax" />
</Context>
```

- [ ] **Step 3: dispatcher-servlet.xml의 `<mvc:interceptors>` 주석 해제**

- [ ] **Step 4: 기동 확인**

Tomcat 재시작 후 `http://localhost:8080/admin/photos/new` 접속.
Expected: `/login`으로 리다이렉트되어 404 (로그인 화면은 Task 7에서 생성). 리다이렉트 자체가 일어나면 정상.

- [ ] **Step 5: 커밋**

```bash
git add -A
git commit -m "feat: login interceptor and samesite cookie"
```

---

### Task 7: 사용자 DAO/Service, 해시 생성 도구, 관리자 계정 INSERT

**Files:**
- Modify: `src/main/resources/sqlmap/mappers/fashion/cmmn/cmmn.xml`
- Modify: `src/main/java/com/fashion/cmmn/dao/CmmnDAO.java`
- Create: `src/main/java/com/fashion/cmmn/service/CmmnService.java`
- Create: `src/main/java/com/fashion/cmmn/service/impl/CmmnServiceImpl.java`
- Create: `src/test/java/com/fashion/PasswordHashTool.java`

**Interfaces:**
- Produces: `CmmnService.selectUserByLoginId(String loginId) → Map<String,Object>|null`(키 `id`, `login_id`, `password_hash`, `role`). `CmmnDAO.selectUserByLoginId(String)`. 판정(BCrypt 비교, 세션 저장)은 Task 8의 컨트롤러가 한다.

- [ ] **Step 1: cmmn.xml에 사용자 SQL 추가**

```xml
    <select id="selectUserByLoginId" parameterType="string" resultType="java.util.HashMap">
        SELECT id, login_id, password_hash, role
          FROM users
         WHERE login_id = #{loginId}
    </select>

```

- [ ] **Step 2: CmmnDAO에 메서드 추가**

```java
    public Map<String, Object> selectUserByLoginId(String loginId) {
        return sqlSession.selectOne(NS + "selectUserByLoginId", loginId);
    }

```

- [ ] **Step 3: CmmnService.java**

```java
package com.fashion.cmmn.service;

import java.util.Map;

public interface CmmnService {

    // 조회 결과 키는 컬럼명 그대로: id, login_id, password_hash, role. 없으면 null
    Map<String, Object> selectUserByLoginId(String loginId);
}
```

- [ ] **Step 4: CmmnServiceImpl.java** — 참고 프로젝트처럼 DAO 위임만 한다.

```java
package com.fashion.cmmn.service.impl;

import com.fashion.cmmn.dao.CmmnDAO;
import com.fashion.cmmn.service.CmmnService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service("cmmnService")
public class CmmnServiceImpl implements CmmnService {

    @Autowired
    private CmmnDAO cmmnDAO;

    @Override
    public Map<String, Object> selectUserByLoginId(String loginId) {
        return cmmnDAO.selectUserByLoginId(loginId);
    }
}
```

- [ ] **Step 5: PasswordHashTool.java** (test 소스. IntelliJ에서 우클릭 → Run)

```java
package com.fashion;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Scanner;

// 관리자 비밀번호의 BCrypt 해시를 만든다. 출력값을 환경변수 FASHION_ADMIN_PASSWORD_HASH에 넣는다.
public class PasswordHashTool {

    public static void main(String[] args) {
        System.out.print("비밀번호 입력: ");
        String raw = new Scanner(System.in).nextLine();
        System.out.println(new BCryptPasswordEncoder().encode(raw));
    }
}
```

- [ ] **Step 6: 관리자 계정 INSERT (psql 대화형)** — PowerShell `-c "..."` 안에서는 `$2a$10$...`의 `$`가 변수로 치환되어 해시가 잘리므로 반드시 psql 프롬프트에서 입력한다.

IntelliJ에서 `PasswordHashTool` 실행 → 비밀번호 입력 → `$2a$10$...` 복사. 이어서:

```bash
psql -U postgres -d fashion
```
`fashion=#` 프롬프트에서:
```sql
INSERT INTO users (login_id, password_hash) VALUES ('admin', '여기에_해시');
SELECT id, login_id, length(password_hash) FROM users;
```
Expected: 1행, `length` = 60. `\q`로 종료.

- [ ] **Step 7: 기동 확인**

Tomcat 재시작. 매퍼 XML 파싱 오류 없이 홈이 뜨면 정상.

- [ ] **Step 8: 커밋**

```bash
git add -A
git commit -m "feat: user lookup and password hash tool"
```

---

### Task 8: 로그인 화면과 API

**Files:**
- Create: `src/main/java/com/fashion/cmmn/controller/CmmnController.java`
- Create: `src/main/java/com/fashion/cmmn/controller/CmmnApiController.java`
- Create: `src/main/webapp/WEB-INF/jsp/login/loginMain.jsp`
- Create: `src/main/webapp/resources/js/app/login/login.js`
- Modify: `src/main/webapp/resources/css/fashion.css` (로그인 스타일 추가)

**Interfaces:**
- Consumes: `CmmnService.selectUserByLoginId`, 빈 `passwordEncoder`, `Response`, `Constants`, `Validation`
- Produces: `GET /login`, `GET /logout`, `POST /api/login` (JSON `{loginId, password}` → `{code:"00"}` 또는 `{code:"01", message}`). 세션 속성 `loginId`(String), `userId`(Long).

- [ ] **Step 1: CmmnController.java**

```java
package com.fashion.cmmn.controller;

import com.fashion.cmmn.util.Constants;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpSession;

@Controller
public class CmmnController {

    @GetMapping("/login")
    public String login(HttpSession session) {
        if (session.getAttribute(Constants.SESSION_LOGIN_ID) != null) {
            return "redirect:/";
        }
        return "login/loginMain";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}
```

- [ ] **Step 2: CmmnApiController.java**

```java
package com.fashion.cmmn.controller;

import com.fashion.cmmn.service.CmmnService;
import com.fashion.cmmn.util.Constants;
import com.fashion.cmmn.util.Response;
import com.fashion.cmmn.util.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.Map;

@Controller
public class CmmnApiController {

    private static final Logger logger = LoggerFactory.getLogger(CmmnApiController.class);

    @Resource(name = "cmmnService")
    private CmmnService cmmnService;

    @ResponseBody
    @PostMapping("/api/login")
    public Response login(@RequestBody Map<String, Object> param, HttpSession session) {
        try {
            String loginId = Validation.requireText(param.get("loginId"), "아이디", 50);
            String password = Validation.requireText(param.get("password"), "비밀번호", 200);

            Map<String, Object> user = cmmnService.login(loginId, password);
            if (user == null) {
                return Response.of(Constants.LOGIN_FAIL, "아이디 또는 비밀번호를 확인해주세요.", null);
            }
            session.setAttribute(Constants.SESSION_LOGIN_ID, user.get("login_id"));
            session.setAttribute(Constants.SESSION_USER_ID, ((Number) user.get("id")).longValue());
            return Response.of(Constants.SUCCESS);
        } catch (IllegalArgumentException e) {
            return Response.of(Constants.FAIL, e.getMessage(), null);
        } catch (Exception e) {
            logger.error("로그인 처리 중 오류", e);
            return Response.of(Constants.FAIL, "로그인 처리 중 오류가 발생했습니다.", null);
        }
    }
}
```

- [ ] **Step 3: loginMain.jsp** (Tiles 레이아웃 없음)

```jsp
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/common/taglib.jsp"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>로그인 - Fashion Archive</title>
    <link rel="stylesheet" href="<c:url value='/resources/css/reset.css'/>">
    <link rel="stylesheet" href="<c:url value='/resources/css/fashion.css'/>">
    <script>var contextPath = '${pageContext.request.contextPath}';</script>
    <script src="<c:url value='/resources/js/common/common.js'/>"></script>
    <script defer src="<c:url value='/resources/js/app/login/login.js'/>"></script>
</head>
<body class="login-page">
<main class="login-box">
    <h1 class="site-title">Fashion Archive</h1>
    <form id="loginForm" autocomplete="off">
        <label>
            <span>아이디</span>
            <input type="text" name="loginId" required autofocus>
        </label>
        <label>
            <span>비밀번호</span>
            <input type="password" name="password" required>
        </label>
        <button type="submit" class="btn btn-primary">로그인</button>
    </form>
    <p><a href="<c:url value='/'/>">홈으로</a></p>
</main>
</body>
</html>
```

- [ ] **Step 4: login.js**

```js
App.login = (function () {
    var form = document.getElementById('loginForm'),
        submitting = false;

    function init() {
        form.addEventListener('submit', function (e) {
            e.preventDefault();
            submit();
        });
    }

    function submit() {
        if (submitting) return;
        var param = App.formToObject(form);
        if (App.isEmpty(param.loginId) || App.isEmpty(param.password)) {
            App.error('알림', '아이디와 비밀번호를 입력해주세요.');
            return;
        }
        submitting = true;
        App.post(contextPath + '/api/login', param)
            .then(function (res) {
                if (res.code === '00') {
                    location.href = contextPath + '/';
                } else {
                    App.error('로그인 실패', res.message);
                }
            })
            .catch(function () {})
            .then(function () { submitting = false; });
    }

    return { init: init };
}());

document.addEventListener('DOMContentLoaded', App.login.init);
```

- [ ] **Step 5: fashion.css에 추가**

```css
.btn { display: inline-block; padding: 10px 18px; border: 1px solid var(--fg); border-radius: 8px; background: #fff; font: inherit; cursor: pointer; }
.btn-primary { background: var(--fg); color: #fff; }
.btn-danger { border-color: #c33; color: #c33; }
.btn:disabled { opacity: .5; cursor: default; }

.login-page { display: flex; align-items: center; justify-content: center; min-height: 100vh; }
.login-box { width: 320px; padding: 32px; background: #fff; border: 1px solid var(--line); border-radius: 12px; text-align: center; }
.login-box form { display: flex; flex-direction: column; gap: 12px; margin: 24px 0 16px; }
.login-box label { display: flex; flex-direction: column; gap: 4px; text-align: left; font-size: 13px; color: var(--muted); }
.login-box input { padding: 10px; border: 1px solid var(--line); border-radius: 8px; font: inherit; }
```

- [ ] **Step 6: 동작 확인**

Tomcat 재시작 → `http://localhost:8080/login`.

| 시도 | Expected |
|---|---|
| 틀린 비밀번호 | alert "로그인 실패 / 아이디 또는 비밀번호를 확인해주세요." |
| 맞는 비밀번호 | 홈으로 이동, 헤더에 "사진 등록 / 로그아웃" |
| `/admin/photos/new` 접속 (로그인 상태) | 404 (화면은 4단계). 리다이렉트되지 않으면 정상 |
| 로그아웃 클릭 | 홈, 헤더에 "로그인" |
| 터미널: `curl -i -X POST http://localhost:8080/api/admin/photos/1/delete` | `403` (헤더 없는 API 요청 차단) |

- [ ] **Step 7: 커밋**

```bash
git add -A
git commit -m "feat: session login page and api"
```

---

## 3단계. 사진 목록·상세 조회 (읽기 전용)

### Task 9: PhotoDAO / LinkDAO / 매퍼 XML

**Files:**
- Create: `src/main/resources/sqlmap/mappers/fashion/photo/photo.xml`
- Create: `src/main/resources/sqlmap/mappers/fashion/photo/link.xml`
- Create: `src/main/java/com/fashion/photo/dao/PhotoDAO.java`
- Create: `src/main/java/com/fashion/photo/dao/LinkDAO.java`

**Interfaces:**
- Produces:
  - `PhotoDAO.selectPhotoList(String slug|null) → List<Map>`(id, title, image_url, created_at)
  - `PhotoDAO.selectPhoto(long id) → Map|null`(id, owner_id, title, memo, image_url, created_at)
  - `PhotoDAO.selectPhotoCategoryIds(long photoId) → List<Integer>`
  - `PhotoDAO.selectPhotoCategoryNames(long photoId) → List<String>`
  - `PhotoDAO.insertPhoto(Map{ownerId,title,memo,imageUrl})` — 실행 후 `param.get("id")`에 생성 키
  - `PhotoDAO.updatePhoto(Map{id,title,memo})`, `deletePhoto(long id) → int`
  - `PhotoDAO.deletePhotoCategories(long photoId)`, `insertPhotoCategories(long photoId, List<Integer> categoryIds)`
  - `LinkDAO.selectLinkList(long photoId) → List<Map>`(id, photo_id, item_label, url, title, sort_order)
  - `LinkDAO.selectLink(long id) → Map|null`, `insertLink(Map{photoId,itemLabel,url,title})`, `updateLink(Map{id,itemLabel,url,title}) → int`, `deleteLink(long id) → int`

- [ ] **Step 1: photo.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.fashion.photo.dao.PhotoDAO">

    <select id="selectPhotoList" parameterType="string" resultType="java.util.HashMap">
        SELECT p.id, p.title, p.image_url, p.created_at
          FROM photo p
        <if test="slug != null">
         WHERE EXISTS (
               SELECT 1
                 FROM photo_category pc
                 JOIN category c ON c.id = pc.category_id
                WHERE pc.photo_id = p.id
                  AND c.slug = #{slug})
        </if>
         ORDER BY p.created_at DESC, p.id DESC
    </select>

    <select id="selectPhoto" parameterType="long" resultType="java.util.HashMap">
        SELECT id, owner_id, title, memo, image_url, created_at
          FROM photo
         WHERE id = #{id}
    </select>

    <select id="selectPhotoCategoryIds" parameterType="long" resultType="int">
        SELECT category_id
          FROM photo_category
         WHERE photo_id = #{photoId}
    </select>

    <select id="selectPhotoCategoryNames" parameterType="long" resultType="string">
        SELECT c.name
          FROM photo_category pc
          JOIN category c ON c.id = pc.category_id
         WHERE pc.photo_id = #{photoId}
         ORDER BY c.sort_order
    </select>

    <insert id="insertPhoto" parameterType="java.util.HashMap" useGeneratedKeys="true" keyProperty="id" keyColumn="id">
        INSERT INTO photo (owner_id, title, memo, image_url)
        VALUES (#{ownerId}, #{title}, #{memo}, #{imageUrl})
    </insert>

    <update id="updatePhoto" parameterType="java.util.HashMap">
        UPDATE photo
           SET title = #{title},
               memo  = #{memo}
         WHERE id = #{id}
    </update>

    <delete id="deletePhoto" parameterType="long">
        DELETE FROM photo WHERE id = #{id}
    </delete>

    <delete id="deletePhotoCategories" parameterType="long">
        DELETE FROM photo_category WHERE photo_id = #{photoId}
    </delete>

    <insert id="insertPhotoCategories" parameterType="java.util.HashMap">
        INSERT INTO photo_category (photo_id, category_id)
        VALUES
        <foreach collection="categoryIds" item="cid" separator=",">
            (#{photoId}, #{cid})
        </foreach>
    </insert>

</mapper>
```

- [ ] **Step 2: link.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.fashion.photo.dao.LinkDAO">

    <select id="selectLinkList" parameterType="long" resultType="java.util.HashMap">
        SELECT id, photo_id, item_label, url, title, sort_order
          FROM product_link
         WHERE photo_id = #{photoId}
         ORDER BY sort_order, id
    </select>

    <select id="selectLink" parameterType="long" resultType="java.util.HashMap">
        SELECT id, photo_id, item_label, url, title, sort_order
          FROM product_link
         WHERE id = #{id}
    </select>

    <!-- 같은 사진 안에서 등록 순서를 유지하기 위해 sort_order = 현재 최대값 + 1 -->
    <insert id="insertLink" parameterType="java.util.HashMap">
        INSERT INTO product_link (photo_id, item_label, url, title, sort_order)
        SELECT #{photoId}, #{itemLabel}, #{url}, #{title}, COALESCE(MAX(sort_order), 0) + 1
          FROM product_link
         WHERE photo_id = #{photoId}
    </insert>

    <update id="updateLink" parameterType="java.util.HashMap">
        UPDATE product_link
           SET item_label = #{itemLabel},
               url        = #{url},
               title      = #{title}
         WHERE id = #{id}
    </update>

    <delete id="deleteLink" parameterType="long">
        DELETE FROM product_link WHERE id = #{id}
    </delete>

</mapper>
```

- [ ] **Step 3: PhotoDAO.java**

```java
package com.fashion.photo.dao;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class PhotoDAO {

    private static final String NS = "com.fashion.photo.dao.PhotoDAO.";

    @Resource(name = "sqlSession")
    private SqlSessionTemplate sqlSession;

    public List<Map<String, Object>> selectPhotoList(String slug) {
        Map<String, Object> param = new HashMap<>();
        param.put("slug", slug);
        return sqlSession.selectList(NS + "selectPhotoList", param);
    }

    public Map<String, Object> selectPhoto(long id) {
        return sqlSession.selectOne(NS + "selectPhoto", id);
    }

    public List<Integer> selectPhotoCategoryIds(long photoId) {
        return sqlSession.selectList(NS + "selectPhotoCategoryIds", photoId);
    }

    public List<String> selectPhotoCategoryNames(long photoId) {
        return sqlSession.selectList(NS + "selectPhotoCategoryNames", photoId);
    }

    public void insertPhoto(Map<String, Object> param) {
        sqlSession.insert(NS + "insertPhoto", param);
    }

    public int updatePhoto(Map<String, Object> param) {
        return sqlSession.update(NS + "updatePhoto", param);
    }

    public int deletePhoto(long id) {
        return sqlSession.delete(NS + "deletePhoto", id);
    }

    public void deletePhotoCategories(long photoId) {
        sqlSession.delete(NS + "deletePhotoCategories", photoId);
    }

    public void insertPhotoCategories(long photoId, List<Integer> categoryIds) {
        Map<String, Object> param = new HashMap<>();
        param.put("photoId", photoId);
        param.put("categoryIds", categoryIds);
        sqlSession.insert(NS + "insertPhotoCategories", param);
    }
}
```

- [ ] **Step 4: LinkDAO.java**

```java
package com.fashion.photo.dao;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Repository
public class LinkDAO {

    private static final String NS = "com.fashion.photo.dao.LinkDAO.";

    @Resource(name = "sqlSession")
    private SqlSessionTemplate sqlSession;

    public List<Map<String, Object>> selectLinkList(long photoId) {
        return sqlSession.selectList(NS + "selectLinkList", photoId);
    }

    public Map<String, Object> selectLink(long id) {
        return sqlSession.selectOne(NS + "selectLink", id);
    }

    public void insertLink(Map<String, Object> param) {
        sqlSession.insert(NS + "insertLink", param);
    }

    public int updateLink(Map<String, Object> param) {
        return sqlSession.update(NS + "updateLink", param);
    }

    public int deleteLink(long id) {
        return sqlSession.delete(NS + "deleteLink", id);
    }
}
```

- [ ] **Step 5: 매퍼 1회 확인** — 기동 시 매퍼 XML 파싱 오류가 없으면 성공. 추가로 psql로 더미 사진을 넣어 둔다 (3단계 화면 확인용).

```bash
psql -U postgres -d fashion -c "INSERT INTO photo (owner_id, title, memo, image_url) VALUES (1, '테스트 코디', '더미', 'https://picsum.photos/seed/1/600/800') RETURNING id;"
psql -U postgres -d fashion -c "INSERT INTO photo_category VALUES (1, 2), (1, 6);"
psql -U postgres -d fashion -c "INSERT INTO product_link (photo_id, item_label, url, title, sort_order) VALUES (1,'아우터','https://example.com/a','린넨 자켓 A',1),(1,'아우터','https://example.com/b','린넨 자켓 B',2),(1,'바지','https://example.com/c','와이드 팬츠',3);"
```

- [ ] **Step 6: 커밋**

```bash
git add -A
git commit -m "feat: photo and link dao with mybatis mappers"
```

---

### Task 10: PhotoService 조회 + 링크 그룹핑 (TDD)

**Files:**
- Create: `src/test/java/com/fashion/photo/service/PhotoServiceImplTest.java`
- Create: `src/main/java/com/fashion/photo/service/PhotoService.java`
- Create: `src/main/java/com/fashion/photo/service/impl/PhotoServiceImpl.java`

**Interfaces:**
- Consumes: `PhotoDAO`, `LinkDAO`, `CmmnDAO.selectCategoryList`
- Produces:
  - `PhotoService.selectPhotoList(String slug|null) → List<Map>`
  - `PhotoService.selectPhotoDetail(long id) → Map`(키 `photo`: Map, `categoryNames`: List<String>, `categoryIds`: List<Integer>, `linkGroups`: `Map<String, List<Map>>`). 없으면 `null`
  - `PhotoServiceImpl.groupLinks(List<Map>) → LinkedHashMap<String, List<Map>>` (static, item_label 등장 순서 유지)
  - `PhotoService.isValidSlug(String slug) → boolean`

- [ ] **Step 1: 실패하는 테스트**

```java
package com.fashion.photo.service;

import com.fashion.photo.service.impl.PhotoServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class PhotoServiceImplTest {

    private static Map<String, Object> link(long id, String label) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", id);
        m.put("item_label", label);
        m.put("url", "https://example.com/" + id);
        return m;
    }

    @Test
    void groupLinks_groupsByLabelKeepingFirstSeenOrder() {
        List<Map<String, Object>> links = Arrays.asList(
                link(1, "아우터"), link(2, "바지"), link(3, "아우터"), link(4, "신발"), link(5, "바지"));

        Map<String, List<Map<String, Object>>> groups = PhotoServiceImpl.groupLinks(links);

        assertEquals(Arrays.asList("아우터", "바지", "신발"), new ArrayList<>(groups.keySet()));
        assertEquals(Arrays.asList(1L, 3L), Arrays.asList(groups.get("아우터").get(0).get("id"), groups.get("아우터").get(1).get("id")));
        assertEquals(2, groups.get("바지").size());
        assertEquals(1, groups.get("신발").size());
    }

    @Test
    void groupLinks_emptyInputGivesEmptyMap() {
        assertTrue(PhotoServiceImpl.groupLinks(Collections.emptyList()).isEmpty());
    }
}
```

- [ ] **Step 2: 실패 확인**

Run: `mvn -q test -Dtest=PhotoServiceImplTest`
Expected: 컴파일 오류

- [ ] **Step 3: PhotoService.java** (3단계 범위. 4·5단계에서 메서드를 추가한다)

```java
package com.fashion.photo.service;

import java.util.List;
import java.util.Map;

public interface PhotoService {

    List<Map<String, Object>> selectPhotoList(String slug);

    // 키: photo, categoryNames, categoryIds, linkGroups. 없으면 null
    Map<String, Object> selectPhotoDetail(long id);

    boolean isValidSlug(String slug);
}
```

- [ ] **Step 4: PhotoServiceImpl.java**

```java
package com.fashion.photo.service.impl;

import com.fashion.cmmn.dao.CmmnDAO;
import com.fashion.photo.dao.LinkDAO;
import com.fashion.photo.dao.PhotoDAO;
import com.fashion.photo.service.PhotoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service("photoService")
public class PhotoServiceImpl implements PhotoService {

    @Autowired
    private PhotoDAO photoDAO;

    @Autowired
    private LinkDAO linkDAO;

    @Autowired
    private CmmnDAO cmmnDAO;

    @Override
    public List<Map<String, Object>> selectPhotoList(String slug) {
        return photoDAO.selectPhotoList(slug);
    }

    @Override
    public Map<String, Object> selectPhotoDetail(long id) {
        Map<String, Object> photo = photoDAO.selectPhoto(id);
        if (photo == null) {
            return null;
        }
        Map<String, Object> detail = new HashMap<>();
        detail.put("photo", photo);
        detail.put("categoryNames", photoDAO.selectPhotoCategoryNames(id));
        detail.put("categoryIds", photoDAO.selectPhotoCategoryIds(id));
        detail.put("linkGroups", groupLinks(linkDAO.selectLinkList(id)));
        return detail;
    }

    @Override
    public boolean isValidSlug(String slug) {
        for (Map<String, Object> c : cmmnDAO.selectCategoryList()) {
            if (slug.equals(c.get("slug"))) {
                return true;
            }
        }
        return false;
    }

    // item_label별로 묶는다. 라벨이 처음 등장한 순서(= 등록 순서)를 유지한다
    public static Map<String, List<Map<String, Object>>> groupLinks(List<Map<String, Object>> links) {
        Map<String, List<Map<String, Object>>> groups = new LinkedHashMap<>();
        for (Map<String, Object> link : links) {
            String label = String.valueOf(link.get("item_label"));
            groups.computeIfAbsent(label, k -> new ArrayList<>()).add(link);
        }
        return groups;
    }
}
```

- [ ] **Step 5: 통과 확인**

Run: `mvn -q test`
Expected: ValidationTest 7 + PhotoServiceImplTest 2 모두 통과

- [ ] **Step 6: 커밋**

```bash
git add -A
git commit -m "feat: photo read service with link grouping"
```

---

### Task 11: 목록·상세 화면

> **구조 변경 (사용자 규칙)**: 화면 컨트롤러(`PhotoController`)는 뷰 이름만 반환하고 경로 값(`currentSlug`, `photoId`)만 모델에 싣는다. 데이터·기능은 전부 `PhotoApiController`(JSON) + 화면 JS 렌더링 (참고 프로젝트의 `ManagementController` / `UserMgmtApiController` 조합). JSP·CSS·JS(`list.js`, `detail.js`)는 작성 완료.
>
> API 계약 — `POST /api/photos/list` `{slug}` → `data: [{id, title, image_url, created_at}]` / `POST /api/photos/detail` `{id}` → `data: {photo, categoryIds, linkGroups}` (없으면 `code 99`). `groupLinks`는 `PhotoApiController`의 static 메서드, 테스트는 `PhotoApiControllerTest`.
> 4·5단계 API(`/api/admin/**`)의 URL·JSON 모양은 아래 Task 14·16 표와 같고 `detail.js`/`photoForm.jsp`가 이미 그 계약으로 작성되어 있다. 조회 로직을 서비스에 두라는 아래 Task 10·11 본문은 이 규칙으로 대체한다.

**Files:**
- Modify: `src/main/java/com/fashion/photo/controller/PhotoController.java`
- Modify: `src/main/webapp/WEB-INF/jsp/fashion/photo/list.jsp`
- Create: `src/main/webapp/WEB-INF/jsp/fashion/photo/detail.jsp`
- Modify: `src/main/webapp/resources/css/fashion.css`

**Interfaces:**
- Consumes: `PhotoService`, `NotFoundException`
- Produces: `GET /`, `GET /c/{slug}`, `GET /photos/{id}`. 모델: `pageTitle`, `currentSlug`, `photos` / `photo`, `categoryNames`, `linkGroups`, `isAdmin`.

- [ ] **Step 1: PhotoController.java 전체 교체**

```java
package com.fashion.photo.controller;

import com.fashion.cmmn.util.Constants;
import com.fashion.cmmn.util.NotFoundException;
import com.fashion.photo.service.PhotoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.Map;

@Controller
public class PhotoController {

    @Resource(name = "photoService")
    private PhotoService photoService;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("pageTitle", "최근 사진");
        model.addAttribute("photos", photoService.selectPhotoList(null));
        return "/fashion/photo/list";
    }

    @GetMapping("/c/{slug}")
    public String category(@PathVariable String slug, Model model) {
        if (!photoService.isValidSlug(slug)) {
            throw new NotFoundException("category: " + slug);
        }
        model.addAttribute("currentSlug", slug);
        model.addAttribute("pageTitle", categoryName(model, slug));
        model.addAttribute("photos", photoService.selectPhotoList(slug));
        return "/fashion/photo/list";
    }

    @GetMapping("/photos/{id}")
    public String detail(@PathVariable long id, Model model, HttpSession session) {
        Map<String, Object> detail = photoService.selectPhotoDetail(id);
        if (detail == null) {
            throw new NotFoundException("photo: " + id);
        }
        model.addAllAttributes(detail);
        model.addAttribute("isAdmin", session.getAttribute(Constants.SESSION_LOGIN_ID) != null);
        return "/fashion/photo/detail";
    }

    @SuppressWarnings("unchecked")
    private static String categoryName(Model model, String slug) {
        for (Map<String, Object> c : (Iterable<Map<String, Object>>) model.getAttribute("categories")) {
            if (slug.equals(c.get("slug"))) {
                return (String) c.get("name");
            }
        }
        return slug;
    }
}
```

`categories`는 `GlobalModelAdvice`가 핸들러 실행 전에 모델에 넣어 두므로 컨트롤러 안에서 읽을 수 있다.

- [ ] **Step 2: list.jsp 전체 교체**

```jsp
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/common/taglib.jsp"%>
<h1 class="page-title"><c:out value="${pageTitle}"/></h1>
<c:choose>
    <c:when test="${empty photos}">
        <p class="empty">아직 등록된 사진이 없습니다.</p>
    </c:when>
    <c:otherwise>
        <ul class="photo-grid">
            <c:forEach var="p" items="${photos}">
                <li>
                    <a href="<c:url value='/photos/${p.id}'/>">
                        <img src="<c:out value='${p.image_url}'/>" alt="<c:out value='${p.title}'/>" loading="lazy">
                        <span class="photo-title"><c:out value="${p.title}"/></span>
                    </a>
                </li>
            </c:forEach>
        </ul>
    </c:otherwise>
</c:choose>
```

- [ ] **Step 3: detail.jsp** (공개 부분. 관리자 UI는 Task 14·16에서 추가)

```jsp
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/common/taglib.jsp"%>
<article class="photo-detail">
    <div class="photo-detail-image">
        <img src="<c:out value='${photo.image_url}'/>" alt="<c:out value='${photo.title}'/>">
    </div>
    <div class="photo-detail-body">
        <h1 class="page-title"><c:out value="${photo.title}"/></h1>
        <p class="photo-categories">
            <c:forEach var="name" items="${categoryNames}"><span class="chip"><c:out value="${name}"/></span></c:forEach>
        </p>
        <c:if test="${not empty photo.memo}">
            <p class="photo-memo"><c:out value="${photo.memo}"/></p>
        </c:if>

        <section class="link-groups">
            <c:if test="${empty linkGroups}">
                <p class="empty">등록된 제품 링크가 없습니다.</p>
            </c:if>
            <c:forEach var="group" items="${linkGroups}">
                <h2 class="link-label"><c:out value="${group.key}"/></h2>
                <ul class="link-list">
                    <c:forEach var="link" items="${group.value}">
                        <li>
                            <a href="<c:out value='${link.url}'/>" target="_blank" rel="noopener noreferrer">
                                <c:out value="${empty link.title ? link.url : link.title}"/>
                            </a>
                        </li>
                    </c:forEach>
                </ul>
            </c:forEach>
        </section>
    </div>
</article>
```

- [ ] **Step 4: fashion.css에 추가**

```css
.photo-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 20px; list-style: none; }
.photo-grid img { width: 100%; aspect-ratio: 3 / 4; object-fit: cover; border-radius: 10px; background: #eee; }
.photo-title { display: block; margin-top: 8px; font-weight: 500; }

.photo-detail { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); gap: 40px; }
.photo-detail-image img { width: 100%; border-radius: 12px; background: #eee; }
.photo-categories { display: flex; gap: 6px; margin-bottom: 16px; }
.chip { padding: 4px 10px; border: 1px solid var(--line); border-radius: 999px; font-size: 13px; color: var(--muted); }
.photo-memo { white-space: pre-wrap; color: #444; margin-bottom: 24px; line-height: 1.6; }
.link-label { font-size: 16px; font-weight: 600; margin: 20px 0 8px; }
.link-list { list-style: none; display: flex; flex-direction: column; gap: 6px; }
.link-list a { display: block; padding: 10px 14px; border: 1px solid var(--line); border-radius: 8px; background: #fff; word-break: break-all; }
.link-list a:hover { border-color: var(--fg); }
@media (max-width: 800px) { .photo-detail { grid-template-columns: 1fr; } }
```

- [ ] **Step 5: 화면 확인**

| URL | Expected |
|---|---|
| `/` | 더미 사진 1장 그리드 |
| `/c/summer` | 같은 사진 (카테고리 2=여름), 메뉴 "여름" 활성화 |
| `/c/spring` | "아직 등록된 사진이 없습니다." |
| `/c/nope` | 404 error.jsp |
| `/photos/1` | 이미지, 제목, 칩 "여름/신발", 아우터(2개)·바지(1개) 링크 그룹 |
| `/photos/999` | 404 |

- [ ] **Step 6: 커밋**

```bash
git add -A
git commit -m "feat: photo list and detail pages"
```

---

## 4단계. 사진 등록·수정·삭제 + Supabase Storage

### Task 12: Supabase 준비와 업로드 엔드포인트 확인

**Files:** 없음 (외부 설정)

- [ ] **Step 1: Supabase 프로젝트 생성** (사용자). Dashboard → Storage → New bucket: 이름 `photos`, **Public bucket** 켬.

- [ ] **Step 2: 값 확보** — Project Settings → API: `Project URL`(`https://xxxx.supabase.co`), `service_role` 키. 이 키는 서버에서만 쓰고 절대 커밋하지 않는다.

- [ ] **Step 3: REST 업로드 엔드포인트 확인** (Git Bash. 값은 셸 변수로만)

```bash
URL=https://xxxx.supabase.co; KEY='service_role_key'; curl -s -o /dev/null -w "%{http_code}\n" -X POST "$URL/storage/v1/object/photos/probe.png" -H "Authorization: Bearer $KEY" -H "apikey: $KEY" -H "Content-Type: image/png" --data-binary @src/main/webapp/resources/probe.png
```
(`probe.png`는 아무 png 파일.) Expected: `200`. 이어서 브라우저로 `$URL/storage/v1/object/public/photos/probe.png`가 열리면 공개 읽기 확인. 마지막으로 삭제 확인:

```bash
curl -s -o /dev/null -w "%{http_code}\n" -X DELETE "$URL/storage/v1/object/photos/probe.png" -H "Authorization: Bearer $KEY" -H "apikey: $KEY"
```
Expected: `200`

| 결과 | 의미 |
|---|---|
| 200 / 열림 / 200 | Task 13의 URL 규칙 확정 |
| 업로드 400 `Bucket not found` | 버킷 이름 불일치 |
| 업로드 403 | 키가 `anon`이거나 잘못됨 |
| 공개 URL 400/404 | 버킷이 Public이 아님 |

- [ ] **Step 4: Tomcat 실행 설정 환경변수 추가** — `FASHION_SUPABASE_URL`, `FASHION_SUPABASE_SERVICE_KEY`, `FASHION_SUPABASE_BUCKET=photos`

---

### Task 13: SupabaseStorage

**Files:**
- Create: `src/main/java/com/fashion/cmmn/storage/SupabaseStorage.java`

**Interfaces:**
- Consumes: 프로퍼티 `Globals.Fashion.Supabase.Url/ServiceKey/Bucket`
- Produces: `SupabaseStorage.upload(byte[] bytes, String ext) → String publicUrl` (실패 시 `IOException`), `SupabaseStorage.delete(String publicUrl)` (실패해도 예외 없이 로그만)

- [ ] **Step 1: SupabaseStorage.java**

```java
package com.fashion.cmmn.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

// Supabase Storage REST. 업로드: POST /storage/v1/object/{bucket}/{path}, 공개 URL: /storage/v1/object/public/{bucket}/{path}
@Component
public class SupabaseStorage {

    private static final Logger logger = LoggerFactory.getLogger(SupabaseStorage.class);
    private static final Map<String, String> CONTENT_TYPES = Map.of("jpg", "image/jpeg", "png", "image/png", "webp", "image/webp");

    @Value("${Globals.Fashion.Supabase.Url}")
    private String baseUrl;

    @Value("${Globals.Fashion.Supabase.ServiceKey}")
    private String serviceKey;

    @Value("${Globals.Fashion.Supabase.Bucket}")
    private String bucket;

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public String upload(byte[] bytes, String ext) throws IOException {
        String path = UUID.randomUUID() + "." + ext;
        HttpRequest request = authorized(objectUrl(path))
                .header("Content-Type", CONTENT_TYPES.get(ext))
                .POST(HttpRequest.BodyPublishers.ofByteArray(bytes))
                .build();
        HttpResponse<String> res = send(request);
        if (res.statusCode() / 100 != 2) {
            throw new IOException("Storage upload failed: HTTP " + res.statusCode() + " " + res.body());
        }
        return baseUrl + "/storage/v1/object/public/" + bucket + "/" + path;
    }

    public void delete(String publicUrl) {
        String marker = "/object/public/" + bucket + "/";
        int idx = publicUrl == null ? -1 : publicUrl.indexOf(marker);
        if (idx < 0) {
            logger.warn("Storage delete skipped, unexpected url: {}", publicUrl);
            return;
        }
        String path = publicUrl.substring(idx + marker.length());
        try {
            HttpResponse<String> res = send(authorized(objectUrl(path)).DELETE().build());
            if (res.statusCode() / 100 != 2) {
                logger.warn("Storage delete failed: HTTP {} {} ({})", res.statusCode(), res.body(), path);
            }
        } catch (IOException e) {
            logger.warn("Storage delete failed: {}", path, e);
        }
    }

    private String objectUrl(String path) {
        return baseUrl + "/storage/v1/object/" + bucket + "/" + path;
    }

    private HttpRequest.Builder authorized(String url) {
        return HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + serviceKey)
                .header("apikey", serviceKey);
    }

    private HttpResponse<String> send(HttpRequest request) throws IOException {
        try {
            return http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Storage request interrupted", e);
        }
    }
}
```

- [ ] **Step 2: 컴파일·기동 확인**

Run: `mvn -q compile`. Tomcat 재시작 → 기동 시 `Could not resolve placeholder` 없이 뜨면 환경변수 정상.

- [ ] **Step 3: 커밋**

```bash
git add -A
git commit -m "feat: supabase storage client"
```

---

### Task 14: 사진 등록·수정·삭제 서비스와 API

**Files:**
- Modify: `src/main/java/com/fashion/photo/service/PhotoService.java`
- Modify: `src/main/java/com/fashion/photo/service/impl/PhotoServiceImpl.java`
- Create: `src/main/java/com/fashion/photo/controller/PhotoApiController.java`

**Interfaces:**
- Consumes: `SupabaseStorage`, `PhotoDAO`, `Validation`, `Constants`, `Response`
- Produces:
  - `PhotoService.createPhoto(byte[] bytes, String ext, String title, String memo, List<Integer> categoryIds, long ownerId) → long id` (throws `IOException`)
  - `PhotoService.updatePhoto(long id, String title, String memo, List<Integer> categoryIds)` — 없으면 `IllegalArgumentException`
  - `PhotoService.deletePhoto(long id)` — 없으면 `IllegalArgumentException`
  - `POST /api/admin/photos` (multipart) → `{code, data:{id}}`
  - `POST /api/admin/photos/{id}` (JSON `{title, memo, categoryIds:[]}`)
  - `POST /api/admin/photos/{id}/delete`

- [ ] **Step 1: PhotoService에 메서드 추가**

```java
    long createPhoto(byte[] bytes, String ext, String title, String memo, List<Integer> categoryIds, long ownerId) throws IOException;

    void updatePhoto(long id, String title, String memo, List<Integer> categoryIds);

    void deletePhoto(long id);
```
(`import java.io.IOException;` 추가)

- [ ] **Step 2: PhotoServiceImpl에 구현 추가**

```java
    @Autowired
    private SupabaseStorage storage;

    // 업로드 → INSERT. INSERT가 실패하면 방금 올린 파일을 지운다 (고아 파일 방지). 트랜잭션 롤백은 예외 전파로 처리
    @Override
    @Transactional
    public long createPhoto(byte[] bytes, String ext, String title, String memo, List<Integer> categoryIds, long ownerId) throws IOException {
        String imageUrl = storage.upload(bytes, ext);
        try {
            Map<String, Object> param = new HashMap<>();
            param.put("ownerId", ownerId);
            param.put("title", title);
            param.put("memo", memo);
            param.put("imageUrl", imageUrl);
            photoDAO.insertPhoto(param);
            long id = ((Number) param.get("id")).longValue();
            photoDAO.insertPhotoCategories(id, categoryIds);
            return id;
        } catch (RuntimeException e) {
            storage.delete(imageUrl);
            throw e;
        }
    }

    @Override
    @Transactional
    public void updatePhoto(long id, String title, String memo, List<Integer> categoryIds) {
        Map<String, Object> param = new HashMap<>();
        param.put("id", id);
        param.put("title", title);
        param.put("memo", memo);
        if (photoDAO.updatePhoto(param) == 0) {
            throw new IllegalArgumentException("대상을 찾을 수 없습니다.");
        }
        photoDAO.deletePhotoCategories(id);
        photoDAO.insertPhotoCategories(id, categoryIds);
    }

    // DB 삭제(CASCADE)가 끝난 뒤 파일을 지운다. 파일 삭제 실패는 고아 파일로 남고 로그만 남는다
    @Override
    public void deletePhoto(long id) {
        Map<String, Object> photo = photoDAO.selectPhoto(id);
        if (photo == null) {
            throw new IllegalArgumentException("대상을 찾을 수 없습니다.");
        }
        photoDAO.deletePhoto(id);
        storage.delete((String) photo.get("image_url"));
    }
```
import 추가: `com.fashion.cmmn.storage.SupabaseStorage`, `org.springframework.transaction.annotation.Transactional`, `java.io.IOException`.

- [ ] **Step 3: PhotoApiController.java**

```java
package com.fashion.photo.controller;

import com.fashion.cmmn.util.Constants;
import com.fashion.cmmn.util.Response;
import com.fashion.cmmn.util.Validation;
import com.fashion.photo.service.PhotoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.*;

@Controller
@RequestMapping("/api/admin")
public class PhotoApiController {

    private static final Logger logger = LoggerFactory.getLogger(PhotoApiController.class);

    @Resource(name = "photoService")
    private PhotoService photoService;

    @ResponseBody
    @PostMapping("/photos")
    public Response create(@RequestParam("file") MultipartFile file,
                           @RequestParam(value = "title", required = false) String title,
                           @RequestParam(value = "memo", required = false) String memo,
                           @RequestParam(value = "categoryIds", required = false) List<Integer> categoryIds,
                           HttpSession session) {
        try {
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("이미지 파일을 선택해주세요.");
            }
            byte[] bytes = file.getBytes();
            String ext = Validation.imageExtension(Arrays.copyOf(bytes, 12));
            String cleanTitle = Validation.requireText(title, "제목", 100);
            String cleanMemo = Validation.optionalText(memo, "메모", 2000);
            List<Integer> ids = requireCategories(categoryIds);
            long ownerId = (Long) session.getAttribute(Constants.SESSION_USER_ID);

            long id = photoService.createPhoto(bytes, ext, cleanTitle, cleanMemo, ids, ownerId);
            return Response.of(Constants.SUCCESS, Collections.singletonMap("id", id));
        } catch (IllegalArgumentException e) {
            return Response.of(Constants.FAIL, e.getMessage(), null);
        } catch (Exception e) {
            logger.error("사진 등록 중 오류", e);
            return Response.of(Constants.FAIL, "이미지 업로드에 실패했습니다.", null);
        }
    }

    @ResponseBody
    @PostMapping("/photos/{id}")
    public Response update(@PathVariable long id, @RequestBody Map<String, Object> param) {
        try {
            String title = Validation.requireText(param.get("title"), "제목", 100);
            String memo = Validation.optionalText(param.get("memo"), "메모", 2000);
            List<Integer> ids = requireCategories(toIntList(param.get("categoryIds")));
            photoService.updatePhoto(id, title, memo, ids);
            return Response.of(Constants.SUCCESS);
        } catch (IllegalArgumentException e) {
            return Response.of(Constants.FAIL, e.getMessage(), null);
        } catch (Exception e) {
            logger.error("사진 수정 중 오류", e);
            return Response.of(Constants.FAIL, "사진 수정 중 오류가 발생했습니다.", null);
        }
    }

    @ResponseBody
    @PostMapping("/photos/{id}/delete")
    public Response delete(@PathVariable long id) {
        try {
            photoService.deletePhoto(id);
            return Response.of(Constants.SUCCESS);
        } catch (IllegalArgumentException e) {
            return Response.of(Constants.FAIL, e.getMessage(), null);
        } catch (Exception e) {
            logger.error("사진 삭제 중 오류", e);
            return Response.of(Constants.FAIL, "사진 삭제 중 오류가 발생했습니다.", null);
        }
    }

    private static List<Integer> requireCategories(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("카테고리를 하나 이상 선택해주세요.");
        }
        return new ArrayList<>(new LinkedHashSet<>(ids));
    }

    // JSON 배열은 List<Object>(Integer 또는 String)로 들어온다
    private static List<Integer> toIntList(Object value) {
        if (!(value instanceof List)) {
            return Collections.emptyList();
        }
        List<Integer> out = new ArrayList<>();
        for (Object o : (List<?>) value) {
            out.add(Integer.parseInt(String.valueOf(o)));
        }
        return out;
    }
}
```

- [ ] **Step 4: 컴파일 확인 후 커밋**

Run: `mvn -q compile`
```bash
git add -A
git commit -m "feat: photo create/update/delete service and api"
```

---

### Task 15: 사진 등록·수정 화면, 상세 페이지 관리자 버튼

**Files:**
- Modify: `src/main/java/com/fashion/photo/controller/PhotoController.java` (폼 2개 추가)
- Create: `src/main/webapp/WEB-INF/jsp/fashion/admin/photoForm.jsp`
- Create: `src/main/webapp/resources/js/app/photo/photoForm.js`
- Create: `src/main/webapp/resources/js/app/photo/detail.js` (삭제 버튼. 링크 UI는 Task 16에서 확장)
- Modify: `src/main/webapp/WEB-INF/jsp/fashion/photo/detail.jsp` (관리자 버튼 + 스크립트)
- Modify: `src/main/webapp/resources/css/fashion.css`

**Interfaces:**
- Consumes: `POST /api/admin/photos`, `POST /api/admin/photos/{id}`, `POST /api/admin/photos/{id}/delete`, `App.upload`, `App.post`
- Produces: `GET /admin/photos/new`, `GET /admin/photos/{id}/edit`. 모델 `photo`(수정 시), `categoryIds`, `mode`("new"|"edit").

- [ ] **Step 1: PhotoController에 추가**

```java
    @GetMapping("/admin/photos/new")
    public String newForm(Model model) {
        model.addAttribute("mode", "new");
        model.addAttribute("categoryIds", Collections.emptyList());
        return "/fashion/admin/photoForm";
    }

    @GetMapping("/admin/photos/{id}/edit")
    public String editForm(@PathVariable long id, Model model) {
        Map<String, Object> detail = photoService.selectPhotoDetail(id);
        if (detail == null) {
            throw new NotFoundException("photo: " + id);
        }
        model.addAttribute("mode", "edit");
        model.addAttribute("photo", detail.get("photo"));
        model.addAttribute("categoryIds", detail.get("categoryIds"));
        return "/fashion/admin/photoForm";
    }
```
import 추가: `java.util.Collections`.

- [ ] **Step 2: photoForm.jsp**

```jsp
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/common/taglib.jsp"%>
<h1 class="page-title">${mode eq 'new' ? '사진 등록' : '사진 수정'}</h1>
<form id="photoForm" class="photo-form" data-mode="${mode}" data-id="${photo.id}">
    <c:if test="${mode eq 'new'}">
        <label>
            <span>이미지 (jpg / png / webp, 10MB 이하)</span>
            <input type="file" name="file" accept="image/jpeg,image/png,image/webp" required>
        </label>
    </c:if>
    <c:if test="${mode eq 'edit'}">
        <img class="photo-form-preview" src="<c:out value='${photo.image_url}'/>" alt="">
    </c:if>
    <label>
        <span>제목</span>
        <input type="text" name="title" maxlength="100" required value="<c:out value='${photo.title}'/>">
    </label>
    <label>
        <span>메모</span>
        <textarea name="memo" rows="4" maxlength="2000"><c:out value="${photo.memo}"/></textarea>
    </label>
    <fieldset class="category-check">
        <legend>카테고리</legend>
        <c:forEach var="cat" items="${categories}">
            <label class="chip-check">
                <input type="checkbox" name="categoryIds" value="${cat.id}"
                    <c:forEach var="cid" items="${categoryIds}"><c:if test="${cid eq cat.id}">checked</c:if></c:forEach>>
                <c:out value="${cat.name}"/>
            </label>
        </c:forEach>
    </fieldset>
    <div class="form-actions">
        <button type="submit" class="btn btn-primary">저장</button>
        <a class="btn" href="<c:url value='${mode eq "new" ? "/" : "/photos/".concat(photo.id)}'/>">취소</a>
    </div>
</form>
<script defer src="<c:url value='/resources/js/app/photo/photoForm.js'/>"></script>
```

- [ ] **Step 3: photoForm.js**

```js
App.photoForm = (function () {
    var form = document.getElementById('photoForm'),
        submitting = false;

    function init() {
        form.addEventListener('submit', function (e) {
            e.preventDefault();
            submit();
        });
    }

    function submit() {
        if (submitting) return;
        var mode = form.dataset.mode,
            id = form.dataset.id,
            fd = new FormData(form);

        if (App.isEmpty(fd.get('title').trim())) {
            App.error('알림', '제목을 입력해주세요.');
            return;
        }
        if (fd.getAll('categoryIds').length === 0) {
            App.error('알림', '카테고리를 하나 이상 선택해주세요.');
            return;
        }
        if (mode === 'new' && !(fd.get('file') && fd.get('file').size > 0)) {
            App.error('알림', '이미지 파일을 선택해주세요.');
            return;
        }

        submitting = true;
        var req = mode === 'new'
            ? App.upload(contextPath + '/api/admin/photos', fd)
            : App.post(contextPath + '/api/admin/photos/' + id, {
                title: fd.get('title'),
                memo: fd.get('memo'),
                categoryIds: fd.getAll('categoryIds')
            });

        req.then(function (res) {
            if (res.code !== '00') {
                App.error('저장 실패', res.message);
                return;
            }
            location.href = contextPath + '/photos/' + (mode === 'new' ? res.data.id : id);
        })
        .catch(function () {})
        .then(function () { submitting = false; });
    }

    return { init: init };
}());

document.addEventListener('DOMContentLoaded', App.photoForm.init);
```

- [ ] **Step 4: detail.jsp에 관리자 버튼 추가** — `<h1 class="page-title">` 바로 아래에 삽입, 파일 끝에 스크립트 추가

```jsp
        <c:if test="${isAdmin}">
            <p class="admin-actions">
                <a class="btn" href="<c:url value='/admin/photos/${photo.id}/edit'/>">수정</a>
                <button type="button" class="btn btn-danger" id="deletePhotoBtn" data-id="${photo.id}">삭제</button>
            </p>
        </c:if>
```
파일 끝:
```jsp
<c:if test="${isAdmin}">
    <script defer src="<c:url value='/resources/js/app/photo/detail.js'/>"></script>
</c:if>
```

- [ ] **Step 5: detail.js (삭제만. Task 16에서 링크 UI 추가)**

```js
App.detail = (function () {
    function init() {
        var del = document.getElementById('deletePhotoBtn');
        if (del) del.addEventListener('click', function () { deletePhoto(del.dataset.id); });
    }

    function deletePhoto(id) {
        if (!confirm('이 사진과 링크를 모두 삭제할까요?')) return;
        App.post(contextPath + '/api/admin/photos/' + id + '/delete')
            .then(function (res) {
                if (res.code !== '00') { App.error('삭제 실패', res.message); return; }
                location.href = contextPath + '/';
            })
            .catch(function () {});
    }

    return { init: init };
}());

document.addEventListener('DOMContentLoaded', App.detail.init);
```

- [ ] **Step 6: fashion.css에 추가**

```css
.photo-form { max-width: 560px; display: flex; flex-direction: column; gap: 16px; }
.photo-form label { display: flex; flex-direction: column; gap: 6px; font-size: 13px; color: var(--muted); }
.photo-form input[type=text], .photo-form textarea { padding: 10px; border: 1px solid var(--line); border-radius: 8px; font: inherit; color: var(--fg); }
.photo-form-preview { max-width: 240px; border-radius: 10px; }
.category-check { border: 0; display: flex; gap: 8px; flex-wrap: wrap; }
.category-check legend { font-size: 13px; color: var(--muted); margin-bottom: 6px; }
.chip-check { display: inline-flex; align-items: center; gap: 6px; padding: 6px 12px; border: 1px solid var(--line); border-radius: 999px; cursor: pointer; }
.chip-check:has(input:checked) { background: var(--fg); color: #fff; }
.form-actions, .admin-actions { display: flex; gap: 8px; margin-bottom: 16px; }
```

- [ ] **Step 7: 동작 확인**

| 시도 | Expected |
|---|---|
| 로그인 → 사진 등록 → jpg + 제목 + 카테고리 2개 → 저장 | 상세 페이지로 이동, Supabase 버킷에 `UUID.jpg` 생김, `/` 그리드에 표시 |
| gif 파일 업로드 | alert "jpg, png, webp 이미지만 등록할 수 있습니다." |
| 카테고리 미선택 | alert "카테고리를 하나 이상 선택해주세요." |
| 수정 → 제목·카테고리 변경 → 저장 | 상세에 반영 |
| 삭제 | 홈으로, 버킷에서 파일 사라짐, `SELECT COUNT(*) FROM product_link WHERE photo_id=…` = 0 |
| 로그아웃 후 `/admin/photos/new` | `/login`으로 |

- [ ] **Step 8: 커밋**

```bash
git add -A
git commit -m "feat: photo create/edit form and delete action"
```

---

## 5단계. 링크 추가·수정·삭제

### Task 16: 링크 서비스·API·상세 페이지 관리자 UI

**Files:**
- Modify: `src/main/java/com/fashion/photo/service/PhotoService.java`
- Modify: `src/main/java/com/fashion/photo/service/impl/PhotoServiceImpl.java`
- Modify: `src/main/java/com/fashion/photo/controller/PhotoApiController.java`
- Modify: `src/main/webapp/WEB-INF/jsp/fashion/photo/detail.jsp`
- Modify: `src/main/webapp/resources/js/app/photo/detail.js`
- Modify: `src/main/webapp/resources/css/fashion.css`

**Interfaces:**
- Produces:
  - `PhotoService.addLink(long photoId, String itemLabel, String url, String title)`, `updateLink(long linkId, String itemLabel, String url, String title)`, `deleteLink(long linkId)` — 대상 없으면 `IllegalArgumentException("대상을 찾을 수 없습니다.")`
  - `POST /api/admin/photos/{id}/links`, `POST /api/admin/links/{linkId}`, `POST /api/admin/links/{linkId}/delete` (JSON `{itemLabel, url, title}`)

- [ ] **Step 1: PhotoService에 추가**

```java
    void addLink(long photoId, String itemLabel, String url, String title);

    void updateLink(long linkId, String itemLabel, String url, String title);

    void deleteLink(long linkId);
```

- [ ] **Step 2: PhotoServiceImpl에 추가**

```java
    @Override
    public void addLink(long photoId, String itemLabel, String url, String title) {
        if (photoDAO.selectPhoto(photoId) == null) {
            throw new IllegalArgumentException("대상을 찾을 수 없습니다.");
        }
        Map<String, Object> param = new HashMap<>();
        param.put("photoId", photoId);
        param.put("itemLabel", itemLabel);
        param.put("url", url);
        param.put("title", title);
        linkDAO.insertLink(param);
    }

    @Override
    public void updateLink(long linkId, String itemLabel, String url, String title) {
        Map<String, Object> param = new HashMap<>();
        param.put("id", linkId);
        param.put("itemLabel", itemLabel);
        param.put("url", url);
        param.put("title", title);
        if (linkDAO.updateLink(param) == 0) {
            throw new IllegalArgumentException("대상을 찾을 수 없습니다.");
        }
    }

    @Override
    public void deleteLink(long linkId) {
        if (linkDAO.deleteLink(linkId) == 0) {
            throw new IllegalArgumentException("대상을 찾을 수 없습니다.");
        }
    }
```

- [ ] **Step 3: PhotoApiController에 추가**

```java
    @ResponseBody
    @PostMapping("/photos/{id}/links")
    public Response addLink(@PathVariable long id, @RequestBody Map<String, Object> param) {
        try {
            photoService.addLink(id,
                    Validation.requireText(param.get("itemLabel"), "아이템", 30),
                    Validation.requireHttpUrl(param.get("url")),
                    Validation.optionalText(param.get("title"), "제목", 100));
            return Response.of(Constants.SUCCESS);
        } catch (IllegalArgumentException e) {
            return Response.of(Constants.FAIL, e.getMessage(), null);
        } catch (Exception e) {
            logger.error("링크 추가 중 오류", e);
            return Response.of(Constants.FAIL, "링크 추가 중 오류가 발생했습니다.", null);
        }
    }

    @ResponseBody
    @PostMapping("/links/{linkId}")
    public Response updateLink(@PathVariable long linkId, @RequestBody Map<String, Object> param) {
        try {
            photoService.updateLink(linkId,
                    Validation.requireText(param.get("itemLabel"), "아이템", 30),
                    Validation.requireHttpUrl(param.get("url")),
                    Validation.optionalText(param.get("title"), "제목", 100));
            return Response.of(Constants.SUCCESS);
        } catch (IllegalArgumentException e) {
            return Response.of(Constants.FAIL, e.getMessage(), null);
        } catch (Exception e) {
            logger.error("링크 수정 중 오류", e);
            return Response.of(Constants.FAIL, "링크 수정 중 오류가 발생했습니다.", null);
        }
    }

    @ResponseBody
    @PostMapping("/links/{linkId}/delete")
    public Response deleteLink(@PathVariable long linkId) {
        try {
            photoService.deleteLink(linkId);
            return Response.of(Constants.SUCCESS);
        } catch (IllegalArgumentException e) {
            return Response.of(Constants.FAIL, e.getMessage(), null);
        } catch (Exception e) {
            logger.error("링크 삭제 중 오류", e);
            return Response.of(Constants.FAIL, "링크 삭제 중 오류가 발생했습니다.", null);
        }
    }
```

- [ ] **Step 4: detail.jsp의 `<section class="link-groups">`를 다음으로 교체** — 방문자는 링크 목록, 관리자는 각 링크가 수정 폼.

```jsp
        <section class="link-groups">
            <c:if test="${empty linkGroups}">
                <p class="empty">등록된 제품 링크가 없습니다.</p>
            </c:if>
            <datalist id="labelOptions">
                <c:forEach var="group" items="${linkGroups}"><option value="<c:out value='${group.key}'/>"></c:forEach>
            </datalist>
            <c:forEach var="group" items="${linkGroups}">
                <h2 class="link-label"><c:out value="${group.key}"/></h2>
                <ul class="link-list">
                    <c:forEach var="link" items="${group.value}">
                        <li>
                            <c:choose>
                                <c:when test="${isAdmin}">
                                    <form class="link-form link-edit" data-id="${link.id}">
                                        <input type="text" name="itemLabel" list="labelOptions" maxlength="30" value="<c:out value='${link.item_label}'/>" placeholder="아이템">
                                        <input type="url" name="url" value="<c:out value='${link.url}'/>" placeholder="https://">
                                        <input type="text" name="title" maxlength="100" value="<c:out value='${link.title}'/>" placeholder="제목(선택)">
                                        <button type="submit" class="btn">저장</button>
                                        <button type="button" class="btn btn-danger link-delete">삭제</button>
                                    </form>
                                </c:when>
                                <c:otherwise>
                                    <a href="<c:out value='${link.url}'/>" target="_blank" rel="noopener noreferrer">
                                        <c:out value="${empty link.title ? link.url : link.title}"/>
                                    </a>
                                </c:otherwise>
                            </c:choose>
                        </li>
                    </c:forEach>
                </ul>
            </c:forEach>
            <c:if test="${isAdmin}">
                <h2 class="link-label">링크 추가</h2>
                <form class="link-form" id="linkAddForm" data-photo-id="${photo.id}">
                    <input type="text" name="itemLabel" list="labelOptions" maxlength="30" placeholder="아이템 (예: 아우터)" required>
                    <input type="url" name="url" placeholder="https://" required>
                    <input type="text" name="title" maxlength="100" placeholder="제목(선택)">
                    <button type="submit" class="btn btn-primary">추가</button>
                </form>
            </c:if>
        </section>
```

- [ ] **Step 5: detail.js 전체 교체**

```js
App.detail = (function () {
    function init() {
        var del = document.getElementById('deletePhotoBtn');
        if (del) del.addEventListener('click', function () { deletePhoto(del.dataset.id); });

        var add = document.getElementById('linkAddForm');
        if (add) add.addEventListener('submit', function (e) {
            e.preventDefault();
            send(contextPath + '/api/admin/photos/' + add.dataset.photoId + '/links', App.formToObject(add));
        });

        document.querySelectorAll('.link-edit').forEach(function (form) {
            form.addEventListener('submit', function (e) {
                e.preventDefault();
                send(contextPath + '/api/admin/links/' + form.dataset.id, App.formToObject(form));
            });
            form.querySelector('.link-delete').addEventListener('click', function () {
                if (!confirm('이 링크를 삭제할까요?')) return;
                send(contextPath + '/api/admin/links/' + form.dataset.id + '/delete');
            });
        });
    }

    // 성공하면 페이지를 다시 읽어 서버가 그린 그룹 순서를 그대로 쓴다
    function send(url, data) {
        App.post(url, data)
            .then(function (res) {
                if (res.code !== '00') { App.error('실패', res.message); return; }
                location.reload();
            })
            .catch(function () {});
    }

    function deletePhoto(id) {
        if (!confirm('이 사진과 링크를 모두 삭제할까요?')) return;
        App.post(contextPath + '/api/admin/photos/' + id + '/delete')
            .then(function (res) {
                if (res.code !== '00') { App.error('삭제 실패', res.message); return; }
                location.href = contextPath + '/';
            })
            .catch(function () {});
    }

    return { init: init };
}());

document.addEventListener('DOMContentLoaded', App.detail.init);
```

- [ ] **Step 6: fashion.css에 추가**

```css
.link-form { display: grid; grid-template-columns: 110px minmax(0, 1fr) 140px auto auto; gap: 6px; align-items: center; }
.link-form input { min-width: 0; padding: 8px; border: 1px solid var(--line); border-radius: 8px; font: inherit; }
.link-form .btn { padding: 8px 12px; }
@media (max-width: 800px) { .link-form { grid-template-columns: 1fr 1fr; } .link-form input[name=url] { grid-column: 1 / -1; } }
```

- [ ] **Step 7: 동작 확인**

| 시도 | Expected |
|---|---|
| 관리자: `아우터` / `https://musinsa.com/...` / `린넨 자켓` 추가 | 새로고침 후 "아우터" 그룹에 표시 |
| 같은 사진에 `바지` 추가 | "바지" 그룹이 아우터 아래에 생김 |
| 아우터 링크의 아이템을 `자켓`으로 바꿔 저장 | "자켓" 그룹으로 이동 |
| URL을 `javascript:alert(1)`로 저장 | alert "http:// 또는 https://로 시작하는 URL을 입력해주세요." |
| 링크 삭제 | 그룹에서 사라짐, 마지막 링크면 그룹 자체가 사라짐 |
| 로그아웃 후 상세 | 입력칸 없이 링크만, 새 탭으로 열림 |

- [ ] **Step 8: 커밋**

```bash
git add -A
git commit -m "feat: product link add/edit/delete on detail page"
```

---

## 6단계. 마무리와 배포

### Task 17: Dockerfile, README, 배포

**Files:**
- Create: `Dockerfile`, `.dockerignore`, `README.md`

- [ ] **Step 1: Dockerfile**

```dockerfile
FROM tomcat:9-jdk17
COPY src/main/webapp/META-INF/context.xml /usr/local/tomcat/conf/Catalina/localhost/ROOT.xml
RUN rm -rf /usr/local/tomcat/webapps/*
COPY target/fashion-archive.war /usr/local/tomcat/webapps/ROOT.war
EXPOSE 8080
```

- [ ] **Step 2: .dockerignore**

```
.git
.idea
docs
src/test
```

- [ ] **Step 3: README.md** — 환경변수 표, 로컬 실행 3단계, 배포 절차를 스펙 10절 그대로 옮긴다.

```markdown
# Fashion Archive

계절·품목별 코디 사진과 "비슷한 제품" 링크를 모아 두는 개인 큐레이션 사이트.
JSP + Spring MVC(eGovFrame 4.1) + MyBatis + PostgreSQL. 사진 파일은 Supabase Storage.

## 환경변수

| 이름 | 설명 | 기본값 |
|---|---|---|
| FASHION_DB_URL | JDBC URL | jdbc:postgresql://localhost:5432/fashion |
| FASHION_DB_USER | DB 사용자 | postgres |
| FASHION_DB_PASSWORD | DB 비밀번호 | (필수) |
| FASHION_SUPABASE_URL | https://xxxx.supabase.co | (필수) |
| FASHION_SUPABASE_SERVICE_KEY | service_role 키 | (필수) |
| FASHION_SUPABASE_BUCKET | 공개 버킷 이름 | photos |
| FASHION_ADMIN_ID | 초기 관리자 아이디 | admin |
| FASHION_ADMIN_PASSWORD_HASH | BCrypt 해시 (`PasswordHashTool`로 생성) | (필수) |

## 로컬 실행

1. `psql -U postgres -c "CREATE DATABASE fashion ENCODING 'UTF8';"` 후 `psql -U postgres -d fashion -f src/main/resources/schema.sql`
2. 위 환경변수를 IntelliJ Tomcat 실행 설정에 입력
3. Tomcat 9 실행 → http://localhost:8080/

## 배포

1. `mvn -q package -DskipTests`
2. `docker build -t fashion-archive .`
3. `docker run -p 8080:8080 -e FASHION_DB_URL=... -e FASHION_DB_PASSWORD=... (나머지 환경변수) fashion-archive`
4. 컨테이너 호스팅(Railway / Render / Fly.io)에 Git 연결, 환경변수 입력. Supabase Postgres의 JDBC URL은 대시보드 Connect → JDBC 항목 사용.
```

- [ ] **Step 4: 로컬 Docker 확인**

```bash
mvn -q package -DskipTests && docker build -t fashion-archive . && docker run --rm -p 8081:8080 -e FASHION_DB_URL=jdbc:postgresql://host.docker.internal:5432/fashion -e FASHION_DB_PASSWORD=... -e FASHION_SUPABASE_URL=... -e FASHION_SUPABASE_SERVICE_KEY=... -e FASHION_ADMIN_PASSWORD_HASH='...' fashion-archive
```
Expected: `http://localhost:8081/` 정상. (`host.docker.internal`로 호스트 Postgres 접속.)

- [ ] **Step 5: 호스팅 선택** — 이 시점에 Railway / Render / Fly.io의 현재 무료·저가 티어와 Supabase 무료 티어(일시정지 정책 포함)를 공식 페이지에서 확인하고 결정한다. 이 문서는 그 결과를 확정하지 않는다.

- [ ] **Step 6: 커밋**

```bash
git add -A
git commit -m "chore: dockerfile and readme"
```

---

## IntelliJ 설정 안내 (1단계 Task 3 Step 15에서 사용)

IntelliJ IDEA **Ultimate** 2026.2 기준. Community에는 Tomcat 실행 설정이 없으므로 이 절은 Ultimate 전제다.

1. **프로젝트 열기**: File → Open → `C:\dev\fashion-archive` 선택 → "Maven project" 감지 팝업이 뜨면 Load. 우측 Maven 도구창에서 새로고침(↻) 아이콘을 눌러 의존성을 받는다. (egovframe 저장소에서 처음 받을 때 수 분 걸릴 수 있다.)
2. **SDK 확인**: File → Project Structure → Project → SDK가 `temurin-17`(또는 JDK 17)인지, Language level이 17인지 확인.
3. **인코딩**: Settings → Editor → File Encodings → Global/Project/Default encoding for properties files 모두 `UTF-8`, "Transparent native-to-ascii conversion" 체크.
4. **Tomcat 실행 설정**: 우측 상단 Run 드롭다운 → Edit Configurations… → `+` → **Tomcat Server → Local**.
   - Name: `fashion-archive`
   - Application server: Configure… → Tomcat Home `C:\dev\apache-tomcat-9.0.93`
   - **Deployment 탭**: `+` → Artifact… → `fashion-archive:war exploded` 선택 → Application context를 **`/`** 로 수정 (기본값 `/fashion_archive_war_exploded`를 지운다)
   - **Server 탭**: On 'Update' action → `Update classes and resources`, On frame deactivation → `Update classes and resources` (JSP·CSS·JS 수정 시 재시작 없이 반영). HTTP port 8080.
   - **Startup/Connection 탭**: Run과 Debug 각각에서 Environment Variables 표에 추가
     - `FASHION_DB_PASSWORD` = 로컬 postgres 비밀번호
     - `FASHION_ADMIN_PASSWORD_HASH` = Task 7에서 생성한 해시 (2단계부터)
     - `FASHION_SUPABASE_URL`, `FASHION_SUPABASE_SERVICE_KEY` (4단계부터)
     - "Pass environment variables" 체크박스가 있으면 켠다.
   - OK.
5. **실행**: 실행 설정을 선택하고 ▶(Run) 또는 🐞(Debug). 하단 Services/Run 창에 Tomcat 로그가 뜨고, 브라우저가 자동으로 `http://localhost:8080/`을 연다.
6. **자주 만나는 문제**
   | 증상 | 원인 / 조치 |
   |---|---|
   | Deployment 탭에 artifact가 없음 | Maven 로드 전. Maven 새로고침 후 다시 시도 |
   | 콘솔에 한글이 `???` | Settings → Build Tools → Maven → Runner VM options `-Dfile.encoding=UTF-8`, Tomcat 실행 설정 VM options에도 동일 추가 |
   | `Could not resolve placeholder` | Startup/Connection 탭의 환경변수 누락. Run/Debug 둘 다 입력했는지 확인 |
   | 8080 사용 중 | 다른 Tomcat/앱 종료 또는 Server 탭 포트 변경 |
   | JSP 수정이 반영 안 됨 | Server 탭의 Update 옵션 확인, 또는 화면에서 Ctrl+F5 |
