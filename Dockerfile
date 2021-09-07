FROM maven:3-openjdk-11 AS build
WORKDIR /build
ADD pom.xml .
RUN mvn -B dependency:go-offline
ADD . .
RUN mvn -B clean package

FROM openjdk:11
WORKDIR /bot
COPY --from=build /build/target/Marina.jar /bot
CMD ["java", "-jar", "Marina.jar"]
