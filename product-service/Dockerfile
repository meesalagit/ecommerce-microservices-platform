# ---------- Stage 1: build the jar with Maven ----------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Copy the Maven wrapper + pom first so dependencies are cached between builds
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

# Now copy the source code and build (tests run in CI, not here)
COPY src src
RUN ./mvnw -B -q package -DskipTests

# ---------- Stage 2: small runtime image with only the JRE + jar ----------
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
