ARG RUNTIME_VERSION
ARG RUNTIME_IMAGE=jre
FROM --platform=linux/amd64 192.168.1.162:5000/library/maven:3.9-eclipse-temurin-${RUNTIME_VERSION} AS build
RUN apt-get update \
    && apt-get upgrade -y \
    && rm -rf /var/lib/apt/lists/*
ARG PROJECT_PATH
WORKDIR /source
COPY .mvn/settings.xml /tmp/settings.xml
COPY ${PROJECT_PATH}/pom.xml ./pom.xml
RUN --mount=type=cache,id=maven-repo,target=/root/.m2/repository \
    mvn --batch-mode -s /tmp/settings.xml dependency:go-offline
COPY ${PROJECT_PATH}/src ./src
ARG BUILD_OUTPUT
RUN --mount=type=cache,id=maven-repo,target=/root/.m2/repository \
    mvn --batch-mode -s /tmp/settings.xml package -DskipTests
# Wyekstrahowany layout (thin jar + BOOT-INF/lib/*.jar rozpakowane do lib/) potrzebny tylko dla
# serwisów, które w runtime wołają zewnętrzny proces "javac" (np. jasper_service kompilujący .jrxml
# przez JasperReports JRJavacCompiler) - javac nie widzi klas zagnieżdżonych w BOOT-INF/lib wewnątrz
# jednego fat jara, tylko realne pliki .jar na classpath. Ekstrakcja jest tania i deterministyczna,
# więc robiona zawsze w tym stage'u - wybór, z którego layoutu skorzysta finalny obraz, jest w
# ARG JAR_LAYOUT drugiego stage'u.
RUN cp ${BUILD_OUTPUT} /source/app.jar \
    && java -Djarmode=tools -jar /source/app.jar extract --destination /source/extracted

FROM --platform=linux/amd64 192.168.1.162:5000/library/eclipse-temurin:${RUNTIME_VERSION}-${RUNTIME_IMAGE}
RUN apt-get update \
    && apt-get upgrade -y \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && rm -f /usr/bin/pebble
ARG APP_PORT
ARG START_COMMAND
ARG JAR_LAYOUT=fat
WORKDIR /app
COPY --from=build /source/app.jar ./app.jar
COPY --from=build /source/extracted ./extracted
# JAR_LAYOUT=extracted usuwa fat jar i zostawia tylko wyekstrahowany layout (app.jar wewnątrz
# extracted/ jest "thin" - manifest Class-Path wskazuje na extracted/lib/*.jar, a nie BOOT-INF).
RUN if [ "$JAR_LAYOUT" = "extracted" ]; then \
      rm -f /app/app.jar && mv /app/extracted/* /app/ && rmdir /app/extracted; \
    else \
      rm -rf /app/extracted; \
    fi
ENV START_COMMAND=${START_COMMAND}
USER 10001
EXPOSE ${APP_PORT}
CMD ["sh", "-c", "exec ${START_COMMAND}"]
