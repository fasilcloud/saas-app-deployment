# Use lightweight Java 17 runtime
FROM eclipse-temurin:17-jre-alpine

# Create non-root user and group
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Set working directory
WORKDIR /app

# Copy JAR and change ownership
COPY --chown=appuser:appgroup target/saas-demo-1.0.0.jar app.jar

# Switch to non-root user
USER appuser

# Expose application port
EXPOSE 8080

# Run Spring Boot app
ENTRYPOINT ["java", "-jar", "app.jar"]

