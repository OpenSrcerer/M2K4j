package online.danielstefani.m2k4j.mqtt

import online.danielstefani.m2k4j.dto.Mqtt5Message
import reactor.core.publisher.Mono

interface Mqtt5Consumer<R> {
    fun pushMessages(messages: List<Mqtt5Message>): Mono<R>
}