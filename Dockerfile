FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /build

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source - split to bust cache on static file changes
COPY src/main/java ./src/main/java
COPY src/main/resources ./src/main/resources

# Build
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S hotelgroup && adduser -S hoteluser -G hotelgroup
COPY --from=builder /build/target/hotel-management-*.jar app.jar
RUN mkdir -p /app/logs && chown -R hoteluser:hotelgroup /app
USER hoteluser
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]