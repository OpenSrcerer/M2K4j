FROM gradle:8.1.1-jdk17-jammy

WORKDIR /app

COPY . .

ENV APP_LOGGING_LEVEL DEBUG

RUN gradle build

ARG DEBUG_OPT
ENV DEBUG_API_OPT=$DEBUG_OPT

CMD java $DEBUG_API_OPT -jar build/libs/M2K4K-0.1.0.jar
