# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:resolve -B || true
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Generate a self-signed PKCS12 keystore so the Spring Boot listener can
# serve HTTPS to the reverse proxy. This removes the plaintext upstream hop
# that the proxy used to make. The keystore lives inside the image (not in
# a mounted volume) and is regenerated on every build.
RUN apk add --no-cache openssl \
    && mkdir -p /app/certs \
    && openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
         -keyout /app/certs/backend.key \
         -out /app/certs/backend.crt \
         -subj "/C=US/ST=Local/L=Local/O=Demo/CN=backend" \
    && openssl pkcs12 -export \
         -in /app/certs/backend.crt \
         -inkey /app/certs/backend.key \
         -out /app/certs/backend.p12 \
         -name backend \
         -password pass:changeit \
    && rm -f /app/certs/backend.key /app/certs/backend.crt
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8443
ENTRYPOINT ["java", "-jar", "app.jar"]
