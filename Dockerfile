#######################################
# Stage 1: Build
#######################################
FROM eclipse-temurin:25-jdk AS build

WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Ensure maven wrapper runs in linux
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

# Download the dependencies
RUN ./mvnw -B dependency:go-offline

# Copy application source
COPY src/ ./src/

# Package spring boot application
RUN ./mvnw -B clean package -DskipTests

#####################################
# Stage 2: Runtime
#####################################

FROM eclipse-temurin:25-jre

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
# create a non root application user
RUN useradd --system --uid 10001 appuser

# copy generated jar
COPY --from=build /app/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Run as not root user
USER appuser

# Start java application
ENTRYPOINT ["java","-jar","app.jar"]