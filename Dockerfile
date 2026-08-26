# ─────────────────────────────────────────────────────────────────────────────
# Obraz aplikacji szymtrener.pl — pod wdrożenie na VPS przez Coolify.
#
# Kontener jest BEZSTANOWY: pliki wgrywane w panelu leżą w bazie (MediaBlob,
# byte[]), nie na dysku. Nie podpinaj wolumenu — kopia zapasowa to sam pg_dump.
# ─────────────────────────────────────────────────────────────────────────────

# ── budowanie ──
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Zależności w osobnej warstwie: zmiana kodu nie unieważnia pobranego repozytorium.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# Testy integracyjne wymagają Dockera (Testcontainers), więc w obrazie ich nie ma —
# uruchamiaj je w CI albo lokalnie przez `mvn verify`.
RUN mvn -B -q clean package -DskipTests

# Rozbicie fat-jara na warstwy: zależności zmieniają się rzadko, kod często,
# więc kolejne wdrożenia wysyłają na serwer kilka MB zamiast kilkudziesięciu.
RUN java -Djarmode=layertools -jar target/*.jar extract --destination /build/layers

# ── uruchomienie ──
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Daty w panelu i harmonogram publikacji liczą się w czasie lokalnym.
ENV TZ=Europe/Warsaw
RUN apk add --no-cache tzdata curl \
 && cp /usr/share/zoneinfo/Europe/Warsaw /etc/localtime \
 && echo "Europe/Warsaw" > /etc/timezone

# Nie root: aplikacja nie potrzebuje żadnych uprawnień poza czytaniem własnych plików.
RUN addgroup -S app && adduser -S -G app app

COPY --from=build --chown=app:app /build/layers/dependencies/ ./
COPY --from=build --chown=app:app /build/layers/spring-boot-loader/ ./
COPY --from=build --chown=app:app /build/layers/snapshot-dependencies/ ./
COPY --from=build --chown=app:app /build/layers/application/ ./

USER app
EXPOSE 8080

# JVM sama dobiera pamięć do limitu kontenera — bez tego przy ciasnym limicie
# proces bywa ubijany przez OOM killera zamiast rzucić czytelny błąd.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

# Coolify czeka na zdrowy kontener przed przełączeniem ruchu.
# start-period z zapasem: pierwszy start robi migracje Flyway.
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
  CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
