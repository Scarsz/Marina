FROM maven:3-openjdk-16 AS build
WORKDIR /build
ADD pom.xml .
RUN mvn -B dependency:go-offline
ADD . .
RUN mvn -B clean package

FROM openjdk:16
WORKDIR /bot
VOLUME /bot/pastes
COPY --from=build /build/target/Marina.jar /bot
EXPOSE 34117/tcp
CMD ["java", "-jar", "Marina.jar"]
