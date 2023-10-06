# https://gist.github.com/sumanmaity112/57d196c8f28c6051a8c8e569ec64a953#file-automated-slim-jre-dockerfile
# Thanks <3

FROM gradle:8.4.0-jdk17-jammy AS BUILD

WORKDIR /appbuild

COPY . .

RUN gradle build --no-daemon

FROM amazoncorretto:17-alpine as CORRETTO-DEPS

WORKDIR /app

COPY --from=build /appbuild/build/libs/M2K4j.jar .

# Get modules list
RUN unzip M2K4j.jar -d temp &&  \
    jdeps  \
      --print-module-deps \
      --ignore-missing-deps \
      --recursive \
      --multi-release 17 \
      --class-path="./temp/BOOT-INF/lib/*" \
      --module-path="./temp/BOOT-INF/lib/*" \
      M2K4j.jar > modules.txt

FROM amazoncorretto:17-alpine as CORRETTO-JDK

WORKDIR /app

COPY --from=corretto-deps /app/modules.txt .

# Output a custom jre built from the modules list
RUN apk add --no-cache binutils && \
    jlink \
     --verbose \
     --add-modules "$(cat modules.txt)" \
     --strip-debug \
     --no-man-pages \
     --no-header-files \
     --compress=2 \
     --output /jre

FROM alpine:latest AS RUN

COPY --from=corretto-jdk /jre /app/jre
COPY --from=build /appbuild/build/libs/M2K4j.jar /app/M2K4j.jar

WORKDIR /app

ARG DEBUG_OPT
ENV DEBUG_API_OPT=$DEBUG_OPT

CMD ./jre/bin/java $DEBUG_API_OPT -jar M2K4j.jar
