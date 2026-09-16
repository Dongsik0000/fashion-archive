FROM tomcat:9-jdk17
# SameSite 쿠키 설정을 ROOT 컨텍스트에 적용
COPY src/main/webapp/META-INF/context.xml /usr/local/tomcat/conf/Catalina/localhost/ROOT.xml
RUN rm -rf /usr/local/tomcat/webapps/*
COPY target/fashion-archive.war /usr/local/tomcat/webapps/ROOT.war
EXPOSE 8080
