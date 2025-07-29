FROM ghcr.io/navikt/baseimages/temurin:21

COPY app/target/app.jar /app/app.jar

ENV JAVA_OPTS="-Xmx2560m \
               -Djava.security.egd=file:/dev/./urandom \
               -Dspring.profiles.active=nais"