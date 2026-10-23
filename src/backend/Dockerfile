# Production image for Render (or any Docker host). Build context: src/backend.
# Configure at runtime: UNICHOPE_JDBC_URL, UNICHOPE_JDBC_USER, UNICHOPE_JDBC_PASSWORD,
# and optionally UNICHOPE_AI_MODE=shared with SOC_LLM_API_KEY_*. Never bake secrets in.

FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
# Download dependencies in their own layer so source-only changes rebuild faster.
RUN ./gradlew --no-daemon --console=plain dependencies > /dev/null
COPY src src
RUN ./gradlew --no-daemon --console=plain bootJar

FROM eclipse-temurin:25-jre
RUN useradd --system --no-create-home unichope
WORKDIR /app
COPY --from=build /workspace/build/libs/unichope-0.0.1-SNAPSHOT.jar app.jar
USER unichope
# Production profile only: no `local`, so session cookies stay Secure.
ENV SPRING_PROFILES_ACTIVE=app-api
# Fit the JVM into a 512 MB free instance.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Xss512k"
EXPOSE 8080
# Render supplies PORT; default to 8080 elsewhere.
CMD ["sh", "-c", "exec java -jar app.jar --server.port=${PORT:-8080}"]
