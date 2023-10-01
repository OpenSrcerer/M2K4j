FROM gradle:8.1.1-jdk17-jammy AS BUILD

WORKDIR /appbuild

COPY . .

ENV APP_LOGGING_LEVEL DEBUG

RUN gradle build --no-daemon

RUN "ls"

FROM openjdk:21-ea-17-slim AS RUN

WORKDIR /app

COPY --from=build /appbuild/build/libs/M2K4j.jar /app/M2K4j.jar

RUN "ls"

ARG DEBUG_OPT
ENV DEBUG_API_OPT=$DEBUG_OPT

CMD java $DEBUG_API_OPT -jar M2K4j.jar
