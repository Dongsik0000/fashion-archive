FROM tomcat:9-jdk17
# 서버에서는 Tomcat을 loopback에만 열고, 로컬 Docker에서는 기본값(0.0.0.0)으로 포트 매핑할 수 있게 한다.
ENV CATALINA_OPTS="-Dtomcat.address=0.0.0.0"
RUN sed -i 's/<Connector port="8080"/<Connector address="${tomcat.address}" port="8080"/' /usr/local/tomcat/conf/server.xml
# SameSite 쿠키 설정을 /p1 컨텍스트에 적용 (nginx가 /p1/ 하위 경로로 리버스 프록시하므로 ROOT가 아닌 p1 컨텍스트로 배포한다)
COPY src/main/webapp/META-INF/context.xml /usr/local/tomcat/conf/Catalina/localhost/p1.xml
RUN rm -rf /usr/local/tomcat/webapps/*
COPY target/fashion-archive.war /usr/local/tomcat/webapps/p1.war
EXPOSE 8080
