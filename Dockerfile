# Single-origin production image: the React app is built and bundled into the Spring Boot jar,
# so the API and the UI are served from one process at one URL (no CORS, no separate frontend host).

# --- Stage 1: build the React frontend ---
FROM node:20-alpine AS frontend
WORKDIR /app
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# --- Stage 2: build the Spring Boot jar with the frontend bundled in ---
FROM eclipse-temurin:17-jdk AS backend
WORKDIR /app
# Warm the Gradle wrapper/deps layer first for faster rebuilds.
COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle settings.gradle* ./
RUN chmod +x ./gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true
COPY src ./src
# Drop the built frontend into static so bootJar serves it at the app root.
COPY --from=frontend /app/dist ./src/main/resources/static
RUN ./gradlew --no-daemon clean bootJar -x test

# --- Stage 3: slim runtime ---
FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app
COPY --from=backend /app/build/libs/*-SNAPSHOT.jar app.jar
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java", "-jar", "app.jar"]
