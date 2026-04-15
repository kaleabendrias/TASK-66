FROM maven:3.9-eclipse-temurin-21
WORKDIR /tests
COPY pom.xml .
# Download dependencies layer separately for better caching
RUN mvn dependency:go-offline -B
COPY src ./src
CMD ["mvn", "test", "-B"]
