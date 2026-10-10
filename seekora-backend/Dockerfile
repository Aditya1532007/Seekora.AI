FROM maven:3.9.9-eclipse-temurin-11 AS build

WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn --batch-mode package

FROM jetty:9-jre11
COPY --from=build /workspace/target/seekora.war /var/lib/jetty/webapps/ROOT.war

CMD ["sh", "-c", "exec java -jar \"$JETTY_HOME/start.jar\" jetty.http.port=\"${PORT:-8080}\""]
