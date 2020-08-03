FROM openjdk:8-jre-alpine

RUN apk --no-cache add curl

ARG JAR_FILE=spring-showcase.jar
COPY target/${JAR_FILE} /opt/application.jar

HEALTHCHECK --start-period=10s --timeout=60s --retries=10 --interval=5s CMD curl -f http://localhost:8080/actuator/health || exit 1

ENV JAVA_OPTS="-Djava.net.preferIPv4Stack=true -Djava.net.preferIPv4Addresses=true"

EXPOSE 8080
ENTRYPOINT exec java $JAVA_OPTS -jar /opt/application.jar