FROM node:24-bookworm-slim AS frontend
WORKDIR /frontend
COPY src/frontend/package.json src/frontend/package-lock.json ./
RUN npm ci
COPY src/frontend/ ./
RUN npm run build

FROM eclipse-temurin:25-jdk AS backend
WORKDIR /backend
COPY src/backend/ ./
COPY --from=frontend /frontend/dist/ ./src/main/resources/static/
RUN chmod +x gradlew && ./gradlew --no-daemon bootJar

FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app
RUN groupadd --system unichope && useradd --system --gid unichope --home-dir /app unichope
COPY --from=backend --chown=unichope:unichope /backend/build/libs/unichope-0.0.1-SNAPSHOT.jar ./app.jar
USER unichope
ENV SPRING_PROFILES_ACTIVE=app-api
ENV SERVER_ADDRESS=0.0.0.0
ENV SERVER_FORWARD_HEADERS_STRATEGY=framework
EXPOSE 10000
CMD ["sh", "-c", "exec java -jar /app/app.jar --server.port=${PORT:-10000}"]
