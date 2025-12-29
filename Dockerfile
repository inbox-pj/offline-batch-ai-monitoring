# =============================================================================
# Multi-stage Dockerfile for AI Monitoring (Backend + Frontend)
# =============================================================================

# =============================================================================
# STAGE 1: Build Backend (Java/Spring Boot)
# =============================================================================
FROM maven:3.9-eclipse-temurin-21-alpine AS backend-build
WORKDIR /app

# Copy pom.xml and download dependencies (for caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# =============================================================================
# STAGE 2: Build Frontend (React)
# =============================================================================
FROM node:18-alpine AS frontend-build
WORKDIR /app

# Copy package files
COPY frontend/package*.json ./

# Install dependencies with legacy peer deps to avoid conflicts
RUN npm install --legacy-peer-deps

# Copy source code
COPY frontend/ ./

# Build the app
RUN npm run build

# =============================================================================
# STAGE 3: Backend Runtime
# =============================================================================
FROM eclipse-temurin:21-jre-alpine AS backend
WORKDIR /app

# Add non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy JAR from build stage
COPY --from=backend-build /app/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# JVM optimization flags
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+OptimizeStringConcat -XX:+UseStringDeduplication"

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

# =============================================================================
# STAGE 4: Frontend Runtime (Nginx)
# =============================================================================
FROM nginx:alpine AS frontend
WORKDIR /usr/share/nginx/html

# Copy custom nginx config
COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf

# Copy built files from frontend build stage
COPY --from=frontend-build /app/build .

# Expose port
EXPOSE 3001

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:3001 || exit 1

CMD ["nginx", "-g", "daemon off;"]

# =============================================================================
# DEFAULT TARGET: Backend
# Use --target=frontend to build the frontend image
# =============================================================================

